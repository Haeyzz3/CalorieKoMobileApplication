package com.calorieko.app.ui.components

import com.calorieko.app.ui.components.BottomNavigation
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing



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

@Composable
fun ProgressRings(
    caloriesCurrent: Int,
    caloriesTarget: Int,
    caloriesBurned: Int,
    caloriesBurnedTarget: Int,
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
            // --- 1. Three Concentric Rings with Center Text ---
            Box(contentAlignment = Alignment.Center) {
                TripleRingChart(
                    size = 290.dp, // Increased overall size to expand inner diameter
                    outerStrokeWidth = 14.dp,
                    middleStrokeWidth = 12.dp,
                    innerStrokeWidth = 10.dp,
                    caloriesCurrent = caloriesCurrent,
                    caloriesTarget = caloriesTarget,
                    burnedCurrent = caloriesBurned,
                    burnedTarget = caloriesBurnedTarget,
                    sodiumCurrent = sodiumCurrent,
                    sodiumTarget = sodiumTarget
                )

                // Center Text: Structured to fit within the expanded inner radius
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Primary Metric: Consumed (Green)
                    Text(
                        text = "$caloriesCurrent",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "/ $caloriesTarget kcal in",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Metrics Side-by-Side (Red & Orange)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$caloriesBurned",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Text(
                                text = "/ $caloriesBurnedTarget out",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$sodiumCurrent",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Text(
                                text = "/ $sodiumTarget mg Na",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. Legend Row --- //
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0xFF4CAF50), label = "Eaten")
                Spacer(modifier = Modifier.width(16.dp))
                LegendDot(color = Color(0xFFEF5350), label = "Burned")
                Spacer(modifier = Modifier.width(16.dp))
                LegendDot(color = Color(0xFFFF9800), label = "Sodium")
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
                color = Color(0xFFFF9800), // Orange
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

// --- Triple Ring Chart ---

@Composable
fun TripleRingChart(
    size: Dp,
    outerStrokeWidth: Dp,
    middleStrokeWidth: Dp,
    innerStrokeWidth: Dp,
    caloriesCurrent: Int,
    caloriesTarget: Int,
    burnedCurrent: Int,
    burnedTarget: Int,
    sodiumCurrent: Int,
    sodiumTarget: Int
) {
    val calorieProgress = (caloriesCurrent.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f)
    val burnedProgress = (burnedCurrent.toFloat() / burnedTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val sodiumProgress = (sodiumCurrent.toFloat() / sodiumTarget.toFloat()).coerceIn(0f, 1f)

    val animatedCalories by animateFloatAsState(targetValue = calorieProgress, animationSpec = tween(1000), label = "calories")
    val animatedBurned by animateFloatAsState(targetValue = burnedProgress, animationSpec = tween(1000), label = "burned")
    val animatedSodium by animateFloatAsState(targetValue = sodiumProgress, animationSpec = tween(1000), label = "sodium")

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val outerStroke = outerStrokeWidth.toPx()
        val middleStroke = middleStrokeWidth.toPx()
        val innerStroke = innerStrokeWidth.toPx()
        val gap = 6.dp.toPx()

        // --- Outer Ring: Calories Consumed (Green) ---
        val outerRadius = (size.toPx() / 2) - (outerStroke / 2)
        drawCircle(
            color = Color(0xFFE8F5E9),
            radius = outerRadius,
            center = center,
            style = Stroke(width = outerStroke)
        )
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = 360f * animatedCalories,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = outerStroke, cap = StrokeCap.Round)
        )

        // --- Middle Ring: Calories Burned (Red) ---
        val middleRadius = outerRadius - outerStroke / 2 - gap - middleStroke / 2
        drawCircle(
            color = Color(0xFFFFEBEE),
            radius = middleRadius,
            center = center,
            style = Stroke(width = middleStroke)
        )
        drawArc(
            color = Color(0xFFEF5350),
            startAngle = -90f,
            sweepAngle = 360f * animatedBurned,
            useCenter = false,
            topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
            size = Size(middleRadius * 2, middleRadius * 2),
            style = Stroke(width = middleStroke, cap = StrokeCap.Round)
        )

        // --- Inner Ring: Sodium (Orange) ---
        val innerRadius = middleRadius - middleStroke / 2 - gap - innerStroke / 2
        drawCircle(
            color = Color(0xFFFFF3E0),
            radius = innerRadius,
            center = center,
            style = Stroke(width = innerStroke)
        )
        drawArc(
            color = Color(0xFFFF9800),
            startAngle = -90f,
            sweepAngle = 360f * animatedSodium,
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
    val progress = (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = label
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Text(
            text = "${current}$unit / ${target}$unit",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.width(80.dp)
        )
    }
}