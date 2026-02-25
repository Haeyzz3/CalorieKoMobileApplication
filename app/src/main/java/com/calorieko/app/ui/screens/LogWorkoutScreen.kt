package com.calorieko.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.draw.alpha
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
                    // UPDATE THIS LINE: Add the onBack parameter
                    WorkoutMode.GPS -> GPSTrackerContent(userWeight = userWeight, onSave = saveWorkout, onBack = onBack)
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

fun GPSTrackerContent(userWeight: Double, onSave: (String, Int, String) -> Unit, onBack: () -> Unit) {
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
            Button(
                // UPDATE THIS LINE: Removed "Outdoor " prefix
                onClick = { isSaving = true; onSave(selectedActivity.name, caloriesBurned, formatTime(timeSeconds)) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isSaving) "Saving..." else "Save to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // --- STRAVA-STYLE DARK GPS TRACKER UI ---
        var showSportSheet by remember { mutableStateOf(false) }
        var is3DMode by remember { mutableStateOf(false) }

        val mapViewRef = remember { mutableStateOf<MapView?>(null) }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {

            // --- MAP VIEW ---
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        // Smoother rendering settings
                        isTilesScaledToDpi = true
                        setScrollableAreaLimitLatitude(
                            MapView.getTileSystem().maxLatitude,
                            MapView.getTileSystem().minLatitude,
                            0
                        )
                        mapViewRef.value = this
                    }
                },
                update = { mapView ->
                    mapView.setTileSource(
                        if (mapType == "Terrain") TileSourceFactory.OpenTopo
                        else TileSourceFactory.MAPNIK
                    )

                    // Smooth follow logic
                    if (followUser && currentGeoPoint != null) {
                        mapView.controller.animateTo(currentGeoPoint, 16.0, 800L)
                    }

                    // Smooth compass rotation
                    if (isCompassMode && lastLocation?.hasBearing() == true) {
                        val targetOrientation = 360f - lastLocation!!.bearing
                        val currentOrientation = mapView.mapOrientation
                        val diff = targetOrientation - currentOrientation
                        // Smooth interpolation
                        mapView.mapOrientation = currentOrientation + diff * 0.3f
                    } else {
                        if (mapView.mapOrientation != 0f) {
                            mapView.mapOrientation = mapView.mapOrientation * 0.7f
                            if (kotlin.math.abs(mapView.mapOrientation) < 1f) {
                                mapView.mapOrientation = 0f
                            }
                        }
                    }

                    mapView.overlays.clear()

                    // Draw route path with glow effect
                    if (pathPoints.isNotEmpty()) {
                        // Outer glow
                        val glowLine = Polyline()
                        glowLine.setPoints(pathPoints)
                        glowLine.outlinePaint.color = android.graphics.Color.parseColor("#4400BFFF")
                        glowLine.outlinePaint.strokeWidth = 28f
                        glowLine.outlinePaint.isAntiAlias = true
                        mapView.overlays.add(glowLine)

                        // Main line
                        val line = Polyline()
                        line.setPoints(pathPoints)
                        line.outlinePaint.color = android.graphics.Color.parseColor("#00BFFF")
                        line.outlinePaint.strokeWidth = 12f
                        line.outlinePaint.isAntiAlias = true
                        line.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        line.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        mapView.overlays.add(line)
                    }

                    // Location marker with pulsing dot effect
                    currentGeoPoint?.let { point ->
                        val marker = Marker(mapView)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate()
                }
            )

            // --- TOP-LEFT: BACK BUTTON ---
            // --- TOP-LEFT: BACK BUTTON ---
            IconButton(
                onClick = { onBack() }, // UPDATE THIS LINE: Replace /* handled by parent */ with onBack()
                modifier = Modifier
                    .padding(start = 16.dp, top = 48.dp)
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .background(Color(0xFF2A2A3E).copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // --- RIGHT SIDE: MAP CONTROL BUTTONS ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Layers button with badge
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF2A2A3E).copy(alpha = 0.92f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable { showLayerMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Layers, "Layers", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    // Badge
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2A2A3E), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showLayerMenu,
                        onDismissRequest = { showLayerMenu = false },
                        modifier = Modifier.background(Color(0xFF2A2A3E))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Standard", color = Color.White) },
                            onClick = { mapType = "Standard"; showLayerMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Terrain", color = Color.White) },
                            onClick = { mapType = "Terrain"; showLayerMenu = false }
                        )
                    }
                }

                // 3D Toggle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (is3DMode) CalorieKoOrange else Color(0xFF2A2A3E).copy(alpha = 0.92f),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable {
                            is3DMode = !is3DMode
                            isCompassMode = is3DMode
                            followUser = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "3D",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Recenter / Scope button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (followUser) Color(0xFF2A2A3E).copy(alpha = 0.92f)
                            else Color(0xFF2A2A3E).copy(alpha = 0.6f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (followUser) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clickable { followUser = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        "Center",
                        tint = if (followUser) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // --- BOTTOM PANEL ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Stats card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF1E1E30),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(top = 8.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Activity name header
                    Text(
                        text = selectedActivity.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Time
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (timeSeconds == 0L) "00:00" else formatTime(timeSeconds),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text("Time", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }

                        // Avg Pace
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (pace > 0) {
                                    val pMin = pace.toInt()
                                    val pSec = ((pace - pMin) * 60).toInt()
                                    String.format(Locale.US, "%d:%02d", pMin, pSec)
                                } else "-:--",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text(
                                "Avg. pace (/km)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }

                        // Distance
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.2f", distanceKm),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text(
                                "Distance (km)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Drag handle for bottom section
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16162A))
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sport selector button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showSportSheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFF3A2820), CircleShape)
                                .border(2.dp, CalorieKoOrange.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                getActivityIcon(selectedActivity),
                                contentDescription = "Sport",
                                tint = CalorieKoOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            // Checkmark badge
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(CalorieKoOrange, CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            selectedActivity.name,
                            color = CalorieKoOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Start / Stop / Pause buttons with animations
                    AnimatedContent(
                        targetState = isTracking,
                        label = "TrackingTransition"
                    ) { tracking ->
                        if (!tracking) {
                            // START button
                            Button(
                                onClick = {
                                    if (hasLocationPermission) {
                                        isTracking = true; isPaused = false
                                        pathPoints = emptyList(); distanceKm = 0.0; timeSeconds = 0L
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pause / Resume button with animated icon swap
                                val pauseBtnSize by animateDpAsState(
                                    targetValue = if (isPaused) 60.dp else 56.dp,
                                    animationSpec = spring(), label = "pauseSize"
                                )
                                Button(
                                    onClick = { isPaused = !isPaused },
                                    modifier = Modifier.size(pauseBtnSize),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPaused) CalorieKoOrange.copy(alpha = 0.85f)
                                        else Color(0xFF2A2A3E)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (isPaused) 8.dp else 2.dp
                                    )
                                ) {
                                    AnimatedContent(
                                        targetState = isPaused,
                                        label = "PauseResumeIcon"
                                    ) { paused ->
                                        Icon(
                                            if (paused) Icons.Default.PlayArrow
                                            else Icons.Default.Pause,
                                            contentDescription = if (paused) "Resume" else "Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                // Stop button
                                Button(
                                    onClick = { isTracking = false; showSummary = true },
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Empty spacer to balance layout (replaces removed Add Route)
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }

            // --- SPORT SELECTION BOTTOM SHEET ---
            if (showSportSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showSportSheet = false }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color(0xFF1E1E30),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(bottom = 32.dp)
                ) {
                    // Handle
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Choose a Sport",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showSportSheet = false }) {
                            Text("✕", color = Color.White, fontSize = 20.sp)
                        }
                    }

                    // Banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2A2A3E)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "New Sports available!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Explore the list and discover your new favorite way to move.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Your Top Sports
                    Text(
                        "Your Top Sports",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF2A2A3E), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getActivityIcon(selectedActivity),
                                    null,
                                    tint = CalorieKoOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                selectedActivity.name,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Foot Sports section
                    Text(
                        "Foot Sports",
                        color = CalorieKoOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    // Sport items
                    OUTDOOR_ACTIVITIES.forEach { activity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedActivity = activity
                                    showSportSheet = false
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    getActivityIcon(activity),
                                    null,
                                    tint = CalorieKoOrange.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    activity.name,
                                    color = if (selectedActivity == activity) CalorieKoOrange
                                    else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = if (selectedActivity == activity) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            }
                            if (selectedActivity == activity) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = CalorieKoOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GPSStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1E1E30).copy(alpha = 0.95f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (unit.isNotEmpty()) {
            Text(unit, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
        }
    }
}