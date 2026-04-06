package com.calorieko.app.data.repository

import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogWithItems
import com.calorieko.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Calendar

/**
 * Read-focused repository that provides all data needed by
 * DashboardScreen and NutritionDetailsScreen.
 *
 * This repository does NOT perform writes — write operations
 * are handled by MealRepository (meals) and ActivityRepository (workouts).
 *
 * Provides both:
 * - **Suspend functions** for one-shot reads (used by NutritionDetailsScreen, etc.)
 * - **Flow functions** for reactive observation (used by DashboardViewModel)
 */
class DashboardRepository(
    private val userDao: UserDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val mealLogDao: MealLogDao,
    private val activityLogDao: ActivityLogDao,
    private val nutritionalValuesRepo: NutritionalValuesRepository
) {

    // ── User Profile ──

    /** Fetch the user's profile from the local Room database. */
    suspend fun getUserProfile(uid: String): UserProfile? {
        return userDao.getUser(uid)
    }

    /** Compute nutritional targets (calories, macros, micros) from a user profile. */
    fun getTargetsForUser(profile: UserProfile): NutritionalTarget {
        return nutritionalValuesRepo.getTargetsForUser(profile)
    }

    // ── Daily Nutrition Summaries ──

    /** Fetch today's nutrition summary (one-shot). */
    suspend fun getTodayNutritionSummary(uid: String): DailyNutritionSummaryEntity? {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return dailyNutritionSummaryDao.getSummaryForDate(uid, todayEpochDay)
    }

    /** Observe today's nutrition summary (reactive Flow — emits on every Room change). */
    fun observeTodayNutritionSummary(uid: String): Flow<DailyNutritionSummaryEntity?> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return dailyNutritionSummaryDao.observeSummaryForDate(uid, todayEpochDay)
    }

    /** Fetch the nutrition summary for a specific date (epoch day). */
    suspend fun getNutritionSummaryForDate(uid: String, dateEpochDay: Long): DailyNutritionSummaryEntity? {
        return dailyNutritionSummaryDao.getSummaryForDate(uid, dateEpochDay)
    }

    /** Fetch nutrition summaries for an inclusive date range (for weekly views). */
    suspend fun getNutritionSummariesForRange(
        uid: String,
        startEpochDay: Long,
        endEpochDay: Long
    ): List<DailyNutritionSummaryEntity> {
        return dailyNutritionSummaryDao.getSummariesForRange(uid, startEpochDay, endEpochDay)
    }

    // ── Today's Activity Logs (for Dashboard Feed) ──

    /** Fetch today's meal logs with their child items (one-shot). */
    suspend fun getTodayMealLogs(uid: String): List<MealLogWithItems> {
        val (startOfDay, endOfDay) = getTodayTimestampRange()
        return mealLogDao.getMealLogsWithItemsByDate(uid, startOfDay, endOfDay)
    }

    /** Observe today's meal logs with their child items (reactive Flow). */
    fun observeTodayMealLogs(uid: String): Flow<List<MealLogWithItems>> {
        val (startOfDay, endOfDay) = getTodayTimestampRange()
        return mealLogDao.observeMealLogsWithItemsByDate(uid, startOfDay, endOfDay)
    }

    /** Fetch today's workout logs (one-shot). */
    suspend fun getTodayWorkoutLogs(uid: String): List<ActivityLogEntity> {
        val (startOfDay, _) = getTodayTimestampRange()
        return activityLogDao.getWorkoutsForToday(uid, startOfDay)
    }

    /** Observe today's workout logs (reactive Flow). */
    fun observeTodayWorkoutLogs(uid: String): Flow<List<ActivityLogEntity>> {
        val (startOfDay, _) = getTodayTimestampRange()
        return activityLogDao.observeWorkoutsForToday(uid, startOfDay)
    }

    // ── Helpers ──

    /**
     * Computes the start and end timestamps for the current day.
     * Returns a Pair of (startOfDayMillis, endOfDayMillis).
     */
    private fun getTodayTimestampRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 86_400_000L
        return Pair(startOfDay, endOfDay)
    }
}
