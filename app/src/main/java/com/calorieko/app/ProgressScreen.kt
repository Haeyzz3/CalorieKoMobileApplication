package com.calorieko.app

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*

@Composable
fun ProgressScreen(onNavigate: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("progress") }
    val scrollState = rememberScrollState()

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
            // --- Header ---
            ProgressHeader()

            // --- Scrollable Content ---
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Weight Chart
                WeightTrackingCard()

                // 2. Calorie Balance
                CalorieBalanceCard()

                // 3. Sodium Trend
                SodiumTrendCard()

                // 4. Dietary Insights
                DietaryInsightsSection()

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// --- 1. Header Component ---
@Composable
fun ProgressHeader() {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Progress",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Date Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Last 7 Days",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }

                IconButton(onClick = { }) {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }
        }
    }
}

// --- 2. Weight Chart Component ---
@Composable
fun WeightTrackingCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Weight Trend", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Current: 78.5 kg", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "-1.2 kg",
                        color = Color(0xFF059669),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Line Chart
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = listOf(79.5f, 79.2f, 79.0f, 78.8f, 78.9f, 78.6f, 78.5f)
                    val width = size.width
                    val height = size.height
                    val spacing = width / (points.size - 1)

                    // Normalize points to fit height
                    val min = points.min() - 0.5f
                    val max = points.max() + 0.5f
                    val range = max - min

                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = index * spacing
                        val y = height - ((value - min) / range * height)
                        if (index == 0) path.moveTo(x, y) else {
                            // Cubic Bezier for smooth curves
                            val prevX = (index - 1) * spacing
                            val prevY = height - ((points[index-1] - min) / range * height)
                            val controlX1 = prevX + (x - prevX) / 2
                            val controlX2 = prevX + (x - prevX) / 2
                            path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        }
                    }

                    // Draw Gradient Fill
                    val fillPath = Path()
                    fillPath.addPath(path)
                    fillPath.lineTo(width, height)
                    fillPath.lineTo(0f, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CalorieKoGreen.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )

                    // Draw Line
                    drawPath(
                        path = path,
                        color = CalorieKoGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Dots
                    points.forEachIndexed { index, value ->
                        val x = index * spacing
                        val y = height - ((value - min) / range * height)
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                        drawCircle(CalorieKoGreen, radius = 3.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }
    }
}

// --- 3. Calorie Chart Component ---
@Composable
fun CalorieBalanceCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Calorie Balance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Mock Data: Pairs of (In, Out)
                val data = listOf(
                    Pair(2200, 1800), Pair(2400, 2000), Pair(1900, 2100),
                    Pair(2500, 1900), Pair(2100, 2100), Pair(2300, 2400), Pair(1800, 1600)
                )
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val maxVal = 2600f

                data.forEachIndexed { index, (input, output) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                            // In Bar (Orange)
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((150 * (input / maxVal)).dp)
                                    .background(CalorieKoOrange, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                            // Out Bar (Blue)
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((150 * (output / maxVal)).dp)
                                    .background(Color(0xFF3B82F6), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(days[index], fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            // Legend
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Intake", CalorieKoOrange)
                LegendItem("Burned", Color(0xFF3B82F6))
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

// --- 4. Sodium Trend Component ---
@Composable
fun SodiumTrendCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Sodium Intake", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sodiumData = listOf(2100f, 2400f, 1800f, 2200f, 1900f, 2500f, 2000f)
                    val width = size.width
                    val height = size.height
                    val spacing = width / (sodiumData.size - 1)
                    val max = 3000f // Scale max
                    val limitY = height - (2300f / max * height) // 2300mg limit line

                    // Draw Limit Line (Dashed)
                    drawLine(
                        color = Color.Red.copy(alpha = 0.5f),
                        start = Offset(0f, limitY),
                        end = Offset(width, limitY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Draw Bars
                    sodiumData.forEachIndexed { index, value ->
                        val x = index * spacing
                        val barHeight = (value / max) * height
                        val isOver = value > 2300f

                        drawLine(
                            color = if (isOver) Color.Red.copy(alpha = 0.6f) else Color(0xFF3B82F6).copy(alpha = 0.6f),
                            start = Offset(x, height),
                            end = Offset(x, height - barHeight),
                            strokeWidth = 12.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Text("Limit: 2300mg", fontSize = 10.sp, color = Color.Red, modifier = Modifier.align(Alignment.End))
        }
    }
}

// --- 5. Dietary Insights Component ---
@Composable
fun DietaryInsightsSection() {
    Column {
        Text("AI Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Sodium Alert",
                description = "Your sodium intake spikes on weekends.",
                icon = Icons.Default.Info,
                color = Color(0xFFEF4444), // Red
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Protein Goal",
                description = "You're hitting 90% of your protein target!",
                icon = Icons.Rounded.Lightbulb,
                color = Color(0xFFF59E0B), // Amber
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InsightCard(title: String, description: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 12.sp, color = Color(0xFF4B5563))
        }
    }
}