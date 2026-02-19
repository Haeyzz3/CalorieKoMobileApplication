package com.calorieko.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import kotlinx.coroutines.delay
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

enum class WorkoutMode { SELECTION, MANUAL, GPS }

@Composable
fun LogWorkoutScreen(onBack: () -> Unit, userWeight: Double = 70.0) {
    var mode by remember { mutableStateOf(WorkoutMode.SELECTION) }

    // Handle back press logic within the screen
    fun handleBack() {
        if (mode == WorkoutMode.SELECTION) {
            onBack()
        } else {
            mode = WorkoutMode.SELECTION
        }
    }

    Scaffold(
        topBar = {
            if (mode != WorkoutMode.GPS) { // GPS screen has its own custom UI
                Surface(color = Color.White, shadowElevation = 1.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { handleBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (mode) {
                                WorkoutMode.SELECTION -> "Log Workout"
                                WorkoutMode.MANUAL -> "Lifestyle Activities"
                                else -> ""
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(targetState = mode, label = "ModeTransition") { targetMode ->
                when (targetMode) {
                    WorkoutMode.SELECTION -> ModeSelectionContent(
                        onSelectManual = { mode = WorkoutMode.MANUAL },
                        onSelectGPS = { mode = WorkoutMode.GPS }
                    )
                    WorkoutMode.MANUAL -> ManualMETsContent(userWeight = userWeight)
                    WorkoutMode.GPS -> GPSTrackerContent(onFinish = { handleBack() })
                }
            }
        }
    }
}

// --- 1. Mode Selection Screen ---
@Composable
fun ModeSelectionContent(onSelectManual: () -> Unit, onSelectGPS: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "Choose how you'd like to track your workout",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Manual Card
        WorkoutSelectionCard(
            title = "Lifestyle Activities",
            description = "Log daily activities and household chores",
            icon = Icons.Default.Person,
            tags = listOf("Gardening", "Walking", "Cleaning"),
            onClick = onSelectManual
        )

        Spacer(modifier = Modifier.height(16.dp))

        // GPS Card
        WorkoutSelectionCard(
            title = "Outdoor Workout",
            description = "Track runs, walks, and cycling with GPS",
            icon = Icons.Default.LocationOn,
            tags = listOf("Running", "Cycling", "Hiking"),
            onClick = onSelectGPS
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Info Card
        Surface(
            color = Color(0xFFEFF6FF), // Blue-50
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.FitnessCenter, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Track calories burned", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Text(
                        "Your workout data syncs with your daily calorie balance automatically.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    tags: List<String>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
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
fun ManualMETsContent(userWeight: Double) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var durationText by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val filteredActivities = remember(searchQuery) {
        ACTIVITIES.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val caloriesBurned = remember(selectedActivity, durationText) {
        val duration = durationText.toDoubleOrNull() ?: 0.0
        val met = selectedActivity?.met ?: 0.0
        (met * userWeight * (duration / 60.0)).roundToInt()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            if (selectedActivity == null) {
                // Search State
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search activities (e.g. Walking)") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedBorderColor = CalorieKoOrange
                        )
                    )
                }

                items(filteredActivities) { activity ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { selectedActivity = activity }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(activity.name, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                                Text(activity.category, fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("${activity.met} MET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CalorieKoOrange, maxLines = 1, softWrap = false)
                        }
                    }
                }
            } else {
                // Activity Selected State
                item {
                    // Selected Activity Header
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(selectedActivity!!.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(selectedActivity!!.category, fontSize = 13.sp, color = Color.Gray)
                                }
                                TextButton(onClick = { selectedActivity = null }) {
                                    Text("Change", color = CalorieKoOrange, maxLines = 1, softWrap = false)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FitnessCenter, null, tint = CalorieKoOrange, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${selectedActivity!!.met} MET", fontSize = 12.sp, color = CalorieKoOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Duration Input
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Duration (minutes)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { durationText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CalorieKoOrange,
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                ),
                                placeholder = { Text("e.g. 30") },
                                leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = Color.Gray) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MET Info Box
                    Surface(
                        color = Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE0B2))
                    ) {
                        Text(
                            text = "MET (Metabolic Equivalent): A measure of exercise intensity.\nCalculation: Calories = MET × weight (kg) × duration (hours)",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Calorie Estimate Card
                    if (caloriesBurned > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Estimated Burn", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("$caloriesBurned", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("$durationText minutes • ${userWeight}kg body weight", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Button
        if (selectedActivity != null && durationText.isNotEmpty()) {
            Surface(shadowElevation = 8.dp) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp)) {
                    Button(
                        onClick = {
                            showSuccess = true
                            // Reset handled via logic in a real app, simplified here
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showSuccess) CalorieKoGreen else CalorieKoOrange
                        )
                    ) {
                        if (showSuccess) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activity Logged!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.LocalFireDepartment, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log $caloriesBurned Calories", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- 3. GPS Tracker Screen ---
@Composable
fun GPSTrackerContent(onFinish: () -> Unit) {
    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var timeSeconds by remember { mutableLongStateOf(0L) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }

    // Timer Logic
    LaunchedEffect(isTracking, isPaused) {
        if (isTracking && !isPaused) {
            while (true) {
                delay(1000)
                timeSeconds++
                distanceKm += 0.003 // Simulating approx 10km/h pace
            }
        }
    }

    val formatTime = { seconds: Long ->
        val m = seconds / 60
        val s = seconds % 60
        "%02d:%02d".format(m, s)
    }

    val caloriesBurned = (distanceKm * 60).toInt() // Rough estimate
    val pace = if (distanceKm > 0) (timeSeconds / 60.0) / distanceKm else 0.0

    if (showSummary) {
        // --- Summary View ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFDCFCE7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Workout Complete!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text("Great job on your outdoor workout", color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            // Row 1: Calories + Duration
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Calorie Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CalorieKoOrange),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Calories", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                        Text("$caloriesBurned", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                // Duration Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duration", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(formatTime(timeSeconds), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Distance + Avg Pace
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Distance Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Distance", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(String.format(Locale.US, "%.2f", distanceKm), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("kilometers", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                // Avg Pace Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsBike, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Avg Pace", color = Color.Gray, fontSize = 12.sp)
                        }
                        val paceMinutes = pace.toInt()
                        val paceSeconds = ((pace - paceMinutes) * 60).toInt()
                        Text(String.format(Locale.US, "%d:%02d", paceMinutes, paceSeconds), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("min/km", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start New Workout button
            Button(
                onClick = {
                    showSummary = false
                    isTracking = false
                    isPaused = false
                    timeSeconds = 0L
                    distanceKm = 0.0
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start New Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save to Dashboard button
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto-sync info note
            Surface(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "This workout has been automatically synced with your daily calorie balance. You've burned $caloriesBurned calories!",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    } else {
        // --- Active Tracker View ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111827)) // Dark bg
        ) {
            // Simulated Map Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1F2937), Color.Black),
                            radius = 800f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(80.dp).alpha(0.2f))
                Text("GPS Tracking Simulation", color = Color.Gray, modifier = Modifier.padding(top = 100.dp))
            }

            // Stats Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Stats + Status indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Top Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GPSStatItem("Time", formatTime(timeSeconds), Icons.Default.Timer, modifier = Modifier.weight(1f))
                        GPSStatItem("Distance", String.format(Locale.US, "%.2f", distanceKm), Icons.Default.LocationOn, "km", modifier = Modifier.weight(1f))
                        GPSStatItem("Pace", String.format(Locale.US, "%.1f", pace),
                            Icons.AutoMirrored.Filled.DirectionsBike, "min/km", modifier = Modifier.weight(1f))
                    }

                    // Status indicator below stats
                    if (isTracking) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!isPaused) {
                            Surface(
                                color = CalorieKoOrange.copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, CalorieKoOrange.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(CalorieKoOrange, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tracking...", color = CalorieKoOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Paused", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isTracking) {
                        Button(
                            onClick = { isPaused = !isPaused },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            if (!isTracking) {
                                isTracking = true
                            } else {
                                isTracking = false
                                showSummary = true
                            }
                        },
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) Color(0xFFEF4444) else CalorieKoOrange
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            if (isTracking) "Stop" else "Start",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (!isTracking) {
                        Text("Tap Start to begin tracking your outdoor workout", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GPSStatItem(label: String, value: String, icon: ImageVector, unit: String = "", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (unit.isNotEmpty()) {
            Text(unit, color = Color.Gray, fontSize = 12.sp)
        }
    }
}