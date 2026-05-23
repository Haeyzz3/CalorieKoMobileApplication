# Firestore and Room Offline-First Implementation Plan

Date: 2026-05-23

## Scope

This plan focuses only on Firebase Auth identity, Cloud Firestore persistence, Room local storage, WorkManager scheduling, cloud restore, and offline retry behavior.

Out of scope for this plan:

- Laravel/API sync behavior
- Settings "Sync Data" behavior
- Food catalog pull behavior, unless it is currently coupled to the same worker and must be separated
- UI redesign

## Current Problems To Address

1. Firestore sync calls swallow exceptions, so callers cannot tell failure from success.
2. `SyncWorker` marks Room rows as synced even when Firestore writes failed internally.
3. Deletes are not durable offline operations because there are no local tombstones or outbox entries.
4. Restored Firestore documents lose their original Firestore document IDs, so future deletes can target the wrong documents.
5. Activity restore inserts cloud rows as pending local rows, causing possible re-upload duplicates.
6. Pantry and planned meal full-state sync clears Firestore before re-pushing, which can leave cloud data empty if the re-push fails.
7. App launch does not guarantee a Firestore catch-up sync.
8. Unique Work uses `REPLACE`, which can cancel an in-flight sync.
9. Logout clears unsynced local data and does not consistently cancel pending sync work.
10. Meal writes and nutrition summary updates are not atomic.
11. Reset/delete-account flows assume Firestore deletes succeeded, but Firestore delete methods currently suppress errors.
12. There is no meaningful automated test coverage for sync behavior.

## Target Architecture

Room remains the source of truth for all user-visible data. Firestore becomes a eventually consistent replica of Room, driven by a durable local sync outbox.

Every user data mutation follows this rule:

1. Open a Room transaction.
2. Apply the local data change.
3. Insert one or more durable outbox records describing the remote Firestore work.
4. Commit the transaction.
5. Enqueue Firestore sync work.

The app reports user success after the Room transaction commits, not after network work completes.

The worker follows this rule:

1. Read pending outbox records in order.
2. Execute the Firestore operation.
3. Only mark/delete the outbox record after Firestore confirms success.
4. Retry failed records with WorkManager backoff and local attempt metadata.

## Core Design Decisions

### 1. Add A Firestore Outbox

Add a Room table named `firestore_outbox`.

Suggested entity:

```kotlin
@Entity(
    tableName = "firestore_outbox",
    indices = [
        Index(value = ["uid", "state", "created_at"]),
        Index(value = ["uid", "entity_type", "entity_key"])
    ]
)
data class FirestoreOutboxEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_key")
    val entityKey: String,

    @ColumnInfo(name = "operation")
    val operation: String,

    @ColumnInfo(name = "remote_path")
    val remotePath: String,

    @ColumnInfo(name = "payload_json")
    val payloadJson: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "state")
    val state: String = "PENDING"
)
```

Supported operations:

- `UPSERT_DOCUMENT`
- `DELETE_DOCUMENT`
- `DELETE_MEAL_LOG_RECURSIVE`
- `CLEAR_COLLECTION`
- `REPLACE_COLLECTION_STATE`

Notes:

- `UPSERT_DOCUMENT` should use a complete payload snapshot where practical. This keeps the worker from depending on rows that may later be deleted or changed.
- `DELETE_DOCUMENT` requires only a stable Firestore path.
- `DELETE_MEAL_LOG_RECURSIVE` deletes child item documents and then the meal log document.
- `CLEAR_COLLECTION` is an ordered barrier operation. Operations created after it must run after it.
- `REPLACE_COLLECTION_STATE` can be used only when the payload includes the full target state and the worker can complete clear plus re-push before marking the operation complete.

### 2. Add Stable Remote IDs

Local auto-generated Room IDs must not be treated as long-term Firestore identity. They change after restore and can collide across devices or reinstalls.

Add stable remote ID columns:

- `activity_log_table.remote_id TEXT NOT NULL`
- `meal_log_table.remote_id TEXT NOT NULL`
- `meal_log_item_table.remote_id TEXT NOT NULL`

Generation rules:

- New local rows get `UUID.randomUUID().toString()`.
- Restored rows preserve the Firestore document ID as `remote_id`.
- Existing rows migrate to their current Firestore-compatible IDs:
  - activity: `remote_id = id.toString()`
  - meal: `remote_id = meal_log_id.toString()`
  - meal item: `remote_id = meal_log_item_id.toString()`

Firestore document paths after migration:

- `users/{uid}/activityLogs/{activity.remoteId}`
- `users/{uid}/mealLogs/{meal.remoteId}`
- `users/{uid}/mealLogs/{meal.remoteId}/items/{item.remoteId}`
- `users/{uid}/dailyNutritionSummaries/{dateEpochDay}`
- `users/{uid}/weightLogs/{timestamp}`
- `users/{uid}/pantryItems/{ingredientName}`
- `users/{uid}/plannedMeals/{dayIndex}_{weekStartDate}_{mealSlot}_{dishLabel}`

### 3. Make Firestore Repository Methods Truthful

Change `FirestoreSyncRepository` from "catch and suppress" to "return success or throw".

Preferred contract:

```kotlin
suspend fun upsertActivityLog(uid: String, log: ActivityLogEntity)
suspend fun deleteActivityLog(uid: String, remoteId: String)
suspend fun upsertMealLog(uid: String, meal: MealLogEntity, items: List<MealLogItemEntity>)
suspend fun deleteMealLogRecursive(uid: String, mealRemoteId: String)
```

Rules:

- Do not catch exceptions unless adding context and rethrowing.
- Do not log and return normally on failure.
- Worker decides retry/failure behavior.
- UI write paths should not call Firestore directly for user data mutations.

### 4. Separate Firestore Sync From Existing Mixed Worker

Create a dedicated Firestore worker, for example:

- `data/remote/firestore/FirestoreSyncWorker.kt`
- `data/remote/firestore/FirestoreAutoSyncManager.kt`
- `data/remote/firestore/FirestoreOutboxDao.kt`

Do not let Firestore reliability depend on the existing worker that also handles Laravel/API sync and food catalog work.

The Firestore worker should:

1. Require `NetworkType.CONNECTED`.
2. Require a non-empty UID in input data.
3. Drain pending outbox entries for that UID in `created_at`, `id` order.
4. Process a bounded batch per run, for example 50 to 200 records.
5. Mark each record complete only after Firestore confirms success.
6. On failure, increment `attempt_count`, store `last_error`, and return `Result.retry()`.
7. Never mark domain rows synced based on a swallowed Firestore call.

### 5. Replace `REPLACE` Work Policy

Do not cancel in-flight sync work.

Use a unique work name per user, for example:

```text
calorieko_firestore_sync_{uid}
```

Preferred policy:

- Use `ExistingWorkPolicy.APPEND_OR_REPLACE` for one-time sync requests.
- Keep the 3-second initial delay for write batching.
- Add a final worker check for remaining pending outbox records and enqueue another run if needed.

Rationale:

- `REPLACE` can cancel a running sync between Firestore writes and local status updates.
- Per-user unique work prevents one account from replacing another account's pending work.
- Appending avoids losing a sync request that arrives while a previous sync is running.

### 6. Enqueue Catch-Up Sync On App Launch And Login

On app launch, after Firebase Auth state is known and the user is verified/logged in:

1. Check whether Room has pending outbox records for `uid`.
2. Enqueue Firestore sync immediately if any exist.

Suggested call sites:

- After `SplashViewModel` determines the current user is verified.
- After `RestoreViewModel.restore()` returns `NotNeeded` or `Success`.
- After successful login.
- After onboarding profile creation and completion writes.

Do not enqueue blindly before Firebase Auth is available.

## Data Model And Migration Plan

### New Entities And DAOs

Add:

- `FirestoreOutboxEntity`
- `FirestoreOutboxDao`
- `FirestoreSyncOperation` constants or enum-like object
- `FirestorePayloadSerializer`

DAO capabilities:

```kotlin
@Query("SELECT * FROM firestore_outbox WHERE uid = :uid AND state = 'PENDING' ORDER BY created_at ASC, id ASC LIMIT :limit")
suspend fun getPending(uid: String, limit: Int): List<FirestoreOutboxEntity>

@Insert
suspend fun insert(operation: FirestoreOutboxEntity): Long

@Insert
suspend fun insertAll(operations: List<FirestoreOutboxEntity>)

@Query("DELETE FROM firestore_outbox WHERE id = :id")
suspend fun deleteById(id: Long)

@Query("UPDATE firestore_outbox SET attempt_count = attempt_count + 1, last_error = :error, updated_at = :now WHERE id = :id")
suspend fun recordFailure(id: Long, error: String, now: Long)

@Query("SELECT COUNT(*) FROM firestore_outbox WHERE uid = :uid AND state = 'PENDING'")
suspend fun pendingCount(uid: String): Int
```

### Entity Migrations

Add a new Room migration after the current version.

Migration steps:

1. Add `firestore_outbox` table.
2. Add `remote_id` to `activity_log_table`.
3. Add `remote_id` to `meal_log_table`.
4. Add `remote_id` to `meal_log_item_table`.
5. Backfill remote IDs from current local IDs.
6. Create indices for remote IDs:
   - `activity_log_table(uid, remote_id)`
   - `meal_log_table(uid, remote_id)`
   - `meal_log_item_table(meal_log_id, remote_id)`

Important:

- Avoid `fallbackToDestructiveMigration` for production builds. If it remains for dev builds, guard it by build type or remove it before release.
- Existing synced rows should not automatically get outbox records unless there is a known reason to backfill.
- Existing unsynced rows with `sync_status = 0` should get migration outbox records so they remain retryable under the new system.

### Sync Status Fields

Keep current `sync_status` fields during the transition, but stop relying on them as the primary retry source.

Long term:

- Either remove them in a later migration, or redefine them as a UI hint only.
- The outbox should be the durable source for pending remote work.

## Write Path Implementation Plan

### User Profile

Current paths:

- `UserRepository.saveProfile()`
- onboarding profile creation in `MainActivity`
- onboarding completion write in `MainActivity`

Required changes:

1. Route all profile writes through `UserRepository`.
2. In a Room transaction:
   - Insert/update `user_profile`.
   - If weight changed, insert `weight_log_table`.
   - Insert profile `UPSERT_DOCUMENT` outbox entry.
   - Insert weight log `UPSERT_DOCUMENT` outbox entry if applicable.
3. Remove direct Firestore calls from profile write paths.
4. Ensure onboarding completion updates `updatedAt` and creates a profile outbox entry.

### Weight Logs

Current path:

- `UserRepository.recordWeightIfChanged()`
- onboarding initial weight log in `MainActivity`

Required changes:

1. Create weight log in the same transaction as profile save when possible.
2. Add outbox `UPSERT_DOCUMENT` for `users/{uid}/weightLogs/{timestamp}`.
3. Do not mark weight logs synced unless the corresponding outbox operation completes.

### Workout / Activity Logs

Current paths:

- `ActivityRepository.insertWorkoutLog()`
- `DiaryViewModel.deleteActivity()`

Required changes:

1. Generate `remoteId` before inserting the activity.
2. Insert activity and outbox upsert in one transaction.
3. Delete activity by:
   - Fetching the row to get `remoteId`.
   - Inserting outbox `DELETE_DOCUMENT`.
   - Deleting the local row.
   - Committing transaction.
   - Enqueuing Firestore sync.
4. Remove comment claiming another sync layer handles deletes unless implemented.

### Meal Logs

Current paths:

- `MealRepository.saveMeal()`
- `MealRepository.deleteMealLogLocally()`
- `DiaryViewModel.deleteMeal()`

Required changes for save:

1. Wrap meal header insert, item insert, nutrition summary upsert, and outbox insertions in one Room transaction.
2. Generate stable `remoteId` for meal and each item before insert.
3. Build a complete meal payload for the outbox.
4. Add daily nutrition summary outbox upsert in the same transaction.
5. Enqueue sync after commit.

Required changes for delete:

1. Fetch meal and items before deletion.
2. Compute updated nutrition summary.
3. In one Room transaction:
   - Insert `DELETE_MEAL_LOG_RECURSIVE` outbox record using `meal.remoteId`.
   - Upsert updated daily nutrition summary.
   - Insert summary `UPSERT_DOCUMENT` outbox record.
   - Delete the meal locally.
4. Remove direct Firestore delete from `DiaryViewModel`.
5. Enqueue sync after commit.

Concurrency note:

- The summary read-modify-write should be protected by the same Room transaction.
- Consider recalculating summaries from meal rows instead of subtracting stale snapshots for higher correctness.

### Daily Nutrition Summaries

Current paths:

- Upserted by `MealRepository.saveMeal()`
- Upserted by `MealRepository.deleteMealLogLocally()`

Required changes:

1. Treat summaries as derived local state but still sync them as documents for restore/dashboard needs.
2. Every summary upsert caused by a meal mutation should create a summary outbox `UPSERT_DOCUMENT`.
3. Use deterministic Firestore document ID `dateEpochDay`.

### Pantry Items

Current paths:

- `PantryViewModel`
- `ExploreViewModel`
- `LogMealViewModel.confirmPantryDeduction()`
- `ManualLogViewModel.confirmPantryDeduction()`

Required changes:

1. Create a `PantryRepository` to own all pantry writes.
2. Replace direct DAO writes in ViewModels with repository calls.
3. For add:
   - Insert Room pantry item.
   - Insert outbox `UPSERT_DOCUMENT`.
4. For remove:
   - Delete Room pantry item.
   - Insert outbox `DELETE_DOCUMENT`.
5. For batch update:
   - Compute add/remove diff.
   - Apply Room changes and outbox operations in one transaction.
6. For clear all:
   - Insert ordered `CLEAR_COLLECTION` outbox operation for `users/{uid}/pantryItems`.
   - Clear local table.
   - Do not use direct Firestore clear in the ViewModel.

Avoid clear-then-repush full-state sync except through a durable outbox operation that is retried until complete.

### Planned Meals

Current paths:

- `PantryViewModel.addMealToPlan()`
- `removeDishFromSlot()`
- `clearMealSlot()`
- `clearMealDay()`
- `clearMealWeek()`
- `copyCurrentWeekToNextReplacing()`
- `copyMealSlot()`
- `copySingleDish()`

Required changes:

1. Create a `MealPlanRepository` to own all planned meal writes.
2. Replace direct DAO writes in `PantryViewModel` with repository calls.
3. For single add/copy:
   - Upsert local row.
   - Insert outbox `UPSERT_DOCUMENT`.
4. For single remove:
   - Delete local row.
   - Insert outbox `DELETE_DOCUMENT`.
5. For clear slot/day/week:
   - Insert `CLEAR_COLLECTION` with query metadata, or generate individual delete outbox records for known local rows.
   - Apply local delete in the same transaction.
6. For replace week/slot:
   - Insert an ordered clear operation first.
   - Insert upsert operations for copied meals after the clear.
   - Apply local replacement in the same transaction.

Ordering is important. A replace operation must not be coalesced into only upserts, because stale Firestore documents need deletion.

## Firestore Worker Algorithm

Pseudo-flow:

```kotlin
override suspend fun doWork(): Result {
    val uid = inputData.getString(KEY_UID) ?: return Result.failure()
    val db = (applicationContext as CalorieKoApplication).database

    val pending = db.firestoreOutboxDao().getPending(uid, limit = 100)
    if (pending.isEmpty()) return Result.success()

    for (op in pending) {
        try {
            firestoreOperationExecutor.execute(op)
            db.firestoreOutboxDao().deleteById(op.id)
        } catch (e: Exception) {
            db.firestoreOutboxDao().recordFailure(op.id, e.message ?: e::class.java.simpleName, System.currentTimeMillis())
            return Result.retry()
        }
    }

    if (db.firestoreOutboxDao().pendingCount(uid) > 0) {
        FirestoreAutoSyncManager.triggerSync(applicationContext, uid)
    }

    return Result.success()
}
```

Execution rules:

- Preserve outbox order.
- Do not process later operations after a failed earlier operation.
- Do not delete an outbox row until Firestore confirms success.
- Use Firestore `WriteBatch` for groups of writes only when one outbox record represents a grouped operation.
- Respect Firestore 500-operation batch limits.
- For recursive meal deletes, delete item documents in batches, then delete the parent meal document.

## Cloud Restore Implementation Plan

### Preserve Firestore IDs

Update fetch methods:

- `fetchActivityLogs()` should set `remoteId = doc.id` and `syncStatus = 1`.
- `fetchMealLogs()` should set `meal.remoteId = mealDoc.id`.
- `fetchMealLogs()` should set each item `remoteId = itemDoc.id`.
- `fetchWeightLogs()` already sets `syncStatus = 1`; preserve that behavior.

### Do Not Create Outbox Entries During Restore

Cloud restore should be treated as seeding Room from a trusted remote snapshot.

Rules:

- Insert restored rows with synced state.
- Do not create outbox records for restored rows.
- Do not enqueue sync solely because restore inserted local data.

### Restore Transaction

Keep restore atomic through `db.withTransaction`.

Additional checks:

- If Firestore profile exists but subcollection fetches fail, fail the restore and roll back.
- Do not silently convert fetch errors into empty lists for restore. `fetch*` methods used by restore should throw on permission/network failures. Empty lists should mean the collection actually exists but has no documents.

## Launch And Account Lifecycle Plan

### App Launch

After Firebase Auth has a verified current user:

1. Run restore if local profile is missing.
2. If restore is not needed or succeeds, check outbox pending count.
3. Enqueue Firestore sync if pending count is greater than zero.

### Login

After login and restore flow:

1. Trigger catch-up sync for pending local outbox rows.
2. Do not trigger if restore failed and local profile is missing.

### Logout

Before clearing local tables:

1. Check pending outbox count.
2. If count is greater than zero, either:
   - block logout with a clear message, or
   - run a foreground/user-visible sync and only logout after success.
3. Cancel pending Firestore sync work for the current UID after successful sync or after user confirms discarding unsynced local changes.
4. Clear user tables.
5. Sign out.

This closes data loss outside the accepted "manual clear app storage before sync" limitation.

### Reset Progress

Do not clear Room after a swallowed Firestore wipe.

Required changes:

1. Firestore delete methods must throw on failure.
2. Reset progress should wait for confirmed Firestore wipe.
3. Then clear Room and outbox in one local transaction.
4. Cancel pending Firestore sync work for that UID.

### Delete Account

Required changes:

1. Re-authenticate.
2. Cancel Firestore sync work for UID.
3. Delete Firestore data using methods that throw on failure.
4. Clear local Room data and outbox.
5. Delete Firebase Auth account last.

## File-Level Change Map

Expected new files:

- `app/src/main/java/com/calorieko/app/data/model/FirestoreOutboxEntity.kt`
- `app/src/main/java/com/calorieko/app/data/local/FirestoreOutboxDao.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreAutoSyncManager.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreSyncWorker.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreOperationExecutor.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestorePayloadSerializer.kt`
- `app/src/main/java/com/calorieko/app/data/repository/PantryRepository.kt`
- `app/src/main/java/com/calorieko/app/data/repository/MealPlanRepository.kt`

Expected major edits:

- `AppDatabase.kt`
  - Add entity, DAO, migration, indices.
- `ActivityLogEntity.kt`
  - Add `remoteId`.
- `MealLogEntity.kt`
  - Add `remoteId`.
- `MealLogItemEntity.kt`
  - Add `remoteId`.
- `FirestoreSyncRepository.kt`
  - Stop swallowing exceptions.
  - Move low-level Firestore operations behind truthful method contracts.
- `ActivityRepository.kt`
  - Make writes transactional with outbox records.
- `MealRepository.kt`
  - Make meal save/delete transactional with outbox records.
- `UserRepository.kt`
  - Make profile/weight writes transactional with outbox records.
- `PantryViewModel.kt`
  - Replace direct DAO plus Firestore calls with `PantryRepository`.
- `ExploreViewModel.kt`
  - Replace direct pantry writes with `PantryRepository`.
- `LogMealViewModel.kt`
  - Replace pantry deduction direct writes with `PantryRepository`.
- `ManualLogViewModel.kt`
  - Replace pantry deduction direct writes with `PantryRepository`.
- `DiaryViewModel.kt`
  - Replace direct delete logic with repositories.
- `CloudRestoreManager.kt`
  - Preserve remote IDs and synced state.
- `RestoreViewModel.kt` or navigation login flow
  - Trigger launch/login catch-up sync after restore succeeds or is not needed.
- `SettingsViewModel.kt`
  - Only for logout/reset/delete-account Firestore correctness, not Settings "Sync Data".

## Phased Implementation

### Phase 1: Build The Sync Foundation

1. Add `FirestoreOutboxEntity` and DAO.
2. Add stable `remoteId` fields and migrations.
3. Add `FirestoreOperationExecutor`.
4. Change Firestore methods to throw on failure.
5. Add `FirestoreSyncWorker`.
6. Add `FirestoreAutoSyncManager`.
7. Wire app/login catch-up scheduling.
8. Keep old sync paths disabled for Firestore user data after the new worker is wired.

Exit criteria:

- A local upsert creates an outbox row.
- Worker sends it to Firestore.
- Worker deletes outbox row only after success.
- Failed Firestore operation remains pending and retries.

### Phase 2: Move Creates/Updates To The Outbox

1. User profile and weight logs.
2. Activity log creation.
3. Meal creation and daily summary upsert.
4. Pantry add/remove/batch update.
5. Planned meal add/copy/update.

Exit criteria:

- No ViewModel directly calls Firestore for normal user data writes.
- All Room writes that affect Firestore create outbox operations in the same transaction.

### Phase 3: Implement Durable Deletes And Clears

1. Activity delete tombstones.
2. Meal recursive delete tombstones.
3. Pantry delete and clear operations.
4. Planned meal single delete, slot/day/week clear, and replace operations.
5. Reset progress and delete-account methods changed to fail if Firestore deletion fails.

Exit criteria:

- Offline deletes survive process death.
- On reconnect, deleted Firestore documents are actually removed.
- Clear/replace operations do not leave stale Firestore documents.

### Phase 4: Fix Restore Identity

1. Preserve activity Firestore doc ID.
2. Preserve meal Firestore doc ID.
3. Preserve meal item Firestore doc ID.
4. Mark restored rows as synced.
5. Ensure restore does not create outbox records.
6. Make restore fetch methods distinguish fetch failure from empty collection.

Exit criteria:

- Fresh install restore followed by delete removes the original Firestore document.
- Fresh install restore followed by sync does not duplicate cloud rows.

### Phase 5: Account Lifecycle Hardening

1. Logout handles pending outbox before clearing Room.
2. App launch and post-login enqueue catch-up sync.
3. WorkManager unique names are per UID.
4. In-flight work is not canceled by normal writes.
5. Pending sync is canceled only during explicit destructive account lifecycle flows.

Exit criteria:

- Opening the app online with pending local changes starts Firestore sync without manual action.
- Rapid writes do not cancel a running sync.
- Logout does not silently discard pending local changes.

### Phase 6: Cleanup And Removal Of Old Behavior

1. Remove direct Firestore writes from ViewModels.
2. Remove Firestore portions from the mixed sync worker or leave them unused.
3. Keep Laravel/API code separate and untouched.
4. Update comments that currently claim Firestore is write-through or fire-and-forget.
5. Add developer documentation for the outbox contract.

Exit criteria:

- Firestore sync behavior is understandable from repositories, outbox DAO, and worker.
- Comments match implementation.

## Test Plan

### Unit Tests

Add tests for:

- Outbox insertion on profile save.
- Outbox insertion on weight change.
- Outbox insertion on activity creation.
- Outbox delete operation on activity delete.
- Meal save creates meal, items, summary, and outbox operations atomically.
- Meal delete creates recursive delete and summary upsert outbox operations atomically.
- Pantry batch update creates expected add/delete operations.
- Planned meal replace creates ordered clear then upsert operations.
- Restore maps Firestore document IDs to `remoteId`.
- Restore inserts rows as synced and creates no outbox rows.

### Worker Tests

Use WorkManager test utilities or a fake executor.

Cover:

- Successful outbox operation is removed.
- Failed outbox operation remains pending and records error.
- Worker returns retry after first failed ordered operation.
- Later operations are not processed after an earlier failure.
- Multiple pending operations process in order.
- A new work request does not cancel an in-flight run.

### Firestore Repository Tests

Use fake Firestore wrapper if direct Firebase SDK mocking is painful.

Cover:

- Upsert payload includes all expected fields.
- Meal upsert writes parent and item docs.
- Recursive meal delete deletes child item docs before parent.
- Repository methods throw on failure.

### Integration / Instrumented Tests

Minimum scenarios:

1. Offline meal creation:
   - Save meal with network unavailable.
   - Verify Room rows and outbox exist.
   - Simulate network and worker success.
   - Verify outbox empty and Firestore fake received meal plus summary.

2. Offline meal delete:
   - Seed Room with a synced meal and remote IDs.
   - Delete offline.
   - Verify Room deletion and outbox tombstone.
   - Run worker.
   - Verify Firestore delete path uses original remote ID.

3. Restore then delete:
   - Restore a Firestore meal document with doc ID `abc`.
   - Delete restored meal.
   - Verify delete targets `users/{uid}/mealLogs/abc`.

4. Pantry clear plus add:
   - Seed pantry with A/B/C.
   - Clear pantry.
   - Add D before sync.
   - Run worker.
   - Verify Firestore ends with D only.

5. Rapid writes:
   - Trigger multiple writes while worker is running.
   - Verify no cancellation and all operations eventually sync.

6. Logout with pending outbox:
   - Seed pending operation.
   - Attempt logout.
   - Verify chosen behavior: block, sync first, or explicit discard confirmation.

## Manual QA Checklist

Before release:

- Create profile online, verify Firestore profile and weight log.
- Edit profile offline, close app, reopen online, verify Firestore updates.
- Log workout offline, reopen online, verify Firestore activity appears.
- Delete workout offline, reopen online, verify Firestore activity is removed.
- Log meal offline, reopen online, verify Firestore meal and summary appear.
- Delete meal offline, reopen online, verify Firestore meal is removed and summary is updated.
- Add pantry item offline, reopen online, verify Firestore pantry item appears.
- Remove pantry item offline, reopen online, verify Firestore pantry item is removed.
- Clear pantry offline, add another item, reopen online, verify cloud has only the new item.
- Add planned meals offline, reopen online, verify Firestore planned meals appear.
- Replace a planned meal slot offline, reopen online, verify stale cloud planned meals are removed.
- Fresh install restore, delete restored meal, verify original Firestore document is removed.
- Rapidly log multiple meals/workouts, verify no duplicates and no missing rows.

## Acceptance Criteria

The implementation is complete when:

1. Every user-facing Firestore-relevant mutation writes to Room first.
2. Every mutation creates durable outbox work in the same Room transaction.
3. No normal user-data ViewModel calls Firestore directly.
4. Firestore repository methods do not swallow failures.
5. Failed Firestore writes/deletes remain retryable after process death.
6. Opening the app online enqueues pending Firestore sync within the expected 5-10 second window.
7. WorkManager no longer cancels in-flight Firestore sync due to later writes.
8. Offline deletes and clears are propagated correctly on reconnect.
9. Restored rows preserve Firestore identity and do not duplicate on the next sync.
10. Logout/reset/delete-account flows do not silently discard unsynced data.
11. Automated tests cover create, update, delete, restore, retry, and rapid-write scenarios.

## Risks And Mitigations

### Risk: Large Migration Surface

Adding remote IDs and an outbox touches multiple data paths.

Mitigation:

- Implement by feature area.
- Keep existing local UI reads unchanged.
- Add tests before deleting old Firestore write paths.

### Risk: Ordered Clear/Replace Semantics

Pantry and planned meal operations can become incorrect if clear and add operations are reordered or coalesced incorrectly.

Mitigation:

- Treat clear operations as ordering barriers.
- Do not coalesce operations across a clear barrier.
- Add tests for clear followed by add before sync.

### Risk: Firestore Recursive Deletes

Firestore client SDK does not recursively delete subcollections automatically.

Mitigation:

- Keep explicit child item query and batched child deletes for meal deletion.
- Preserve meal remote ID during restore.
- Consider flattening meal items into parent documents later only if Firestore query needs allow it.

### Risk: Existing Cloud Documents Use Old Numeric IDs

Existing Firestore documents may already use local numeric IDs.

Mitigation:

- Migration backfills `remoteId` from current local IDs.
- Restore preserves existing Firestore doc IDs whether numeric or UUID.
- New rows use UUIDs.

### Risk: Pending Outbox During Logout

Blocking logout can be frustrating, but silent data loss is worse.

Mitigation:

- Provide a clear UX choice later: "Sync before logout" or "Discard unsynced changes".
- The implementation should support both; product can decide wording.

## Recommended Implementation Order

1. Add outbox table, DAO, remote IDs, and migrations.
2. Build Firestore worker and executor with fakeable dependencies.
3. Make Firestore repository methods throw on failure.
4. Convert ActivityRepository create/delete first as the smallest vertical slice.
5. Add worker tests around that vertical slice.
6. Convert MealRepository save/delete and summary sync.
7. Convert UserRepository profile/weight writes.
8. Add PantryRepository and convert pantry flows.
9. Add MealPlanRepository and convert planned meal flows.
10. Fix restore identity preservation.
11. Add launch/login catch-up scheduling.
12. Harden logout/reset/delete-account flows.
13. Remove old direct Firestore write paths and stale comments.

