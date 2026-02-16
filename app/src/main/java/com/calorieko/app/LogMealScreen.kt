package com.calorieko.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── Supported Regional Dishes (from Figma) ──────────────

val SUPPORTED_DISHES = listOf(
    "Chicken Adobo",
    "Pork Sinigang",
    "Beef Kare-Kare",
    "Lechon Kawali",
    "Pancit Canton",
    "Lumpia Shanghai",
    "Bicol Express",
    "Sisig",
    "Tinola",
    "Caldereta",
    "Menudo",
    "Afritada",
    "Pinakbet",
    "Laing",
    "Bulalo",
    "Balbacua",
    "Law-uy",
    "Binagoongan",
    "Dinuguan",
    "Paksiw na Isda"
)

// ─── MAIN LOG MEAL SCREEN ────────────────────────────────

@Composable
fun LogMealScreen(
    onBack: () -> Unit,
    onMealLogged: ((name: String, weight: Int) -> Unit)? = null
) {
    var flashEnabled by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("idle") } // idle, detecting-scale, detecting-dish, ready, error
    var weight by remember { mutableIntStateOf(0) }
    var detectedDish by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Simulate automatic scanning process
    LaunchedEffect(scanStatus) {
        if (scanStatus == "idle") {
            // Wait 1.5s, then start detecting scale
            delay(1500)
            scanStatus = "detecting-scale"
        }
    }

    // Simulate weight counting up
    LaunchedEffect(scanStatus) {
        if (scanStatus == "detecting-scale") {
            var currentWeight = 0
            while (currentWeight < 250) {
                delay(100)
                currentWeight += (10..40).random()
                if (currentWeight >= 250) currentWeight = 250
                weight = currentWeight
            }
            // Weight stable – start dish detection
            scanStatus = "detecting-dish"
        }
    }

    // Simulate AI dish detection
    LaunchedEffect(scanStatus) {
        if (scanStatus == "detecting-dish") {
            delay(2000) // AI processing time
            // 80% chance of success
            val isSuccessful = (1..5).random() != 1
            if (isSuccessful) {
                detectedDish = SUPPORTED_DISHES.random()
                scanStatus = "ready"
            } else {
                errorMessage = "Dish Not Supported. CalorieKo is specialized for 20-30 specific regional dishes. Please ensure proper lighting or try a supported dish."
                scanStatus = "error"
            }
        }
    }

    // Handle confirm
    val handleConfirmLog: () -> Unit = {
        if (scanStatus == "ready" && detectedDish != null && weight > 0) {
            onMealLogged?.invoke(detectedDish!!, weight)
            onBack()
        }
    }

    // Handle retry
    val handleRetry: () -> Unit = {
        weight = 0
        detectedDish = null
        errorMessage = null
        scanStatus = "idle"
    }

    // ── Full-screen camera UI ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Simulated Camera Preview (gradient bg) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111827), // gray-900
                            Color(0xFF1F2937), // gray-800
                            Color(0xFF111827)  // gray-900
                        )
                    )
                )
        )

        // ── Top Controls (Cancel + Flash) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Cancel Button (X)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Flash Toggle
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { flashEnabled = !flashEnabled }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn
                        else Icons.Default.FlashOff,
                        contentDescription = if (flashEnabled) "Disable flash" else "Enable flash",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Real-Time Data Badges ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 72.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Weight Badge (bg-white/95 rounded-full)
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated green dot when weight detected
                    val dotColor = if (weight > 0) Color(0xFF4CAF50) else Color(0xFFD1D5DB)

                    if (weight > 0) {
                        val pulseAnim = rememberInfiniteTransition(label = "weightPulse")
                        val pulseAlpha by pulseAnim.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "weightDotAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor.copy(alpha = pulseAlpha))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weight: ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937) // gray-800
                    )
                    Text(
                        text = "${weight}g",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50) // green
                    )
                }
            }

            // AI Identification Badge
            val aiBadgeColor = when {
                scanStatus == "detecting-dish" -> Color(0xFFFF9800).copy(alpha = 0.95f)
                detectedDish != null -> Color(0xFF4CAF50).copy(alpha = 0.95f)
                else -> Color.White.copy(alpha = 0.95f)
            }

            Surface(
                color = aiBadgeColor,
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        scanStatus == "detecting-dish" -> {
                            val pulseAnim = rememberInfiniteTransition(label = "aiPulse")
                            val pulseAlpha by pulseAnim.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "aiPulseAlpha"
                            )
                            Text(
                                text = "Analyzing...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = pulseAlpha)
                            )
                        }
                        detectedDish != null -> {
                            val pulseAnim = rememberInfiniteTransition(label = "dishPulse")
                            val dotAlpha by pulseAnim.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dishDotAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = dotAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = detectedDish!!,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                        else -> {
                            Text(
                                text = "No dish detected",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280) // gray-500
                            )
                        }
                    }
                }
            }
        }

        // ── Scale Framing Guide ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp)
                .width(288.dp), // w-72
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Dashed frame with corner markers
                val frameColor = if (weight > 0) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .drawBehind {
                            val dashFloat = floatArrayOf(20f, 10f)
                            drawRoundRect(
                                color = frameColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(dashFloat, 0f)
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                            )
                        }
                ) {
                    // Corner markers (white L-shaped indicators)
                    // Top-Left
                    Box(modifier = Modifier.align(Alignment.TopStart).offset((-2).dp, (-2).dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .drawBehind {
                                    drawLine(Color.White, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 4.dp.toPx())
                                    drawLine(Color.White, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 4.dp.toPx())
                                }
                        )
                    }
                    // Top-Right
                    Box(modifier = Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .drawBehind {
                                    drawLine(Color.White, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 4.dp.toPx())
                                    drawLine(Color.White, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 4.dp.toPx())
                                }
                        )
                    }
                    // Bottom-Left
                    Box(modifier = Modifier.align(Alignment.BottomStart).offset((-2).dp, 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .drawBehind {
                                    drawLine(Color.White, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 4.dp.toPx())
                                    drawLine(Color.White, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 4.dp.toPx())
                                }
                        )
                    }
                    // Bottom-Right
                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .drawBehind {
                                    drawLine(Color.White, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 4.dp.toPx())
                                    drawLine(Color.White, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 4.dp.toPx())
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Instruction Text
                Text(
                    text = if (weight > 0) "✓ Scale detected" else "Place Smart Scale within frame",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Scanning Animation Line ──
        if (scanStatus == "detecting-scale" || scanStatus == "detecting-dish") {
            val scanAnim = rememberInfiniteTransition(label = "scan")
            val scanPosition by scanAnim.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scanLine"
            )
            val scanAlpha by scanAnim.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scanAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(scanPosition)
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF4CAF50).copy(alpha = scanAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // ── Error Overlay ──
        AnimatedVisibility(
            visible = scanStatus == "error" && errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Error icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)), // red-100
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFDC2626) // red-600
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Dish Not Supported",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage ?: "",
                            fontSize = 14.sp,
                            color = Color(0xFF4B5563), // gray-600
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel button
                            Button(
                                onClick = onBack,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF3F4F6) // gray-100
                                )
                            ) {
                                Text(
                                    "Cancel",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151) // gray-700
                                )
                            }
                            // Try Again button
                            Button(
                                onClick = handleRetry,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800)
                                )
                            ) {
                                Text(
                                    "Try Again",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom: Confirm Button + Status ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = handleConfirmLog,
                    enabled = scanStatus == "ready",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        disabledContainerColor = Color(0xFF4B5563).copy(alpha = 0.5f) // gray-600
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = if (scanStatus == "ready") "Confirm Log" else "Detecting...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Status hint
                if (scanStatus != "ready" && scanStatus != "error") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (scanStatus) {
                            "detecting-scale" -> "Waiting for scale data..."
                            "detecting-dish" -> "AI is analyzing the dish..."
                            else -> "Position your dish and scale..."
                        },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
