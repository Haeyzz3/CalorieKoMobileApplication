package com.calorieko.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LocationTrackingService — a proper Android **Foreground Service** that keeps
 * GPS tracking alive even when the screen is off or the app is in the background.
 *
 * ── Why a Foreground Service? ──
 * Without this, Android's battery optimizations (Doze mode, app standby) will
 * kill the location callbacks within ~5 minutes of the screen turning off.
 * A foreground service with a persistent notification tells the OS that the user
 * explicitly started a time-sensitive task that should not be interrupted.
 *
 * ── Architecture ──
 * The service uses a bound pattern via [LocalBinder] so the Compose UI can
 * directly observe the location state flows and send commands (start/pause/stop).
 * It also holds a partial [WakeLock] so the CPU stays awake for timer increments
 * even when the screen is off.
 *
 * ── Timer Design ──
 * The elapsed duration is computed from [SystemClock.elapsedRealtime()] anchored
 * at tracking start time, minus accumulated pause durations. This is immune to
 * Thread.sleep drift and provides wall-clock accuracy regardless of CPU scheduling.
 *
 * ── GPS Warm-Up ──
 * The first few seconds of GPS data are buffered before selecting the most accurate
 * reading as the route anchor point. This prevents stale cell-tower or cached locations
 * from causing coordinate displacement (e.g., plotting the route at the wrong location).
 */
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        const val NOTIFICATION_CHANNEL_ID = "calorieko_workout_tracking"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.calorieko.app.ACTION_START_TRACKING"
        const val ACTION_STOP = "com.calorieko.app.ACTION_STOP_TRACKING"

        // ── GPS Accuracy Thresholds ──

        /** Maximum accuracy (meters) for the map display dot. */
        private const val MAP_DISPLAY_ACCURACY = 20f

        /** Maximum accuracy (meters) for tracking distance/path points. */
        private const val TRACKING_ACCURACY = 20f

        /** Ideal accuracy (meters) for the initial GPS anchor point. */
        private const val ANCHOR_ACCURACY = 12f

        /** How long (ms) to buffer GPS readings before accepting an anchor. */
        private const val WARM_UP_DURATION_MS = 5000L

        // ── Stationary Jitter Suppression ──

        /** GPS Doppler speed (m/s) below which the user is considered stationary.
         *  0.5 m/s ≈ 1.8 km/h — provides margin for Doppler noise (0-0.4 m/s
         *  while stationary) while staying well below any walking pace (≥1.0 m/s). */
        private const val STATIONARY_SPEED_THRESHOLD = 0.5f

        /** Position-derived speed (m/s) fallback threshold when no Doppler speed
         *  is available. Stricter than the Doppler threshold because position-derived
         *  speed is inherently noisy from GPS jitter. */
        private const val STATIONARY_CALC_SPEED_THRESHOLD = 0.4

        /** Number of consecutive stationary readings before entering "locked" mode. */
        private const val STATIONARY_LOCK_COUNT = 3

        /** Displacement (meters) required to break out of locked-stationary mode.
         *  Must be large enough that no single GPS jitter spike can trigger it. */
        private const val LOCKED_STATIONARY_MIN_DISPLACEMENT = 10.0f

        /** Global flag so Compose can quickly check if the service is alive. */
        @Volatile
        var isServiceRunning = false
            private set
    }

    // ── Binder for Compose UI ──
    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    private val binder = LocalBinder()

    // ── Location State (observable by the UI) ──
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    /** Latest high-accuracy GPS coordinate for map rendering. */
    private val _currentPoint = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentPoint: StateFlow<Pair<Double, Double>?> = _currentPoint.asStateFlow()

    /** Accumulated route points as (lat, lng) pairs. */
    private val _pathPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Double, Double>>> = _pathPoints.asStateFlow()

    // ── Metrics ──
    private val _timeSeconds = MutableStateFlow(0L)
    val timeSeconds: StateFlow<Long> = _timeSeconds.asStateFlow()

    private val _movingTimeSeconds = MutableStateFlow(0L)
    val movingTimeSeconds: StateFlow<Long> = _movingTimeSeconds.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _currentPace = MutableStateFlow(0.0)
    val currentPace: StateFlow<Double> = _currentPace.asStateFlow()

    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()

    // ── Internal State ──
    private var lastMovementTimeMs = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerThread: Thread? = null
    private var isTimerRunning = false

    // ── Stationary Detection ──
    // Tracks how many consecutive GPS readings indicated the user is stationary.
    // After STATIONARY_LOCK_COUNT readings, we enter "locked stationary" mode
    // which requires a much larger displacement to break out, preventing
    // GPS jitter spikes from registering as real movement.
    private var consecutiveStationaryCount = 0

    // ── Wall-Clock Timer Anchors ──
    // Using SystemClock.elapsedRealtime() ensures the timer is immune to
    // Thread.sleep drift. The total elapsed time is:
    //   (now - trackingStartRealtimeMs - totalPauseMs) / 1000
    private var trackingStartRealtimeMs = 0L
    private var accumulatedPauseMs = 0L
    private var pauseStartRealtimeMs = 0L

    // ── GPS Warm-Up State ──
    // The first few seconds after GPS activation often yield low-accuracy readings
    // from cell towers or cached locations. We buffer readings during the warm-up
    // window and select the most accurate one as the route anchor point.
    private var isGpsWarmedUp = false
    private var gpsWarmUpStartMs = 0L
    private val warmUpLocations = mutableListOf<Location>()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                handleLocationUpdate(location)
            }
        }
    }

    // ── Service Lifecycle ──

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }
            ACTION_START -> {
                // Only start the foreground notification here.
                // Location updates and timer are started by startTracking()
                // (called from the bound UI) to prevent duplicate registration.
                startForegroundWithNotification()
            }
        }
        // START_STICKY: OS will re-create the service if it's killed.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        isServiceRunning = false
        stopTracking()
        super.onDestroy()
    }

    // ── Public API (called from Compose via binder) ──

    fun startTracking() {
        _isTracking.value = true
        _isPaused.value = false
        _timeSeconds.value = 0L
        _movingTimeSeconds.value = 0L
        _distanceKm.value = 0.0
        _currentPace.value = 0.0
        _lastLocation.value = null
        _pathPoints.value = emptyList()
        _isMoving.value = false
        lastMovementTimeMs = System.currentTimeMillis()
        consecutiveStationaryCount = 0

        // Anchor wall-clock for accurate duration tracking
        trackingStartRealtimeMs = SystemClock.elapsedRealtime()
        accumulatedPauseMs = 0L
        pauseStartRealtimeMs = 0L

        // Reset GPS warm-up: buffer initial readings before accepting an anchor
        isGpsWarmedUp = false
        gpsWarmUpStartMs = System.currentTimeMillis()
        warmUpLocations.clear()

        acquireWakeLock()
        startForegroundWithNotification()
        startLocationUpdates()
        startTimer()
    }

    fun pauseTracking() {
        _isPaused.value = true
        // Record when this pause started so we can subtract it from elapsed time
        pauseStartRealtimeMs = SystemClock.elapsedRealtime()
        // Timer thread stays alive — it computes time from wall-clock,
        // so _timeSeconds naturally freezes while paused.
        updateNotification("Workout Paused")
    }

    fun resumeTracking() {
        // Accumulate the pause duration into the total pause offset
        if (pauseStartRealtimeMs > 0L) {
            accumulatedPauseMs += SystemClock.elapsedRealtime() - pauseStartRealtimeMs
            pauseStartRealtimeMs = 0L
        }
        _isPaused.value = false
        lastMovementTimeMs = System.currentTimeMillis()
        updateNotification("Tracking Workout...")
    }

    fun stopTracking() {
        // Finalize time one last time from wall-clock to ensure the saved value
        // is authoritative and matches what was displayed on screen.
        if (_isTracking.value && trackingStartRealtimeMs > 0L) {
            val now = SystemClock.elapsedRealtime()
            val currentPauseMs = if (_isPaused.value && pauseStartRealtimeMs > 0L) {
                now - pauseStartRealtimeMs
            } else 0L
            val totalElapsedMs = now - trackingStartRealtimeMs - accumulatedPauseMs - currentPauseMs
            _timeSeconds.value = (totalElapsedMs / 1000L).coerceAtLeast(0L)
        }

        _isTracking.value = false
        _isPaused.value = false
        stopTimer()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /** Reset all metrics to initial state. */
    fun resetMetrics() {
        _timeSeconds.value = 0L
        _movingTimeSeconds.value = 0L
        _distanceKm.value = 0.0
        _currentPace.value = 0.0
        _lastLocation.value = null
        _pathPoints.value = emptyList()
        _isMoving.value = false
        isGpsWarmedUp = false
        warmUpLocations.clear()
        consecutiveStationaryCount = 0
    }

    // ── Location Handling ──

    private fun handleLocationUpdate(location: Location) {
        // _currentPoint is now only updated when movement is confirmed (below).
        // This prevents the camera from following GPS jitter while stationary,
        // which was causing the visible "blue dot drift" on the map.
        // Pre-tracking: still update for initial map centering.
        if (!_isTracking.value && location.accuracy < MAP_DISPLAY_ACCURACY) {
            _currentPoint.value = Pair(location.latitude, location.longitude)
        }

        if (!_isTracking.value || _isPaused.value) return

        // ── GPS Warm-Up Phase ──
        // Buffer readings for the first few seconds to let the GPS module lock on
        // to satellites. The initial readings often use cell tower triangulation or
        // a stale cached location, which can be hundreds of meters away from the
        // actual position (explains the SPC → Ritz Hotel displacement).
        if (!isGpsWarmedUp) {
            if (location.accuracy < TRACKING_ACCURACY) {
                warmUpLocations.add(location)
            }

            if (System.currentTimeMillis() - gpsWarmUpStartMs > WARM_UP_DURATION_MS) {
                // Warm-up period over — select the most accurate reading as anchor
                val bestLocation = warmUpLocations
                    .filter { it.accuracy <= ANCHOR_ACCURACY }
                    .minByOrNull { it.accuracy }
                    ?: warmUpLocations.minByOrNull { it.accuracy }

                if (bestLocation != null) {
                    isGpsWarmedUp = true
                    _isMoving.value = true
                    _pathPoints.value = listOf(Pair(bestLocation.latitude, bestLocation.longitude))
                    _lastLocation.value = bestLocation
                    lastMovementTimeMs = System.currentTimeMillis()
                    warmUpLocations.clear()
                    Log.d(TAG, "GPS warm-up complete. Anchor accuracy: ${bestLocation.accuracy}m " +
                            "at (${bestLocation.latitude}, ${bestLocation.longitude})")
                } else {
                    // No usable reading yet — extend warm-up until we get one
                    Log.d(TAG, "GPS warm-up extended: no reading with accuracy < ${TRACKING_ACCURACY}m")
                }
            }
            return
        }

        // Strict accuracy gate: ignore garbage data
        if (location.accuracy > TRACKING_ACCURACY) return

        val prevLocation = _lastLocation.value
        if (prevLocation != null) {
            val distanceToUpdate = prevLocation.distanceTo(location)
            val timeDeltaSec = (location.time - prevLocation.time) / 1000.0
            val calculatedSpeed = if (timeDeltaSec > 0) distanceToUpdate / timeDeltaSec else 0.0

            // ── Stationary Detection (Hard Gate) ──
            // Use GPS Doppler speed as the PRIMARY indicator when available.
            // Doppler speed is derived from satellite frequency shift, which is
            // far more reliable than position-derived speed for detecting stationarity.
            // CRITICAL: Do NOT use AND logic — GPS speed noise alone (0.4 m/s while
            // stationary) could bypass one check, and calculatedSpeed (noisy from
            // position jitter) could bypass the other. Use INDEPENDENT checks.
            val isLikelyStationary = if (location.hasSpeed()) {
                // Doppler speed available: trust it as sole indicator
                location.speed < STATIONARY_SPEED_THRESHOLD
            } else {
                // No hardware speed: fall back to position-derived speed
                // with a strict threshold (0.4 m/s) since it's inherently noisy
                calculatedSpeed < STATIONARY_CALC_SPEED_THRESHOLD
            }

            if (isLikelyStationary) {
                consecutiveStationaryCount++
                _isMoving.value = false

                // ── Stale Anchor Prevention ──
                // Update _lastLocation to the current (jittered) position.
                // This prevents the "ratcheting" problem where jitter accumulates
                // relative to a fixed old anchor until it exceeds the displacement
                // threshold, adding phantom distance.
                _lastLocation.value = location
                return  // ← Hard gate: do NOT process displacement while stationary
            }

            // ── Displacement Check ──
            // The speed gate above already confirmed the user is NOT stationary.
            // Now we apply a moderate displacement filter to absorb residual noise.
            // Key insight (Strava-aligned): once speed confirms movement, use a LOW
            // displacement threshold — walking at 1.4 m/s × 3s interval = ~4.2m,
            // so the threshold must be BELOW that to count real walking segments.
            val isLockedStationary = consecutiveStationaryCount >= STATIONARY_LOCK_COUNT
            val minDisplacement = if (isLockedStationary) {
                LOCKED_STATIONARY_MIN_DISPLACEMENT
            } else {
                // Normal movement: 3m minimum absorbs sub-step GPS noise
                // while accepting real walking displacement (~4m per reading).
                3.0f
            }

            if (distanceToUpdate >= minDisplacement && calculatedSpeed < 12.0) {
                // Confirmed real movement — reset stationary counter
                consecutiveStationaryCount = 0
                _isMoving.value = true
                _distanceKm.value += distanceToUpdate / 1000.0
                lastMovementTimeMs = System.currentTimeMillis()

                _pathPoints.value = _pathPoints.value + Pair(location.latitude, location.longitude)
                _lastLocation.value = location

                // Update camera follow position ONLY on confirmed movement.
                // This prevents the map from chasing GPS jitter while stationary.
                _currentPoint.value = Pair(location.latitude, location.longitude)

                // Calculate pace using TOTAL elapsed time, not just moving time.
                // Total elapsed time gives the correct average pace that matches
                // what fitness apps like Strava display as "overall pace".
                if (_distanceKm.value > 0.02) {
                    val timeInMinutes = _timeSeconds.value / 60.0
                    if (timeInMinutes > 0) {
                        val rawPace = timeInMinutes / _distanceKm.value
                        _currentPace.value = rawPace.coerceIn(1.0, 60.0)
                    }
                }
            } else {
                // Below threshold — treat as not moving.
                // If locked stationary, update anchor to prevent ratcheting.
                // If normal, keep anchor at last confirmed movement point.
                if (isLockedStationary) {
                    _lastLocation.value = location
                }
                _isMoving.value = false
            }
        } else {
            // Defensive fallback — warm-up should have set _lastLocation
            _isMoving.value = true
            _pathPoints.value = _pathPoints.value + Pair(location.latitude, location.longitude)
            _lastLocation.value = location
            lastMovementTimeMs = System.currentTimeMillis()
        }
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(2.0f)  // Reduced to 2.0m to receive more frequent updates and prevent sparse OS batching
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    // ── Timer ──

    /**
     * Starts a background thread that updates [_timeSeconds] every second.
     *
     * Unlike the previous implementation that incremented a counter (susceptible
     * to Thread.sleep drift), this computes elapsed time directly from
     * [SystemClock.elapsedRealtime], ensuring wall-clock accuracy. The timer
     * thread stays alive during pauses so the UI can observe the frozen time.
     */
    private fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true

        timerThread = Thread {
            while (isTimerRunning) {
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    break
                }

                if (!_isTracking.value) continue

                // ── Compute elapsed time from wall-clock ──
                // This is immune to Thread.sleep drift and gives consistent
                // results regardless of CPU scheduling or Doze mode timing.
                val now = SystemClock.elapsedRealtime()
                val currentPauseMs = if (_isPaused.value && pauseStartRealtimeMs > 0L) {
                    now - pauseStartRealtimeMs
                } else 0L
                val totalElapsedMs = now - trackingStartRealtimeMs - accumulatedPauseMs - currentPauseMs
                _timeSeconds.value = (totalElapsedMs / 1000L).coerceAtLeast(0L)

                if (_isPaused.value) continue

                // Auto-pause moving time if no location update in 3.5s
                if (System.currentTimeMillis() - lastMovementTimeMs > 3500L) {
                    _isMoving.value = false
                }

                if (_isMoving.value) {
                    _movingTimeSeconds.value++
                }

                // Update notification with live timer every second (visible on lock screen)
                val formatted = DurationFormatter.formatDigital(_timeSeconds.value)
                val dist = "%.2f km".format(_distanceKm.value)
                updateNotification("$formatted • $dist")
            }
        }.apply {
            name = "CalorieKo-Timer"
            isDaemon = true
            start()
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerThread?.interrupt()
        timerThread = null
    }

    // ── Notification ──

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)

        // Delete old channel (it used IMPORTANCE_LOW which hid the notification)
        manager.deleteNotificationChannel("calorieko_tracking")

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Workout Tracking",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows when CalorieKo is actively tracking your workout"
            setShowBadge(false)
            setSound(null, null)  // No sound despite DEFAULT importance
            enableVibration(false)
        }

        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        // Intent to open the app when the notification is tapped
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action for the notification
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CalorieKo • Tracking Workout")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Tracking Workout...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    private fun updateNotification(text: String) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(text))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification: ${e.message}")
        }
    }

    // ── WakeLock ──

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CalorieKo::WorkoutTrackingWakeLock"
            ).apply {
                // 4 hour max timeout as a safety net
                acquire(4 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}
