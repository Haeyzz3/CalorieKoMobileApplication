package com.calorieko.app.data.remote.api

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calorieko.app.BuildConfig
import com.calorieko.app.CalorieKoApplication

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
            Log.e(TAG, "No UID provided; aborting API sync.")
            return Result.failure()
        }

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
                weightLogDao = db.weightLogDao(),
                context = applicationContext
            )

            when (val result = syncManager.syncToBackend(uid)) {
                is ApiSyncResult.Success -> {
                    Log.d(TAG, "Laravel delta sync success: ${result.message}")
                    val foodSyncManager = FoodCatalogSyncManager(applicationContext, apiService)
                    foodSyncManager.pullFoodCatalog()
                    Result.success()
                }
                is ApiSyncResult.Error -> {
                    Log.w(TAG, "Laravel sync failed: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker exception; retrying.", e)
            Result.retry()
        }
    }
}
