package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.MealLogItemEntity

@Dao
interface MealLogItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MealLogItemEntity>): List<Long>

    @Query("SELECT * FROM meal_log_item_table WHERE meal_log_id = :mealLogId")
    suspend fun getItemsForMealLog(mealLogId: Long): List<MealLogItemEntity>

    @Query("DELETE FROM meal_log_item_table WHERE meal_log_id = :mealLogId")
    suspend fun deleteItemsForMealLog(mealLogId: Long)
}
