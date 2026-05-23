package com.calorieko.app.data.remote.firestore

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object FirestoreAutoSyncManager {
    private const val TAG = "FirestoreAutoSync"
    private const val TAG_PREFIX = "firestore_sync_"

    fun uniqueWorkName(uid: String): String = "calorieko_firestore_sync_$uid"

    fun triggerSync(context: Context, uid: String, immediate: Boolean = false) {
        if (uid.isBlank()) {
            Log.w(TAG, "triggerSync called with empty UID.")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val builder = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(FirestoreSyncWorker.KEY_UID to uid))
            .addTag(TAG_PREFIX + uid)

        if (!immediate) {
            builder.setInitialDelay(3, TimeUnit.SECONDS)
        }

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                uniqueWorkName(uid),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                builder.build()
            )
    }

    fun cancel(context: Context, uid: String) {
        if (uid.isBlank()) return
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(uniqueWorkName(uid))
    }
}
