package com.calorieko.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*
import kotlinx.coroutines.delay

// Define the 3 states of the screen
enum class PairingStatus {
    SEARCHING, CONNECTING, CONNECTED
}

@Composable
fun ScalePairingScreen(onComplete: () -> Unit) {
    var status by remember { mutableStateOf(PairingStatus.SEARCHING) }

    // --- 1. The Timeline Logic ---
    LaunchedEffect(Unit) {
        delay(2000) // Search for 2 seconds
        status = PairingStatus.CONNECTING

        delay(2000) // Connect for 2 seconds
        status = PairingStatus.CONNECTED

        delay(1500) // Show success for 1.5 seconds
        onComplete()
    }

    // --- 2. Animations ---
    // Infinite transition for the pulsing circles
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulse 1
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha1"
    )

    // Pulse 2 (Delayed slightly by offset logic in a real app, simplified here)
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha2"
    )

    // UI Structure
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F9FA), Color.White)
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // --- Central Animation Container ---
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background Pulsing Circles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale1)
                    .alpha(pulseAlpha1)
                    .background(CalorieKoGreen, CircleShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .scale(pulseScale2)
                    .alpha(pulseAlpha2)
                    .background(CalorieKoGreen, CircleShape)
            )

            // Center Icon Card
            val isConnected = status == PairingStatus.CONNECTED

            // Gradient Logic: Blue/Purple if searching, Green if connected
            val gradientBrush = if (isConnected) {
                Brush.linearGradient(listOf(CalorieKoGreen, CalorieKoLightGreen))
            } else {
                Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFF7E57C2)))
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.size(128.dp),
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isConnected,
                        transitionSpec = {
                            scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                        },
                        label = "IconSwitch"
                    ) { connected ->
                        if (connected) {
                            Icon(
                                imageVector = Icons.Rounded.MonitorWeight, // Scale Icon
                                contentDescription = "Connected",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = "Bluetooth",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }

            // Scale Illustration (Shows up at bottom when not searching)
            androidx.compose.animation.AnimatedVisibility(
                visible = status != PairingStatus.SEARCHING,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 40.dp)
            ) {
                ScaleGraphic()
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // --- Status Text ---
        AnimatedContent(
            targetState = status,
            label = "TextChange"
        ) { currentStatus ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (currentStatus) {
                        PairingStatus.SEARCHING -> "Searching for Device..."
                        PairingStatus.CONNECTING -> "Connecting..."
                        PairingStatus.CONNECTED -> "Connected!"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (currentStatus) {
                        PairingStatus.SEARCHING -> "Looking for your CalorieKo Smart Scale nearby"
                        PairingStatus.CONNECTING -> "Auto-connecting to your CalorieKo Smart Scale"
                        PairingStatus.CONNECTED -> "Your smart scale is ready to use"
                    },
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Progress Dots ---
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val step = when (status) {
                PairingStatus.SEARCHING -> 0
                PairingStatus.CONNECTING -> 1
                PairingStatus.CONNECTED -> 2
            }

            repeat(3) { index ->
                val active = index <= step
                val current = index == step

                // Animate dots size/color
                val scale by animateFloatAsState(if (current) 1.2f else 1f, label = "dotScale")
                val alpha by animateFloatAsState(if (active) 1f else 0.3f, label = "dotAlpha")

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (active) CalorieKoGreen else Color.Gray)
                )
            }
        }

        // --- Tips Box (Only when searching) ---
        Spacer(modifier = Modifier.height(48.dp))
        AnimatedVisibility(
            visible = status == PairingStatus.SEARCHING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFFEFF6FF), // Blue 50
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)), // Blue 100
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Make sure your smart scale is powered on and within range",
                    color = Color(0xFF1E3A8A), // Blue 900
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// Custom Graphic to mimic the "3D Scale" illustration
@Composable
fun ScaleGraphic() {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(90.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB))),
                RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // The "Screen" of the scale
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 12.dp)
                .width(80.dp)
                .height(40.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF374151), Color(0xFF1F2937))),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "888.8",
                color = Color(0xFF4ADE80), // Bright Green Digital Text
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Brand Name at bottom
        Text(
            text = "CalorieKo",
            fontSize = 10.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}