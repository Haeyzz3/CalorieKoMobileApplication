package com.calorieko.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.graphics.toColorInt
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    ActivityItem("20", "Dancing, general", "Exercise", 4.5)
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

@Composable
fun LogWorkoutScreen(onBack: () -> Unit, userWeight: Double = 70.0) {
    var mode by remember { mutableStateOf(WorkoutMode.SELECTION) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val activityLogDao = db.activityLogDao()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    val saveWorkout: (String, Int, String) -> Unit = { name, calories, duration ->
        scope.launch(Dispatchers.IO) {
            val currentTimeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val log = ActivityLogEntity(
                uid = uid,
                type = "workout",
                name = name,
                timeString = currentTimeString,
                weightOrDuration = duration,
                calories = calories,
                timestamp = System.currentTimeMillis()
            )
            activityLogDao.insertLog(log)

            withContext(Dispatchers.Main) {
                onBack()
            }
        }
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
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(targetState = mode, label = "ModeTransition") { targetMode ->
                when (targetMode) {
                    WorkoutMode.SELECTION -> ModeSelectionContent(onSelectManual = { mode = WorkoutMode.MANUAL }, onSelectGPS = { mode = WorkoutMode.GPS })
                    WorkoutMode.MANUAL -> ManualMETsContent(userWeight = userWeight, onSave = saveWorkout)
                    WorkoutMode.GPS -> GPSTrackerContent(userWeight = userWeight, onSave = saveWorkout)
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

    val filteredActivities = remember(searchQuery) { ACTIVITIES.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) } }
    val caloriesBurned = remember(selectedActivity, durationText) {
        val duration = durationText.toDoubleOrNull() ?: 0.0
        val met = selectedActivity?.met ?: 0.0
        (met * userWeight * (duration / 60.0)).roundToInt()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
            if (selectedActivity == null) {
                item { OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search activities (e.g. Walking)") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = CalorieKoOrange)) }
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
                            OutlinedTextField(value = durationText, onValueChange = { durationText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CalorieKoOrange, unfocusedBorderColor = Color(0xFFE5E7EB)), placeholder = { Text("e.g. 30") }, leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = Color.Gray) })
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

// --- 3. ADVANCED OPENSTREETMAP TRACKER ---
@Composable
fun GPSTrackerContent(userWeight: Double, onSave: (String, Int, String) -> Unit) {
    val context = LocalContext.current

    // UI State
    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf(OUTDOOR_ACTIVITIES[0]) } // Default Run
    var showLayerMenu by remember { mutableStateOf(false) }

    // Tracking Math
    var timeSeconds by remember { mutableLongStateOf(0L) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }
    var isSaving by remember { mutableStateOf(false) }

    // Map Settings
    var mapType by remember { mutableStateOf("Standard") } // Standard, Terrain
    var isCompassMode by remember { mutableStateOf(false) } // False = Center/Birds Eye, True = Forward Rotation
    var followUser by remember { mutableStateOf(true) }

    // Maps State
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var pathPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var currentGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(Unit) { Configuration.getInstance().userAgentValue = context.packageName }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) { isTracking = true; isPaused = false }
    }

    LaunchedEffect(isTracking, isPaused) {
        if (isTracking && !isPaused) { while (true) { delay(1000); timeSeconds++ } }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location.accuracy < 25f) {
                        val newPoint = GeoPoint(location.latitude, location.longitude)
                        currentGeoPoint = newPoint

                        if (isTracking && !isPaused) {
                            pathPoints = pathPoints + newPoint
                            if (lastLocation != null) {
                                distanceKm += lastLocation!!.distanceTo(location) / 1000.0
                            }
                        }
                        lastLocation = location
                    }
                }
            }
        }
    }

    DisposableEffect(isTracking, isPaused, hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).setMinUpdateIntervalMillis(2000L).build()
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        }
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    val formatTime = { seconds: Long -> "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60) }

    // Smart Calorie calculation using MET formulas based on selected activity
    val hours = timeSeconds / 3600.0
    val caloriesBurned = if (timeSeconds > 0) (selectedActivity.met * userWeight * hours).toInt() else 0
    val pace = if (distanceKm > 0) ((timeSeconds / 60.0) / distanceKm) else 0.0

    if (showSummary) {
        // --- SUMMARY SCREEN ---
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(40.dp)) }
            Spacer(modifier = Modifier.height(24.dp)); Text("${selectedActivity.name} Complete!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)); Text("Great job on your outdoor workout", color = Color.Gray); Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = CalorieKoOrange), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Calories", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp) }; Text("$caloriesBurned", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White) } }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Duration", color = Color.Gray, fontSize = 12.sp) }; Text(formatTime(timeSeconds), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)) } }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Distance", color = Color.Gray, fontSize = 12.sp) }; Text(String.format(Locale.US, "%.2f", distanceKm), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)); Text("kilometers", color = Color.Gray, fontSize = 12.sp) } }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.DirectionsBike, null, tint = Color.Gray, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Avg Pace", color = Color.Gray, fontSize = 12.sp) }; val paceMinutes = pace.toInt(); val paceSeconds = ((pace - paceMinutes) * 60).toInt(); Text(String.format(Locale.US, "%d:%02d", paceMinutes, paceSeconds), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)); Text("min/km", color = Color.Gray, fontSize = 12.sp) } }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { isSaving = true; onSave("Outdoor ${selectedActivity.name}", caloriesBurned, formatTime(timeSeconds)) }, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isSaving) "Saving..." else "Save to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- ACTUAL OPENSTREETMAP BACKGROUND ---
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                    }
                },
                update = { mapView ->
                    // Set Terrain vs Standard Map
                    mapView.setTileSource(if (mapType == "Terrain") TileSourceFactory.OpenTopo else TileSourceFactory.MAPNIK)

                    // Follow and Perspective logic
                    if (followUser && currentGeoPoint != null) {
                        mapView.controller.animateTo(currentGeoPoint)
                    }

                    // Rotate map based on bearing if in compass mode
                    if (isCompassMode && lastLocation?.hasBearing() == true) {
                        mapView.mapOrientation = 360f - lastLocation!!.bearing
                    } else {
                        mapView.mapOrientation = 0f // Bird's Eye (North up)
                    }

                    mapView.overlays.clear()

                    // Draw path
                    if (pathPoints.isNotEmpty()) {
                        val line = Polyline()
                        line.setPoints(pathPoints)
                        line.outlinePaint.color = "#F97316".toColorInt() // Orange
                        line.outlinePaint.strokeWidth = 18f
                        mapView.overlays.add(line)
                    }

                    // Draw Location Marker
                    currentGeoPoint?.let {
                        val marker = Marker(mapView)
                        marker.position = it
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        // Create a simple circle drawable as the marker icon
                        val dotDrawable = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setSize(40, 40)
                            setColor("#F97316".toColorInt())
                            setStroke(4, android.graphics.Color.WHITE)
                        }
                        marker.icon = dotDrawable
                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate()
                }
            )

            // --- UI CONTROLS OVERLAY ---
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {

                // Top Settings (Map Controls & Activities)
                Column {
                    // Floating Map Controls
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Column(horizontalAlignment = Alignment.End) {
                            // Layer Switcher
                            Box {
                                IconButton(
                                    onClick = { showLayerMenu = true },
                                    modifier = Modifier.background(Color.White, CircleShape).shadow(4.dp, CircleShape)
                                ) { Icon(Icons.Default.Layers, "Map Type", tint = Color.Black) }

                                DropdownMenu(expanded = showLayerMenu, onDismissRequest = { showLayerMenu = false }) {
                                    DropdownMenuItem(text = { Text("Standard (Street)") }, onClick = { mapType = "Standard"; showLayerMenu = false })
                                    DropdownMenuItem(text = { Text("Terrain (Topo)") }, onClick = { mapType = "Terrain"; showLayerMenu = false })
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Perspective Switcher (Center vs Directional)
                            IconButton(
                                onClick = {
                                    isCompassMode = !isCompassMode
                                    followUser = true
                                },
                                modifier = Modifier.background(if (isCompassMode) CalorieKoOrange else Color.White, CircleShape).shadow(4.dp, CircleShape)
                            ) { Icon(Icons.Default.Explore, "Perspective", tint = if (isCompassMode) Color.White else Color.Black) }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Recenter Button
                            IconButton(
                                onClick = { followUser = true },
                                modifier = Modifier.background(if (followUser) CalorieKoGreen else Color.White, CircleShape).shadow(4.dp, CircleShape)
                            ) { Icon(Icons.Default.MyLocation, "Center Map", tint = if (followUser) Color.White else Color.Black) }
                        }
                    }

                    // Activity Selector (Only visible when NOT tracking)
                    if (!isTracking && timeSeconds == 0L) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) {
                            items(OUTDOOR_ACTIVITIES) { activity ->
                                Surface(
                                    color = if (selectedActivity == activity) CalorieKoOrange else Color.White.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.clickable { selectedActivity = activity },
                                    border = BorderStroke(1.dp, if (selectedActivity == activity) CalorieKoOrange else Color.LightGray)
                                ) {
                                    Text(
                                        text = activity.name,
                                        color = if (selectedActivity == activity) Color.White else Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Dashboard (Stats & Start/Stop)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Stats Box
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GPSStatItem("Time", formatTime(timeSeconds), Icons.Default.Timer, modifier = Modifier.weight(1f))
                        GPSStatItem("Distance", String.format(Locale.US, "%.2f", distanceKm), Icons.Default.LocationOn, modifier = Modifier.weight(1f), unit = "km")
                        GPSStatItem(if (selectedActivity.category == "Cycling") "Speed" else "Pace",
                            if (selectedActivity.category == "Cycling") String.format(Locale.US, "%.1f", if(timeSeconds>0) (distanceKm/(timeSeconds/3600.0)) else 0.0) else String.format(Locale.US, "%.1f", pace),
                            Icons.AutoMirrored.Filled.DirectionsBike,
                            modifier = Modifier.weight(1f),
                            unit = if (selectedActivity.category == "Cycling") "km/h" else "min/km")
                    }

                    // Status Indicator
                    if (isTracking) {
                        Surface(color = if (!isPaused) CalorieKoOrange.copy(alpha = 0.9f) else Color(0xFFEF4444).copy(alpha = 0.9f), shape = CircleShape, modifier = Modifier.padding(bottom = 16.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (!isPaused) "${selectedActivity.name} Active" else "Paused", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    } else if (!hasLocationPermission) {
                        Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                            Text("Location permission required", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    }

                    // Main Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (isTracking) {
                            Button(onClick = { isPaused = !isPaused }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)), modifier = Modifier.padding(end = 16.dp), shape = RoundedCornerShape(12.dp)) {
                                Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        Button(
                            onClick = {
                                if (!isTracking) {
                                    if (hasLocationPermission) { isTracking = true; isPaused = false; pathPoints = emptyList(); distanceKm = 0.0; timeSeconds = 0L }
                                    else { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                                } else {
                                    isTracking = false; showSummary = true
                                }
                            },
                            modifier = Modifier.size(if(isTracking) 80.dp else 120.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isTracking) Color(0xFFEF4444) else CalorieKoOrange),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text(if (isTracking) "Stop" else "Start", fontWeight = FontWeight.Bold, fontSize = if(isTracking) 16.sp else 20.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GPSStatItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, unit: String = "") {
    Column(modifier = modifier.background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.LightGray, fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (unit.isNotEmpty()) { Text(unit, color = Color.Gray, fontSize = 11.sp) }
    }
}