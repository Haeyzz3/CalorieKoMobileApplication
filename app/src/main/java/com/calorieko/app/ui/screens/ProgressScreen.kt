package com.calorieko.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProgressScreen(onNavigate: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val userDao = db.userDao()
    val activityLogDao = db.activityLogDao()

    var activeTab by remember { mutableStateOf("progress") }

    // Targets
    var targetCalories by remember { mutableStateOf(2000) }
    var targetBurned by remember { mutableStateOf(500) }
    var targetSodium by remember { mutableStateOf(2300) }
    var targetProtein by remember { mutableStateOf(150) }
    var targetCarbs by remember { mutableStateOf(200) }
    var targetFats by remember { mutableStateOf(65) }

    // Current values from today's logs
    var currentCalories by remember { mutableStateOf(0) }
    var caloriesBurned by remember { mutableStateOf(0) }
    var currentSodium by remember { mutableStateOf(0) }
    var currentProtein by remember { mutableStateOf(0) }
    var currentCarbs by remember { mutableStateOf(0) }
    var currentFats by remember { mutableStateOf(0) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            withContext(Dispatchers.IO) {
                val profile = userDao.getUser(uid)
                if (profile != null) {
                    val bmr = if (profile.sex.equals("Male", ignoreCase = true)) {
                        (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) + 5
                    } else {
                        (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) - 161
                    }
                    val activityMultiplier = when (profile.activityLevel) {
                        "lightly_active" -> 1.375
                        "active" -> 1.55
                        "very_active" -> 1.725
                        "not_very_active" -> 1.2
                        else -> 1.2
                    }
                    val tdee = bmr * activityMultiplier
                    targetCalories = when (profile.goal) {
                        "lose_weight", "weight_loss", "weight" -> (tdee - 500).toInt().coerceAtLeast(1200)
                        "gain_muscle" -> (tdee + 300).toInt()
                        else -> tdee.toInt()
                    }
                    val (proteinPct, carbsPct, fatsPct) = when (profile.goal) {
                        "lose_weight", "weight_loss", "weight" -> Triple(0.35, 0.35, 0.30)
                        "gain_muscle" -> Triple(0.30, 0.45, 0.25)
                        else -> Triple(0.30, 0.40, 0.30)
                    }
                    targetProtein = ((targetCalories * proteinPct) / 4).toInt()
                    targetCarbs = ((targetCalories * carbsPct) / 4).toInt()
                    targetFats = ((targetCalories * fatsPct) / 9).toInt()
                    targetSodium = 2300
                    targetBurned = 500
                }

                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis

                val logs = activityLogDao.getLogsForToday(uid, startOfDay)
                currentCalories = logs.filter { it.type == "meal" }.sumOf { it.calories }
                caloriesBurned = logs.filter { it.type == "workout" }.sumOf { it.calories }
                currentSodium = logs.filter { it.type == "meal" }.sumOf { it.sodium }
                currentProtein = logs.filter { it.type == "meal" }.sumOf { it.protein }
                currentCarbs = logs.filter { it.type == "meal" }.sumOf { it.carbs }
                currentFats = logs.filter { it.type == "meal" }.sumOf { it.fats }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                if (it != "progress") {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Progress",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            ProgressRings(
                caloriesCurrent = currentCalories,
                caloriesTarget = targetCalories,
                caloriesBurned = caloriesBurned,
                caloriesBurnedTarget = targetBurned,
                sodiumCurrent = currentSodium,
                sodiumTarget = targetSodium,
                proteinCurrent = currentProtein,
                proteinTarget = targetProtein,
                carbsCurrent = currentCarbs,
                carbsTarget = targetCarbs,
                fatsCurrent = currentFats,
                fatsTarget = targetFats
            )
        }
    }
}

@Composable
fun ProgressRings(
    caloriesCurrent: Int,
    caloriesTarget: Int,
    caloriesBurned: Int,
    caloriesBurnedTarget: Int,
    sodiumCurrent: Int,
    sodiumTarget: Int,
    proteinCurrent: Int,
    proteinTarget: Int,
    carbsCurrent: Int,
    carbsTarget: Int,
    fatsCurrent: Int,
    fatsTarget: Int
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Three Concentric Rings with Center Text ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                TripleRingChart(
                    size = 220.dp,
                    outerStrokeWidth = 10.dp,
                    middleStrokeWidth = 8.dp,
                    innerStrokeWidth = 7.dp,
                    caloriesCurrent = caloriesCurrent,
                    caloriesTarget = caloriesTarget,
                    burnedCurrent = caloriesBurned,
                    burnedTarget = caloriesBurnedTarget,
                    sodiumCurrent = sodiumCurrent,
                    sodiumTarget = sodiumTarget
                )

                // Center Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFFFAFAFA), CircleShape)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Consumed (Green)
                    Text(
                        text = "$caloriesCurrent",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C),
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "/ $caloriesTarget kcal",
                        fontSize = 10.sp,
                        color = Color(0xFF9E9E9E),
                        lineHeight = 12.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Burned (Red) + Sodium (Orange) as compact row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$caloriesBurned",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE53935),
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "burned",
                                fontSize = 8.sp,
                                color = Color(0xFFBDBDBD),
                                lineHeight = 10.sp
                            )
                        }

                        // Tiny divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFFE0E0E0))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${sodiumCurrent}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFB8C00),
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "mg Na",
                                fontSize = 8.sp,
                                color = Color(0xFFBDBDBD),
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. Legend Row (pill chips) ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0xFF4CAF50), label = "Eaten")
                LegendDot(color = Color(0xFFEF5350), label = "Burned")
                LegendDot(color = Color(0xFFFB8C00), label = "Sodium")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. Macro Progress Bars with divider ---
            HorizontalDivider(
                color = Color(0xFFF0F0F0),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            MacroProgressBar(
                label = "Protein",
                current = proteinCurrent,
                target = proteinTarget,
                color = Color(0xFF3B82F6),
                unit = "g"
            )
            Spacer(modifier = Modifier.height(16.dp))
            MacroProgressBar(
                label = "Carbs",
                current = carbsCurrent,
                target = carbsTarget,
                color = Color(0xFFFB8C00),
                unit = "g"
            )
            Spacer(modifier = Modifier.height(16.dp))
            MacroProgressBar(
                label = "Fats",
                current = fatsCurrent,
                target = fatsTarget,
                color = Color(0xFF8B5CF6),
                unit = "g"
            )
        }
    }
}

// --- Triple Ring Chart ---

@Composable
fun TripleRingChart(
    size: Dp,
    outerStrokeWidth: Dp,
    middleStrokeWidth: Dp,
    innerStrokeWidth: Dp,
    caloriesCurrent: Int,
    caloriesTarget: Int,
    burnedCurrent: Int,
    burnedTarget: Int,
    sodiumCurrent: Int,
    sodiumTarget: Int
) {
    val calorieProgress = (caloriesCurrent.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f)
    val burnedProgress = (burnedCurrent.toFloat() / burnedTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val sodiumProgress = (sodiumCurrent.toFloat() / sodiumTarget.toFloat()).coerceIn(0f, 1f)

    val animatedCalories by animateFloatAsState(targetValue = calorieProgress, animationSpec = tween(1000), label = "")
    val animatedBurned by animateFloatAsState(targetValue = burnedProgress, animationSpec = tween(1000), label = "")
    val animatedSodium by animateFloatAsState(targetValue = sodiumProgress, animationSpec = tween(1000), label = "")

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val outerStroke = outerStrokeWidth.toPx()
        val middleStroke = middleStrokeWidth.toPx()
        val innerStroke = innerStrokeWidth.toPx()
        val gap = 10.dp.toPx()

        // --- Outer Ring: Calories Consumed (Green) ---
        val outerRadius = (size.toPx() / 2) - (outerStroke / 2)
        drawCircle(
            color = Color(0xFFF1F8E9), // Very light green track
            radius = outerRadius,
            center = center,
            style = Stroke(width = outerStroke)
        )
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = 360f * animatedCalories,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = outerStroke, cap = StrokeCap.Round)
        )

        // --- Middle Ring: Calories Burned (Red) ---
        val middleRadius = outerRadius - outerStroke / 2 - gap - middleStroke / 2
        drawCircle(
            color = Color(0xFFFCE4EC), // Very light pink track
            radius = middleRadius,
            center = center,
            style = Stroke(width = middleStroke)
        )
        drawArc(
            color = Color(0xFFEF5350),
            startAngle = -90f,
            sweepAngle = 360f * animatedBurned,
            useCenter = false,
            topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
            size = Size(middleRadius * 2, middleRadius * 2),
            style = Stroke(width = middleStroke, cap = StrokeCap.Round)
        )

        // --- Inner Ring: Sodium (Orange) ---
        val innerRadius = middleRadius - middleStroke / 2 - gap - innerStroke / 2
        drawCircle(
            color = Color(0xFFFFF8E1), // Very light amber track
            radius = innerRadius,
            center = center,
            style = Stroke(width = innerStroke)
        )
        drawArc(
            color = Color(0xFFFB8C00),
            startAngle = -90f,
            sweepAngle = 360f * animatedSodium,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = innerStroke, cap = StrokeCap.Round)
        )
    }
}

// --- Legend Dot (pill chip style) ---

@Composable
fun LegendDot(color: Color, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF616161)
            )
        }
    }
}

// --- Horizontal Macro Progress Bar ---

@Composable
fun MacroProgressBar(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    unit: String
) {
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = label
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot + Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(76.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
        }

        // Progress bar - thinner, more refined
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = color,
            trackColor = Color(0xFFF5F5F5),
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Value text
        Text(
            text = "${current}$unit / ${target}$unit",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.width(80.dp)
        )
    }
}