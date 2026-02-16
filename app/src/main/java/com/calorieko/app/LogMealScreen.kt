package com.calorieko.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Constants ---
val SUPPORTED_DISHES = listOf(
    "Chicken Adobo", "Pork Sinigang", "Beef Kare-Kare", "Lechon Kawali",
    "Pancit Canton", "Lumpia Shanghai", "Bicol Express", "Sisig",
    "Tinola", "Caldereta"
)

enum class ScanStatus { IDLE, DETECTING_SCALE, DETECTING_DISH, READY, ERROR }

@Composable
fun LogMealScreen(onBack: () -> Unit, onMealLogged: (String, Int) -> Unit) {
    var flashEnabled by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf(ScanStatus.IDLE) }
    var weight by remember { mutableIntStateOf(0) }
    var detectedDish by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // --- Simulation Logic ---
    fun startSimulation() {
        scope.launch {
            scanStatus = ScanStatus.DETECTING_SCALE

            // Simulate weight counting up
            var currentWeight = 0
            val targetWeight = 250
            while (currentWeight < targetWeight) {
                delay(100)
                currentWeight += Random.nextInt(10, 30)
                if (currentWeight > targetWeight) currentWeight = targetWeight
                weight = currentWeight
            }

            scanStatus = ScanStatus.DETECTING_DISH
            delay(2000) // Simulate AI processing

            // Success/Fail Chance (80% success)
            val isSuccessful = Random.nextDouble() > 0.2

            if (isSuccessful) {
                detectedDish = SUPPORTED_DISHES.random()
                scanStatus = ScanStatus.READY
            } else {
                scanStatus = ScanStatus.ERROR
                errorMessage = "Dish Not Supported. CalorieKo is specialized for specific regional dishes. Please ensure proper lighting."
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1500) // Initial delay before starting
        startSimulation()
    }

    fun handleRetry() {
        weight = 0
        detectedDish = null
        errorMessage = null
        scanStatus = ScanStatus.IDLE
        scope.launch {
            delay(100)
            startSimulation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera Preview Simulation (Gradient + Grid)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF111827), Color(0xFF1F2937), Color(0xFF111827))
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                val step = 40.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawCircle(Color.White, radius = 1f, center = Offset(x.toFloat(), 0f)) // Simplified grid dots
                }
            }
        }

        // 2. Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }

            IconButton(
                onClick = { flashEnabled = !flashEnabled },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    null,
                    tint = if (flashEnabled) Color(0xFFFFD700) else Color.White
                )
            }
        }

        // 3. Real-Time Data Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Weight Badge
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(50),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (weight > 0) CalorieKoGreen else Color.Gray,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weight: ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "${weight}g",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalorieKoGreen
                    )
                }
            }

            // AI Badge
            val aiBadgeColor = when (scanStatus) {
                ScanStatus.DETECTING_DISH -> CalorieKoOrange.copy(alpha = 0.95f)
                ScanStatus.READY -> CalorieKoGreen.copy(alpha = 0.95f)
                else -> Color.White.copy(alpha = 0.95f)
            }

            Surface(
                color = aiBadgeColor,
                shape = RoundedCornerShape(50),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (scanStatus == ScanStatus.DETECTING_DISH) {
                        Text("Analyzing...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    } else if (detectedDish != null) {
                        Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(detectedDish!!, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("No dish detected", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                    }
                }
            }
        }

        // 4. Scanner Animation Overlay
        if (scanStatus == ScanStatus.DETECTING_SCALE || scanStatus == ScanStatus.DETECTING_DISH) {
            ScannerAnimation()
        }

        // 5. Scale Framing Guide
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 50.dp) // Adjust based on screen height
                .width(280.dp)
                .aspectRatio(4f / 3f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val borderColor = if (weight > 0) CalorieKoGreen else Color.White.copy(alpha = 0.5f)

                // Dashed Border
                drawRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )
                )

                // Corner Markers
                val cornerLen = 20.dp.toPx()
                val stroke = 4.dp.toPx()

                // Top Left
                drawLine(Color.White, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
                drawLine(Color.White, Offset(0f, 0f), Offset(0f, cornerLen), stroke)

                // Top Right
                drawLine(Color.White, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), stroke)
                drawLine(Color.White, Offset(size.width, 0f), Offset(size.width, cornerLen), stroke)

                // Bottom Left
                drawLine(Color.White, Offset(0f, size.height), Offset(cornerLen, size.height), stroke)
                drawLine(Color.White, Offset(0f, size.height), Offset(0f, size.height - cornerLen), stroke)

                // Bottom Right
                drawLine(Color.White, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), stroke)
                drawLine(Color.White, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), stroke)
            }

            Text(
                text = if (weight > 0) "✓ Scale detected" else "Place Smart Scale within frame",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 16.dp)
                    .offset(y = 30.dp)
            )
        }

        // 6. Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(24.dp)
        ) {
            Button(
                onClick = {
                    if (scanStatus == ScanStatus.READY && detectedDish != null) {
                        onMealLogged(detectedDish!!, weight)
                        onBack()
                    }
                },
                enabled = scanStatus == ScanStatus.READY,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalorieKoOrange,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (scanStatus == ScanStatus.READY) "Confirm Log" else "Detecting...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (scanStatus != ScanStatus.READY && scanStatus != ScanStatus.ERROR) {
                Text(
                    text = when(scanStatus) {
                        ScanStatus.DETECTING_SCALE -> "Waiting for scale data..."
                        ScanStatus.DETECTING_DISH -> "AI is analyzing the dish..."
                        else -> "Position your dish and scale..."
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 7. Error Overlay
        if (scanStatus == ScanStatus.ERROR && errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(enabled = false) {} // Absorb clicks
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFFEE2E2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Dish Not Supported", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMessage!!,
                            fontSize = 14.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { handleRetry() },
                                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val yPercent by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = size.height * yPercent

        // Draw Scanning Line
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, CalorieKoGreen, Color.Transparent)
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 4.dp.toPx()
        )

        // Draw Glow
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(CalorieKoGreen.copy(alpha = 0f), CalorieKoGreen.copy(alpha = 0.2f)),
                startY = y - 50f,
                endY = y
            ),
            topLeft = Offset(0f, y - 50f),
            size = androidx.compose.ui.geometry.Size(size.width, 50f)
        )
    }
}