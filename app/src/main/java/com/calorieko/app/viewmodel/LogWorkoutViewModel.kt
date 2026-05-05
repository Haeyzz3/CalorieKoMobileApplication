package com.calorieko.app.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.repository.ActivityRepository
import com.calorieko.app.util.LocationTrackingService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * ── Persistent State Bridge ──
 * This ViewModel acts as the single source of truth for workout tracking state.
 * It binds to [LocationTrackingService] and mirrors its StateFlows into its own
 * StateFlows. Because a ViewModel survives Activity configuration changes and
 * composable re-compositions, the workout state persists even when:
 *   - The user presses the Home button (Activity.onStop → onDestroy)
 *   - The user returns to the app (new Activity created, same ViewModel)
 *   - The composable is disposed and re-composed
 *
 * The service continues running as a foreground service in the background.
 * When the ViewModel re-binds (on return), it picks up the live state from the
 * still-running service and seamlessly resumes the UI.
 *
 * ── Offline-First Save Flow ──
 * 1. Compress photo (if present) on IO dispatcher
 * 2. Build the [ActivityLogEntity] with `sync_status = 0` (PENDING)
 * 3. Insert to Room via [ActivityRepository] — **instant**, no network call
 * 4. Emit [Event.SaveSuccess] immediately so the UI navigates back
 * 5. [AutoSyncManager] schedules a background WorkManager job to push
 *    the un-synced record to Firestore + Laravel when network is available
 *
 * This ensures saving never fails due to connectivity and the user
 * always sees success feedback within milliseconds.
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

    // ── User State ──

    private val _userWeight = MutableStateFlow(70.0)
    val userWeight: StateFlow<Double> = _userWeight.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val uid: String get() = auth.currentUser?.uid ?: ""

    // ── Service Binding State ──
    // These flows mirror the service's live state. They persist across
    // composable dispose/re-compose cycles because the ViewModel is scoped
    // to the NavBackStackEntry, not the composable.

    private val _serviceBound = MutableStateFlow(false)
    val serviceBound: StateFlow<Boolean> = _serviceBound.asStateFlow()

    private var _service: LocationTrackingService? = null
    val service: LocationTrackingService? get() = _service

    // ── Proxied Tracking State ──
    // These are populated from the service's StateFlows via collector jobs.
    // When the service is unbound (e.g., brief window during rebind), the
    // last-known values are retained — no reset to zero.

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _timeSeconds = MutableStateFlow(0L)
    val timeSeconds: StateFlow<Long> = _timeSeconds.asStateFlow()

    private val _movingTimeSeconds = MutableStateFlow(0L)
    val movingTimeSeconds: StateFlow<Long> = _movingTimeSeconds.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()
    
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _currentPace = MutableStateFlow(0.0)
    val currentPace: StateFlow<Double> = _currentPace.asStateFlow()

    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private val _currentPoint = MutableStateFlow(LocationTrackingService.lastKnownPoint)
    val currentPoint: StateFlow<Pair<Double, Double>?> = _currentPoint.asStateFlow()

    private val _pathPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Double, Double>>> = _pathPoints.asStateFlow()

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    // ── Internal State ──
    private var collectorJob: Job? = null
    private var boundContext: Context? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            _service = (binder as LocationTrackingService.LocalBinder).getService()
            _serviceBound.value = true
            Log.d(TAG, "Service connected. isTracking=${_service?.isTracking?.value}")
            startCollectingServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Called only on unexpected disconnection (crash). NOT called on unbind.
            Log.w(TAG, "Service disconnected unexpectedly")
            _service = null
            _serviceBound.value = false
            collectorJob?.cancel()
        }
    }

    init {
        loadUserWeight()
    }

    /**
     * Bind to the LocationTrackingService. Should be called when the GPS
     * tracker composable enters composition. Safe to call multiple times —
     * will no-op if already bound.
     */
    fun bindService(context: Context) {
        if (_serviceBound.value) return
        boundContext = context.applicationContext
        val intent = Intent(context, LocationTrackingService::class.java)
        context.applicationContext.bindService(
            intent, serviceConnection, Context.BIND_AUTO_CREATE
        )
        Log.d(TAG, "Binding to service...")
    }

    /**
     * Unbind from the service. Called when the GPS tracker composable
     * leaves composition. The service continues running as a foreground
     * service — only the binding is released.
     *
     * NOTE: We do NOT reset the proxied state flows here. This is the key
     * to surviving backgrounding — the last-known values persist in the
     * ViewModel's StateFlows until the service is re-bound.
     */
    fun unbindService(context: Context) {
        if (!_serviceBound.value) return
        collectorJob?.cancel()
        collectorJob = null
        try {
            context.applicationContext.unbindService(serviceConnection)
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding service: ${e.message}")
        }
        _service = null
        _serviceBound.value = false
        Log.d(TAG, "Unbound from service. State retained in ViewModel.")
    }

    /**
     * Start coroutines that mirror the service's StateFlows into the
     * ViewModel's own StateFlows. This is the bridge that ensures
     * Compose UI always reads from ViewModel state (which persists)
     * rather than directly from the service (which may not be bound).
     */
    private fun startCollectingServiceState() {
        val svc = _service ?: return
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            // Launch parallel collectors for each state flow
            launch { svc.isTracking.collect { _isTracking.value = it } }
            launch { svc.isPaused.collect { _isPaused.value = it } }
            launch { svc.timeSeconds.collect { _timeSeconds.value = it } }
            launch { svc.movingTimeSeconds.collect { _movingTimeSeconds.value = it } }
            launch { svc.distanceKm.collect { _distanceKm.value = it } }
            launch { svc.steps.collect { _steps.value = it } }
            launch { svc.currentPace.collect { _currentPace.value = it } }
            launch { svc.isMoving.collect { _isMoving.value = it } }
            launch { svc.lastLocation.collect { _lastLocation.value = it } }
            launch { svc.currentPoint.collect { _currentPoint.value = it } }
            launch { svc.pathPoints.collect { _pathPoints.value = it } }
            launch { svc.trackingError.collect { _trackingError.value = it } }
        }
    }

    // ── Service Control Methods ──
    // These proxy commands to the service. Safe to call even if service
    // is not yet bound — they simply no-op.

    fun startTracking() { _service?.startTracking() }
    fun pauseTracking() { _service?.pauseTracking() }
    fun resumeTracking() { _service?.resumeTracking() }
    fun stopTracking() { _service?.stopTracking() }
    fun resetMetrics() { _service?.resetMetrics() }
    fun clearTrackingError() {
        _trackingError.value = null
        _service?.clearTrackingError()
    }

    /**
     * Snapshot the current service values directly (bypasses compose
     * recomposition lag). Used at save time for maximum accuracy.
     */
    fun snapshotTimeSeconds(): Long = _service?.timeSeconds?.value ?: _timeSeconds.value
    fun snapshotPace(): Double = _service?.currentPace?.value ?: _currentPace.value
    fun snapshotMovingTime(): Long = _service?.movingTimeSeconds?.value ?: _movingTimeSeconds.value
    fun snapshotSteps(): Int = _service?.steps?.value ?: _steps.value

    private fun loadUserWeight() {
        val currentUid = uid
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _userWeight.value = activityRepository.getUserWeight(currentUid)
            }
        }
    }

    // ── Save Workout (Offline-First) ──

    /**
     * Saves a workout log **locally** to Room and emits [Event.SaveSuccess].
     *
     * The entire save path is local-only:
     * 1. Photo compression runs on [Dispatchers.IO]
     * 2. Room insert is instantaneous (no network call)
     * 3. Success is emitted to the UI immediately
     * 4. Background sync is scheduled automatically by the repository
     *
     * This function will only fail on a local I/O error (extremely rare),
     * never on a network timeout or Firestore/API failure.
     */
    fun saveWorkout(
        context: Context,
        name: String,
        calories: Int,
        duration: String,
        dist: Double?,
        pace: Double?,
        movTime: Long?,
        steps: Int?,
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

                    // 2. Build entity (sync_status = 0 by default = PENDING)
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
                        steps = steps,
                        encodedPath = path,
                        mapType = mType,
                        photoUri = finalPhotoUri,
                        notes = note,
                        activityTag = tag
                        // syncStatus defaults to 0 (PENDING) — will be synced in background
                    )

                    // 3. Insert to Room ONLY — no Firestore, no API, no network
                    //    AutoSyncManager is triggered inside the repository to schedule
                    //    a background sync when connectivity is available.
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

    override fun onCleared() {
        super.onCleared()
        // ViewModel is being destroyed (e.g., NavBackStackEntry popped).
        // Unbind from service but DON'T stop it — it may still be tracking.
        collectorJob?.cancel()
        boundContext?.let { ctx ->
            try {
                ctx.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding on clear: ${e.message}")
            }
        }
        _service = null
        _serviceBound.value = false
    }

    companion object {
        private const val TAG = "LogWorkoutVM"

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
