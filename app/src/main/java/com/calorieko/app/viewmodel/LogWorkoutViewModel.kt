package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.repository.ActivityRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for LogWorkoutScreen.
 *
 * Manages:
 * - User weight (loaded from Room for MET calorie calculations)
 * - Save operation (photo compression → Room insert → Firestore sync)
 *
 * GPS tracking state (timer, location callbacks, path points, distance, pace)
 * STAYS as local composable state inside GPSTrackerContent, since it is
 * inherently lifecycle-bound to location callbacks and LaunchedEffect timers.
 */
class LogWorkoutViewModel(
    private val auth: FirebaseAuth,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data object SaveSuccess : Event()
        data class SaveError(val message: String) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── State ──

    private val _userWeight = MutableStateFlow(70.0)
    val userWeight: StateFlow<Double> = _userWeight.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val uid: String get() = auth.currentUser?.uid ?: ""

    init {
        loadUserWeight()
    }

    private fun loadUserWeight() {
        val currentUid = uid
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _userWeight.value = activityRepository.getUserWeight(currentUid)
            }
        }
    }

    // ── Save Workout ──

    /**
     * Saves a workout log: compresses photo (if present), builds the entity,
     * inserts to Room, syncs to Firestore, and emits [Event.SaveSuccess].
     *
     * @param context Needed for ContentResolver to read photo URIs.
     */
    fun saveWorkout(
        context: Context,
        name: String,
        calories: Int,
        duration: String,
        dist: Double?,
        pace: Double?,
        movTime: Long?,
        path: String?,
        mType: String?,
        photoUri: String?,
        note: String?,
        tag: String?
    ) {
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Compress and encode photo if present
                    val finalPhotoUri = if (!photoUri.isNullOrEmpty()) {
                        activityRepository.compressAndSavePhoto(context, photoUri)
                    } else {
                        photoUri
                    }

                    // 2. Build entity
                    val currentTimeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    val log = ActivityLogEntity(
                        uid = uid,
                        type = "workout",
                        name = name,
                        timeString = currentTimeString,
                        weightOrDuration = duration,
                        calories = calories,
                        timestamp = System.currentTimeMillis(),
                        distanceKm = dist,
                        pace = pace,
                        movingTimeSeconds = movTime,
                        encodedPath = path,
                        mapType = mType,
                        photoUri = finalPhotoUri,
                        notes = note,
                        activityTag = tag
                    )

                    // 3. Insert to Room + Firestore sync
                    activityRepository.insertWorkoutLog(uid, log)
                }
                _isSaving.value = false
                _events.send(Event.SaveSuccess)
            } catch (e: Exception) {
                _isSaving.value = false
                _events.send(Event.SaveError(e.message ?: "Save failed"))
            }
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            activityRepository: ActivityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LogWorkoutViewModel::class.java)) {
                    return LogWorkoutViewModel(auth, activityRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
