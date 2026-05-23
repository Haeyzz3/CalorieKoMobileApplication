package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.WeightLogEntity

@Dao
interface WeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightLog(log: WeightLogEntity)

    @Query(
        """
        SELECT * FROM weight_log_table
        WHERE uid = :uid
          AND date_epoch_day >= :startEpochDay
          AND date_epoch_day <= :endEpochDay
        ORDER BY date_epoch_day ASC
        """
    )
    suspend fun getWeightLogsForRange(
        uid: String,
        startEpochDay: Long,
        endEpochDay: Long
    ): List<WeightLogEntity>

    @Query(
        """
        SELECT * FROM weight_log_table
        WHERE uid = :uid
          AND date_epoch_day <= :epochDay
        ORDER BY date_epoch_day DESC, timestamp DESC
        LIMIT 1
        """
    )
    suspend fun getLatestOnOrBefore(uid: String, epochDay: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_log_table WHERE uid = :uid ORDER BY date_epoch_day ASC, timestamp ASC")
    suspend fun getAllWeightLogsForUser(uid: String): List<WeightLogEntity>

    @Query(
        """
        SELECT * FROM weight_log_table
        WHERE uid = :uid
          AND updated_at > :sinceTimestamp
        ORDER BY date_epoch_day ASC
        """
    )
    suspend fun getWeightLogsModifiedSince(
        uid: String,
        sinceTimestamp: Long
    ): List<WeightLogEntity>

    @Query("SELECT * FROM weight_log_table WHERE uid = :uid AND sync_status = 0 ORDER BY date_epoch_day ASC")
    suspend fun getUnsyncedWeightLogs(uid: String): List<WeightLogEntity>

    @Query("UPDATE weight_log_table SET sync_status = 1 WHERE uid = :uid AND timestamp IN (:timestamps)")
    suspend fun markAsSynced(uid: String, timestamps: List<Long>)

    @Query("DELETE FROM weight_log_table")
    suspend fun deleteAll()
}
