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

    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot")
    suspend fun removeMeal(dayIndex: Int, weekStartDate: String, mealSlot: String)

    @Query("SELECT * FROM PLANNED_MEALS_TABLE WHERE week_start_date = :weekStartDate ORDER BY day_index ASC")
    fun getMealsForWeek(weekStartDate: String): Flow<List<PlannedMealEntity>>

    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE week_start_date = :weekStartDate")
    suspend fun clearWeek(weekStartDate: String)

    /** One-shot fetch of all planned meals (for cloud sync). */
    @Query("SELECT * FROM PLANNED_MEALS_TABLE ORDER BY week_start_date ASC, day_index ASC")
    suspend fun getAllPlannedMeals(): List<PlannedMealEntity>
}
