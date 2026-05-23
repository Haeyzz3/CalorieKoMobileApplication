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

        val pending = outboxDao.getPending(uid, BATCH_LIMIT)
        if (pending.isEmpty()) return Result.success()

        for (operation in pending) {
            try {
                executor.execute(operation)
                outboxDao.deleteById(operation.id)
            } catch (e: Exception) {
                outboxDao.recordFailure(
                    id = operation.id,
                    error = e.message ?: e::class.java.simpleName,
                    now = System.currentTimeMillis()
                )
                Log.w(TAG, "Firestore operation ${operation.id} failed; retrying later.", e)
                return Result.retry()
            }
        }

        if (outboxDao.pendingCount(uid) > 0) {
            FirestoreAutoSyncManager.triggerSync(applicationContext, uid, immediate = true)
        }

        return Result.success()
    }
}
