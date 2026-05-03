package com.calorieko.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.calorieko.app.R
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
class LocationTrackingService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "LocationTrackingService"
        const val NOTIFICATION_CHANNEL_ID = "calorieko_workout_v4"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.calorieko.app.ACTION_START_TRACKING"
        const val ACTION_STOP = "com.calorieko.app.ACTION_STOP_TRACKING"

        // ── GPS Accuracy Thresholds ──

        /** Maximum accuracy (meters) for the map display dot. */
        private const val MAP_DISPLAY_ACCURACY = 25f

        /** Maximum accuracy (meters) for tracking distance/path points. */
        private const val TRACKING_ACCURACY = 25f

        /** Ideal accuracy (meters) for the initial GPS anchor point. */
        private const val ANCHOR_ACCURACY = 15f

        /** How long (ms) to buffer GPS readings before accepting an anchor.
         *  10 seconds gives the GPS module enough time to obtain a satellite fix
         *  instead of relying on cell tower triangulation or cached positions. */
        private const val WARM_UP_DURATION_MS = 10000L

        /** Maximum displacement (meters) allowed during post-warm-up stabilization.
         *  If the first few readings after warm-up show a jump larger than this,
         *  the anchor was wrong (cell tower → satellite transition). Re-anchor
         *  without adding distance. */
        private const val POST_WARMUP_MAX_JUMP = 50.0f

        /** Number of readings after warm-up that undergo stabilization checks. */
        private const val POST_WARMUP_STABILIZATION_COUNT = 3

        // ── GPS Smoothing (Moving Average) ──
        // GPS locations bounce randomly when stationary (jitter). By keeping a rolling
        // window of recent raw coordinates and calculating their average (centroid),
        // we completely neutralize the jitter. The smoothed point stays perfectly still.
        private const val SMOOTHING_WINDOW_SIZE = 6

        /** Maximum distance (meters) that can be added from a single GPS reading.
         *  Caps teleport glitches and cell-tower jumps that slip through other checks. */
        private const val MAX_SINGLE_READING_DISTANCE = 35.0f

        /** Global flag so Compose can quickly check if the service is alive. */
        @Volatile
        var isServiceRunning = false
            private set

        /** Static cache of the last known GPS coordinate so the ViewModel can
         *  seed its _currentPoint on recreation (survives ViewModel death). */
        @Volatile
        var lastKnownPoint: Pair<Double, Double>? = null
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
    
    /** The last hardware location received, used strictly to compute instantaneous speed. */
    private var lastRawLocation: Location? = null
    
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

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
    
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialStepCount = -1
    private var pausedStepsToSubtract = 0
    private var stepsAtPause = 0
    private var lastRecordedTotalSteps = 0

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
    private val recentPositions = mutableListOf<Location>()

    // ── Wall-Clock Timing for Speed Calculations ──
    private var lastUpdateWallClockMs = 0L

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
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
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
            else -> {
                // OS recreated the service via START_STICKY after killing it.
                // Re-promote to foreground immediately so ColorOS/MIUI don't
                // kill it again within seconds for lacking a notification.
                if (_isTracking.value) {
                    startForegroundWithNotification()
                    Log.w(TAG, "Service re-created by OS (START_STICKY). Re-promoted to foreground.")
                }
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

    /**
     * Called when the user swipes the app from the Recents screen.
     * On stock Android this is fine, but OPPO/ColorOS will destroy the
     * service entirely unless we explicitly re-schedule it.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (_isTracking.value) {
            Log.w(TAG, "onTaskRemoved — workout is active, re-scheduling service.")
            val restartIntent = Intent(applicationContext, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
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
        lastUpdateWallClockMs = 0L

        _steps.value = 0
        initialStepCount = -1
        pausedStepsToSubtract = 0
        stepsAtPause = 0
        lastRecordedTotalSteps = 0

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
        
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun pauseTracking() {
        _isPaused.value = true
        // Record when this pause started so we can subtract it from elapsed time
        pauseStartRealtimeMs = SystemClock.elapsedRealtime()
        stepsAtPause = lastRecordedTotalSteps
        // Timer thread stays alive — it computes time from wall-clock,
        // so _timeSeconds naturally freezes while paused.
        val formatted = DurationFormatter.formatDigital(_timeSeconds.value)
        val dist = "%.2f km".format(_distanceKm.value)
        val pace = _currentPace.value
        val paceStr = if (pace > 0.0 && pace <= 999.0) {
            val pMin = pace.toInt()
            val pSec = ((pace - pMin) * 60).toInt()
            "%d:%02d /km".format(pMin, pSec)
        } else {
            "-:-- /km"
        }
        updateNotificationWithStats(formatted, dist, paceStr, isPaused = true)
    }

    fun resumeTracking() {
        // Accumulate the pause duration into the total pause offset
        if (pauseStartRealtimeMs > 0L) {
            accumulatedPauseMs += SystemClock.elapsedRealtime() - pauseStartRealtimeMs
            pauseStartRealtimeMs = 0L
        }
        if (stepsAtPause > 0 && lastRecordedTotalSteps >= stepsAtPause) {
            pausedStepsToSubtract += (lastRecordedTotalSteps - stepsAtPause)
        }
        stepsAtPause = 0
        _isPaused.value = false
        lastMovementTimeMs = System.currentTimeMillis()
        updateNotificationWithStats(
            DurationFormatter.formatDigital(_timeSeconds.value),
            "%.2f km".format(_distanceKm.value),
            "-:-- /km"
        )
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
        sensorManager?.unregisterListener(this)
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
        recentPositions.clear()
        lastUpdateWallClockMs = 0L
    }

    // ── Location Handling ──

    private fun handleLocationUpdate(location: Location) {
        // _currentPoint is now only updated when movement is confirmed (below).
        // This prevents the camera from following GPS jitter while stationary,
        // which was causing the visible "blue dot drift" on the map.
        // Pre-tracking: still update for initial map centering.
        if (!_isTracking.value && location.accuracy < MAP_DISPLAY_ACCURACY) {
            _currentPoint.value = Pair(location.latitude, location.longitude)
            lastKnownPoint = _currentPoint.value
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

            // Keep the blue dot visible during warm-up by continuously updating
            // _currentPoint with every reasonably accurate reading.
            if (location.accuracy < MAP_DISPLAY_ACCURACY) {
                _currentPoint.value = Pair(location.latitude, location.longitude)
                lastKnownPoint = _currentPoint.value
            }

            if (System.currentTimeMillis() - gpsWarmUpStartMs > WARM_UP_DURATION_MS) {
                // Warm-up period over — select the best anchor.
                // Priority: readings with hasSpeed() (satellite fix indicator)
                // are preferred over those without (cell tower/cached readings).
                val satelliteReadings = warmUpLocations.filter { it.hasSpeed() }
                val candidatePool = satelliteReadings.ifEmpty { warmUpLocations }

                val bestLocation = candidatePool
                    .filter { it.accuracy <= ANCHOR_ACCURACY }
                    .minByOrNull { it.accuracy }
                    ?: candidatePool.minByOrNull { it.accuracy }

                if (bestLocation != null) {
                    isGpsWarmedUp = true
                    _isMoving.value = true
                    _pathPoints.value = listOf(Pair(bestLocation.latitude, bestLocation.longitude))
                    _lastLocation.value = bestLocation
                    _currentPoint.value = Pair(bestLocation.latitude, bestLocation.longitude)
                    lastKnownPoint = _currentPoint.value
                    lastMovementTimeMs = System.currentTimeMillis()
                    lastUpdateWallClockMs = System.currentTimeMillis()
                    warmUpLocations.clear()
                    recentPositions.clear()
                    for (i in 0 until SMOOTHING_WINDOW_SIZE) {
                        recentPositions.add(bestLocation)
                    }
                    Log.d(TAG, "GPS warm-up complete. Anchor accuracy: ${bestLocation.accuracy}m " +
                            "hasSpeed=${bestLocation.hasSpeed()} " +
                            "at (${bestLocation.latitude}, ${bestLocation.longitude})")
                } else {
                    // No usable reading yet — extend warm-up until we get one
                    Log.d(TAG, "GPS warm-up extended: no reading with accuracy < ${TRACKING_ACCURACY}m")
                }
            }
            return
        }

        // Strict accuracy gate: ignore garbage data
        if (location.accuracy > TRACKING_ACCURACY) {
            lastRawLocation = location
            return
        }

        val prevLocation = _lastLocation.value
        if (prevLocation != null) {
            val distanceFromAnchor = prevLocation.distanceTo(location)
            
            // Calculate TRUE instantaneous hardware speed to prevent jitter jumps after standing still.
            // Using prevLocation.time was a flaw because if you stood still for 60s, a 30m jitter
            // would calculate as 0.5m/s (valid walking) instead of the actual 15m/s jitter spike!
            val rawPrev = lastRawLocation ?: prevLocation
            val hardwareTimeDeltaSec = (location.time - rawPrev.time) / 1000.0
            val hardwareDistance = rawPrev.distanceTo(location)
            val instantaneousSpeedMps = if (hardwareTimeDeltaSec > 0) hardwareDistance / hardwareTimeDeltaSec else 0.0

            // ── 1. Teleport Glitch Prevention ──
            // If the jump is massive (>35m) OR the instantaneous speed implies impossible 
            // human speeds (>8m/s = 28km/h), it's a GPS glitch. Re-anchor WITHOUT adding distance.
            if (distanceFromAnchor > MAX_SINGLE_READING_DISTANCE || instantaneousSpeedMps > 8.0) {
                Log.d(TAG, "Teleport detected: Dist ${distanceFromAnchor.toInt()}m, Speed ${instantaneousSpeedMps.toInt()}m/s. Re-anchoring.")
                _lastLocation.value = location
                lastRawLocation = location
                
                // CRITICAL FIX: Flush the smoothing window! 
                // If we don't flush it, the old points will mix with the new teleported point,
                // causing the centroid to slowly "drag" across the map. This drag looks like 
                // valid movement and adds fake distance. By flushing it, the centroid instantly 
                // snaps to the new anchor, and the minimum-step filter will easily drop it.
                recentPositions.clear()
                for (i in 0 until SMOOTHING_WINDOW_SIZE) {
                    recentPositions.add(location)
                }
                
                return
            }

            // ── 2. Stationary Detection (Android Sensor) ──
            val reportedSpeed = if (location.hasSpeed()) location.speed.toDouble() else instantaneousSpeedMps
            if (reportedSpeed < 0.4 && distanceFromAnchor < 15.0f) {
                _isMoving.value = false
                lastRawLocation = location
                return
            }

            // ── 3. GPS Smoothing (Moving Average Filter) ──
            // Jitter bounces back and forth. By averaging the last raw points,
            // the resulting "centroid" completely stabilizes.
            // the smoothed location will not move, allowing the min-step filter below to drop it!
            recentPositions.add(location)
            if (recentPositions.size > SMOOTHING_WINDOW_SIZE) {
                recentPositions.removeAt(0)
            }
            
            val avgLat = recentPositions.map { it.latitude }.average()
            val avgLng = recentPositions.map { it.longitude }.average()
            
            val smoothedLocation = Location(location).apply {
                latitude = avgLat
                longitude = avgLng
            }
            
            // Recalculate distance using the SMOOTHED location
            val smoothedDistanceFromAnchor = prevLocation.distanceTo(smoothedLocation)

            // ── 4. Minimum Step Filter ──
            // Enforce a small step from the last anchor. Because the smoothed location
            // is perfectly stable when stationary, smoothedDistanceFromAnchor will be < 4.0m.
            if (smoothedDistanceFromAnchor < 4.0f) {
                _isMoving.value = false
                lastRawLocation = location
                return
            }

            // ── 5. Add Distance (Success!) ──
            // Update the map and path using the beautifully smoothed curve!
            _currentPoint.value = Pair(smoothedLocation.latitude, smoothedLocation.longitude)
            lastKnownPoint = _currentPoint.value
            _distanceKm.value += smoothedDistanceFromAnchor / 1000.0
            _isMoving.value = true
            lastMovementTimeMs = System.currentTimeMillis()

            if (_pathPoints.value.isEmpty()) {
                _pathPoints.value = listOf(Pair(prevLocation.latitude, prevLocation.longitude))
            }
            _pathPoints.value = _pathPoints.value + Pair(smoothedLocation.latitude, smoothedLocation.longitude)
            
            _lastLocation.value = smoothedLocation
            lastRawLocation = location
            lastUpdateWallClockMs = System.currentTimeMillis()

            // (Pace calculation moved to the timer thread to dynamically update using moving time)
            
            // Fallback for steps if hardware sensor hasn't fired yet
            if (initialStepCount == -1) {
                _steps.value = (_distanceKm.value * 1312.33).toInt()
            }
        } else {
            // Defensive fallback — warm-up should have set _lastLocation
            _isMoving.value = true
            _pathPoints.value = _pathPoints.value + Pair(location.latitude, location.longitude)
            _lastLocation.value = location
            _currentPoint.value = Pair(location.latitude, location.longitude)
            lastKnownPoint = _currentPoint.value
            lastMovementTimeMs = System.currentTimeMillis()
            lastUpdateWallClockMs = System.currentTimeMillis()
            lastRawLocation = location
            
            recentPositions.clear()
            for (i in 0 until SMOOTHING_WINDOW_SIZE) {
                recentPositions.add(location)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(0f)  // Fetch every point possible for smoothest Strava-like path
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

                // ── Compute Real-Time Strava-style Pace ──
                // Strava calculates Average Pace using MOVING TIME, not total elapsed time.
                // This prevents your pace from being ruined when waiting at a stoplight.
                // Because _movingTimeSeconds auto-pauses, your pace accurately reflects your active speed.
                if (_distanceKm.value > 0.01) {
                    val movingTimeInMinutes = _movingTimeSeconds.value / 60.0
                    if (movingTimeInMinutes > 0) {
                        val rawPace = movingTimeInMinutes / _distanceKm.value
                        _currentPace.value = rawPace.coerceIn(1.0, 999.0)
                    }
                }

                // Update notification with live stats every second (visible on lock screen)
                val formatted = DurationFormatter.formatDigital(_timeSeconds.value)
                val dist = "%.2f km".format(_distanceKm.value)
                val pace = _currentPace.value
                val paceStr = if (pace > 0.0 && pace <= 999.0) {
                    val pMin = pace.toInt()
                    val pSec = ((pace - pMin) * 60).toInt()
                    "%d:%02d /km".format(pMin, pSec)
                } else {
                    "-:-- /km"
                }
                updateNotificationWithStats(formatted, dist, paceStr)
            }
        }.apply {
            name = "CalorieKo-Timer"
            // CRITICAL: Do NOT use isDaemon = true!
            // Daemon threads are killed immediately when the process is
            // being considered for termination. A non-daemon thread signals
            // the JVM that this thread is doing meaningful work, giving the
            // timer (and therefore the service) a better survival chance
            // on aggressive OEMs like OPPO/ColorOS and Xiaomi/MIUI.
            isDaemon = false
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

        // Delete legacy channel name (one-time cleanup)
        manager.deleteNotificationChannel("calorieko_tracking")

        // Only create if it doesn't already exist — avoids wiping user settings
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Workout Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live workout stats while CalorieKo is tracking"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Build a standard notification with live workout data.
     *
     * ── Lock Screen Strategy ──
     * Realme/OPPO/Vivo lock screens hide `contentText` but always display
     * `contentTitle`. So we put the TIMER directly in the title:
     *   Title:   "⏱ 05:32 — 1.24 km"
     *   Content: "Pace: 5:12 /km • Tracking Workout"
     *
     * This ensures the runner can see their time at a glance on the lock
     * screen without unlocking or expanding the notification.
     */
    private fun buildNotification(
        time: String,
        distance: String,
        pace: String,
        isPaused: Boolean = false
    ): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Put the timer in the TITLE — this is always visible on lock screen
        val title = if (isPaused) {
            "⏸ $time — $distance (Paused)"
        } else {
            "⏱ $time — $distance"
        }
        val content = "Pace: $pace • CalorieKo Workout"

        // Explicit public version for lock screens
        val publicNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .build()

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicNotification)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setShowWhen(false)
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(
            time = "00:00",
            distance = "0.00 km",
            pace = "-:-- /km"
        )
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    private fun updateNotificationWithStats(
        time: String,
        distance: String,
        pace: String,
        isPaused: Boolean = false
    ) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(time, distance, pace, isPaused))
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

    // ── SensorEventListener ──
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            lastRecordedTotalSteps = totalSteps
            if (initialStepCount == -1) {
                // If we already accumulated some steps via GPS fallback, subtract them so we don't reset to 0
                initialStepCount = totalSteps - _steps.value
            }
            if (_isTracking.value && !_isPaused.value) {
                _steps.value = totalSteps - initialStepCount - pausedStepsToSubtract
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }
}
