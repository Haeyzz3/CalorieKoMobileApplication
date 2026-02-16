package com.calorieko.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ─── DATA MODELS ─────────────────────────────────────────

data class CalorieBalanceEntry(val day: String, val intake: Int, val burned: Int)
data class SodiumEntry(val day: String, val sodium: Int)
data class WeightEntry(val day: String, val weight: Float)
data class TopFood(
    val id: String,
    val name: String,
    val frequency: Int,
    val avgCalories: Int,
    val avgSodium: Int
)

// ─── PROGRESS SCREEN ─────────────────────────────────────

@Composable
fun ProgressScreen(
    onNavigate: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("progress") }
    var viewMode by remember { mutableStateOf("weekly") }

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // ── Mock Data ──
    val weeklyCalorieData = listOf(
        CalorieBalanceEntry("Mon", 1850, 450),
        CalorieBalanceEntry("Tue", 2100, 380),
        CalorieBalanceEntry("Wed", 1920, 520),
        CalorieBalanceEntry("Thu", 2050, 400),
        CalorieBalanceEntry("Fri", 1880, 350),
        CalorieBalanceEntry("Sat", 2200, 600),
        CalorieBalanceEntry("Sun", 1950, 420)
    )

    val weeklySodiumData = listOf(
        SodiumEntry("Mon", 1800), SodiumEntry("Tue", 2100),
        SodiumEntry("Wed", 1650), SodiumEntry("Thu", 2400),
        SodiumEntry("Fri", 1920), SodiumEntry("Sat", 2500),
        SodiumEntry("Sun", 1850)
    )
    val monthlySodiumData = listOf(
        SodiumEntry("Wk 1", 2100), SodiumEntry("Wk 2", 2450),
        SodiumEntry("Wk 3", 1980), SodiumEntry("Wk 4", 2200)
    )

    val weeklyWeightData = listOf(
        WeightEntry("Mon", 75.2f), WeightEntry("Tue", 75.0f),
        WeightEntry("Wed", 74.8f), WeightEntry("Thu", 74.6f),
        WeightEntry("Fri", 74.4f), WeightEntry("Sat", 74.2f),
        WeightEntry("Sun", 74.0f)
    )
    val monthlyWeightData = listOf(
        WeightEntry("Wk 1", 76.5f), WeightEntry("Wk 2", 75.8f),
        WeightEntry("Wk 3", 75.0f), WeightEntry("Wk 4", 74.0f)
    )

    val topFoods = listOf(
        TopFood("1", "Chicken Adobo", 5, 380, 890),
        TopFood("2", "Law-uy (Sautéed Vegetables)", 4, 180, 520),
        TopFood("3", "Grilled Fish with Rice", 4, 420, 450),
        TopFood("4", "Pancit Canton", 3, 510, 1200),
        TopFood("5", "Fruit Salad", 3, 150, 25)
    )

    val averageTDEE = weeklyCalorieData.map { it.intake - it.burned }.average().roundToInt()

    val sodiumData = if (viewMode == "weekly") weeklySodiumData else monthlySodiumData
    val weightData = if (viewMode == "weekly") weeklyWeightData else monthlyWeightData

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            // ── Header ──
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        "Your Progress",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekly / Monthly toggle
                    Surface(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            listOf("weekly" to "Weekly", "monthly" to "Monthly").forEach { (id, label) ->
                                Surface(
                                    color = if (viewMode == id) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(50),
                                    shadowElevation = if (viewMode == id) 1.dp else 0.dp,
                                    modifier = Modifier
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { viewMode = id }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (viewMode == id) Color(0xFF1F2937) else Color(0xFF4B5563),
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Calorie Balance Chart
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, easing = EaseOutCubic)
                    )
                ) {
                    CalorieBalanceChart(data = weeklyCalorieData, averageTDEE = averageTDEE)
                }

                // 2. Sodium Trend Chart
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 100, easing = EaseOutCubic)
                    )
                ) {
                    SodiumTrendChart(data = sodiumData, dailyLimit = 2300)
                }

                // 3. Weight Tracking Chart
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 200, easing = EaseOutCubic)
                    )
                ) {
                    WeightTrackingChart(data = weightData)
                }

                // 4. Dietary Insights
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 300, easing = EaseOutCubic)
                    )
                ) {
                    DietaryInsightsCard(topFoods = topFoods)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── CALORIE BALANCE BAR CHART ───────────────────────────

@Composable
fun CalorieBalanceChart(data: List<CalorieBalanceEntry>, averageTDEE: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Calorie Balance", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Intake vs. Output (Last 7 Days)", fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(16.dp))

            // Bar Chart (Canvas)
            val maxValue = data.maxOf { maxOf(it.intake, it.burned) }.toFloat()
            val chartMaxValue = ((maxValue / 500).toInt() + 1) * 500f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val leftPad = 40f
                val bottomPad = 30f
                val chartW = size.width - leftPad
                val chartH = size.height - bottomPad
                val barGroupWidth = chartW / data.size
                val barWidth = barGroupWidth * 0.3f
                val gridSteps = 5

                // Grid lines
                for (i in 0..gridSteps) {
                    val y = chartH - (chartH * i / gridSteps)
                    drawLine(
                        color = Color(0xFFF0F0F0),
                        start = Offset(leftPad, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    // Y-axis labels
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(chartMaxValue * i / gridSteps).toInt()}",
                        0f, y + 4f,
                        android.graphics.Paint().apply {
                            textSize = 24f
                            color = 0xFF9CA3AF.toInt()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // Bars
                data.forEachIndexed { index, entry ->
                    val groupX = leftPad + index * barGroupWidth
                    val intakeH = (entry.intake / chartMaxValue) * chartH
                    val burnedH = (entry.burned / chartMaxValue) * chartH

                    // Intake bar (green)
                    drawRoundRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(groupX + barGroupWidth * 0.15f, chartH - intakeH),
                        size = androidx.compose.ui.geometry.Size(barWidth, intakeH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                    // Burned bar (orange)
                    drawRoundRect(
                        color = Color(0xFFFF9800),
                        topLeft = Offset(groupX + barGroupWidth * 0.15f + barWidth + 4f, chartH - burnedH),
                        size = androidx.compose.ui.geometry.Size(barWidth, burnedH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )

                    // X-axis labels
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.day,
                        groupX + barGroupWidth / 2,
                        size.height - 4f,
                        android.graphics.Paint().apply {
                            textSize = 26f
                            color = 0xFF9CA3AF.toInt()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            // Legend
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Text("Intake", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                    Text("Burned", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }

            // Weekly Average TDEE
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Average TDEE", fontSize = 14.sp, color = Color(0xFF4B5563))
                Text("$averageTDEE kcal", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            }
        }
    }
}

// ─── SODIUM TREND LINE CHART ─────────────────────────────

@Composable
fun SodiumTrendChart(data: List<SodiumEntry>, dailyLimit: Int) {
    val daysExceeded = data.count { it.sodium > dailyLimit }
    val averageSodium = data.map { it.sodium }.average().roundToInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Sodium Trend", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hypertension Monitoring", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
                if (daysExceeded > 0) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFEF4444)
                            )
                            Text(
                                "$daysExceeded day${if (daysExceeded > 1) "s" else ""} over limit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Line chart Canvas
            val maxSodium = (data.maxOf { it.sodium } + 500).toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val leftPad = 40f
                val bottomPad = 30f
                val rightPad = 10f
                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - bottomPad

                // Grid lines
                val gridSteps = 5
                for (i in 0..gridSteps) {
                    val y = chartH - (chartH * i / gridSteps)
                    drawLine(Color(0xFFF0F0F0), Offset(leftPad, y), Offset(size.width - rightPad, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(maxSodium * i / gridSteps).toInt()}",
                        0f, y + 4f,
                        android.graphics.Paint().apply {
                            textSize = 22f; color = 0xFF9CA3AF.toInt()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // Dashed limit line
                val limitY = chartH - (dailyLimit / maxSodium) * chartH
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(leftPad, limitY),
                    end = Offset(size.width - rightPad, limitY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
                // Limit label
                drawContext.canvas.nativeCanvas.drawText(
                    "Limit: ${dailyLimit}mg",
                    size.width - rightPad - 10f, limitY - 8f,
                    android.graphics.Paint().apply {
                        textSize = 22f; color = 0xFFEF4444.toInt()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )

                // Line path
                val points = data.mapIndexed { index, entry ->
                    val x = leftPad + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartW
                    val y = chartH - (entry.sodium / maxSodium) * chartH
                    Offset(x, y)
                }

                // Draw line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFFFF9800),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw dots
                points.forEach { pt ->
                    drawCircle(Color.White, radius = 10f, center = pt)
                    drawCircle(Color(0xFFFF9800), radius = 7f, center = pt)
                }

                // X-axis labels
                data.forEachIndexed { index, entry ->
                    val x = leftPad + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartW
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.day, x, size.height - 4f,
                        android.graphics.Paint().apply {
                            textSize = 26f; color = 0xFF9CA3AF.toInt()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            // Weekly Average stat
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Average", fontSize = 14.sp, color = Color(0xFF4B5563))
                Text(
                    "$averageSodium mg",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (averageSodium > dailyLimit) Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            }
        }
    }
}

// ─── WEIGHT TRACKING LINE CHART ──────────────────────────

@Composable
fun WeightTrackingChart(data: List<WeightEntry>) {
    val startWeight = data.firstOrNull()?.weight ?: 0f
    val endWeight = data.lastOrNull()?.weight ?: 0f
    val weightChange = endWeight - startWeight
    val weightChangeAbs = kotlin.math.abs(weightChange)
    val trend = when {
        weightChange < -0.05f -> "down"
        weightChange > 0.05f -> "up"
        else -> "stable"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Weight & Body Metrics", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Weight Changes Over Time", fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(16.dp))

            // Line Chart Canvas
            val minW = data.minOf { it.weight } - 2f
            val maxW = data.maxOf { it.weight } + 2f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val leftPad = 50f
                val bottomPad = 30f
                val rightPad = 10f
                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - bottomPad

                // Grid lines
                val gridSteps = 5
                for (i in 0..gridSteps) {
                    val y = chartH - (chartH * i / gridSteps)
                    val value = minW + (maxW - minW) * i / gridSteps
                    drawLine(Color(0xFFF0F0F0), Offset(leftPad, y), Offset(size.width - rightPad, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", value),
                        leftPad - 8f, y + 4f,
                        android.graphics.Paint().apply {
                            textSize = 22f; color = 0xFF9CA3AF.toInt()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // Line path
                val points = data.mapIndexed { index, entry ->
                    val x = leftPad + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartW
                    val y = chartH - ((entry.weight - minW) / (maxW - minW)) * chartH
                    Offset(x, y)
                }

                // Smooth line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Dots
                points.forEach { pt ->
                    drawCircle(Color.White, radius = 10f, center = pt)
                    drawCircle(Color(0xFF4CAF50), radius = 7f, center = pt)
                }

                // X-axis labels
                data.forEachIndexed { index, entry ->
                    val x = leftPad + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartW
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.day, x, size.height - 4f,
                        android.graphics.Paint().apply {
                            textSize = 26f; color = 0xFF9CA3AF.toInt()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            // Insight box
            Spacer(modifier = Modifier.height(16.dp))

            val (insightBg, insightBorder, insightText) = when (trend) {
                "down" -> Triple(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF15803D))
                "up" -> Triple(Color(0xFFFFF7ED), Color(0xFFFED7AA), Color(0xFFC2410C))
                else -> Triple(Color(0xFFF9FAFB), Color(0xFFE5E7EB), Color(0xFF374151))
            }
            val insightIcon = when (trend) {
                "down" -> Icons.AutoMirrored.Filled.TrendingDown
                "up" -> Icons.AutoMirrored.Filled.TrendingUp
                else -> Icons.Default.Remove
            }
            val insightMessage = when (trend) {
                "down" -> "You are down ${String.format("%.1f", weightChangeAbs)}kg this week—great job!"
                "up" -> "Your weight increased by ${String.format("%.1f", weightChangeAbs)}kg this week."
                else -> "Your weight has remained stable this week."
            }

            Surface(
                color = insightBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, insightBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        insightIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = insightText
                    )
                    Column {
                        Text(
                            "Weekly Insight",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = insightText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            insightMessage,
                            fontSize = 14.sp,
                            color = insightText
                        )
                    }
                }
            }
        }
    }
}

// ─── DIETARY INSIGHTS ────────────────────────────────────

@Composable
fun DietaryInsightsCard(topFoods: List<TopFood>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Dietary Insights", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Most Frequently Logged Foods", fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal scrollable food cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(topFoods) { food ->
                    FoodInsightCard(food = food)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "💡 Tip: Identifying high-sodium foods helps manage hypertension",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun FoodInsightCard(food: TopFood) {
    val isHighSodium = food.avgSodium > 800

    Surface(
        color = Color(0xFFF9FAFB),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.width(192.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Name + warning icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    food.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isHighSodium) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency badge
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Logged ${food.frequency}x this week",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Avg Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg. Calories", fontSize = 12.sp, color = Color(0xFF4B5563))
                Text(
                    "${food.avgCalories} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Avg Sodium
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg. Sodium", fontSize = 12.sp, color = Color(0xFF4B5563))
                Text(
                    "${food.avgSodium} mg",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighSodium) Color(0xFFEA580C) else Color(0xFF374151)
                )
            }

            // High sodium warning
            if (isHighSodium) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "High sodium content",
                    fontSize = 12.sp,
                    color = Color(0xFFEA580C)
                )
            }
        }
    }
}
