package com.calorieko.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import java.util.Locale

// --- Data Models ---
data class UserData(
    val name: String = "Juan Dela Cruz",
    val memberSince: String = "January 2025",
    val streak: Int = 7,
    val level: Int = 5,
    val age: Int = 25,
    val height: Double = 170.0, // cm
    val weight: Double = 70.0,  // kg
    val goal: String = "general"
)

data class Badge(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val colorBg: Color,
    val colorIcon: Color,
    val earned: Boolean,
    val progress: Int,
    val max: Int
)

@Composable
fun ProfileScreen(onNavigate: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("profile") }
    val scrollState = rememberScrollState()

    // Mock Data
    val userData = UserData()

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 1. Header Section
            ProfileHeader(userData)

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 2. Baseline Metrics Grid
                BaselineMetricsGrid(userData)

                // 3. Health Goals Section
                HealthGoalsSection(userData.goal)

                // 4. Milestones & Badges
                MilestonesSection(userData.streak)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- 1. Header with Animated Streak Ring ---
@Composable
fun ProfileHeader(user: UserData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                )
            )
            .padding(top = 32.dp, bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated Streak Ring Profile
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                // Animation State
                val animatedProgress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    animatedProgress.animateTo(
                        targetValue = user.streak / 30f,
                        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
                    )
                }

                Canvas(modifier = Modifier.size(130.dp)) {
                    // Background Ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Progress Ring
                    drawArc(
                        color = Color(0xFFFFD700), // Gold
                        startAngle = -90f,
                        sweepAngle = animatedProgress.value * 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Avatar
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Level Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent, // Gradient handled by box
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFC107), Color(0xFFFF9800))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(
                                "Level ${user.level}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Info
            Text(
                text = user.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                Text(
                    "Member since ${user.memberSince}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Streak Chip
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Text("${user.streak} Day Streak!", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// --- 2. Baseline Metrics Grid ---
@Composable
fun BaselineMetricsGrid(user: UserData) {
    // Calculate BMI
    val heightInMeters = user.height / 100
    val bmi = user.weight / (heightInMeters * heightInMeters)
    val bmiRounded = String.format(Locale.US, "%.1f", bmi)

    val bmiInfo = when {
        bmi < 18.5 -> Pair("Underweight", Color(0xFF2563EB)) // Blue
        bmi < 25 -> Pair("Normal", Color(0xFF16A34A)) // Green
        bmi < 30 -> Pair("Overweight", Color(0xFFEA580C)) // Orange
        else -> Pair("Obese", Color(0xFFDC2626)) // Red
    }

    Column {
        Text(
            "Baseline Metrics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Weight
            MetricCard(
                title = "Current Weight",
                value = "${user.weight}",
                unit = "kilograms",
                icon = Icons.Default.MonitorWeight,
                iconColor = CalorieKoGreen,
                bgColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
            // Height
            MetricCard(
                title = "Height",
                value = "${user.height}",
                unit = "centimeters",
                icon = Icons.Default.Straighten, // Or Height icon if available in extended
                iconColor = Color(0xFF2563EB),
                bgColor = Color(0xFFEFF6FF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Age
            MetricCard(
                title = "Age",
                value = "${user.age}",
                unit = "years old",
                icon = Icons.Default.Cake,
                iconColor = Color(0xFF9333EA),
                bgColor = Color(0xFFFAF5FF),
                modifier = Modifier.weight(1f)
            )
            // BMI
            MetricCard(
                title = "BMI",
                value = bmiRounded,
                unit = bmiInfo.first,
                unitColor = bmiInfo.second,
                icon = Icons.Default.MonitorHeart,
                iconColor = Color(0xFFEA580C),
                bgColor = Color(0xFFFFF7ED),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    unitColor: Color = Color.Gray
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(bgColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
            }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(
                unit,
                fontSize = 12.sp,
                fontWeight = if (unitColor != Color.Gray) FontWeight.Bold else FontWeight.Normal,
                color = unitColor
            )
        }
    }
}

// --- 3. Health Goals Section ---
@Composable
fun HealthGoalsSection(goalCode: String) {
    val goalInfo = when (goalCode) {
        "hypertension" -> Triple("Hypertension Management", Icons.Default.Favorite, Color(0xFFEF4444))
        "weight_loss" -> Triple("Weight Control", Icons.Default.MonitorWeight, Color(0xFF2563EB))
        "diabetes" -> Triple("Diabetes Management", Icons.Default.MonitorHeart, Color(0xFF9333EA))
        else -> Triple("General Health & Wellness", Icons.Default.TrackChanges, Color(0xFF16A34A))
    }

    val description = when(goalCode) {
        "hypertension" -> "Managing sodium intake and tracking blood pressure through balanced nutrition."
        "weight_loss" -> "Achieving sustainable weight loss through calorie management and portion control."
        "diabetes" -> "Maintaining stable blood sugar levels through carbohydrate tracking and meal timing."
        else -> "Building healthy eating habits and maintaining overall wellness."
    }

    Column {
        Text(
            "Health Goals",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(goalInfo.third.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(goalInfo.second, null, tint = goalInfo.third, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Primary Focus", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        Text(goalInfo.first, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description Box
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Button
                OutlinedButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, CalorieKoGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CalorieKoGreen)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- 4. Milestones Section ---
@Composable
fun MilestonesSection(currentStreak: Int) {
    // Mock Badges
    val badges = remember { listOf(
        Badge(1, "Sodium Defender", "3 Days Under Limit", Icons.Default.WaterDrop, Color(0xFFDBEAFE), Color(0xFF2563EB), true, 3, 3),
        Badge(2, "Scale Pro", "10 Meals Logged", Icons.Default.MonitorWeight, Color(0xFFDCFCE7), Color(0xFF16A34A), true, 10, 10),
        Badge(3, "Streak Master", "7 Day Streak", Icons.Default.LocalFireDepartment, Color(0xFFFFEDD5), Color(0xFFEA580C), true, 7, 7),
        Badge(4, "Photo Logger", "15 Photos Taken", Icons.Default.CameraAlt, Color(0xFFF3E8FF), Color(0xFF9333EA), false, 8, 15),
        Badge(5, "Workout Warrior", "5 Workouts Logged", Icons.Default.Bolt, Color(0xFFFEF3C7), Color(0xFFD97706), false, 2, 5),
        Badge(6, "Health Champion", "30 Day Streak", Icons.Default.MilitaryTech, Color(0xFFFCE7F3), Color(0xFFDB2777), false, 7, 30)
    )}

    val earned = badges.filter { it.earned }
    val inProgress = badges.filter { !it.earned }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Milestones & Achievements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${earned.size}/${badges.size}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
            }
        }

        // Earned Badges Grid
        if (earned.isNotEmpty()) {
            Text("Earned Badges", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                earned.forEach { badge ->
                    EarnedBadgeCard(badge, Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // In Progress List
        if (inProgress.isNotEmpty()) {
            Text("In Progress", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                inProgress.forEach { badge ->
                    InProgressBadgeCard(badge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Motivation Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Keep Going! 🎉", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You're making great progress! Complete more activities to unlock new badges and climb the leaderboard.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EarnedBadgeCard(badge: Badge, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(badge.colorBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge.icon, null, tint = badge.colorIcon, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(badge.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), maxLines = 1)
            Text(badge.description, fontSize = 10.sp, color = Color(0xFF6B7280), maxLines = 1)

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(50)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFF15803D), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Earned", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                }
            }
        }
    }
}

@Composable
fun InProgressBadgeCard(badge: Badge) {
    // Animate Progress Bar
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = badge.progress.toFloat() / badge.max.toFloat(),
            animationSpec = tween(durationMillis = 1000, delayMillis = 200, easing = LinearOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(badge.colorBg.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge.icon, null, tint = badge.colorIcon, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(badge.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                Text(badge.description, fontSize = 12.sp, color = Color(0xFF6B7280))

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE5E7EB))) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.value)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("${badge.progress}/${badge.max} completed", fontSize = 11.sp, color = Color(0xFF4B5563))
            }
        }
    }
}