package com.calorieko.app.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import com.calorieko.app.data.remote.ImageUtils
import com.calorieko.app.ble.BleConnectionState
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.ActivityLogEntry
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogWithItems
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.components.ExpandableNutrientGrid
import com.calorieko.app.ui.components.NutrientChip
import com.calorieko.app.ui.components.ProgressRings
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, bleScaleManager: BleScaleManager, onNavigate: (String) -> Unit) {

    // ── Collect ViewModel State ──
    val firstName by viewModel.userName.collectAsState()
    val localPhotoUrl by viewModel.localPhotoUrl.collectAsState()
    val firebaseProfileImageUrl = viewModel.firebaseProfileImageUrl

    val targetCalories by viewModel.targetCalories.collectAsState()
    val targetBurned by viewModel.targetBurned.collectAsState()
    val targetSodium by viewModel.targetSodium.collectAsState()
    val targetProtein by viewModel.targetProtein.collectAsState()
    val targetCarbs by viewModel.targetCarbs.collectAsState()
    val targetFats by viewModel.targetFats.collectAsState()

    val nutritionSummary by viewModel.nutritionSummary.collectAsState()
    val todayWorkoutLogs by viewModel.todayWorkoutLogs.collectAsState()

    val currentCalories = nutritionSummary?.totalCalories?.toInt() ?: 0
    val caloriesBurned = todayWorkoutLogs.sumOf { it.calories }
    val currentSodium = nutritionSummary?.totalSodium?.toInt() ?: 0
    val currentProtein = nutritionSummary?.totalProtein?.toInt() ?: 0
    val currentCarbs = nutritionSummary?.totalCarbs?.toInt() ?: 0
    val currentFats = nutritionSummary?.totalFat?.toInt() ?: 0

    val todayMealLogs by viewModel.todayMealLogs.collectAsState()
    val activityLog by viewModel.activityFeed.collectAsState()

    // ── Local UI State ──
    var activeTab by remember { mutableStateOf("home") }
    var selectedMeal by remember { mutableStateOf<MealLogWithItems?>(null) }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                if (it != "home") {
                    onNavigate(it)
                }
            })
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {

            // --- 1. Header ---
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Profile Picture and Text
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photoModel: Any? = if (localPhotoUrl.isNotEmpty()) {
                            localPhotoUrl
                        } else {
                            firebaseProfileImageUrl
                        }
                        val bitmap = remember(photoModel) {
                            if (photoModel is String && photoModel.startsWith(ImageUtils.BASE64_PREFIX)) {
                                ImageUtils.decodeBase64ToBitmap(photoModel)
                            } else null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onNavigate("profile") },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = photoModel,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onNavigate("profile") },
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Hello, $firstName!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                            Text(
                                text = currentDate,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Right Side: Scale Connected Badge
                    val bleState by bleScaleManager.connectionState.collectAsState()

                    val (badgeBgColor, badgeContentColor, badgeText) = when (bleState) {
                        is BleConnectionState.Connected -> Triple(Color(0xFFECFDF5), Color(0xFF059669), "Scale Connected")
                        is BleConnectionState.Connecting, is BleConnectionState.Scanning -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), "Connecting...")
                        else -> Triple(Color(0xFFF3F4F6), Color(0xFF6B7280), "Scale Disconnected")
                    }

                    Surface(
                        color = badgeBgColor,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = badgeContentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = badgeContentColor
                            )
                        }
                    }
                }
            }

            // --- 2. Main Dashboard Content ---
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // Wrap in a Box to make the entire component clickable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = { onNavigate("nutritionDetails") }
                            )
                    ) {
                        ProgressRings(
                            caloriesCurrent = currentCalories,
                            caloriesTarget = targetCalories,
                            caloriesBurned = caloriesBurned,
                            caloriesBurnedTarget = targetBurned,
                            sodiumCurrent = currentSodium,
                            sodiumTarget = targetSodium,
                            proteinCurrent = currentProtein,
                            proteinTarget = targetProtein,
                            carbsCurrent = currentCarbs,
                            carbsTarget = targetCarbs,
                            fatsCurrent = currentFats,
                            fatsTarget = targetFats
                        )
                    }

                    ActionButtonsRevised(
                        onLogMeal = { onNavigate("logMeal") },
                        onLogWorkout = { onNavigate("logWorkout") }
                    )

                    // Explore Dishes CTA
                    Surface(
                        onClick = { onNavigate("explore") },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF0F9FF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🍽️", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Explore Dishes",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF)
                                )
                                Text(
                                    "Browse all supported dishes & data sources",
                                    fontSize = 12.sp,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            Text("→", fontSize = 18.sp, color = Color(0xFF3B82F6))
                        }
                    }

                    DailyActivityFeedRevised(
                        activities = activityLog,
                        onMealClick = { activityEntry ->
                            // Find the matching MealLogWithItems by ID
                            val mealId = activityEntry.id.toLongOrNull()
                            if (mealId != null) {
                                selectedMeal = todayMealLogs.find { it.mealLog.mealLogId == mealId }
                            }
                        },
                        onWorkoutClick = { activityEntry ->
                            onNavigate("activity_details/${activityEntry.id}")
                        }
                    )
                }
            }
        }
    }

    // --- Meal Detail Bottom Sheet Overlay ---
    selectedMeal?.let { mealWithItems ->
        MealDetailBottomSheet(
            mealWithItems = mealWithItems,
            onDismiss = { selectedMeal = null }
        )
    }
}

// --- REVISED COMPONENTS ---

/**
 * Maps an activity name to an appropriate Material icon.
 * Epic 6: Dynamic activity icons based on workout type.
 * Task 3: Expanded to support localized Filipino lifestyle activities.
 */
fun getActivityIcon(activityName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = activityName.lowercase()
    return when {
        // Cardio / Running
        "running" in lower || "jog" in lower || "takbo" in lower -> Icons.AutoMirrored.Filled.DirectionsRun
        // Cycling
        "biking" in lower || "cycling" in lower || "bike" in lower || "bisikleta" in lower -> Icons.Default.DirectionsBike
        // Swimming
        "swim" in lower || "langoy" in lower -> Icons.Default.Pool
        // Yoga / Stretching
        "yoga" in lower || "stretch" in lower || "meditation" in lower -> Icons.Default.SelfImprovement
        // Basketball
        "basket" in lower -> Icons.Default.SportsBasketball
        // Soccer / Football
        "soccer" in lower || "football" in lower -> Icons.Default.SportsSoccer
        // Hiking / Walking
        "hike" in lower || "trail" in lower || "walk" in lower || "lakad" in lower -> Icons.Default.Hiking
        // Rowing
        "row" in lower || "kayak" in lower -> Icons.Default.Rowing
        // Gym / Weightlifting
        "weight" in lower || "lift" in lower || "gym" in lower || "press" in lower -> Icons.Default.FitnessCenter
        // --- Filipino Localized Activities ---
        // Laundry / Hand-washing clothes
        "laba" in lower || "washing" in lower || "laundry" in lower -> Icons.Default.LocalLaundryService
        // Mopping / Scrubbing (Paglalampaso)
        "lampaso" in lower || "scrub" in lower || "mop" in lower -> Icons.Default.CleaningServices
        // Sweeping / Cleaning
        "sweep" in lower || "clean" in lower || "walis" in lower -> Icons.Default.CleaningServices
        // Fetching water (Pagiigib)
        "igib" in lower || "fetch" in lower -> Icons.Default.WaterDrop
        // Dancing (Sayaw)
        "sayaw" in lower || "danc" in lower || "zumba" in lower -> Icons.Default.MusicNote
        // Gardening / Planting
        "garden" in lower || "plant" in lower || "tanim" in lower -> Icons.Default.NordicWalking
        // Cooking
        "cook" in lower || "luto" in lower -> Icons.Default.LocalFireDepartment
        // Home chores generic fallback
        "chore" in lower || "gawain" in lower -> Icons.Default.Home
        // Boxing / Martial Arts
        "box" in lower || "martial" in lower -> Icons.Default.FitnessCenter
        // Jump rope / Skipping
        "jump" in lower || "skip" in lower || "talon" in lower -> Icons.AutoMirrored.Filled.DirectionsRun
        // Default
        else -> Icons.Default.LocalFireDepartment
    }
}

@Composable
fun ActionButtonsRevised(onLogMeal: () -> Unit, onLogWorkout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            onClick = onLogMeal,
            color = Color(0xFF4CAF50),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Icon(Icons.Default.MonitorWeight, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Log Meal", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("AI + Scale", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

        Surface(
            onClick = onLogWorkout,
            color = Color(0xFFFF9800),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Log Workout", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("Track Activity", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DailyActivityFeedRevised(
    activities: List<ActivityLogEntry>,
    onMealClick: (ActivityLogEntry) -> Unit = {},
    onWorkoutClick: (ActivityLogEntry) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Today's Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (activities.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalDining,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("No activities logged yet", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activities.forEach { activity ->
                        ActivityItemRevised(
                            activity = activity,
                            onClick = if (activity.type == "meal") {
                                { onMealClick(activity) }
                            } else {
                                { onWorkoutClick(activity) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItemRevised(activity: ActivityLogEntry, onClick: (() -> Unit)? = null) {
    val isMeal = activity.type == "meal"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = if (isMeal) Color(0xFFDCFCE7) else Color(0xFFFFEDD5),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMeal) Icons.Default.LocalDining
                                  else getActivityIcon(activity.name),
                    contentDescription = null,
                    tint = if (isMeal) Color(0xFF16A34A) else Color(0xFFEA580C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = activity.time,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMeal) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isMeal) Color(0xFF16A34A) else Color(0xFFEA580C),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${activity.details.calories} cal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }



            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMeal && activity.details.weight != null) {
                    Icon(Icons.Default.MonitorWeight, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activity.details.weight,
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (isMeal && activity.details.sodium != null) {
                    Text(
                        text = "Sodium: ${activity.details.sodium}mg",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                }

                if (!isMeal && activity.details.duration != null) {
                    Text(
                        text = "Duration: ${activity.details.duration}",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Meal Detail Bottom Sheet (shown when a meal feed item is tapped)
// ───────────────────────────────────────────────────────────────

@Composable
fun MealDetailBottomSheet(
    mealWithItems: MealLogWithItems,
    onDismiss: () -> Unit
) {
    val meal = mealWithItems.mealLog
    val items = mealWithItems.items

    val totalCalories = items.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = items.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = items.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = items.sumOf { it.fat.toDouble() }.toFloat()

    // Track expanded state per dish (by index)
    val expandedDishes = remember { mutableStateMapOf<Int, Boolean>() }
    var totalsExpanded by remember { mutableStateOf(false) }

    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val formattedTime = timeFormat.format(java.util.Date(meal.timestamp))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        // Prevent clicks inside the sheet from dismissing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
                .background(Color(0xFFF8F9FA))
        ) {

            // Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Meal Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            "${meal.mealType} • $formattedTime",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    // Placeholder for symmetry
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Dish list + Totals
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Individual dish cards
                itemsIndexed(items) { index, item ->
                    val isExpanded = expandedDishes[index] == true
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.dishName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1F2937)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${item.weightGrams.toInt()}g  •  ${item.calories.fmt()} kcal",
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "P: ${item.protein.fmt()}g  C: ${item.carbs.fmt()}g  F: ${item.fat.fmt()}g",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
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
                            }

                            // Expandable full nutrition details
                            if (isExpanded) {
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                ExpandableNutrientGrid(
                                    fiber = item.fiber,
                                    sugar = item.sugar,
                                    saturatedFat = item.saturatedFat,
                                    polyunsaturatedFat = item.polyunsaturatedFat,
                                    monounsaturatedFat = item.monounsaturatedFat,
                                    transFat = item.transFat,
                                    cholesterol = item.cholesterol,
                                    sodium = item.sodium,
                                    potassium = item.potassium,
                                    vitaminA = item.vitaminA,
                                    vitaminC = item.vitaminC,
                                    calcium = item.calcium,
                                    iron = item.iron
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
                        colors = CardDefaults.cardColors(
                            containerColor = CalorieKoGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Meal Totals",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = CalorieKoGreen.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Calories",
                                    fontSize = 14.sp,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    "${totalCalories.fmt()} kcal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CalorieKoGreen
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
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
                                    fiber = items.sumOf { it.fiber.toDouble() }.toFloat(),
                                    sugar = items.sumOf { it.sugar.toDouble() }.toFloat(),
                                    saturatedFat = items.sumOf { it.saturatedFat.toDouble() }.toFloat(),
                                    polyunsaturatedFat = items.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat(),
                                    monounsaturatedFat = items.sumOf { it.monounsaturatedFat.toDouble() }.toFloat(),
                                    transFat = items.sumOf { it.transFat.toDouble() }.toFloat(),
                                    cholesterol = items.sumOf { it.cholesterol.toDouble() }.toFloat(),
                                    sodium = items.sumOf { it.sodium.toDouble() }.toFloat(),
                                    potassium = items.sumOf { it.potassium.toDouble() }.toFloat(),
                                    vitaminA = items.sumOf { it.vitaminA.toDouble() }.toFloat(),
                                    vitaminC = items.sumOf { it.vitaminC.toDouble() }.toFloat(),
                                    calcium = items.sumOf { it.calcium.toDouble() }.toFloat(),
                                    iron = items.sumOf { it.iron.toDouble() }.toFloat()
                                )
                            }
                        }
                    }
                }
            }

            // Close button at the bottom
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Close",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Float.fmt() = String.format(java.util.Locale.US, "%.1f", this)