package com.calorieko.app.ui.components


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.calorieko.app.ui.theme.MacroCarbs
import com.calorieko.app.ui.theme.MacroFat
import com.calorieko.app.ui.theme.MacroProtein
import com.calorieko.app.ui.theme.RingBurned
import com.calorieko.app.ui.theme.RingEaten
import com.calorieko.app.ui.theme.RingSodium


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

                // Center Text: Constrained to fit strictly within the innermost ring
                // The inner ring radius = size/2 - outerStroke - gap - middleStroke - gap - innerStroke/2
                // At size=290dp with these strokes, usable inner diameter ≈ 160dp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(150.dp) // Hard-cap to inner ring clear area
                        .padding(4.dp)
                ) {
                    // Primary Metric: Consumed (Green)
                    // Scale down if eaten calories are 5+ digits
                    val caloriesFontSize = if (caloriesCurrent >= 10000) 26.sp else 32.sp
                    Text(
                        text = "~$caloriesCurrent",
                        fontSize = caloriesFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        maxLines = 1
                    )
                    Text(
                        text = "/ $caloriesTarget kcal in",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary Metrics Side-by-Side (Red & Orange)
                    // Dynamically scale font size when either value reaches 4+ digits
                    val secondaryFontSize = when {
                        caloriesBurned >= 10000 || sodiumCurrent >= 10000 -> 14.sp
                        caloriesBurned >= 1000 || sodiumCurrent >= 1000 -> 17.sp
                        else -> 22.sp
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "~$caloriesBurned",
                                fontSize = secondaryFontSize,
                                fontWeight = FontWeight.Bold,
                                color = RingBurned,
                                maxLines = 1
                            )
                            Text(
                                text = "/ $caloriesBurnedTarget out",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "~$sodiumCurrent",
                                fontSize = secondaryFontSize,
                                fontWeight = FontWeight.Bold,
                                color = RingSodium,
                                maxLines = 1
                            )
                            Text(
                                text = "/ $sodiumTarget Na",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 1
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
                LegendDot(color = RingEaten, label = "Eaten")
                Spacer(modifier = Modifier.width(16.dp))
                LegendDot(color = RingBurned, label = "Burned")
                Spacer(modifier = Modifier.width(16.dp))
                LegendDot(color = RingSodium, label = "Sodium")
            }

            Spacer(modifier = Modifier.height(24.dp))

            MacroProgressBar(
                label = "Protein",
                current = proteinCurrent,
                target = proteinTarget,
                color = MacroProtein,
                unit = "g"
            )
            Spacer(modifier = Modifier.height(14.dp))
            MacroProgressBar(
                label = "Carbs",
                current = carbsCurrent,
                target = carbsTarget,
                color = MacroCarbs,
                unit = "g"
            )
            Spacer(modifier = Modifier.height(14.dp))
            MacroProgressBar(
                label = "Fats",
                current = fatsCurrent,
                target = fatsTarget,
                color = MacroFat,
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
            color = Color(0xFFE8F5E9), // Light green track
            radius = outerRadius,
            center = center,
            style = Stroke(width = outerStroke)
        )
        drawArc(
            color = RingEaten,
            startAngle = -90f,
            sweepAngle = 360f * animatedCalories,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = outerStroke, cap = StrokeCap.Round)
        )

        // --- Middle Ring: Calories Burned (Coral Red) ---
        val middleRadius = outerRadius - outerStroke / 2 - gap - middleStroke / 2
        drawCircle(
            color = Color(0xFFFFEBEE), // Light red track
            radius = middleRadius,
            center = center,
            style = Stroke(width = middleStroke)
        )
        drawArc(
            color = RingBurned,
            startAngle = -90f,
            sweepAngle = 360f * animatedBurned,
            useCenter = false,
            topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
            size = Size(middleRadius * 2, middleRadius * 2),
            style = Stroke(width = middleStroke, cap = StrokeCap.Round)
        )

        // --- Inner Ring: Sodium (Cyan/Teal) ---
        val innerRadius = middleRadius - middleStroke / 2 - gap - innerStroke / 2
        drawCircle(
            color = Color(0xFFE0F7FA), // Light cyan track
            radius = innerRadius,
            center = center,
            style = Stroke(width = innerStroke)
        )
        drawArc(
            color = RingSodium,
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
            text = "~${current}$unit / ${target}$unit",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.width(80.dp)
        )
    }
}