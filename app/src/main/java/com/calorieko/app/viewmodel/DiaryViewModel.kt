package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.model.ActivityLogEntity
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class DiaryViewModel(
    private val auth: FirebaseAuth,
    private val dashboardRepository: DashboardRepository,
    private val activityLogDao: ActivityLogDao,
    private val mealLogDao: MealLogDao
) : ViewModel() {

    // ── Targets ──

    private val _targets = MutableStateFlow<NutritionalTarget?>(null)
    val targets: StateFlow<NutritionalTarget?> = _targets.asStateFlow()

    // ── Day View Data ──

    private val _daySummary = MutableStateFlow<DailyNutritionSummaryEntity?>(null)
    val daySummary: StateFlow<DailyNutritionSummaryEntity?> = _daySummary.asStateFlow()

    // ── Week View Data ──

    private val _weekSummaries = MutableStateFlow<List<DailyNutritionSummaryEntity>>(emptyList())
    val weekSummaries: StateFlow<List<DailyNutritionSummaryEntity>> = _weekSummaries.asStateFlow()

    // ── Activity Log (for History Tab) ──

    private val _activityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _activityLogs.asStateFlow()

    // ── Weekly Activity Logs (for Activities chart in week view) ──

    private val _weeklyActivityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val weeklyActivityLogs: StateFlow<List<ActivityLogEntity>> = _weeklyActivityLogs.asStateFlow()

    // ── Meal Logs (for Meals Tab) ──

    private val _mealLogs = MutableStateFlow<List<MealLogWithItems>>(emptyList())
    val mealLogs: StateFlow<List<MealLogWithItems>> = _mealLogs.asStateFlow()

    // ── Date Navigation State ──

    private val _dayOffset = MutableStateFlow(0)
    val dayOffset: StateFlow<Int> = _dayOffset.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    private val _viewMode = MutableStateFlow("day")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val uid: String
        get() = auth.currentUser?.uid ?: ""

    init {
        loadTargets()
        loadDaySummary()
        loadWeekSummaries()
        loadActivityLogs()
        loadWeeklyActivityLogs()
        loadMealLogs()
    }

    // ── Public Actions ──

    fun setDayOffset(offset: Int) {
        _dayOffset.value = offset
        loadDaySummary()
        loadActivityLogs()
        loadMealLogs()
    }

    fun incrementDayOffset() {
        _dayOffset.value++
        loadDaySummary()
        loadActivityLogs()
        loadMealLogs()
    }

    fun decrementDayOffset() {
        _dayOffset.value--
        loadDaySummary()
        loadActivityLogs()
        loadMealLogs()
    }

    fun setWeekOffset(offset: Int) {
        _weekOffset.value = offset
        loadWeekSummaries()
        loadWeeklyActivityLogs()
    }

    fun incrementWeekOffset() {
        _weekOffset.value++
        loadWeekSummaries()
        loadWeeklyActivityLogs()
    }

    fun decrementWeekOffset() {
        _weekOffset.value--
        loadWeekSummaries()
        loadWeeklyActivityLogs()
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
    }

    /**
     * Handles date picker selection: converts picked date to a dayOffset
     * relative to today, switches to day view, and triggers a reload.
     */
    fun pickDate(pickedDate: LocalDate) {
        val today = LocalDate.now()
        _dayOffset.value = (pickedDate.toEpochDay() - today.toEpochDay()).toInt()
        _viewMode.value = "day"
        loadDaySummary()
        loadMealLogs()
    }

    // ── Data Loading ──

    private fun loadTargets() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val profile = dashboardRepository.getUserProfile(uid) ?: return@withContext
                _targets.value = dashboardRepository.getTargetsForUser(profile)
            }
        }
    }

    private fun loadDaySummary() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val selectedDate = today.plusDays(_dayOffset.value.toLong())
                val epochDay = selectedDate.toEpochDay()
                _daySummary.value = dashboardRepository.getNutritionSummaryForDate(uid, epochDay)
            }
        }
    }

    private fun loadWeekSummaries() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val weekStart = today.plusWeeks(_weekOffset.value.toLong()).with(DayOfWeek.MONDAY)
                val weekEnd = weekStart.plusDays(6)
                _weekSummaries.value = dashboardRepository.getNutritionSummariesForRange(
                    uid, weekStart.toEpochDay(), weekEnd.toEpochDay()
                )
            }
        }
    }

    private fun loadActivityLogs() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val selectedDate = LocalDate.now().plusDays(_dayOffset.value.toLong())
                val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
                _activityLogs.value = activityLogDao.getLogsForRange(uid, startOfDay, endOfDay)
            }
        }
    }

    private fun loadWeeklyActivityLogs() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                val weekStart = today.plusWeeks(_weekOffset.value.toLong()).with(DayOfWeek.MONDAY)
                val weekEnd = weekStart.plusDays(6)
                val startMs = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMs = weekEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                _weeklyActivityLogs.value = activityLogDao.getLogsForRange(uid, startMs, endMs)
            }
        }
    }

    private fun loadMealLogs() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val selectedDate = LocalDate.now().plusDays(_dayOffset.value.toLong())
                val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
                _mealLogs.value = mealLogDao.getMealLogsWithItemsByDate(uid, startOfDay, endOfDay)
            }
        }
    }

    fun deleteActivity(activityId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // We're currently just deleting it locally. The ApiSyncManager
                // handles pushing deletions or resyncing on load depending on architecture.
                activityLogDao.deleteLogById(activityId)
                // Reload activities to update UI immediately
                loadActivityLogs()
                loadWeeklyActivityLogs()
            }
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            dashboardRepository: DashboardRepository,
            activityLogDao: ActivityLogDao,
            mealLogDao: MealLogDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
                    return DiaryViewModel(auth, dashboardRepository, activityLogDao, mealLogDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
