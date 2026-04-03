package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.model.BadgeStats
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.repository.UserRepository
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

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val mealLogDao: MealLogDao,
    private val activityLogDao: ActivityLogDao
) : ViewModel() {

    // ── User Profile ──

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // ── Badge Stats ──

    private val _badgeStats = MutableStateFlow(BadgeStats())
    val badgeStats: StateFlow<BadgeStats> = _badgeStats.asStateFlow()

    // ── Firebase-derived Display Info ──

    private val _displayName = MutableStateFlow("User")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _memberSince = MutableStateFlow("January 2025")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    val firebasePhotoUrl = auth.currentUser?.photoUrl

    init {
        // Compute Firebase-derived values immediately
        _displayName.value = auth.currentUser?.displayName ?: "User"

        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        _memberSince.value = auth.currentUser?.metadata?.creationTimestamp?.let {
            sdf.format(Date(it))
        } ?: "January 2025"

        loadProfileData()
    }

    /**
     * Loads user profile and badge stats from Room.
     */
    fun loadProfileData() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // A. Fetch user profile
                _userProfile.value = userRepository.getUserProfile(uid)

                // B. Fetch badge stats
                _badgeStats.value = userRepository.getBadgeStats(uid, mealLogDao, activityLogDao)
            }
        }
    }

    /**
     * Reloads profile data. Call when returning from EditProfileScreen.
     */
    fun refreshProfile() {
        loadProfileData()
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            userRepository: UserRepository,
            mealLogDao: MealLogDao,
            activityLogDao: ActivityLogDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                    return ProfileViewModel(auth, userRepository, mealLogDao, activityLogDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
