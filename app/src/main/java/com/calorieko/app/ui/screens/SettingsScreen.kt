package com.calorieko.app.ui.screens



import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.calorieko.app.ui.components.BottomNavigation
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("settings") }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // --- State ---
    var scaleConnected by remember { mutableStateOf(true) }
    var scaleBattery by remember { mutableIntStateOf(87) }
    var dataEncryption by remember { mutableStateOf(true) }
    var localProcessing by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(true) }
    var activityAlerts by remember { mutableStateOf(false) }
    var useMetricUnits by remember { mutableStateOf(true) }

    // Dialog States
    var showRecalibrationDialog by remember { mutableStateOf(false) }
    var isCalibrating by remember { mutableStateOf(false) }

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var isWiping by remember { mutableStateOf(false) }

    // --- ADD THIS NEW STATE ---
    var showLogOutConfirmDialog by remember { mutableStateOf(false) }

    // Logic Handlers
    fun handleRecalibrate() {
        scope.launch {
            isCalibrating = true
            delay(2000) // Simulate work
            isCalibrating = false
            snackbarHostState.showSnackbar("Scale recalibrated successfully!")
        }
    }

    fun handleWipeData() {
        showWipeConfirmDialog = false
        scope.launch {
            isWiping = true
            delay(1500) // Simulate work
            isWiping = false
            snackbarHostState.showSnackbar("Local data has been wiped successfully.")
        }
    }
    fun handleSignOut() {
        scope.launch {
            try {
                // 1. Clear the Firebase Auth local session
                FirebaseAuth.getInstance().signOut()

                // 2. Clear the Google Credential Manager cache
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())

                // 3. Trigger navigation back to the intro/login screen
                onNavigate("logout")

            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error signing out: ${e.message}")
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // 1. Hardware Section
                SettingsSectionHeader("Hardware (IoT Integration)")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Scale Connectivity
                    SettingsRow(
                        icon = Icons.Default.Bluetooth,
                        iconBgColor = if (scaleConnected) Color(0xFFECFDF5) else Color(0xFFF3F4F6),
                        iconColor = if (scaleConnected) CalorieKoGreen else Color.Gray,
                        title = "CalorieKo Smart Scale",
                        subtitle = if (scaleConnected) "Connected" else "Disconnected",
                        subtitleColor = if (scaleConnected) CalorieKoGreen else Color.Gray,
                        onClick = { scaleConnected = !scaleConnected }
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = Color(0xFFF3F4F6)
                    )

                    // Battery
                    SettingsRow(
                        icon = Icons.Default.BatteryStd,
                        iconBgColor = Color(0xFFF3F4F6),
                        iconColor = Color(0xFF374151),
                        title = "Scale Battery",
                        subtitle = null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Battery Bar
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE5E7EB))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(scaleBattery / 100f)
                                        .background(
                                            if (scaleBattery > 50) CalorieKoGreen
                                            else if (scaleBattery > 20) Color(0xFFFF9800)
                                            else Color(0xFFEF4444)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$scaleBattery%", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = Color(0xFFF3F4F6)
                    )

                    // Calibration Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Calibration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                            Text("Recalibrate load cell for accuracy", fontSize = 12.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = { handleRecalibrate() },
                            enabled = !isCalibrating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CalorieKoGreen,
                                disabledContainerColor = CalorieKoGreen.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(if (isCalibrating) "Calibrating..." else "Recalibrate", fontSize = 12.sp)
                        }
                    }
                }

                // 2. Security Section
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionHeader("Security & Data Privacy")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Encryption
                    SettingsRow(
                        icon = Icons.Default.Shield,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconColor = Color(0xFF2563EB),
                        title = "Data Encryption",
                        subtitle = "SQLCipher AES-256 Encryption"
                    ) {
                        Switch(
                            checked = dataEncryption,
                            onCheckedChange = { dataEncryption = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CalorieKoGreen)
                        )
                    }
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = Color(0xFFF3F4F6)
                    )

                    // Edge AI
                    SettingsRow(
                        icon = Icons.Default.Security,
                        iconBgColor = Color(0xFFFAF5FF),
                        iconColor = Color(0xFF9333EA),
                        title = "Edge AI Inference",
                        subtitle = "Local Image Processing Only"
                    ) {
                        Switch(
                            checked = localProcessing,
                            onCheckedChange = { localProcessing = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CalorieKoGreen)
                        )
                    }

                    // Wipe Data Button
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = { showWipeConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isWiping) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFDC2626), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wiping Data...")
                            } else {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wipe All Local Data", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This will delete all locally stored logs and data",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // --- Sign Out Button ---
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    OutlinedButton(
                        onClick = { showLogOutConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF374151)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Log Out", fontWeight = FontWeight.SemiBold)
                    }
                }

                // 3. Preferences Section
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionHeader("Preferences")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Notification Header
                    SubSectionHeader("Notifications", Icons.Default.Notifications)

                    SettingsToggleRow(
                        title = "Meal Reminders",
                        subtitle = "Get notified for breakfast, lunch, and dinner",
                        checked = mealReminders,
                        onCheckedChange = { mealReminders = it }
                    )
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = Color(0xFFF3F4F6)
                    )

                    SettingsToggleRow(
                        title = "Activity Alerts",
                        subtitle = "Reminders to stay active throughout the day",
                        checked = activityAlerts,
                        onCheckedChange = { activityAlerts = it }
                    )

                    // Units Header
                    SubSectionHeader("Units", Icons.Default.Straighten)

                    // Unit Switcher
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Measurement System", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitButton(
                                text = "Metric (kg / cm)",
                                isSelected = useMetricUnits,
                                onClick = { useMetricUnits = true },
                                modifier = Modifier.weight(1f)
                            )
                            UnitButton(
                                text = "Imperial (lbs / in)",
                                isSelected = !useMetricUnits,
                                onClick = { useMetricUnits = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Wipe Data Confirmation Dialog
        if (showWipeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmDialog = false },
                title = { Text("Wipe Local Data?") },
                text = { Text("Are you sure you want to wipe all local data? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = { handleWipeData() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Wipe Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = Color.White,
                titleContentColor = Color(0xFF1F2937),
                textContentColor = Color(0xFF4B5563)
            )
        }

        // Log Out Confirmation Dialog
        if (showLogOutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogOutConfirmDialog = false },
                title = { Text("Log Out?") },
                text = { Text("Are you sure you want to log out of your account?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogOutConfirmDialog = false
                            handleSignOut()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogOutConfirmDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = Color.White,
                titleContentColor = Color(0xFF1F2937),
                textContentColor = Color(0xFF4B5563)
            )
        }
    }
}




// --- Helper Components ---

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF6B7280),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SubSectionHeader(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Icon(icon, null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String?,
    subtitleColor: Color = Color(0xFF6B7280),
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = subtitleColor)
            }
        }
        trailingContent()
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CalorieKoGreen)
        )
    }
}

@Composable
fun UnitButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CalorieKoGreen else Color(0xFFF3F4F6),
            contentColor = if (isSelected) Color.White else Color(0xFF4B5563)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}