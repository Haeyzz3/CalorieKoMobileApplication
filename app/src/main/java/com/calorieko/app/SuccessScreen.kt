package com.calorieko.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SuccessScreen(onEnterDashboard: () -> Unit) {
    // --- Animation States ---
    var showIcon by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showListItems by remember { mutableIntStateOf(0) } // Count how many items to show
    var showButton by remember { mutableStateOf(false) }

    // --- Trigger Animations Sequence ---
    LaunchedEffect(Unit) {
        delay(200)
        showIcon = true

        delay(600) // Wait for spring animation
        showText = true

        delay(500)
        showListItems = 1
        delay(200)
        showListItems = 2
        delay(200)
        showListItems = 3

        delay(400)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                )
            )
    ) {
        // --- Decorative Background Blobs (Simulating blur-3xl) ---
        // Top Right
        Box(
            modifier = Modifier
                .offset(x = 100.dp, y = (-50).dp)
                .size(200.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
        )
        // Bottom Left
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = 100.dp)
                .size(250.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        // --- Main Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 1. Success Icon with Pulse and Sparkles
            Box(contentAlignment = Alignment.Center) {
                // Pulse Animation
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "scale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f, targetValue = 0.2f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "alpha"
                )

                // The Glow Ring
                if (showIcon) {
                    Box(
                        modifier = Modifier
                            .size(112.dp) // Slightly larger than icon bg
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .background(Color.White, CircleShape)
                    )
                }

                // The Main Check Icon
                AnimatedCheckIcon(showIcon)

                // The Sparkles
                if (showIcon) {
                    SparklesEffect()
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 2. Success Text
            AnimatedVisibility(
                visible = showText,
                enter = slideInVertically { 50 } + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "All Set! 🎉",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your CalorieKo account is ready.\nStart tracking your nutrition journey now!",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Checklist Items
            val checklist = listOf(
                "Profile configured",
                "Health goals set",
                "Smart scale connected"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                checklist.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = showListItems > index,
                        enter = slideInVertically { 50 } + fadeIn()
                    ) {
                        SuccessCheckItem(text = item)
                    }
                }
            }
        }

        // --- Bottom Button ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = showButton,
                enter = slideInVertically { 100 } + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onEnterDashboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = "Enter Dashboard",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalorieKoGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ready to start your healthy journey",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// --- Helper Component: Animated Check Icon ---
@Composable
fun AnimatedCheckIcon(showIcon: Boolean) {
    AnimatedVisibility(
        visible = showIcon,
        enter = androidx.compose.animation.scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = CalorieKoGreen,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

// --- Helper Component: Checklist Item ---
@Composable
fun SuccessCheckItem(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Tiny Circle Check
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}

// --- Helper Component: Sparkles Animation ---
@Composable
fun SparklesEffect() {
    // We create 6 sparkles distributed in a circle
    val sparkleCount = 6
    val angleStep = (2 * Math.PI) / sparkleCount

    val transition = rememberInfiniteTransition(label = "sparkle")
    // Note: In React this was a one-off animation, but Infinite loop is often nicer for "Success" screens.
    // If you want one-off, we'd use Animatable. Let's do One-Off to match React exactly.

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Delay slightly to match the icon pop
        delay(500)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        )
    }

    if (progress.value < 1f) { // Only render while animating
        Box(modifier = Modifier.size(1.dp)) { // Anchor point center
            repeat(sparkleCount) { i ->
                val angle = i * angleStep
                // Calculate position based on progress (moving outward)
                val distance = 60.dp * progress.value // Moves 60dp out
                val x = (cos(angle) * distance.value).dp
                val y = (sin(angle) * distance.value).dp

                // Scale goes 0 -> 1 -> 0
                // Opacity goes 0 -> 1 -> 0
                // We simulate this with a simple curve logic based on progress
                val scale = if (progress.value < 0.5f) progress.value * 2 else (1f - progress.value) * 2

                Icon(
                    imageVector = Icons.Rounded.AutoAwesome, // Sparkle Icon
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .scale(scale)
                        .size(24.dp)
                )
            }
        }
    }
}