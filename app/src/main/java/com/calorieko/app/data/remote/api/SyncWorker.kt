package com.calorieko.app.data.remote.api

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calorieko.app.BuildConfig
import com.calorieko.app.CalorieKoApplication

/**
 * WorkManager [CoroutineWorker] that performs a delta sync to the Laravel backend.
 *
 * ── When Is This Triggered? ──
 * [AutoSyncManager.triggerSync] enqueues a **unique** OneTimeWorkRequest
 * after every Room write (meal log, activity log, profile update).
 * WorkManager coalesces duplicate enqueues via `ExistingWorkPolicy.REPLACE`,
 * so rapid consecutive writes (e.g., logging 5 dishes in a meal) result in
 * a single sync request with a short debounce delay.
 *
 * ── Constraints ──
 * The work request specifies `NetworkType.CONNECTED`, so this worker
 * will never run without internet connectivity. If the device is offline,
 * the request stays queued and fires automatically when network returns.
 *
 * ── Retry ──
 * Returns [Result.retry] on transient failures so WorkManager applies
 * exponential backoff (30s, 60s, 120s, ...).
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

        Log.d(TAG, "Auto-sync triggered for UID: $uid")

        return try {
            val app = applicationContext as CalorieKoApplication
            val db = app.database

            val apiService = RetrofitClient.getApiService(BuildConfig.API_BASE_URL)
            val syncManager = ApiSyncManager(
                apiService = apiService,
                userDao = db.userDao(),
                activityLogDao = db.activityLogDao(),
                mealLogDao = db.mealLogDao(),
                dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
                context = applicationContext
            )

            when (val result = syncManager.syncToBackend(uid)) {
                is ApiSyncResult.Success -> {
                    Log.d(TAG, "Auto-sync SUCCESS: ${result.message}")
                    Result.success()
                }
                is ApiSyncResult.Error -> {
                    Log.w(TAG, "Auto-sync FAILED (will retry): ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-sync exception (will retry)", e)
            Result.retry()
        }
    }
}
