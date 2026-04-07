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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ProfileViewModel — reactive via Room Flow observation for the user profile.
 *
 * ── How it works ──
 * `userProfile` is observed via Room `Flow<T>`, which automatically re-emits
 * whenever the `user_profile` table changes. This means any saves from
 * EditProfileScreen instantly reflect here — no manual refresh needed.
 *
 * Badge stats are still loaded once (one-shot) since they aggregate across
 * multiple DAOs and don't change while viewing the profile.
 */
class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val mealLogDao: MealLogDao,
    private val activityLogDao: ActivityLogDao
) : ViewModel() {

    private val uid: String? = auth.currentUser?.uid

    // ── User Profile (reactive — auto-updates on Room changes) ──

    val userProfile: StateFlow<UserProfile?> =
        if (uid != null) {
            userRepository.observeUserProfile(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        } else {
            MutableStateFlow(null)
        }

    // ── Badge Stats (one-shot — loaded once per screen visit) ──

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

        loadBadgeStats()
    }

    /**
     * Loads badge stats from Room (one-shot).
     * Profile data is handled reactively via the Flow above.
     */
    private fun loadBadgeStats() {
        val currentUid = uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _badgeStats.value = userRepository.getBadgeStats(currentUid, mealLogDao, activityLogDao)
            }
        }
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
