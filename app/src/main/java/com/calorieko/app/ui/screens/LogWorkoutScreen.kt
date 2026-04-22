package com.calorieko.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.calorieko.app.viewmodel.LogWorkoutViewModel
import androidx.compose.runtime.collectAsState
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// --- Data Models ---
data class ActivityItem(val id: String, val name: String, val category: String, val met: Double)

val ACTIVITIES = listOf(
    ActivityItem("1", "Walking, slow pace (2.0 mph)", "Walking", 2.0),
    ActivityItem("2", "Walking, moderate pace (3.0 mph)", "Walking", 3.5),
    ActivityItem("3", "Walking, brisk pace (4.0 mph)", "Walking", 5.0),
    ActivityItem("4", "Jogging, general", "Running", 7.0),
    ActivityItem("5", "Running, 5 mph (12 min/mile)", "Running", 8.3),
    ActivityItem("6", "Running, 6 mph (10 min/mile)", "Running", 9.8),
    ActivityItem("7", "Gardening, general", "Household", 4.0),
    ActivityItem("8", "House cleaning, general", "Household", 3.3),
    ActivityItem("15", "Bicycling, leisure", "Cycling", 6.8),
    ActivityItem("19", "Yoga, Hatha", "Exercise", 2.5),
    ActivityItem("20", "Dancing, general", "Exercise", 4.5), // Added: Filipino Gawaing Bahay
    ActivityItem("21", "Pagwawalis (Sweeping)", "Household", 3.3),
    ActivityItem("22", "Paglalampaso (Floor scrubbing with Bunot)", "Household", 3.8),
    ActivityItem("23", "Paglalaba (Hand-washing clothes)", "Household", 3.0),
    ActivityItem("24", "Pagiigib (Fetching water)", "Household", 5.0),
)

val OUTDOOR_ACTIVITIES = listOf(
    ActivityItem("gps1", "Run", "Running", 9.8),
    ActivityItem("gps2", "Walk", "Walking", 3.5),
    ActivityItem("gps3", "Cycling", "Cycling", 8.0),
    ActivityItem("gps4", "Trail Running", "Running", 10.0),
    ActivityItem("gps5", "Hike", "Walking", 6.0),
    ActivityItem("gps6", "Mountain Bike", "Cycling", 8.5)
)

enum class WorkoutMode { SELECTION, MANUAL, GPS }

// Helper function to get sport-specific icons
fun getActivityIcon(activity: ActivityItem): ImageVector {
    return when (activity.id) {
        "gps1" -> Icons.AutoMirrored.Filled.DirectionsRun   // Run
        "gps2" -> Icons.AutoMirrored.Filled.DirectionsWalk   // Walk
        "gps3" -> Icons.AutoMirrored.Filled.DirectionsBike  // Cycling
        "gps4" -> Icons.Default.Landscape            // Trail Running
        "gps5" -> Icons.Default.Hiking               // Hike
        "gps6" -> Icons.AutoMirrored.Filled.DirectionsBike  // Mountain Bike
        else -> Icons.Default.FitnessCenter
    }
}

@Composable
fun LogWorkoutScreen(
    viewModel: LogWorkoutViewModel,
    activityIdToEdit: Int? = null,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(WorkoutMode.SELECTION) }

    val context = LocalContext.current

    // Auto-resume: if the tracking service is still running in background
    // (user pressed back during active tracking), jump to GPS mode on re-entry
    LaunchedEffect(Unit) {
        if (com.calorieko.app.util.LocationTrackingService.isServiceRunning) {
            mode = WorkoutMode.GPS
        }
    }

    // ── Collect ViewModel State ──
    val userWeight by viewModel.userWeight.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // ── Handle one-shot events ──
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LogWorkoutViewModel.Event.SaveSuccess -> onBack()
                is LogWorkoutViewModel.Event.SaveError -> { /* Could show a toast here */ }
            }
        }
    }

    val saveWorkout: (String, Int, String, Double?, Double?, Long?, String?, String?, String?, String?, String?) -> Unit = { name, calories, duration, dist, pace, movTime, path, mType, pUri, note, tag ->
        viewModel.saveWorkout(context, name, calories, duration, dist, pace, movTime, path, mType, pUri, note, tag)
    }

    fun handleBack() {
        if (mode == WorkoutMode.SELECTION) {
            onBack()
        } else {
            mode = WorkoutMode.SELECTION
        }
    }

    Scaffold(
        topBar = {
            if (mode != WorkoutMode.GPS) {
                Surface(color = Color.White, shadowElevation = 1.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { handleBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = when (mode) { WorkoutMode.SELECTION -> "Log Workout"; WorkoutMode.MANUAL -> "Lifestyle Activities"; else -> "" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AnimatedContent(targetState = mode, label = "ModeTransition") { targetMode ->
                when (targetMode) {
                    WorkoutMode.SELECTION -> ModeSelectionContent(onSelectManual = { mode = WorkoutMode.MANUAL }, onSelectGPS = { mode = WorkoutMode.GPS })
                    WorkoutMode.MANUAL -> ManualMETsContent(userWeight = userWeight, onSave = { name, cals, dur -> saveWorkout(name, cals, dur, null, null, null, null, null, null, null, null) })
                    WorkoutMode.GPS -> GPSTrackerContent(userWeight = userWeight, viewModel = viewModel, onSave = saveWorkout, onBack = { mode = WorkoutMode.SELECTION }, onBackToDashboard = onBack)
                }
            }

            // --- Saving Overlay ---
            AnimatedVisibility(
                visible = isSaving,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = CalorieKoGreen)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Saving Workout...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 1. Mode Selection Screen ---
@Composable
fun ModeSelectionContent(onSelectManual: () -> Unit, onSelectGPS: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Choose how you'd like to track your workout", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 24.dp))
        WorkoutSelectionCard(title = "Lifestyle Activities", description = "Log daily activities and household chores", icon = Icons.Default.Person, tags = listOf("Gardening", "Walking", "Cleaning"), onClick = onSelectManual)
        Spacer(modifier = Modifier.height(16.dp))
        WorkoutSelectionCard(title = "Outdoor Workout", description = "Track runs, walks, and cycling with GPS", icon = Icons.Default.LocationOn, tags = listOf("Run", "Cycle", "Hike", "Trail"), onClick = onSelectGPS)
        Spacer(modifier = Modifier.height(32.dp))
        Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFDBEAFE))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.FitnessCenter, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Track calories burned", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Text("Your workout data syncs with your daily calorie balance automatically.", fontSize = 12.sp, color = Color(0xFF1E40AF), lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun WorkoutSelectionCard(title: String, description: String, icon: ImageVector, tags: List<String>, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(56.dp).background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CalorieKoOrange, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Text(description, fontSize = 13.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    tags.forEach { tag ->
                        Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(50)) {
                            Text(tag, fontSize = 11.sp, color = Color(0xFF4B5563), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Manual METs Screen ---
@Composable
fun ManualMETsContent(userWeight: Double, onSave: (String, Int, String) -> Unit) {
    // (Unchanged from previous code)
    var searchQuery by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var durationText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val filteredActivities = remember(searchQuery) { ACTIVITIES.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) } }
    val caloriesBurned = remember(selectedActivity, durationText) {
        val duration = durationText.toDoubleOrNull() ?: 0.0
        val met = selectedActivity?.met ?: 0.0
        (met * userWeight * (duration / 60.0)).roundToInt()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
            if (selectedActivity == null) {
                item { OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search activities (e.g. Walking)") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = CalorieKoOrange)) }
                items(filteredActivities) { activity ->
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedActivity = activity }) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text(activity.name, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)); Text(activity.category, fontSize = 12.sp, color = Color.Gray) }
                            Text("${activity.met} MET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CalorieKoOrange, maxLines = 1, softWrap = false)
                        }
                    }
                }
            } else {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text(selectedActivity!!.name, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(selectedActivity!!.category, fontSize = 13.sp, color = Color.Gray) }
                                TextButton(onClick = { selectedActivity = null }) { Text("Change", color = CalorieKoOrange, maxLines = 1, softWrap = false) }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, tint = CalorieKoOrange, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("${selectedActivity!!.met} MET", fontSize = 12.sp, color = CalorieKoOrange, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Duration (minutes)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = durationText, onValueChange = { durationText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CalorieKoOrange, unfocusedBorderColor = Color(0xFFE5E7EB)), placeholder = { Text("e.g. 30") }, leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = Color.Gray) })
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (caloriesBurned > 0) {
                        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))), RoundedCornerShape(16.dp)).padding(24.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Estimated Burn", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("$caloriesBurned", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("$durationText minutes • ${userWeight}kg body weight", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        if (selectedActivity != null && durationText.isNotEmpty()) {
            Surface(shadowElevation = 8.dp) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp)) {
                    Button(onClick = { isSaving = true; onSave(selectedActivity!!.name, caloriesBurned, "$durationText min") }, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange)) {
                        Icon(Icons.Default.LocalFireDepartment, null); Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isSaving) "Saving..." else "Log $caloriesBurned Calories", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 3. ADVANCED GPS TRACKER (backed by Foreground Service) ---

@Composable
fun GPSTrackerContent(userWeight: Double, viewModel: LogWorkoutViewModel, onSave: (String, Int, String, Double?, Double?, Long?, String?, String?, String?, String?, String?) -> Unit, onBack: () -> Unit, onBackToDashboard: () -> Unit) {
    val context = LocalContext.current

    // ── Service Binding via ViewModel ──
    // The ViewModel manages the service connection and survives Activity recreation.
    // On re-composition (e.g., returning from background), it re-binds to the
    // still-running foreground service and picks up the live state.
    DisposableEffect(Unit) {
        viewModel.bindService(context)
        onDispose {
            // Only unbind — do NOT stop the service if tracking is still active
            viewModel.unbindService(context)
        }
    }

    // ── Collect all tracking state from the ViewModel ──
    // These persist even when the service is temporarily unbound (backgrounding)
    // because the ViewModel retains the last-known values.
    val isTracking by viewModel.isTracking.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val timeSeconds by viewModel.timeSeconds.collectAsState()
    val movingTimeSeconds by viewModel.movingTimeSeconds.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val currentPace by viewModel.currentPace.collectAsState()
    val isMoving by viewModel.isMoving.collectAsState()
    val lastLocation by viewModel.lastLocation.collectAsState()

    // Convert service's (lat,lng) pairs to Mapbox Points for map rendering
    val servicePathPoints by viewModel.pathPoints.collectAsState()
    val pathPoints = remember(servicePathPoints) {
        servicePathPoints.map { (lat, lng) -> Point.fromLngLat(lng, lat) }
    }

    val serviceCurrentPoint by viewModel.currentPoint.collectAsState()
    val currentPoint = remember(serviceCurrentPoint) {
        serviceCurrentPoint?.let { (lat, lng) -> Point.fromLngLat(lng, lat) }
    }

    // Photo and Map Expanded States
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showExpandedMap by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedPhotoUri = uri
    }

    // Summary Screen Dropdown States
    var showSummarySportDropdown by remember { mutableStateOf(false) }
    var showSummaryTagDropdown by remember { mutableStateOf(false) }
    var showSummaryMapTypeDropdown by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("") }
    val activityTags = listOf("None", "For a Cause", "Workout", "Race", "Recovery")

    // UI State
    var showSummary by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf(OUTDOOR_ACTIVITIES[0]) } // Default Run
    var showLayerMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Save Activity State
    var activityTitle by remember { mutableStateOf("") }
    var privateNotes by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val gpsFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var showLocationDialog by remember { mutableStateOf(false) }

    // Map Settings
    var mapType by remember { mutableStateOf("Dark") } // Dark, Standard, Terrain
    var isCompassMode by remember { mutableStateOf(false) } // False = Center/Birds Eye, True = Forward Rotation
    var followUser by remember { mutableStateOf(true) }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    // POST_NOTIFICATIONS runtime permission (required on Android 13+ / API 33+).
    // Without this, the foreground service notification is completely invisible.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether granted or denied, start the service — it will still run,
        // but the notification will only be visible if permission was granted.
        val intent = android.content.Intent(context, com.calorieko.app.util.LocationTrackingService::class.java).apply {
            action = com.calorieko.app.util.LocationTrackingService.ACTION_START
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
        viewModel.startTracking()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            // Also verify GPS/Location services are actually turned on
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                showLocationDialog = true
            } else {
                // On Android 13+, also request notification permission before starting the service
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    val intent = android.content.Intent(context, com.calorieko.app.util.LocationTrackingService::class.java).apply {
                        action = com.calorieko.app.util.LocationTrackingService.ACTION_START
                    }
                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                    viewModel.startTracking()
                }
            }
        }
    }

    // ── Proactive Offline Map Caching ──
    var hasTriggeredOfflineCache by remember { mutableStateOf(false) }
    LaunchedEffect(currentPoint) {
        if (currentPoint != null && !hasTriggeredOfflineCache) {
            hasTriggeredOfflineCache = true
            com.calorieko.app.util.OfflineMapManager.downloadRegion(
                context = context,
                centerLat = currentPoint!!.latitude(),
                centerLng = currentPoint!!.longitude(),
                radiusKm = 5.0
            )
        }
    }

    // NOTE: Timer and location updates are now handled entirely by LocationTrackingService.
    // The LaunchedEffect timer and DisposableEffect locationCallback that were here before
    // have been removed. The service handles:
    //   - Timer incrementing (timeSeconds, movingTimeSeconds)
    //   - GPS location callbacks (pathPoints, distanceKm, currentPace)
    //   - WakeLock acquisition (keeps CPU alive when screen is off)
    //   - Foreground notification (prevents OS from killing the service)

    val formatTime = { seconds: Long ->
        com.calorieko.app.util.DurationFormatter.formatDigital(seconds)
    }

    val hours = movingTimeSeconds / 3600.0
    val caloriesBurned = if (movingTimeSeconds > 0) (selectedActivity.met * userWeight * hours).toInt() else 0

    if (showSummary) {
        if (showExpandedMap) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).zIndex(10f)) {
                var polylineAnnotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager?>(null) }
                var currentPolyline by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation?>(null) }

                var lastAppliedMapType by remember { mutableStateOf(mapType) }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        com.mapbox.maps.MapView(ctx).apply {
                            val style = when (mapType) {
                                "Standard" -> Style.MAPBOX_STREETS
                                "Terrain" -> Style.OUTDOORS
                                else -> Style.DARK
                            }
                            mapboxMap.loadStyle(style) {
                                polylineAnnotationManager = annotations.createPolylineAnnotationManager()
                            }
                            // Enable location puck on the expanded summary map too
                            location.updateSettings {
                                enabled = true
                                pulsingEnabled = true
                                puckBearingEnabled = false
                            }
                        }
                    },
                    update = { mapView ->
                        // Only reload style when mapType actually changes
                        if (mapType != lastAppliedMapType) {
                            lastAppliedMapType = mapType
                            val style = when (mapType) {
                                "Standard" -> Style.MAPBOX_STREETS
                                "Terrain" -> Style.OUTDOORS
                                else -> Style.DARK
                            }
                            mapView.mapboxMap.loadStyle(style) {
                                polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager()
                                // Re-draw the polyline after style switch
                                currentPolyline = null
                            }
                        }

                        lastLocation?.let { loc ->
                            mapView.mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                    .zoom(16.0)
                                    .build()
                            )
                        }

                        if (pathPoints.size >= 2 && polylineAnnotationManager != null) {
                            if (currentPolyline == null) {
                                val options = PolylineAnnotationOptions()
                                    .withPoints(pathPoints)
                                    .withLineColor("#F97316")
                                    .withLineWidth(6.0)
                                currentPolyline = polylineAnnotationManager?.create(options)
                            } else {
                                currentPolyline?.points = pathPoints
                                polylineAnnotationManager?.update(currentPolyline!!)
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF121212).copy(alpha = 0.8f)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showExpandedMap = false },
                        modifier = Modifier.background(Color(0xFF2A2A2A), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }

                    Box {
                        Button(
                            onClick = { showSummaryMapTypeDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(mapType, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = showSummaryMapTypeDropdown,
                            onDismissRequest = { showSummaryMapTypeDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            listOf("Dark", "Standard", "Terrain").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = Color.White) },
                                    onClick = { mapType = type; showSummaryMapTypeDropdown = false }
                                )
                            }
                        }
                    }
                }
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resume",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        showSummary = false
                        viewModel.resumeTracking()
                    }
                )
                Text(
                    text = "Save Activity",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        placeholder = { Text("Activity Title", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { gpsFocusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color(0xFF2A2A2A), focusedBorderColor = CalorieKoOrange,
                            focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(8.dp), singleLine = true
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { showSummarySportDropdown = true },
                            shape = RoundedCornerShape(8.dp), color = Color(0xFF1E1E1E), border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(getActivityIcon(selectedActivity), null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(selectedActivity.name, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showSummarySportDropdown, onDismissRequest = { showSummarySportDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            OUTDOOR_ACTIVITIES.forEach { activity ->
                                DropdownMenuItem(
                                    text = { Text(activity.name, color = Color.White) },
                                    onClick = { selectedActivity = activity; showSummarySportDropdown = false }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
                                .clickable { showExpandedMap = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Default.Map, null, tint = CalorieKoOrange, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to view route", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${pathPoints.size} data points", color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
                                .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedPhotoUri != null) {
                                AsyncImage(model = selectedPhotoUri, contentDescription = "Workout Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+", color = Color.Gray, fontSize = 28.sp)
                                    Text("Add Photos", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showSummaryMapTypeDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, CalorieKoOrange), shape = RoundedCornerShape(22.dp)
                        ) {
                            Text("Change Map Type: $mapType", color = CalorieKoOrange, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = showSummaryMapTypeDropdown, onDismissRequest = { showSummaryMapTypeDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            listOf("Dark", "Standard", "Terrain").forEach { type ->
                                DropdownMenuItem(text = { Text(type, color = Color.White) }, onClick = { mapType = type; showSummaryMapTypeDropdown = false })
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedTag.isEmpty()) "Activity Tag" else selectedTag,
                            onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                            trailingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color(0xFF2A2A2A), focusedBorderColor = CalorieKoOrange,
                                focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { showSummaryTagDropdown = true })
                        DropdownMenu(
                            expanded = showSummaryTagDropdown, onDismissRequest = { showSummaryTagDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            activityTags.forEach { tag ->
                                DropdownMenuItem(text = { Text(tag, color = Color.White) }, onClick = { selectedTag = if (tag == "None") "" else tag; showSummaryTagDropdown = false })
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = privateNotes, onValueChange = { privateNotes = it },
                        placeholder = { Text("Jot down private notes here. Only you can see these.", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { gpsFocusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color(0xFF2A2A2A), focusedBorderColor = CalorieKoOrange,
                            focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Discard Activity", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { showDiscardDialog = true },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        isSaving = true
                        val finalTitle = if (activityTitle.isNotBlank()) activityTitle else selectedActivity.name
                        val pathString = pathPoints.joinToString("|") { "${it.latitude()},${it.longitude()}" }
                        val permanentPhotoPath = saveImageToInternalStorage(context, selectedPhotoUri)
                        val safeDistance = if (distanceKm.isNaN()) 0.0 else distanceKm

                        // ── Snapshot values directly from the service's StateFlow ──
                        // This bypasses Compose's collectAsState() recomposition pipeline,
                        // ensuring the saved values are exactly what the service holds
                        // at this instant — no lag, no race condition.
                        val snapshotTime = viewModel.snapshotTimeSeconds()
                        val snapshotPace = viewModel.snapshotPace()
                        val snapshotMovingTime = viewModel.snapshotMovingTime()

                        onSave(
                            finalTitle, caloriesBurned, formatTime(snapshotTime), safeDistance, snapshotPace,
                            snapshotMovingTime, pathString, mapType, permanentPhotoPath, privateNotes, selectedTag
                        )

                        // Stop the foreground service after saving
                        viewModel.stopTracking()
                    },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange), shape = RoundedCornerShape(28.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (showDiscardDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Are you sure?") },
                text = { Text("Discarding this activity will erase it permanently.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false; showSummary = false
                        viewModel.stopTracking()
                        viewModel.resetMetrics()
                        onBack()
                    }) { Text("Discard", color = Color(0xFFEF4444)) }
                },
                dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel", color = Color.White) } },
                containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White, textContentColor = Color.Gray
            )
        }
    } else {
        var showSportSheet by remember { mutableStateOf(false) }
        var is3DMode by remember { mutableStateOf(false) }

        val mapViewRef = remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
        var polylineManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
        val activePolylines = remember { mutableListOf<PolylineAnnotation>() }
        
        var circleManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
        var currentPuck by remember { mutableStateOf<CircleAnnotation?>(null) }

        LaunchedEffect(mapType) {
            val style = when (mapType) { "Standard" -> Style.MAPBOX_STREETS; "Terrain" -> Style.OUTDOORS; else -> Style.DARK }
            mapViewRef.value?.let { mapView ->
                mapView.mapboxMap.loadStyle(style) {
                    mapView.annotations.cleanup()
                    activePolylines.clear()
                    currentPuck = null
                    polylineManager = mapView.annotations.createPolylineAnnotationManager()
                    circleManager = mapView.annotations.createCircleAnnotationManager()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    com.mapbox.maps.MapView(ctx).apply {
                        mapboxMap.loadStyle(Style.DARK) {
                            annotations.cleanup()
                            activePolylines.clear()
                            currentPuck = null
                            polylineManager = annotations.createPolylineAnnotationManager()
                            circleManager = annotations.createCircleAnnotationManager()
                        }
                        mapboxMap.setCamera(CameraOptions.Builder().zoom(16.0).pitch(0.0).build())
                        scalebar.enabled = false

                        // Disable Mapbox native location puck so we can draw our own filtered dot.
                        // The native puck listens directly to raw GPS, bypassing our jitter filter.
                        location.updateSettings {
                            enabled = false
                        }

                        // Get initial GPS fix to center camera on user's location immediately
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(ctx)
                            fusedClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    mapboxMap.setCamera(
                                        CameraOptions.Builder()
                                            .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                            .zoom(16.0)
                                            .build()
                                    )
                                }
                            }
                        }

                        this.gestures.addOnMoveListener(object : OnMoveListener {
                            override fun onMoveBegin(detector: MoveGestureDetector) { followUser = false }
                            override fun onMove(detector: MoveGestureDetector): Boolean = false
                            override fun onMoveEnd(detector: MoveGestureDetector) {}
                        })
                        mapViewRef.value = this
                    }
                },
                update = { mapView ->
                    if (followUser && currentPoint != null) {
                        val cameraBuilder = CameraOptions.Builder().center(currentPoint)
                        if (isCompassMode && lastLocation?.hasBearing() == true) {
                            cameraBuilder.bearing(lastLocation!!.bearing.toDouble()).pitch(60.0).zoom(17.5)
                        } else {
                            cameraBuilder.bearing(0.0).pitch(0.0).zoom(16.0)
                        }
                        mapView.camera.easeTo(cameraBuilder.build(), MapAnimationOptions.Builder().duration(800L).build())
                    }

                    // Update Mapbox UI without creating thousands of duplicate markers:
                    // If managers exist, update existing annotations. If they don't yet exist in the lists, create them.
                    
                    if (pathPoints.isNotEmpty() && polylineManager != null) {
                        if (activePolylines.isEmpty()) {
                            activePolylines.add(polylineManager!!.create(PolylineAnnotationOptions().withPoints(pathPoints).withLineColor("#00BFFF").withLineWidth(16.0).withLineOpacity(0.2)))
                            activePolylines.add(polylineManager!!.create(PolylineAnnotationOptions().withPoints(pathPoints).withLineColor("#00BFFF").withLineWidth(10.0).withLineOpacity(0.4)))
                            activePolylines.add(polylineManager!!.create(PolylineAnnotationOptions().withPoints(pathPoints).withLineColor("#00DFFF").withLineWidth(5.0)))
                        } else {
                            activePolylines.forEach { 
                                it.points = pathPoints
                                polylineManager!!.update(it)
                            }
                        }
                    }

                    // Draw the custom location puck (blue dot) that strictly follows our filtered currentPoint
                    if (currentPoint != null && circleManager != null) {
                        if (currentPuck == null) {
                            val options = CircleAnnotationOptions()
                                .withPoint(currentPoint)
                                .withCircleRadius(10.0)
                                .withCircleColor("#4A90D9")
                                .withCircleStrokeWidth(3.0)
                                .withCircleStrokeColor("#FFFFFF")
                            currentPuck = circleManager!!.create(options)
                        } else {
                            currentPuck!!.point = currentPoint
                            circleManager!!.update(currentPuck!!)
                        }
                    }
                }
            )

            IconButton(
                onClick = {
                    if (isTracking || isPaused) {
                        // Service continues running in background — don't stop it.
                        // Navigate all the way back to dashboard.
                        onBackToDashboard()
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.padding(start = 16.dp, top = 48.dp).align(Alignment.TopStart).size(44.dp).background(Color(0xFF2A2A3E).copy(alpha = 0.9f), CircleShape)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp)) }

            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFF2A2A3E).copy(alpha = 0.92f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape).clickable { showLayerMenu = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Layers, "Layers", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    Box(
                        modifier = Modifier.size(18.dp).background(Color(0xFF2A2A3E), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape).align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) { Text("3", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    DropdownMenu(expanded = showLayerMenu, onDismissRequest = { showLayerMenu = false }, modifier = Modifier.background(Color(0xFF2A2A3E))) {
                        DropdownMenuItem(text = { Text(" Dark", color = if (mapType == "Dark") CalorieKoOrange else Color.White) }, onClick = { mapType = "Dark"; showLayerMenu = false })
                        DropdownMenuItem(text = { Text(" Standard", color = if (mapType == "Standard") CalorieKoOrange else Color.White) }, onClick = { mapType = "Standard"; showLayerMenu = false })
                        DropdownMenuItem(text = { Text(" Terrain", color = if (mapType == "Terrain") CalorieKoOrange else Color.White) }, onClick = { mapType = "Terrain"; showLayerMenu = false })
                    }
                }

                Box(
                    modifier = Modifier.size(48.dp).background(if (is3DMode) CalorieKoOrange else Color(0xFF2A2A3E).copy(alpha = 0.92f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape).clickable { is3DMode = !is3DMode; isCompassMode = is3DMode; followUser = true },
                    contentAlignment = Alignment.Center
                ) { Text("3D", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }

                Box(
                    modifier = Modifier.size(48.dp).background(if (followUser) Color(0xFF2A2A3E).copy(alpha = 0.92f) else Color(0xFF2A2A3E).copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, if (followUser) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f), CircleShape)
                        .clickable {
                            followUser = true
                            val targetPoint = currentPoint
                            if (targetPoint != null) {
                                val cameraBuilder = CameraOptions.Builder().center(targetPoint)
                                if (isCompassMode && lastLocation?.hasBearing() == true) {
                                    cameraBuilder.bearing(lastLocation!!.bearing.toDouble()).pitch(60.0).zoom(17.5)
                                } else {
                                    cameraBuilder.bearing(0.0).pitch(0.0).zoom(16.0)
                                }
                                mapViewRef.value?.camera?.easeTo(cameraBuilder.build(), MapAnimationOptions.Builder().duration(800L).build())
                            } else {
                                // Before tracking: use last known GPS fix to center
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            val cameraBuilder = CameraOptions.Builder().center(Point.fromLngLat(loc.longitude, loc.latitude))
                                            cameraBuilder.bearing(0.0).pitch(0.0).zoom(16.0)
                                            mapViewRef.value?.camera?.easeTo(cameraBuilder.build(), MapAnimationOptions.Builder().duration(800L).build())
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.MyLocation, "Center", tint = if (followUser) Color.White else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp)) }
            }

            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(if (isTracking && isPaused) Color(0xFFFFC107) else Color(0xFF222233), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(top = 10.dp, bottom = 12.dp)
                ) {
                    Box(modifier = Modifier.width(40.dp).height(4.dp).background(if (isTracking && isPaused) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isTracking && isPaused) "Stopped" else selectedActivity.name,
                        color = if (isTracking && isPaused) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF16162A)).padding(top = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (timeSeconds == 0L) "00:00" else formatTime(timeSeconds), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(text = "Time", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val paceDisplay = if (currentPace > 0.0 && currentPace <= 60.0) {
                                val pMin = currentPace.toInt()
                                val pSec = ((currentPace - pMin) * 60).toInt()
                                String.format(java.util.Locale.US, "%d:%02d", pMin, pSec)
                            } else { "-:--" }
                            Text(text = paceDisplay, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(text = if (isTracking && !isPaused) "Split avg. pace (/km)" else "Avg. pace (/km)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = String.format(java.util.Locale.US, "%.2f", distanceKm), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(text = "Distance (km)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF16162A)).padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSportSheet = true }) {
                        Box(
                            modifier = Modifier.size(52.dp).background(Color(0xFF3A2820), CircleShape).border(2.dp, CalorieKoOrange.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(getActivityIcon(selectedActivity), contentDescription = "Sport", tint = CalorieKoOrange, modifier = Modifier.size(24.dp))
                            Box(modifier = Modifier.size(18.dp).background(CalorieKoOrange, CircleShape).align(Alignment.BottomEnd), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(selectedActivity.name, color = CalorieKoOrange, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    AnimatedContent(targetState = isTracking, label = "TrackingTransition") { tracking ->
                        if (!tracking) {
                            Button(
                                onClick = {
                                    if (hasLocationPermission) {
                                        // Check if GPS/Location services are actually turned on
                                        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                                        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                                        if (!isGpsEnabled) {
                                            showLocationDialog = true
                                        } else {
                                            // On Android 13+, request notification permission first
                                            // so the foreground service notification is visible
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                // Start the foreground service and begin tracking
                                                val intent = android.content.Intent(context, com.calorieko.app.util.LocationTrackingService::class.java).apply {
                                                    action = com.calorieko.app.util.LocationTrackingService.ACTION_START
                                                }
                                                androidx.core.content.ContextCompat.startForegroundService(context, intent)
                                                viewModel.startTracking()
                                            }
                                        }
                                    } else {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                },
                                modifier = Modifier.size(72.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange), elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                            ) { Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White, modifier = Modifier.size(36.dp)) }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                val pauseBtnSize by animateDpAsState(targetValue = if (isPaused) 60.dp else 56.dp, animationSpec = spring(), label = "pauseSize")
                                Button(
                                    onClick = {
                                        if (isPaused) viewModel.resumeTracking() else viewModel.pauseTracking()
                                    },
                                    modifier = Modifier.size(pauseBtnSize), shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isPaused) CalorieKoOrange.copy(alpha = 0.85f) else Color(0xFF2A2A3E)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isPaused) 8.dp else 2.dp)
                                ) {
                                    AnimatedContent(targetState = isPaused, label = "PauseResumeIcon") { paused ->
                                        Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = if (paused) "Resume" else "Pause", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                                Button(
                                    onClick = {
                                        viewModel.pauseTracking()
                                        showSummary = true
                                        val cal = java.util.Calendar.getInstance()
                                        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                                        val timePrefix = when { hour < 12 -> "Morning"; hour < 17 -> "Afternoon"; else -> "Evening" }
                                        activityTitle = "$timePrefix ${selectedActivity.name}"
                                    },
                                    modifier = Modifier.size(72.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                                ) { Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(32.dp)) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }

            if (showSportSheet) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showSportSheet = false })
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFF1E1E30), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).padding(bottom = 32.dp)
                ) {
                    Box(modifier = Modifier.padding(top = 12.dp).width(40.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Choose a Sport", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showSportSheet = false }) { Text("✕", color = Color.White, fontSize = 20.sp) }
                    }
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF2A2A3E)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("New Sports available!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Explore the list and discover your new favorite way to move.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Your Top Sports", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(56.dp).background(Color(0xFF2A2A3E), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Icon(getActivityIcon(selectedActivity), null, tint = CalorieKoOrange, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(selectedActivity.name, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Foot Sports", color = CalorieKoOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                    OUTDOOR_ACTIVITIES.forEach { activity ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedActivity = activity; showSportSheet = false }.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(getActivityIcon(activity), null, tint = CalorieKoOrange.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(activity.name, color = if (selectedActivity == activity) CalorieKoOrange else Color.White, fontSize = 16.sp, fontWeight = if (selectedActivity == activity) FontWeight.Bold else FontWeight.Normal)
                            }
                            if (selectedActivity == activity) {
                                Icon(Icons.Default.Check, null, tint = CalorieKoOrange, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // GPS Location Services Dialog
    if (showLocationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Location Required", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("GPS is turned off. Please enable location services for accurate workout tracking.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Open Settings", color = CalorieKoOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Cancel", color = Color.White) }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
} // THE GPSTrackerContent FUNCTION IS NOW PROPERLY CLOSED HERE

@Composable
fun GPSStatItem(
    label: String, value: String, icon: ImageVector, unit: String = "", modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color(0xFF1E1E30).copy(alpha = 0.95f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (unit.isNotEmpty()) { Text(unit, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp) }
    }
}

// Helper function to permanently save temporary URIs
fun saveImageToInternalStorage(context: Context, uri: Uri?): String? {
    if (uri == null) return null
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "workout_photo_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}