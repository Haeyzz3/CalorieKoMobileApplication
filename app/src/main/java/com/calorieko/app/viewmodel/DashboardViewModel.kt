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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DashboardViewModel — fully reactive via Room Flow observation.
 *
 * ── How it works ──
 * Instead of one-shot `suspend` fetches that require manual `refreshData()` calls,
 * all dashboard data (nutrition summary, meal logs, workout logs) is observed via
 * Room `Flow<T>` queries. Room automatically re-emits whenever the underlying tables
 * change, so the UI updates instantly when a meal or workout is logged — no navigation
 * callback or manual refresh needed.
 *
 * User profile & targets are still loaded once (they rarely change mid-session).
 */
class DashboardViewModel(
    private val auth: FirebaseAuth,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val uid: String? = auth.currentUser?.uid

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

    // ── Reactive Data State (Flow-based — auto-updates on Room changes) ──

    /** Today's nutrition summary, observed reactively from Room. */
    val nutritionSummary: StateFlow<DailyNutritionSummaryEntity?> =
        if (uid != null) {
            dashboardRepository.observeTodayNutritionSummary(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        } else {
            MutableStateFlow(null)
        }

    /** Today's meal logs with items, observed reactively from Room. */
    val todayMealLogs: StateFlow<List<MealLogWithItems>> =
        if (uid != null) {
            dashboardRepository.observeTodayMealLogs(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    /** Today's workout logs, observed reactively from Room. */
    val todayWorkoutLogs: StateFlow<List<ActivityLogEntity>> =
        if (uid != null) {
            dashboardRepository.observeTodayWorkoutLogs(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    // ── Computed Activity Feed (derived from the reactive meal + workout Flows) ──

    val activityFeed: StateFlow<List<ActivityLogEntry>> =
        combine(todayMealLogs, todayWorkoutLogs) { meals, workouts ->
            buildActivityFeed(meals, workouts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUserProfileAndTargets()
    }

    /**
     * Loads user profile and computes nutritional targets.
     * This is a one-shot load — profile data rarely changes mid-session.
     * The reactive Flows handle all dashboard data (nutrition, meals, workouts).
     */
    private fun loadUserProfileAndTargets() {
        val currentUid = uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val profile = dashboardRepository.getUserProfile(currentUid)
                if (profile != null) {
                    val targets = dashboardRepository.getTargetsForUser(profile)
                    _targetCalories.value = targets.targetCalories
                    _targetProtein.value = targets.targetProtein
                    _targetCarbs.value = targets.targetCarbs
                    _targetFats.value = targets.targetFats
                    _targetSodium.value = targets.targetSodium
                    _targetBurned.value = 500
                    _localPhotoUrl.value = profile.photoUrl

                    val fbName = auth.currentUser?.displayName
                        ?.split(" ")?.firstOrNull()
                    _userName.value = fbName
                        ?: profile.name.split(" ").firstOrNull()
                        ?: "User"
                }
            }
        }
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
