package com.calorieko.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun CaloriesTabContent(viewMode: String) {
    if (viewMode == "day") {
        CaloriesDayView()
    } else {
        CaloriesWeekView()
    }
}

// =====================================================
// DAY VIEW (existing)
// =====================================================

@Composable
private fun CaloriesDayView() {
    val breakfastCal = 0
    val lunchCal = 0
    val dinnerCal = 0
    val snacksCal = 0
    val totalCalories = breakfastCal + lunchCal + dinnerCal + snacksCal
    val goalCalories = 2170
    val netCalories = totalCalories

    val totalForPercent = if (totalCalories > 0) totalCalories.toFloat() else 1f
    val breakfastPct = if (totalCalories > 0) (breakfastCal / totalForPercent * 100).toInt() else 0
    val lunchPct = if (totalCalories > 0) (lunchCal / totalForPercent * 100).toInt() else 0
    val dinnerPct = if (totalCalories > 0) (dinnerCal / totalForPercent * 100).toInt() else 0
    val snacksPct = if (totalCalories > 0) (snacksCal / totalForPercent * 100).toInt() else 0

    val breakfastColor = Color(0xFF1565C0)
    val lunchColor = Color(0xFF0D47A1)
    val dinnerColor = Color(0xFF42A5F5)
    val snacksColor = Color(0xFF90CAF9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // --- Pie Chart Card ---
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Donut Chart
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 40f
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2,
                            (size.height - diameter) / 2
                        )
                        val arcSize = Size(diameter, diameter)

                        if (totalCalories == 0) {
                            drawArc(
                                color = Color(0xFFE0E0E0),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        } else {
                            var startAngle = -90f
                            val meals = listOf(
                                breakfastCal to breakfastColor,
                                lunchCal to lunchColor,
                                dinnerCal to dinnerColor,
                                snacksCal to snacksColor
                            )
                            meals.forEach { (cal, color) ->
                                if (cal > 0) {
                                    val sweep = (cal.toFloat() / totalCalories) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Meal Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MealLegendItem(color = breakfastColor, label = "Breakfast", percent = breakfastPct, calories = breakfastCal)
                    MealLegendItem(color = lunchColor, label = "Lunch", percent = lunchPct, calories = lunchCal)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MealLegendItem(color = dinnerColor, label = "Dinner", percent = dinnerPct, calories = dinnerCal)
                    MealLegendItem(color = snacksColor, label = "Snacks", percent = snacksPct, calories = snacksCal)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calorie Summary Rows
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CalorieSummaryRow(label = "Total Calories", value = totalCalories.toString(), valueColor = Color(0xFF212121))
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                CalorieSummaryRow(label = "Net Calories", value = netCalories.toString(), valueColor = Color(0xFF212121))
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                CalorieSummaryRow(label = "Goal", value = goalCalories.toFormattedString(), valueColor = Color(0xFF1565C0))
            }
        }
    }
}

// =====================================================
// WEEK VIEW (new)
// =====================================================

@Composable
private fun CaloriesWeekView() {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Total, 1 = Net
    val isTotal = selectedSubTab == 0

    // Mock data
    val goalCalories = 2170
    val weeklyGoal = goalCalories * 7 // 15,190
    val dayLabels = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Avg")
    val dailyTotalCals = listOf(0, 0, 0, 0, 0, 0, 0) // Mock: all zeros
    val dailyNetCals = listOf(0, 0, 0, 0, 0, 0, 0)
    val avgTotal = if (dailyTotalCals.sum() > 0) dailyTotalCals.average().toInt() else 0
    val avgNet = if (dailyNetCals.sum() > 0) dailyNetCals.average().toInt() else 0
    val caloriesUnderGoal = weeklyGoal - dailyTotalCals.sum()
    val netUnderGoal = weeklyGoal - dailyNetCals.sum()

    // Meal percentages (for Total tab legend)
    val breakfastColor = Color(0xFF1565C0)
    val lunchColor = Color(0xFF0D47A1)
    val dinnerColor = Color(0xFF42A5F5)
    val snacksColor = Color(0xFF90CAF9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // --- Chart Card ---
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Total / Net Toggle ---
                Row(
                    modifier = Modifier
                        .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
                ) {
                    // Total button
                    Box(
                        modifier = Modifier
                            .clickable { selectedSubTab = 0 }
                            .background(
                                if (isTotal) Color(0xFF1565C0) else Color.Transparent,
                                RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTotal) Color.White else Color(0xFF757575)
                        )
                    }
                    // Net button
                    Box(
                        modifier = Modifier
                            .clickable { selectedSubTab = 1 }
                            .background(
                                if (!isTotal) Color(0xFF1565C0) else Color.Transparent,
                                RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Net",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isTotal) Color.White else Color(0xFF757575)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Bar Chart ---
                val chartValues = if (isTotal) {
                    dailyTotalCals + avgTotal
                } else {
                    dailyNetCals + avgNet
                }
                val yAxisLabels = listOf(0, 600, 1200, 1800, 2400)
                val maxY = 2400f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val leftPadding = 100f
                        val bottomPadding = 50f
                        val chartWidth = size.width - leftPadding - 20f
                        val chartHeight = size.height - bottomPadding - 20f
                        val barCount = chartValues.size

                        // Draw Y-axis labels and horizontal grid lines
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#9E9E9E")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }

                        yAxisLabels.forEach { yVal ->
                            val yPos = 20f + chartHeight - (yVal / maxY) * chartHeight
                            // Grid line
                            drawLine(
                                color = Color(0xFFEEEEEE),
                                start = Offset(leftPadding, yPos),
                                end = Offset(size.width - 20f, yPos),
                                strokeWidth = 1f
                            )
                            // Y label
                            drawContext.canvas.nativeCanvas.drawText(
                                yVal.toFormattedString(),
                                leftPadding - 12f,
                                yPos + 10f,
                                textPaint
                            )
                        }

                        // Draw bars
                        val barWidth = chartWidth / barCount * 0.5f
                        val barSpacing = chartWidth / barCount

                        chartValues.forEachIndexed { index, value ->
                            val barHeight = if (maxY > 0) (value / maxY) * chartHeight else 0f
                            val x = leftPadding + index * barSpacing + barSpacing / 2 - barWidth / 2
                            val yTop = 20f + chartHeight - barHeight

                            if (value > 0) {
                                drawRect(
                                    color = Color(0xFF1565C0),
                                    topLeft = Offset(x, yTop),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }

                        // Draw goal line for Net tab
                        if (!isTotal) {
                            val goalY = 20f + chartHeight - (goalCalories / maxY) * chartHeight
                            drawLine(
                                color = Color(0xFF212121),
                                start = Offset(leftPadding, goalY),
                                end = Offset(size.width - 20f, goalY),
                                strokeWidth = 3f
                            )
                        }

                        // Draw X-axis labels
                        val xTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#9E9E9E")
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        dayLabels.forEachIndexed { index, label ->
                            val x = leftPadding + index * barSpacing + barSpacing / 2
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                x,
                                size.height - 5f,
                                xTextPaint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Meal Legend (Total tab only) ---
                if (isTotal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MealLegendItem(color = breakfastColor, label = "Breakfast", percent = 0, calories = 0)
                        MealLegendItem(color = lunchColor, label = "Lunch", percent = 0, calories = 0)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MealLegendItem(color = dinnerColor, label = "Dinner", percent = 0, calories = 0)
                        MealLegendItem(color = snacksColor, label = "Snacks", percent = 0, calories = 0)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Summary Rows ---
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isTotal) {
                    CalorieSummaryRow(
                        label = "Calories Under Weekly Goal",
                        value = caloriesUnderGoal.toFormattedString(),
                        valueColor = Color(0xFF212121)
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    CalorieSummaryRow(
                        label = "Daily Average",
                        value = avgTotal.toString(),
                        valueColor = Color(0xFF212121)
                    )
                } else {
                    CalorieSummaryRow(
                        label = "Net Calories Under Weekly Goal",
                        value = netUnderGoal.toFormattedString(),
                        valueColor = Color(0xFF212121)
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    CalorieSummaryRow(
                        label = "Net Average",
                        value = avgNet.toString(),
                        valueColor = Color(0xFF212121)
                    )
                }
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                CalorieSummaryRow(
                    label = "Goal",
                    value = goalCalories.toFormattedString(),
                    valueColor = Color(0xFF1565C0)
                )
            }
        }
    }
}

// =====================================================
// SHARED HELPERS
// =====================================================

@Composable
private fun MealLegendItem(
    color: Color,
    label: String,
    percent: Int,
    calories: Int
) {
    Row(
        modifier = Modifier.width(140.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(14.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
            Text(
                text = "$percent% ($calories cal)",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun CalorieSummaryRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF424242)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun Int.toFormattedString(): String {
    return String.format(Locale.US, "%,d", this)
}
