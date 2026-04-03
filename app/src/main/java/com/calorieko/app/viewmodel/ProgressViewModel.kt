package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.repository.ActivityRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * ViewModel for ProgressScreen.
 *
 * Manages:
 * - User weight for the weight trend chart
 * - Activity logs for the selected time range (weekly/monthly)
 * - View mode toggle (weekly ↔ monthly) with automatic data re-fetch
 *
 * Chart data processing (calorie data, sodium data, weight data, top foods)
 * stays as composable-level `remember(weeklyLogs)` transformations since
 * they are pure UI-data mappings with no backend dependency.
 */
class ProgressViewModel(
    private val auth: FirebaseAuth,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    // ── State ──

    private val _weeklyLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val weeklyLogs: StateFlow<List<ActivityLogEntity>> = _weeklyLogs.asStateFlow()

    private val _userWeight = MutableStateFlow(74.0)
    val userWeight: StateFlow<Double> = _userWeight.asStateFlow()

    private val _viewMode = MutableStateFlow("weekly")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

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

    /**
     * Fetches user weight and activity logs for the currently selected time range.
     */
    fun loadData() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _dataLoaded.value = true
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 1. Fetch user weight
                _userWeight.value = activityRepository.getUserWeight(uid)

                // 2. Compute time range
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val endTime = calendar.timeInMillis + 24 * 60 * 60 * 1000L

                val daysBack = if (_viewMode.value == "weekly") 7 else 30
                calendar.add(Calendar.DAY_OF_YEAR, -(daysBack - 1))
                val startTime = calendar.timeInMillis

                // 3. Fetch logs
                _weeklyLogs.value = activityRepository.getLogsForRange(uid, startTime, endTime)
            }
            _dataLoaded.value = true
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            activityRepository: ActivityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
                    return ProgressViewModel(auth, activityRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
