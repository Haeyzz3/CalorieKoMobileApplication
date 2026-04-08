package com.calorieko.app.data.remote.api

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calorieko.app.BuildConfig
import com.calorieko.app.CalorieKoApplication
import com.calorieko.app.data.remote.FirestoreSyncRepository

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

                // ── Step 2: Push un-synced logs to Firestore ──
                try {
                    val firestoreRepo = FirestoreSyncRepository()
                    firestoreRepo.syncActivityLogsBatch(uid, unsyncedLogs)
                    Log.d(TAG, "Firestore batch sync complete for ${unsyncedLogs.size} logs.")
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore sync failed (non-fatal, continuing to Laravel): ${e.message}")
                    // Continue — Firestore is fire-and-forget, don't block Laravel sync
                }
            }

            // ── Step 3: Push to Laravel backend (delta sync includes ALL modified data) ──
            val apiService = RetrofitClient.getApiService(BuildConfig.API_BASE_URL)
            val syncManager = ApiSyncManager(
                apiService = apiService,
                userDao = db.userDao(),
                activityLogDao = activityLogDao,
                mealLogDao = db.mealLogDao(),
                dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
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
