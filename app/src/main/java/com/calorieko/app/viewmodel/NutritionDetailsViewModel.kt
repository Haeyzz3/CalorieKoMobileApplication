package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
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

class NutritionDetailsViewModel(
    private val auth: FirebaseAuth,
    private val dashboardRepository: DashboardRepository
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
    }

    // ── Public Actions ──

    fun setDayOffset(offset: Int) {
        _dayOffset.value = offset
        loadDaySummary()
    }

    fun incrementDayOffset() {
        _dayOffset.value++
        loadDaySummary()
    }

    fun decrementDayOffset() {
        _dayOffset.value--
        loadDaySummary()
    }

    fun setWeekOffset(offset: Int) {
        _weekOffset.value = offset
        loadWeekSummaries()
    }

    fun incrementWeekOffset() {
        _weekOffset.value++
        loadWeekSummaries()
    }

    fun decrementWeekOffset() {
        _weekOffset.value--
        loadWeekSummaries()
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

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            dashboardRepository: DashboardRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NutritionDetailsViewModel::class.java)) {
                    return NutritionDetailsViewModel(auth, dashboardRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
