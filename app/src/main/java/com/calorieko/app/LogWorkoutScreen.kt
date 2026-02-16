package com.calorieko.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── DATA: MET Activity (from Compendium of Physical Activities) ──

data class MetActivity(
    val id: String,
    val name: String,
    val category: String,
    val met: Double
)

val ACTIVITIES = listOf(
    MetActivity("1", "Walking, slow pace (2.0 mph)", "Walking", 2.0),
    MetActivity("2", "Walking, moderate pace (3.0 mph)", "Walking", 3.5),
    MetActivity("3", "Walking, brisk pace (4.0 mph)", "Walking", 5.0),
    MetActivity("4", "Jogging, general", "Running", 7.0),
    MetActivity("5", "Running, 5 mph (12 min/mile)", "Running", 8.3),
    MetActivity("6", "Running, 6 mph (10 min/mile)", "Running", 9.8),
    MetActivity("7", "Gardening, general", "Household", 4.0),
    MetActivity("8", "Gardening, weeding", "Household", 4.5),
    MetActivity("9", "House cleaning, general", "Household", 3.3),
    MetActivity("10", "Mopping floors", "Household", 3.5),
    MetActivity("11", "Sweeping floors", "Household", 3.3),
    MetActivity("12", "Washing dishes", "Household", 2.3),
    MetActivity("13", "Cooking or food preparation", "Household", 2.5),
    MetActivity("14", "Laundry, washing clothes", "Household", 2.3),
    MetActivity("15", "Bicycling, leisure (10-11.9 mph)", "Cycling", 6.8),
    MetActivity("16", "Bicycling, moderate (12-13.9 mph)", "Cycling", 8.0),
    MetActivity("17", "Stair climbing, slow pace", "Exercise", 4.0),
    MetActivity("18", "Stair climbing, fast pace", "Exercise", 8.8),
    MetActivity("19", "Yoga, Hatha", "Exercise", 2.5),
    MetActivity("20", "Dancing, general", "Exercise", 4.5),
    MetActivity("21", "Swimming, leisurely", "Water", 6.0),
    MetActivity("22", "Swimming, moderate effort", "Water", 8.0),
    MetActivity("23", "Child care, feeding", "Daily Activities", 2.0),
    MetActivity("24", "Child care, bathing", "Daily Activities", 2.5),
    MetActivity("25", "Playing with children, moderate", "Daily Activities", 4.0)
)

// ─── MAIN SCREEN ─────────────────────────────────────────

@Composable
fun LogWorkoutScreen(
    onBack: () -> Unit,
    userWeight: Float = 70f
) {
    var mode by remember { mutableStateOf("selection") } // "selection", "manual", "gps"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // ── Header (bg-white px-6 py-4 shadow-sm sticky) ──
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (mode == "selection") onBack()
                            else mode = "selection"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF374151), // gray-700
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Title changes based on mode
                Text(
                    text = when (mode) {
                        "manual" -> "Lifestyle Activities"
                        "gps" -> "Outdoor Workout"
                        else -> "Log Workout"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937) // gray-800
                )
            }
        }

        // ── Main Content ──
        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                "selection" -> ModeSelectionScreen(
                    onModeSelect = { selectedMode -> mode = selectedMode }
                )
                "manual" -> ManualMETsScreen(userWeight = userWeight)
                "gps" -> GPSTrackerScreen()
            }
        }
    }
}

// ─── MODE SELECTION SCREEN ───────────────────────────────

@Composable
fun ModeSelectionScreen(onModeSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Subtitle
        Text(
            text = "Choose how you'd like to track your workout",
            fontSize = 14.sp,
            color = Color(0xFF4B5563), // gray-600
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ── Lifestyle Activities Card ──
        ModeCard(
            title = "Lifestyle Activities",
            description = "Log daily activities and household chores",
            tags = listOf("Gardening", "Walking", "Cleaning"),
            iconContent = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFFF9800)
                )
            },
            onClick = { onModeSelect("manual") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Outdoor Workout Card ──
        ModeCard(
            title = "Outdoor Workout",
            description = "Track runs, walks, and cycling with GPS",
            tags = listOf("Running", "Cycling", "Hiking"),
            iconContent = {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFFF9800)
                )
            },
            onClick = { onModeSelect("gps") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Info Card (bg-blue-50 border-blue-100) ──
        Surface(
            color = Color(0xFFEFF6FF), // blue-50
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE)) // blue-100
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF2563EB) // blue-600
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Track calories burned",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3A5F) // blue-900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your workout data syncs with your daily calorie balance automatically.",
                        fontSize = 12.sp,
                        color = Color(0xFF1D4ED8) // blue-700
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    tags: List<String>,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    // Matches Figma: bg-white rounded-2xl p-6 shadow-md border border-gray-100
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)), // gray-100
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon box (bg-orange-50 p-4 rounded-xl)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF7ED)), // orange-50
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1F2937) // gray-800
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563), // gray-600
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        Surface(
                            color = Color(0xFFF3F4F6), // gray-100
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563), // gray-600
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── MANUAL METs SCREEN ──────────────────────────────────

@Composable
fun ManualMETsScreen(userWeight: Float) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf<MetActivity?>(null) }
    var duration by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    // Filter activities
    val filteredActivities = ACTIVITIES.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    // Calculate calories: Calories = MET × weight (kg) × duration (hours)
    val caloriesBurned = if (selectedActivity != null && duration.isNotBlank()) {
        val durationHours = (duration.toFloatOrNull() ?: 0f) / 60f
        Math.round(selectedActivity!!.met * userWeight * durationHours)
    } else 0

    // Handle log
    val handleLog: () -> Unit = {
        if (selectedActivity != null && duration.isNotBlank()) {
            showSuccess = true
        }
    }

    // Reset after success
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            showSuccess = false
            selectedActivity = null
            duration = ""
            searchQuery = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Content ──
        if (selectedActivity == null) {
            // Activity selection mode
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Search Bar (matches Figma)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search activities...", color = Color(0xFF9CA3AF))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF), // gray-400
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE5E7EB), // gray-200
                        focusedBorderColor = Color(0xFFFF9800),
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Activity List
                if (filteredActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activities found",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredActivities) { activity ->
                            ActivityListItem(
                                activity = activity,
                                onClick = { selectedActivity = activity }
                            )
                        }
                    }
                }
            }
        } else {
            // Duration input & estimate
            val durationFloat = duration.toFloatOrNull() ?: 0f
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Selected Activity Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedActivity!!.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    text = selectedActivity!!.category,
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B7280),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Text(
                                text = "Change",
                                fontSize = 14.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { selectedActivity = null }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // MET badge (bg-orange-50 rounded-lg)
                        Surface(
                            color = Color(0xFFFFF7ED), // orange-50
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedActivity!!.met} MET",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }

                // Duration Input Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Label with clock icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF6B7280) // gray-500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Duration (minutes)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151) // gray-700
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            placeholder = { Text("e.g., 30", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF9FAFB), // gray-50
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFFFF9800),
                            ),
                            singleLine = true
                        )
                    }
                }

                // Live Calorie Estimate (gradient card, only shown when valid)
                if (durationFloat > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF9800), // orange-500
                                            Color(0xFFEA580C)  // orange-600
                                        )
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Estimated Calories Burned",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "$caloriesBurned",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${durationFloat.toInt()} minutes • ${userWeight.toInt()}kg body weight",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFEDD5) // orange-100
                                )
                            }
                        }
                    }
                }

                // Info Note (MET explanation)
                Surface(
                    color = Color(0xFFEFF6FF), // blue-50
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Text(
                        text = "MET (Metabolic Equivalent): A measure of exercise intensity. " +
                                "Calculation: Calories = MET × weight (kg) × duration (hours)",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A5F), // blue-900
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom Action Button
            if (selectedActivity != null && durationFloat > 0) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        HorizontalDivider(
                            color = Color(0xFFE5E7EB),
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                        Button(
                            onClick = handleLog,
                            enabled = !showSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showSuccess) Color(0xFF22C55E) // green-500
                                else Color(0xFFFF9800),
                                disabledContainerColor = Color(0xFF22C55E)
                            )
                        ) {
                            if (showSuccess) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Activity Logged!",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            } else {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Log $caloriesBurned Calories Burned",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Activity list item (matches Figma: bg-white rounded-xl p-4 shadow-sm border)
@Composable
fun ActivityListItem(activity: MetActivity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = activity.category,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${activity.met} MET",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF9800)
            )
        }
    }
}

// ─── GPS TRACKER SCREEN ──────────────────────────────────

@Composable
fun GPSTrackerScreen() {
    var isTracking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var timeSeconds by remember { mutableIntStateOf(0) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }

    // Simulate tracking
    LaunchedEffect(isTracking, isPaused) {
        while (isTracking && !isPaused) {
            delay(1000)
            timeSeconds++
            distanceKm += 0.0025 // ~0.15km per minute
            distanceKm = Math.round(distanceKm * 100.0) / 100.0
        }
    }

    // Format time
    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    fun formatTimeDetailed(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        else "$m:${s.toString().padStart(2, '0')}"
    }

    // Current pace (min/km)
    val currentPace = if (distanceKm > 0 && timeSeconds > 0) {
        val paceSeconds = timeSeconds / distanceKm
        val paceMinutes = (paceSeconds / 60).toInt()
        val paceSecondsRemainder = (paceSeconds % 60).toInt()
        "$paceMinutes:${paceSecondsRemainder.toString().padStart(2, '0')}"
    } else "0:00"

    // Calories (~100 cal per km for running)
    val caloriesBurned = Math.round(distanceKm * 100).toInt()

    // ── Summary screen ──
    if (showSummary) {
        WorkoutSummaryScreen(
            caloriesBurned = caloriesBurned,
            totalTime = formatTimeDetailed(timeSeconds),
            distance = distanceKm,
            avgPace = currentPace,
            onStartNew = {
                showSummary = false
                isTracking = false
                isPaused = false
                timeSeconds = 0
                distanceKm = 0.0
            }
        )
        return
    }

    // ── Tracking UI ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF111827), Color(0xFF1F2937)) // gray-900 to gray-800
                )
            )
    ) {
        // Map placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1F2937),
                            Color(0xFF111827),
                            Color.Black
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isTracking) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 100.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 16.dp),
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "GPS Tracking Simulation",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Tracking route animation
        if (isTracking) {
            val pulseAnim = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by pulseAnim.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 280.dp)
                    .fillMaxWidth(0.5f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF9800).copy(alpha = pulseAlpha))
            )
        }

        // ── Stats Overlay ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Stats (3 cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GPSStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    label = "Time",
                    value = formatTime(timeSeconds)
                )
                GPSStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    label = "Distance",
                    value = String.format("%.2f", distanceKm),
                    unit = "km"
                )
                GPSStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FitnessCenter,
                    label = "Pace",
                    value = currentPace,
                    unit = "min/km"
                )
            }

            // Status Badge
            if (isTracking) {
                val dotAnim = rememberInfiniteTransition(label = "dot")
                val dotAlpha by dotAnim.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotAlpha"
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9800).copy(alpha = dotAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaused) "Paused" else "Tracking...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pause button (only when tracking)
                if (isTracking) {
                    Button(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Start/Stop button (big circle)
                Button(
                    onClick = {
                        if (!isTracking) {
                            isTracking = true
                            isPaused = false
                        } else {
                            isTracking = false
                            isPaused = false
                            showSummary = true
                        }
                    },
                    modifier = Modifier.size(128.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking) Color(0xFFEF4444) // red-500
                        else Color(0xFFFF9800)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 12.dp
                    )
                ) {
                    Text(
                        text = if (isTracking) "Stop" else "Start",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Instructions (only before tracking)
                if (!isTracking) {
                    Text(
                        text = "Tap Start to begin tracking your outdoor workout",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF), // gray-400
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GPSStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String? = null
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF9CA3AF) // gray-400
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (unit != null) {
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

// ─── WORKOUT SUMMARY SCREEN ─────────────────────────────

@Composable
fun WorkoutSummaryScreen(
    caloriesBurned: Int,
    totalTime: String,
    distance: Double,
    avgPace: String,
    onStartNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Success Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)), // green-100
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF16A34A) // green-600
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Workout Complete!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Great job on your outdoor workout",
                fontSize = 16.sp,
                color = Color(0xFF4B5563) // gray-600
            )
        }

        // Stats Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calories card (gradient orange)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF9800), Color(0xFFEA580C))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calories", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$caloriesBurned",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Duration card
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                label = "Duration",
                value = totalTime
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Distance card
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocationOn,
                label = "Distance",
                value = String.format("%.2f", distance),
                unit = "kilometers"
            )
            // Avg Pace card
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FitnessCenter,
                label = "Avg Pace",
                value = avgPace,
                unit = "min/km"
            )
        }

        // Action Buttons
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStartNew,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)
            )
        ) {
            Text(
                "Start New Workout",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Button(
            onClick = { /* Save to dashboard */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E) // green-500
            )
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Save to Dashboard",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Info
        Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
        ) {
            Text(
                text = "This workout has been automatically synced with your daily calorie balance. You've burned $caloriesBurned calories!",
                fontSize = 12.sp,
                color = Color(0xFF1E3A5F),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SummaryStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Color(0xFFE5E7EB)), // gray-200
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF4B5563) // gray-600
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontSize = 14.sp, color = Color(0xFF4B5563))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            if (unit != null) {
                Text(
                    unit,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
