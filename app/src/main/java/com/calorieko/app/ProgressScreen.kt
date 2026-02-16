package com.calorieko.app

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*
import androidx.core.graphics.toColorInt

// --- Data Models (Matching React Prototype) ---
data class DailyCalorie(val day: String, val intake: Int, val burned: Int)
data class DailySodium(val day: String, val sodium: Int)
data class DailyWeight(val day: String, val weight: Float)
data class TopFood(val name: String, val frequency: Int, val avgCalories: Int, val avgSodium: Int)

@Composable
fun ProgressScreen(onNavigate: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("progress") }
    var viewMode by remember { mutableStateOf("weekly") }

    // Mock Data - Weekly
    val weeklyCalorieData = remember { listOf(
        DailyCalorie("Mon", 1850, 450), DailyCalorie("Tue", 2100, 380),
        DailyCalorie("Wed", 1920, 520), DailyCalorie("Thu", 2050, 400),
        DailyCalorie("Fri", 1880, 350), DailyCalorie("Sat", 2200, 600),
        DailyCalorie("Sun", 1950, 420)
    )}

    val weeklySodiumData = remember { listOf(
        DailySodium("Mon", 1800), DailySodium("Tue", 2100),
        DailySodium("Wed", 1650), DailySodium("Thu", 2400),
        DailySodium("Fri", 1920), DailySodium("Sat", 2500),
        DailySodium("Sun", 1850)
    )}

    val weeklyWeightData = remember { listOf(
        DailyWeight("Mon", 75.2f), DailyWeight("Tue", 75.0f),
        DailyWeight("Wed", 74.8f), DailyWeight("Thu", 74.6f),
        DailyWeight("Fri", 74.4f), DailyWeight("Sat", 74.2f),
        DailyWeight("Sun", 74.0f)
    )}

    // Mock Data - Monthly (For demonstration of toggle)
    val monthlyCalorieData = remember { listOf(
        DailyCalorie("W1", 14000, 3000), DailyCalorie("W2", 13500, 2800),
        DailyCalorie("W3", 14200, 3200), DailyCalorie("W4", 13800, 2900)
    )}

    val monthlySodiumData = remember { listOf(
        DailySodium("W1", 2100), DailySodium("W2", 2450),
        DailySodium("W3", 1980), DailySodium("W4", 2200)
    )}

    val monthlyWeightData = remember { listOf(
        DailyWeight("W1", 76.5f), DailyWeight("W2", 75.8f),
        DailyWeight("W3", 75.0f), DailyWeight("W4", 74.0f)
    )}

    val topFoods = remember { listOf(
        TopFood("Chicken Adobo", 5, 380, 890),
        TopFood("Law-uy (Veg)", 4, 180, 520),
        TopFood("Grilled Fish", 4, 420, 450),
        TopFood("Pancit Canton", 3, 510, 1200)
    )}

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
        ) {
            // Header with Toggle
            ProgressHeader(viewMode = viewMode, onViewModeChange = { viewMode = it })

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Calorie Balance Chart
                item {
                    CalorieBalanceChart(
                        data = if (viewMode == "weekly") weeklyCalorieData else monthlyCalorieData
                    )
                }

                // 2. Sodium Trend Chart
                item {
                    SodiumTrendChart(
                        data = if (viewMode == "weekly") weeklySodiumData else monthlySodiumData,
                        limit = 2300
                    )
                }

                // 3. Weight Tracking Chart
                item {
                    WeightTrackingChart(
                        data = if (viewMode == "weekly") weeklyWeightData else monthlyWeightData
                    )
                }

                // 4. Dietary Insights
                item {
                    DietaryInsights(foods = topFoods)
                }

                // Bottom Spacer
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// --- 1. Header with Segmented Control ---
@Composable
fun ProgressHeader(viewMode: String, onViewModeChange: (String) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Your Progress",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Custom Segmented Control
            Box(
                modifier = Modifier
                    .background(Color(0xFFF3F4F6), CircleShape)
                    .padding(4.dp)
            ) {
                Row {
                    listOf("weekly" to "Weekly", "monthly" to "Monthly").forEach { (mode, label) ->
                        val isSelected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { onViewModeChange(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1F2937) else Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Animated Calorie Balance Chart ---
@Composable
fun CalorieBalanceChart(data: List<DailyCalorie>) {
    // Animation State
    var startAnimation by remember { mutableStateOf(false) }
    val progress = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "BarProgress"
    )

    LaunchedEffect(data) {
        startAnimation = false
        // Slight delay to restart animation on data change
        kotlinx.coroutines.delay(50)
        startAnimation = true
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Calorie Balance", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Text("Intake vs. Output", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            val maxVal = (data.maxOfOrNull { kotlin.math.max(it.intake, it.burned) } ?: 2500).toFloat() * 1.1f

            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 12.dp.toPx()
                    val groupSpacing = size.width / data.size
                    val chartHeight = size.height

                    // Draw Grid Lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = chartHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = Color(0xFFE5E7EB),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    data.forEachIndexed { index, day ->
                        val x = index * groupSpacing + (groupSpacing / 2)

                        // Intake Bar (Green)
                        val intakeHeight = (day.intake / maxVal) * chartHeight * progress.value
                        drawRoundRect(
                            color = CalorieKoGreen,
                            topLeft = Offset(x - barWidth - 2.dp.toPx(), chartHeight - intakeHeight),
                            size = Size(barWidth, intakeHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Burned Bar (Orange)
                        val burnedHeight = (day.burned / maxVal) * chartHeight * progress.value
                        drawRoundRect(
                            color = CalorieKoOrange,
                            topLeft = Offset(x + 2.dp.toPx(), chartHeight - burnedHeight),
                            size = Size(barWidth, burnedHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // X Axis Labels
                        drawContext.canvas.nativeCanvas.drawText(
                            day.day,
                            x,
                            chartHeight + 20.dp.toPx(),
                            Paint().apply {
                                color = "#9CA3AF".toColorInt()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            // Legend
            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Intake", CalorieKoGreen)
                LegendItem("Burned", CalorieKoOrange)
            }
        }
    }
}

// --- 3. Animated Sodium Trend Chart ---
@Composable
fun SodiumTrendChart(data: List<DailySodium>, limit: Int) {
    // Animation
    val animatableProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatableProgress.snapTo(0f)
        animatableProgress.animateTo(1f, animationSpec = tween(1500, easing = LinearOutSlowInEasing))
    }

    val daysOverLimit = data.count { it.sodium > limit }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Sodium Trend", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                    Text("Hypertension Monitoring", fontSize = 12.sp, color = Color.Gray)
                }
                if (daysOverLimit > 0) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$daysOverLimit days over", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxVal = (data.maxOfOrNull { it.sodium } ?: 3000).toFloat() * 1.2f
                    val chartHeight = size.height
                    val spacing = size.width / (data.size - 1)

                    // Draw Limit Line
                    val limitY = chartHeight - ((limit / maxVal) * chartHeight)
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(0f, limitY),
                        end = Offset(size.width, limitY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Label for limit
                    drawContext.canvas.nativeCanvas.drawText(
                        "Limit: ${limit}mg",
                        size.width - 10.dp.toPx(),
                        limitY - 6.dp.toPx(),
                        Paint().apply {
                            color = "#EF4444".toColorInt()
                            textSize = 10.sp.toPx()
                            textAlign = Paint.Align.RIGHT
                        }
                    )

                    // Construct Path
                    val path = Path()
                    data.forEachIndexed { index, day ->
                        val x = index * spacing
                        val y = chartHeight - ((day.sodium / maxVal) * chartHeight)
                        if (index == 0) path.moveTo(x, y) else {
                            // Bezier Curve
                            val prevX = (index - 1) * spacing
                            val prevY = chartHeight - ((data[index - 1].sodium / maxVal) * chartHeight)
                            val conX1 = prevX + (x - prevX) / 2
                            val conX2 = prevX + (x - prevX) / 2
                            path.cubicTo(conX1, prevY, conX2, y, x, y)
                        }

                        // Draw X Axis Labels
                        drawContext.canvas.nativeCanvas.drawText(
                            day.day,
                            x,
                            chartHeight + 20.dp.toPx(),
                            Paint().apply {
                                color = "#9CA3AF".toColorInt()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }

                    // Animate Path Drawing
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, false)
                    val length = pathMeasure.length
                    val partialPath = Path()
                    pathMeasure.getSegment(0f, length * animatableProgress.value, partialPath, true)

                    drawPath(
                        path = partialPath,
                        color = CalorieKoOrange,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw Dots (only if fully animated)
                    if (animatableProgress.value == 1f) {
                        data.forEachIndexed { index, day ->
                            val x = index * spacing
                            val y = chartHeight - ((day.sodium / maxVal) * chartHeight)
                            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                            drawCircle(CalorieKoOrange, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Animated Weight Chart & Insight ---
@Composable
fun WeightTrackingChart(data: List<DailyWeight>) {
    // Calculate Insight
    val startWeight = data.firstOrNull()?.weight ?: 0f
    val endWeight = data.lastOrNull()?.weight ?: 0f
    val change = endWeight - startWeight
    val isDown = change < 0
    val changeAbs = kotlin.math.abs(change)

    // Animation
    val animatableProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatableProgress.snapTo(0f)
        animatableProgress.animateTo(1f, animationSpec = tween(1500, easing = LinearOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Weight Metrics", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Text("Weight Changes Over Time", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minW = (data.minOfOrNull { it.weight } ?: 70f) - 1f
                    val maxW = (data.maxOfOrNull { it.weight } ?: 80f) + 1f
                    val range = maxW - minW
                    val chartHeight = size.height
                    val spacing = size.width / (data.size - 1)

                    // Grid Lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = chartHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = Color(0xFFF3F4F6),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val path = Path()
                    // Create gradient fill path
                    val fillPath = Path()

                    data.forEachIndexed { index, day ->
                        val x = index * spacing
                        val y = chartHeight - ((day.weight - minW) / range * chartHeight)
                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, chartHeight) // Start bottom-left
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * spacing
                            val prevY = chartHeight - ((data[index - 1].weight - minW) / range * chartHeight)
                            val conX1 = prevX + (x - prevX) / 2
                            val conX2 = prevX + (x - prevX) / 2
                            path.cubicTo(conX1, prevY, conX2, y, x, y)
                            fillPath.cubicTo(conX1, prevY, conX2, y, x, y)
                        }
                        // Draw X Axis Labels
                        drawContext.canvas.nativeCanvas.drawText(
                            day.day,
                            x,
                            chartHeight + 20.dp.toPx(),
                            Paint().apply {
                                color = "#9CA3AF".toColorInt()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }

                    // Close fill path
                    fillPath.lineTo(size.width, chartHeight)
                    fillPath.close()

                    // Draw Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CalorieKoGreen.copy(alpha = 0.2f), Color.Transparent),
                            endY = chartHeight
                        )
                    )

                    // Draw Line Animation
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, false)
                    val length = pathMeasure.length
                    val partialPath = Path()
                    pathMeasure.getSegment(0f, length * animatableProgress.value, partialPath, true)

                    drawPath(
                        path = partialPath,
                        color = CalorieKoGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw Dots (if finished)
                    if (animatableProgress.value == 1f) {
                        data.forEachIndexed { index, day ->
                            val x = index * spacing
                            val y = chartHeight - ((day.weight - minW) / range * chartHeight)
                            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                            drawCircle(CalorieKoGreen, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Insight Box
            Surface(
                color = if (isDown) Color(0xFFECFDF5) else Color(0xFFFFF7ED),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDown) Color(0xFFA7F3D0) else Color(0xFFFED7AA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isDown) Icons.AutoMirrored.Rounded.TrendingDown else if (change > 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.Rounded.Remove,
                        contentDescription = null,
                        tint = if (isDown) Color(0xFF047857) else Color(0xFFC2410C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Weekly Insight",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDown) Color(0xFF065F46) else Color(0xFF9A3412)
                        )
                        Text(
                            text = if (isDown) "You are down ${String.format(java.util.Locale.US, "%.1f", changeAbs)}kg this week—great job!"
                            else "Your weight increased by ${String.format(java.util.Locale.US, "%.1f", changeAbs)}kg this week.",
                            fontSize = 13.sp,
                            color = if (isDown) Color(0xFF065F46) else Color(0xFF9A3412),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// --- 5. Dietary Insights List ---
@Composable
fun DietaryInsights(foods: List<TopFood>) {
    Column {
        Text(
            text = "Dietary Insights",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Food Item", modifier = Modifier.weight(2f), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text("Freq", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Avg Cal", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }

                // List Items
                foods.forEachIndexed { index, food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(food.name, modifier = Modifier.weight(2f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                        Text(food.frequency.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("${food.avgCalories}", modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    if (index < foods.size - 1) {
                        HorizontalDivider(Modifier, thickness = 1.dp, color = Color(0xFFF3F4F6))
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}