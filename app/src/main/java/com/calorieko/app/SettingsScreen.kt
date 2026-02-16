package com.calorieko.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── SETTINGS SCREEN ─────────────────────────────────────

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("settings") }

    // State for all settings
    var scaleConnected by remember { mutableStateOf(true) }
    val scaleBattery = 87
    var isCalibrating by remember { mutableStateOf(false) }
    var dataEncryption by remember { mutableStateOf(true) }
    var localProcessing by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(true) }
    var activityAlerts by remember { mutableStateOf(false) }
    var useMetricUnits by remember { mutableStateOf(true) }
    var showWipeDialog by remember { mutableStateOf(false) }

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            // ── Header ──
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                }
            }

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ═══════════════════════════════════════════
                // HARDWARE (IoT Integration)
                // ═══════════════════════════════════════════
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300, easing = EaseOutCubic)
                    )
                ) {
                    Column {
                        SettingsSectionHeader("HARDWARE (IoT Integration)")

                        Surface(
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Scale Connectivity
                                SettingsRow(
                                    icon = Icons.Default.Bluetooth,
                                    iconBg = if (scaleConnected) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                                    iconTint = if (scaleConnected) Color(0xFF4CAF50) else Color(0xFF9CA3AF),
                                    title = "CalorieKo Smart Scale",
                                    subtitle = if (scaleConnected) "Connected" else "Disconnected",
                                    subtitleColor = if (scaleConnected) Color(0xFF4CAF50) else Color(0xFF6B7280),
                                    showDivider = true,
                                    trailing = {
                                        IconButton(onClick = { scaleConnected = !scaleConnected }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Toggle connection",
                                                tint = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                )

                                // Scale Battery
                                SettingsRow(
                                    icon = Icons.Default.BatteryFull,
                                    iconBg = Color(0xFFF9FAFB),
                                    iconTint = Color(0xFF374151),
                                    title = "Scale Battery",
                                    showDivider = true,
                                    trailing = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Battery bar
                                            Box(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xFFE5E7EB))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(scaleBattery / 100f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(50))
                                                        .background(
                                                            when {
                                                                scaleBattery > 50 -> Color(0xFF4CAF50)
                                                                scaleBattery > 20 -> Color(0xFFFF9800)
                                                                else -> Color(0xFFEF4444)
                                                            }
                                                        )
                                                )
                                            }
                                            Text(
                                                "$scaleBattery%",
                                                fontSize = 12.sp,
                                                color = Color(0xFF4B5563)
                                            )
                                        }
                                    },
                                    customSubtitle = {
                                        // Battery bar is in trailing
                                    }
                                )

                                // Calibration
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Calibration",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF111827)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Recalibrate load cell for accuracy",
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (!isCalibrating) {
                                                isCalibrating = true
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    isCalibrating = false
                                                }
                                            }
                                        },
                                        enabled = !isCalibrating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50),
                                            disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            if (isCalibrating) "Calibrating..." else "Recalibrate",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════
                // SECURITY & DATA PRIVACY
                // ═══════════════════════════════════════════
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = 80)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300, delayMillis = 80, easing = EaseOutCubic)
                    )
                ) {
                    Column {
                        SettingsSectionHeader("SECURITY & DATA PRIVACY")

                        Surface(
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Data Encryption
                                SettingsToggleRow(
                                    icon = Icons.Default.Shield,
                                    iconBg = Color(0xFFEFF6FF),
                                    iconTint = Color(0xFF2563EB),
                                    title = "Data Encryption",
                                    subtitle = "SQLCipher AES-256 Encryption",
                                    checked = dataEncryption,
                                    onCheckedChange = { dataEncryption = it },
                                    showDivider = true
                                )

                                // Edge AI Inference
                                SettingsToggleRow(
                                    icon = Icons.Default.Security,
                                    iconBg = Color(0xFFF5F3FF),
                                    iconTint = Color(0xFF7C3AED),
                                    title = "Edge AI Inference",
                                    subtitle = "Local Image Processing Only",
                                    checked = localProcessing,
                                    onCheckedChange = { localProcessing = it },
                                    showDivider = true
                                )

                                // Wipe All Local Data
                                Column(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = { showWipeDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFEF2F2)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 14.dp)
                                    ) {
                                        Text(
                                            "Wipe All Local Data",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "This will delete all locally stored logs and data",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════
                // PREFERENCES
                // ═══════════════════════════════════════════
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(300, delayMillis = 160)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300, delayMillis = 160, easing = EaseOutCubic)
                    )
                ) {
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        SettingsSectionHeader("PREFERENCES")

                        Surface(
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // ── Notifications sub-header ──
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 24.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF4B5563)
                                        )
                                        Text(
                                            "Notifications",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF374151)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE5E7EB))

                                // Meal Reminders
                                SettingsToggleRow(
                                    title = "Meal Reminders",
                                    subtitle = "Get notified for breakfast, lunch, and dinner",
                                    checked = mealReminders,
                                    onCheckedChange = { mealReminders = it },
                                    showDivider = true
                                )

                                // Activity Alerts
                                SettingsToggleRow(
                                    title = "Activity Alerts",
                                    subtitle = "Reminders to stay active throughout the day",
                                    checked = activityAlerts,
                                    onCheckedChange = { activityAlerts = it },
                                    showDivider = true
                                )

                                // ── Units sub-header ──
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 24.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Straighten,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF4B5563)
                                        )
                                        Text(
                                            "Units",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF374151)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE5E7EB))

                                // Measurement System toggle
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        "Measurement System",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Metric button
                                        Button(
                                            onClick = { useMetricUnits = true },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (useMetricUnits) Color(0xFF4CAF50) else Color(0xFFF3F4F6)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            elevation = if (useMetricUnits) ButtonDefaults.buttonElevation(1.dp) else ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Text(
                                                "Metric (kg / cm)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (useMetricUnits) Color.White else Color(0xFF4B5563)
                                            )
                                        }

                                        // Imperial button
                                        Button(
                                            onClick = { useMetricUnits = false },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!useMetricUnits) Color(0xFF4CAF50) else Color(0xFFF3F4F6)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            elevation = if (!useMetricUnits) ButtonDefaults.buttonElevation(1.dp) else ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Text(
                                                "Imperial (lbs / in)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (!useMetricUnits) Color.White else Color(0xFF4B5563)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Wipe Data Confirmation Dialog ──
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Text(
                    "Wipe All Local Data?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            },
            text = {
                Text(
                    "Are you sure you want to wipe all local data? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWipeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Wipe Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─── SECTION HEADER ──────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6B7280),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

// ─── GENERIC SETTINGS ROW ────────────────────────────────

@Composable
fun SettingsRow(
    icon: ImageVector? = null,
    iconBg: Color = Color(0xFFF9FAFB),
    iconTint: Color = Color(0xFF374151),
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = Color(0xFF6B7280),
    showDivider: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    customSubtitle: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                }
                if (customSubtitle != null) {
                    customSubtitle()
                }
            }

            if (trailing != null) {
                trailing()
            }
        }

        if (showDivider) {
            HorizontalDivider(color = Color(0xFFE5E7EB))
        }
    }
}

// ─── TOGGLE SETTINGS ROW ─────────────────────────────────

@Composable
fun SettingsToggleRow(
    icon: ImageVector? = null,
    iconBg: Color = Color.Transparent,
    iconTint: Color = Color(0xFF374151),
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD1D5DB),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(color = Color(0xFFE5E7EB))
        }
    }
}
