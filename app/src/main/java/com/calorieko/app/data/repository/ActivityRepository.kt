package com.calorieko.app.data.repository

import android.content.Context
import android.net.Uri
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.ImageUtils

/**
 * Repository for workout CRUD operations.
 *
 * Encapsulates:
 * - Room read/write for ActivityLogEntity
 * - Firestore sync on writes
 * - Photo compression for workout images
 * - User weight/name lookups needed by workout screens
 */
class ActivityRepository(
    private val activityLogDao: ActivityLogDao,
    private val userDao: UserDao,
    private val firestoreSyncRepo: FirestoreSyncRepository
) {

    // ── Workout Write ──

    /**
     * Inserts a workout log into Room and syncs to Firestore.
     * Returns the Room-generated row ID.
     */
    suspend fun insertWorkoutLog(uid: String, log: ActivityLogEntity): Long {
        val newId = activityLogDao.insertLog(log)
        if (uid.isNotEmpty()) {
            firestoreSyncRepo.syncActivityLog(uid, log.copy(id = newId.toInt()))
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
