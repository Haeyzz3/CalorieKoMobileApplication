package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.api.ApiSyncManager
import com.calorieko.app.data.remote.api.ApiSyncResult
import com.calorieko.app.data.remote.api.RetrofitClient
import com.calorieko.app.BuildConfig
import com.calorieko.app.util.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for SettingsScreen.
 *
 * Takes [AppDatabase] directly (rather than individual DAOs) because
 * the sync and wipe operations span ALL tables. This is an intentional
 * design choice — SettingsViewModel acts as a system-level orchestrator.
 *
 * ── Sync Architecture ──
 * Firestore  →  Client-state layer (offline-first, real-time cross-device)
 * Laravel    →  System of Record (admin dashboard, analytics, archiving)
 *
 * Both backends are synced in parallel. The Laravel sync uses delta payloads
 * (only records modified since `lastSuccessfulSyncTimestamp`) with Last Write Wins
 * conflict resolution.
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
        data object WipeSuccess : Event()
        data object LogoutReady : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Loading State ──

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isWipingData = MutableStateFlow(false)
    val isWipingData: StateFlow<Boolean> = _isWipingData.asStateFlow()

    // ── Lazy API Sync Manager ──

    private val apiSyncManager: ApiSyncManager by lazy {
        val apiService = RetrofitClient.getApiService(BuildConfig.API_BASE_URL)
        ApiSyncManager(
            apiService = apiService,
            userDao = db.userDao(),
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
            context = appContext
        )
    }

    // ── Public Actions ──

    /**
     * Syncs all local data to BOTH backends:
     *
     * 1. **Firestore** (full sync) — Iterates over all entity types for real-time cloud state.
     * 2. **Laravel API** (delta sync) — Transmits only records modified since last successful sync,
     *    with `updated_at` timestamps for Last Write Wins conflict resolution.
     */
    fun syncAllData() {
        val uid = auth.currentUser?.uid ?: return
        if (_isSyncing.value) return

        _isSyncing.value = true

        viewModelScope.launch {
            var firestoreSuccess = false
            var apiResult: ApiSyncResult? = null

            try {
                withContext(Dispatchers.IO) {
                    // ══════════════════════════════════════════════════
                    // 1. FIRESTORE SYNC (Full — Client State Layer)
                    // ══════════════════════════════════════════════════
                    try {
                        // 1a. Profile (single doc — no batching needed)
                        db.userDao().getUser(uid)?.let { profile ->
                            firestoreSyncRepo.syncProfile(uid, profile)
                        }

                        // 1b. Activity Logs (batched)
                        val activityLogs = db.activityLogDao().getAllLogsForUser(uid)
                        firestoreSyncRepo.syncActivityLogsBatch(uid, activityLogs)

                        // 1c. Meal Logs + Items (each already uses WriteBatch internally)
                        db.mealLogDao().getAllMealLogsWithItems(uid).forEach { mlwi ->
                            firestoreSyncRepo.syncMealLog(uid, mlwi.mealLog, mlwi.items)
                        }

                        // 1d. Nutrition Summaries (batched)
                        val summaries = db.dailyNutritionSummaryDao().getAllSummariesForUser(uid)
                        firestoreSyncRepo.syncDailyNutritionSummariesBatch(uid, summaries)

                        // 1e. Pantry Items (batched)
                        val pantryItems = db.pantryDao().getAllItemsList()
                        firestoreSyncRepo.syncPantryItemsBatch(uid, pantryItems)

                        // 1f. Planned Meals (batched)
                        val plannedMeals = db.mealPlanDao().getAllPlannedMeals()
                        firestoreSyncRepo.syncPlannedMealsBatch(uid, plannedMeals)

                        firestoreSuccess = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Firestore failure doesn't block Laravel sync
                    }

                    // ══════════════════════════════════════════════════
                    // 2. LARAVEL API SYNC (Delta — System of Record)
                    // ══════════════════════════════════════════════════
                    if (NetworkUtils.isOnline(appContext)) {
                        apiResult = apiSyncManager.syncToBackend(uid)
                    }
                }

                _isSyncing.value = false

                // ── Report outcome ──
                when {
                    firestoreSuccess && apiResult is ApiSyncResult.Success -> {
                        val conflicts = apiResult.conflicts
                        if (!conflicts.isNullOrEmpty()) {
                            _events.send(Event.SyncPartial(
                                "Synced successfully with ${conflicts.size} server-override conflict(s)."
                            ))
                        } else {
                            _events.send(Event.SyncSuccess)
                        }
                    }
                    firestoreSuccess && apiResult is ApiSyncResult.Error -> {
                        _events.send(Event.SyncPartial(
                            "Cloud sync OK, but Laravel sync failed: ${apiResult.message}"
                        ))
                    }
                    firestoreSuccess && apiResult == null -> {
                        _events.send(Event.SyncPartial("Cloud sync OK. No internet for Laravel sync."))
                    }
                    !firestoreSuccess && apiResult is ApiSyncResult.Success -> {
                        _events.send(Event.SyncPartial("Laravel sync OK, but Firestore sync failed."))
                    }
                    else -> {
                        _events.send(Event.SyncError("Sync failed for both backends."))
                    }
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _events.send(Event.SyncError(e.message ?: "Sync failed"))
            }
        }
    }

    /**
     * Wipes all local Room data and cloud Firestore data for the current user.
     * Also resets the delta sync timestamp so the next sync is a full sync.
     */
    fun wipeAllData() {
        val uid = auth.currentUser?.uid
        if (_isWipingData.value) return

        _isWipingData.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Wipe Firestore cloud data
                    if (uid != null) {
                        firestoreSyncRepo.wipeAllUserData(uid)
                    }
                    // 2. Wipe the local Room database
                    db.clearAllTables()
                    // 3. Reset delta sync timestamp (critical!)
                    apiSyncManager.resetSyncTimestamp()
                }
                _isWipingData.value = false
                _events.send(Event.WipeSuccess)
            } catch (e: Exception) {
                _isWipingData.value = false
                e.printStackTrace()
                // Still emit success since partial wipe may have occurred
                _events.send(Event.WipeSuccess)
            }
        }
    }

    /**
     * Clears local Room data and signs out of Firebase.
     */
    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try { db.clearAllTables() } catch (_: Exception) {}
            }
            auth.signOut()
            _events.send(Event.LogoutReady)
        }
    }

    companion object {
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
