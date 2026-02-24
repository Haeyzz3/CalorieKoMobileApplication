package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calorieko.app.data.model.ActivityLogEntity

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insertLog(log: ActivityLogEntity)

    // Fetch all logs for a specific user within a specific time range (e.g., today)
    @Query("SELECT * FROM activity_log_table WHERE uid = :uid AND timestamp >= :startOfDay")
    suspend fun getLogsForToday(uid: String, startOfDay: Long): List<ActivityLogEntity>
}