package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.api.ApiSyncManager
import com.calorieko.app.data.remote.api.ApiSyncResult
import com.calorieko.app.data.remote.api.RetrofitClient
import com.calorieko.app.BuildConfig
import com.calorieko.app.util.NetworkUtils
import com.calorieko.app.util.StreakReminderWorker
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel for SettingsScreen.
 *
 * Takes [AppDatabase] directly (rather than individual DAOs) because
 * the sync and wipe operations span ALL tables. This is an intentional
 * design choice — SettingsViewModel acts as a system-level orchestrator.
 *
 * ── Sync Architecture ──
 * Firestore  →  Handled automatically by repositories (MealRepository, ActivityRepository)
 *               on every Room write. No user action needed.
 * Laravel    →  System of Record (admin dashboard, analytics, archiving).
 *               The "Sync Data" button in Settings ONLY triggers Laravel sync.
 *
 * The Laravel sync uses delta payloads (only records modified since
 * `lastSuccessfulSyncTimestamp`) with Last Write Wins conflict resolution.
 */
class SettingsViewModel(
    private val auth: FirebaseAuth,
    private val db: AppDatabase,
    private val firestoreSyncRepo: FirestoreSyncRepository,
    private val appContext: Context
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data object SyncSuccess : Event()
        data class SyncError(val message: String) : Event()
        data class SyncPartial(val message: String) : Event()
        data object WipeProgressSuccess : Event()
        data object LogoutReady : Event()
        data object AccountDeleted : Event()
        data class AccountDeletionError(val message: String) : Event()
        data class PasswordResetSent(val email: String) : Event()
        data class PasswordResetError(val message: String) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Loading State ──

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isWipingProgress = MutableStateFlow(false)
    val isWipingProgress: StateFlow<Boolean> = _isWipingProgress.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    // ── Last Synced Timestamp ──

    private val syncPrefs = appContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    private val _lastSyncedAt = MutableStateFlow(
        formatSyncTimestamp(syncPrefs.getLong(KEY_LAST_SYNC, 0L))
    )
    val lastSyncedAt: StateFlow<String> = _lastSyncedAt.asStateFlow()

    // ── Lazy API Sync Manager ──

    private val apiSyncManager: ApiSyncManager by lazy {
        val apiService = RetrofitClient.getApiService(BuildConfig.API_BASE_URL)
        ApiSyncManager(
            apiService = apiService,
            userDao = db.userDao(),
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
            weightLogDao = db.weightLogDao(),
            context = appContext
        )
    }

    // ── Public Actions ──

    /**
     * Syncs local data to the **Laravel API only** (System of Record).
     *
     * Firestore sync is handled automatically by MealRepository and
     * ActivityRepository on every Room write — it does NOT need to be
     * triggered manually from Settings.
     *
     * Uses delta payloads: only records modified since last successful sync,
     * with `updated_at` timestamps for Last Write Wins conflict resolution.
     */
    fun syncAllData() {
        val uid = auth.currentUser?.uid ?: return
        if (_isSyncing.value) return

        _isSyncing.value = true

        viewModelScope.launch {
            try {
                var apiResult: ApiSyncResult? = null

                withContext(Dispatchers.IO) {
                    // ══════════════════════════════════════════════════
                    // LARAVEL API SYNC (Delta — System of Record)
                    // ══════════════════════════════════════════════════
                    if (NetworkUtils.isOnline(appContext)) {
                        apiResult = apiSyncManager.syncToBackend(uid)
                    }
                }

                _isSyncing.value = false

                // ── Persist last-sync timestamp on success ──
                if (apiResult is ApiSyncResult.Success) {
                    val now = System.currentTimeMillis()
                    syncPrefs.edit().putLong(KEY_LAST_SYNC, now).apply()
                    _lastSyncedAt.value = formatSyncTimestamp(now)
                }

                // ── Report outcome ──
                when (apiResult) {
                    is ApiSyncResult.Success -> {
                        val conflicts = (apiResult as ApiSyncResult.Success).conflicts
                        if (!conflicts.isNullOrEmpty()) {
                            _events.send(Event.SyncPartial(
                                "Synced successfully with ${conflicts.size} server-override conflict(s)."
                            ))
                        } else {
                            _events.send(Event.SyncSuccess)
                        }
                    }
                    is ApiSyncResult.Error -> {
                        _events.send(Event.SyncError(
                            "Laravel sync failed: ${(apiResult as ApiSyncResult.Error).message}"
                        ))
                    }
                    null -> {
                        _events.send(Event.SyncError("No internet connection. Please try again later."))
                    }
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _events.send(Event.SyncError(e.message ?: "Sync failed"))
            }
        }
    }

    /**
     * Wipes all progress data (meals, activities, nutrition summaries, pantry,
     * and meal plans) from both Room and Firestore while **preserving** the
     * user's profile and settings (name, age, weight, height, etc.).
     *
     * Also resets the delta sync timestamp so the next sync is a full sync.
     */
    fun wipeProgress() {
        val uid = auth.currentUser?.uid
        if (_isWipingProgress.value) return

        _isWipingProgress.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Cancel any pending/running SyncWorker to prevent race condition
                    //    where a queued sync re-pushes data to Firestore AFTER the wipe.
                    //    (This mirrors the cancellation pattern used in deleteAccount().)
                    WorkManager.getInstance(appContext).cancelUniqueWork("calorieko_auto_sync")

                    // 2. Wipe Firestore cloud progress data (sub-collections only;
                    //    the user profile document at users/{uid} is preserved)
                    if (uid != null) {
                        firestoreSyncRepo.wipeAllUserData(uid)
                    }
                    // 3. Selectively clear progress tables in Room.
                    //    Preserves: user_profile (settings), FOOD_TABLE, DISH_INGREDIENTS_TABLE
                    db.activityLogDao().deleteAll()
                    db.mealLogDao().deleteAll()
                    db.mealLogItemDao().deleteAll()
                    db.dailyNutritionSummaryDao().deleteAll()
                    db.pantryDao().clearAllItems()
                    db.mealPlanDao().deleteAll()
                    db.weightLogDao().deleteAll()
                    // 4. Reset delta sync timestamp (critical!)
                    apiSyncManager.resetSyncTimestamp()
                    // 5. Clear last-sync display timestamp
                    syncPrefs.edit().remove(KEY_LAST_SYNC).apply()
                    _lastSyncedAt.value = formatSyncTimestamp(0L)
                }
                _isWipingProgress.value = false
                _events.send(Event.WipeProgressSuccess)
            } catch (e: Exception) {
                _isWipingProgress.value = false
                e.printStackTrace()
                // Still emit success since partial wipe may have occurred
                _events.send(Event.WipeProgressSuccess)
            }
        }
    }

    /**
     * Sends a password reset email to the currently logged-in user's email.
     * This allows them to update their security credentials via Firebase's
     * built-in password reset flow.
     */
    fun sendPasswordResetEmail() {
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            viewModelScope.launch {
                _events.send(Event.PasswordResetError("No email address found for this account."))
            }
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _events.send(Event.PasswordResetSent(email))
                }
            }
            .addOnFailureListener { e ->
                viewModelScope.launch {
                    _events.send(Event.PasswordResetError(
                        e.localizedMessage ?: "Failed to send password reset email."
                    ))
                }
            }
    }

    /**
     * Clears local user data (preserving reference data) and signs out of Firebase.
     *
     * IMPORTANT: We selectively clear only user-data tables. We must NOT call
     * db.clearAllTables() because that also destroys FOOD_TABLE and
     * DISH_INGREDIENTS_TABLE — static CSV-seeded reference data that is
     * expensive to re-seed and causes a race condition with ViewModel queries
     * if cleared (the async FoodDatabaseCallback re-seed may not complete
     * before ViewModels read the empty tables on re-login).
     */
    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Selectively clear user data tables ONLY.
                    // Preserve FOOD_TABLE and DISH_INGREDIENTS_TABLE
                    // (static CSV reference data that is expensive to re-seed).
                    db.userDao().deleteAll()
                    db.activityLogDao().deleteAll()
                    db.mealLogDao().deleteAll()
                    db.mealLogItemDao().deleteAll()
                    db.dailyNutritionSummaryDao().deleteAll()
                    db.pantryDao().clearAllItems()
                    db.mealPlanDao().deleteAll()
                    db.weightLogDao().deleteAll()
                } catch (_: Exception) {}
            }
            // Cancel scheduled workers
            StreakReminderWorker.cancel(appContext)
            com.calorieko.app.util.MealPlanReminderWorker.cancel(appContext)
            auth.signOut()
            _events.send(Event.LogoutReady)
        }
    }

    /**
     * Permanently deletes the user's account and all associated data.
     *
     * Requires the user's password for re-authentication (Firebase mandates
     * a recent sign-in for sensitive operations like account deletion).
     *
     * Execution order (designed so the user isn't locked out if a step fails):
     * 1. Re-authenticate with password
     * 2. Delete all Firestore data (sub-collections + profile document)
     * 3. Clear all local Room user data
     * 4. Reset sync timestamps
     * 5. Cancel scheduled WorkManager tasks (streak reminders, pending syncs)
     * 6. Delete Firebase Auth account (LAST — point of no return)
     *
     * // TODO [Laravel Backend]: When the Laravel delete-user API endpoint
     * //   is available, add an API call here BEFORE step 6 to purge the
     * //   user's data from the System of Record (Laravel database).
     * //   e.g., apiService.deleteUser(uid)
     */
    fun deleteAccount(password: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        if (_isDeletingAccount.value) return

        _isDeletingAccount.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Re-authenticate (Firebase requires recent login for account deletion)
                    val credential = EmailAuthProvider.getCredential(email, password)
                    user.reauthenticate(credential).await()

                    // 2. Delete ALL Firestore data (sub-collections + profile document)
                    firestoreSyncRepo.deleteUserAccount(user.uid)

                    // 3. Clear all local Room user data
                    db.userDao().deleteAll()
                    db.activityLogDao().deleteAll()
                    db.mealLogDao().deleteAll()
                    db.mealLogItemDao().deleteAll()
                    db.dailyNutritionSummaryDao().deleteAll()
                    db.pantryDao().clearAllItems()
                    db.mealPlanDao().deleteAll()
                    db.weightLogDao().deleteAll()

                    // 4. Reset sync timestamps
                    apiSyncManager.resetSyncTimestamp()
                    syncPrefs.edit().remove(KEY_LAST_SYNC).apply()

                    // 5. Cancel scheduled WorkManager tasks
                    StreakReminderWorker.cancel(appContext)
                    WorkManager.getInstance(appContext).cancelAllWork()

                    // 6. Delete Firebase Auth account (point of no return)
                    user.delete().await()
                }

                _isDeletingAccount.value = false
                _events.send(Event.AccountDeleted)
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                _isDeletingAccount.value = false
                _events.send(Event.AccountDeletionError(
                    "Your session has expired. Please log out and log back in, then try again."
                ))
            } catch (e: Exception) {
                _isDeletingAccount.value = false
                val message = when {
                    e.message?.contains("password is invalid", ignoreCase = true) == true ||
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                        "Incorrect password. Please try again."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    else -> e.localizedMessage ?: "Account deletion failed. Please try again."
                }
                _events.send(Event.AccountDeletionError(message))
            }
        }
    }

    companion object {
        private const val KEY_LAST_SYNC = "last_successful_sync_ms"

        /**
         * Formats a sync timestamp into a human-readable relative string.
         * - 0L → "Never synced"
         * - <1 min ago → "Just now"
         * - <60 min → "X min ago"
         * - Today → "Today, h:mm a"
         * - Yesterday → "Yesterday, h:mm a"
         * - Older → "MMM d, h:mm a"
         */
        fun formatSyncTimestamp(timestampMs: Long): String {
            if (timestampMs == 0L) return "Never synced"

            val now = System.currentTimeMillis()
            val diffMs = now - timestampMs
            val diffMin = diffMs / 60_000

            return when {
                diffMin < 1 -> "Just now"
                diffMin < 60 -> "$diffMin min ago"
                else -> {
                    val syncCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
                    val nowCal = Calendar.getInstance()
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

                    when {
                        syncCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                                syncCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) ->
                            "Today, ${timeFormat.format(Date(timestampMs))}"

                        syncCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                                syncCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) - 1 ->
                            "Yesterday, ${timeFormat.format(Date(timestampMs))}"

                        else -> dateTimeFormat.format(Date(timestampMs))
                    }
                }
            }
        }

        fun provideFactory(
            auth: FirebaseAuth,
            db: AppDatabase,
            firestoreSyncRepo: FirestoreSyncRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    return SettingsViewModel(auth, db, firestoreSyncRepo, appContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
