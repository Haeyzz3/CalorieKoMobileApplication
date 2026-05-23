package com.calorieko.app.data.remote.api

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Schedules a background delta sync via WorkManager after every Room write.
 *
 * ── Design Decisions ──
 *
 * 1. **Unique Work with APPEND_OR_REPLACE policy**: If a sync is already
 *    running, later requests are appended instead of canceling in-flight work.
 *
 * 2. **Initial delay of 3 seconds** — Gives the user time to finish a batch of
 *    writes (log multiple dishes, save profile, etc.) before the sync fires.
 *    Short enough that the admin dashboard sees fresh data within seconds.
 *
 * 3. **Network constraint** — The worker only runs when the device has internet.
 *    When offline, the request is queued and fires on reconnect.
 *
 * 4. **Exponential backoff** — WorkManager automatically retries failed syncs
 *    with 30s base backoff, capped at 5 hours.
 *
 * ── Usage ──
 * Call [triggerSync] from any repository or ViewModel after a Room insert/update:
 * ```
 * AutoSyncManager.triggerSync(context, uid)
 * ```
 */
object AutoSyncManager {

    private const val TAG = "AutoSyncManager"
    private const val UNIQUE_WORK_NAME = "calorieko_auto_sync"

    /**
     * Enqueues a one-time background sync to the Laravel backend.
     *
     * Safe to call frequently; rapid writes append a follow-up run without
     * canceling an in-flight API sync.
     *
     * @param context  Application or Activity context (WorkManager extracts appContext)
     * @param uid      Firebase UID of the currently authenticated user
     */
    fun triggerSync(context: Context, uid: String) {
        if (uid.isEmpty()) {
            Log.w(TAG, "triggerSync called with empty UID — skipping.")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(3, TimeUnit.SECONDS)  // Debounce: batch rapid writes
            .setInputData(workDataOf(SyncWorker.KEY_UID to uid))
            .addTag("auto_sync")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncRequest
            )

        Log.d(TAG, "Auto-sync queued for UID: $uid (3s debounce)")
    }
}
