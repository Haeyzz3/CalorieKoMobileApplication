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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.ui.theme.*

@Composable
fun MacrosTabContent(
    viewMode: String,
    daySummary: DailyNutritionSummaryEntity?,
    targetProtein: Int,
    targetCarbs: Int,
    targetFats: Int,
    weekDaySummaries: List<DailyNutritionSummaryEntity?>,
    weekDayLabels: List<String>
) {
    if (viewMode == "day") {
        MacrosDayView(
            daySummary = daySummary,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFats = targetFats
        )
    } else {
        MacrosWeekView(
            weekDaySummaries = weekDaySummaries,
            weekDayLabels = weekDayLabels,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFats = targetFats
        )
    }
}

// =====================================================
// DAY VIEW
// =====================================================

@Composable
private fun MacrosDayView(
    daySummary: DailyNutritionSummaryEntity?,
    targetProtein: Int,
    targetCarbs: Int,
    targetFats: Int
) {
    val carbsGrams = daySummary?.totalCarbs?.toInt() ?: 0
    val fatGrams = daySummary?.totalFat?.toInt() ?: 0
    val proteinGrams = daySummary?.totalProtein?.toInt() ?: 0
    val totalGrams = carbsGrams + fatGrams + proteinGrams

    // Goal percentages based on targets
    val totalTargetGrams = (targetCarbs + targetFats + targetProtein).coerceAtLeast(1)
    val carbsGoalPct = (targetCarbs * 100) / totalTargetGrams
    val fatGoalPct = (targetFats * 100) / totalTargetGrams
    val proteinGoalPct = (targetProtein * 100) / totalTargetGrams

    // Actual percentages
    val totalForPct = if (totalGrams > 0) totalGrams else 1
    val carbsTotalPct = if (totalGrams > 0) (carbsGrams * 100) / totalForPct else 0
    val fatTotalPct = if (totalGrams > 0) (fatGrams * 100) / totalForPct else 0
    val proteinTotalPct = if (totalGrams > 0) (proteinGrams * 100) / totalForPct else 0

    val carbsColor = MacroCarbs
    val fatColor = MacroFat
    val proteinColor = MacroProtein

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                // --- Donut Chart ---
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

                        if (totalGrams == 0) {
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
                            val macros = listOf(
                                carbsGrams to carbsColor,
                                fatGrams to fatColor,
                                proteinGrams to proteinColor
                            )
                            macros.forEach { (grams, color) ->
                                if (grams > 0) {
                                    val sweep = (grams.toFloat() / totalGrams) * 360f
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

                // --- Header Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = "Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Macro Rows ---
                MacroLegendRow(
                    color = carbsColor,
                    name = "Carbohydrates",
                    grams = carbsGrams,
                    valuePct = carbsTotalPct,
                    goalPct = carbsGoalPct
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = fatColor,
                    name = "Fat",
                    grams = fatGrams,
                    valuePct = fatTotalPct,
                    goalPct = fatGoalPct
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = proteinColor,
                    name = "Protein",
                    grams = proteinGrams,
                    valuePct = proteinTotalPct,
                    goalPct = proteinGoalPct
                )
            }
        }
    }
}

// =====================================================
// WEEK VIEW
// =====================================================

@Composable
private fun MacrosWeekView(
    weekDaySummaries: List<DailyNutritionSummaryEntity?>,
    weekDayLabels: List<String>,
    targetProtein: Int,
    targetCarbs: Int,
    targetFats: Int
) {
    // Total grams across the week
    val carbsGrams = weekDaySummaries.sumOf { (it?.totalCarbs ?: 0f).toDouble() }.toInt()
    val fatGrams = weekDaySummaries.sumOf { (it?.totalFat ?: 0f).toDouble() }.toInt()
    val proteinGrams = weekDaySummaries.sumOf { (it?.totalProtein ?: 0f).toDouble() }.toInt()

    val totalTargetGrams = (targetCarbs + targetFats + targetProtein).coerceAtLeast(1)
    val carbsGoalPct = (targetCarbs * 100) / totalTargetGrams
    val fatGoalPct = (targetFats * 100) / totalTargetGrams
    val proteinGoalPct = (targetProtein * 100) / totalTargetGrams

    // Average percentages (across the week)
    val daysWithData = weekDaySummaries.count { it != null }.coerceAtLeast(1)
    val avgCarbs = carbsGrams / daysWithData
    val avgFat = fatGrams / daysWithData
    val avgProtein = proteinGrams / daysWithData
    val avgTotal = (avgCarbs + avgFat + avgProtein).coerceAtLeast(1)
    val carbsAvgPct = if (avgTotal > 0) (avgCarbs * 100) / avgTotal else 0
    val fatAvgPct = if (avgTotal > 0) (avgFat * 100) / avgTotal else 0
    val proteinAvgPct = if (avgTotal > 0) (avgProtein * 100) / avgTotal else 0

    // Daily total macro grams for the bar chart
    val dailyGrams = weekDaySummaries.map { s ->
        ((s?.totalCarbs ?: 0f) + (s?.totalFat ?: 0f) + (s?.totalProtein ?: 0f)).toInt()
    }
    val avgGrams = if (dailyGrams.sum() > 0) dailyGrams.average().toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                // --- Bar Chart ---
                val chartValues = dailyGrams + avgGrams
                val yAxisLabels = listOf(0, 560, 1120)
                val maxY = 1120f

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

                        // Y-axis labels and grid lines
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
                                formatNumber(yVal),
                                leftPadding - 12f,
                                yPos + 10f,
                                textPaint
                            )
                        }

                        // Bars
                        val barWidth = chartWidth / barCount * 0.5f
                        val barSpacing = chartWidth / barCount

                        chartValues.forEachIndexed { index, value ->
                            val barHeight = (value / maxY) * chartHeight
                            val x = leftPadding + index * barSpacing + barSpacing / 2 - barWidth / 2
                            val yTop = 20f + chartHeight - barHeight

                            if (value > 0) {
                                drawRect(
                                    color = MacroCarbs,
                                    topLeft = Offset(x, yTop),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }

                        // X-axis labels
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

                Spacer(modifier = Modifier.height(20.dp))

                // --- Header Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Avg",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = "Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Macro Rows ---
                MacroLegendRow(
                    color = MacroCarbs,
                    name = "Carbohydrates",
                    grams = carbsGrams,
                    valuePct = carbsAvgPct,
                    goalPct = carbsGoalPct
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = MacroFat,
                    name = "Fat",
                    grams = fatGrams,
                    valuePct = fatAvgPct,
                    goalPct = fatGoalPct
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = MacroProtein,
                    name = "Protein",
                    grams = proteinGrams,
                    valuePct = proteinAvgPct,
                    goalPct = proteinGoalPct
                )
            }
        }
    }
}

// =====================================================
// SHARED HELPERS
// =====================================================

@Composable
private fun MacroLegendRow(
    color: Color,
    name: String,
    grams: Int,
    valuePct: Int,
    goalPct: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color square
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Name + grams
        Text(
            text = "$name (~${grams}g)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = DarkText,
            modifier = Modifier.weight(1f)
        )
        // Value %
        Text(
            text = "$valuePct%",
            fontSize = 14.sp,
            color = SubtleText,
            modifier = Modifier.width(70.dp)
        )
        // Goal %
        Text(
            text = "$goalPct%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MacroProtein,
            modifier = Modifier.width(60.dp)
        )
    }
}

private fun formatNumber(value: Int): String {
    return String.format(java.util.Locale.US, "%,d", value)
}
