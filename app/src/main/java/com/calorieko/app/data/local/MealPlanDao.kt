package com.calorieko.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.calorieko.app.data.model.PlannedMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: PlannedMealEntity)

    /** Inserts a dish only if it would not exceed the per-slot dish limit. */
    @Transaction
    suspend fun insertMealIfSlotHasCapacity(meal: PlannedMealEntity, limit: Int): Boolean {
        val slotMeals = getMealsForSlotOneShot(meal.uid, meal.dayIndex, meal.weekStartDate, meal.mealSlot)
        val replacesExistingDish = slotMeals.any { it.dishLabel == meal.dishLabel }
        if (!replacesExistingDish && slotMeals.size >= limit) {
            return false
        }
        insertMeal(meal)
        return true
    }

    /** Removes a single dish from a specific slot. */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot AND dish_label = :dishLabel")
    suspend fun removeDish(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String)

    /** Removes one clearable dish from a specific slot. Preserves logged & skipped rows. */
    @Query("""
        DELETE FROM PLANNED_MEALS_TABLE
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate
          AND meal_slot = :mealSlot AND dish_label = :dishLabel
          AND status IN ('planned', 'missed')
    """)
    suspend fun removeDishClearableOnly(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String)

    /** Removes all dishes from a specific slot (clears the entire meal). */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot")
    suspend fun clearSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String)

    /** Removes only clearable dishes from a specific slot. Preserves logged & skipped rows. */
    @Query("""
        DELETE FROM PLANNED_MEALS_TABLE
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate
          AND meal_slot = :mealSlot AND status IN ('planned', 'missed')
    """)
    suspend fun clearSlotClearableOnly(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String)

    @Query("SELECT * FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND week_start_date = :weekStartDate ORDER BY day_index ASC")
    fun getMealsForWeek(uid: String, weekStartDate: String): Flow<List<PlannedMealEntity>>

    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND week_start_date = :weekStartDate")
    suspend fun clearWeek(uid: String, weekStartDate: String)

    /** Removes meals for selected days within a week. */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND week_start_date = :weekStartDate AND day_index IN (:dayIndices)")
    suspend fun clearWeekDays(uid: String, weekStartDate: String, dayIndices: List<Int>)

    /** Removes all meals for a specific day within a week. */
    @Query("DELETE FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate")
    suspend fun clearDay(uid: String, dayIndex: Int, weekStartDate: String)

    /** One-shot fetch of all planned meals (for cloud sync). Unscoped — SyncWorker scopes via Firestore path. */
    @Query("SELECT * FROM PLANNED_MEALS_TABLE ORDER BY week_start_date ASC, day_index ASC")
    suspend fun getAllPlannedMeals(): List<PlannedMealEntity>

    /** Deletes all planned meals. Used during logout to clear user data only. Intentionally unscoped. */
    @Query("DELETE FROM PLANNED_MEALS_TABLE")
    suspend fun deleteAll()

    /** One-shot fetch of meals for a specific day and week (for notifications, status updates). */
    @Query("SELECT * FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate")
    suspend fun getMealsForDayOneShot(uid: String, dayIndex: Int, weekStartDate: String): List<PlannedMealEntity>

    /** One-shot fetch of meals for a specific slot. */
    @Query("SELECT * FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot")
    suspend fun getMealsForSlotOneShot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String): List<PlannedMealEntity>

    /** Returns the number of logged/skipped rows in a specific slot. */
    @Query("""
        SELECT COUNT(*) FROM PLANNED_MEALS_TABLE
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate
          AND meal_slot = :mealSlot AND status IN ('logged', 'skipped')
    """)
    suspend fun countProtectedMealsInSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String): Int

    /** Meal counts per week for scrubber density dots. */
    @Query("""
        SELECT week_start_date, COUNT(*) as count 
        FROM PLANNED_MEALS_TABLE 
        WHERE uid = :uid AND week_start_date IN (:weekStartDates) 
        GROUP BY week_start_date
    """)
    suspend fun getMealCountsForWeeks(uid: String, weekStartDates: List<String>): List<WeekMealCount>

    /** One-shot fetch of meals for a specific week (for copy-week). */
    @Query("SELECT * FROM PLANNED_MEALS_TABLE WHERE uid = :uid AND week_start_date = :weekStartDate")
    suspend fun getMealsForWeekOneShot(uid: String, weekStartDate: String): List<PlannedMealEntity>

    /** Batch insert meals (for copy-week). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<PlannedMealEntity>)

    /** Replaces all planned meals in a target week. */
    @Transaction
    suspend fun replaceWeek(uid: String, weekStartDate: String, meals: List<PlannedMealEntity>) {
        clearWeek(uid, weekStartDate)
        if (meals.isNotEmpty()) {
            insertMeals(meals)
        }
    }

    /** Replaces all planned meals in a target meal slot. */
    @Transaction
    suspend fun replaceSlot(
        uid: String,
        dayIndex: Int,
        weekStartDate: String,
        mealSlot: String,
        meals: List<PlannedMealEntity>
    ) {
        clearSlot(uid, dayIndex, weekStartDate, mealSlot)
        if (meals.isNotEmpty()) {
            insertMeals(meals)
        }
    }

    /** Replaces only clearable meals in a target meal slot. Preserves logged & skipped rows. */
    @Transaction
    suspend fun replaceSlotClearableOnly(
        uid: String,
        dayIndex: Int,
        weekStartDate: String,
        mealSlot: String,
        meals: List<PlannedMealEntity>
    ) {
        clearSlotClearableOnly(uid, dayIndex, weekStartDate, mealSlot)
        if (meals.isNotEmpty()) {
            insertMeals(meals)
        }
    }

    // ═══ MEAL PLAN STATUS ═══

    /** Update the status of a specific planned meal (single dish in a slot). */
    @Query("""
        UPDATE PLANNED_MEALS_TABLE 
        SET status = :status 
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate 
          AND meal_slot = :mealSlot AND dish_label = :dishLabel
    """)
    suspend fun updateMealStatus(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String, status: String)

    /** Update the status of ALL dishes in a specific slot. */
    @Query("""
        UPDATE PLANNED_MEALS_TABLE 
        SET status = :status 
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate AND meal_slot = :mealSlot
    """)
    suspend fun updateSlotStatus(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String, status: String)

    /** Marks only clearable rows in a slot as skipped. Preserves logged rows. */
    @Query("""
        UPDATE PLANNED_MEALS_TABLE
        SET status = 'skipped'
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate
          AND meal_slot = :mealSlot AND status IN ('planned', 'missed')
    """)
    suspend fun skipSlotClearableOnly(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String): Int

    /** Reverts only skipped rows in a slot back to planned. Preserves logged rows. */
    @Query("""
        UPDATE PLANNED_MEALS_TABLE
        SET status = 'planned'
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate
          AND meal_slot = :mealSlot AND status = 'skipped'
    """)
    suspend fun unskipSlotSkippedOnly(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String): Int

    // ═══ STATUS-AWARE CLEAR (preserves logged & skipped) ═══

    /** Clears only clearable meals (planned/missed) for a specific day. Preserves logged & skipped. */
    @Query("""
        DELETE FROM PLANNED_MEALS_TABLE 
        WHERE uid = :uid AND day_index = :dayIndex AND week_start_date = :weekStartDate 
        AND status IN ('planned', 'missed')
    """)
    suspend fun clearDayClearableOnly(uid: String, dayIndex: Int, weekStartDate: String)

    /** Clears only clearable meals (planned/missed) for an entire week. Preserves logged & skipped. */
    @Query("""
        DELETE FROM PLANNED_MEALS_TABLE 
        WHERE uid = :uid AND week_start_date = :weekStartDate 
        AND status IN ('planned', 'missed')
    """)
    suspend fun clearWeekClearableOnly(uid: String, weekStartDate: String)

    /** Clears only clearable meals (planned/missed) for selected days within a week. Preserves logged & skipped. */
    @Query("""
        DELETE FROM PLANNED_MEALS_TABLE 
        WHERE uid = :uid AND week_start_date = :weekStartDate AND day_index IN (:dayIndices) 
        AND status IN ('planned', 'missed')
    """)
    suspend fun clearWeekDaysClearableOnly(uid: String, weekStartDate: String, dayIndices: List<Int>)

    // ═══ UID BACKFILL ═══

    /** Stamps the user's uid on pre-migration rows that have uid = ''. Idempotent. */
    @Query("UPDATE PLANNED_MEALS_TABLE SET uid = :uid WHERE uid = ''")
    suspend fun backfillUid(uid: String)
}

/** Result class for week meal count query. */
data class WeekMealCount(
    @ColumnInfo(name = "week_start_date") val weekStartDate: String,
    @ColumnInfo(name = "count") val count: Int
)
