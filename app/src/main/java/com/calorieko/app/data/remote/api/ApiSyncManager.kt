package com.calorieko.app.data.remote.api

import android.content.Context
import android.content.SharedPreferences
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
    data class Success(
        val message: String,
        val lastSyncTimestamp: Long?,
        val conflicts: List<SyncConflict>? = null
    ) : ApiSyncResult()

    data class Error(val message: String) : ApiSyncResult()
}

/**
 * Orchestrates **delta** data sync from local Room database to the Laravel backend.
 *
 * ── Delta Sync Strategy ──
 * Instead of transmitting the entire Room database on every sync, this manager:
 * 1. Retrieves `lastSuccessfulSyncTimestamp` from SharedPreferences (default: 0).
 * 2. Queries each DAO for records where `updated_at > lastSuccessfulSyncTimestamp`.
 * 3. Maps only the modified Room entities → [SyncFullPayload] DTOs (with `updatedAt`).
 * 4. POSTs the delta payload to `/api/sync/full` via Retrofit.
 * 5. On success, persists the new `lastSuccessfulSyncTimestamp` to SharedPreferences.
 *
 * ── Conflict Resolution ──
 * The payload includes `updated_at` on every entity. The Laravel backend implements
 * "Last Write Wins" — if the server record is newer, the mobile update is rejected
 * for that entity. The response includes a `conflicts` list detailing rejections.
 *
 * This class does NOT handle network checks — the caller is responsible
 * for verifying connectivity before invoking [syncToBackend].
 */
class ApiSyncManager(
    private val apiService: CalorieKoApiService,
    private val userDao: UserDao,
    private val activityLogDao: ActivityLogDao,
    private val mealLogDao: MealLogDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val context: Context
) {
    companion object {
        private const val TAG = "ApiSyncManager"
        private const val PREFS_NAME = "calorieko_sync_prefs"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_successful_sync_timestamp"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the epoch-millis timestamp of the last successful sync.
     * Returns 0 on first run, which causes ALL records to be included (initial full sync).
     */
    private fun getLastSuccessfulSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    /**
     * Persists the timestamp of a successful sync so subsequent syncs
     * only transmit records modified after this point.
     */
    private fun setLastSuccessfulSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    /**
     * Compiles only **modified** local data for the given user and pushes it to the backend.
     *
     * On first sync (lastSuccessfulSyncTimestamp == 0), this effectively becomes a full sync
     * since every record's `updated_at` is greater than 0.
     *
     * Must be called from a coroutine on [kotlinx.coroutines.Dispatchers.IO].
     */
    suspend fun syncToBackend(uid: String): ApiSyncResult {
        return try {
            val lastSync = getLastSuccessfulSyncTimestamp()
            Log.d(TAG, "Starting DELTA sync for UID: $uid (lastSync=$lastSync)")

            // ── 1. Fetch ONLY modified data from Room (delta queries) ──
            val profile = userDao.getUser(uid)
            val activityLogs = activityLogDao.getLogsModifiedSince(uid, lastSync)
            val mealLogsWithItems = mealLogDao.getMealLogsWithItemsModifiedSince(uid, lastSync)
            val nutritionSummaries = dailyNutritionSummaryDao.getSummariesModifiedSince(uid, lastSync)

            Log.d(TAG, "Delta payload: profile=${profile != null}, " +
                    "activities=${activityLogs.size}, meals=${mealLogsWithItems.size}, " +
                    "summaries=${nutritionSummaries.size}")

            // ── 2. Build the delta sync payload (with updatedAt on every entity) ──

            // Profile is always sent if it exists and was modified since last sync.
            // If profile.updatedAt <= lastSync, we still send it as null (unchanged).
            val syncProfile = profile?.takeIf { it.updatedAt > lastSync }?.let {
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
                    level = it.level,
                    updatedAt = it.updatedAt
                )
            }

            val syncMeals = mealLogsWithItems.map { mealWithItems ->
                SyncMeal(
                    uid = uid,
                    mealType = mealWithItems.mealLog.mealType,
                    timestamp = mealWithItems.mealLog.timestamp,
                    notes = mealWithItems.mealLog.notes,
                    updatedAt = mealWithItems.mealLog.updatedAt,
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
                            iron = item.iron,
                            updatedAt = item.updatedAt
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
                    activityTag = log.activityTag,
                    updatedAt = log.updatedAt
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
                    snacksCalories = summary.snacksCalories,
                    updatedAt = summary.updatedAt
                )
            }

            // Check if there's actually anything to sync
            var hasData = syncProfile != null ||
                    syncMeals.isNotEmpty() ||
                    syncActivities.isNotEmpty() ||
                    syncSummaries.isNotEmpty()

            // ── FULL SYNC FALLBACK ──
            // If delta is empty but user explicitly tapped "Sync Data",
            // re-query ALL records and send a full payload. This handles:
            //   - Server database was reset/wiped
            //   - Previous sync silently failed on the server
            //   - Delta timestamp drifted ahead of actual data
            var finalProfile = syncProfile
            var finalMeals = syncMeals
            var finalActivities = syncActivities
            var finalSummaries = syncSummaries

            if (!hasData) {
                Log.d(TAG, "Delta is empty — falling back to FULL sync of all records.")

                finalProfile = profile?.let {
                    SyncProfile(
                        name = it.name, email = it.email, age = it.age,
                        weight = it.weight, height = it.height, sex = it.sex,
                        activityLevel = it.activityLevel, goal = it.goal,
                        streak = it.streak, level = it.level, updatedAt = it.updatedAt
                    )
                }

                val allMeals = mealLogDao.getAllMealLogsWithItems(uid)
                finalMeals = allMeals.map { mealWithItems ->
                    SyncMeal(
                        uid = uid,
                        mealType = mealWithItems.mealLog.mealType,
                        timestamp = mealWithItems.mealLog.timestamp,
                        notes = mealWithItems.mealLog.notes,
                        updatedAt = mealWithItems.mealLog.updatedAt,
                        items = mealWithItems.items.map { item ->
                            SyncMealItem(
                                foodId = item.foodId, dishName = item.dishName,
                                weightGrams = item.weightGrams, calories = item.calories,
                                protein = item.protein, carbs = item.carbs, fat = item.fat,
                                fiber = item.fiber, sugar = item.sugar,
                                saturatedFat = item.saturatedFat,
                                polyunsaturatedFat = item.polyunsaturatedFat,
                                monounsaturatedFat = item.monounsaturatedFat,
                                transFat = item.transFat, cholesterol = item.cholesterol,
                                sodium = item.sodium, potassium = item.potassium,
                                vitaminA = item.vitaminA, vitaminC = item.vitaminC,
                                calcium = item.calcium, iron = item.iron,
                                updatedAt = item.updatedAt
                            )
                        }
                    )
                }

                val allActivities = activityLogDao.getAllLogsForUser(uid)
                finalActivities = allActivities.map { log ->
                    SyncActivity(
                        uid = uid, type = log.type, name = log.name,
                        timeString = log.timeString, weightOrDuration = log.weightOrDuration,
                        calories = log.calories, protein = log.protein,
                        carbs = log.carbs, fats = log.fats, sodium = log.sodium,
                        timestamp = log.timestamp, distanceKm = log.distanceKm,
                        pace = log.pace, movingTimeSeconds = log.movingTimeSeconds,
                        mapType = log.mapType, notes = log.notes,
                        activityTag = log.activityTag, updatedAt = log.updatedAt
                    )
                }

                val allSummaries = dailyNutritionSummaryDao.getAllSummariesForUser(uid)
                finalSummaries = allSummaries.map { summary ->
                    SyncNutritionSummary(
                        uid = uid, dateEpochDay = summary.dateEpochDay,
                        totalCalories = summary.totalCalories, totalProtein = summary.totalProtein,
                        totalCarbs = summary.totalCarbs, totalFiber = summary.totalFiber,
                        totalSugar = summary.totalSugar, totalFat = summary.totalFat,
                        totalSaturatedFat = summary.totalSaturatedFat,
                        totalPolyunsaturatedFat = summary.totalPolyunsaturatedFat,
                        totalMonounsaturatedFat = summary.totalMonounsaturatedFat,
                        totalTransFat = summary.totalTransFat,
                        totalCholesterol = summary.totalCholesterol,
                        totalSodium = summary.totalSodium, totalPotassium = summary.totalPotassium,
                        totalVitaminA = summary.totalVitaminA, totalVitaminC = summary.totalVitaminC,
                        totalCalcium = summary.totalCalcium, totalIron = summary.totalIron,
                        breakfastCalories = summary.breakfastCalories,
                        lunchCalories = summary.lunchCalories,
                        dinnerCalories = summary.dinnerCalories,
                        snacksCalories = summary.snacksCalories,
                        updatedAt = summary.updatedAt
                    )
                }

                hasData = finalProfile != null ||
                        finalMeals.isNotEmpty() ||
                        finalActivities.isNotEmpty() ||
                        finalSummaries.isNotEmpty()

                if (!hasData) {
                    Log.d(TAG, "No data at all in local database — nothing to transmit.")
                    return ApiSyncResult.Success(
                        message = "No local data to sync.",
                        lastSyncTimestamp = lastSync
                    )
                }
            }

            val payload = SyncFullPayload(
                uid = uid,
                lastSyncTimestamp = lastSync,
                profile = finalProfile,
                meals = finalMeals,
                activities = finalActivities,
                nutritionSummaries = finalSummaries
            )

            // ── 3. Authenticate and Send to backend ──
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                ?: throw Exception("User not logged into Firebase")

            // Suspend naturally instead of blocking in an OkHttp interceptor
            val token = user.getIdToken(true).await().token
                ?: throw Exception("Failed to retrieve Firebase ID token")

            Log.d(TAG, "Sending DELTA payload to /api/sync/full...")
            val response = apiService.syncFull("Bearer $token", payload)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    // ── 4. Persist the new sync timestamp ──
                    val serverTimestamp = body.lastSuccessfulSync ?: System.currentTimeMillis()
                    setLastSuccessfulSyncTimestamp(serverTimestamp)

                    val conflictCount = body.conflicts?.size ?: 0
                    Log.d(TAG, "═══ Delta sync SUCCEEDED: ${body.message} " +
                            "(conflicts=$conflictCount) ═══")

                    ApiSyncResult.Success(
                        message = body.message,
                        lastSyncTimestamp = serverTimestamp,
                        conflicts = body.conflicts
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

    /**
     * Resets the last sync timestamp, forcing the next sync to transmit ALL records.
     * Use this after a data wipe or account reset.
     */

    fun resetSyncTimestamp() {
        prefs.edit().remove(KEY_LAST_SYNC_TIMESTAMP).apply()
        Log.d(TAG, "Sync timestamp reset — next sync will be a full sync.")
    }
}
