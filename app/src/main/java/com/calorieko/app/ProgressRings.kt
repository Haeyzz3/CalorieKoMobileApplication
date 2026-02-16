package com.calorieko.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*

@Composable
fun ProgressRings(
    caloriesCurrent: Int,
    caloriesTarget: Int,
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Two Concentric Rings with Center Text ---
            Box(contentAlignment = Alignment.Center) {
                // Canvas draws 2 rings: Calories (green, outer) and Sodium (orange, inner)
                DualRingChart(
                    size = 220.dp,
                    outerStrokeWidth = 16.dp,
                    innerStrokeWidth = 14.dp,
                    caloriesCurrent = caloriesCurrent,
                    caloriesTarget = caloriesTarget,
                    sodiumCurrent = sodiumCurrent,
                    sodiumTarget = sodiumTarget
                )

                // Center Text: Calories + Sodium stacked
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Calorie value
                    Text(
                        text = "$caloriesCurrent",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32) // Dark green
                    )
                    Text(
                        text = "/ $caloriesTarget kcal",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Sodium value
                    Text(
                        text = "$sodiumCurrent",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalorieKoOrange
                    )
                    Text(
                        text = "/ $sodiumTarget mg",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 2. Legend Row (Calories + Sodium) ---
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = CalorieKoGreen, label = "Calories")
                Spacer(modifier = Modifier.width(24.dp))
                LegendDot(color = CalorieKoOrange, label = "Sodium")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. Horizontal Macro Progress Bars ---
            MacroProgressBar(
                label = "Protein",
                current = proteinCurrent,
                target = proteinTarget,
                color = Color(0xFF3B82F6), // Blue
                unit = "g"
            )
            Spacer(modifier = Modifier.height(14.dp))
            MacroProgressBar(
                label = "Carbs",
                current = carbsCurrent,
                target = carbsTarget,
                color = CalorieKoOrange,
                unit = "g"
            )
            Spacer(modifier = Modifier.height(14.dp))
            MacroProgressBar(
                label = "Fats",
                current = fatsCurrent,
                target = fatsTarget,
                color = Color(0xFF8B5CF6), // Purple
                unit = "g"
            )
        }
    }
}

// --- Dual Ring Chart (2 concentric rings) ---

@Composable
fun DualRingChart(
    size: Dp,
    outerStrokeWidth: Dp,
    innerStrokeWidth: Dp,
    caloriesCurrent: Int,
    caloriesTarget: Int,
    sodiumCurrent: Int,
    sodiumTarget: Int
) {
    val calorieProgress = (caloriesCurrent.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f)
    val sodiumProgress = (sodiumCurrent.toFloat() / sodiumTarget.toFloat()).coerceIn(0f, 1f)

    val animatedCalories by animateFloatAsState(
        targetValue = calorieProgress,
        animationSpec = tween(1000),
        label = "calories"
    )
    val animatedSodium by animateFloatAsState(
        targetValue = sodiumProgress,
        animationSpec = tween(1000),
        label = "sodium"
    )

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val outerStroke = outerStrokeWidth.toPx()
        val innerStroke = innerStrokeWidth.toPx()
        val gap = 10.dp.toPx() // Space between the two rings

        // --- Outer Ring: Calories (Green) ---
        val outerRadius = (size.toPx() / 2) - (outerStroke / 2)

        // Background track
        drawCircle(
            color = Color(0xFFE8F5E9), // Light green track
            radius = outerRadius,
            center = center,
            style = Stroke(width = outerStroke)
        )
        // Progress arc
        val outerSweep = 360f * animatedCalories
        drawArc(
            color = Color(0xFF4CAF50), // CalorieKoGreen
            startAngle = -90f,
            sweepAngle = outerSweep,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = outerStroke, cap = StrokeCap.Round)
        )

        // --- Inner Ring: Sodium (Orange) ---
        val innerRadius = outerRadius - outerStroke / 2 - gap - innerStroke / 2

        // Background track
        drawCircle(
            color = Color(0xFFFFF3E0), // Light orange track
            radius = innerRadius,
            center = center,
            style = Stroke(width = innerStroke)
        )
        // Progress arc
        val innerSweep = 360f * animatedSodium
        drawArc(
            color = Color(0xFFFF9800), // CalorieKoOrange
            startAngle = -90f,
            sweepAngle = innerSweep,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = innerStroke, cap = StrokeCap.Round)
        )
    }
}

// --- Legend Dot ---

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4B5563)
        )
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
            modifier = Modifier.width(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(10.dp),
            color = color,
            trackColor = Color(0xFFF1F5F9),
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Value text: "65g / 120g"
        Text(
            text = "${current}$unit / ${target}$unit",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.width(80.dp)
        )
    }
}