package com.calorieko.app.data.repository

import android.content.Context
import android.net.Uri
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.remote.ImageUtils
import com.calorieko.app.data.remote.api.AutoSyncManager

/**
 * Repository for workout CRUD operations.
 *
 * ── Offline-First Design ──
 * The local Room database is the **single source of truth**.
 * On save, data is written to Room instantly with `sync_status = 0` (PENDING).
 * The UI receives success immediately — no network call blocks the save.
 *
 * Background synchronization:
 * After the Room insert, [AutoSyncManager.triggerSync] enqueues a WorkManager
 * job with a `NetworkType.CONNECTED` constraint. The [SyncWorker] will:
 * 1. Query all un-synced records from Room (sync_status = 0)
 * 2. Push them to Firestore + Laravel backend
 * 3. Mark them as synced (sync_status = 1) on success
 *
 * This ensures the app works fully offline and syncs when connectivity returns.
 */
class ActivityRepository(
    private val activityLogDao: ActivityLogDao,
    private val userDao: UserDao,
    private val appContext: Context
) {

    // ── Workout Write (Offline-First) ──

    /**
     * Inserts a workout log into Room **only** (instant, no network).
     *
     * The record is saved with `sync_status = 0` (PENDING), meaning it will
     * be picked up by the background [SyncWorker] when network is available.
     *
     * After the local insert, triggers [AutoSyncManager] to schedule a
     * background sync with `NetworkType.CONNECTED` constraint.
     *
     * @return The Room-generated row ID.
     */
    suspend fun insertWorkoutLog(uid: String, log: ActivityLogEntity): Long {
        // 1. Insert to Room instantly (sync_status defaults to 0 = PENDING)
        val newId = activityLogDao.insertLog(log)

        // 2. Schedule background sync (fires when network available)
        if (uid.isNotEmpty()) {
            AutoSyncManager.triggerSync(appContext, uid)
        }

        return newId
    }

    // ── Workout Read ──

    /** Fetch a single activity log by its Room ID. */
    suspend fun getLogById(id: Int): ActivityLogEntity? {
        return activityLogDao.getLogById(id)
    }

    /** Fetch activity logs for a time range (used by ProgressScreen charts). */
    suspend fun getLogsForRange(uid: String, startTime: Long, endTime: Long): List<ActivityLogEntity> {
        return activityLogDao.getLogsForRange(uid, startTime, endTime)
    }

    // ── User Data Lookups ──

    /** Fetch the user's weight for MET calorie calculations. */
    suspend fun getUserWeight(uid: String): Double {
        return userDao.getUser(uid)?.weight ?: 70.0
    }

    /** Fetch the user's display name for activity details header. */
    suspend fun getUserName(uid: String): String {
        val profile = userDao.getUserProfile(uid)
        return if (profile != null && profile.name.isNotEmpty()) profile.name else "CalorieKo athlete"
    }

    // ── Photo Processing ──

    /**
     * Compresses and Base64-encodes a photo URI for storage.
     * Falls back to the raw URI string if compression fails.
     *
     * @param context Needed for ContentResolver
     * @param photoUriStr Raw URI string from the photo picker
     * @return Encoded string or original URI string
     */
    suspend fun compressAndSavePhoto(context: Context, photoUriStr: String): String {
        val uri = Uri.parse(photoUriStr)
        val encodedStr = ImageUtils.compressAndEncode(context, uri, maxDimension = 800, quality = 70)
        return encodedStr ?: photoUriStr
    }
}
