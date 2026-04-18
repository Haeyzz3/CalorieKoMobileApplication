package com.calorieko.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.LogMealPhase
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.ml.CalorieKoClassifier
import com.calorieko.app.ui.components.CameraPreview
import com.calorieko.app.ui.components.ExpandableNutrientGrid
import com.calorieko.app.ui.components.NutrientChip
import com.calorieko.app.ui.components.NutrientDisclaimerDialog
import com.calorieko.app.ui.components.NutrientDisclaimerIconButton
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.calorieko.app.viewmodel.LogMealEvent
import com.calorieko.app.viewmodel.LogMealViewModel
import com.calorieko.app.viewmodel.ManualLogEvent
import com.calorieko.app.viewmodel.ManualLogViewModel
import kotlin.math.roundToInt

// ───────────────────────────────────────────────────────────────
// Mode Selection Content
// ───────────────────────────────────────────────────────────────

@Composable
private fun MealModeSelectionContent(
    onSelectAI: () -> Unit,
    onSelectManual: () -> Unit,
    onBack: () -> Unit
) {

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Meal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Text(
                "How would you like to log your meal?",
                color = Color(0xFF6B7280),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Card 1 — AI + Smart Scale
            MealModeCard(
                icon = Icons.Default.CameraAlt,
                secondaryIcon = Icons.Default.MonitorWeight,
                title = "AI + Smart Scale",
                description = "Point your camera at the dish and let AI identify it. Weight is read automatically from your connected scale.",
                tags = listOf("AI Recognition", "Auto-Weigh"),
                accentColor = CalorieKoGreen,
                accentBgColor = Color(0xFFDCFCE7),
                onClick = onSelectAI
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2 — Manual Entry
            MealModeCard(
                icon = Icons.Default.Edit,
                secondaryIcon = null,
                title = "Manual Entry",
                description = "Search for a dish from the supported list and enter the weight yourself. Perfect when your scale is unavailable.",
                tags = listOf("No Scale Needed", "Quick Log"),
                accentColor = CalorieKoOrange,
                accentBgColor = Color(0xFFFFF7ED),
                onClick = onSelectManual
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Info tip
            Surface(
                color = Color(0xFFF0F9FF),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Full nutrition tracking",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            "Both methods log all 17 nutrients and sync with your daily intake automatically.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealModeCard(
    icon: ImageVector,
    secondaryIcon: ImageVector?,
    title: String,
    description: String,
    tags: List<String>,
    accentColor: Color,
    accentBgColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (secondaryIcon != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
                        Icon(secondaryIcon, null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                } else {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(vertical = 4.dp),
                    lineHeight = 18.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(50)) {
                            Text(
                                tag,
                                fontSize = 11.sp,
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Entry Point — Routes between Mode Selection, AI, and Manual
// ───────────────────────────────────────────────────────────────

@Composable
fun LogMealScreenWithManual(
    viewModel: LogMealViewModel,
    manualLogViewModel: ManualLogViewModel,
    bleScaleManager: BleScaleManager,
    onBack: () -> Unit,
    onMealConfirmed: () -> Unit,
    onNavigateToPairing: () -> Unit = {}
) {
    val context = LocalContext.current
    val phase by viewModel.phase.collectAsState()
    var isManualMode by remember { mutableStateOf(false) }

    // Classifier hoisted here so it survives phase changes (MODE_SELECTION <-> SCANNING).
    // Created once per navigation to logMeal, not on every camera open/close cycle.
    val classifier = remember { CalorieKoClassifier(context) }
    DisposableEffect(Unit) { onDispose { classifier.close() } }

    // Listen for AI flow one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LogMealEvent.MealConfirmed -> onMealConfirmed()
            }
        }
    }

    // Listen for manual flow one-shot events
    LaunchedEffect(Unit) {
        manualLogViewModel.events.collect { event ->
            when (event) {
                ManualLogEvent.MealConfirmed -> onMealConfirmed()
            }
        }
    }

    if (isManualMode) {
        ManualMealContent(
            viewModel = manualLogViewModel,
            onBack = { isManualMode = false },
            onMealConfirmed = onMealConfirmed
        )
        return
    }

    when (phase) {
        LogMealPhase.MODE_SELECTION -> {
            MealModeSelectionContent(
                onSelectAI = { viewModel.setPhase(LogMealPhase.SCANNING) },
                onSelectManual = { isManualMode = true },
                onBack = onBack
            )
        }
        LogMealPhase.SCANNING, LogMealPhase.DISH_READY, LogMealPhase.MEAL_SUMMARY -> {
            AiScaleMealContent(
                viewModel = viewModel,
                classifier = classifier,
                bleScaleManager = bleScaleManager,
                onBack = { viewModel.setPhase(LogMealPhase.MODE_SELECTION) },
                onMealConfirmed = onMealConfirmed,
                onNavigateToPairing = onNavigateToPairing
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────
// AI + Scale Meal Content (extracted from original LogMealScreen)
// ───────────────────────────────────────────────────────────────

@Composable
private fun AiScaleMealContent(
    viewModel: LogMealViewModel,
    classifier: CalorieKoClassifier,
    bleScaleManager: BleScaleManager,
    onBack: () -> Unit,
    onMealConfirmed: () -> Unit,
    onNavigateToPairing: () -> Unit = {}
) {
    val context = LocalContext.current

    // Collect one-shot navigation events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LogMealEvent.MealConfirmed -> onMealConfirmed()
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val showSettingsDialog = remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }

    // Observers
    val phase by viewModel.phase.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val weightStable by viewModel.weightStable.collectAsState()
    val latestResults by viewModel.latestResults.collectAsState()
    val topLabel by viewModel.topLabel.collectAsState()
    val topConfidence by viewModel.topConfidence.collectAsState()
    val currentDetectedFood by viewModel.currentDetectedFood.collectAsState()
    val showUnsupportedBanner by viewModel.showUnsupportedBanner.collectAsState()
    val showLogFailedBanner by viewModel.showLogFailedBanner.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val showCandidateSelection by viewModel.showCandidateSelection.collectAsState()
    val candidate1 by viewModel.candidate1.collectAsState()
    val candidate2 by viewModel.candidate2.collectAsState()
    val pendingDishName by viewModel.pendingDishName.collectAsState()
    val pendingConfidence by viewModel.pendingConfidence.collectAsState()
    val pendingCaloriesEst by viewModel.pendingCaloriesEst.collectAsState()
    val loggedDishes by viewModel.loggedDishes.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val isScaleConnected by viewModel.isScaleConnected.collectAsState()

    val connectionState by bleScaleManager.connectionState.collectAsState()
    LaunchedEffect(connectionState) {
        viewModel.updateScaleConnectionStatus(connectionState is com.calorieko.app.ble.BleConnectionState.Connected)
    }

    val realWeight by bleScaleManager.liveWeight.collectAsState()
    LaunchedEffect(realWeight) {
        viewModel.updateRealWeight(realWeight)
    }

    // ── Permission denied fallback ──
    if (!hasCameraPermission) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF111827)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera permission is required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Grant permission to use meal logging", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val activity = context as? Activity
                        val shouldShowRationale = activity?.let {
                            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                        } ?: false

                        if (shouldShowRationale) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            showSettingsDialog.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Grant Permission") }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Go Back") }
            }
        }

        if (showSettingsDialog.value) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog.value = false },
                title = { Text(text = "Permission Required") },
                text = { Text(text = "Camera permission is permanently denied. Please grant the permission in the app settings.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSettingsDialog.value = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Go to Settings")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSettingsDialog.value = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        return
    }

    // ── MEAL_SUMMARY overlay ──
    if (phase == LogMealPhase.MEAL_SUMMARY) {
        val isConfirming by viewModel.isConfirming.collectAsState()
        MealSummaryOverlay(
            dishes = loggedDishes,
            mealType = mealType,
            onMealTypeChange = { viewModel.updateMealType(it) },
            onRemoveDish = { viewModel.removeDish(it) },
            onAddMore = { viewModel.setPhase(LogMealPhase.SCANNING) },
            onConfirmMeal = { viewModel.confirmMeal() },
            onCancel = onBack,
            isConfirming = isConfirming
        )
        return
    }

    // ── Main camera UI ──
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. Live camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            classifier = classifier,
            flashEnabled = flashEnabled,
            onFrameAnalyzed = { results ->
                viewModel.onFrameAnalyzed(results)
            }
        )

        // 2. Top controls (Close + Flash)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) { Icon(Icons.Default.Close, null, tint = Color.White) }

            IconButton(
                onClick = { flashEnabled = !flashEnabled },
                modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    null,
                    tint = if (flashEnabled) Color(0xFFFFD700) else Color.White
                )
            }
        }

        // 3. Real-time data badges (Weight + AI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Weight badge
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(50),
                shadowElevation = 8.dp,
                modifier = Modifier.clickable { onNavigateToPairing() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).background(
                            if (!isScaleConnected) Color.Red
                            else if (weightStable && weight > 0) CalorieKoGreen 
                            else if (weight > 0) CalorieKoOrange 
                            else Color.Gray,
                            CircleShape
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isScaleConnected) {
                        Text("Weight: ", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                        Text(
                            "${weight.roundToInt()}g",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (weightStable) CalorieKoGreen else CalorieKoOrange
                        )
                    } else {
                        Text(
                            "Disconnected",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }

            // AI badge
            val displayLabel = currentDetectedFood?.nameEn ?: topLabel
            val confPercent = (topConfidence * 100).toInt()
            val aiReady = currentDetectedFood != null && topConfidence >= LogMealViewModel.CONFIDENCE_THRESHOLD
            val aiBadgeColor = when {
                topLabel.isEmpty()            -> Color.White.copy(alpha = 0.95f)
                aiReady                       -> CalorieKoGreen.copy(alpha = 0.95f)
                topConfidence > 0.3f          -> CalorieKoOrange.copy(alpha = 0.95f)
                else                          -> Color.White.copy(alpha = 0.95f)
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
                    if (topLabel.isEmpty()) {
                        Text("Analyzing...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                    } else {
                        val textColor = if (aiReady || topConfidence > 0.3f) Color.White else Color(0xFF6B7280)
                        Text(
                            "$displayLabel ${confPercent}%",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor
                        )
                    }
                }
            }
        }


        // 5. Framing guide
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 50.dp)
                .width(280.dp)
                .aspectRatio(4f / 3f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val borderColor = if (weightStable && weight > 0) CalorieKoGreen else Color.White.copy(alpha = 0.5f)
                drawRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )
                )
                val cornerLen = 20.dp.toPx()
                val stroke = 4.dp.toPx()
                drawLine(Color.White, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
                drawLine(Color.White, Offset(0f, 0f), Offset(0f, cornerLen), stroke)
                drawLine(Color.White, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), stroke)
                drawLine(Color.White, Offset(size.width, 0f), Offset(size.width, cornerLen), stroke)
                drawLine(Color.White, Offset(0f, size.height), Offset(cornerLen, size.height), stroke)
                drawLine(Color.White, Offset(0f, size.height), Offset(0f, size.height - cornerLen), stroke)
                drawLine(Color.White, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), stroke)
                drawLine(Color.White, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), stroke)
            }

            Text(
                text = when {
                    weightStable && weight > 0 -> "✓ Scale detected"
                    weight > 0                 -> "Stabilizing weight..."
                    else                       -> "Place food container on the Smart Scale"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(top = 16.dp).offset(y = 30.dp)
            )
        }

        // 6. Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(24.dp)
        ) {
            // Logged dishes counter + Review button
            if (loggedDishes.isNotEmpty()) {
                Surface(
                    color = CalorieKoGreen.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.setPhase(LogMealPhase.MEAL_SUMMARY) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Restaurant, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${loggedDishes.size} dish${if (loggedDishes.size > 1) "es" else ""} logged — Tap to review",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Capture Button (while scanning)
            if (phase == LogMealPhase.SCANNING) {
                val isReady = weightStable && weight > 0 && latestResults.isNotEmpty()
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable(enabled = isReady && !isProcessing) { viewModel.processCapture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(if (isReady && !isProcessing) Color.White else Color.Gray, CircleShape)
                        )
                    }
                }
                
                val statusText = when {
                    isProcessing -> "Processing..."
                    !weightStable && weight == 0f -> "Waiting for scale data..."
                    !weightStable -> "Stabilizing weight..."
                    else -> "Ready to capture"
                }
                Text(
                    statusText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        // 7. DISH_READY bottom sheet
        if (phase == LogMealPhase.DISH_READY) {
            DishReadySheet(
                dishName = pendingDishName,
                confidence = pendingConfidence,
                weight = weight,
                estimatedCalories = pendingCaloriesEst,
                onLogDish = { viewModel.logCurrentDish() },
                onCancel = { viewModel.cancelDishReady() }
            )
        }

        // 8. Candidate Selection UI
        if (showCandidateSelection && candidate1 != null && candidate2 != null) {
            CandidateSelectionSheet(
                candidate1 = candidate1!!,
                candidate2 = candidate2!!,
                onSelect = { food, conf -> viewModel.onCandidateSelected(food, conf) },
                onCancel = { viewModel.cancelCandidateSelection() }
            )
        }

        // 9. Inline unsupported-dish banner (replaces the old blocking ErrorOverlay)
        AnimatedVisibility(
            visible = showUnsupportedBanner,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.hideUnsupportedBanner() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Dish not recognized — try a supported Filipino dish",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 10. Inline log-failed banner
        AnimatedVisibility(
            visible = showLogFailedBanner,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.hideLogFailedBanner() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Failed to log dish — please try again",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Manual Meal Content
// ───────────────────────────────────────────────────────────────

@Composable
private fun ManualMealContent(
    viewModel: ManualLogViewModel,
    onBack: () -> Unit,
    onMealConfirmed: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredDishes by viewModel.filteredDishes.collectAsState()
    val selectedDish by viewModel.selectedDish.collectAsState()
    val manualWeightText by viewModel.manualWeightText.collectAsState()
    val loggedDishes by viewModel.loggedDishes.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val showSummary by viewModel.showSummary.collectAsState()

    val isConfirming by viewModel.isConfirming.collectAsState()

    // ── Meal Summary overlay ──
    if (showSummary) {
        ManualMealSummaryOverlay(
            dishes = loggedDishes,
            mealType = mealType,
            onMealTypeChange = { viewModel.updateMealType(it) },
            onRemoveDish = { viewModel.removeDish(it) },
            onAddMore = { viewModel.setShowSummary(false) },
            onConfirmMeal = { viewModel.confirmMeal() },
            onCancel = onBack,
            isConfirming = isConfirming
        )
        return
    }

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedDish != null) {
                            viewModel.clearSelectedDish()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedDish != null) "Enter Weight" else "Manual Entry",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (selectedDish == null) {
                // ── Phase 1: Dish Selection ──
                DishSelectionContent(
                    searchQuery = searchQuery,
                    filteredDishes = filteredDishes,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onSelectDish = { viewModel.selectDish(it) }
                )
            } else {
                // ── Phase 2: Weight Input ──
                WeightInputContent(
                    dish = selectedDish!!,
                    weightText = manualWeightText,
                    onWeightChange = { viewModel.updateWeightText(it) },
                    onChangeDish = { viewModel.clearSelectedDish() },
                    onAddDish = { viewModel.addDish() }
                )
            }

            // ── Floating logged dishes counter (only on dish selection, not weight input) ──
            if (loggedDishes.isNotEmpty() && selectedDish == null) {
                Surface(
                    color = CalorieKoGreen,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.setShowSummary(true) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Restaurant, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${loggedDishes.size} dish${if (loggedDishes.size > 1) "es" else ""} logged — Tap to review",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DishSelectionContent(
    searchQuery: String,
    filteredDishes: List<FoodItem>,
    onSearchChange: (String) -> Unit,
    onSelectDish: (FoodItem) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search dishes (e.g. Adobo, Sinigang)") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = CalorieKoGreen
            ),
            singleLine = true
        )

        // Dish list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDishes, key = { it.foodId }) { dish ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDish(dish) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                dish.nameEn,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                            if (dish.namePh != dish.nameEn && dish.namePh.isNotBlank()) {
                                Text(
                                    dish.namePh,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(50)) {
                                Text(
                                    dish.category,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${dish.caloriesPer100g.fmt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalorieKoGreen
                            )
                            Text(
                                "kcal/100g",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            // Extra bottom padding for the floating pill
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun WeightInputContent(
    dish: FoodItem,
    weightText: String,
    onWeightChange: (String) -> Unit,
    onChangeDish: () -> Unit,
    onAddDish: () -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val parsedWeight = weightText.toFloatOrNull() ?: 0f
    val estimatedCalories = if (parsedWeight > 0f) dish.caloriesPer100g * parsedWeight / 100f else 0f
    val isValid = parsedWeight > 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Selected dish card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            dish.nameEn,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        if (dish.namePh != dish.nameEn && dish.namePh.isNotBlank()) {
                            Text(
                                dish.namePh,
                                fontSize = 13.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                    TextButton(onClick = onChangeDish) {
                        Text("Change", color = CalorieKoOrange, maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(50)) {
                        Text(
                            dish.category,
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                        Text(
                            "${dish.caloriesPer100g.fmt()} kcal/100g",
                            fontSize = 11.sp,
                            color = CalorieKoGreen,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weight input
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Weight (grams)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = onWeightChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    placeholder = { Text("e.g. 250") },
                    leadingIcon = {
                        Icon(Icons.Default.MonitorWeight, null, tint = Color.Gray)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live calorie estimate
        if (isValid) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(CalorieKoGreen, Color(0xFF16A34A))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Estimated Calories",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "≈ ${estimatedCalories.toInt()}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "kcal  •  ${parsedWeight.toInt()}g of ${dish.nameEn}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Add Dish button
        Button(
            onClick = onAddDish,
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = CalorieKoOrange,
                disabledContainerColor = Color(0xFFD1D5DB)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Add Dish",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Manual Meal Summary Overlay
// ───────────────────────────────────────────────────────────────

@Composable
private fun ManualMealSummaryOverlay(
    dishes: List<LoggedDish>,
    mealType: String,
    onMealTypeChange: (String) -> Unit,
    onRemoveDish: (Int) -> Unit,
    onAddMore: () -> Unit,
    onConfirmMeal: () -> Unit,
    onCancel: () -> Unit,
    isConfirming: Boolean = false
) {
    val totalCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = dishes.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = dishes.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = dishes.sumOf { it.fat.toDouble() }.toFloat()

    // Track expanded state per dish (by index)
    val expandedDishes = remember { mutableStateMapOf<Int, Boolean>() }
    var totalsExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, null)
                    }
                    Text("Meal Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    // Placeholder for symmetry
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Meal type selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                    val selected = mealType == type
                    Surface(
                        onClick = { onMealTypeChange(type) },
                        color = if (selected) CalorieKoGreen else Color.White,
                        shape = RoundedCornerShape(50),
                        shadowElevation = if (selected) 0.dp else 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            type,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Dish list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(dishes) { index, dish ->
                    val isExpanded = expandedDishes[index] == true
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dish.dishNameEn, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${dish.weightGrams.roundToInt()}g  •  ~${dish.calories.fmt()} kcal",
                                        fontSize = 13.sp, color = Color(0xFF6B7280)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "P: ~${dish.protein.fmt()}g  C: ~${dish.carbs.fmt()}g  F: ~${dish.fat.fmt()}g",
                                        fontSize = 12.sp, color = Color(0xFF9CA3AF)
                                    )
                                }
                                IconButton(onClick = { expandedDishes[index] = !isExpanded }) {
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onRemoveDish(index) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            }

                            // Expandable full nutrition details
                            if (isExpanded) {
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                ExpandableNutrientGrid(
                                    fiber = dish.fiber,
                                    sugar = dish.sugar,
                                    sodium = dish.sodium,
                                    potassium = dish.potassium,
                                    vitaminA = dish.vitaminA,
                                    vitaminC = dish.vitaminC,
                                    calcium = dish.calcium,
                                    iron = dish.iron
                                )
                            }
                        }
                    }
                }

                // Totals card
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CalorieKoGreen.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var showDisclaimer by remember { mutableStateOf(false) }
                            if (showDisclaimer) {
                                NutrientDisclaimerDialog(onDismiss = { showDisclaimer = false })
                            }
                            Text("Estimated Meal Totals", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1F2937))
                            Spacer(modifier = Modifier.width(4.dp))
                            NutrientDisclaimerIconButton(onClick = { showDisclaimer = true })
                        }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = CalorieKoGreen.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Calories", fontSize = 14.sp, color = Color(0xFF374151))
                                Text("≈${totalCalories.fmt()} kcal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CalorieKoGreen)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                NutrientChip("Protein", "${totalProtein.fmt()}g")
                                NutrientChip("Carbs", "${totalCarbs.fmt()}g")
                                NutrientChip("Fat", "${totalFat.fmt()}g")
                            }

                            Spacer(Modifier.height(8.dp))

                            // Expand/collapse toggle for full totals
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { totalsExpanded = !totalsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (totalsExpanded) "Hide Full Breakdown" else "View Full Breakdown",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CalorieKoGreen
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    if (totalsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = CalorieKoGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (totalsExpanded) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = CalorieKoGreen.copy(alpha = 0.3f))
                                ExpandableNutrientGrid(
                                    fiber = dishes.sumOf { it.fiber.toDouble() }.toFloat(),
                                    sugar = dishes.sumOf { it.sugar.toDouble() }.toFloat(),
                                    sodium = dishes.sumOf { it.sodium.toDouble() }.toFloat(),
                                    potassium = dishes.sumOf { it.potassium.toDouble() }.toFloat(),
                                    vitaminA = dishes.sumOf { it.vitaminA.toDouble() }.toFloat(),
                                    vitaminC = dishes.sumOf { it.vitaminC.toDouble() }.toFloat(),
                                    calcium = dishes.sumOf { it.calcium.toDouble() }.toFloat(),
                                    iron = dishes.sumOf { it.iron.toDouble() }.toFloat()
                                )
                            }
                        }
                    }
                }
            }

            // Bottom buttons
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = onConfirmMeal,
                        enabled = dishes.isNotEmpty() && !isConfirming,
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen, disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isConfirming) "Saving..." else "Confirm Meal",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddMore,
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add More Dishes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Candidate Selection Bottom Sheet
// ───────────────────────────────────────────────────────────────

@Composable
private fun CandidateSelectionSheet(
    candidate1: Pair<com.calorieko.app.data.model.FoodItem, Float>,
    candidate2: Pair<com.calorieko.app.data.model.FoodItem, Float>,
    onSelect: (com.calorieko.app.data.model.FoodItem, Float) -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(enabled = false) {}
        )

        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(50))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "Which dish is this?",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The AI is not absolutely certain. Please select the correct dish from the top 2 matches.",
                    fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onSelect(candidate1.first, candidate1.second) },
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("${candidate1.first.nameEn} (${(candidate1.second * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSelect(candidate2.first, candidate2.second) },
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("${candidate2.first.nameEn} (${(candidate2.second * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Try Again") }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// DISH_READY Bottom Sheet
// ───────────────────────────────────────────────────────────────

@Composable
private fun DishReadySheet(
    dishName: String,
    confidence: Float,
    weight: Float,
    estimatedCalories: Float,
    onLogDish: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Semi-transparent backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(enabled = false) {}
        )

        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Pill indicator
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(50))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                // Success icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFDCFCE7), CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "Dish Recognized!",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                // Info rows
                InfoRow("Dish", dishName)
                InfoRow("Confidence", "${(confidence * 100).toInt()}%")
                InfoRow("Weight", "${weight.roundToInt()}g")
                InfoRow("Est. Calories", "≈${estimatedCalories.toInt()} kcal")

                Spacer(Modifier.height(24.dp))

                // Buttons
                Button(
                    onClick = onLogDish,
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log This Dish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF6B7280))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
    }
}

// ───────────────────────────────────────────────────────────────
// Meal Summary Overlay (AI flow — original)
// ───────────────────────────────────────────────────────────────

@Composable
private fun MealSummaryOverlay(
    dishes: List<LoggedDish>,
    mealType: String,
    onMealTypeChange: (String) -> Unit,
    onRemoveDish: (Int) -> Unit,
    onAddMore: () -> Unit,
    onConfirmMeal: () -> Unit,
    onCancel: () -> Unit,
    isConfirming: Boolean = false
) {
    val totalCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = dishes.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = dishes.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = dishes.sumOf { it.fat.toDouble() }.toFloat()

    // Track expanded state per dish (by index)
    val expandedDishes = remember { mutableStateMapOf<Int, Boolean>() }
    var totalsExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, null)
                    }
                    Text("Meal Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    // Placeholder for symmetry
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Meal type selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                    val selected = mealType == type
                    Surface(
                        onClick = { onMealTypeChange(type) },
                        color = if (selected) CalorieKoGreen else Color.White,
                        shape = RoundedCornerShape(50),
                        shadowElevation = if (selected) 0.dp else 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            type,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Dish list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(dishes) { index, dish ->
                    val isExpanded = expandedDishes[index] == true
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dish.dishNameEn, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${dish.weightGrams.roundToInt()}g  •  ~${dish.calories.fmt()} kcal",
                                        fontSize = 13.sp, color = Color(0xFF6B7280)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "P: ~${dish.protein.fmt()}g  C: ~${dish.carbs.fmt()}g  F: ~${dish.fat.fmt()}g",
                                        fontSize = 12.sp, color = Color(0xFF9CA3AF)
                                    )
                                }
                                IconButton(onClick = { expandedDishes[index] = !isExpanded }) {
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onRemoveDish(index) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            }

                            // Expandable full nutrition details
                            if (isExpanded) {
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                ExpandableNutrientGrid(
                                    fiber = dish.fiber,
                                    sugar = dish.sugar,
                                    sodium = dish.sodium,
                                    potassium = dish.potassium,
                                    vitaminA = dish.vitaminA,
                                    vitaminC = dish.vitaminC,
                                    calcium = dish.calcium,
                                    iron = dish.iron
                                )
                            }
                        }
                    }
                }

                // Totals card
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CalorieKoGreen.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var showDisclaimer by remember { mutableStateOf(false) }
                            if (showDisclaimer) {
                                NutrientDisclaimerDialog(onDismiss = { showDisclaimer = false })
                            }
                            Text("Estimated Meal Totals", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1F2937))
                            Spacer(modifier = Modifier.width(4.dp))
                            NutrientDisclaimerIconButton(onClick = { showDisclaimer = true })
                        }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = CalorieKoGreen.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Calories", fontSize = 14.sp, color = Color(0xFF374151))
                                Text("≈${totalCalories.fmt()} kcal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CalorieKoGreen)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                NutrientChip("Protein", "${totalProtein.fmt()}g")
                                NutrientChip("Carbs", "${totalCarbs.fmt()}g")
                                NutrientChip("Fat", "${totalFat.fmt()}g")
                            }

                            Spacer(Modifier.height(8.dp))

                            // Expand/collapse toggle for full totals
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { totalsExpanded = !totalsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (totalsExpanded) "Hide Full Breakdown" else "View Full Breakdown",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CalorieKoGreen
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    if (totalsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = CalorieKoGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (totalsExpanded) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = CalorieKoGreen.copy(alpha = 0.3f))
                                ExpandableNutrientGrid(
                                    fiber = dishes.sumOf { it.fiber.toDouble() }.toFloat(),
                                    sugar = dishes.sumOf { it.sugar.toDouble() }.toFloat(),
                                    sodium = dishes.sumOf { it.sodium.toDouble() }.toFloat(),
                                    potassium = dishes.sumOf { it.potassium.toDouble() }.toFloat(),
                                    vitaminA = dishes.sumOf { it.vitaminA.toDouble() }.toFloat(),
                                    vitaminC = dishes.sumOf { it.vitaminC.toDouble() }.toFloat(),
                                    calcium = dishes.sumOf { it.calcium.toDouble() }.toFloat(),
                                    iron = dishes.sumOf { it.iron.toDouble() }.toFloat()
                                )
                            }
                        }
                    }
                }
            }

            // Bottom buttons
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = onConfirmMeal,
                        enabled = dishes.isNotEmpty() && !isConfirming,
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen, disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isConfirming) "Saving..." else "Confirm Meal",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddMore,
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add More Dishes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// NutrientChip and ExpandableNutrientGrid are now imported from
// com.calorieko.app.ui.components.NutrientComponents




private fun Float.fmt() = String.format(java.util.Locale.US, "%.1f", this)