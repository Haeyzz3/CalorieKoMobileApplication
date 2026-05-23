# Firestore Room Offline-First Execution Plan

## Summary

The attached plan’s core objective is to make Room the durable source of truth and Firestore an eventually consistent replica driven by a local outbox. The implementation will add stable remote IDs, a `firestore_outbox` Room table, a dedicated Firestore worker, truthful Firestore methods that throw on failure, restore identity preservation, and lifecycle protections so offline writes/deletes survive process death and sync later.

Current repo findings: Room is at version `28`, there are no `remote_id` columns yet, `FirestoreSyncRepository` catches and suppresses failures, `SyncWorker` mixes Firestore/Laravel/food sync, `AutoSyncManager` uses global `REPLACE`, and ViewModels still directly call Firestore for pantry, planned meals, meal deletes, and onboarding writes.

## Files

New files:
- `app/src/main/java/com/calorieko/app/data/model/FirestoreOutboxEntity.kt`
- `app/src/main/java/com/calorieko/app/data/local/FirestoreOutboxDao.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreSyncOperation.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestorePayloadSerializer.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreOperationExecutor.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreSyncWorker.kt`
- `app/src/main/java/com/calorieko/app/data/remote/firestore/FirestoreAutoSyncManager.kt`
- `app/src/main/java/com/calorieko/app/data/repository/PantryRepository.kt`
- `app/src/main/java/com/calorieko/app/data/repository/MealPlanRepository.kt`

Major edits:
- `AppDatabase.kt`, `ActivityLogEntity.kt`, `MealLogEntity.kt`, `MealLogItemEntity.kt`
- `ActivityRepository.kt`, `MealRepository.kt`, `UserRepository.kt`
- `FirestoreSyncRepository.kt`, `CloudRestoreManager.kt`
- `MainActivity.kt`, `DiaryViewModel.kt`, `PantryViewModel.kt`, `ExploreViewModel.kt`, `LogMealViewModel.kt`, `ManualLogViewModel.kt`, `SettingsViewModel.kt`
- `SyncWorker.kt`, `AutoSyncManager.kt`
- `app/build.gradle.kts` and `gradle/libs.versions.toml` for focused test dependencies

## Key Changes

1. Add Room migration `28 -> 29`.
   - Add `firestore_outbox`.
   - Add `remote_id TEXT NOT NULL` to activity, meal, and meal item tables.
   - Backfill remote IDs from current local IDs.
   - Add remote ID indexes.
   - Register `FirestoreOutboxEntity` and DAO.
   - Remove or build-type-guard `fallbackToDestructiveMigration`.

2. Implement the Firestore outbox foundation.
   - Runtime writes create payload snapshots with Gson-backed serialization.
   - Migration-created outbox rows for existing unsynced activity/meal/weight rows may have `payload_json = null`; executor resolves those from Room only for this migration backfill case.
   - Supported operations for v1: `UPSERT_DOCUMENT`, `DELETE_DOCUMENT`, `DELETE_MEAL_LOG_RECURSIVE`, and `CLEAR_COLLECTION`.
   - Planned meal and pantry replace flows use ordered `CLEAR_COLLECTION` then upsert rows, not `REPLACE_COLLECTION_STATE`.

3. Split Firestore sync from the existing worker.
   - `FirestoreSyncWorker` drains pending outbox rows for one UID in `created_at, id` order.
   - It deletes an outbox row only after Firestore confirms success.
   - On first failure, it records attempt/error and returns `Result.retry()`.
   - `FirestoreAutoSyncManager` uses per-user work names and `ExistingWorkPolicy.APPEND_OR_REPLACE`.

4. Convert write paths one subsystem at a time.
   - Repositories that span multiple DAOs will accept `AppDatabase` and use `db.withTransaction`.
   - `UserRepository` owns profile, onboarding completion, and weight log writes.
   - `ActivityRepository` owns workout create/delete and activity tombstones.
   - `MealRepository` owns meal save/delete, item writes, daily summary updates, and meal tombstones.
   - New `PantryRepository` owns pantry add/remove/batch/clear.
   - New `MealPlanRepository` owns planned meal add/remove/clear/copy/replace.
   - ViewModels stop calling `FirestoreSyncRepository` for normal user-data mutations.

5. Make Firestore methods truthful and restore-safe.
   - Remove catch-and-suppress behavior from Firestore write/delete methods.
   - Restore fetches throw on collection fetch failure; empty lists mean actual empty collections.
   - Restored activity, meal, and meal item rows preserve Firestore document IDs in `remoteId`.
   - Restored rows are inserted as synced and create no outbox rows.

6. Harden lifecycle.
   - App launch/login enqueue immediate Firestore catch-up only after auth is verified and restore is `Success` or `NotNeeded`.
   - Logout policy: block logout if pending Firestore outbox rows exist, and emit a clear user-facing error/event.
   - Reset progress and delete account cancel per-user Firestore/API work and only clear Room after Firestore destructive operations succeed.
   - Existing `SyncWorker` keeps Laravel/food-catalog behavior only; Firestore sections are removed.

## Test Plan

Add tests for:
- Migration creates outbox table, remote IDs, indexes, and backfills unsynced rows.
- Profile/weight, activity, meal, pantry, and planned meal writes create Room data plus outbox rows atomically.
- Meal save/delete updates daily nutrition summaries inside the same transaction.
- Worker processes operations in order, removes successes, records failures, retries, and stops after first failure.
- Restore preserves Firestore document IDs and creates no outbox rows.
- Pantry clear-plus-add and planned-meal replace produce ordered clear then upsert operations.
- Logout with pending outbox is blocked.
- Existing Laravel sync still runs without Firestore user-data writes.

## Assumptions

- No repo files are mutated in this Plan Mode turn.
- Gson will be used for outbox payload JSON because the project already includes it via Retrofit.
- New local rows use UUID remote IDs; migrated existing rows use their current numeric local IDs as remote IDs.
- Firestore document paths remain compatible with existing cloud data.
- The existing Settings “Sync Data” button remains Laravel/API-focused, not Firestore-focused.
