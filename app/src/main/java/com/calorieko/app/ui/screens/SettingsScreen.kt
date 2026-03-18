package com.calorieko.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    bleScaleManager: BleScaleManager? = null // Brought back to handle scale options
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var isWipingData by remember { mutableStateOf(false) }

    // State for the cool notification banner
    var showSuccessBanner by remember { mutableStateOf(false) }

    // Initialize Database Access
    val db = remember { AppDatabase.getDatabase(context, scope) }

    // Auto-hide the success banner after 3 seconds
    LaunchedEffect(showSuccessBanner) {
        if (showSuccessBanner) {
            delay(3000)
            showSuccessBanner = false
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
                        SettingsRow(icon = Icons.Default.Lock, title = "Change Password", subtitle = "Update your security credentials", iconColor = Color(0xFF8B5CF6), onClick = { Toast.makeText(context, "Password settings coming soon", Toast.LENGTH_SHORT).show() })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- PREFERENCES SECTION ---
                Text("Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(icon = Icons.Default.Notifications, title = "Notifications", subtitle = "Reminders and meal alerts", iconColor = CalorieKoOrange, onClick = { Toast.makeText(context, "Notifications coming soon", Toast.LENGTH_SHORT).show() })
                        SettingsDivider()
                        SettingsRow(icon = Icons.Default.Sync, title = "Sync Data", subtitle = "Manually backup to cloud", iconColor = CalorieKoGreen, onClick = { Toast.makeText(context, "Syncing data...", Toast.LENGTH_SHORT).show() })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SMART SCALE SECTION ---
                Text("Smart Scale", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(icon = Icons.Default.Bluetooth, title = "Pairing Status", subtitle = "Manage Bluetooth scale", iconColor = Color(0xFF3B82F6), onClick = { onNavigate("scalePairing/settings") })
                        SettingsDivider()
                        SettingsRow(icon = Icons.Default.BatteryStd, title = "Battery Level", subtitle = "Check scale battery", iconColor = CalorieKoGreen, onClick = { Toast.makeText(context, "Scale Battery: Good", Toast.LENGTH_SHORT).show() })
                        SettingsDivider()
                        SettingsRow(icon = Icons.Default.MonitorWeight, title = "Recalibrate Scale", subtitle = "Tare the scale to zero", iconColor = CalorieKoOrange, onClick = { Toast.makeText(context, "Scale recalibrated to 0.0g", Toast.LENGTH_SHORT).show() })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- ABOUT & LEGAL SECTION ---
                Text("About", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(icon = Icons.Default.PrivacyTip, title = "Privacy Notice", subtitle = "Read our data policies and terms", iconColor = Color(0xFF6B7280), onClick = { Toast.makeText(context, "Opening Privacy Notice...", Toast.LENGTH_SHORT).show() })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- DANGER ZONE ---
                Text("Danger Zone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.DeleteForever, title = "Wipe Local Data", subtitle = "Clear all activities and meals from this device",
                            iconColor = Color(0xFFEF4444), textColor = Color(0xFFEF4444), showArrow = false,
                            onClick = { showWipeDialog = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Logout, title = "Logout", subtitle = "Sign out of your account",
                            iconColor = Color(0xFF6B7280), showArrow = false,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp)) // Extra padding at bottom
            }

            // --- MODERN ANIMATED SUCCESS BANNER ---
            AnimatedVisibility(
                visible = showSuccessBanner,
                enter = slideInVertically(initialOffsetY = { -it - 50 }, animationSpec = tween(500)) + fadeIn(tween(500)),
                exit = slideOutVertically(targetOffsetY = { -it - 50 }, animationSpec = tween(500)) + fadeOut(tween(500)),
                modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Data Wiped Successfully", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Your local device storage is clean.", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- WIPE LOCAL DATA DIALOG ---
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isWipingData) showWipeDialog = false },
            title = { Text("Wipe All Local Data?", fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)) },
            text = { Text("This will permanently delete all unsynced meals, workouts, and settings from this device. Are you sure?", color = Color(0xFF4B5563)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isWipingData = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                // Physically Wipe the Room Database Tables
                                db.clearAllTables()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            withContext(Dispatchers.Main) {
                                isWipingData = false
                                showWipeDialog = false
                                showSuccessBanner = true // Trigger the cool animation
                            }
                        }
                    },
                    enabled = !isWipingData
                ) {
                    Text(if (isWipingData) "Wiping..." else "Yes, Wipe Data", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }, enabled = !isWipingData) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White,
            properties = DialogProperties(dismissOnBackPress = !isWipingData, dismissOnClickOutside = !isWipingData)
        )
    }

    // --- LOGOUT DIALOG ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)) },
            text = { Text("Are you sure you want to log out of your account?", color = Color(0xFF4B5563)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            auth.signOut()
                            onLogout()
                        }
                    }
                ) {
                    Text("Logout", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
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