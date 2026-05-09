package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.model.BadgeStats
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.repository.ActivityRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
data class ProfileUiState(
    val totalWorkouts: Int = 0
)

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
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

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // ── Computed Streak (dynamic — walks backwards through logged days) ──

    private val _computedStreak = MutableStateFlow(0)
    val computedStreak: StateFlow<Int> = _computedStreak.asStateFlow()

    // ── Firebase-derived Display Info ──

    private val _displayName = MutableStateFlow("User")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _memberSince = MutableStateFlow("January 2025")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    val firebasePhotoUrl = auth.currentUser?.photoUrl

    init {
        // Compute Firebase-derived values immediately
        val authName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "User"
        _displayName.value = authName

        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        _memberSince.value = auth.currentUser?.metadata?.creationTimestamp?.let {
            sdf.format(Date(it))
        } ?: "January 2025"

        loadBadgeStats()
        loadWorkoutMilestoneProgress()
        loadComputedStreak()

        // Keep displayName reactive to profile changes from EditProfileScreen
        viewModelScope.launch {
            userProfile.collect { profile ->
                val name = profile?.name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: "User"
                _displayName.value = name
            }
        }
    }

    /**
     * Loads badge stats from Room (one-shot).
     * Profile data is handled reactively via the Flow above.
     */
    private fun loadBadgeStats() {
        val currentUid = uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val stats = userRepository.getBadgeStats(currentUid, mealLogDao, activityLogDao)
                _badgeStats.value = stats
                _uiState.value = _uiState.value.copy(totalWorkouts = stats.totalWorkouts)
            }
        }
    }

    private fun loadWorkoutMilestoneProgress() {
        val currentUid = uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val totalWorkouts = try {
                    activityRepository.getTotalWorkoutsCount(currentUid)
                } catch (_: Exception) {
                    0
                }
                _uiState.value = _uiState.value.copy(totalWorkouts = totalWorkouts)
            }
        }
    }

    /**
     * Computes the user's current streak by walking backwards from today.
     *
     * Inclusive model:
     * - If the user logged any activity or meal TODAY → streak starts at 1.
     * - Then checks yesterday, the day before, etc., incrementing while there
     *   is at least one log per day.
     *
     * This fixes the 0-day bug where the static `UserProfile.streak` field
     * was never being incremented by any logic.
     */
    private fun loadComputedStreak() {
        val currentUid = uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val zoneId = ZoneId.systemDefault()
                val today = LocalDate.now(zoneId)
                val activityDates = activityRepository.getLogTimestampsForUser(currentUid)
                    .map { it.toLocalDate(zoneId) }
                val mealDates = mealLogDao.getMealLogTimestampsForUser(currentUid)
                    .map { it.toLocalDate(zoneId) }
                val loggedDates = (activityDates + mealDates)
                    .filterNot { it.isAfter(today) }
                    .distinct()
                    .sortedDescending()
                val loggedDateSet = loggedDates.toSet()

                var cursor = when {
                    today in loggedDateSet -> today
                    today.minusDays(1) in loggedDateSet -> today.minusDays(1)
                    else -> null
                }
                var streak = 0

                while (cursor != null && cursor in loggedDateSet) {
                    streak++
                    cursor = cursor.minusDays(1)
                }

                _computedStreak.value = streak
            }
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            userRepository: UserRepository,
            activityRepository: ActivityRepository,
            mealLogDao: MealLogDao,
            activityLogDao: ActivityLogDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                    return ProfileViewModel(auth, userRepository, activityRepository, mealLogDao, activityLogDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
