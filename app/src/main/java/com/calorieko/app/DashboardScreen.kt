package com.calorieko.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data Model for Activity Log
data class ActivityLogEntry(
    val id: String,
    val type: String, // "meal" or "workout"
    val time: String,
    val name: String,
    val calories: Int,
    val sodium: Int = 0,
    val detailText: String // "250g" or "30 min"
)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf("home") }

    // --- Mock Data ---
    val userName = "User" // You can pass this in later
    val targetCalories = 2450

    val activityLog = listOf(
        ActivityLogEntry("1", "meal", "8:30 AM", "Chicken Adobo", 380, 890, "250g"),
        ActivityLogEntry("2", "workout", "7:00 AM", "Morning Walk", 150, 0, "30 min"),
        ActivityLogEntry("3", "meal", "12:45 PM", "Grilled Fish", 420, 450, "320g")
    )

    // Calculate Totals
    val caloriesConsumed = activityLog.filter { it.type == "meal" }.sumOf { it.calories }
    val caloriesBurned = activityLog.filter { it.type == "workout" }.sumOf { it.calories }
    val currentCalories = caloriesConsumed - caloriesBurned

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = { activeTab = it })
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)) // Light Gray Background
                .padding(paddingValues)
        ) {

            // --- 1. Sticky Header ---
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, $userName!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        // Date Formatter
                        val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                        Text(
                            text = currentDate,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    // Scale Connected Badge
                    Surface(
                        color = Color(0xFFECFDF5), // Green 50
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = Color(0xFF059669), // Green 600
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scale Connected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF047857) // Green 700
                            )
                        }
                    }
                }
            }

            // --- 2. Scrollable Content ---
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // Placeholder for Progress Rings (We'll build this properly next!)
                ProgressSummaryCard(current = currentCalories, target = targetCalories)

                // Action Buttons
                ActionButtons(
                    onLogMeal = { onNavigate("logMeal") },
                    onLogWorkout = { onNavigate("logWorkout") }
                )

                // Daily Activity Feed
                DailyActivityFeed(activityLog)

                // Extra space at bottom for scrolling past FABs/BottomNav
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// --- Sub-Components (Usually these would be in separate files) ---

@Composable
fun ActionButtons(onLogMeal: () -> Unit, onLogWorkout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Log Meal Button
        Button(
            onClick = onLogMeal,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Meal")
        }

        // Log Workout Button
        Button(
            onClick = onLogWorkout,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)), // Blue
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Workout")
        }
    }
}

@Composable
fun DailyActivityFeed(activities: List<ActivityLogEntry>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            // Add Button (Small)
            Surface(
                shape = CircleShape,
                color = Color(0xFFF3F4F6),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }

        activities.forEach { activity ->
            ActivityCard(activity)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityLogEntry) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background
            val bgGradient = if (activity.type == "meal") {
                listOf(CalorieKoOrange, CalorieKoLightOrange)
            } else {
                listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)) // Blue gradient
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(bgGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activity.type == "meal") Icons.Default.Restaurant else Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "${activity.time} • ${activity.detailText}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Calories
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if(activity.type == "meal") "+${activity.calories}" else "-${activity.calories}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if(activity.type == "meal") CalorieKoOrange else Color(0xFF3B82F6)
                )
                Text(
                    text = "kcal",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// Temporary Placeholder for the complex Progress Rings
@Composable
fun ProgressSummaryCard(current: Int, target: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("Progress Rings Component\n(Coming Next)", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }
    }
}