package com.calorieko.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Refresh
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
import com.calorieko.app.ble.BleConnectionState
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.ui.theme.*
import kotlinx.coroutines.delay

// Define the states of the screen
enum class PairingStatus {
    SEARCHING, CONNECTING, CONNECTED, FAILED
}

@Composable
fun ScalePairingScreen(
    bleScaleManager: BleScaleManager,
    onComplete: () -> Unit
) {
    // ── Observe real BLE state ──
    val bleState by bleScaleManager.connectionState.collectAsState()

    // Map BLE state → UI state
    val status = when (bleState) {
        is BleConnectionState.Idle -> PairingStatus.SEARCHING
        is BleConnectionState.Scanning -> PairingStatus.SEARCHING
        is BleConnectionState.Connecting -> PairingStatus.CONNECTING
        is BleConnectionState.Connected -> PairingStatus.CONNECTED
        is BleConnectionState.Failed -> PairingStatus.FAILED
    }

    val failureReason = (bleState as? BleConnectionState.Failed)?.reason ?: ""

    // ── Permission handling ──
    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }.toTypedArray()
    }

    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        permissionsGranted = grants.values.all { it }
        if (permissionsGranted) {
            bleScaleManager.startScan()
        } else {
            bleScaleManager.failWithReason("Bluetooth permissions are required to connect to your scale.")
        }
    }

    // Start scan when permissions are granted (or request them)
    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    // Scan timeout — if still scanning after 15 s, show failure
    LaunchedEffect(bleState) {
        if (bleState is BleConnectionState.Scanning) {
            delay(15_000)
            // If still scanning after 15 s, stop and fail
            if (bleScaleManager.connectionState.value is BleConnectionState.Scanning) {
                bleScaleManager.stopScan()
                bleScaleManager.failWithReason("Scan timed out. Make sure your scale is on and nearby.")
            }
        }
    }

    // Navigate on success (after showing "Connected" for 1.5 s)
    LaunchedEffect(status) {
        if (status == PairingStatus.CONNECTED) {
            delay(1500)
            onComplete()
        }
    }

    // Clean up scan when leaving the screen (connection stays alive at AppNavigation scope)
    DisposableEffect(Unit) {
        onDispose { bleScaleManager.stopScan() }
    }

    // ── Animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

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

    // ── UI ──
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

        // ── Central Animation Container ──
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background Pulsing Circles (only pulse while searching/connecting)
            if (status == PairingStatus.SEARCHING || status == PairingStatus.CONNECTING) {
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
            }

            val isConnected = status == PairingStatus.CONNECTED
            val isFailed = status == PairingStatus.FAILED

            val gradientBrush = when {
                isConnected -> Brush.linearGradient(listOf(CalorieKoGreen, CalorieKoLightGreen))
                isFailed -> Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFFF7043)))
                else -> Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFF7E57C2)))
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
                                imageVector = Icons.Rounded.MonitorWeight,
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

            // Scale illustration (shows when connecting or connected)
            androidx.compose.animation.AnimatedVisibility(
                visible = status == PairingStatus.CONNECTING || status == PairingStatus.CONNECTED,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 40.dp)
            ) {
                ScaleGraphic()
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // ── Status Text ──
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
                        PairingStatus.FAILED -> "Connection Failed"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentStatus == PairingStatus.FAILED) Color(0xFFE53935) else Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (currentStatus) {
                        PairingStatus.SEARCHING -> "Looking for your CalorieKo Smart Scale nearby"
                        PairingStatus.CONNECTING -> "Auto-connecting to your CalorieKo Smart Scale"
                        PairingStatus.CONNECTED -> "Your smart scale is ready to use"
                        PairingStatus.FAILED -> failureReason.ifEmpty { "Could not reach your scale" }
                    },
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Progress Dots ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val step = when (status) {
                PairingStatus.SEARCHING -> 0
                PairingStatus.CONNECTING -> 1
                PairingStatus.CONNECTED -> 2
                PairingStatus.FAILED -> 0
            }

            repeat(3) { index ->
                val active = index <= step && status != PairingStatus.FAILED
                val current = index == step && status != PairingStatus.FAILED

                val dotScale by animateFloatAsState(if (current) 1.2f else 1f, label = "dotScale")
                val dotAlpha by animateFloatAsState(if (active) 1f else 0.3f, label = "dotAlpha")

                Box(
                    modifier = Modifier
                        .scale(dotScale)
                        .alpha(dotAlpha)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (active) CalorieKoGreen else Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // ── Tips Box (only when searching) ──
        AnimatedVisibility(
            visible = status == PairingStatus.SEARCHING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Make sure your smart scale is powered on and within range",
                    color = Color(0xFF1E3A8A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // ── Retry Button (only on failure) ──
        AnimatedVisibility(
            visible = status == PairingStatus.FAILED,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = { bleScaleManager.startScan() },
                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                color = Color(0xFF4ADE80),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "CalorieKo",
            fontSize = 10.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}