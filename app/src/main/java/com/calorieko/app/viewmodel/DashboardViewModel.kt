package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.ActivityDetails
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.ActivityLogEntry
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogWithItems
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.local.DishRecipeDao
import com.calorieko.app.data.repository.DashboardRepository
import com.calorieko.app.data.repository.NutritionalTarget
import com.calorieko.app.util.DurationFormatter
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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
 * User profile & targets are observed reactively so edits propagate back to the UI.
 */
class DashboardViewModel(
    private val auth: FirebaseAuth,
    private val dashboardRepository: DashboardRepository,
    private val mealPlanDao: MealPlanDao,
    private val dishRecipeDao: DishRecipeDao,
    private val appContext: android.content.Context
) : ViewModel() {

    private val uid: String? = auth.currentUser?.uid

    // ── User Info ──

    private val profileFlow: StateFlow<UserProfile?> =
        if (uid != null) {
            dashboardRepository.observeUserProfile(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        } else {
            MutableStateFlow(null)
        }

    val userName: StateFlow<String> = profileFlow
        .map { profile ->
            // Guard: displayName or profile.name might be "" (empty but not null),
            // which wouldn't fall through the ?: chain. Use takeIf to convert blanks → null.
            auth.currentUser?.displayName
                ?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: profile?.name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: "User"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val localPhotoUrl: StateFlow<String> = profileFlow
        .map { profile -> profile?.photoUrl ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val firebaseProfileImageUrl = auth.currentUser?.photoUrl

    val userProfile: StateFlow<UserProfile?> = profileFlow

    val goalTitle: StateFlow<String> = profileFlow
        .map { profile ->
            when (profile?.goal?.lowercase()?.trim()) {
                "weight_loss" -> "Weight Control"
                "gain_muscle" -> "Gain Muscle"
                else -> "General Health & Wellness"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "General Health & Wellness")

    // ── Targets ──

    val targetCalories: StateFlow<Int> = profileFlow
        .map { profile -> profile?.let { dashboardRepository.getTargetsForUser(it).targetCalories } ?: 2000 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val targetBurned: StateFlow<Int> = profileFlow
        .map { profile -> profile?.let { calculateTargetBurned(it) } ?: 500 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500)

    val targetSodium: StateFlow<Int> = profileFlow
        .map { profile -> profile?.let { dashboardRepository.getTargetsForUser(it).targetSodium } ?: 2300 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2300)

    val targetProtein: StateFlow<Int> = profileFlow
        .map { profile -> profile?.let { dashboardRepository.getTargetsForUser(it).targetProtein } ?: 150 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val targetCarbs: StateFlow<Int> = profileFlow
        .map { profile -> profile?.let { dashboardRepository.getTargetsForUser(it).targetCarbs } ?: 200 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200)

    val targetFats: StateFlow<Int> = profileFlow
        .map { profile -> profile?.let { dashboardRepository.getTargetsForUser(it).targetFats } ?: 65 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 65)

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

    // ── Pull-to-Refresh ──

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Triggers a cloud sync and briefly shows the refresh indicator.
     * Since all data is reactive via Room Flow, the UI auto-updates
     * once the sync writes new data to the local database.
     */
    fun refreshData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            withContext(Dispatchers.IO) {
                try {
                    uid?.let {
                        FirestoreAutoSyncManager.triggerSync(appContext, it, immediate = true)
                        AutoSyncManager.triggerSync(
                            appContext, it
                        )
                    }
                } catch (_: Exception) {}
            }
            // Small delay so user sees the refresh indicator
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    init {
        loadDishNames()
    }

    // ── Today's Planned Meals ──

    /** Today's planned meals, reactively observed from Room. */
    val todayPlannedMeals: StateFlow<List<PlannedMealEntity>> = run {
        val weekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayDayIndex = LocalDate.now().dayOfWeek.value - 1 // Mon=0, Sun=6

        mealPlanDao.getMealsForWeek(weekStart)
            .map { meals -> meals.filter { it.dayIndex == todayDayIndex } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /** Formats dish_label to display name. Checks DishRecipeEntity first for proper name. */
    private val _dishNameCache = mutableMapOf<String, String>()
    fun getPlannedDishName(dishLabel: String): String {
        return _dishNameCache.getOrPut(dishLabel) {
            dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    /** Pre-load actual dish names from the database. */
    private fun loadDishNames() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allRecipes = dishRecipeDao.getAllDishRecipes()
                allRecipes.forEach { recipe ->
                    _dishNameCache[recipe.dishLabel] = recipe.namePh
                }
            }
        }
    }

    private fun calculateTargetBurned(profile: UserProfile): Int {
        // Dynamic burn target: Active calories = TDEE - BMR
        // BMR via Mifflin-St Jeor (used only for burn target estimation,
        // not for nutritional TEA which uses NDAP method).
        val bmr = if (profile.sex.equals("Male", ignoreCase = true)) {
            (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) + 5
        } else {
            (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) - 161
        }
        // Backward-compatible: accepts both new NDAP IDs and legacy IDs.
        val activityMultiplier = when (profile.activityLevel.lowercase().trim()) {
            "sedentary", "not_very_active"  -> 1.2
            "light", "lightly_active"       -> 1.375
            "moderate", "active"             -> 1.55
            "vigorous", "very_active"        -> 1.725
            else -> 1.2
        }
        val tdee = bmr * activityMultiplier
        val baseBurn = (tdee - bmr).toInt().coerceAtLeast(150) // floor at 150 kcal

        val goalLower = profile.goal.lowercase().trim()
        val isWeightLoss = "lose" in goalLower || "weight_loss" in goalLower || "weight control" in goalLower
        return if (isWeightLoss) baseBurn + 300 else baseBurn
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
            // Standardise duration display:
            //  - GPS workouts have movingTimeSeconds → use DurationFormatter directly
            //  - Manual workouts only have weightOrDuration ("30 min") → parse & reformat
            // Always use weightOrDuration (total elapsed time) for display consistency.
            // movingTimeSeconds only counts GPS-moving time, which can be much less than
            // the actual workout duration and would mismatch the Diary screen.
            val formattedDuration = run {
                val parsed = DurationFormatter.parseToSeconds(entity.weightOrDuration)
                if (parsed != null) DurationFormatter.formatSeconds(parsed)
                else entity.weightOrDuration  // fallback: show raw string
            }

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
                    duration = formattedDuration
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
            dashboardRepository: DashboardRepository,
            mealPlanDao: MealPlanDao,
            dishRecipeDao: DishRecipeDao,
            appContext: android.content.Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                    return DashboardViewModel(auth, dashboardRepository, mealPlanDao, dishRecipeDao, appContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
