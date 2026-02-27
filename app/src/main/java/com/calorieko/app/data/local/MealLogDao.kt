package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogWithItems

@Dao
interface MealLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLogEntity): Long

    @Query("DELETE FROM meal_log_table WHERE meal_log_id = :mealLogId")
    suspend fun deleteMealLog(mealLogId: Long)

    /** Fetch all meal logs for a user within a timestamp range, newest first. */
    @Query(
        """
        SELECT * FROM meal_log_table
        WHERE uid = :uid AND timestamp >= :startTimestamp AND timestamp < :endTimestamp
        ORDER BY timestamp DESC
        """
    )
    suspend fun getMealLogsByDate(uid: String, startTimestamp: Long, endTimestamp: Long): List<MealLogEntity>

    /** Fetch a single meal log with all its child items (dishes). */
    @Transaction
    @Query("SELECT * FROM meal_log_table WHERE meal_log_id = :mealLogId")
    suspend fun getMealLogWithItems(mealLogId: Long): MealLogWithItems?

    /** Fetch all meal logs with items for a user on a given day. */
    @Transaction
    @Query(
        """
        SELECT * FROM meal_log_table
        WHERE uid = :uid AND timestamp >= :startTimestamp AND timestamp < :endTimestamp
        ORDER BY timestamp DESC
        """
    )
    suspend fun getMealLogsWithItemsByDate(uid: String, startTimestamp: Long, endTimestamp: Long): List<MealLogWithItems>
}
