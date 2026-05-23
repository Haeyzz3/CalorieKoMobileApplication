package com.calorieko.app.data.remote.firestore

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calorieko.app.CalorieKoApplication

class FirestoreSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "FirestoreSyncWorker"
        const val KEY_UID = "firestore_sync_uid"
        private const val BATCH_LIMIT = 100
    }

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID)
        if (uid.isNullOrBlank()) {
            Log.e(TAG, "No UID provided.")
            return Result.failure()
        }

        val app = applicationContext as CalorieKoApplication
        val database = app.database
        val outboxDao = database.firestoreOutboxDao()
        val executor = FirestoreOperationExecutor(database)

        val processor = FirestoreOutboxProcessor(
            outboxDao = outboxDao,
            executor = executor,
            batchLimit = BATCH_LIMIT
        )

        return when (val result = processor.process(uid)) {
            FirestoreOutboxProcessResult.Retry -> {
                Log.w(TAG, "Firestore outbox processing failed; retrying later.")
                Result.retry()
            }
            is FirestoreOutboxProcessResult.Success -> {
                if (result.shouldEnqueueFollowUp) {
                    FirestoreAutoSyncManager.triggerSync(applicationContext, uid, immediate = true)
                }
                Result.success()
            }
        }
    }
}
