package com.calorieko.app.data.remote.api

import android.util.Log
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.UserDao
import kotlinx.coroutines.tasks.await

/**
 * Result of a sync-to-backend operation.
 */
sealed class ApiSyncResult {
    data class Success(val message: String, val lastSyncTimestamp: Long?) : ApiSyncResult()
    data class Error(val message: String) : ApiSyncResult()
}

/**
 * Orchestrates the full data sync from local Room database to the Laravel backend.
 *
 * Flow:
 * 1. Read all user data from Room DAOs
 * 2. Map Room entities → [SyncFullPayload] DTOs
 * 3. POST to `/api/sync/full` via Retrofit
 * 4. Return typed [ApiSyncResult]
 *
 * This class does NOT handle network checks — the caller is responsible
 * for verifying connectivity before invoking [syncToBackend].
 */
class ApiSyncManager(
    private val apiService: CalorieKoApiService,
    private val userDao: UserDao,
    private val activityLogDao: ActivityLogDao,
    private val mealLogDao: MealLogDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao
) {
    companion object {
        private const val TAG = "ApiSyncManager"
    }

    /**
     * Compiles all local data for the given user and pushes it to the backend.
     *
     * Must be called from a coroutine on [kotlinx.coroutines.Dispatchers.IO].
     */
    suspend fun syncToBackend(uid: String): ApiSyncResult {
        return try {
            Log.d(TAG, "Starting full sync to backend for UID: $uid")

            // ── 1. Fetch local data from Room ──
            val profile = userDao.getUser(uid)
            val activityLogs = activityLogDao.getAllLogsForUser(uid)
            val mealLogsWithItems = mealLogDao.getAllMealLogsWithItems(uid)
            val nutritionSummaries = dailyNutritionSummaryDao.getAllSummariesForUser(uid)

            Log.d(TAG, "Local data: profile=${profile != null}, " +
                    "activities=${activityLogs.size}, meals=${mealLogsWithItems.size}, " +
                    "summaries=${nutritionSummaries.size}")

            // ── 2. Build the sync payload ──
            val syncProfile = profile?.let {
                SyncProfile(
                    name = it.name,
                    email = it.email,
                    age = it.age,
                    weight = it.weight,
                    height = it.height,
                    sex = it.sex,
                    activityLevel = it.activityLevel,
                    goal = it.goal,
                    streak = it.streak,
                    level = it.level
                )
            }

            val syncMeals = mealLogsWithItems.map { mealWithItems ->
                SyncMeal(
                    uid = uid,
                    mealType = mealWithItems.mealLog.mealType,
                    timestamp = mealWithItems.mealLog.timestamp,
                    notes = mealWithItems.mealLog.notes,
                    items = mealWithItems.items.map { item ->
                        SyncMealItem(
                            foodId = item.foodId,
                            dishName = item.dishName,
                            weightGrams = item.weightGrams,
                            calories = item.calories,
                            protein = item.protein,
                            carbs = item.carbs,
                            fat = item.fat,
                            fiber = item.fiber,
                            sugar = item.sugar,
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
            }

            val syncActivities = activityLogs.map { log ->
                SyncActivity(
                    uid = uid,
                    type = log.type,
                    name = log.name,
                    timeString = log.timeString,
                    weightOrDuration = log.weightOrDuration,
                    calories = log.calories,
                    protein = log.protein,
                    carbs = log.carbs,
                    fats = log.fats,
                    sodium = log.sodium,
                    timestamp = log.timestamp,
                    distanceKm = log.distanceKm,
                    pace = log.pace,
                    movingTimeSeconds = log.movingTimeSeconds,
                    mapType = log.mapType,
                    notes = log.notes,
                    activityTag = log.activityTag
                )
            }

            val syncSummaries = nutritionSummaries.map { summary ->
                SyncNutritionSummary(
                    uid = uid,
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
            }

            val payload = SyncFullPayload(
                uid = uid,
                lastSyncTimestamp = System.currentTimeMillis() / 1000,
                profile = syncProfile,
                meals = syncMeals,
                activities = syncActivities,
                nutritionSummaries = syncSummaries
            )

            // ── 3. Authenticate and Send to backend ──
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                ?: throw Exception("User not logged into Firebase")
            
            // Suspend naturally instead of blocking in an OkHttp interceptor
            val token = user.getIdToken(true).await().token
                ?: throw Exception("Failed to retrieve Firebase ID token")

            Log.d(TAG, "Sending payload to /api/sync/full...")
            val response = apiService.syncFull("Bearer $token", payload)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d(TAG, "═══ Sync to backend SUCCEEDED: ${body.message} ═══")
                    ApiSyncResult.Success(
                        message = body.message,
                        lastSyncTimestamp = body.lastSuccessfulSync
                    )
                } else {
                    val errorMsg = body?.message ?: "Unknown server error"
                    Log.e(TAG, "Sync response indicates failure: $errorMsg")
                    ApiSyncResult.Error(errorMsg)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "Sync HTTP error ${response.code()}: $errorBody")
                ApiSyncResult.Error("Server error (${response.code()}): ${response.message()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync to backend failed", e)
            ApiSyncResult.Error(e.message ?: "Unknown error")
        }
    }
}
