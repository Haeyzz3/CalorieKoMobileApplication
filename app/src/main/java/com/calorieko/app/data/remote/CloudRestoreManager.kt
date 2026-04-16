package com.calorieko.app.data.remote

import android.util.Log
import androidx.room.withTransaction
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Result of the cloud restore operation.
 */
sealed class RestoreResult {
    /** Local profile already exists — restore was skipped. */
    data object NotNeeded : RestoreResult()

    /** Data was successfully restored from Firestore. */
    data class Success(val profileName: String) : RestoreResult()

    /** No Firestore profile found — this is a genuinely new user. */
    data object NoCloudData : RestoreResult()

    /** Restore failed due to an error (e.g., offline, permission denied). */
    data class Failed(val error: String) : RestoreResult()
}

/**
 * Orchestrates a one-time bulk data restore from Cloud Firestore
 * into the local Room database.
 *
 * "One-time" means: it only runs when `userDao.getUser(uid) == null`,
 * i.e., the local database has no profile for the authenticated user.
 * This covers fresh installs, reinstalls, and post-DB-wipe scenarios.
 *
 * Restore order (respecting foreign key dependencies):
 * 1. Profile → 2. Activity Logs → 3. Meal Logs (with ID re-mapping)
 * → 4. Daily Nutrition Summaries → 5. Pantry Items → 6. Planned Meals
 *
 * All Room inserts are wrapped in a single [withTransaction] block.
 * If any step fails, the entire transaction rolls back — leaving the
 * database clean so the restore can be retried on the next launch.
 */
class CloudRestoreManager(
    private val db: AppDatabase,
    private val firestoreSyncRepo: FirestoreSyncRepository,
    private val userDao: UserDao,
    private val activityLogDao: ActivityLogDao,
    private val mealLogDao: MealLogDao,
    private val mealLogItemDao: MealLogItemDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val pantryDao: PantryDao,
    private val mealPlanDao: MealPlanDao
) {
    companion object {
        private const val TAG = "CloudRestore"
    }

    /**
     * Holds all data fetched from Firestore so the 5 parallel fetches
     * can be destructured cleanly at the call site.
     */
    private data class FetchResults(
        val activityLogs: List<ActivityLogEntity>,
        val mealLogsWithItems: List<Pair<MealLogEntity, List<MealLogItemEntity>>>,
        val summaries: List<DailyNutritionSummaryEntity>,
        val pantryItems: List<String>,
        val plannedMeals: List<PlannedMealEntity>
    )

    /**
     * Checks if a cloud restore is needed and performs it if so.
     *
     * This method is safe to call from any coroutine context — it does
     * not touch the UI thread. The caller should invoke this on
     * [kotlinx.coroutines.Dispatchers.IO].
     */
    suspend fun restoreIfNeeded(uid: String): RestoreResult {
        return try {
            // Step 0: Check if local data already exists
            val existingProfile = userDao.getUser(uid)
            if (existingProfile != null) {
                Log.d(TAG, "Local profile exists for $uid — skipping restore")
                return RestoreResult.NotNeeded
            }

            Log.d(TAG, "No local profile for $uid — checking Firestore...")

            // Step 1: Fetch profile (the gate check)
            val profile = firestoreSyncRepo.fetchProfile(uid)
            if (profile == null) {
                Log.d(TAG, "No Firestore profile — this is a new user")
                return RestoreResult.NoCloudData
            }

            // ═══ Phase 1: Fetch ALL data from Firestore (parallel) ═══
            Log.d(TAG, "Fetching all cloud data for ${profile.name} (parallel)...")

            val fetchResults = coroutineScope {
                val activityLogsDeferred = async { firestoreSyncRepo.fetchActivityLogs(uid) }
                val mealLogsDeferred = async { firestoreSyncRepo.fetchMealLogs(uid) }
                val summariesDeferred = async { firestoreSyncRepo.fetchDailyNutritionSummaries(uid) }
                val pantryDeferred = async { firestoreSyncRepo.fetchPantryItems(uid) }
                val plannedDeferred = async { firestoreSyncRepo.fetchPlannedMeals(uid) }

                FetchResults(
                    activityLogs = activityLogsDeferred.await(),
                    mealLogsWithItems = mealLogsDeferred.await(),
                    summaries = summariesDeferred.await(),
                    pantryItems = pantryDeferred.await(),
                    plannedMeals = plannedDeferred.await()
                )
            }

            Log.d(TAG, "  Fetched: ${fetchResults.activityLogs.size} activity logs, " +
                    "${fetchResults.mealLogsWithItems.size} meal logs, " +
                    "${fetchResults.summaries.size} summaries, " +
                    "${fetchResults.pantryItems.size} pantry items, " +
                    "${fetchResults.plannedMeals.size} planned meals")

            // ═══ Phase 2: Insert ALL data into Room (atomic transaction) ═══
            Log.d(TAG, "Starting atomic Room transaction...")

            db.withTransaction {
                // 1. Profile
                userDao.insertUser(profile)

                // 2. Activity Logs
                for (log in fetchResults.activityLogs) {
                    activityLogDao.insertLog(log)
                }

                // 3. Meal Logs (with parent-child ID re-mapping)
                //    Mark as synced (status=1) since they came FROM Firestore cloud.
                for ((mealLog, items) in fetchResults.mealLogsWithItems) {
                    // Insert the parent with syncStatus=1 → get the new Room-generated mealLogId
                    val newMealLogId = mealLogDao.insertMealLog(mealLog.copy(syncStatus = 1))

                    // Re-map each child item's foreign key to the new parent ID
                    val remappedItems = items.map { item ->
                        item.copy(mealLogId = newMealLogId)
                    }
                    if (remappedItems.isNotEmpty()) {
                        mealLogItemDao.insertItems(remappedItems)
                    }
                }

                // 4. Daily Nutrition Summaries
                for (summary in fetchResults.summaries) {
                    dailyNutritionSummaryDao.upsertSummary(summary)
                }

                // 5. Pantry Items
                for (itemName in fetchResults.pantryItems) {
                    pantryDao.insertItem(PantryItem(ingredientName = itemName))
                }

                // 6. Planned Meals
                for (meal in fetchResults.plannedMeals) {
                    mealPlanDao.insertMeal(meal)
                }
            }

            Log.d(TAG, "═══ Full restore complete for ${profile.name} ═══")
            RestoreResult.Success(profile.name)

        } catch (e: Exception) {
            Log.e(TAG, "Cloud restore failed for $uid", e)
            RestoreResult.Failed(e.message ?: "Unknown error")
        }
    }
}
