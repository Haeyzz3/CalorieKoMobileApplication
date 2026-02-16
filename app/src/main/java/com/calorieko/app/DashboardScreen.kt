package com.calorieko.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── DATA MODELS ─────────────────────────────────────────

data class ActivityDetails(
    val weight: String? = null,
    val calories: Int,
    val sodium: Int? = null,
    val duration: String? = null
)

data class ActivityLogEntry(
    val id: String,
    val type: String, // "meal" or "workout"
    val time: String,
    val name: String,
    val details: ActivityDetails
)

// ─── DASHBOARD SCREEN ────────────────────────────────────

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf("home") }

    // Entrance animation trigger
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // --- Mock Data (matching Figma) ---
    val userName = "User"
    val targetCalories = 2450
    val targetSodium = 2300

    val activityLog = listOf(
        ActivityLogEntry(
            id = "1",
            type = "meal",
            time = "8:30 AM",
            name = "Chicken Adobo",
            details = ActivityDetails(weight = "250g", calories = 380, sodium = 890)
        ),
        ActivityLogEntry(
            id = "2",
            type = "workout",
            time = "7:00 AM",
            name = "Morning Walk",
            details = ActivityDetails(calories = 150, duration = "30 min")
        ),
        ActivityLogEntry(
            id = "3",
            type = "meal",
            time = "12:45 PM",
            name = "Grilled Fish with Rice",
            details = ActivityDetails(weight = "320g", calories = 420, sodium = 450)
        )
    )

    // Calculate totals from activity log
    val caloriesConsumed = activityLog
        .filter { it.type == "meal" }
        .sumOf { it.details.calories }

    val caloriesBurned = activityLog
        .filter { it.type == "workout" }
        .sumOf { it.details.calories }

    val currentCalories = caloriesConsumed - caloriesBurned

    val currentSodium = activityLog
        .filter { it.type == "meal" }
        .sumOf { it.details.sodium ?: 0 }

    // Mock macronutrient data
    val proteinCurrent = 65
    val proteinTarget = 120
    val carbsCurrent = 180
    val carbsTarget = 250
    val fatsCurrent = 45
    val fatsTarget = 65

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = { tab ->
                activeTab = tab
                if (tab != "home") {
                    onNavigate(tab)
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

            // ── Sticky Header ────────────────────────────
            DashboardHeader(userName = userName)

            // ── Scrollable Content ───────────────────────
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // Progress Rings Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(600, easing = EaseOutCubic)
                            )
                ) {
                    ProgressRingsCard(
                        caloriesCurrent = currentCalories,
                        caloriesTarget = targetCalories,
                        sodiumCurrent = currentSodium,
                        sodiumTarget = targetSodium,
                        proteinCurrent = proteinCurrent,
                        proteinTarget = proteinTarget,
                        carbsCurrent = carbsCurrent,
                        carbsTarget = carbsTarget,
                        fatsCurrent = fatsCurrent,
                        fatsTarget = fatsTarget
                    )
                }

                // Action Buttons Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 150)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(600, delayMillis = 150, easing = EaseOutCubic)
                            )
                ) {
                    ActionButtons(
                        onLogMeal = { onNavigate("logMeal") },
                        onLogWorkout = { onNavigate("logWorkout") }
                    )
                }

                // Daily Activity Feed Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(600, delayMillis = 300, easing = EaseOutCubic)
                            )
                ) {
                    DailyActivityFeed(activities = activityLog)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── HEADER (matches Figma: bg-white, px-6, py-5, shadow-sm, sticky) ───

@Composable
fun DashboardHeader(userName: String) {
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
                // "Hello, User!" (text-2xl font-bold text-gray-800)
                Text(
                    text = "Hello, $userName!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937) // gray-800
                )
                // Date (text-sm text-gray-500 mt-0.5)
                val currentDate = SimpleDateFormat(
                    "EEEE, MMMM d",
                    Locale.getDefault()
                ).format(Date())
                Text(
                    text = currentDate,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280), // gray-500
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Scale Connection Status (bg-green-50 rounded-full px-3 py-2)
            Surface(
                color = Color(0xFFF0FDF4), // green-50
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = Color(0xFF16A34A), // green-600
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scale Connected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF15803D) // green-700
                    )
                }
            }
        }
    }
}

// ─── PROGRESS RINGS CARD (matches Figma: bg-white rounded-2xl p-6 shadow-sm) ──

@Composable
fun ProgressRingsCard(
    caloriesCurrent: Int,
    caloriesTarget: Int,
    sodiumCurrent: Int,
    sodiumTarget: Int,
    proteinCurrent: Int,
    proteinTarget: Int,
    carbsCurrent: Int,
    carbsTarget: Int,
    fatsCurrent: Int,
    fatsTarget: Int
) {
    val calorieProgress = (caloriesCurrent.toFloat() / caloriesTarget).coerceIn(0f, 1f)
    val sodiumProgress = (sodiumCurrent.toFloat() / sodiumTarget).coerceIn(0f, 1f)
    val proteinProgress = (proteinCurrent.toFloat() / proteinTarget).coerceIn(0f, 1f)
    val carbsProgress = (carbsCurrent.toFloat() / carbsTarget).coerceIn(0f, 1f)
    val fatsProgress = (fatsCurrent.toFloat() / fatsTarget).coerceIn(0f, 1f)

    // Animated progress values
    val animCalorie by animateFloatAsState(
        targetValue = calorieProgress,
        animationSpec = tween(1200, 300, EaseOutCubic),
        label = "calorieAnim"
    )
    val animSodium by animateFloatAsState(
        targetValue = sodiumProgress,
        animationSpec = tween(1200, 500, EaseOutCubic),
        label = "sodiumAnim"
    )
    val animProtein by animateFloatAsState(
        targetValue = proteinProgress,
        animationSpec = tween(800, 600, EaseOutCubic),
        label = "proteinAnim"
    )
    val animCarbs by animateFloatAsState(
        targetValue = carbsProgress,
        animationSpec = tween(800, 700, EaseOutCubic),
        label = "carbsAnim"
    )
    val animFats by animateFloatAsState(
        targetValue = fatsProgress,
        animationSpec = tween(800, 800, EaseOutCubic),
        label = "fatsAnim"
    )

    // Animated counter
    val animCaloriesCount by animateIntAsState(
        targetValue = caloriesCurrent,
        animationSpec = tween(1200, 300, EaseOutCubic),
        label = "calorieCount"
    )
    val animSodiumCount by animateIntAsState(
        targetValue = sodiumCurrent,
        animationSpec = tween(1200, 500, EaseOutCubic),
        label = "sodiumCount"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── SVG-style Progress Rings (220x220, matching Figma) ──

            val ringSize = 220.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ringSize)
            ) {
                // Draw rings on Canvas
                Canvas(modifier = Modifier.size(ringSize)) {
                    val strokeW = 14.dp.toPx()
                    val canvasSize = size.minDimension
                    val center = canvasSize / 2f

                    // Outer ring (Calories) - radius 90
                    val outerRadius = 90.dp.toPx()
                    val outerCircumference = (2 * Math.PI * outerRadius).toFloat()
                    val outerTopLeft = Offset(
                        center - outerRadius,
                        center - outerRadius
                    )
                    val outerDiameter = outerRadius * 2

                    // Outer track (#E8F5E9)
                    drawArc(
                        color = Color(0xFFE8F5E9),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = Size(outerDiameter, outerDiameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Outer progress (#4CAF50)
                    drawArc(
                        color = Color(0xFF4CAF50),
                        startAngle = -90f,
                        sweepAngle = animCalorie * 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = Size(outerDiameter, outerDiameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Inner ring (Sodium) - radius 70
                    val innerRadius = 70.dp.toPx()
                    val innerTopLeft = Offset(
                        center - innerRadius,
                        center - innerRadius
                    )
                    val innerDiameter = innerRadius * 2

                    // Inner track (#FFF3E0)
                    drawArc(
                        color = Color(0xFFFFF3E0),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = Size(innerDiameter, innerDiameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Inner progress (#FF9800)
                    drawArc(
                        color = Color(0xFFFF9800),
                        startAngle = -90f,
                        sweepAngle = animSodium * 360f,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = Size(innerDiameter, innerDiameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Center text (absolute positioned in Figma)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Calories value (text-3xl font-bold text-gray-800)
                    Text(
                        text = "$animCaloriesCount",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    // / target kcal (text-xs text-gray-500)
                    Text(
                        text = "/ $caloriesTarget kcal",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    // Divider line (h-px w-12 bg-gray-200)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Sodium value (text-lg font-semibold text-orange-500)
                    Text(
                        text = "$animSodiumCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF97316) // orange-500
                    )
                    // / target mg (text-xs text-gray-500)
                    Text(
                        text = "/ $sodiumTarget mg",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Legend (flex justify-center gap-6) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Calories legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calories",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563) // gray-600
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Sodium legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sodium",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Macronutrients Breakdown (linear progress bars) ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroProgressBar(
                    label = "Protein",
                    current = proteinCurrent,
                    target = proteinTarget,
                    progress = animProtein,
                    color = Color(0xFF3B82F6), // blue-500
                    unit = "g"
                )
                MacroProgressBar(
                    label = "Carbs",
                    current = carbsCurrent,
                    target = carbsTarget,
                    progress = animCarbs,
                    color = Color(0xFFF59E0B), // amber-500
                    unit = "g"
                )
                MacroProgressBar(
                    label = "Fats",
                    current = fatsCurrent,
                    target = fatsTarget,
                    progress = animFats,
                    color = Color(0xFF8B5CF6), // purple-500
                    unit = "g"
                )
            }
        }
    }
}

// ── Macro Progress Bar (matches Figma linear bar style) ──

@Composable
fun MacroProgressBar(
    label: String,
    current: Int,
    target: Int,
    progress: Float,
    color: Color,
    unit: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label with dot (w-2 h-2 rounded-full)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(70.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF374151) // gray-700
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Progress bar (flex-1 bg-gray-100 rounded-full h-2)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFF3F4F6)) // gray-100
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Value text (text-sm font-medium text-gray-700)
        Text(
            text = "${current}${unit} / ${target}${unit}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
    }
}

// ─── ACTION BUTTONS (matches Figma: grid-cols-2 gap-4) ──────

@Composable
fun ActionButtons(onLogMeal: () -> Unit, onLogWorkout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Log Meal Button (bg-[#4CAF50], rounded-2xl, p-6)
        Button(
            onClick = onLogMeal,
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Camera + Scale icons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Log Meal",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "AI + Scale",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Log Workout Button (bg-[#FF9800], rounded-2xl, p-6)
        Button(
            onClick = onLogWorkout,
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Zap icon
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Log Workout",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "Track Activity",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ─── DAILY ACTIVITY FEED (matches Figma: bg-white rounded-2xl p-6 shadow-sm) ──

@Composable
fun DailyActivityFeed(activities: List<ActivityLogEntry>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // Title (font-semibold text-gray-800 mb-4)
            Text(
                text = "Today's Activity",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (activities.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9CA3AF).copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No activities logged yet",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "Start by logging a meal or workout",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                // Activity list (space-y-3)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activities.forEachIndexed { index, activity ->
                        // Staggered animation
                        var itemVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { itemVisible = true }

                        AnimatedVisibility(
                            visible = itemVisible,
                            enter = fadeIn(
                                animationSpec = tween(400, delayMillis = index * 100)
                            ) + slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = tween(
                                    400, delayMillis = index * 100,
                                    easing = EaseOutCubic
                                )
                            )
                        ) {
                            ActivityCard(activity)
                        }
                    }
                }
            }
        }
    }
}

// ── Activity Card (matches Figma: p-4 bg-gray-50 rounded-xl hover:bg-gray-100) ──

@Composable
fun ActivityCard(activity: ActivityLogEntry) {
    val isMeal = activity.type == "meal"

    Surface(
        color = Color(0xFFF9FAFB), // gray-50
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {

                // Icon (w-10 h-10 rounded-full)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMeal) Color(0xFFDCFCE7) // green-100
                            else Color(0xFFFFEDD5) // orange-100
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMeal) Icons.Default.Restaurant
                        else Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isMeal) Color(0xFF16A34A) // green-600
                        else Color(0xFFEA580C), // orange-600
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content (flex-1)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Name & Time
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
                                color = Color(0xFF6B7280), // gray-500
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Calories Badge (px-2 py-1 bg-white rounded-lg border)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFE5E7EB) // gray-200
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMeal) Icons.AutoMirrored.Filled.TrendingUp
                                    else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isMeal) Color(0xFF16A34A) // green-600
                                    else Color(0xFFEA580C), // orange-600
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

                    // Additional details row (weight, sodium, duration)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMeal && activity.details.weight != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MonitorWeight,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF4B5563) // gray-600
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activity.details.weight!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563)
                                )
                            }
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
    }
}