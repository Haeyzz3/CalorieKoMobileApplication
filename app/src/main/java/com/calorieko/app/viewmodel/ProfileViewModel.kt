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
import java.util.Calendar
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
        _displayName.value = auth.currentUser?.displayName ?: "User"

        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        _memberSince.value = auth.currentUser?.metadata?.creationTimestamp?.let {
            sdf.format(Date(it))
        } ?: "January 2025"

        loadBadgeStats()
        loadComputedStreak()
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
                var streak = 0
                val cal = Calendar.getInstance()

                // Walk backwards up to 365 days max
                for (i in 0..365) {
                    val dayStart = cal.clone() as Calendar
                    dayStart.set(Calendar.HOUR_OF_DAY, 0)
                    dayStart.set(Calendar.MINUTE, 0)
                    dayStart.set(Calendar.SECOND, 0)
                    dayStart.set(Calendar.MILLISECOND, 0)
                    val startMs = dayStart.timeInMillis
                    val endMs = startMs + 86_400_000L

                    val hasActivity = activityLogDao.getLogsForRange(currentUid, startMs, endMs).isNotEmpty()
                    val hasMeal = mealLogDao.getMealLogsByDate(currentUid, startMs, endMs).isNotEmpty()

                    if (hasActivity || hasMeal) {
                        streak++
                        cal.add(Calendar.DAY_OF_YEAR, -1) // check the previous day
                    } else {
                        break // gap found, streak ends
                    }
                }

                _computedStreak.value = streak
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
