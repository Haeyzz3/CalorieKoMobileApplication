package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLogEntity): Long

    @Query("DELETE FROM meal_log_table WHERE meal_log_id = :mealLogId")
    suspend fun deleteMealLog(mealLogId: Long)

    /** Fetch all meal logs for a user within a timestamp range, newest first (one-shot). */
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

    /** Fetch all meal logs with items for a user on a given day (one-shot). */
    @Transaction
    @Query(
        """
        SELECT * FROM meal_log_table
        WHERE uid = :uid AND timestamp >= :startTimestamp AND timestamp < :endTimestamp
        ORDER BY timestamp DESC
        """
    )
    suspend fun getMealLogsWithItemsByDate(uid: String, startTimestamp: Long, endTimestamp: Long): List<MealLogWithItems>

    /** Observe all meal logs with items for a user on a given day (reactive Flow). */
    @Transaction
    @Query(
        """
        SELECT * FROM meal_log_table
        WHERE uid = :uid AND timestamp >= :startTimestamp AND timestamp < :endTimestamp
        ORDER BY timestamp DESC
        """
    )
    fun observeMealLogsWithItemsByDate(uid: String, startTimestamp: Long, endTimestamp: Long): Flow<List<MealLogWithItems>>

    @Query("SELECT COUNT(*) FROM meal_log_table WHERE uid = :uid")
    suspend fun getTotalMealsCount(uid: String): Int

    /** Fetch all meal logs with items for a user (for cloud sync). */
    @Transaction
    @Query("SELECT * FROM meal_log_table WHERE uid = :uid ORDER BY timestamp ASC")
    suspend fun getAllMealLogsWithItems(uid: String): List<MealLogWithItems>

    // ═══ DELTA SYNC QUERIES ═══

    /** Fetch only meal logs (with items) modified after the given timestamp (for delta sync payloads). */
    @Transaction
    @Query("SELECT * FROM meal_log_table WHERE uid = :uid AND updated_at > :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getMealLogsWithItemsModifiedSince(uid: String, sinceTimestamp: Long): List<MealLogWithItems>

    /** Deletes all meal logs. Used during logout to clear user data only. */
    @Query("DELETE FROM meal_log_table")
    suspend fun deleteAll()
}
