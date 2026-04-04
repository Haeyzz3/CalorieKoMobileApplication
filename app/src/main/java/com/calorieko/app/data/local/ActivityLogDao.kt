package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calorieko.app.data.model.ActivityLogEntity

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insertLog(log: ActivityLogEntity): Long

    // Fetch all logs for a specific user within a specific time range, ordered from latest to oldest
    @Query("SELECT * FROM activity_log_table WHERE uid = :uid AND timestamp >= :startOfDay ORDER BY timestamp DESC")
    suspend fun getLogsForToday(uid: String, startOfDay: Long): List<ActivityLogEntity>

    @Query("SELECT * FROM activity_log_table WHERE uid = :uid AND timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    suspend fun getLogsForRange(uid: String, startTime: Long, endTime: Long): List<ActivityLogEntity>

    /** Fetch only workout entries for a user from a given start-of-day timestamp. */
    @Query("SELECT * FROM activity_log_table WHERE uid = :uid AND type = 'workout' AND timestamp >= :startOfDay ORDER BY timestamp DESC")
    suspend fun getWorkoutsForToday(uid: String, startOfDay: Long): List<ActivityLogEntity>

    @Query("SELECT * FROM activity_log_table WHERE id = :id")
    suspend fun getLogById(id: Int): ActivityLogEntity?

    @Query("SELECT COUNT(*) FROM activity_log_table WHERE uid = :uid AND type = 'workout'")
    suspend fun getTotalWorkoutsCount(uid: String): Int

    @Query("SELECT COUNT(*) FROM activity_log_table WHERE uid = :uid AND photoUri IS NOT NULL AND photoUri != ''")
    suspend fun getWorkoutsWithPhotoCount(uid: String): Int

    /** Fetch all activity logs for a user (for cloud sync). */
    @Query("SELECT * FROM activity_log_table WHERE uid = :uid ORDER BY timestamp ASC")
    suspend fun getAllLogsForUser(uid: String): List<ActivityLogEntity>

    // ═══ DELTA SYNC QUERIES ═══

    /** Fetch only activity logs modified after the given timestamp (for delta sync payloads). */
    @Query("SELECT * FROM activity_log_table WHERE uid = :uid AND updated_at > :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getLogsModifiedSince(uid: String, sinceTimestamp: Long): List<ActivityLogEntity>
}