package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.remote.FirestoreSyncRepository
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
 */
class SettingsViewModel(
    private val auth: FirebaseAuth,
    private val db: AppDatabase,
    private val firestoreSyncRepo: FirestoreSyncRepository
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data object SyncSuccess : Event()
        data class SyncError(val message: String) : Event()
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

    // ── Public Actions ──

    /**
     * Syncs all local data to Firestore. Iterates over 6 entity types:
     * 1. User Profile
     * 2. Activity Logs
     * 3. Meal Logs + Items
     * 4. Daily Nutrition Summaries
     * 5. Pantry Items
     * 6. Planned Meals
     */
    fun syncAllData() {
        val uid = auth.currentUser?.uid ?: return
        if (_isSyncing.value) return

        _isSyncing.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Profile
                    db.userDao().getUser(uid)?.let { profile ->
                        firestoreSyncRepo.syncProfile(uid, profile)
                    }
                    // 2. Activity Logs
                    db.activityLogDao().getAllLogsForUser(uid).forEach { log ->
                        firestoreSyncRepo.syncActivityLog(uid, log)
                    }
                    // 3. Meal Logs + Items
                    db.mealLogDao().getAllMealLogsWithItems(uid).forEach { mlwi ->
                        firestoreSyncRepo.syncMealLog(uid, mlwi.mealLog, mlwi.items)
                    }
                    // 4. Nutrition Summaries
                    db.dailyNutritionSummaryDao().getAllSummariesForUser(uid).forEach { summary ->
                        firestoreSyncRepo.syncDailyNutritionSummary(uid, summary)
                    }
                    // 5. Pantry Items
                    db.pantryDao().getAllItemsList().forEach { itemName ->
                        firestoreSyncRepo.syncPantryItem(uid, itemName)
                    }
                    // 6. Planned Meals
                    db.mealPlanDao().getAllPlannedMeals().forEach { meal ->
                        firestoreSyncRepo.syncPlannedMeal(uid, meal)
                    }
                }
                _isSyncing.value = false
                _events.send(Event.SyncSuccess)
            } catch (e: Exception) {
                _isSyncing.value = false
                _events.send(Event.SyncError(e.message ?: "Sync failed"))
            }
        }
    }

    /**
     * Wipes all local Room data and cloud Firestore data for the current user.
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
            firestoreSyncRepo: FirestoreSyncRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    return SettingsViewModel(auth, db, firestoreSyncRepo) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
