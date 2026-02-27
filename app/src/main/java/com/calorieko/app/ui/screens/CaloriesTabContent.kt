package com.calorieko.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun CaloriesTabContent() {
    // Mock data for meals
    val breakfastCal = 0
    val lunchCal = 0
    val dinnerCal = 0
    val snacksCal = 0
    val totalCalories = breakfastCal + lunchCal + dinnerCal + snacksCal
    val goalCalories = 2170
    val netCalories = totalCalories // simplified mock

    // Calculate percentages
    val totalForPercent = if (totalCalories > 0) totalCalories.toFloat() else 1f
    val breakfastPct = if (totalCalories > 0) (breakfastCal / totalForPercent * 100).toInt() else 0
    val lunchPct = if (totalCalories > 0) (lunchCal / totalForPercent * 100).toInt() else 0
    val dinnerPct = if (totalCalories > 0) (dinnerCal / totalForPercent * 100).toInt() else 0
    val snacksPct = if (totalCalories > 0) (snacksCal / totalForPercent * 100).toInt() else 0

    // Meal colors
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

                // --- Meal Legend (2x2 grid) ---
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

        // --- Calorie Summary Rows ---
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
