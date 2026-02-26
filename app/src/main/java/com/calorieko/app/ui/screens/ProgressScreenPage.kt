package com.calorieko.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.ui.components.BottomNavigation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

// ==================== DATA MODELS ====================

private data class DayCalorieData(val dayLabel: String, val intake: Int, val burned: Int)
private data class DaySodiumData(val dayLabel: String, val sodium: Int)
private data class DayWeightData(val dayLabel: String, val weight: Double)
private data class TopFoodItem(val name: String, val frequency: Int, val avgCalories: Int, val avgSodium: Int)

// ==================== MAIN SCREEN ====================

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
    var viewMode by remember { mutableStateOf("weekly") }

    var weeklyLogs by remember { mutableStateOf<List<ActivityLogEntity>>(emptyList()) }
    var userWeight by remember { mutableStateOf(74.0) }
    var dataLoaded by remember { mutableStateOf(false) }

    // Fetch data
    LaunchedEffect(currentUser?.uid, viewMode) {
        currentUser?.uid?.let { uid ->
            withContext(Dispatchers.IO) {
                val profile = userDao.getUser(uid)
                if (profile != null) userWeight = profile.weight

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val endTime = calendar.timeInMillis + 24 * 60 * 60 * 1000L

                val daysBack = if (viewMode == "weekly") 7 else 30
                calendar.add(Calendar.DAY_OF_YEAR, -(daysBack - 1))
                val startTime = calendar.timeInMillis

                weeklyLogs = activityLogDao.getLogsForRange(uid, startTime, endTime)
            }
            dataLoaded = true
        }
        if (currentUser == null) dataLoaded = true
    }

    // Process chart data
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val calorieData = remember(weeklyLogs) {
        dayLabels.mapIndexed { index, label ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.add(Calendar.DAY_OF_YEAR, index)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            val dayLogs = weeklyLogs.filter { it.timestamp in dayStart until dayEnd }
            DayCalorieData(
                label,
                dayLogs.filter { it.type == "meal" }.sumOf { it.calories },
                dayLogs.filter { it.type == "workout" }.sumOf { it.calories }
            )
        }
    }

    val sodiumData = remember(weeklyLogs) {
        dayLabels.mapIndexed { index, label ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.add(Calendar.DAY_OF_YEAR, index)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            val dayLogs = weeklyLogs.filter { it.timestamp in dayStart until dayEnd }
            DaySodiumData(label, dayLogs.filter { it.type == "meal" }.sumOf { it.sodium })
        }
    }

    val weightData = remember(userWeight) {
        // Simulated trend from user's profile weight
        dayLabels.mapIndexed { index, label ->
            DayWeightData(label, userWeight + (6 - index) * 0.2)
        }
    }

    val topFoods = remember(weeklyLogs) {
        weeklyLogs.filter { it.type == "meal" }
            .groupBy { it.name }
            .map { (name, logs) ->
                TopFoodItem(
                    name = name,
                    frequency = logs.size,
                    avgCalories = if (logs.isNotEmpty()) logs.sumOf { it.calories } / logs.size else 0,
                    avgSodium = if (logs.isNotEmpty()) logs.sumOf { it.sodium } / logs.size else 0
                )
            }
            .sortedByDescending { it.frequency }
            .take(5)
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                if (it != "progress") onNavigate(it)
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== HEADER =====
            ProgressHeaderSection(viewMode = viewMode, onViewModeChange = { viewMode = it })

            // ===== CHART SECTIONS =====
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Stagger entrance animations
                AnimatedVisibility(
                    visible = dataLoaded,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                ) {
                    CalorieBalanceCard(data = calorieData)
                }

                AnimatedVisibility(
                    visible = dataLoaded,
                    enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100)) { it / 4 }
                ) {
                    SodiumTrendCard(data = sodiumData, dailyLimit = 2300)
                }

                AnimatedVisibility(
                    visible = dataLoaded,
                    enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(tween(400, delayMillis = 200)) { it / 4 }
                ) {
                    WeightTrackingCard(data = weightData)
                }

                AnimatedVisibility(
                    visible = dataLoaded,
                    enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(tween(400, delayMillis = 300)) { it / 4 }
                ) {
                    DietaryInsightsCard(foods = topFoods)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== HEADER with TOGGLE ====================

@Composable
private fun ProgressHeaderSection(viewMode: String, onViewModeChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Your Progress",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pill toggle — matches Figma: inline-flex bg-gray-100 rounded-full p-1
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF3F4F6))
                    .padding(4.dp)
            ) {
                listOf("weekly" to "Weekly", "monthly" to "Monthly").forEach { (key, label) ->
                    val isSelected = viewMode == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(50))
                                else Modifier
                            )
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { onViewModeChange(key) }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF1F2937) else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

// ==================== CALORIE BALANCE CHART ====================
// Figma: BarChart with rounded top bars, green intake (#4CAF50), orange burned (#FF9800)
// Dashed grid lines, legend with circle icons, Weekly Average TDEE footer

@Composable
private fun CalorieBalanceCard(data: List<DayCalorieData>) {
    val maxValue = (data.maxOfOrNull { maxOf(it.intake, it.burned) } ?: 2200).coerceAtLeast(100)
    val averageTDEE = if (data.isNotEmpty()) {
        data.sumOf { it.intake - it.burned } / data.size
    } else 0
    val yMax = ((maxValue / 550) + 1) * 550

    // Animate bars growing up
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Calorie Balance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Intake vs. Output (Last 7 Days)",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // === BAR CHART ===
            val density = LocalDensity.current
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val chartW = size.width
                val chartH = size.height
                val leftPad = with(density) { 36.dp.toPx() }
                val bottomPad = with(density) { 24.dp.toPx() }
                val drawW = chartW - leftPad
                val drawH = chartH - bottomPad
                val progress = animProgress.value

                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9CA3AF")
                    textSize = with(density) { 11.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val yLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9CA3AF")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }

                // Dashed horizontal grid lines (matches recharts strokeDasharray="3 3")
                val ySteps = 4
                for (i in 0..ySteps) {
                    val value = (yMax.toFloat() / ySteps) * i
                    val y = drawH - (drawH * (value / yMax.toFloat()))
                    drawLine(
                        color = Color(0xFFF0F0F0),
                        start = Offset(leftPad, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        value.roundToInt().toString(),
                        leftPad - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() },
                        yLabelPaint
                    )
                }

                // Bars with animated height
                val barGroupW = drawW / data.size
                val barW = barGroupW * 0.3f
                val gap = barGroupW * 0.04f
                val cornerR = with(density) { 4.dp.toPx() }

                data.forEachIndexed { index, day ->
                    val cx = leftPad + barGroupW * (index + 0.5f)

                    // Intake bar (green) — animated
                    val intakeH = (day.intake.toFloat() / yMax) * drawH * progress
                    if (intakeH > 0) {
                        drawRoundRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(cx - barW - gap / 2, drawH - intakeH),
                            size = Size(barW, intakeH),
                            cornerRadius = CornerRadius(cornerR, cornerR)
                        )
                    }

                    // Burned bar (orange) — animated
                    val burnedH = (day.burned.toFloat() / yMax) * drawH * progress
                    if (burnedH > 0) {
                        drawRoundRect(
                            color = Color(0xFFFF9800),
                            topLeft = Offset(cx + gap / 2, drawH - burnedH),
                            size = Size(barW, burnedH),
                            cornerRadius = CornerRadius(cornerR, cornerR)
                        )
                    }

                    // X-axis day label
                    drawContext.canvas.nativeCanvas.drawText(
                        day.dayLabel,
                        cx,
                        chartH - with(density) { 4.dp.toPx() },
                        labelPaint
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend row (circles like recharts iconType="circle")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).background(Color(0xFF4CAF50), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Intake", fontSize = 12.sp, color = Color(0xFF6B7280))
                Spacer(Modifier.width(24.dp))
                Box(Modifier.size(10.dp).background(Color(0xFFFF9800), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Burned", fontSize = 12.sp, color = Color(0xFF6B7280))
            }

            // Divider + Weekly Average TDEE (matches border-t border-gray-100)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Average TDEE", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                Text(
                    text = "$averageTDEE kcal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}

// ==================== SODIUM TREND CHART ====================
// Figma: LineChart monotone, orange line (#FF9800), red dashed ReferenceLine (#ef4444),
// AlertTriangle badge for days over limit, weekly average footer

@Composable
private fun SodiumTrendCard(data: List<DaySodiumData>, dailyLimit: Int) {
    val daysOverLimit = data.count { it.sodium > dailyLimit }
    val weeklyAverage = if (data.isNotEmpty()) data.sumOf { it.sodium } / data.size else 0
    val maxSodium = (data.maxOfOrNull { it.sodium } ?: 3000).coerceAtLeast(dailyLimit + 500)
    val yMax = ((maxSodium / 500) + 1) * 500

    // Animate line drawing
    val lineProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        lineProgress.snapTo(0f)
        lineProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header row with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Sodium Trend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hypertension Monitoring",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                // "X days over limit" badge (matches bg-red-50 rounded-lg)
                if (daysOverLimit > 0) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFEF4444)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$daysOverLimit day${if (daysOverLimit > 1) "s" else ""} over limit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === LINE CHART ===
            val density = LocalDensity.current
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val chartW = size.width
                val chartH = size.height
                val leftPad = with(density) { 40.dp.toPx() }
                val bottomPad = with(density) { 24.dp.toPx() }
                val drawW = chartW - leftPad
                val drawH = chartH - bottomPad
                val progress = lineProgress.value

                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9CA3AF")
                    textSize = with(density) { 11.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val yLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9CA3AF")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                val limitLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#EF4444")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }

                // Dashed grid + Y-axis labels
                val ySteps = 4
                for (i in 0..ySteps) {
                    val value = (yMax.toFloat() / ySteps) * i
                    val y = drawH - (drawH * (value / yMax.toFloat()))
                    drawLine(
                        color = Color(0xFFF0F0F0),
                        start = Offset(leftPad, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        value.roundToInt().toString(),
                        leftPad - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() },
                        yLabelPaint
                    )
                }

                // Red dashed limit line (matches ReferenceLine stroke="#ef4444" strokeDasharray="5 5")
                val limitY = drawH - (drawH * (dailyLimit.toFloat() / yMax.toFloat()))
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(leftPad, limitY),
                    end = Offset(chartW, limitY),
                    strokeWidth = with(density) { 2.dp.toPx() },
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                // Limit label on right
                drawContext.canvas.nativeCanvas.drawText(
                    "Limit: ${dailyLimit}mg",
                    chartW - with(density) { 4.dp.toPx() },
                    limitY - with(density) { 6.dp.toPx() },
                    limitLabelPaint
                )

                // Compute all points
                if (data.isNotEmpty()) {
                    val points = data.mapIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        val y = drawH - (drawH * (day.sodium.toFloat() / yMax.toFloat()))
                        Offset(x, y)
                    }

                    // Animated monotone line
                    val totalPoints = points.size
                    val drawnCount = (totalPoints * progress).toInt().coerceAtLeast(1)

                    val path = Path()
                    for (i in 0 until drawnCount.coerceAtMost(totalPoints)) {
                        if (i == 0) path.moveTo(points[i].x, points[i].y)
                        else path.lineTo(points[i].x, points[i].y)
                    }
                    // Partial segment for smooth animation
                    if (drawnCount < totalPoints && progress > 0) {
                        val frac = (totalPoints * progress) - drawnCount
                        if (frac > 0 && drawnCount < totalPoints) {
                            val from = points[drawnCount - 1]
                            val to = points[drawnCount]
                            path.lineTo(from.x + (to.x - from.x) * frac, from.y + (to.y - from.y) * frac)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFF9800),
                        style = Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
                    )

                    // Draw dots (fill=#FF9800, strokeWidth=2, r=4) — animated
                    for (i in 0 until drawnCount.coerceAtMost(totalPoints)) {
                        drawCircle(Color(0xFFFF9800), radius = with(density) { 5.dp.toPx() }, center = points[i])
                        drawCircle(Color.White, radius = with(density) { 2.5.dp.toPx() }, center = points[i])
                    }

                    // X-axis labels
                    data.forEachIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        drawContext.canvas.nativeCanvas.drawText(
                            day.dayLabel, x, chartH - with(density) { 4.dp.toPx() }, labelPaint
                        )
                    }
                }
            }

            // Footer: Weekly Average (matches border-t border-gray-100)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Average", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                Text(
                    text = "$weeklyAverage mg",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (weeklyAverage > dailyLimit) Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            }
        }
    }
}

// ==================== WEIGHT & BODY METRICS ====================
// Figma: LineChart monotone green (#4CAF50), dots, insight box with TrendingDown/Up icon

@Composable
private fun WeightTrackingCard(data: List<DayWeightData>) {
    val startWeight = data.firstOrNull()?.weight ?: 0.0
    val endWeight = data.lastOrNull()?.weight ?: 0.0
    val weightChange = endWeight - startWeight
    val weightChangeAbs = abs(weightChange)
    val trend = when {
        weightChange < -0.05 -> "down"
        weightChange > 0.05 -> "up"
        else -> "stable"
    }

    val minW = (data.minOfOrNull { it.weight } ?: 70.0) - 2.0
    val maxW = (data.maxOfOrNull { it.weight } ?: 76.0) + 2.0
    val range = (maxW - minW).coerceAtLeast(0.1)

    // Animate line
    val lineProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        lineProgress.snapTo(0f)
        lineProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Weight & Body Metrics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Weight Changes Over Time", fontSize = 12.sp, color = Color(0xFF9CA3AF))

            Spacer(modifier = Modifier.height(16.dp))

            // === LINE CHART ===
            val density = LocalDensity.current
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val chartW = size.width
                val chartH = size.height
                val leftPad = with(density) { 40.dp.toPx() }
                val bottomPad = with(density) { 24.dp.toPx() }
                val drawW = chartW - leftPad
                val drawH = chartH - bottomPad
                val progress = lineProgress.value

                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9CA3AF")
                    textSize = with(density) { 11.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val yLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9CA3AF")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }

                // Dashed grid
                val ySteps = 3
                for (i in 0..ySteps) {
                    val value = minW + (range / ySteps) * i
                    val y = drawH - (drawH * ((value - minW) / range)).toFloat()
                    drawLine(
                        color = Color(0xFFF0F0F0),
                        start = Offset(leftPad, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", value),
                        leftPad - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() },
                        yLabelPaint
                    )
                }

                if (data.isNotEmpty()) {
                    val points = data.mapIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        val y = drawH - (drawH * ((day.weight - minW) / range)).toFloat()
                        Offset(x, y)
                    }

                    // Animated line
                    val totalPts = points.size
                    val drawnCount = (totalPts * progress).toInt().coerceAtLeast(1)
                    val path = Path()
                    for (i in 0 until drawnCount.coerceAtMost(totalPts)) {
                        if (i == 0) path.moveTo(points[i].x, points[i].y)
                        else path.lineTo(points[i].x, points[i].y)
                    }
                    if (drawnCount < totalPts && progress > 0) {
                        val frac = (totalPts * progress) - drawnCount
                        if (frac > 0 && drawnCount < totalPts) {
                            val from = points[drawnCount - 1]
                            val to = points[drawnCount]
                            path.lineTo(from.x + (to.x - from.x) * frac, from.y + (to.y - from.y) * frac)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4CAF50),
                        style = Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
                    )

                    // Dots (matches fill=#4CAF50, strokeWidth=2, r=4)
                    for (i in 0 until drawnCount.coerceAtMost(totalPts)) {
                        drawCircle(Color(0xFF4CAF50), radius = with(density) { 5.dp.toPx() }, center = points[i])
                        drawCircle(Color.White, radius = with(density) { 2.5.dp.toPx() }, center = points[i])
                    }

                    // X-axis labels
                    data.forEachIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        drawContext.canvas.nativeCanvas.drawText(
                            day.dayLabel, x, chartH - with(density) { 4.dp.toPx() }, labelPaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Insight Box (matches rounded-xl border + color-coded bg)
            val insightBg = when (trend) {
                "down" -> Color(0xFFF0FDF4)
                "up" -> Color(0xFFFFF7ED)
                else -> Color(0xFFF9FAFB)
            }
            val insightBorder = when (trend) {
                "down" -> Color(0xFFBBF7D0)
                "up" -> Color(0xFFFED7AA)
                else -> Color(0xFFE5E7EB)
            }
            val insightTextColor = when (trend) {
                "down" -> Color(0xFF15803D)
                "up" -> Color(0xFFC2410C)
                else -> Color(0xFF374151)
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = insightBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, insightBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Trend icon
                    when (trend) {
                        "down" -> Icon(
                            Icons.AutoMirrored.Outlined.TrendingDown, null,
                            Modifier.size(18.dp), tint = Color(0xFF16A34A)
                        )
                        "up" -> Icon(
                            Icons.AutoMirrored.Outlined.TrendingUp, null,
                            Modifier.size(18.dp), tint = Color(0xFFEA580C)
                        )
                        else -> Icon(
                            Icons.Outlined.TipsAndUpdates, null,
                            Modifier.size(18.dp), tint = Color(0xFF6B7280)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Weekly Insight",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = insightTextColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (trend) {
                                "down" -> "You are down ${String.format("%.1f", weightChangeAbs)}kg this week—great job!"
                                "up" -> "Your weight increased by ${String.format("%.1f", weightChangeAbs)}kg this week."
                                else -> "Your weight has remained stable this week."
                            },
                            fontSize = 14.sp,
                            color = insightTextColor,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== DIETARY INSIGHTS ====================
// Figma: Horizontal scrollable cards (w-48), high sodium threshold >800mg,
// AlertCircle orange icon, hover border, tip at bottom

@Composable
private fun DietaryInsightsCard(foods: List<TopFoodItem>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Dietary Insights", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(2.dp))
            Text("Most Frequently Logged Foods", fontSize = 12.sp, color = Color(0xFF9CA3AF))

            Spacer(modifier = Modifier.height(16.dp))

            if (foods.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No meals logged yet this week", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                }
            } else {
                // Horizontal scrollable food cards (matches flex gap-4 overflow-x-auto)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    foods.forEach { food ->
                        FoodInsightCard(food = food)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tip (matches text-xs text-gray-500)
            Text(
                text = "💡 Tip: Identifying high-sodium foods helps manage hypertension",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun FoodInsightCard(food: TopFoodItem) {
    val isHighSodium = food.avgSodium > 800 // Matches prototype threshold

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.width(192.dp) // w-48 = 12rem ≈ 192dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Food name + warning icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = food.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isHighSodium) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "High sodium",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFF97316) // orange-500
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency badge (matches bg-white rounded-md)
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = buildString {
                        append("Logged ")
                        append(food.frequency)
                        append("x this week")
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Avg. Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg. Calories", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Text(
                    "${food.avgCalories} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A) // green-600
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avg. Sodium
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg. Sodium", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Text(
                    "${food.avgSodium} mg",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighSodium) Color(0xFFEA580C) else Color(0xFF374151) // orange-600 or gray-700
                )
            }

            // High sodium warning (matches border-t border-gray-200, text-orange-600)
            if (isHighSodium) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High sodium content",
                    fontSize = 12.sp,
                    color = Color(0xFFEA580C)
                )
            }
        }
    }
}
