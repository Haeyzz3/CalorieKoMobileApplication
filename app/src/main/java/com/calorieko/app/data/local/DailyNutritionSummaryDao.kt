package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.calorieko.app.data.model.DailyNutritionSummaryEntity

@Dao
interface DailyNutritionSummaryDao {

    /** Insert or replace the daily summary (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DailyNutritionSummaryEntity): Long

    /** Fetch the summary for a specific user and date. */
    @Query(
        """
        SELECT * FROM daily_nutrition_summary_table
        WHERE uid = :uid AND date_epoch_day = :dateEpochDay
        LIMIT 1
        """
    )
    suspend fun getSummaryForDate(uid: String, dateEpochDay: Long): DailyNutritionSummaryEntity?

    /** Fetch summaries for a date range (inclusive), for weekly views. */
    @Query(
        """
        SELECT * FROM daily_nutrition_summary_table
        WHERE uid = :uid AND date_epoch_day >= :startEpochDay AND date_epoch_day <= :endEpochDay
        ORDER BY date_epoch_day ASC
        """
    )
    suspend fun getSummariesForRange(uid: String, startEpochDay: Long, endEpochDay: Long): List<DailyNutritionSummaryEntity>
}
