package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.local.WeightLogDao
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogWithItems
import com.calorieko.app.data.model.WeightLogEntity
import com.calorieko.app.data.repository.ActivityRepository
import com.calorieko.app.data.repository.NutritionalValuesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

/**
 * ViewModel for ProgressScreen.
 *
 * Manages:
 * - User weight for the weight trend chart
 * - Activity logs (workouts) for burned calorie data
 * - Daily nutrition summaries for intake calorie, sodium, and nutrient charts
 * - Meal logs with items for dietary insights (top foods)
 * - View mode toggle (7-day / 30-day / 90-day) with automatic data re-fetch
 */
class ProgressViewModel(
    private val auth: FirebaseAuth,
    private val activityRepository: ActivityRepository,
    private val nutritionSummaryDao: DailyNutritionSummaryDao,
    private val mealLogDao: MealLogDao,
    private val weightLogDao: WeightLogDao,
    private val nutritionalValuesRepo: NutritionalValuesRepository,
    private val userDao: UserDao
) : ViewModel() {

    // ── State ──

    private val _weeklyLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val weeklyLogs: StateFlow<List<ActivityLogEntity>> = _weeklyLogs.asStateFlow()

    /** Daily nutrition summaries for the selected range (for intake calories, sodium, etc.) */
    private val _nutritionSummaries = MutableStateFlow<List<DailyNutritionSummaryEntity>>(emptyList())
    val nutritionSummaries: StateFlow<List<DailyNutritionSummaryEntity>> = _nutritionSummaries.asStateFlow()

    /** Meal logs with items for dietary insights (top foods by frequency/sodium). */
    private val _mealLogsWithItems = MutableStateFlow<List<MealLogWithItems>>(emptyList())
    val mealLogsWithItems: StateFlow<List<MealLogWithItems>> = _mealLogsWithItems.asStateFlow()

    private val _weightLogs = MutableStateFlow<List<WeightLogEntity>>(emptyList())
    val weightLogs: StateFlow<List<WeightLogEntity>> = _weightLogs.asStateFlow()

    private val _userWeight = MutableStateFlow(74.0)
    val userWeight: StateFlow<Double> = _userWeight.asStateFlow()

    private val _viewMode = MutableStateFlow("7_days")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _selectedMetric = MutableStateFlow("Calorie Balance")
    val selectedMetric: StateFlow<String> = _selectedMetric.asStateFlow()

    /** User's personalized daily calorie target (from NutritionalValuesRepository). */
    private val _targetCalories = MutableStateFlow(2000)
    val targetCalories: StateFlow<Int> = _targetCalories.asStateFlow()

    private val _dataLoaded = MutableStateFlow(false)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    init {
        loadData()
    }

    /**
     * Updates the view mode and re-fetches data for the new time range.
     */
    fun setViewMode(mode: String) {
        if (_viewMode.value != mode) {
            _viewMode.value = mode
            loadData()
        }
    }

    fun setMetric(metric: String) {
        _selectedMetric.value = metric
    }

    /**
     * Fetches user weight, activity logs, nutrition summaries, and meal logs
     * for the currently selected time range.
     */
    fun loadData() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _dataLoaded.value = true
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 1. Fetch user weight and target calories
                _userWeight.value = activityRepository.getUserWeight(uid)
                val profile = userDao.getUser(uid)
                if (profile != null) {
                    _targetCalories.value = nutritionalValuesRepo.getTargetsForUser(profile).targetCalories
                }

                // 2. Compute time range
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val endTime = calendar.timeInMillis + 24 * 60 * 60 * 1000L

                val daysBack = when (_viewMode.value) {
                    "7_days" -> 7
                    "30_days" -> 30
                    "90_days" -> 90
                    else -> 7
                }
                calendar.add(Calendar.DAY_OF_YEAR, -(daysBack - 1))
                val startTime = calendar.timeInMillis

                // 3. Fetch workout logs from activity_log_table
                _weeklyLogs.value = activityRepository.getLogsForRange(uid, startTime, endTime)

                // 4. Fetch nutrition summaries from daily_nutrition_summary_table
                val today = LocalDate.now()
                val startDate = today.minusDays((daysBack - 1).toLong())
                _nutritionSummaries.value = nutritionSummaryDao.getSummariesForRange(
                    uid,
                    startDate.toEpochDay(),
                    today.toEpochDay()
                )

                // 5. Fetch meal logs with items for dietary insights
                _mealLogsWithItems.value = mealLogDao.getMealLogsWithItemsByDate(
                    uid, startTime, endTime
                )

                // 6. Fetch historical weight measurements. The chart should
                // plot actual dated logs, not duplicate the current profile
                // weight across the selected range.
                val endEpochDay = today.toEpochDay()
                val weights = weightLogDao.getAllWeightLogsForUser(uid)
                    .filter { it.weightKg > 0.0 }
                    .sortedWith(compareBy<WeightLogEntity> { it.dateEpochDay }.thenBy { it.timestamp })
                _weightLogs.value = weights.ifEmpty {
                    listOf(
                        WeightLogEntity(
                            uid = uid,
                            dateEpochDay = endEpochDay,
                            weightKg = _userWeight.value,
                            timestamp = System.currentTimeMillis(),
                            updatedAt = 0L,
                            syncStatus = 1
                        )
                    )
                }
            }
            _dataLoaded.value = true
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            activityRepository: ActivityRepository,
            nutritionSummaryDao: DailyNutritionSummaryDao,
            mealLogDao: MealLogDao,
            weightLogDao: WeightLogDao,
            nutritionalValuesRepo: NutritionalValuesRepository,
            userDao: UserDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
                    return ProgressViewModel(auth, activityRepository, nutritionSummaryDao, mealLogDao, weightLogDao, nutritionalValuesRepo, userDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
