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
import androidx.compose.material.icons.filled.Map
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
import com.calorieko.app.data.remote.SyncRepository
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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.scalebar.scalebar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex

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
    val syncRepository = remember {
        SyncRepository(
            userDao = db.userDao(),
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            mealLogItemDao = db.mealLogItemDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao()
        )
    }
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    val saveWorkout: (String, Int, String, Double?, Double?, Long?, String?, String?, String?, String?, String?) -> Unit = { name, calories, duration, dist, pace, movTime, path, mType, pUri, note, tag ->
        scope.launch(Dispatchers.IO) {
            val currentTimeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val log = ActivityLogEntity(
                uid = uid,
                type = "workout",
                name = name,
                timeString = currentTimeString,
                weightOrDuration = duration,
                calories = calories,
                timestamp = System.currentTimeMillis(),
                // GPS Fields
                distanceKm = dist,
                pace = pace,
                movingTimeSeconds = movTime,
                encodedPath = path,
                mapType = mType,
                photoUri = pUri,
                notes = note,
                activityTag = tag
            )
            activityLogDao.insertLog(log)

            withContext(Dispatchers.Main) {
                onBack()
            }
            try { syncRepository.syncSingleActivityLog(log) } catch (_: Exception) {}
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
                    WorkoutMode.MANUAL -> ManualMETsContent(userWeight = userWeight, onSave = { name, cals, dur -> saveWorkout(name, cals, dur, null, null, null, null, null, null, null, null) })
                    WorkoutMode.GPS -> GPSTrackerContent(userWeight = userWeight, onSave = saveWorkout, onBack = { handleBack() })
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

fun GPSTrackerContent(userWeight: Double, onSave: (String, Int, String, Double?, Double?, Long?, String?, String?, String?, String?, String?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current

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
    val activityTags = listOf("None", "Commute", "Workout", "Race", "Long Run")

    // UI State
    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf(OUTDOOR_ACTIVITIES[0]) } // Default Run
    var showLayerMenu by remember { mutableStateOf(false) }

    // Tracking Math
    var timeSeconds by remember { mutableLongStateOf(0L) }
    var movingTimeSeconds by remember { mutableLongStateOf(0L) }
    var lastMovementTimeMs by remember { mutableLongStateOf(0L) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }
    var currentPace by remember { mutableDoubleStateOf(0.0) }
    var isSaving by remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }

    // Save Activity State
    var activityTitle by remember { mutableStateOf("") }
    var privateNotes by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Map Settings
    var mapType by remember { mutableStateOf("Dark") } // Dark, Standard, Terrain
    var isCompassMode by remember { mutableStateOf(false) } // False = Center/Birds Eye, True = Forward Rotation
    var followUser by remember { mutableStateOf(true) }

    // Maps State
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var pathPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var currentPoint by remember { mutableStateOf<Point?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) { 
            isTracking = true; isPaused = false
            pathPoints = emptyList(); distanceKm = 0.0; timeSeconds = 0L; lastLocation = null; movingTimeSeconds = 0L; lastMovementTimeMs = System.currentTimeMillis()
            currentPace = 0.0; isMoving = false
        }
    }

    LaunchedEffect(isTracking, isPaused) {
        if (isTracking && !isPaused) { 
            while (true) { 
                delay(1000)
                timeSeconds++ 
                
                // Auto-pause timer if no new location received from OS in 3.5 seconds
                if (System.currentTimeMillis() - lastMovementTimeMs > 3500L) {
                    isMoving = false
                }
                
                if (isMoving) {
                    movingTimeSeconds++
                }
            } 
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Update current point for map display smoothly, if not completely crazy
                    if (location.accuracy < 50f) {
                        currentPoint = Point.fromLngLat(location.longitude, location.latitude)
                    }

                    if (isTracking && !isPaused) {
                        // 1. Core Accuracy Check (Forgiving for pockets/trees)
                        if (location.accuracy > 50f) continue

                        if (lastLocation != null) {
                            val distanceToUpdate = lastLocation!!.distanceTo(location)
                            val timeDeltaSec = (location.time - lastLocation!!.time) / 1000.0
                            val calculatedSpeed = if (timeDeltaSec > 0) distanceToUpdate / timeDeltaSec else 0.0
                            val physicalSpeed = if (location.hasSpeed()) location.speed else 0.0f
                            
                            // Hardware Floor: If physical speed is basically zero, we are definitely NOT moving.
                            if (physicalSpeed < 0.25f) {
                                isMoving = false
                            }

                            // --- SHAKE & JITTER FILTER (The Strava Logic) ---
                            // 1. Accuracy Gate: If the coordinate jump is smaller than the current margin of error, it's jitter.
                            val isAccurateEnough = distanceToUpdate > (location.accuracy * 0.5f)
                            
                            // 2. Velocity Harmony: If coordinates jump but the physical sensor says we are slow, it's a 'shake' jump.
                            // If calculated speed is 3x higher than what the physical sensors detect, it's likely a signal glitch/shake.
                            val isConsistentMotion = if (physicalSpeed > 0.5f) {
                                calculatedSpeed < (physicalSpeed * 3.0) 
                            } else {
                                calculatedSpeed < 1.0 // If walking extremely slow, don't allow jumps > 1m/s
                            }

                            // 2. Distance Filter
                            // Minimum 3.5 meters + Accuracy check to verify travel.
                            if (distanceToUpdate >= 3.5f && isAccurateEnough && isConsistentMotion) {
                                if (calculatedSpeed < 12.0) { // Max ~43 km/h sanity check
                                    
                                    // VERY SLOW DRIFT CHECK:
                                    // Protects against 10-second signal wander.
                                    if (calculatedSpeed > 0.45 || physicalSpeed > 0.5f) { 
                                        isMoving = true
                                        distanceKm += distanceToUpdate / 1000.0
                                        lastMovementTimeMs = System.currentTimeMillis()
                                        
                                        val newPoint = Point.fromLngLat(location.longitude, location.latitude)
                                        pathPoints = pathPoints + newPoint
                                        lastLocation = location

                                        if (distanceKm > 0.05) {
                                            currentPace = (movingTimeSeconds / 60.0) / distanceKm
                                        }
                                    } else {
                                        // Drift or Shake transition
                                        lastLocation = location
                                    }
                                } else {
                                    // Massive jump/Teleport. Reset to avoid line-stretching but don't add distance.
                                    lastLocation = location
                                }
                            }
                        } else {
                            // First tracking anchor point
                            isMoving = true
                            val newPoint = Point.fromLngLat(location.longitude, location.latitude)
                            pathPoints = pathPoints + newPoint
                            lastLocation = location
                            lastMovementTimeMs = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(isTracking, isPaused, hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(2000L)
                .setMinUpdateDistanceMeters(1.5f) // Let Android natively block stationary jitter
                .build()
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        }
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    val formatTime = { seconds: Long -> 
        if (seconds < 3600) {
            "%02d:%02d".format(seconds / 60, seconds % 60)
        } else {
            "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60) 
        }
    }

    // Smart Calorie calculation using MET formulas based on selected activity (uses moving time to stop stationary calorie bloat)
    val hours = movingTimeSeconds / 3600.0
    val caloriesBurned = if (movingTimeSeconds > 0) (selectedActivity.met * userWeight * hours).toInt() else 0
    // "pace" logic is now handled uniquely inside currentPace to freeze it during stops!

    if (showSummary) {
        // --- EXPANDED MAP PREVIEW OVERLAY ---
        if (showExpandedMap) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).zIndex(10f)) {
                // Read-only Mapbox View
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        com.mapbox.maps.MapView(ctx).apply {
                            mapboxMap.loadStyle(
                                when (mapType) {
                                    "Standard" -> Style.MAPBOX_STREETS
                                    "Terrain" -> Style.OUTDOORS
                                    else -> Style.DARK
                                }
                            )
                            val polylineManager = annotations.createPolylineAnnotationManager()

                            // Draw the saved route
                            if (pathPoints.isNotEmpty()) {
                                polylineManager.create(
                                    PolylineAnnotationOptions()
                                        .withPoints(pathPoints)
                                        .withLineColor("#F97316") // CalorieKo Orange
                                        .withLineWidth(5.0)
                                )
                                // Center camera on the route
                                mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(pathPoints.last())
                                        .zoom(14.0)
                                        .build()
                                )
                            }
                        }
                    },
                    update = { mapView ->
                        // Update style if changed from dropdown
                        val style = when (mapType) {
                            "Standard" -> Style.MAPBOX_STREETS
                            "Terrain" -> Style.OUTDOORS
                            else -> Style.DARK
                        }
                        mapView.mapboxMap.loadStyle(style)
                    }
                )

                // Top Bar for Expanded Map
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

                    // Map Type Dropdown inside the expanded view
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

        // --- STRAVA-STYLE SAVE ACTIVITY SCREEN ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)) // Deep black background
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resume",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { showSummary = false; isPaused = true }
                )
                Text(
                    text = "Save Activity",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp)) // To center title
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Title Field
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        placeholder = { Text("Activity Title", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color(0xFF2A2A2A),
                            focusedBorderColor = CalorieKoOrange,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                item {
                    // Sport Type Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSummarySportDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(getActivityIcon(selectedActivity), null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(selectedActivity.name, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showSummarySportDropdown,
                            onDismissRequest = { showSummarySportDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            OUTDOOR_ACTIVITIES.forEach { activity ->
                                DropdownMenuItem(
                                    text = { Text(activity.name, color = Color.White) },
                                    onClick = {
                                        selectedActivity = activity
                                        showSummarySportDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Map Preview & Photos Interactive
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Map Preview Box (Clickable)
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showExpandedMap = true }, // Opens expanded map
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Default.Map, null, tint = CalorieKoOrange, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to view route", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${pathPoints.size} data points", color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        // Add Photos Box (Clickable Launcher)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Workout Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
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
                    // Change Map Type Button with Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showSummaryMapTypeDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, CalorieKoOrange),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text("Change Map Type: $mapType", color = CalorieKoOrange, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showSummaryMapTypeDropdown,
                            onDismissRequest = { showSummaryMapTypeDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            listOf("Dark", "Standard", "Terrain").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = Color.White) },
                                    onClick = {
                                        mapType = type
                                        showSummaryMapTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Activity Tag Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedTag.isEmpty()) "Activity Tag" else selectedTag,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                            trailingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color(0xFF2A2A2A),
                                focusedBorderColor = CalorieKoOrange,
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        // Invisible overlay to catch the click over the text field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { showSummaryTagDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showSummaryTagDropdown,
                            onDismissRequest = { showSummaryTagDropdown = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            activityTags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag, color = Color.White) },
                                    onClick = {
                                        selectedTag = if (tag == "None") "" else tag
                                        showSummaryTagDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Jot down private notes
                    OutlinedTextField(
                        value = privateNotes,
                        onValueChange = { privateNotes = it },
                        placeholder = { Text("Jot down private notes here. Only you can see these.", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color(0xFF2A2A2A),
                            focusedBorderColor = CalorieKoOrange,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Discard Activity Link
                    Text(
                        text = "Discard Activity",
                        color = Color(0xFFEF4444),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { showDiscardDialog = true },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Bottom Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        isSaving = true
                        val finalTitle = if (activityTitle.isNotBlank()) activityTitle else selectedActivity.name
                        val pathString = pathPoints.joinToString("|") { "${it.latitude()},${it.longitude()}" }
                        
                        onSave(
                            finalTitle, 
                            caloriesBurned, 
                            formatTime(timeSeconds),
                            distanceKm,
                            currentPace,
                            movingTimeSeconds,
                            pathString,
                            mapType,
                            selectedPhotoUri?.toString(),
                            privateNotes,
                            selectedTag
                        ) 
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Discard Confirmation Dialog
        if (showDiscardDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Are you sure?") },
                text = { Text("Discarding this activity will erase it permanently.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        showSummary = false
                        isTracking = false
                        // Reset everything
                        timeSeconds = 0L; movingTimeSeconds = 0L; distanceKm = 0.0; pathPoints = emptyList(); lastLocation = null
                        onBack()
                    }) {
                        Text("Discard", color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color.White,
                textContentColor = Color.Gray
            )
        }
    } else {
        // --- STRAVA-STYLE DARK GPS TRACKER UI ---
        var showSportSheet by remember { mutableStateOf(false) }
        var is3DMode by remember { mutableStateOf(false) }

        val mapViewRef = remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
        var polylineManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
        var circleManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }

        // Handle map style changes
        LaunchedEffect(mapType) {
            val style = when (mapType) {
                "Standard" -> Style.MAPBOX_STREETS
                "Terrain" -> Style.OUTDOORS
                else -> Style.DARK // Dark theme by default
            }
            mapViewRef.value?.mapboxMap?.loadStyle(style)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {

            // --- MAPBOX MAP VIEW (Dark theme + Smooth animations) ---
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    com.mapbox.maps.MapView(ctx).apply {
                        // Dark style to match Strava-style dark UI
                        mapboxMap.loadStyle(Style.DARK)
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .zoom(16.0)
                                .pitch(0.0)
                                .build()
                        )
                        polylineManager = annotations.createPolylineAnnotationManager()
                        circleManager = annotations.createCircleAnnotationManager()
                        // Hide the scale bar (ft ruler) from the map
                        scalebar.enabled = false
                        this.gestures.addOnMoveListener(object : OnMoveListener {
                            override fun onMoveBegin(detector: MoveGestureDetector) {
                                followUser = false
                            }
                            override fun onMove(detector: MoveGestureDetector): Boolean = false
                            override fun onMoveEnd(detector: MoveGestureDetector) {}
                        })
                        mapViewRef.value = this
                    }
                },
                update = { mapView ->
                    // Smooth camera follow with animation (800ms ease)
                    if (followUser && currentPoint != null) {
                        val cameraBuilder = CameraOptions.Builder()
                            .center(currentPoint)

                        if (isCompassMode && lastLocation?.hasBearing() == true) {
                            // 3D compass mode: tilted view, rotated to bearing, closer zoom
                            cameraBuilder.bearing(lastLocation!!.bearing.toDouble())
                            cameraBuilder.pitch(60.0)  // Strong 3D tilt
                            cameraBuilder.zoom(17.5)    // Closer zoom in 3D
                        } else {
                            // Normal top-down mode
                            cameraBuilder.bearing(0.0)
                            cameraBuilder.pitch(0.0)
                            cameraBuilder.zoom(16.0)
                        }

                        // Smooth animated camera transition
                        mapView.camera.easeTo(
                            cameraBuilder.build(),
                            MapAnimationOptions.Builder()
                                .duration(800L)  // 800ms smooth animation
                                .build()
                        )
                    }

                    // Clear and redraw annotations
                    polylineManager?.deleteAll()
                    circleManager?.deleteAll()

                    // Draw route path with glow effect
                    if (pathPoints.isNotEmpty()) {
                        // Outer glow line (wider, semi-transparent)
                        polylineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(pathPoints)
                                .withLineColor("#00BFFF")
                                .withLineWidth(16.0)
                                .withLineOpacity(0.2)
                        )
                        // Inner glow
                        polylineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(pathPoints)
                                .withLineColor("#00BFFF")
                                .withLineWidth(10.0)
                                .withLineOpacity(0.4)
                        )
                        // Main bright line
                        polylineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(pathPoints)
                                .withLineColor("#00DFFF")
                                .withLineWidth(5.0)
                        )
                    }

                    // Location dot with glow rings
                    currentPoint?.let { point ->
                        // Outer glow ring
                        circleManager?.create(
                            CircleAnnotationOptions()
                                .withPoint(point)
                                .withCircleRadius(16.0)
                                .withCircleColor("#00BFFF")
                                .withCircleOpacity(0.15)
                        )
                        // Middle glow ring
                        circleManager?.create(
                            CircleAnnotationOptions()
                                .withPoint(point)
                                .withCircleRadius(11.0)
                                .withCircleColor("#00BFFF")
                                .withCircleOpacity(0.3)
                        )
                        // Core dot
                        circleManager?.create(
                            CircleAnnotationOptions()
                                .withPoint(point)
                                .withCircleRadius(7.0)
                                .withCircleColor("#00DFFF")
                                .withCircleStrokeWidth(2.5)
                                .withCircleStrokeColor("#FFFFFF")
                        )
                    }
                }
            )

            // --- TOP-LEFT: BACK BUTTON ---
            // --- TOP-LEFT: BACK BUTTON ---
            IconButton(
                onClick = { onBack() },
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
                        Text("3", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showLayerMenu,
                        onDismissRequest = { showLayerMenu = false },
                        modifier = Modifier.background(Color(0xFF2A2A3E))
                    ) {
                        DropdownMenuItem(
                            text = { Text(" Dark", color = if (mapType == "Dark") CalorieKoOrange else Color.White) },
                            onClick = { mapType = "Dark"; showLayerMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(" Standard", color = if (mapType == "Standard") CalorieKoOrange else Color.White) },
                            onClick = { mapType = "Standard"; showLayerMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(" Terrain", color = if (mapType == "Terrain") CalorieKoOrange else Color.White) },
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
                        .clickable { 
                            followUser = true 
                            // Force snap instantly even if currentPoint hasn't updated yet!
                            currentPoint?.let { point ->
                                val cameraBuilder = CameraOptions.Builder().center(point)
                                if (isCompassMode && lastLocation?.hasBearing() == true) {
                                    cameraBuilder.bearing(lastLocation!!.bearing.toDouble())
                                    cameraBuilder.pitch(60.0).zoom(17.5)
                                } else {
                                    cameraBuilder.bearing(0.0).pitch(0.0).zoom(16.0)
                                }
                                mapViewRef.value?.camera?.easeTo(
                                    cameraBuilder.build(),
                                    MapAnimationOptions.Builder().duration(800L).build()
                                )
                            }
                        },
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
                // Header Banner (Yellow when stopped, dark when running)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isTracking && isPaused) Color(0xFFFFC107) else Color(0xFF222233),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(top = 10.dp, bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(if (isTracking && isPaused) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isTracking && isPaused) "Stopped" else selectedActivity.name,
                        color = if (isTracking && isPaused) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // Stats card body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16162A))
                        .padding(top = 16.dp)
                ) {
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
                                text = if (currentPace > 0 && currentPace < 60) {
                                    val pMin = currentPace.toInt()
                                    val pSec = ((currentPace - pMin) * 60).toInt()
                                    String.format(Locale.US, "%d:%02d", pMin, pSec)
                                } else "-:--",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text(
                                if (isTracking && !isPaused) "Split avg. pace (/km)" else "Avg. pace (/km)",
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
                                        pathPoints = emptyList(); distanceKm = 0.0; timeSeconds = 0L; lastLocation = null
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
                                    onClick = { 
                                        isPaused = true // Just pause it, so we can resume from summary if needed
                                        showSummary = true
                                        // Set default title based on time of day
                                        val cal = java.util.Calendar.getInstance()
                                        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                                        val timePrefix = when {
                                            hour < 12 -> "Morning"
                                            hour < 17 -> "Afternoon"
                                            else -> "Evening"
                                        }
                                        activityTitle = "$timePrefix ${selectedActivity.name}"
                                    },
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