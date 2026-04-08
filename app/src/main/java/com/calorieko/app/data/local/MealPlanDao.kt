package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.PlannedMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: PlannedMealEntity)

    /** Removes a single dish from a specific slot. */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot AND dish_label = :dishLabel")
    suspend fun removeDish(dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String)

    /** Removes all dishes from a specific slot (clears the entire meal). */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot")
    suspend fun clearSlot(dayIndex: Int, weekStartDate: String, mealSlot: String)

    @Query("SELECT * FROM PLANNED_MEALS_TABLE WHERE week_start_date = :weekStartDate ORDER BY day_index ASC")
    fun getMealsForWeek(weekStartDate: String): Flow<List<PlannedMealEntity>>

    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE week_start_date = :weekStartDate")
    suspend fun clearWeek(weekStartDate: String)

    /** Removes all meals for a specific day within a week. */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE day_index = :dayIndex AND week_start_date = :weekStartDate")
    suspend fun clearDay(dayIndex: Int, weekStartDate: String)

    /** One-shot fetch of all planned meals (for cloud sync). */
    @Query("SELECT * FROM PLANNED_MEALS_TABLE ORDER BY week_start_date ASC, day_index ASC")
    suspend fun getAllPlannedMeals(): List<PlannedMealEntity>
}
