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

@Composable
fun MacrosTabContent(viewMode: String) {
    if (viewMode == "day") {
        MacrosDayView()
    } else {
        MacrosWeekView()
    }
}

// =====================================================
// DAY VIEW
// =====================================================

@Composable
private fun MacrosDayView() {
    // Mock data
    val carbsGrams = 0
    val fatGrams = 0
    val proteinGrams = 0
    val totalGrams = carbsGrams + fatGrams + proteinGrams

    val carbsGoalPct = 50
    val fatGoalPct = 30
    val proteinGoalPct = 20

    val carbsTotalPct = 0
    val fatTotalPct = 0
    val proteinTotalPct = 0

    val carbsColor = Color(0xFF00897B)   // Teal
    val fatColor = Color(0xFF7B1FA2)     // Purple
    val proteinColor = Color(0xFFFFA726) // Orange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                        color = Color(0xFF757575),
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = "Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575),
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
                    goalPct = carbsGoalPct,
                    valueLabel = "Total"
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = fatColor,
                    name = "Fat",
                    grams = fatGrams,
                    valuePct = fatTotalPct,
                    goalPct = fatGoalPct,
                    valueLabel = "Total"
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = proteinColor,
                    name = "Protein",
                    grams = proteinGrams,
                    valuePct = proteinTotalPct,
                    goalPct = proteinGoalPct,
                    valueLabel = "Total"
                )
            }
        }
    }
}

// =====================================================
// WEEK VIEW
// =====================================================

@Composable
private fun MacrosWeekView() {
    // Mock data
    val carbsGrams = 0
    val fatGrams = 0
    val proteinGrams = 0

    val carbsGoalPct = 50
    val fatGoalPct = 30
    val proteinGoalPct = 20

    val carbsAvgPct = 0
    val fatAvgPct = 0
    val proteinAvgPct = 0

    val dayLabels = listOf("Fri", "Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Avg")
    val dailyGrams = listOf(0, 0, 0, 0, 0, 0, 0)
    val avgGrams = if (dailyGrams.sum() > 0) dailyGrams.average().toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                            color = android.graphics.Color.parseColor("#9E9E9E")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }

                        yAxisLabels.forEach { yVal ->
                            val yPos = 20f + chartHeight - (yVal / maxY) * chartHeight
                            drawLine(
                                color = Color(0xFFEEEEEE),
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

                        // X-axis labels
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
                        color = Color(0xFF757575),
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = "Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575),
                        modifier = Modifier.width(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Macro Rows ---
                MacroLegendRow(
                    color = Color(0xFF00897B),
                    name = "Carbohydrates",
                    grams = carbsGrams,
                    valuePct = carbsAvgPct,
                    goalPct = carbsGoalPct,
                    valueLabel = "Avg"
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = Color(0xFF7B1FA2),
                    name = "Fat",
                    grams = fatGrams,
                    valuePct = fatAvgPct,
                    goalPct = fatGoalPct,
                    valueLabel = "Avg"
                )
                Spacer(modifier = Modifier.height(12.dp))
                MacroLegendRow(
                    color = Color(0xFFFFA726),
                    name = "Protein",
                    grams = proteinGrams,
                    valuePct = proteinAvgPct,
                    goalPct = proteinGoalPct,
                    valueLabel = "Avg"
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
    goalPct: Int,
    valueLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color square
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Name + grams
        Text(
            text = "$name (${grams}g)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        // Value %
        Text(
            text = "$valuePct%",
            fontSize = 14.sp,
            color = Color(0xFF616161),
            modifier = Modifier.width(70.dp)
        )
        // Goal %
        Text(
            text = "$goalPct%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1565C0),
            modifier = Modifier.width(60.dp)
        )
    }
}

private fun formatNumber(value: Int): String {
    return String.format(java.util.Locale.US, "%,d", value)
}
