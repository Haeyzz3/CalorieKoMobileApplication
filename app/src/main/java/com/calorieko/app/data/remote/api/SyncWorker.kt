package com.calorieko.app.data.remote.api

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calorieko.app.BuildConfig
import com.calorieko.app.CalorieKoApplication
import com.calorieko.app.data.local.FoodDatabaseCallback
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * WorkManager [CoroutineWorker] that performs offline-first background sync.
 *
 * ── Offline-First Sync Pipeline ──
 * This worker runs ONLY when the device has network (constraint: NetworkType.CONNECTED).
 * It performs the following steps:
 *
 * 1. **Read un-synced activity logs** from Room (sync_status = 0)
 * 2. **Push to Firestore** via [FirestoreSyncRepository.syncActivityLogsBatch]
 * 3. **Push to Laravel** via [ApiSyncManager.syncToBackend] (delta sync)
 * 4. **Mark records as synced** in Room (sync_status = 1)
 * 5. **Pull food catalog** from admin server (server → mobile, non-fatal)
 *
 * If any step fails, the worker returns [Result.retry] so WorkManager applies
 * exponential backoff (30s → 60s → 120s → ... capped at 5h).
 *
 * ── When Is This Triggered? ──
 * [AutoSyncManager.triggerSync] enqueues a unique OneTimeWorkRequest after every
 * Room write. WorkManager coalesces duplicates via REPLACE policy, acting as
 * a natural debounce. When the device is offline, the request stays queued and
 * fires automatically when connectivity returns.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val KEY_UID = "sync_worker_uid"
    }

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID)
        if (uid.isNullOrEmpty()) {
            Log.e(TAG, "No UID provided — aborting sync.")
            return Result.failure()
        }

        Log.d(TAG, "Offline-first sync triggered for UID: $uid")

        return try {
            val app = applicationContext as CalorieKoApplication
            val db = app.database

            // ── Step 1: Read un-synced activity logs from Room ──
            val activityLogDao = db.activityLogDao()
            val unsyncedLogs = activityLogDao.getUnsyncedLogs(uid)

            if (unsyncedLogs.isEmpty()) {
                Log.d(TAG, "No un-synced activity logs found — running delta sync only.")
            } else {
                Log.d(TAG, "Found ${unsyncedLogs.size} un-synced activity logs to push.")

                // ── Step 2a: Push un-synced activity logs to Firestore ──
                try {
                    val firestoreRepo = FirestoreSyncRepository()
                    firestoreRepo.syncActivityLogsBatch(uid, unsyncedLogs)
                    Log.d(TAG, "Firestore batch sync complete for ${unsyncedLogs.size} logs.")
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore sync failed (non-fatal, continuing to Laravel): ${e.message}")
                    // Continue — Firestore is fire-and-forget, don't block Laravel sync
                }
            }

            // ── Step 2b: Push un-synced meal logs to Firestore ──
            val mealLogDao = db.mealLogDao()
            val unsyncedMeals = mealLogDao.getUnsyncedMealLogs(uid)

            if (unsyncedMeals.isEmpty()) {
                Log.d(TAG, "No un-synced meal logs found.")
            } else {
                Log.d(TAG, "Found ${unsyncedMeals.size} un-synced meal logs to push.")
                try {
                    val firestoreRepo = FirestoreSyncRepository()
                    for (mealWithItems in unsyncedMeals) {
                        firestoreRepo.syncMealLog(uid, mealWithItems.mealLog, mealWithItems.items)
                    }
                    // Also sync daily nutrition summaries for affected dates
                    val summaryDao = db.dailyNutritionSummaryDao()
                    val affectedDates = unsyncedMeals.map { meal ->
                        java.time.Instant.ofEpochMilli(meal.mealLog.timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toEpochDay()
                    }.distinct()
                    for (dateEpochDay in affectedDates) {
                        val summary = summaryDao.getSummaryForDate(uid, dateEpochDay)
                        if (summary != null) {
                            firestoreRepo.syncDailyNutritionSummary(uid, summary)
                        }
                    }
                    // Mark meal logs as synced
                    val syncedIds = unsyncedMeals.map { it.mealLog.mealLogId }
                    mealLogDao.markMealLogsAsSynced(syncedIds)
                    Log.d(TAG, "Marked ${syncedIds.size} meal logs as synced (status=1).")
                } catch (e: Exception) {
                    Log.w(TAG, "Meal log Firestore sync failed (non-fatal): ${e.message}")
                }
            }

            // ── Step 2c: Full-state sync for Pantry items ──
            //    Read current Room state → clear Firestore → re-push.
            //    Handles all offline mutations (adds, deletes, clears) in one shot.
            val pantryDao = db.pantryDao()
            try {
                val firestoreRepo = FirestoreSyncRepository()
                val currentPantryItems = pantryDao.getAllItemsList()
                firestoreRepo.clearPantryItems(uid)
                if (currentPantryItems.isNotEmpty()) {
                    firestoreRepo.syncPantryItemsBatch(uid, currentPantryItems)
                }
                Log.d(TAG, "Pantry full-state sync complete: ${currentPantryItems.size} items.")
            } catch (e: Exception) {
                Log.w(TAG, "Pantry full-state sync failed (non-fatal): ${e.message}")
            }

            // ── Step 2d: Full-state sync for Planned Meals ──
            val mealPlanDao = db.mealPlanDao()
            try {
                val firestoreRepo = FirestoreSyncRepository()
                val currentMeals = mealPlanDao.getAllPlannedMeals()
                firestoreRepo.clearAllPlannedMeals(uid)
                if (currentMeals.isNotEmpty()) {
                    firestoreRepo.syncPlannedMealsBatch(uid, currentMeals)
                }
                Log.d(TAG, "Meal plan full-state sync complete: ${currentMeals.size} meals.")
            } catch (e: Exception) {
                Log.w(TAG, "Meal plan full-state sync failed (non-fatal): ${e.message}")
            }

            // ── Step 2e: Sync user profile to Firestore ──
            try {
                val firestoreRepo = FirestoreSyncRepository()
                val userProfile = db.userDao().getUser(uid)
                if (userProfile != null) {
                    firestoreRepo.syncProfile(uid, userProfile)
                    Log.d(TAG, "User profile synced to Firestore.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Profile Firestore sync failed (non-fatal): ${e.message}")
            }

            // ── Step 3: Push to Laravel backend (delta sync includes ALL modified data) ──
            val weightLogDao = db.weightLogDao()
            val unsyncedWeightLogs = weightLogDao.getUnsyncedWeightLogs(uid)
            if (unsyncedWeightLogs.isNotEmpty()) {
                try {
                    val firestoreRepo = FirestoreSyncRepository()
                    firestoreRepo.syncWeightLogsBatch(uid, unsyncedWeightLogs)
                    weightLogDao.markAsSynced(uid, unsyncedWeightLogs.map { it.dateEpochDay })
                    Log.d(TAG, "Weight log Firestore sync complete for ${unsyncedWeightLogs.size} rows.")
                } catch (e: Exception) {
                    Log.w(TAG, "Weight log Firestore sync failed (non-fatal): ${e.message}")
                }
            }

            val apiService = RetrofitClient.getApiService(BuildConfig.API_BASE_URL)
            val syncManager = ApiSyncManager(
                apiService = apiService,
                userDao = db.userDao(),
                activityLogDao = activityLogDao,
                mealLogDao = db.mealLogDao(),
                dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
                weightLogDao = db.weightLogDao(),
                context = applicationContext
            )

            when (val result = syncManager.syncToBackend(uid)) {
                is ApiSyncResult.Success -> {
                    Log.d(TAG, "Laravel delta sync SUCCESS: ${result.message}")

                    // ── Step 4: Mark activity logs as synced in Room ──
                    if (unsyncedLogs.isNotEmpty()) {
                        val syncedIds = unsyncedLogs.map { it.id }
                        activityLogDao.markAsSynced(syncedIds)
                        Log.d(TAG, "Marked ${syncedIds.size} activity logs as synced (status=1).")
                    }

                    // ── Step 5: Pull food catalog from admin server ──
                    // Non-fatal — if the pull fails, the app continues using
                    // CSV-seeded or previously-synced food data.
                    val foodSyncManager = FoodCatalogSyncManager(applicationContext, apiService)
                    foodSyncManager.pullFoodCatalog()

                    Result.success()
                }
                is ApiSyncResult.Error -> {
                    Log.w(TAG, "Laravel sync FAILED (will retry): ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker exception (will retry)", e)
            Result.retry()
        }
    }
}
