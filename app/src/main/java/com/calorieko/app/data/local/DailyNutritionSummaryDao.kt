package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNutritionSummaryDao {

    /** Insert or replace the daily summary (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DailyNutritionSummaryEntity): Long

    /** Fetch the summary for a specific user and date (one-shot). */
    @Query(
        """
        SELECT * FROM daily_nutrition_summary_table
        WHERE uid = :uid AND date_epoch_day = :dateEpochDay
        LIMIT 1
        """
    )
    suspend fun getSummaryForDate(uid: String, dateEpochDay: Long): DailyNutritionSummaryEntity?

    /** Observe the summary for a specific user and date (reactive Flow). */
    @Query(
        """
        SELECT * FROM daily_nutrition_summary_table
        WHERE uid = :uid AND date_epoch_day = :dateEpochDay
        LIMIT 1
        """
    )
    fun observeSummaryForDate(uid: String, dateEpochDay: Long): Flow<DailyNutritionSummaryEntity?>

    /** Fetch summaries for a date range (inclusive), for weekly views. */
    @Query(
        """
        SELECT * FROM daily_nutrition_summary_table
        WHERE uid = :uid AND date_epoch_day >= :startEpochDay AND date_epoch_day <= :endEpochDay
        ORDER BY date_epoch_day ASC
        """
    )
    suspend fun getSummariesForRange(uid: String, startEpochDay: Long, endEpochDay: Long): List<DailyNutritionSummaryEntity>

    /** Fetch all nutrition summaries for a user (for cloud sync). */
    @Query("SELECT * FROM daily_nutrition_summary_table WHERE uid = :uid ORDER BY date_epoch_day ASC")
    suspend fun getAllSummariesForUser(uid: String): List<DailyNutritionSummaryEntity>

    // ═══ DELTA SYNC QUERIES ═══

    /** Fetch only summaries modified after the given timestamp (for delta sync payloads). */
    @Query("SELECT * FROM daily_nutrition_summary_table WHERE uid = :uid AND updated_at > :sinceTimestamp ORDER BY date_epoch_day ASC")
    suspend fun getSummariesModifiedSince(uid: String, sinceTimestamp: Long): List<DailyNutritionSummaryEntity>
}
