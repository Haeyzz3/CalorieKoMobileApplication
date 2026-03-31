package com.calorieko.app.data.remote

import android.util.Log
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.PantryItem

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
 */
class CloudRestoreManager(
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

            // ═══ Begin Restore ═══
            Log.d(TAG, "Starting full restore for ${profile.name}...")

            // 1. Profile
            userDao.insertUser(profile)
            Log.d(TAG, "✓ Profile restored")

            // 2. Activity Logs
            val activityLogs = firestoreSyncRepo.fetchActivityLogs(uid)
            for (log in activityLogs) {
                activityLogDao.insertLog(log)
            }
            Log.d(TAG, "✓ ${activityLogs.size} activity logs restored")

            // 3. Meal Logs (with parent-child ID re-mapping)
            val mealLogsWithItems = firestoreSyncRepo.fetchMealLogs(uid)
            for ((mealLog, items) in mealLogsWithItems) {
                // Insert the parent → get the new Room-generated mealLogId
                val newMealLogId = mealLogDao.insertMealLog(mealLog)

                // Re-map each child item's foreign key to the new parent ID
                val remappedItems = items.map { item ->
                    item.copy(mealLogId = newMealLogId)
                }
                if (remappedItems.isNotEmpty()) {
                    mealLogItemDao.insertItems(remappedItems)
                }
            }
            Log.d(TAG, "✓ ${mealLogsWithItems.size} meal logs restored (with items)")

            // 4. Daily Nutrition Summaries
            val summaries = firestoreSyncRepo.fetchDailyNutritionSummaries(uid)
            for (summary in summaries) {
                dailyNutritionSummaryDao.upsertSummary(summary)
            }
            Log.d(TAG, "✓ ${summaries.size} nutrition summaries restored")

            // 5. Pantry Items
            val pantryItems = firestoreSyncRepo.fetchPantryItems(uid)
            for (itemName in pantryItems) {
                pantryDao.insertItem(PantryItem(ingredientName = itemName))
            }
            Log.d(TAG, "✓ ${pantryItems.size} pantry items restored")

            // 6. Planned Meals
            val plannedMeals = firestoreSyncRepo.fetchPlannedMeals(uid)
            for (meal in plannedMeals) {
                mealPlanDao.insertMeal(meal)
            }
            Log.d(TAG, "✓ ${plannedMeals.size} planned meals restored")

            Log.d(TAG, "═══ Full restore complete for ${profile.name} ═══")
            RestoreResult.Success(profile.name)

        } catch (e: Exception) {
            Log.e(TAG, "Cloud restore failed for $uid", e)
            RestoreResult.Failed(e.message ?: "Unknown error")
        }
    }
}
