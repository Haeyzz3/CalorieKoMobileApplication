package com.calorieko.app.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.components.ProgressRings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Data Model matching daily-activity-feed.tsx
data class ActivityLogEntry(
    val id: String,
    val type: String, // "meal" or "workout"
    val time: String,
    val name: String,
    val details: ActivityDetails
)

data class ActivityDetails(
    val weight: String? = null,
    val calories: Int,
    val sodium: Int? = null,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val duration: String? = null
)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {

    // 1. Get the current authenticated user
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // 2. Local Context & Database Setup
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val userDao = db.userDao()
    val activityLogDao = db.activityLogDao()

    val firstName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"
    val profileImageUrl = currentUser?.photoUrl

    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf("home") }

    // --- 3. DYNAMIC TARGET STATE ---
    var targetCalories by remember { mutableStateOf(2000) }
    var targetSodium by remember { mutableStateOf(2300) }
    var targetProtein by remember { mutableStateOf(150) }
    var targetCarbs by remember { mutableStateOf(200) }
    var targetFats by remember { mutableStateOf(65) }

    // --- 4. REAL ACTIVITY LOG STATE ---
    var realActivityLog by remember { mutableStateOf<List<ActivityLogEntity>>(emptyList()) }

    // --- 5. CALCULATED VALUES (derived from realActivityLog) ---
    val caloriesConsumed = realActivityLog.filter { it.type == "meal" }.sumOf { it.calories }
    val caloriesBurned = realActivityLog.filter { it.type == "workout" }.sumOf { it.calories }
    val currentCalories = caloriesConsumed - caloriesBurned

    val currentSodium = realActivityLog.filter { it.type == "meal" }.sumOf { it.sodium }
    val currentProtein = realActivityLog.filter { it.type == "meal" }.sumOf { it.protein }
    val currentCarbs = realActivityLog.filter { it.type == "meal" }.sumOf { it.carbs }
    val currentFats = realActivityLog.filter { it.type == "meal" }.sumOf { it.fats }

    // --- 6. FETCH USER PROFILE & ACTIVITY LOG ---
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            withContext(Dispatchers.IO) {
                // A. Fetch user profile for target calculations
                val profile = userDao.getUser(uid)

                if (profile != null) {
                    // Calculate BMR using Mifflin-St Jeor
                    val bmr = if (profile.sex.equals("Male", ignoreCase = true)) {
                        (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) + 5
                    } else {
                        (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) - 161
                    }

                    // Calculate TDEE
                    val activityMultiplier = when (profile.activityLevel) {
                        "lightly_active" -> 1.375
                        "active" -> 1.55
                        "very_active" -> 1.725
                        "not_very_active" -> 1.2
                        else -> 1.2
                    }
                    val tdee = bmr * activityMultiplier

                    // Goal Adjustments
                    targetCalories = when (profile.goal) {
                        "lose_weight", "weight_loss", "weight" -> (tdee - 500).toInt().coerceAtLeast(1200)
                        "gain_muscle" -> (tdee + 300).toInt()
                        else -> tdee.toInt()
                    }

                    // Macro Splits
                    val (proteinPct, carbsPct, fatsPct) = when (profile.goal) {
                        "lose_weight", "weight_loss", "weight" -> Triple(0.35, 0.35, 0.30)
                        "gain_muscle" -> Triple(0.30, 0.45, 0.25)
                        else -> Triple(0.30, 0.40, 0.30)
                    }

                    targetProtein = ((targetCalories * proteinPct) / 4).toInt()
                    targetCarbs = ((targetCalories * carbsPct) / 4).toInt()
                    targetFats = ((targetCalories * fatsPct) / 9).toInt()
                    targetSodium = 2300
                }

                // B. Fetch today's activity logs
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis

                realActivityLog = activityLogDao.getLogsForToday(uid, startOfDay)
            }
        }
    }

    // --- 7. CONVERT ActivityLogEntity to ActivityLogEntry for the feed ---
    val activityLog = realActivityLog.map { entity ->
        ActivityLogEntry(
            id = entity.id.toString(),
            type = entity.type,
            time = entity.timeString,
            name = entity.name,
            details = ActivityDetails(
                weight = if (entity.type == "meal") entity.weightOrDuration else null,
                calories = entity.calories,
                sodium = entity.sodium,
                protein = entity.protein,
                carbs = entity.carbs,
                fats = entity.fats,
                duration = if (entity.type == "workout") entity.weightOrDuration else null
            )
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                if (it != "home") {
                    onNavigate(it)
                }
            })
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {

            // --- 1. Header ---
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Profile Picture and Text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onNavigate("profile") },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Hello, $firstName!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                        Text(
                            text = currentDate,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Right Side: Scale Connected Badge
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scale Connected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            // --- 2. Scrollable Content ---
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // Progress Rings
                ProgressRings(
                    caloriesCurrent = currentCalories,
                    caloriesTarget = targetCalories,
                    sodiumCurrent = currentSodium,
                    sodiumTarget = targetSodium,
                    proteinCurrent = currentProtein,
                    proteinTarget = targetProtein,
                    carbsCurrent = currentCarbs,
                    carbsTarget = targetCarbs,
                    fatsCurrent = currentFats,
                    fatsTarget = targetFats
                )

                // Action Buttons
                ActionButtonsRevised(
                    onLogMeal = { onNavigate("logMeal") },
                    onLogWorkout = { onNavigate("logWorkout") }
                )

                // Daily Activity Feed
                DailyActivityFeedRevised(activityLog)

                // Bottom Spacer
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// --- REVISED COMPONENTS ---

@Composable
fun ActionButtonsRevised(onLogMeal: () -> Unit, onLogWorkout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Log Meal Button (Green)
        Surface(
            onClick = onLogMeal,
            color = Color(0xFF4CAF50),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Icon(Icons.Default.MonitorWeight, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Log Meal", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("AI + Scale", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

        // Log Workout Button (Orange)
        Surface(
            onClick = onLogWorkout,
            color = Color(0xFFFF9800),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Log Workout", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("Track Activity", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DailyActivityFeedRevised(activities: List<ActivityLogEntry>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Today's Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (activities.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalDining,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("No activities logged yet", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                // List Items
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activities.forEach { activity ->
                        ActivityItemRevised(activity)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItemRevised(activity: ActivityLogEntry) {
    val isMeal = activity.type == "meal"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Circle
        Surface(
            shape = CircleShape,
            color = if (isMeal) Color(0xFFDCFCE7) else Color(0xFFFFEDD5),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMeal) Icons.Default.LocalDining else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (isMeal) Color(0xFF16A34A) else Color(0xFFEA580C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Title & Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = activity.time,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                // Calories Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMeal) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isMeal) Color(0xFF16A34A) else Color(0xFFEA580C),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${activity.details.calories} cal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }

            // Details Footer
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMeal && activity.details.weight != null) {
                    Icon(Icons.Default.MonitorWeight, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activity.details.weight,
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (isMeal && activity.details.sodium != null) {
                    Text(
                        text = "Sodium: ${activity.details.sodium}mg",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                }

                if (!isMeal && activity.details.duration != null) {
                    Text(
                        text = "Duration: ${activity.details.duration}",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}