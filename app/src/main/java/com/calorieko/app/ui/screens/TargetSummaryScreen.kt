package com.calorieko.app.ui.screens


import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.EggAlt
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

// 1. Update signature to accept REAL data
// 1. Update the signature
@Composable
fun TargetSummaryScreen(
    targetCalories: Int,
    targetSodium: Int,
    targetProtein: Int,
    targetCarbs: Int,
    targetFats: Int,
    goalTitle: String,
    onContinue: () -> Unit
) {

    // 2. Add macro animations
    val animatedProtein by animateIntAsState(targetValue = targetProtein, animationSpec = tween(1000, 700), label = "protein")
    val animatedCarbs by animateIntAsState(targetValue = targetCarbs, animationSpec = tween(1000, 800), label = "carbs")
    val animatedFats by animateIntAsState(targetValue = targetFats, animationSpec = tween(1000, 900), label = "fats")


    // Animation states using the PASSED values
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
            .background(Color(0xFFF8F9FA))
    ) {
        // --- 1. Progress Bar (100%) ---
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFF1F5F9))) {
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(1f)
                    .background(Brush.horizontalGradient(listOf(CalorieKoGreen, CalorieKoLightGreen)))
            )
        }

        // --- 2. Scrollable Content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(CalorieKoGreen, CalorieKoLightGreen))
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("STEP 3 OF 3", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Your Daily Targets", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Text(
                text = "Based on your profile and goals",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Goal Badge (Uses REAL Goal Title)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("YOUR GOAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = goalTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                }
            }

            // Metric Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricGradientCard(
                    title = "Daily Calories",
                    subtitle = "Energy target",
                    value = animatedCalories.toString(),
                    unit = "kcal",
                    icon = Icons.Rounded.LocalFireDepartment,
                    gradientColors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                )

                // Macros Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallMacroCard(
                        title = "Protein", value = animatedProtein.toString(), modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.SetMeal, color = Color(0xFFEF4444) // Red
                    )
                    SmallMacroCard(
                        title = "Carbs", value = animatedCarbs.toString(), modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.BreakfastDining, color = Color(0xFFF59E0B) // Yellow/Amber
                    )
                    SmallMacroCard(
                        title = "Fats", value = animatedFats.toString(), modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.EggAlt, color = Color(0xFF3B82F6) // Blue
                    )
                }

                MetricGradientCard(
                    title = "Daily Sodium",
                    subtitle = "Salt intake limit",
                    value = animatedSodium.toString(),
                    unit = "mg",
                    icon = Icons.Rounded.WaterDrop,
                    gradientColors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                )
            }

            // Pro Tip Box
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Pro Tip: ", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), fontSize = 14.sp)
                    Text(
                        text = "These targets are personalized based on your age, weight, height, and health goals.",
                        color = Color(0xFF1E3A8A),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // --- 3. Connect Button ---
        Surface(color = Color.White, shadowElevation = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.horizontalGradient(listOf(CalorieKoOrange, CalorieKoLightOrange)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Connect Smart Scale", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowRight, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

// Helper Gradient Card (Unchanged)
@Composable
fun MetricGradientCard(title: String, subtitle: String, value: String, unit: String, icon: ImageVector, gradientColors: List<Color>) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(gradientColors))
    ) {
        // Decorative circles
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(20.dp, 20.dp).size(100.dp).alpha(0.1f).background(Color.White, CircleShape))

        Row(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White.copy(0.9f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(subtitle, color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(unit, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.8f))
            }
        }
    }
}
@Composable
fun SmallMacroCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = 0.8f))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Text("g", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
            }
        }
    }
}