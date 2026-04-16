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
 */
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        const val NOTIFICATION_CHANNEL_ID = "calorieko_workout_tracking"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.calorieko.app.ACTION_START_TRACKING"
        const val ACTION_STOP = "com.calorieko.app.ACTION_STOP_TRACKING"

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
                startForegroundWithNotification()
                startLocationUpdates()
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

        acquireWakeLock()
        startForegroundWithNotification()
        startLocationUpdates()
        startTimer()
    }

    fun pauseTracking() {
        _isPaused.value = true
        stopTimer()
        updateNotification("Workout Paused")
    }

    fun resumeTracking() {
        _isPaused.value = false
        lastMovementTimeMs = System.currentTimeMillis()
        startTimer()
        updateNotification("Tracking Workout...")
    }

    fun stopTracking() {
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
    }

    // ── Location Handling ──

    private fun handleLocationUpdate(location: Location) {
        // Update current point for map display — tighter filter to avoid visible drift
        if (location.accuracy < 20f) {
            _currentPoint.value = Pair(location.latitude, location.longitude)
        }

        if (!_isTracking.value || _isPaused.value) return

        // Strict accuracy gate: ignore garbage data
        if (location.accuracy > 25f) return

        val prevLocation = _lastLocation.value
        if (prevLocation != null) {
            val distanceToUpdate = prevLocation.distanceTo(location)
            val timeDeltaSec = (location.time - prevLocation.time) / 1000.0
            val calculatedSpeed = if (timeDeltaSec > 0) distanceToUpdate / timeDeltaSec else 0.0
            val physicalSpeed = if (location.hasSpeed()) location.speed else 0.0f

            // Check if standing still
            if (physicalSpeed < 0.25f && calculatedSpeed < 0.5) {
                _isMoving.value = false
            }

            // Stationary jitter filter — only process if moved > 5 meters
            if (distanceToUpdate >= 5.0f) {
                // Teleport/glitch check — max 12 m/s (~43 km/h)
                if (calculatedSpeed < 12.0) {
                    _isMoving.value = true
                    _distanceKm.value += distanceToUpdate / 1000.0
                    lastMovementTimeMs = System.currentTimeMillis()

                    _pathPoints.value = _pathPoints.value + Pair(location.latitude, location.longitude)
                    _lastLocation.value = location

                    if (_distanceKm.value > 0.02) {
                        val timeInMinutes = _movingTimeSeconds.value / 60.0
                        val rawPace = timeInMinutes / _distanceKm.value
                        _currentPace.value = rawPace.coerceAtMost(60.0)
                    }
                }
            }
        } else {
            // First tracking anchor point
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
            .setMinUpdateDistanceMeters(3.0f)  // Increased from 1.5m to reduce stationary drift
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    // ── Timer ──

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

                if (!_isTracking.value || _isPaused.value) continue

                _timeSeconds.value++

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
