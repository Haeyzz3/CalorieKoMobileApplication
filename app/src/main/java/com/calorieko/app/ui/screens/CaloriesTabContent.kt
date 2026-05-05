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
import androidx.core.graphics.toColorInt
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.ui.theme.*
import java.util.Locale

@Composable
fun CaloriesTabContent(
    viewMode: String,
    daySummary: DailyNutritionSummaryEntity?,
    goalCalories: Int,
    weekDaySummaries: List<DailyNutritionSummaryEntity?>,
    weekDayLabels: List<String>,
    dayBurnedCalories: Int = 0,
    weekDayBurnedCalories: List<Int> = emptyList()
) {
    if (viewMode == "day") {
        CaloriesDayView(
            daySummary = daySummary,
            goalCalories = goalCalories,
            burnedCalories = dayBurnedCalories
        )
    } else {
        CaloriesWeekView(
            weekDaySummaries = weekDaySummaries,
            weekDayLabels = weekDayLabels,
            goalCalories = goalCalories,
            weekDayBurnedCalories = weekDayBurnedCalories
        )
    }
}

// =====================================================
// DAY VIEW
// =====================================================

@Composable
private fun CaloriesDayView(
    daySummary: DailyNutritionSummaryEntity?,
    goalCalories: Int,
    burnedCalories: Int
) {
    val breakfastCal = daySummary?.breakfastCalories?.toInt() ?: 0
    val lunchCal = daySummary?.lunchCalories?.toInt() ?: 0
    val dinnerCal = daySummary?.dinnerCalories?.toInt() ?: 0
    val snacksCal = daySummary?.snacksCalories?.toInt() ?: 0
    val totalCalories = breakfastCal + lunchCal + dinnerCal + snacksCal
    val netCalories = totalCalories - burnedCalories

    val totalForPercent = if (totalCalories > 0) totalCalories.toFloat() else 1f
    val breakfastPct = if (totalCalories > 0) (breakfastCal / totalForPercent * 100).toInt() else 0
    val lunchPct = if (totalCalories > 0) (lunchCal / totalForPercent * 100).toInt() else 0
    val dinnerPct = if (totalCalories > 0) (dinnerCal / totalForPercent * 100).toInt() else 0
    val snacksPct = if (totalCalories > 0) (snacksCal / totalForPercent * 100).toInt() else 0

    val breakfastColor = CalorieKoGreen
    val lunchColor = CalorieKoDarkGreen
    val dinnerColor = CalorieKoOrange
    val snacksColor = CalorieKoLightOrange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // --- Pie Chart Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                color = DividerGray,
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CalorieSummaryRow(label = "Estimated Total Calories", value = "~$totalCalories", valueColor = DarkText)
                HorizontalDivider(color = DividerGray, thickness = 1.dp)
                CalorieSummaryRow(label = "Estimated Net Calories", value = "~$netCalories", valueColor = DarkText)
                HorizontalDivider(color = DividerGray, thickness = 1.dp)
                CalorieSummaryRow(label = "Goal", value = goalCalories.toFormattedString(), valueColor = CalorieKoGreen)
            }
        }
    }
}

// =====================================================
// WEEK VIEW
// =====================================================

@Composable
private fun CaloriesWeekView(
    weekDaySummaries: List<DailyNutritionSummaryEntity?>,
    weekDayLabels: List<String>,
    goalCalories: Int,
    weekDayBurnedCalories: List<Int>
) {
    val selectedSubTab = remember { mutableIntStateOf(0) } // 0 = Total, 1 = Net
    val isTotal = selectedSubTab.intValue == 0

    val weeklyGoal = goalCalories * 7

    val dailyTotalCals = (0 until 7).map { i ->
        weekDaySummaries.getOrNull(i)?.totalCalories?.toInt() ?: 0
    }
    val dailyBurnedCals = (0 until 7).map { i ->
        weekDayBurnedCalories.getOrElse(i) { 0 }
    }
    val dailyNetCals = dailyTotalCals.mapIndexed { index, total ->
        total - dailyBurnedCals[index]
    }
    val dailyNetChartCals = dailyNetCals.map { it.coerceAtLeast(0) }

    val avgTotal = if (dailyTotalCals.sum() > 0) dailyTotalCals.average().toInt() else 0
    val avgNet = if (dailyTotalCals.sum() > 0 || dailyBurnedCals.sum() > 0) dailyNetCals.average().toInt() else 0
    val caloriesUnderGoal = weeklyGoal - dailyTotalCals.sum()
    val netUnderGoal = weeklyGoal - dailyNetCals.sum()
    val activityBurned = dailyBurnedCals.sum()

    // Meal breakdown aggregates for the week (Total tab legend)
    val weekBreakfast = weekDaySummaries.sumOf { (it?.breakfastCalories ?: 0f).toDouble() }.toInt()
    val weekLunch = weekDaySummaries.sumOf { (it?.lunchCalories ?: 0f).toDouble() }.toInt()
    val weekDinner = weekDaySummaries.sumOf { (it?.dinnerCalories ?: 0f).toDouble() }.toInt()
    val weekSnacks = weekDaySummaries.sumOf { (it?.snacksCalories ?: 0f).toDouble() }.toInt()
    val weekTotal = weekBreakfast + weekLunch + weekDinner + weekSnacks
    val weekForPct = if (weekTotal > 0) weekTotal.toFloat() else 1f

    val breakfastColor = CalorieKoGreen
    val lunchColor = CalorieKoDarkGreen
    val dinnerColor = CalorieKoOrange
    val snacksColor = CalorieKoLightOrange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // --- Chart Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        .border(1.dp, DividerGray, RoundedCornerShape(6.dp))
                ) {
                    // Total button
                    Box(
                        modifier = Modifier
                            .clickable { selectedSubTab.intValue = 0 }
                            .background(
                                if (isTotal) CalorieKoGreen else Color.Transparent,
                                RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTotal) Color.White else SubtleText
                        )
                    }
                    // Net button
                    Box(
                        modifier = Modifier
                            .clickable { selectedSubTab.intValue = 1 }
                            .background(
                                if (!isTotal) CalorieKoGreen else Color.Transparent,
                                RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Net",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isTotal) Color.White else SubtleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Bar Chart (Stacked by Meal Type) ---
                // Per-day meal breakdown for stacked bars
                val dailyBreakfast = weekDaySummaries.map { (it?.breakfastCalories ?: 0f).toInt() }
                val dailyLunch = weekDaySummaries.map { (it?.lunchCalories ?: 0f).toInt() }
                val dailyDinner = weekDaySummaries.map { (it?.dinnerCalories ?: 0f).toInt() }
                val dailySnacks = weekDaySummaries.map { (it?.snacksCalories ?: 0f).toInt() }

                val maxChartValue = (
                    dailyTotalCals +
                        dailyNetChartCals +
                        listOf(goalCalories, avgTotal, avgNet.coerceAtLeast(0), 2400)
                    ).maxOrNull() ?: 2400
                val maxYValue = roundedChartMax(maxChartValue)
                val yAxisLabels = (0..4).map { tick -> (maxYValue * tick) / 4 }
                val maxY = maxYValue.toFloat()

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
                        // 7 days + 1 avg column
                        val barCount = weekDayLabels.size

                        // Draw Y-axis labels and horizontal grid lines
                        val textPaint = android.graphics.Paint().apply {
                            color = "#9E9E9E".toColorInt()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }

                        yAxisLabels.forEach { yVal ->
                            val yPos = 20f + chartHeight - (yVal / maxY) * chartHeight
                            drawLine(
                                color = DividerGray,
                                start = Offset(leftPadding, yPos),
                                end = Offset(size.width - 20f, yPos),
                                strokeWidth = 1f
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                yVal.toFormattedString(),
                                leftPadding - 12f,
                                yPos + 10f,
                                textPaint
                            )
                        }

                        // Draw stacked bars
                        val barWidth = chartWidth / barCount * 0.5f
                        val barSpacing = chartWidth / barCount

                        // Meal segments per day: Breakfast (bottom) → Lunch → Dinner → Snacks (top)
                        val mealColors = listOf(breakfastColor, lunchColor, dinnerColor, snacksColor)

                        for (dayIndex in 0 until barCount) {
                            val segments = if (dayIndex < 7) {
                                if (isTotal) {
                                    listOf(
                                        dailyBreakfast.getOrElse(dayIndex) { 0 },
                                        dailyLunch.getOrElse(dayIndex) { 0 },
                                        dailyDinner.getOrElse(dayIndex) { 0 },
                                        dailySnacks.getOrElse(dayIndex) { 0 }
                                    )
                                } else {
                                    listOf(dailyNetChartCals.getOrElse(dayIndex) { 0 }, 0, 0, 0)
                                }
                            } else {
                                // Avg column — show as single green bar
                                val avg = if (isTotal) avgTotal else avgNet.coerceAtLeast(0)
                                listOf(avg, 0, 0, 0)
                            }

                            val x = leftPadding + dayIndex * barSpacing + barSpacing / 2 - barWidth / 2
                            var yBottom = 20f + chartHeight  // baseline

                            segments.forEachIndexed { segIdx, segVal ->
                                if (segVal > 0) {
                                    val segHeight = (segVal / maxY) * chartHeight
                                    yBottom -= segHeight
                                    drawRect(
                                        color = mealColors[segIdx],
                                        topLeft = Offset(x, yBottom),
                                        size = Size(barWidth, segHeight)
                                    )
                                }
                            }
                        }

                        // Draw goal line for Net tab
                        if (!isTotal) {
                            val goalY = (
                                20f + chartHeight - (goalCalories / maxY) * chartHeight
                                ).coerceIn(20f, 20f + chartHeight)
                            drawLine(
                                color = DarkText,
                                start = Offset(leftPadding, goalY),
                                end = Offset(size.width - 20f, goalY),
                                strokeWidth = 3f
                            )
                        }

                        // Draw X-axis labels
                        val xTextPaint = android.graphics.Paint().apply {
                            color = "#9E9E9E".toColorInt()
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        weekDayLabels.forEachIndexed { index, label ->
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
                        MealLegendItem(
                            color = breakfastColor, label = "Breakfast",
                            percent = if (weekTotal > 0) (weekBreakfast / weekForPct * 100).toInt() else 0,
                            calories = weekBreakfast
                        )
                        MealLegendItem(
                            color = lunchColor, label = "Lunch",
                            percent = if (weekTotal > 0) (weekLunch / weekForPct * 100).toInt() else 0,
                            calories = weekLunch
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MealLegendItem(
                            color = dinnerColor, label = "Dinner",
                            percent = if (weekTotal > 0) (weekDinner / weekForPct * 100).toInt() else 0,
                            calories = weekDinner
                        )
                        MealLegendItem(
                            color = snacksColor, label = "Snacks",
                            percent = if (weekTotal > 0) (weekSnacks / weekForPct * 100).toInt() else 0,
                            calories = weekSnacks
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Summary Rows ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isTotal) {
                    CalorieSummaryRow(
                        label = "Calories Under Weekly Goal",
                        value = "~${caloriesUnderGoal.toFormattedString()}",
                        valueColor = DarkText
                    )
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    CalorieSummaryRow(
                        label = "Daily Average",
                        value = "~$avgTotal",
                        valueColor = DarkText
                    )
                } else {
                    CalorieSummaryRow(
                        label = "Net Calories Under Weekly Goal",
                        value = "~${netUnderGoal.toFormattedString()}",
                        valueColor = DarkText
                    )
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    CalorieSummaryRow(
                        label = "Net Average",
                        value = "~$avgNet",
                        valueColor = DarkText
                    )
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    CalorieSummaryRow(
                        label = "Activity Burned",
                        value = "~${activityBurned.toFormattedString()}",
                        valueColor = CalorieKoOrange
                    )
                }
                HorizontalDivider(color = DividerGray, thickness = 1.dp)
                CalorieSummaryRow(
                    label = "Goal",
                    value = goalCalories.toFormattedString(),
                    valueColor = CalorieKoGreen
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
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText
            )
            Text(
                text = "$percent% (~$calories cal)",
                fontSize = 12.sp,
                color = SubtleText
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
            color = SubtleText
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

private fun roundedChartMax(value: Int, step: Int = 600): Int {
    val safeValue = value.coerceAtLeast(step)
    return ((safeValue + step - 1) / step) * step
}
