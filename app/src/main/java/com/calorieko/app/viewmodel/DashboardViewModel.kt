package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.model.ActivityDetails
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.ActivityLogEntry
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogWithItems
import com.calorieko.app.data.repository.DashboardRepository
import com.calorieko.app.data.repository.NutritionalTarget
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val auth: FirebaseAuth,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    // ── User Info ──

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _localPhotoUrl = MutableStateFlow("")
    val localPhotoUrl: StateFlow<String> = _localPhotoUrl.asStateFlow()

    val firebaseProfileImageUrl = auth.currentUser?.photoUrl

    // ── Targets ──

    private val _targetCalories = MutableStateFlow(2000)
    val targetCalories: StateFlow<Int> = _targetCalories.asStateFlow()

    private val _targetBurned = MutableStateFlow(500)
    val targetBurned: StateFlow<Int> = _targetBurned.asStateFlow()

    private val _targetSodium = MutableStateFlow(2300)
    val targetSodium: StateFlow<Int> = _targetSodium.asStateFlow()

    private val _targetProtein = MutableStateFlow(150)
    val targetProtein: StateFlow<Int> = _targetProtein.asStateFlow()

    private val _targetCarbs = MutableStateFlow(200)
    val targetCarbs: StateFlow<Int> = _targetCarbs.asStateFlow()

    private val _targetFats = MutableStateFlow(65)
    val targetFats: StateFlow<Int> = _targetFats.asStateFlow()

    // ── Data State ──

    private val _nutritionSummary = MutableStateFlow<DailyNutritionSummaryEntity?>(null)
    val nutritionSummary: StateFlow<DailyNutritionSummaryEntity?> = _nutritionSummary.asStateFlow()

    private val _todayMealLogs = MutableStateFlow<List<MealLogWithItems>>(emptyList())
    val todayMealLogs: StateFlow<List<MealLogWithItems>> = _todayMealLogs.asStateFlow()

    private val _todayWorkoutLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val todayWorkoutLogs: StateFlow<List<ActivityLogEntity>> = _todayWorkoutLogs.asStateFlow()

    // ── Computed Activity Feed ──

    private val _activityFeed = MutableStateFlow<List<ActivityLogEntry>>(emptyList())
    val activityFeed: StateFlow<List<ActivityLogEntry>> = _activityFeed.asStateFlow()

    // ── Derived Values ──

    val currentCalories: Int get() = _nutritionSummary.value?.totalCalories?.toInt() ?: 0
    val caloriesBurned: Int get() = _todayWorkoutLogs.value.sumOf { it.calories }
    val currentSodium: Int get() = _nutritionSummary.value?.totalSodium?.toInt() ?: 0
    val currentProtein: Int get() = _nutritionSummary.value?.totalProtein?.toInt() ?: 0
    val currentCarbs: Int get() = _nutritionSummary.value?.totalCarbs?.toInt() ?: 0
    val currentFats: Int get() = _nutritionSummary.value?.totalFat?.toInt() ?: 0

    init {
        loadDashboardData()
    }

    /**
     * Loads all dashboard data: user profile, targets, nutrition summary,
     * meal logs, workout logs, and builds the unified activity feed.
     */
    fun loadDashboardData() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // A. Fetch user profile for target calculations
                val profile = dashboardRepository.getUserProfile(uid)
                if (profile != null) {
                    val targets = dashboardRepository.getTargetsForUser(profile)
                    _targetCalories.value = targets.targetCalories
                    _targetProtein.value = targets.targetProtein
                    _targetCarbs.value = targets.targetCarbs
                    _targetFats.value = targets.targetFats
                    _targetSodium.value = targets.targetSodium
                    _targetBurned.value = 500
                    _localPhotoUrl.value = profile.photoUrl

                    // Use the profile name if Firebase displayName is missing
                    val fbName = auth.currentUser?.displayName
                        ?.split(" ")?.firstOrNull()
                    _userName.value = fbName
                        ?: profile.name.split(" ").firstOrNull()
                        ?: "User"
                }

                // B. Fetch today's nutrition summary
                _nutritionSummary.value = dashboardRepository.getTodayNutritionSummary(uid)

                // C. Fetch today's meal logs (for the activity feed)
                val mealLogs = dashboardRepository.getTodayMealLogs(uid)
                _todayMealLogs.value = mealLogs

                // D. Fetch today's workout logs
                val workoutLogs = dashboardRepository.getTodayWorkoutLogs(uid)
                _todayWorkoutLogs.value = workoutLogs

                // E. Build the unified activity feed
                _activityFeed.value = buildActivityFeed(mealLogs, workoutLogs)
            }
        }
    }

    /**
     * Reloads all data. Call this when returning from logMeal/logWorkout
     * to pick up newly persisted entries.
     */
    fun refreshData() {
        loadDashboardData()
    }

    // ── Private Helpers ──

    private fun buildActivityFeed(
        mealLogs: List<MealLogWithItems>,
        workoutLogs: List<ActivityLogEntity>
    ): List<ActivityLogEntry> {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        // Convert meal logs to feed entries
        val mealEntries = mealLogs.map { mealWithItems ->
            val meal = mealWithItems.mealLog
            val items = mealWithItems.items
            val totalCal = items.sumOf { it.calories.toDouble() }.toInt()
            val totalSod = items.sumOf { it.sodium.toDouble() }.toInt()
            val totalProt = items.sumOf { it.protein.toDouble() }.toInt()
            val totalCarb = items.sumOf { it.carbs.toDouble() }.toInt()
            val totalFat = items.sumOf { it.fat.toDouble() }.toInt()
            val totalWeight = items.sumOf { it.weightGrams.toDouble() }.toInt()
            val dishNames = items.joinToString(", ") { it.dishName }

            ActivityLogEntry(
                id = meal.mealLogId.toString(),
                type = "meal",
                time = timeFormat.format(Date(meal.timestamp)),
                name = dishNames,
                details = ActivityDetails(
                    weight = "${totalWeight}g",
                    calories = totalCal,
                    sodium = totalSod,
                    protein = totalProt,
                    carbs = totalCarb,
                    fats = totalFat
                ),
                timestamp = meal.timestamp
            )
        }

        // Convert workout logs to feed entries
        val workoutEntries = workoutLogs.map { entity ->
            ActivityLogEntry(
                id = entity.id.toString(),
                type = entity.type,
                time = entity.timeString,
                name = entity.name,
                details = ActivityDetails(
                    calories = entity.calories,
                    protein = entity.protein,
                    carbs = entity.carbs,
                    fats = entity.fats,
                    duration = entity.weightOrDuration
                ),
                timestamp = entity.timestamp
            )
        }

        // Merge and sort by timestamp (newest first)
        return (mealEntries + workoutEntries).sortedByDescending { it.timestamp }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            dashboardRepository: DashboardRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                    return DashboardViewModel(auth, dashboardRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
