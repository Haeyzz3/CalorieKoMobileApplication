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

/**
 * ViewModel for ActivityDetailsScreen.
 *
 * Takes [activityId] as a constructor param (Option A) so the activity
 * loading is fully managed by the ViewModel, keeping the nav route clean.
 *
 * Manages:
 * - Activity log entity loaded from Room by ID
 * - User display name fetched from Room
 */
class ActivityDetailsViewModel(
    private val auth: FirebaseAuth,
    private val activityRepository: ActivityRepository,
    private val activityId: Int
) : ViewModel() {

    // ── State ──

    private val _activityLog = MutableStateFlow<ActivityLogEntity?>(null)
    val activityLog: StateFlow<ActivityLogEntity?> = _activityLog.asStateFlow()

    private val _userName = MutableStateFlow("CalorieKo athlete")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 1. Load activity log by ID
                _activityLog.value = activityRepository.getLogById(activityId)

                // 2. Load user display name
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    _userName.value = activityRepository.getUserName(uid)
                }
            }
            _isLoading.value = false
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            activityRepository: ActivityRepository,
            activityId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ActivityDetailsViewModel::class.java)) {
                    return ActivityDetailsViewModel(auth, activityRepository, activityId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
