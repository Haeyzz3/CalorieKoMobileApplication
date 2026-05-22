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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.style.TextDecoration

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.LogMealPhase
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.local.IngredientNutritionBreakdown
import com.calorieko.app.data.model.RawIngredientEntity
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
import com.calorieko.app.viewmodel.PantryDeductionItem
import com.calorieko.app.viewmodel.PlannedWeightMethod
import com.calorieko.app.viewmodel.canConfirmPlannedQuickLog
import com.calorieko.app.util.PortionScaler
import com.calorieko.app.util.RecipeCustomizationRules
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
                            "Both methods log all 12 nutrients and sync with your daily intake automatically.",
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
    val showPantryDeduction by viewModel.showPantryDeduction.collectAsState()
    val pantryDeductionItems by viewModel.pantryDeductionItems.collectAsState()

    val connectionState by bleScaleManager.connectionState.collectAsState()
    LaunchedEffect(connectionState) {
        viewModel.updateScaleConnectionStatus(connectionState is com.calorieko.app.ble.BleConnectionState.Connected)
    }

    val realWeight by bleScaleManager.liveWeight.collectAsState()
    LaunchedEffect(realWeight) {
        viewModel.updateRealWeight(realWeight)
    }

    if (showPantryDeduction && pantryDeductionItems.isNotEmpty()) {
        PantryDeductionScreen(
            deductionItems = pantryDeductionItems,
            onConfirm = { selectedKeys -> viewModel.confirmPantryDeduction(selectedKeys) },
            onSkip = { viewModel.skipPantryDeduction() }
        )
        return
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
            isConfirming = isConfirming,
            viewModel = viewModel
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
            val displayLabel = currentDetectedFood?.namePh ?: topLabel
            val confPercent = (topConfidence * 100).toInt()
            val aiReady = currentDetectedFood != null && topConfidence >= LogMealViewModel.CONFIDENCE_THRESHOLD
            val aiBadgeColor = when {
                aiReady -> CalorieKoGreen.copy(alpha = 0.95f)
                else    -> CalorieKoOrange.copy(alpha = 0.95f)
            }
            val aiBadgeTextColor = Color.White

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
                        Text("Analyzing...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = aiBadgeTextColor)
                    } else {
                        Text(
                            "$displayLabel ${confPercent}%",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = aiBadgeTextColor
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
    val showPantryDeduction by viewModel.showPantryDeduction.collectAsState()
    val pantryDeductionItems by viewModel.pantryDeductionItems.collectAsState()

    val isConfirming by viewModel.isConfirming.collectAsState()

    if (showPantryDeduction && pantryDeductionItems.isNotEmpty()) {
        PantryDeductionScreen(
            deductionItems = pantryDeductionItems,
            onConfirm = { selectedKeys -> viewModel.confirmPantryDeduction(selectedKeys) },
            onSkip = { viewModel.skipPantryDeduction() }
        )
        return
    }

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
            isConfirming = isConfirming,
            manualViewModel = viewModel
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
    filteredDishes: List<DishRecipeEntity>,
    onSearchChange: (String) -> Unit,
    onSelectDish: (DishRecipeEntity) -> Unit
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
            items(filteredDishes, key = { it.dishLabel }) { dish ->
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
                                dish.namePh,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                            if (dish.nameEn != dish.namePh && dish.nameEn.isNotBlank()) {
                                Text(
                                    dish.nameEn,
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
                                "${dish.calPerServing.fmt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalorieKoGreen
                            )
                            Text(
                                "kcal/serving",
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
    dish: DishRecipeEntity?,
    weightText: String,
    onWeightChange: (String) -> Unit,
    onChangeDish: (() -> Unit)?,
    onAddDish: () -> Unit,
    actionText: String = "Add Dish",
    progressText: String? = null,
    dishLabelFallback: String = "",
    showDefaultServingPrefillHint: Boolean = true
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val scrollState = rememberScrollState()
    val parsedWeight = weightText.toFloatOrNull() ?: 0f
    val estimatedCalories = if (parsedWeight > 0f && dish != null && dish.cookedWeightG > 0f)
        (parsedWeight / dish.cookedWeightG) * dish.calPerServing * dish.servings
    else 0f
    val isValid = parsedWeight > 0f
    val primaryName = dish?.namePh?.ifBlank { dish.nameEn }
        ?: dishLabelFallback.replace("_", " ").replaceFirstChar { it.uppercase() }
    val secondaryName = dish?.nameEn?.takeIf { it.isNotBlank() && it != primaryName }
    val category = dish?.category ?: "Planned meal"
    val caloriesPerServing = dish?.calPerServing ?: 0f
    val defaultServingWeightText = dish?.let { defaultServingWeightText(it) }.orEmpty()
    val showDefaultServingHint = showDefaultServingPrefillHint &&
        defaultServingWeightText.isNotEmpty() &&
        weightText == defaultServingWeightText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Selected dish card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (progressText != null) {
                    Text(
                        progressText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalorieKoGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            primaryName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        if (secondaryName != null) {
                            Text(
                                secondaryName,
                                fontSize = 13.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                    onChangeDish?.let { changeDish ->
                        TextButton(onClick = changeDish) {
                            Text("Change", color = CalorieKoOrange, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(50)) {
                        Text(
                            category,
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (caloriesPerServing > 0f) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                            Text(
                                "${caloriesPerServing.fmt()} kcal/serving",
                                fontSize = 11.sp,
                                color = CalorieKoGreen,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
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
                    "Weight details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { onWeightChange(sanitizeDecimalInput(it)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedTextColor = Color(0xFF1F2937),
                        unfocusedTextColor = Color(0xFF1F2937),
                        cursorColor = CalorieKoGreen
                    ),
                    label = { Text("Weight (grams)") },
                    placeholder = { Text("e.g. 50") },
                    leadingIcon = {
                        Icon(Icons.Default.MonitorWeight, null, tint = Color.Gray)
                    },
                    suffix = { Text("g", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true
                )
                if (showDefaultServingHint) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Default serving prefilled. Edit if your portion is different.",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
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
                        "Estimated for entered weight",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                actionText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Manual Meal Summary Overlay
// ───────────────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ManualMealSummaryOverlay(
    dishes: List<LoggedDish>,
    mealType: String,
    onMealTypeChange: (String) -> Unit,
    onRemoveDish: (Int) -> Unit,
    onAddMore: () -> Unit,
    onConfirmMeal: () -> Unit,
    onCancel: () -> Unit,
    isConfirming: Boolean = false,
    manualViewModel: ManualLogViewModel? = null,
    isPlannedMeal: Boolean = false,
    canConfirmMeal: Boolean = dishes.isNotEmpty(),
    confirmDisabledReason: String? = null
) {
    val totalCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = dishes.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = dishes.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = dishes.sumOf { it.fat.toDouble() }.toFloat()

    // Track expanded state per dish (by index)
    val expandedDishes = remember { mutableStateMapOf<Int, Boolean>() }
    var totalsExpanded by remember { mutableStateOf(false) }

    // ── Ingredient bottom sheet state ──
    val scope = rememberCoroutineScope()
    var ingredientSheetDishIndex by remember { mutableStateOf(-1) }
    var ingredientBreakdown by remember { mutableStateOf<Map<String, com.calorieko.app.data.local.IngredientNutritionBreakdown>?>(null) }
    var activeSubstitutions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeTweaks by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var substitutionTarget by remember { mutableStateOf<String?>(null) }
    var substitutionCandidates by remember { mutableStateOf<List<com.calorieko.app.data.model.RawIngredientEntity>>(emptyList()) }
    var ingredientHasAlternatives by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

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
                    val substitutionsForCount = parseSubstitutionsJson(dish.substitutionsJson)
                    val activeCustomizationCount =
                        substitutionsForCount.size + parseTweaksJson(dish.tweaksJson)
                            .filterKeys { substitutionsForCount[it] != ManualLogViewModel.REMOVED_INGREDIENT }
                            .size
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
                                    Text(dish.dishNamePh.ifBlank { dish.dishNameEn }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${dish.weightGrams.roundToInt()}g  -  ~${dish.calories.fmt()} kcal",
                                        fontSize = 13.sp, color = Color(0xFF6B7280)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "P: ~${dish.protein.fmt()}g  C: ~${dish.carbs.fmt()}g  F: ~${dish.fat.fmt()}g",
                                        fontSize = 12.sp, color = Color(0xFF9CA3AF)
                                    )
                                    if (activeCustomizationCount > 0) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFFF5F3FF),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                "$activeCustomizationCount customization${if (activeCustomizationCount == 1) "" else "s"} active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF6D28D9),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { expandedDishes[index] = !isExpanded }) {
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (!isPlannedMeal) {
                                    IconButton(onClick = { onRemoveDish(index) }) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    }
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

                            // View Ingredients button (only if dishLabel is available and VM is provided)
                            if (manualViewModel != null && dish.dishLabel.isNotEmpty()) {
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            ingredientSheetDishIndex = index
                                            val substitutions = parseSubstitutionsJson(dish.substitutionsJson)
                                            val tweaks = parseTweaksJson(dish.tweaksJson)
                                                .filterKeys { substitutions[it] != ManualLogViewModel.REMOVED_INGREDIENT }
                                            activeSubstitutions = substitutions
                                            activeTweaks = tweaks
                                            ingredientBreakdown = null
                                            ingredientHasAlternatives = emptyMap()
                                            scope.launch {
                                                ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    manualViewModel.getIngredientBreakdown(dish.dishLabel, substitutions)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Restaurant, null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("View Ingredients", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                                }
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
                        enabled = canConfirmMeal && !isConfirming,
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
                    if (confirmDisabledReason != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            confirmDisabledReason,
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isPlannedMeal) {
                        // Informational banner for planned meal restrictions
                        Surface(
                            color = Color(0xFFF0F9FF),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Further adjustments cannot be made to planned meals here. " +
                                        "Please modify your planned meals in the Pantry prior to logging.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    } else {
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

    // ── Ingredient Bottom Sheet ──
    val manualSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (ingredientSheetDishIndex >= 0 && manualViewModel != null) {
        val dish = dishes.getOrNull(ingredientSheetDishIndex)
        if (dish != null) {
            // Determine if this is a planned-meal dish (view-only substitutions)
            val isFromMealPlan = isPlannedMeal

                ModalBottomSheet(
                onDismissRequest = {
                    ingredientSheetDishIndex = -1
                    ingredientBreakdown = null
                    activeTweaks = emptyMap()
                    substitutionTarget = null
                    substitutionCandidates = emptyList()
                    ingredientHasAlternatives = emptyMap()
                },
                sheetState = manualSheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    // Header
                    Text(
                        dish.dishNamePh.ifBlank { dish.dishNameEn },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "${dish.weightGrams.roundToInt()}g cooked portion",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )

                    // Planned dish badge
                    if (isFromMealPlan) {
                        val planCustomizationCount = activeSubstitutions.size + activeTweaks.size
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📅", fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "From Meal Plan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF059669)
                                )
                                if (planCustomizationCount > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "- $planCustomizationCount customization${if (planCustomizationCount == 1) "" else "s"}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Raw ingredient nutritional values (before cooking)",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (activeTweaks.isNotEmpty()) {
                        IngredientTweaksBanner(
                            tweakCount = activeTweaks.size,
                            message = if (isPlannedMeal) {
                                "Saved from Pantry"
                            } else {
                                "Nutrition updated for this logged portion"
                            },
                            onClear = if (isPlannedMeal) null else ({
                                manualViewModel.clearIngredientTweaksFromDish(ingredientSheetDishIndex)
                                activeTweaks = emptyMap()
                            })
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    val loadedIngredientBreakdown = ingredientBreakdown
                    LaunchedEffect(loadedIngredientBreakdown) {
                        val loaded = loadedIngredientBreakdown ?: return@LaunchedEffect
                        val missingKeys = loaded.keys.filterNot { ingredientHasAlternatives.containsKey(it) }
                        if (missingKeys.isNotEmpty()) {
                            val alternatives = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                missingKeys.associateWith { key ->
                                    manualViewModel.getSubstitutesForIngredient(key).isNotEmpty()
                                }
                            }
                            ingredientHasAlternatives = ingredientHasAlternatives + alternatives
                        }
                    }
                    when {
                        loadedIngredientBreakdown == null -> {
                            Text("Loading ingredients...", fontSize = 13.sp, color = Color(0xFF9CA3AF))
                        }
                        loadedIngredientBreakdown.isEmpty() -> {
                            NoIngredientBreakdownState()
                        }
                        else -> {

                    // ── Substitution picker (only shown when NOT from meal plan) ──
                    if (substitutionTarget != null && substitutionCandidates.isNotEmpty() && !isPlannedMeal) {
                        Surface(
                            color = Color(0xFFF0F9FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Swap: ${manualViewModel.formatIngredientName(substitutionTarget!!)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0C4A6E)
                                    )
                                    TextButton(onClick = {
                                        substitutionTarget = null
                                        substitutionCandidates = emptyList()
                                    }) {
                                        Text("Cancel", fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                substitutionCandidates.forEach { candidate ->
                                    val dishIdx = ingredientSheetDishIndex
                                    Surface(
                                        onClick = {
                                            val newSubs = activeSubstitutions.toMutableMap()
                                            newSubs[substitutionTarget!!] = candidate.ingredientKey
                                            val sanitizedSubs = RecipeCustomizationRules.sanitizeSubstitutions(newSubs)
                                            activeSubstitutions = sanitizedSubs
                                            manualViewModel.applySubstitutionToDish(dishIdx, sanitizedSubs)
                                            // Reload breakdown
                                            scope.launch {
                                                val label = dish.dishLabel.ifEmpty { return@launch }
                                                ingredientBreakdown = null
                                                ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    manualViewModel.getIngredientBreakdown(label, sanitizedSubs)
                                                }
                                            }
                                            substitutionTarget = null
                                            substitutionCandidates = emptyList()
                                        },
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    candidate.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF374151)
                                                )
                                                Text(
                                                    "${candidate.calories.toInt()} kcal/100g • P:${candidate.protein.toInt()}g F:${candidate.fat.toInt()}g C:${candidate.carbs.toInt()}g",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                            }
                                            Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Ingredient list ──
                    if (isPlannedMeal) {
                        Surface(
                            color = Color(0xFFFFF7ED),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Planned meal customizations are locked here. Modify this recipe in Pantry before logging.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9A3412),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                    Text("Ingredients", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                    Spacer(Modifier.height(8.dp))

                    loadedIngredientBreakdown.forEach { (originalIngredientKey, ing) ->
                        val substitutedWith = activeSubstitutions[originalIngredientKey]
                        val isRemoved = ing.isRemoved || substitutedWith == ManualLogViewModel.REMOVED_INGREDIENT
                        val isSubstituted = !isRemoved && (ing.replacementIngredientKey != null || substitutedWith != null)
                        val canRemove = RecipeCustomizationRules.canRemoveIngredient(
                            originalIngredientKey = originalIngredientKey,
                            ingredientType = ing.ingredientType
                        )
                        val hasSubstitutionAlternatives = ingredientHasAlternatives[originalIngredientKey] == true
                        val tweakMultiplier = activeTweaks[originalIngredientKey] ?: 1f
                        val isTweaked = !isRemoved && tweakMultiplier != 1f
                        val canScalePortion = !isRemoved && !PortionScaler.isQualitative(ing.portionQuantity)
                        val canTweak = !isPlannedMeal && canScalePortion
                        val displayPortion = when {
                            ing.portionQuantity.isNotBlank() && canScalePortion -> PortionScaler.scale(ing.portionQuantity, tweakMultiplier)
                            ing.portionQuantity.isNotBlank() -> ing.portionQuantity
                            else -> "${(ing.rawWeightGrams * tweakMultiplier).toInt()}g raw"
                        }

                        val effectiveName = when {
                            isRemoved -> ing.originalDisplayName
                            isSubstituted -> ing.replacementDisplayName ?: substitutedWith?.let { manualViewModel.formatIngredientName(it) } ?: ing.displayName
                            else -> ing.displayName
                        }

                        // Skip removed ingredients' nutrition display but show the row
                        val rowBgColor = when {
                            isRemoved -> Color(0xFFFEF2F2)   // light red
                            isTweaked -> Color(0xFFF5F3FF)
                            isSubstituted -> Color(0xFFF0F9FF) // light blue
                            else -> Color(0xFFF9FAFB)
                        }

                        Surface(
                            color = rowBgColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (isRemoved) {
                                            // Removed ingredient: strikethrough name + badge
                                            Text(
                                                effectiveName,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = Color(0xFF9CA3AF),
                                                style = androidx.compose.ui.text.TextStyle(
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                )
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Surface(
                                                color = Color(0xFFFEE2E2),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    "Removed from recipe",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isSubstituted) {
                                            // Substituted ingredient: show new name + original
                                            Text(
                                                effectiveName,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = Color(0xFF0C4A6E)
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.SwapHoriz,
                                                    null,
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Replaces ${ing.originalDisplayName}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF0284C7)
                                                )
                                            }
                                            Text(
                                                displayPortion,
                                                fontSize = 11.sp,
                                                color = if (isTweaked) Color(0xFF7C3AED) else Color(0xFF9CA3AF),
                                                fontWeight = if (isTweaked) FontWeight.Medium else FontWeight.Normal
                                            )
                                        } else {
                                            Text(effectiveName, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF374151))
                                            Text(
                                                displayPortion,
                                                fontSize = 11.sp,
                                                color = if (isTweaked) Color(0xFF7C3AED) else Color(0xFF9CA3AF),
                                                fontWeight = if (isTweaked) FontWeight.Medium else FontWeight.Normal
                                            )
                                        }
                                    }

                                    // Swap button — only shown when NOT from meal plan and NOT removed
                                    if ((isRemoved || isSubstituted) && !isPlannedMeal) {
                                        Surface(
                                            onClick = {
                                                val newSubs = activeSubstitutions.toMutableMap()
                                                newSubs.remove(originalIngredientKey)
                                                val sanitizedSubs = RecipeCustomizationRules.sanitizeSubstitutions(newSubs)
                                                activeSubstitutions = sanitizedSubs
                                                manualViewModel.applySubstitutionToDish(ingredientSheetDishIndex, sanitizedSubs)
                                                scope.launch {
                                                    val label = dish.dishLabel.ifEmpty { return@launch }
                                                    ingredientBreakdown = null
                                                    ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        manualViewModel.getIngredientBreakdown(label, sanitizedSubs)
                                                    }
                                                }
                                            },
                                            color = if (isRemoved) Color(0xFFFEE2E2) else Color(0xFFBAE6FD),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Undo",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isRemoved) Color(0xFFDC2626) else Color(0xFF0C4A6E),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        if (hasSubstitutionAlternatives && !isPlannedMeal) {
                                            Surface(
                                                onClick = {
                                                    scope.launch {
                                                        substitutionTarget = originalIngredientKey
                                                        substitutionCandidates = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            manualViewModel.getSubstitutesForIngredient(originalIngredientKey)
                                                        }
                                                        if (substitutionCandidates.isEmpty()) {
                                                            substitutionTarget = null
                                                            ingredientHasAlternatives = ingredientHasAlternatives + (originalIngredientKey to false)
                                                        }
                                                    }
                                                },
                                                color = Color(0xFF0284C7).copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Icon(Icons.Default.SwapHoriz, "Swap", tint = Color(0xFF0284C7), modifier = Modifier.padding(4.dp).size(16.dp))
                                            }
                                        }
                                        if (canRemove && !isPlannedMeal) {
                                            if (hasSubstitutionAlternatives) {
                                                Spacer(Modifier.width(6.dp))
                                            }
                                            Surface(
                                                onClick = {
                                                    val newSubs = activeSubstitutions.toMutableMap()
                                                    newSubs[originalIngredientKey] = ManualLogViewModel.REMOVED_INGREDIENT
                                                    val sanitizedSubs = RecipeCustomizationRules.sanitizeSubstitutions(newSubs)
                                                    activeSubstitutions = sanitizedSubs
                                                    activeTweaks = activeTweaks - originalIngredientKey
                                                    manualViewModel.applySubstitutionToDish(ingredientSheetDishIndex, sanitizedSubs)
                                                    scope.launch {
                                                        val label = dish.dishLabel.ifEmpty { return@launch }
                                                        ingredientBreakdown = null
                                                        ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            manualViewModel.getIngredientBreakdown(label, sanitizedSubs)
                                                        }
                                                    }
                                                },
                                                color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFEF4444), modifier = Modifier.padding(4.dp).size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Nutrition row — only show for non-removed ingredients
                                if (!isRemoved) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Column {
                                            Text("${(ing.calories * tweakMultiplier).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                            Text("kcal", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        }
                                        Column {
                                            Text("${(ing.protein * tweakMultiplier).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                            Text("protein", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        }
                                        Column {
                                            Text("${(ing.carbs * tweakMultiplier).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEAB308))
                                            Text("carbs", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        }
                                        Column {
                                            Text("${(ing.fat * tweakMultiplier).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                                            Text("fats", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        }
                                        Column {
                                            Text("${(ing.sodium * tweakMultiplier).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                            Text("mg Na", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                        }
                                    }
                                    if (canTweak) {
                                        Spacer(Modifier.height(8.dp))
                                        IngredientTweakStepper(
                                            currentMultiplier = tweakMultiplier,
                                            onSelect = { step ->
                                                activeTweaks = if (step == 1f) {
                                                    activeTweaks - originalIngredientKey
                                                } else {
                                                    activeTweaks + (originalIngredientKey to step)
                                                }
                                                manualViewModel.applyIngredientTweakToDish(
                                                    ingredientSheetDishIndex,
                                                    originalIngredientKey,
                                                    step
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                        } // end else (ingredientBreakdown loaded)
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
    candidate1: Pair<com.calorieko.app.data.model.DishRecipeEntity, Float>,
    candidate2: Pair<com.calorieko.app.data.model.DishRecipeEntity, Float>,
    onSelect: (com.calorieko.app.data.model.DishRecipeEntity, Float) -> Unit,
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
                    Text("${candidate1.first.namePh} (${(candidate1.second * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSelect(candidate2.first, candidate2.second) },
                    colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("${candidate2.first.namePh} (${(candidate2.second * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
private fun PantryDeductionScreen(
    deductionItems: List<PantryDeductionItem>,
    onConfirm: (selectedKeys: Set<String>) -> Unit,
    onSkip: () -> Unit
) {
    val checkedItems = remember(deductionItems) {
        mutableStateMapOf<String, Boolean>().apply {
            deductionItems.forEach { item -> put(item.ingredientKey, false) }
        }
    }
    val selectedKeys = checkedItems.filterValues { it }.keys.toSet()
    val selectedCount = selectedKeys.size

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onSkip) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Skip pantry update")
                    }
                    Text("Update Pantry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Spacer(Modifier.size(48.dp))
                }
            }
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onConfirm(selectedKeys) },
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalorieKoGreen,
                            disabledContainerColor = Color(0xFFE5E7EB),
                            disabledContentColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            text = if (selectedCount == 1) "Remove & Finish" else "Remove $selectedCount Items & Finish",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Skip - Keep All", color = Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = CalorieKoGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Did you use up any of these ingredients?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Check the pantry items you want to remove, or skip to keep everything.",
                            fontSize = 13.sp,
                            color = Color(0xFF166534),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(deductionItems, key = { it.ingredientKey }) { item ->
                    val checked = checkedItems[item.ingredientKey] == true
                    Surface(
                        onClick = { checkedItems[item.ingredientKey] = !checked },
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp,
                        border = if (checked) BorderStroke(1.dp, CalorieKoGreen.copy(alpha = 0.45f)) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked -> checkedItems[item.ingredientKey] = isChecked },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CalorieKoGreen,
                                    uncheckedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Used in: ${item.usedInDishes.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSummaryOverlay(
    dishes: List<LoggedDish>,
    mealType: String,
    onMealTypeChange: (String) -> Unit,
    onRemoveDish: (Int) -> Unit,
    onAddMore: () -> Unit,
    onConfirmMeal: () -> Unit,
    onCancel: () -> Unit,
    isConfirming: Boolean = false,
    viewModel: LogMealViewModel
) {
    val totalCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = dishes.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = dishes.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = dishes.sumOf { it.fat.toDouble() }.toFloat()

    // Track expanded state per dish (by index)
    val expandedDishes = remember { mutableStateMapOf<Int, Boolean>() }
    var totalsExpanded by remember { mutableStateOf(false) }

    // Ingredient detail bottom sheet state
    var ingredientSheetDishIndex by remember { mutableStateOf<Int?>(null) }
    var ingredientBreakdown by remember { mutableStateOf<Map<String, IngredientNutritionBreakdown>?>(null) }
    var substitutionCandidates by remember { mutableStateOf<List<RawIngredientEntity>>(emptyList()) }
    var substitutionTarget by remember { mutableStateOf<String?>(null) }
    var activeSubstitutions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeTweaks by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var ingredientHasAlternatives by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    val substitutionsForCount = parseSubstitutionsJson(dish.substitutionsJson)
                    val activeCustomizationCount =
                        substitutionsForCount.size + parseTweaksJson(dish.tweaksJson)
                            .filterKeys { substitutionsForCount[it] != LogMealViewModel.REMOVED_INGREDIENT }
                            .size
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
                                    Text(dish.dishNamePh.ifBlank { dish.dishNameEn }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${dish.weightGrams.roundToInt()}g  -  ~${dish.calories.fmt()} kcal",
                                        fontSize = 13.sp, color = Color(0xFF6B7280)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "P: ~${dish.protein.fmt()}g  C: ~${dish.carbs.fmt()}g  F: ~${dish.fat.fmt()}g",
                                        fontSize = 12.sp, color = Color(0xFF9CA3AF)
                                    )
                                    if (activeCustomizationCount > 0) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFFF5F3FF),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                "$activeCustomizationCount customization${if (activeCustomizationCount == 1) "" else "s"} active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF6D28D9),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
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

                            // View Ingredients button
                            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        ingredientSheetDishIndex = index
                                        val substitutions = parseSubstitutionsJson(dish.substitutionsJson)
                                        val tweaks = parseTweaksJson(dish.tweaksJson)
                                            .filterKeys { substitutions[it] != LogMealViewModel.REMOVED_INGREDIENT }
                                        activeSubstitutions = substitutions
                                        activeTweaks = tweaks
                                        ingredientBreakdown = null
                                        ingredientHasAlternatives = emptyMap()
                                        scope.launch {
                                            val mlLabel = dish.dishLabel
                                            if (mlLabel.isEmpty()) {
                                                ingredientBreakdown = emptyMap()
                                                return@launch
                                            }
                                            ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                viewModel.getIngredientBreakdown(mlLabel, substitutions)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Restaurant, null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("View Ingredients", fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
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

    // ── Ingredient Detail Bottom Sheet ──
    if (ingredientSheetDishIndex != null) {
        val dishIdx = ingredientSheetDishIndex!!
        val dish = dishes.getOrNull(dishIdx)

        if (dish != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    ingredientSheetDishIndex = null
                    ingredientBreakdown = null
                    activeTweaks = emptyMap()
                    substitutionTarget = null
                    substitutionCandidates = emptyList()
                    ingredientHasAlternatives = emptyMap()
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    // Header
                    Text(dish.dishNamePh.ifBlank { dish.dishNameEn }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Text("${dish.weightGrams.roundToInt()}g cooked portion", fontSize = 13.sp, color = Color(0xFF6B7280))
                    Spacer(Modifier.height(16.dp))

                    if (activeTweaks.isNotEmpty()) {
                        IngredientTweaksBanner(
                            tweakCount = activeTweaks.size,
                            message = "Nutrition updated for this logged portion",
                            onClear = {
                                viewModel.clearIngredientTweaksFromDish(dishIdx)
                                activeTweaks = emptyMap()
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    val loadedIngredientBreakdown = ingredientBreakdown
                    LaunchedEffect(loadedIngredientBreakdown) {
                        val loaded = loadedIngredientBreakdown ?: return@LaunchedEffect
                        val missingKeys = loaded.keys.filterNot { ingredientHasAlternatives.containsKey(it) }
                        if (missingKeys.isNotEmpty()) {
                            val alternatives = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                missingKeys.associateWith { key ->
                                    viewModel.getSubstitutesForIngredient(key).isNotEmpty()
                                }
                            }
                            ingredientHasAlternatives = ingredientHasAlternatives + alternatives
                        }
                    }
                    when {
                        loadedIngredientBreakdown == null -> {
                            Text("Loading ingredients...", fontSize = 13.sp, color = Color(0xFF9CA3AF))
                        }
                        loadedIngredientBreakdown.isEmpty() -> {
                            NoIngredientBreakdownState()
                        }
                        else -> {
                        // Substitution picker
                        if (substitutionTarget != null && substitutionCandidates.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFF0F9FF),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Swap: ${viewModel.formatIngredientName(substitutionTarget!!)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0C4A6E))
                                        TextButton(onClick = { substitutionTarget = null; substitutionCandidates = emptyList() }) {
                                            Text("Cancel", fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    substitutionCandidates.forEach { candidate ->
                                        Surface(
                                            onClick = {
                                                val newSubs = activeSubstitutions.toMutableMap()
                                                newSubs[substitutionTarget!!] = candidate.ingredientKey
                                                val sanitizedSubs = RecipeCustomizationRules.sanitizeSubstitutions(newSubs)
                                                activeSubstitutions = sanitizedSubs
                                                viewModel.applySubstitutionToDish(dishIdx, sanitizedSubs)
                                                // Reload breakdown
                                                scope.launch {
                                                    val mlLabel = dish.dishLabel.ifEmpty { return@launch }
                                                    ingredientBreakdown = null
                                                    ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        viewModel.getIngredientBreakdown(mlLabel, sanitizedSubs)
                                                    }
                                                }
                                                substitutionTarget = null
                                                substitutionCandidates = emptyList()
                                            },
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(candidate.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                                                    Text("${candidate.calories.toInt()} kcal/100g • P:${candidate.protein.toInt()}g F:${candidate.fat.toInt()}g C:${candidate.carbs.toInt()}g",
                                                        fontSize = 11.sp, color = Color(0xFF9CA3AF))
                                                }
                                                Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // Ingredient list with nutrition
                        Text("Ingredients", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                        Spacer(Modifier.height(8.dp))

                        loadedIngredientBreakdown.forEach { (originalIngredientKey, breakdown) ->
                            val substitutedWith = activeSubstitutions[originalIngredientKey]
                            val isRemoved = breakdown.isRemoved || substitutedWith == LogMealViewModel.REMOVED_INGREDIENT
                            val isSubstituted = !isRemoved && (breakdown.replacementIngredientKey != null || substitutedWith != null)
                            val canRemove = RecipeCustomizationRules.canRemoveIngredient(
                                originalIngredientKey = originalIngredientKey,
                                ingredientType = breakdown.ingredientType
                            )
                            val hasSubstitutionAlternatives = ingredientHasAlternatives[originalIngredientKey] == true
                            val tweakMultiplier = activeTweaks[originalIngredientKey] ?: 1f
                            val isTweaked = !isRemoved && tweakMultiplier != 1f
                            val canTweak = !isRemoved && !PortionScaler.isQualitative(breakdown.portionQuantity)
                            val displayPortion = when {
                                breakdown.portionQuantity.isNotBlank() && canTweak -> PortionScaler.scale(breakdown.portionQuantity, tweakMultiplier)
                                breakdown.portionQuantity.isNotBlank() -> breakdown.portionQuantity
                                else -> "${(breakdown.rawWeightGrams * tweakMultiplier).toInt()}g raw"
                            }
                            val effectiveName = when {
                                isRemoved -> breakdown.originalDisplayName
                                isSubstituted -> breakdown.replacementDisplayName ?: substitutedWith?.let { viewModel.formatIngredientName(it) } ?: breakdown.displayName
                                else -> breakdown.displayName
                            }
                            val rowColor = when {
                                isRemoved -> Color(0xFFFEF2F2)
                                isTweaked -> Color(0xFFF5F3FF)
                                isSubstituted -> Color(0xFFF0F9FF)
                                else -> Color(0xFFF9FAFB)
                            }
                            val rowBorder = when {
                                isTweaked -> Color(0xFFDDD6FE)
                                isSubstituted -> Color(0xFFBAE6FD)
                                else -> Color.Transparent
                            }
                            Surface(
                                color = rowColor,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, rowBorder),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                effectiveName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSubstituted) FontWeight.Medium else FontWeight.Normal,
                                                color = when {
                                                    isRemoved -> Color(0xFF6B7280)
                                                    isSubstituted -> Color(0xFF0C4A6E)
                                                    else -> Color(0xFF374151)
                                                },
                                                textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            when {
                                                isSubstituted -> Text("Replaces ${breakdown.originalDisplayName}", fontSize = 10.sp, color = Color(0xFF0284C7))
                                                isRemoved -> Text("Removed from recipe", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                                else -> Text(
                                                    displayPortion,
                                                    fontSize = 11.sp,
                                                    color = if (isTweaked) Color(0xFF7C3AED) else Color(0xFF9CA3AF),
                                                    fontWeight = if (isTweaked) FontWeight.Medium else FontWeight.Normal
                                                )
                                            }
                                            if (isSubstituted && !isRemoved) {
                                                Text(
                                                    displayPortion,
                                                    fontSize = 11.sp,
                                                    color = if (isTweaked) Color(0xFF7C3AED) else Color(0xFF9CA3AF),
                                                    fontWeight = if (isTweaked) FontWeight.Medium else FontWeight.Normal
                                                )
                                            }
                                        }
                                        if (isRemoved || isSubstituted) {
                                            Surface(
                                                onClick = {
                                                    val newSubs = activeSubstitutions.toMutableMap()
                                                    newSubs.remove(originalIngredientKey)
                                                    val sanitizedSubs = RecipeCustomizationRules.sanitizeSubstitutions(newSubs)
                                                    activeSubstitutions = sanitizedSubs
                                                    viewModel.applySubstitutionToDish(dishIdx, sanitizedSubs)
                                                    scope.launch {
                                                        val mlLabel = dish.dishLabel.ifEmpty { return@launch }
                                                        ingredientBreakdown = null
                                                        ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            viewModel.getIngredientBreakdown(mlLabel, sanitizedSubs)
                                                        }
                                                    }
                                                },
                                                color = if (isRemoved) Color(0xFFFEE2E2) else Color(0xFFBAE6FD),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Undo",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRemoved) Color(0xFFDC2626) else Color(0xFF0C4A6E),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            if (hasSubstitutionAlternatives) {
                                                Surface(
                                                    onClick = {
                                                        scope.launch {
                                                            substitutionTarget = originalIngredientKey
                                                            substitutionCandidates = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                viewModel.getSubstitutesForIngredient(originalIngredientKey)
                                                            }
                                                            if (substitutionCandidates.isEmpty()) {
                                                                substitutionTarget = null
                                                                ingredientHasAlternatives = ingredientHasAlternatives + (originalIngredientKey to false)
                                                            }
                                                        }
                                                    },
                                                    color = Color(0xFF0284C7).copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Icon(Icons.Default.SwapHoriz, "Swap", tint = Color(0xFF0284C7), modifier = Modifier.padding(4.dp).size(16.dp))
                                                }
                                            }
                                            if (canRemove) {
                                                if (hasSubstitutionAlternatives) {
                                                    Spacer(Modifier.width(6.dp))
                                                }
                                                Surface(
                                                    onClick = {
                                                        val newSubs = activeSubstitutions.toMutableMap()
                                                        newSubs[originalIngredientKey] = LogMealViewModel.REMOVED_INGREDIENT
                                                        val sanitizedSubs = RecipeCustomizationRules.sanitizeSubstitutions(newSubs)
                                                        activeSubstitutions = sanitizedSubs
                                                        activeTweaks = activeTweaks - originalIngredientKey
                                                        viewModel.applySubstitutionToDish(dishIdx, sanitizedSubs)
                                                        scope.launch {
                                                            val mlLabel = dish.dishLabel.ifEmpty { return@launch }
                                                            ingredientBreakdown = null
                                                            ingredientBreakdown = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                viewModel.getIngredientBreakdown(mlLabel, sanitizedSubs)
                                                            }
                                                        }
                                                    },
                                                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFEF4444), modifier = Modifier.padding(4.dp).size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    if (!isRemoved) {
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Column {
                                                Text("${(breakdown.calories * tweakMultiplier).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                                Text("kcal", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                            }
                                            Column {
                                                Text("${(breakdown.protein * tweakMultiplier).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                                Text("protein", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                            }
                                            Column {
                                                Text("${(breakdown.carbs * tweakMultiplier).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEAB308))
                                                Text("carbs", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                            }
                                            Column {
                                                Text("${(breakdown.fat * tweakMultiplier).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                                                Text("fats", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                            }
                                            Column {
                                                Text("${(breakdown.sodium * tweakMultiplier).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                                Text("mg Na", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                            }
                                        }
                                        if (canTweak) {
                                            Spacer(Modifier.height(8.dp))
                                            IngredientTweakStepper(
                                                currentMultiplier = tweakMultiplier,
                                                onSelect = { step ->
                                                    activeTweaks = if (step == 1f) {
                                                        activeTweaks - originalIngredientKey
                                                    } else {
                                                        activeTweaks + (originalIngredientKey to step)
                                                    }
                                                    viewModel.applyIngredientTweakToDish(
                                                        dishIdx,
                                                        originalIngredientKey,
                                                        step
                                                    )
                                                }
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
    }
}

@Composable
private fun NoIngredientBreakdownState() {
    Surface(
        color = Color(0xFFF9FAFB),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "No ingredient breakdown available",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "This dish uses direct nutrition data instead of recipe ingredients.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun IngredientTweaksBanner(
    tweakCount: Int,
    message: String,
    onClear: (() -> Unit)?
) {
    Surface(
        color = Color(0xFFF5F3FF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$tweakCount ingredient amount${if (tweakCount == 1) "" else "s"} adjusted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5B21B6)
                )
                Text(
                    message,
                    fontSize = 11.sp,
                    color = Color(0xFF7C3AED)
                )
            }
            if (onClear != null) {
                Surface(
                    onClick = onClear,
                    color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientTweakStepper(
    currentMultiplier: Float,
    onSelect: (Float) -> Unit
) {
    val steps = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f)
    val labels = listOf("\u00BC\u00d7", "\u00BD\u00d7", "1\u00d7", "1\u00BD\u00d7", "2\u00d7", "3\u00d7", "4\u00d7")

    Text(
        "Adjust amount",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6B7280)
    )
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = currentMultiplier == step
            Surface(
                onClick = { onSelect(step) },
                color = when {
                    isActive && step != 1f -> Color(0xFF7C3AED)
                    isActive -> Color(0xFF374151)
                    else -> Color(0xFFE5E7EB)
                },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    labels[index],
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color.White else Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 5.dp)
                )
            }
        }
    }
}

private fun parseSubstitutionsJson(json: String): Map<String, String> {
    if (json.isBlank()) return emptyMap()
    return try {
        val obj = org.json.JSONObject(json)
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key -> map[key] = obj.getString(key) }
        RecipeCustomizationRules.sanitizeSubstitutions(map)
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun parseTweaksJson(json: String): Map<String, Float> {
    if (json.isBlank()) return emptyMap()
    return try {
        val obj = org.json.JSONObject(json)
        val map = mutableMapOf<String, Float>()
        obj.keys().forEach { key ->
            val value = obj.getDouble(key).toFloat()
            if (value.isFinite() && value > 0f && value != 1f) {
                map[key] = value
            }
        }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

// NutrientChip and ExpandableNutrientGrid are now imported from
// com.calorieko.app.ui.components.NutrientComponents
// ───────────────────────────────────────────────────────────────
// Quick Log Screen (used for planned meal → one-tap logging)
// ───────────────────────────────────────────────────────────────

@Composable
fun QuickLogScreen(
    viewModel: ManualLogViewModel,
    dishLabel: String,
    mealSlot: String,
    onBack: () -> Unit,
    onMealConfirmed: () -> Unit,
    bleScaleManager: BleScaleManager,
    onNavigateToPairing: () -> Unit = {}
) {
    val dishes by viewModel.loggedDishes.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val showSummary by viewModel.showSummary.collectAsState()
    val isConfirming by viewModel.isConfirming.collectAsState()
    val showPantryDeduction by viewModel.showPantryDeduction.collectAsState()
    val pantryDeductionItems by viewModel.pantryDeductionItems.collectAsState()
    val plannedEntries by viewModel.plannedQuickLogEntries.collectAsState()
    val plannedMethod by viewModel.plannedWeightMethod.collectAsState()
    val plannedIndex by viewModel.plannedWeightIndex.collectAsState()
    val isPlannedQuickLog by viewModel.isPlannedQuickLog.collectAsState()
    val currentPlannedRecipe by viewModel.currentPlannedRecipe.collectAsState()
    val plannedManualWeightText by viewModel.plannedManualWeightText.collectAsState()
    val plannedScaleWeight by viewModel.plannedScaleWeight.collectAsState()
    val plannedScaleWeightStable by viewModel.plannedScaleWeightStable.collectAsState()
    val connectionState by bleScaleManager.connectionState.collectAsState()
    val liveWeight by bleScaleManager.liveWeight.collectAsState()

    LaunchedEffect(connectionState) {
        viewModel.updatePlannedScaleConnectionStatus(
            connectionState is com.calorieko.app.ble.BleConnectionState.Connected
        )
    }

    LaunchedEffect(liveWeight) {
        viewModel.updatePlannedScaleWeight(liveWeight)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ManualLogEvent.MealConfirmed -> onMealConfirmed()
            }
        }
    }

    if (showPantryDeduction && pantryDeductionItems.isNotEmpty()) {
        PantryDeductionScreen(
            deductionItems = pantryDeductionItems,
            onConfirm = { selectedKeys -> viewModel.confirmPantryDeduction(selectedKeys) },
            onSkip = { viewModel.skipPantryDeduction() }
        )
    } else if (isPlannedQuickLog && plannedEntries.isNotEmpty() && plannedMethod == null) {
        PlannedWeightMethodSelectionContent(
            mealSlot = mealSlot.ifBlank { mealType },
            dishCount = plannedEntries.size,
            onSelectScale = { viewModel.selectPlannedWeightMethod(PlannedWeightMethod.SMART_SCALE) },
            onSelectManual = { viewModel.selectPlannedWeightMethod(PlannedWeightMethod.MANUAL) },
            onBack = onBack
        )
    } else if (isPlannedQuickLog && !showSummary) {
        val currentEntry = plannedEntries.getOrNull(plannedIndex)
        if (currentEntry == null) {
            QuickLogErrorState(
                title = "No planned dish found",
                message = "This planned meal could not be loaded.",
                onBack = onBack
            )
        } else if (plannedMethod == PlannedWeightMethod.MANUAL) {
            PlannedManualWeightContent(
                recipe = currentPlannedRecipe,
                entry = currentEntry,
                weightText = plannedManualWeightText,
                progressText = "Dish ${plannedIndex + 1} of ${plannedEntries.size}",
                onWeightChange = { viewModel.setCurrentPlannedManualWeight(it) },
                onAddWeight = { viewModel.logCurrentPlannedDishWithManualWeight() },
                onBack = onBack
            )
        } else {
            PlannedScaleWeightContent(
                recipe = currentPlannedRecipe,
                entry = currentEntry,
                progressText = "Dish ${plannedIndex + 1} of ${plannedEntries.size}",
                connectionState = connectionState,
                weight = plannedScaleWeight,
                isStable = plannedScaleWeightStable,
                onTare = { bleScaleManager.sendTareCommand() },
                onUseWeight = { viewModel.logCurrentPlannedDishWithScaleWeight(plannedScaleWeight) },
                onNavigateToPairing = onNavigateToPairing,
                onBack = onBack
            )
        }
    } else if (showSummary && dishes.isNotEmpty()) {
        val requiredCount = plannedEntries.size
        val canConfirm = if (isPlannedQuickLog) {
            canConfirmPlannedQuickLog(requiredCount, dishes)
        } else {
            dishes.isNotEmpty()
        }
        val disabledReason = if (isPlannedQuickLog && !canConfirm) {
            "Enter a positive weight for every planned dish before confirming."
        } else {
            null
        }
        ManualMealSummaryOverlay(
            dishes = dishes,
            mealType = mealType,
            onMealTypeChange = { viewModel.updateMealType(it) },
            onRemoveDish = { viewModel.removeDish(it) },
            onAddMore = { /* Not applicable for quick-log */ },
            onConfirmMeal = { viewModel.confirmMeal() },
            onCancel = onBack,
            isConfirming = isConfirming,
            manualViewModel = viewModel,
            isPlannedMeal = isPlannedQuickLog,
            canConfirmMeal = canConfirm,
            confirmDisabledReason = disabledReason
        )
    } else if (showSummary && dishes.isEmpty()) {
        // Error state: recipe not found or all dishes failed to load
        QuickLogErrorState(
            title = "No dishes ready to log",
            message = "\"${dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }}\" could not be prepared.",
            onBack = onBack
        )
    } else {
        // Loading state while the dish is being pre-computed
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Preparing quick log...", fontSize = 16.sp, color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                Text(dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
private fun PlannedWeightMethodSelectionContent(
    mealSlot: String,
    dishCount: Int,
    onSelectScale: () -> Unit,
    onSelectManual: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Weigh Planned Meal",
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
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "$mealSlot - $dishCount planned dish${if (dishCount == 1) "" else "es"}",
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            )
            MealModeCard(
                icon = Icons.Default.MonitorWeight,
                secondaryIcon = null,
                title = "Smart Scale",
                description = "Use the connected scale and confirm each stable weight.",
                tags = listOf("Connected Scale", "Stable Capture"),
                accentColor = CalorieKoGreen,
                accentBgColor = Color(0xFFDCFCE7),
                onClick = onSelectScale
            )
            MealModeCard(
                icon = Icons.Default.Edit,
                secondaryIcon = null,
                title = "Manual Entry",
                description = "Type the actual cooked weight for each planned dish.",
                tags = listOf("No Scale Needed", "Dish by Dish"),
                accentColor = CalorieKoOrange,
                accentBgColor = Color(0xFFFFF7ED),
                onClick = onSelectManual
            )
        }
    }
}

@Composable
private fun PlannedManualWeightContent(
    recipe: DishRecipeEntity?,
    entry: com.calorieko.app.viewmodel.QuickLogDishEntry,
    weightText: String,
    progressText: String,
    onWeightChange: (String) -> Unit,
    onAddWeight: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = CalorieKoGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Manual Weight", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            WeightInputContent(
                dish = recipe,
                weightText = weightText,
                onWeightChange = onWeightChange,
                onChangeDish = null,
                onAddDish = onAddWeight,
                actionText = "Use This Weight",
                progressText = progressText,
                dishLabelFallback = entry.dishLabel,
                showDefaultServingPrefillHint = false
            )
        }
    }
}

@Composable
private fun PlannedScaleWeightContent(
    recipe: DishRecipeEntity?,
    entry: com.calorieko.app.viewmodel.QuickLogDishEntry,
    progressText: String,
    connectionState: com.calorieko.app.ble.BleConnectionState,
    weight: Float,
    isStable: Boolean,
    onTare: () -> Unit,
    onUseWeight: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onBack: () -> Unit
) {
    val isConnected = connectionState is com.calorieko.app.ble.BleConnectionState.Connected
    val canUseWeight = isConnected && isStable && weight > 0f
    val dishName = recipe?.namePh?.ifBlank { recipe.nameEn }
        ?: entry.dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
    val statusText = when {
        !isConnected -> "Scale disconnected"
        weight <= 0f -> "Place the dish on the scale"
        isStable -> "Stable"
        else -> "Stabilizing..."
    }

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = CalorieKoGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Smart Scale Weight", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(progressText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CalorieKoGreen)
            Spacer(Modifier.height(8.dp))
            Text(dishName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), textAlign = TextAlign.Center)
            recipe?.nameEn?.takeIf { it.isNotBlank() && it != dishName }?.let { englishName ->
                Text(englishName, fontSize = 13.sp, color = Color(0xFF9CA3AF), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = if (isConnected) CalorieKoGreen else Color(0xFF9CA3AF),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(statusText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${weight.roundToInt()}g",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canUseWeight) CalorieKoGreen else Color(0xFF1F2937)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onTare,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Zero Scale")
                }
                if (!isConnected) {
                    Button(
                        onClick = onNavigateToPairing,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange)
                    ) {
                        Text("Pair Scale", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onUseWeight,
                        enabled = canUseWeight,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalorieKoGreen,
                            disabledContainerColor = Color(0xFFD1D5DB)
                        )
                    ) {
                        Text("Use This Weight", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLogErrorState(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEA580C),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

private fun Float.fmt() = String.format(java.util.Locale.US, "%.1f", this)

private fun sanitizeDecimalInput(value: String): String {
    val builder = StringBuilder()
    var hasDecimal = false
    value.forEach { char ->
        when {
            char.isDigit() -> builder.append(char)
            char == '.' && !hasDecimal -> {
                builder.append(char)
                hasDecimal = true
            }
        }
    }
    return builder.toString().take(6)
}

private fun defaultServingWeightText(dish: DishRecipeEntity): String {
    val defaultWeight = when {
        dish.perServingWeightG > 0f -> dish.perServingWeightG
        dish.servings > 0 -> dish.cookedWeightG / dish.servings
        else -> 0f
    }
    return if (defaultWeight > 0f) {
        String.format(java.util.Locale.US, "%.0f", defaultWeight)
    } else {
        ""
    }
}
