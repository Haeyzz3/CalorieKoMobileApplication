package com.calorieko.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import com.calorieko.app.ble.BleConnectionState
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.calorieko.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

/**
 * Type of notification banner to display in the Settings screen.
 */
private enum class NotificationType {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Tracks the current step in the calibration wizard.
 */
private enum class CalibrationStep {
    TEST_ACCURACY,      // Step 1: Passive live weight view
    INPUT_TRUTH_VALUE,  // Step 3: Ask for actual weight of item on scale
    CALIBRATING,        // Waiting for CAL_OK from the scale
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    bleScaleManager: BleScaleManager? = null // Brought back to handle scale options
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountPassword by remember { mutableStateOf("") }
    var deleteAccountPasswordVisible by remember { mutableStateOf(false) }

    // Collect ViewModel State
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isWipingProgress by viewModel.isWipingProgress.collectAsState()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()

    // State for the notification banner system
    var showNotificationBanner by remember { mutableStateOf(false) }
    var notificationType by remember { mutableStateOf(NotificationType.SUCCESS) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }

    // --- Calibration Dialog State ---
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationStep by remember { mutableStateOf(CalibrationStep.TEST_ACCURACY) }
    var weightInput by remember { mutableStateOf("") }
    var showCalibrationBanner by remember { mutableStateOf(false) }
    var calibrationBannerMessage by remember { mutableStateOf("") }

    // Collect Live Weight from Scale
    val liveWeight by (bleScaleManager?.liveWeight?.collectAsState()) ?: remember { mutableStateOf(0f) }

    // Helper to show notification banner
    fun showBanner(type: NotificationType, title: String, message: String) {
        notificationType = type
        notificationTitle = title
        notificationMessage = message
        showNotificationBanner = true
    }

    // Handle one-shot events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsViewModel.Event.SyncSuccess -> {
                    showBanner(
                        NotificationType.SUCCESS,
                        "Sync Complete",
                        "All data synced successfully to cloud ✓"
                    )
                }
                is SettingsViewModel.Event.SyncPartial -> {
                    showBanner(
                        NotificationType.WARNING,
                        "Partial Sync",
                        event.message
                    )
                }
                is SettingsViewModel.Event.SyncError -> {
                    showBanner(
                        NotificationType.ERROR,
                        "Sync Failed",
                        event.message
                    )
                }
                is SettingsViewModel.Event.WipeProgressSuccess -> {
                    showWipeDialog = false
                    showBanner(
                        NotificationType.SUCCESS,
                        "Progress Reset",
                        "All meals, activities, and plans have been cleared."
                    )
                }
                is SettingsViewModel.Event.LogoutReady -> {
                    onLogout()
                }
                is SettingsViewModel.Event.AccountDeleted -> {
                    showDeleteAccountDialog = false
                    deleteAccountPassword = ""
                    onLogout()
                }
                is SettingsViewModel.Event.AccountDeletionError -> {
                    showBanner(
                        NotificationType.ERROR,
                        "Deletion Failed",
                        event.message
                    )
                }
                is SettingsViewModel.Event.PasswordResetSent -> {
                    showBanner(
                        NotificationType.SUCCESS,
                        "Password Reset",
                        "A link to reset your password was sent to ${event.email}"
                    )
                }
                is SettingsViewModel.Event.PasswordResetError -> {
                    showBanner(
                        NotificationType.ERROR,
                        "Reset Failed",
                        event.message
                    )
                }
            }
        }
    }

    // Auto-hide the notification banner after 4 seconds
    LaunchedEffect(showNotificationBanner) {
        if (showNotificationBanner) {
            delay(4000)
            showNotificationBanner = false
        }
    }

    // Auto-hide the calibration banner after 3 seconds
    LaunchedEffect(showCalibrationBanner) {
        if (showCalibrationBanner) {
            delay(3000)
            showCalibrationBanner = false
        }
    }

    // Listen for calibration events from the BLE scale
    LaunchedEffect(showCalibrationDialog) {
        if (showCalibrationDialog && bleScaleManager != null) {
            bleScaleManager.calibrationEvent.collect { event ->
                when (event) {
                    "CAL_OK" -> {
                        if (calibrationStep == CalibrationStep.CALIBRATING) {
                            // Don't close the dialog immediately – allow the user to see the weight "snap" to correct value
                            calibrationStep = CalibrationStep.TEST_ACCURACY
                            weightInput = ""
                            calibrationBannerMessage = "Calibration Successful"
                            showCalibrationBanner = true
                            bleScaleManager.clearCalibrationEvent()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        bottomBar = { BottomNavigation(activeTab = "settings", onTabChange = { onNavigate(it) }) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Added verticalScroll so everything fits on small screens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // --- ACCOUNT SECTION ---
                Text("Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(icon = Icons.Default.Person, title = "Edit Profile", subtitle = "Update your height, weight, and goals", iconColor = Color(0xFF3B82F6), onClick = { onNavigate("editProfile") })
                        SettingsDivider()
                        SettingsRow(icon = Icons.Default.Lock, title = "Change Password", subtitle = "Update your security credentials", iconColor = Color(0xFF8B5CF6), onClick = { showPasswordResetDialog = true })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- PREFERENCES SECTION ---
                Text("Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(icon = Icons.Default.Notifications, title = "System Notifications", subtitle = "Manage app notification permissions", iconColor = CalorieKoOrange, onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        })
                        SettingsDivider()

                        // ── Meal Plan Reminders Toggle ──
                        var mealRemindersEnabled by remember {
                            mutableStateOf(com.calorieko.app.util.MealPlanReminderWorker.isEnabled(context))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mealRemindersEnabled = !mealRemindersEnabled
                                    com.calorieko.app.util.MealPlanReminderWorker.setEnabled(context, mealRemindersEnabled)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CalorieKoGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = CalorieKoGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Meal Plan Reminders", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                                Text(
                                    if (mealRemindersEnabled) "You'll be reminded before each planned meal" else "Meal reminders are turned off",
                                    fontSize = 12.sp, color = Color(0xFF9CA3AF)
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = mealRemindersEnabled,
                                onCheckedChange = { enabled ->
                                    mealRemindersEnabled = enabled
                                    com.calorieko.app.util.MealPlanReminderWorker.setEnabled(context, enabled)
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CalorieKoGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                )
                            )
                        }

                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Sync,
                            title = if (isSyncing) "Syncing..." else "Sync Data",
                            subtitle = if (isSyncing) "Backing up to cloud..." else "Last synced: $lastSyncedAt",
                            iconColor = CalorieKoGreen,
                            onClick = {
                                viewModel.syncAllData()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SMART SCALE SECTION ---
                Text("Smart Scale", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(icon = Icons.Default.Bluetooth, title = "Pairing Status", subtitle = "Manage Bluetooth scale", iconColor = Color(0xFF3B82F6), onClick = { onNavigate("scalePairing/settings") })
//                        SettingsDivider()
//                        // TODO: Remove Battery Level option
//                        // SettingsRow(icon = Icons.Default.BatteryStd, title = "Battery Level", subtitle = "Check scale battery", iconColor = CalorieKoGreen, onClick = { Toast.makeText(context, "Scale Battery: Good", Toast.LENGTH_SHORT).show() })
//                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.MonitorWeight,
                            title = "Recalibrate Scale",
                            subtitle = "Calibrate load cell using a known weight",
                            iconColor = CalorieKoOrange,
                            onClick = {
                                val connState = bleScaleManager?.connectionState?.value
                                if (connState is BleConnectionState.Connected) {
                                    calibrationStep = CalibrationStep.TEST_ACCURACY
                                    weightInput = ""
                                    showCalibrationDialog = true
                                } else {
                                    showBanner(NotificationType.WARNING, "Scale Not Connected", "Please pair your Bluetooth scale first.")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- ABOUT & LEGAL SECTION ---
                Text("About", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = CalorieKoGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PrivacyTip,
                                    contentDescription = null,
                                    tint = CalorieKoGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Privacy notice",
                                    color = CalorieKoGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Your health data stays on this device by default. If you choose to sign in and back up to CalorieKo Cloud, your fitness profile, meals, and workout logs are sent to our server so sync can work across devices.",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- DANGER ZONE ---
                Text("Danger Zone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.DeleteForever, title = "Reset Progress", subtitle = "Clear all meals, activities, and plans",
                            iconColor = Color(0xFFEF4444), textColor = Color(0xFFEF4444), showArrow = false,
                            onClick = { showWipeDialog = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Logout, title = "Logout", subtitle = "Sign out of your account",
                            iconColor = Color(0xFF6B7280), showArrow = false,
                            onClick = { showLogoutDialog = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.PersonOff, title = "Delete Account", subtitle = "Permanently delete your account and all data",
                            iconColor = Color(0xFFEF4444), textColor = Color(0xFFEF4444), showArrow = false,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp)) // Extra padding at bottom
            }

            // --- PREMIUM NOTIFICATION BANNER ---
            NotificationBanner(
                visible = showNotificationBanner,
                type = notificationType,
                title = notificationTitle,
                message = notificationMessage,
                onDismiss = { showNotificationBanner = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(10f)
            )

            // --- CALIBRATION SUCCESS BANNER ---
            NotificationBanner(
                visible = showCalibrationBanner,
                type = NotificationType.INFO,
                title = calibrationBannerMessage,
                message = "Your scale is now accurately calibrated.",
                onDismiss = { showCalibrationBanner = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(10f)
            )
        }
    }

    // --- RESET PROGRESS DIALOG ---
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isWipingProgress) showWipeDialog = false },
            title = { Text("Reset All Progress?", fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)) },
            text = { Text("This will permanently delete all your logged meals, activities, nutrition history, pantry items, and meal plans from this device and the cloud.\n\nYour profile and settings (name, weight, height, goals) will be preserved.\n\nThis action cannot be undone.", color = Color(0xFF4B5563)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.wipeProgress()
                    },
                    enabled = !isWipingProgress
                ) {
                    Text(if (isWipingProgress) "Resetting..." else "Yes, Reset Progress", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }, enabled = !isWipingProgress) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White,
            properties = DialogProperties(dismissOnBackPress = !isWipingProgress, dismissOnClickOutside = !isWipingProgress)
        )
    }

    // --- LOGOUT DIALOG ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)) },
            text = { Text("Are you sure you want to log out?", color = Color(0xFF4B5563)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Log Out", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White
        )
    }

    // --- DELETE ACCOUNT DIALOG ---
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteAccountDialog = false
                    deleteAccountPassword = ""
                    deleteAccountPasswordVisible = false
                }
            },
            title = {
                Text(
                    "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Column {
                    Text(
                        "This will permanently delete your account and all associated data, including:\n\n" +
                            "• Your profile and settings\n" +
                            "• All logged meals and activities\n" +
                            "• Nutrition history and meal plans\n" +
                            "• Pantry items\n\n" +
                            "This action is irreversible. Enter your password to confirm.",
                        color = Color(0xFF4B5563),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deleteAccountPassword,
                        onValueChange = { deleteAccountPassword = it },
                        label = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        singleLine = true,
                        visualTransformation = if (deleteAccountPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { deleteAccountPasswordVisible = !deleteAccountPasswordVisible }) {
                                Icon(
                                    imageVector = if (deleteAccountPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (deleteAccountPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        enabled = !isDeletingAccount,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            focusedLabelColor = Color(0xFFEF4444),
                            cursorColor = Color(0xFFEF4444)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(deleteAccountPassword)
                    },
                    enabled = deleteAccountPassword.isNotEmpty() && !isDeletingAccount
                ) {
                    Text(
                        if (isDeletingAccount) "Deleting..." else "Delete My Account",
                        color = if (deleteAccountPassword.isNotEmpty() && !isDeletingAccount)
                            Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        deleteAccountPassword = ""
                        deleteAccountPasswordVisible = false
                    },
                    enabled = !isDeletingAccount
                ) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White,
            properties = DialogProperties(
                dismissOnBackPress = !isDeletingAccount,
                dismissOnClickOutside = !isDeletingAccount
            )
        )
    }

    // --- CALIBRATION DIALOG ---
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = {
                // Only allow dismiss when not actively waiting for a BLE response
                if (calibrationStep != CalibrationStep.CALIBRATING) {
                    showCalibrationDialog = false
                    calibrationStep = CalibrationStep.TEST_ACCURACY
                    weightInput = ""
                }
            },
            title = {
                Text(
                    text = when (calibrationStep) {
                        CalibrationStep.TEST_ACCURACY -> "Test Accuracy"
                        CalibrationStep.INPUT_TRUTH_VALUE -> "Final Calibration"
                        CalibrationStep.CALIBRATING -> "Updating Scale..."
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (calibrationStep) {
                        CalibrationStep.TEST_ACCURACY -> {
                            Text(
                                text = String.format("%.1f g", liveWeight),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalorieKoOrange,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                            Text(
                                "Place an item with a known weight (like a calibration weight or a specific coin) on the scale.",
                                color = Color(0xFF4B5563),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (showCalibrationBanner) {
                                // Show a temporary success nudge if we just calibrated
                                Text(
                                    "✨ Calibration Successful! Check the reading now.",
                                    color = CalorieKoGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            Text(
                                "If the reading is incorrect, tap \"Recalibrate Scale\" below.",
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        CalibrationStep.INPUT_TRUTH_VALUE -> {
                            Text(
                                "What is the actual weight of the item currently on the scale?",
                                color = Color(0xFF4B5563),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { newValue ->
                                    // Allow digits and a single decimal point
                                    if (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1) {
                                        weightInput = newValue
                                    }
                                },
                                label = { Text("Actual Weight (g)") },
                                placeholder = { Text("e.g. 50.0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { }),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CalorieKoOrange,
                                    focusedLabelColor = CalorieKoOrange,
                                    cursorColor = CalorieKoOrange
                                )
                            )
                        }
                        CalibrationStep.CALIBRATING -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                color = CalorieKoOrange,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Applying calibration...\nPlease wait.",
                                color = Color(0xFF4B5563),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                when (calibrationStep) {
                    CalibrationStep.TEST_ACCURACY -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { bleScaleManager?.sendTareCommand() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CalorieKoOrange)
                            ) {
                                Text("Zero Scale", color = CalorieKoOrange, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    calibrationStep = CalibrationStep.INPUT_TRUTH_VALUE
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Recalibrate", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    CalibrationStep.INPUT_TRUTH_VALUE -> {
                        Button(
                            onClick = {
                                val weight = weightInput.toFloatOrNull()
                                if (weight != null && weight > 0) {
                                    calibrationStep = CalibrationStep.CALIBRATING
                                    bleScaleManager?.sendCalibrateCommand(weight)
                                } else {
                                    showBanner(
                                        NotificationType.WARNING,
                                        "Invalid Weight",
                                        "Enter a valid weight in grams"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                            enabled = weightInput.isNotBlank()
                        ) {
                            Text("Calibrate", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    CalibrationStep.CALIBRATING -> {}
                }
            },
            dismissButton = {
                // Only show Cancel when not in a loading state
                if (calibrationStep != CalibrationStep.CALIBRATING) {
                    TextButton(onClick = {
                        showCalibrationDialog = false
                        calibrationStep = CalibrationStep.TEST_ACCURACY
                        weightInput = ""
                    }) {
                        Text(if (calibrationStep == CalibrationStep.TEST_ACCURACY) "Done" else "Cancel", color = Color(0xFF6B7280))
                    }
                }
            },
            containerColor = Color.White,
            properties = DialogProperties(
                dismissOnBackPress = calibrationStep != CalibrationStep.CALIBRATING,
                dismissOnClickOutside = calibrationStep != CalibrationStep.CALIBRATING
            )
        )
    }

    // --- PASSWORD RESET CONFIRMATION DIALOG ---
    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = {
                Text(
                    "Change Password",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            },
            text = {
                Text(
                    "For your security, we will send a password reset link to your registered email. Do you want to proceed?",
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordResetDialog = false
                        viewModel.sendPasswordResetEmail()
                    }
                ) {
                    Text("Proceed", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White
        )
    }
}

// Reusable Settings Row Component
@Composable
fun SettingsRow(
    icon: ImageVector, title: String, subtitle: String, iconColor: Color,
    textColor: Color = Color(0xFF1F2937), showArrow: Boolean = true, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
        }
        if (showArrow) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Navigate", tint = Color(0xFFD1D5DB), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SettingsDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)).padding(horizontal = 20.dp))
}

/**
 * Premium animated notification banner that slides in from the top.
 *
 * Supports 4 types (SUCCESS, ERROR, WARNING, INFO) each with a curated
 * gradient palette, matching icon, and smooth spring animation.
 * Includes a dismiss button for manual close.
 */
@Composable
private fun NotificationBanner(
    visible: Boolean,
    type: NotificationType,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (gradient, icon) = when (type) {
        NotificationType.SUCCESS -> Pair(
            listOf(Color(0xFF10B981), Color(0xFF059669)),
            Icons.Default.CheckCircle
        )
        NotificationType.ERROR -> Pair(
            listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
            Icons.Default.Error
        )
        NotificationType.WARNING -> Pair(
            listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            Icons.Default.Warning
        )
        NotificationType.INFO -> Pair(
            listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),
            Icons.Default.Info
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it - 60 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 60 },
            animationSpec = tween(400)
        ) + fadeOut(tween(300)),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(gradient))
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = type.name,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // Text content
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (message.isNotBlank()) {
                            Text(
                                text = message,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    // Dismiss button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
