package com.calorieko.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val duration: String? = null
)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {

    // 1. Get the current authenticated user
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser


    // 2. Splits the full name by space and takes the first word. Defaults to "User" if null.
    val firstName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"

    val profileImageUrl = currentUser?.photoUrl

    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf("home") }

    // --- Mock Data ---
    val targetCalories = 2450
    val targetSodium = 2300
    val targetProtein = 120
    val targetCarbs = 250
    val targetFats = 65

    val activityLog = listOf(
        ActivityLogEntry("1", "meal", "8:30 AM", "Chicken Adobo", ActivityDetails("250g", 380, 890)),
        ActivityLogEntry("2", "workout", "7:00 AM", "Morning Walk", ActivityDetails(duration = "30 min", calories = 150)),
        ActivityLogEntry("3", "meal", "12:45 PM", "Grilled Fish", ActivityDetails("320g", 420, 450))
    )

    // Calculate Totals
    val caloriesConsumed = activityLog.filter { it.type == "meal" }.sumOf { it.details.calories }
    val caloriesBurned = activityLog.filter { it.type == "workout" }.sumOf { it.details.calories }
    val currentCalories = caloriesConsumed - caloriesBurned
    val currentSodium = activityLog.filter { it.type == "meal" }.sumOf { it.details.sodium ?: 0 }

    // Mock Macros
    val currentProtein = 65
    val currentCarbs = 180
    val currentFats = 45

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
                .background(Color(0xFFF8F9FA)) // Light Gray Background
                .padding(paddingValues)
        ) {

            // --- 1. Header (Sticky-ish visuals) ---
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp) // Reduced padding to slim down the header
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Profile Picture and Text
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // 1. Scaled-down profile picture matching a standard web navbar avatar
                    // 1. Scaled-down profile picture matching a standard web navbar avatar
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onNavigate("profile") }, // Makes the image act as a button
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp)) // Tighter spacing

                    // 2. Greeting and Date
                    Column {
                        Text(
                            text = "Hello, $firstName!",
                            fontSize = 18.sp, // Scaled down from 24.sp to match web dashboard headers
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                        Text(
                            text = currentDate,
                            fontSize = 12.sp, // Scaled down from 14.sp
                            color = Color.Gray
                        )
                    }
                }

                // Right Side: Scale Connected Badge (Slightly more compact)
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
                            fontSize = 10.sp, // Reduced to match new header scale
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
                    .padding(horizontal = 16.dp, vertical = 24.dp), // px-4 py-6
                verticalArrangement = Arrangement.spacedBy(24.dp) // space-y-6
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

                // Action Buttons (Revised Grid Layout)
                ActionButtonsRevised(
                    onLogMeal = { onNavigate("logMeal") },
                    onLogWorkout = { onNavigate("logWorkout") }
                )

                // Daily Activity Feed (Revised Card Layout)
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
        horizontalArrangement = Arrangement.spacedBy(16.dp) // gap-4
    ) {
        // Log Meal Button (Green)
        Surface(
            onClick = onLogMeal,
            color = Color(0xFF4CAF50), // bg-[#4CAF50]
            shape = RoundedCornerShape(16.dp), // rounded-2xl
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(130.dp) // Tall button
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
            color = Color(0xFFFF9800), // bg-[#FF9800]
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
    // White Card Container
    Card(
        shape = RoundedCornerShape(16.dp), // rounded-2xl
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // shadow-sm
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) { // p-6
            Text(
                text = "Today's Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937), // text-gray-800
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

    // Gray Item Container (bg-gray-50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .background(Color(0xFFF9FAFB)) // gray-50
            .padding(16.dp), // p-4
        verticalAlignment = Alignment.Top
    ) {
        // Icon Circle
        Surface(
            shape = CircleShape,
            color = if (isMeal) Color(0xFFDCFCE7) else Color(0xFFFFEDD5), // green-100 vs orange-100
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMeal) Icons.Default.LocalDining else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (isMeal) Color(0xFF16A34A) else Color(0xFFEA580C), // green-600 vs orange-600
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
                        color = Color(0xFF1F2937) // gray-800
                    )
                    Text(
                        text = activity.time,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280) // gray-500
                    )
                }

                // Calories Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)), // border-gray-200
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
                            color = Color(0xFF374151) // gray-700
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
                        color = Color(0xFF4B5563) // gray-600
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