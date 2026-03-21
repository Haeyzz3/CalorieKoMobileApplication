package com.calorieko.app.data.remote

import android.util.Log
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogWithItems
import com.calorieko.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository that pushes locally-stored Room data up to the CalorieKo
 * Laravel API.  Each `sync*` method is a suspend function designed to
 * be called from a ViewModel or WorkManager worker.
 *
 * All network calls run on [Dispatchers.IO].
 *
 * Usage (from a ViewModel):
 * ```
 *   viewModelScope.launch {
 *       val result = syncRepository.syncAll(uid)
 *       if (result.isSuccess) { /* show success */ }
 *   }
 * ```
 */
class SyncRepository(
    private val userDao: UserDao,
    private val activityLogDao: ActivityLogDao,
    private val mealLogDao: MealLogDao,
    private val mealLogItemDao: MealLogItemDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val api: CalorieKoApi = RetrofitClient.api
) {

    companion object {
        private const val TAG = "SyncRepository"
    }

    // ────────────────────────────────────────────────────────
    //  Full sync — pushes everything for the current user
    // ────────────────────────────────────────────────────────

    /**
     * Performs a full sync of all data for the given [uid].
     * Returns [Result.success] if all syncs succeed, or [Result.failure] with the first error.
     */
    suspend fun syncAll(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            syncProfile(uid)
            syncActivityLogs(uid)
            syncMealLogs(uid)
            syncNutritionSummaries(uid)
            Log.i(TAG, "Full sync completed for user $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed for user $uid", e)
            Result.failure(e)
        }
    }

    // ────────────────────────────────────────────────────────
    //  Individual sync methods
    // ────────────────────────────────────────────────────────

    /**
     * Sync the user's profile to the backend.
     */
    suspend fun syncProfile(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = userDao.getUserProfile(uid)
                ?: return@withContext Result.failure(Exception("No local profile found for $uid"))

            val response = api.syncProfile(profile)
            if (response.isSuccessful) {
                Log.d(TAG, "Profile synced for $uid")
                Result.success(Unit)
            } else {
                val error = "Profile sync failed: ${response.code()} ${response.errorBody()?.string()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync today's activity logs (meals + workouts) to the backend.
     * Uses batch endpoint for efficiency.
     */
    suspend fun syncActivityLogs(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Sync all logs from the last 24 hours
            val startOfDay = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val logs = activityLogDao.getLogsForToday(uid, startOfDay)

            if (logs.isEmpty()) {
                Log.d(TAG, "No activity logs to sync")
                return@withContext Result.success(Unit)
            }

            val response = api.syncActivityLogBatch(ActivityLogBatchRequest(entries = logs))
            if (response.isSuccessful) {
                Log.d(TAG, "Synced ${logs.size} activity logs")
                Result.success(Unit)
            } else {
                val error = "Activity log sync failed: ${response.code()} ${response.errorBody()?.string()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Activity log sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync a single activity log entry to the backend.
     * Call this right after inserting a new log locally.
     */
    suspend fun syncSingleActivityLog(log: ActivityLogEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.syncActivityLog(log)
            if (response.isSuccessful) {
                Log.d(TAG, "Single activity log synced: ${log.name}")
                Result.success(Unit)
            } else {
                val error = "Single activity log sync failed: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Single activity log sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync today's meal logs (with child items) to the backend.
     */
    suspend fun syncMealLogs(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val endOfDay = today.plusDays(1).atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            val mealLogsWithItems = mealLogDao.getMealLogsWithItemsByDate(uid, startOfDay, endOfDay)

            if (mealLogsWithItems.isEmpty()) {
                Log.d(TAG, "No meal logs to sync")
                return@withContext Result.success(Unit)
            }

            var failCount = 0
            for (mealLogWithItems in mealLogsWithItems) {
                val result = syncSingleMealLog(mealLogWithItems)
                if (result.isFailure) failCount++
            }

            if (failCount > 0) {
                Result.failure(Exception("$failCount meal log(s) failed to sync"))
            } else {
                Log.d(TAG, "Synced ${mealLogsWithItems.size} meal logs")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Meal log sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync a single meal log with its items to the backend.
     * Call this right after logging a new meal locally.
     */
    suspend fun syncSingleMealLog(mealLogWithItems: MealLogWithItems): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = MealLogSyncRequest(
                uid = mealLogWithItems.mealLog.uid,
                mealType = mealLogWithItems.mealLog.mealType,
                timestamp = mealLogWithItems.mealLog.timestamp,
                notes = mealLogWithItems.mealLog.notes,
                items = mealLogWithItems.items.map { item ->
                    MealLogItemDto(
                        foodId = item.foodId,
                        dishName = item.dishName,
                        weightGrams = item.weightGrams,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fiber = item.fiber,
                        sugar = item.sugar,
                        fat = item.fat,
                        saturatedFat = item.saturatedFat,
                        polyunsaturatedFat = item.polyunsaturatedFat,
                        monounsaturatedFat = item.monounsaturatedFat,
                        transFat = item.transFat,
                        cholesterol = item.cholesterol,
                        sodium = item.sodium,
                        potassium = item.potassium,
                        vitaminA = item.vitaminA,
                        vitaminC = item.vitaminC,
                        calcium = item.calcium,
                        iron = item.iron
                    )
                }
            )

            val response = api.syncMealLog(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Meal log synced: ${request.mealType}")
                Result.success(Unit)
            } else {
                val error = "Meal log sync failed: ${response.code()} ${response.errorBody()?.string()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Meal log sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync today's daily nutrition summary to the backend.
     */
    suspend fun syncNutritionSummaries(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val todayEpochDay = LocalDate.now().toEpochDay()
            val summary = dailyNutritionSummaryDao.getSummaryForDate(uid, todayEpochDay)
                ?: return@withContext Result.success(Unit) // No summary yet today

            val result = syncSingleNutritionSummary(summary)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Nutrition summary sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync a single daily nutrition summary to the backend.
     * Call this after upserting a summary locally.
     */
    suspend fun syncSingleNutritionSummary(summary: DailyNutritionSummaryEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = DailyNutritionSyncRequest(
                uid = summary.uid,
                dateEpochDay = summary.dateEpochDay,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFiber = summary.totalFiber,
                totalSugar = summary.totalSugar,
                totalFat = summary.totalFat,
                totalSaturatedFat = summary.totalSaturatedFat,
                totalPolyunsaturatedFat = summary.totalPolyunsaturatedFat,
                totalMonounsaturatedFat = summary.totalMonounsaturatedFat,
                totalTransFat = summary.totalTransFat,
                totalCholesterol = summary.totalCholesterol,
                totalSodium = summary.totalSodium,
                totalPotassium = summary.totalPotassium,
                totalVitaminA = summary.totalVitaminA,
                totalVitaminC = summary.totalVitaminC,
                totalCalcium = summary.totalCalcium,
                totalIron = summary.totalIron,
                breakfastCalories = summary.breakfastCalories,
                lunchCalories = summary.lunchCalories,
                dinnerCalories = summary.dinnerCalories,
                snacksCalories = summary.snacksCalories
            )

            val response = api.syncNutritionSummary(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Nutrition summary synced for epoch day ${summary.dateEpochDay}")
                Result.success(Unit)
            } else {
                val error = "Nutrition summary sync failed: ${response.code()} ${response.errorBody()?.string()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nutrition summary sync error", e)
            Result.failure(e)
        }
    }
}
