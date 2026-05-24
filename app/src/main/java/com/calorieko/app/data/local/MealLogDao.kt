package com.calorieko.app.data.local

import androidx.room.ColumnInfo
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

    @Query("SELECT timestamp FROM meal_log_table WHERE uid = :uid ORDER BY timestamp DESC")
    suspend fun getMealLogTimestampsForUser(uid: String): List<Long>

    /** Fetch all meal logs with items for a user (for cloud sync). */
    @Transaction
    @Query("SELECT * FROM meal_log_table WHERE uid = :uid ORDER BY timestamp ASC")
    suspend fun getAllMealLogsWithItems(uid: String): List<MealLogWithItems>

    // ═══ DELTA SYNC QUERIES ═══

    /** Fetch only meal logs (with items) modified after the given timestamp (for delta sync payloads). */
    @Transaction
    @Query("SELECT * FROM meal_log_table WHERE uid = :uid AND updated_at > :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getMealLogsWithItemsModifiedSince(uid: String, sinceTimestamp: Long): List<MealLogWithItems>

    // ═══ OFFLINE-FIRST SYNC QUERIES ═══

    /** Fetch all meal logs (with items) that have NOT been synced to Firestore yet (sync_status = 0). */
    @Transaction
    @Query("SELECT * FROM meal_log_table WHERE uid = :uid AND sync_status = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMealLogs(uid: String): List<MealLogWithItems>

    /** Mark a batch of meal log IDs as synced (sync_status = 1). */
    @Query("UPDATE meal_log_table SET sync_status = 1 WHERE meal_log_id IN (:ids)")
    suspend fun markMealLogsAsSynced(ids: List<Long>)

    /** Deletes all meal logs. Used during logout to clear user data only. */
    @Query("DELETE FROM meal_log_table")
    suspend fun deleteAll()

    // ═══ MEAL PLAN COMPLETION STATUS ═══

    /**
     * Observe logged dish references (meal_type + dish_name) for a user within a timestamp range.
     * Lightweight query for meal plan completion status derivation — avoids loading full nutrient columns.
     */
    @Query("""
        SELECT m.meal_type, i.dish_name
        FROM meal_log_table m
        INNER JOIN meal_log_item_table i ON m.meal_log_id = i.meal_log_id
        WHERE m.uid = :uid AND m.timestamp >= :startTimestamp AND m.timestamp < :endTimestamp
    """)
    fun observeLoggedDishNames(uid: String, startTimestamp: Long, endTimestamp: Long): Flow<List<LoggedDishRef>>
}

/**
 * Lightweight projection of a logged dish for meal plan status cross-referencing.
 * Avoids loading full MealLogWithItems (with all nutrient columns) when only
 * meal_type + dish_name are needed for completion status derivation.
 */
data class LoggedDishRef(
    @ColumnInfo(name = "meal_type") val mealType: String,
    @ColumnInfo(name = "dish_name") val dishName: String
)
