package com.calorieko.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.remote.ImageUtils
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreEntityType
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer

class ActivityRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {
    private val activityLogDao = db.activityLogDao()
    private val userDao = db.userDao()
    private val outboxDao = db.firestoreOutboxDao()

    suspend fun insertWorkoutLog(uid: String, log: ActivityLogEntity): Long {
        val persisted = log.copy(
            uid = uid,
            syncStatus = 0,
            updatedAt = System.currentTimeMillis()
        )

        val newId = db.withTransaction {
            val id = activityLogDao.insertLog(persisted)
            outboxDao.insert(
                FirestorePayloadSerializer.upsert(
                    uid = uid,
                    entityType = FirestoreEntityType.ACTIVITY_LOG,
                    entityKey = persisted.remoteId,
                    remotePath = FirestorePayloadSerializer.activityPath(uid, persisted.remoteId),
                    payload = FirestorePayloadSerializer.activityPayload(persisted)
                )
            )
            id
        }

        triggerSync(uid)
        return newId
    }

    suspend fun deleteWorkoutLog(uid: String, activityId: Int): Boolean {
        var deleted = false
        db.withTransaction {
            val log = activityLogDao.getLogById(activityId) ?: return@withTransaction
            outboxDao.insert(
                FirestorePayloadSerializer.deleteDocument(
                    uid = uid,
                    entityType = FirestoreEntityType.ACTIVITY_LOG,
                    entityKey = log.remoteId,
                    remotePath = FirestorePayloadSerializer.activityPath(uid, log.remoteId)
                )
            )
            activityLogDao.deleteLogById(activityId)
            deleted = true
        }

        if (deleted) triggerSync(uid)
        return deleted
    }

    suspend fun getLogById(id: Int): ActivityLogEntity? {
        return activityLogDao.getLogById(id)
    }

    suspend fun getLogsForRange(uid: String, startTime: Long, endTime: Long): List<ActivityLogEntity> {
        return activityLogDao.getLogsForRange(uid, startTime, endTime)
    }

    suspend fun getTotalWorkoutsCount(uid: String): Int {
        return activityLogDao.getTotalWorkoutsCount(uid)
    }

    suspend fun getLogTimestampsForUser(uid: String): List<Long> {
        return activityLogDao.getLogTimestampsForUser(uid)
    }

    suspend fun getUserWeight(uid: String): Double {
        return userDao.getUser(uid)?.weight ?: 70.0
    }

    suspend fun getUserName(uid: String): String {
        val profile = userDao.getUserProfile(uid)
        return if (profile != null && profile.name.isNotEmpty()) profile.name else "CalorieKo athlete"
    }

    suspend fun compressAndSavePhoto(context: Context, photoUriStr: String): String {
        val uri = Uri.parse(photoUriStr)
        val encodedStr = ImageUtils.compressAndEncode(context, uri, maxDimension = 800, quality = 70)
        return encodedStr ?: photoUriStr
    }

    private fun triggerSync(uid: String) {
        if (uid.isBlank()) return
        FirestoreAutoSyncManager.triggerSync(appContext, uid)
        AutoSyncManager.triggerSync(appContext, uid)
    }
}
