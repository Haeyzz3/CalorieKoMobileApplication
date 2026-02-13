package com.calorieko.app

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*

@Composable
fun TargetSummaryScreen(onContinue: () -> Unit) {
    // --- Mock Data ---
    // In React this came from userData.targetCalories
    val targetCalories = 2450
    val targetSodium = 2300
    val selectedGoalTitle = "Weight Control" // Mocking the goal title

    // Animation states for the numbers
    val animatedCalories by animateIntAsState(
        targetValue = targetCalories,
        animationSpec = tween(durationMillis = 1000, delayMillis = 500),
        label = "calories"
    )
    val animatedSodium by animateIntAsState(
        targetValue = targetSodium,
        animationSpec = tween(durationMillis = 1000, delayMillis = 600),
        label = "sodium"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Light gray background
    ) {
        // --- 1. Progress Bar (100%) ---
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFF1F5F9))) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f) // 100% width
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                        )
                    )
            )
        }

        // --- 2. Scrollable Content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // --- Header ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Check Icon Circle
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "STEP 3 OF 3",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your Daily Targets",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Text(
                text = "Based on your profile and goals",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // --- Goal Badge ---
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "YOUR GOAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedGoalTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                }
            }

            // --- Metric Cards ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Calories Card (Orange Gradient)
                MetricGradientCard(
                    title = "Daily Calories",
                    subtitle = "Energy target",
                    value = animatedCalories.toString(), // Uses animation
                    unit = "kcal",
                    icon = Icons.Rounded.LocalFireDepartment,
                    gradientColors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                )

                // Sodium Card (Green Gradient)
                MetricGradientCard(
                    title = "Daily Sodium",
                    subtitle = "Salt intake limit",
                    value = animatedSodium.toString(), // Uses animation
                    unit = "mg",
                    icon = Icons.Rounded.WaterDrop,
                    gradientColors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                )
            }

            // --- Pro Tip Box ---
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF), // blue-50
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)), // blue-100
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Pro Tip: ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A), // blue-900
                        fontSize = 14.sp
                    )
                    Text(
                        text = "These targets are personalized based on your age, weight, height, and health goals. You can adjust them anytime in settings.",
                        color = Color(0xFF1E3A8A),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // --- 3. Connect Button ---
        Surface(
            color = Color.White,
            shadowElevation = 16.dp, // Adds a subtle shadow at the bottom
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Connect Smart Scale",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- Helper Component: Gradient Card ---
@Composable
fun MetricGradientCard(
    title: String,
    subtitle: String,
    value: String,
    unit: String,
    icon: ImageVector,
    gradientColors: List<Color>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradientColors))
    ) {
        // Decorative Background Circles (The "white/10" in Tailwind)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 20.dp)
                .size(120.dp)
                .alpha(0.1f)
                .background(Color.White, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-10).dp)
                .size(80.dp)
                .alpha(0.1f)
                .background(Color.White, CircleShape)
        )

        // Content
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon + Title
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            // Footer: Big Number + Unit
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = unit,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}