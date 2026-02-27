package com.calorieko.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.ml.CalorieKoClassifier
import com.calorieko.app.ml.DishLabelMapper
import com.calorieko.app.ui.components.CameraPreview
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// ───────────────────────────────────────────────────────────────
// Data classes & enums
// ───────────────────────────────────────────────────────────────

/** Phases of the Log-Meal workflow. */
enum class LogMealPhase { SCANNING, DISH_READY, MEAL_SUMMARY, ERROR }

/** A dish that has been recognized and queued for logging. */
data class LoggedDish(
    val dishNameEn: String,
    val weightGrams: Float,
    val confidence: Float,
    val foodId: Int,
    // Pre-computed nutrients
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val sugar: Float,
    val saturatedFat: Float,
    val polyunsaturatedFat: Float,
    val monounsaturatedFat: Float,
    val transFat: Float,
    val cholesterol: Float,
    val sodium: Float,
    val potassium: Float,
    val vitaminA: Float,
    val vitaminC: Float,
    val calcium: Float,
    val iron: Float
)

private const val CONFIDENCE_THRESHOLD = 0.70f

// ───────────────────────────────────────────────────────────────
// Main Screen
// ───────────────────────────────────────────────────────────────

@Composable
fun LogMealScreen(onBack: () -> Unit, onMealConfirmed: () -> Unit) {

    // ── Context / DB / Auth ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: ""

    // ── Classifier (lifecycle-aware) ──
    val classifier = remember { CalorieKoClassifier(context) }
    DisposableEffect(Unit) { onDispose { classifier.close() } }

    // ── Camera permission ──
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── State ──
    var flashEnabled by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(LogMealPhase.SCANNING) }
    var weight by remember { mutableIntStateOf(0) }
    var weightStable by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // AI results from the latest frame
    var topLabel by remember { mutableStateOf("") }
    var topConfidence by remember { mutableFloatStateOf(0f) }

    // Dish being confirmed (DISH_READY phase)
    var pendingDishName by remember { mutableStateOf("") }
    var pendingConfidence by remember { mutableFloatStateOf(0f) }
    var pendingCaloriesEst by remember { mutableFloatStateOf(0f) }

    // All dishes queued in this meal session
    val loggedDishes = remember { mutableStateListOf<LoggedDish>() }

    // Meal type auto-detected by time of day
    var mealType by remember {
        val hour = LocalTime.now().hour
        mutableStateOf(
            when {
                hour < 10 -> "Breakfast"
                hour < 14 -> "Lunch"
                hour < 17 -> "Snacks"
                else      -> "Dinner"
            }
        )
    }

    // ── Mock BLE Weight Simulation ──
    fun startWeightSimulation() {
        weight = 0
        weightStable = false
        scope.launch {
            val target = Random.nextInt(150, 351)
            var current = 0
            while (current < target) {
                delay(80)
                current += Random.nextInt(8, 25)
                if (current > target) current = target
                weight = current
            }
            weightStable = true
        }
    }

    // Start weight simulation when we enter SCANNING
    LaunchedEffect(phase) {
        if (phase == LogMealPhase.SCANNING) {
            startWeightSimulation()
        }
    }

    // ── Auto-transition logic ──
    // When SCANNING: if weight is stable AND AI confidence ≥ threshold → DISH_READY
    LaunchedEffect(weightStable, topLabel, topConfidence, phase) {
        if (phase == LogMealPhase.SCANNING && weightStable && weight > 0) {
            if (topLabel == "negative" && topConfidence >= CONFIDENCE_THRESHOLD) {
                errorMessage = "This dish is not supported by CalorieKo. Please ensure proper lighting and try a supported Filipino dish."
                phase = LogMealPhase.ERROR
            } else if (DishLabelMapper.isSupported(topLabel) && topConfidence >= CONFIDENCE_THRESHOLD) {
                val foodName = DishLabelMapper.toFoodName(topLabel) ?: return@LaunchedEffect
                pendingDishName = foodName
                pendingConfidence = topConfidence
                // Quick calorie estimate
                val food = withContext(Dispatchers.IO) { db.foodDao().getFoodByName(foodName) }
                pendingCaloriesEst = if (food != null) food.caloriesPer100g * weight / 100f else 0f
                phase = LogMealPhase.DISH_READY
            }
        }
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
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
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
        return
    }

    // ── MEAL_SUMMARY overlay ──
    if (phase == LogMealPhase.MEAL_SUMMARY) {
        MealSummaryOverlay(
            dishes = loggedDishes,
            mealType = mealType,
            onMealTypeChange = { mealType = it },
            onRemoveDish = { loggedDishes.removeAt(it) },
            onAddMore = { phase = LogMealPhase.SCANNING },
            onConfirmMeal = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        persistMeal(db, uid, mealType, loggedDishes)
                    }
                    onMealConfirmed()
                }
            },
            onCancel = onBack
        )
        return
    }

    // ── Main camera UI ──
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. Live camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            classifier = classifier,
            onFrameAnalyzed = { results ->
                if (results.isNotEmpty() && phase == LogMealPhase.SCANNING) {
                    topLabel = results[0].first
                    topConfidence = results[0].second
                }
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
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).background(
                            if (weightStable && weight > 0) CalorieKoGreen else if (weight > 0) CalorieKoOrange else Color.Gray,
                            CircleShape
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Weight: ", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                    Text(
                        "${weight}g",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (weightStable) CalorieKoGreen else CalorieKoOrange
                    )
                }
            }

            // AI badge
            val displayLabel = DishLabelMapper.toFoodName(topLabel) ?: topLabel
            val confPercent = (topConfidence * 100).toInt()
            val aiReady = DishLabelMapper.isSupported(topLabel) && topConfidence >= CONFIDENCE_THRESHOLD
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

        // 4. Scanner animation (while scanning)
        if (phase == LogMealPhase.SCANNING && !weightStable) {
            ScannerAnimation()
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
                            .clickable { phase = LogMealPhase.MEAL_SUMMARY }
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

            // Status text (while scanning)
            if (phase == LogMealPhase.SCANNING) {
                val statusText = when {
                    !weightStable && weight == 0 -> "Waiting for scale data..."
                    !weightStable               -> "Stabilizing weight..."
                    topConfidence < CONFIDENCE_THRESHOLD -> "AI is analyzing the dish..."
                    else                                 -> "Preparing..."
                }
                Text(
                    statusText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
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
                onLogDish = {
                    scope.launch {
                        val food = withContext(Dispatchers.IO) { db.foodDao().getFoodByName(pendingDishName) }
                        if (food != null) {
                            val w = weight.toFloat()
                            loggedDishes.add(
                                LoggedDish(
                                    dishNameEn = pendingDishName,
                                    weightGrams = w,
                                    confidence = pendingConfidence,
                                    foodId = food.foodId,
                                    calories        = food.caloriesPer100g * w / 100f,
                                    protein         = food.proteinPer100g * w / 100f,
                                    carbs           = food.carbsPer100g * w / 100f,
                                    fat             = food.fatPer100g * w / 100f,
                                    fiber           = food.fiberPer100g * w / 100f,
                                    sugar           = food.sugarPer100g * w / 100f,
                                    saturatedFat    = food.saturatedFatPer100g * w / 100f,
                                    polyunsaturatedFat = food.polyunsaturatedFatPer100g * w / 100f,
                                    monounsaturatedFat = food.monounsaturatedFatPer100g * w / 100f,
                                    transFat        = food.transFatPer100g * w / 100f,
                                    cholesterol     = food.cholesterolPer100g * w / 100f,
                                    sodium          = food.sodiumPer100g * w / 100f,
                                    potassium       = food.potassiumPer100g * w / 100f,
                                    vitaminA        = food.vitaminAPer100g * w / 100f,
                                    vitaminC        = food.vitaminCPer100g * w / 100f,
                                    calcium         = food.calciumPer100g * w / 100f,
                                    iron            = food.ironPer100g * w / 100f
                                )
                            )
                        }
                        // Reset for next scan
                        topLabel = ""
                        topConfidence = 0f
                        phase = LogMealPhase.SCANNING
                    }
                },
                onCancel = {
                    topLabel = ""
                    topConfidence = 0f
                    phase = LogMealPhase.SCANNING
                }
            )
        }

        // 8. Error overlay
        if (phase == LogMealPhase.ERROR && errorMessage != null) {
            ErrorOverlay(
                message = errorMessage!!,
                onRetry = {
                    errorMessage = null
                    topLabel = ""
                    topConfidence = 0f
                    phase = LogMealPhase.SCANNING
                },
                onCancel = onBack
            )
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
    weight: Int,
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
                InfoRow("Weight", "${weight}g")
                InfoRow("Est. Calories", "${estimatedCalories.toInt()} kcal")

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
// Meal Summary Overlay
// ───────────────────────────────────────────────────────────────

@Composable
private fun MealSummaryOverlay(
    dishes: List<LoggedDish>,
    mealType: String,
    onMealTypeChange: (String) -> Unit,
    onRemoveDish: (Int) -> Unit,
    onAddMore: () -> Unit,
    onConfirmMeal: () -> Unit,
    onCancel: () -> Unit
) {
    val totalCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = dishes.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = dishes.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = dishes.sumOf { it.fat.toDouble() }.toFloat()

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
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dish.dishNameEn, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F2937))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${dish.weightGrams.toInt()}g  •  ${dish.calories.toInt()} kcal",
                                    fontSize = 13.sp, color = Color(0xFF6B7280)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "P: ${dish.protein.toInt()}g  C: ${dish.carbs.toInt()}g  F: ${dish.fat.toInt()}g",
                                    fontSize = 12.sp, color = Color(0xFF9CA3AF)
                                )
                            }
                            IconButton(onClick = { onRemoveDish(index) }) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
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
                            Text("Meal Totals", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1F2937))
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = CalorieKoGreen.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Calories", fontSize = 14.sp, color = Color(0xFF374151))
                                Text("${totalCalories.toInt()} kcal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CalorieKoGreen)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                NutrientChip("Protein", "${totalProtein.toInt()}g")
                                NutrientChip("Carbs", "${totalCarbs.toInt()}g")
                                NutrientChip("Fat", "${totalFat.toInt()}g")
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
                        enabled = dishes.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen, disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm Meal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

@Composable
private fun NutrientChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
        Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
    }
}

// ───────────────────────────────────────────────────────────────
// Error overlay
// ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Dish Not Supported", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = CalorieKoOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Try Again") }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Scanner animation (retained from original)
// ───────────────────────────────────────────────────────────────

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
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, CalorieKoGreen, Color.Transparent)
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 4.dp.toPx()
        )
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

// ───────────────────────────────────────────────────────────────
// Persistence helper
// ───────────────────────────────────────────────────────────────

/**
 * Persists a confirmed meal to Room.
 *
 * 1. Inserts a [MealLogEntity] (parent)
 * 2. Inserts all [MealLogItemEntity] children
 * 3. Inserts an [ActivityLogEntity] per dish (for the Dashboard activity feed)
 * 4. Upserts [DailyNutritionSummaryEntity]
 */
private suspend fun persistMeal(
    db: AppDatabase,
    uid: String,
    mealType: String,
    dishes: List<LoggedDish>
) {
    val now = System.currentTimeMillis()
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(now))

    // 1. Insert parent MealLog
    val mealLogId = db.mealLogDao().insertMealLog(
        MealLogEntity(uid = uid, mealType = mealType, timestamp = now)
    )

    // 2. Insert child items
    val items = dishes.map { d ->
        MealLogItemEntity(
            mealLogId       = mealLogId,
            foodId          = d.foodId,
            dishName        = d.dishNameEn,
            weightGrams     = d.weightGrams,
            calories        = d.calories,
            protein         = d.protein,
            carbs           = d.carbs,
            fiber           = d.fiber,
            sugar           = d.sugar,
            fat             = d.fat,
            saturatedFat    = d.saturatedFat,
            polyunsaturatedFat = d.polyunsaturatedFat,
            monounsaturatedFat = d.monounsaturatedFat,
            transFat        = d.transFat,
            cholesterol     = d.cholesterol,
            sodium          = d.sodium,
            potassium       = d.potassium,
            vitaminA        = d.vitaminA,
            vitaminC        = d.vitaminC,
            calcium         = d.calcium,
            iron            = d.iron
        )
    }
    db.mealLogItemDao().insertItems(items)

    // 3. Insert ActivityLogEntity per dish (dashboard feed compatibility)
    for (d in dishes) {
        db.activityLogDao().insertLog(
            ActivityLogEntity(
                uid             = uid,
                type            = "meal",
                name            = d.dishNameEn,
                timeString      = timeString,
                weightOrDuration = "${d.weightGrams.toInt()}g",
                calories        = d.calories.toInt(),
                protein         = d.protein.toInt(),
                carbs           = d.carbs.toInt(),
                fats            = d.fat.toInt(),
                sodium          = d.sodium.toInt(),
                timestamp       = now
            )
        )
    }

    // 4. Upsert DailyNutritionSummary
    val today = LocalDate.now().toEpochDay()
    val existing = db.dailyNutritionSummaryDao().getSummaryForDate(uid, today)

    val mealCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()

    val updated = (existing ?: DailyNutritionSummaryEntity(uid = uid, dateEpochDay = today)).let { s ->
        s.copy(
            id                     = s.id,
            totalCalories          = s.totalCalories + mealCalories,
            totalProtein           = s.totalProtein + dishes.sumOf { it.protein.toDouble() }.toFloat(),
            totalCarbs             = s.totalCarbs + dishes.sumOf { it.carbs.toDouble() }.toFloat(),
            totalFiber             = s.totalFiber + dishes.sumOf { it.fiber.toDouble() }.toFloat(),
            totalSugar             = s.totalSugar + dishes.sumOf { it.sugar.toDouble() }.toFloat(),
            totalFat               = s.totalFat + dishes.sumOf { it.fat.toDouble() }.toFloat(),
            totalSaturatedFat      = s.totalSaturatedFat + dishes.sumOf { it.saturatedFat.toDouble() }.toFloat(),
            totalPolyunsaturatedFat = s.totalPolyunsaturatedFat + dishes.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat(),
            totalMonounsaturatedFat = s.totalMonounsaturatedFat + dishes.sumOf { it.monounsaturatedFat.toDouble() }.toFloat(),
            totalTransFat          = s.totalTransFat + dishes.sumOf { it.transFat.toDouble() }.toFloat(),
            totalCholesterol       = s.totalCholesterol + dishes.sumOf { it.cholesterol.toDouble() }.toFloat(),
            totalSodium            = s.totalSodium + dishes.sumOf { it.sodium.toDouble() }.toFloat(),
            totalPotassium         = s.totalPotassium + dishes.sumOf { it.potassium.toDouble() }.toFloat(),
            totalVitaminA          = s.totalVitaminA + dishes.sumOf { it.vitaminA.toDouble() }.toFloat(),
            totalVitaminC          = s.totalVitaminC + dishes.sumOf { it.vitaminC.toDouble() }.toFloat(),
            totalCalcium           = s.totalCalcium + dishes.sumOf { it.calcium.toDouble() }.toFloat(),
            totalIron              = s.totalIron + dishes.sumOf { it.iron.toDouble() }.toFloat(),
            breakfastCalories      = s.breakfastCalories + if (mealType == "Breakfast") mealCalories else 0f,
            lunchCalories          = s.lunchCalories + if (mealType == "Lunch") mealCalories else 0f,
            dinnerCalories         = s.dinnerCalories + if (mealType == "Dinner") mealCalories else 0f,
            snacksCalories         = s.snacksCalories + if (mealType == "Snacks") mealCalories else 0f
        )
    }
    db.dailyNutritionSummaryDao().upsertSummary(updated)
}