package com.calorieko.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.viewmodel.ProgressViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ==================== DATA MODELS ====================

private data class DayCalorieData(val dayLabel: String, val intake: Int, val burned: Int)
private data class DaySodiumData(val dayLabel: String, val sodium: Int)
private data class DayWeightData(val dayLabel: String, val weight: Double)
private data class TopFoodItem(val name: String, val frequency: Int, val avgCalories: Int, val avgSodium: Int)

/**
 * One row in the Entries history list (one calendar day aggregated).
 */
private data class DayEntry(
    val timestamp: Long,          // day-start epoch millis
    val fullDate: String,         // e.g. "Monday, April 13, 2026"
    val dayName: String,          // e.g. "Monday"
    val intakeCalories: Int,
    val burnedCalories: Int,
    val sodium: Int
)

// ==================== CHART DATA BUILDERS ====================
// These replace the old inline viewMode == "weekly"/"monthly" checks
// that never matched the real viewMode strings ("7_days", "30_days", "90_days").

/**
 * Builds calorie chart points from raw logs, grouped by day / week / month
 * depending on [viewMode].  Labels use real calendar dates (e.g. "4/13").
 */
private fun buildCalorieChartData(
    logs: List<ActivityLogEntity>,
    viewMode: String
): List<DayCalorieData> = when (viewMode) {
    "7_days" -> (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 86_400_000L
        val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        val dayLogs = logs.filter { it.timestamp in dayStart until dayEnd }
        DayCalorieData(
            label,
            dayLogs.filter { it.type == "meal" }.sumOf { it.calories },
            dayLogs.filter { it.type == "workout" }.sumOf { it.calories }
        )
    }
    "30_days" -> (3 downTo 0).map { weeksAgo ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        val weekStart = cal.timeInMillis
        val weekEnd = weekStart + 7 * 86_400_000L
        val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        val weekLogs = logs.filter { it.timestamp in weekStart until weekEnd }
        DayCalorieData(
            label,
            weekLogs.filter { it.type == "meal" }.sumOf { it.calories } / 7,
            weekLogs.filter { it.type == "workout" }.sumOf { it.calories } / 7
        )
    }
    "90_days" -> (2 downTo 0).map { monthsAgo ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -monthsAgo)
        val monthStart = cal.timeInMillis
        val label = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(monthStart))
        val nextCal = cal.clone() as Calendar
        nextCal.add(Calendar.MONTH, 1)
        val monthEnd = nextCal.timeInMillis
        val daysInMonth = ((monthEnd - monthStart) / 86_400_000L).toInt().coerceAtLeast(1)
        val monthLogs = logs.filter { it.timestamp in monthStart until monthEnd }
        DayCalorieData(
            label,
            monthLogs.filter { it.type == "meal" }.sumOf { it.calories } / daysInMonth,
            monthLogs.filter { it.type == "workout" }.sumOf { it.calories } / daysInMonth
        )
    }
    else -> emptyList()
}

private fun buildSodiumChartData(
    logs: List<ActivityLogEntity>,
    viewMode: String
): List<DaySodiumData> = when (viewMode) {
    "7_days" -> (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 86_400_000L
        val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        val dayLogs = logs.filter { it.timestamp in dayStart until dayEnd }
        DaySodiumData(label, dayLogs.filter { it.type == "meal" }.sumOf { it.sodium })
    }
    "30_days" -> (3 downTo 0).map { weeksAgo ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        val weekStart = cal.timeInMillis
        val weekEnd = weekStart + 7 * 86_400_000L
        val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        val weekLogs = logs.filter { it.timestamp in weekStart until weekEnd }
        DaySodiumData(label, weekLogs.filter { it.type == "meal" }.sumOf { it.sodium } / 7)
    }
    "90_days" -> (2 downTo 0).map { monthsAgo ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -monthsAgo)
        val monthStart = cal.timeInMillis
        val label = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(monthStart))
        val nextCal = cal.clone() as Calendar
        nextCal.add(Calendar.MONTH, 1)
        val monthEnd = nextCal.timeInMillis
        val daysInMonth = ((monthEnd - monthStart) / 86_400_000L).toInt().coerceAtLeast(1)
        val monthLogs = logs.filter { it.timestamp in monthStart until monthEnd }
        DaySodiumData(label, monthLogs.filter { it.type == "meal" }.sumOf { it.sodium } / daysInMonth)
    }
    else -> emptyList()
}

private fun buildWeightChartData(
    userWeight: Double,
    viewMode: String
): List<DayWeightData> = when (viewMode) {
    "7_days" -> (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        DayWeightData(label, userWeight)
    }
    "30_days" -> (3 downTo 0).map { weeksAgo ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        DayWeightData(label, userWeight)
    }
    "90_days" -> (2 downTo 0).map { monthsAgo ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, -monthsAgo)
        val label = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(cal.timeInMillis))
        DayWeightData(label, userWeight)
    }
    else -> emptyList()
}

/**
 * Aggregates raw logs by calendar day for the "Entries" history list.
 * Sorted newest first, matching the MyFitnessPal pattern.
 */
private fun buildDayEntries(logs: List<ActivityLogEntity>): List<DayEntry> {
    val fullDateFmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val dayNameFmt = SimpleDateFormat("EEEE", Locale.getDefault())
    return logs
        .groupBy { log ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = log.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        .map { (dayTs, dayLogs) ->
            DayEntry(
                timestamp = dayTs,
                fullDate = fullDateFmt.format(Date(dayTs)),
                dayName = dayNameFmt.format(Date(dayTs)),
                intakeCalories = dayLogs.filter { it.type == "meal" }.sumOf { it.calories },
                burnedCalories = dayLogs.filter { it.type == "workout" }.sumOf { it.calories },
                sodium = dayLogs.filter { it.type == "meal" }.sumOf { it.sodium }
            )
        }
        .sortedByDescending { it.timestamp }
}

// ==================== MAIN SCREEN ====================

@Composable
fun ProgressScreen(viewModel: ProgressViewModel, onNavigate: (String) -> Unit) {
    // ── Collect ViewModel State ──
    val weeklyLogs by viewModel.weeklyLogs.collectAsState()
    val userWeight by viewModel.userWeight.collectAsState()
    val selectedMetric by viewModel.selectedMetric.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val dataLoaded by viewModel.dataLoaded.collectAsState()

    var activeTab by remember { mutableStateOf("progress") }

    // ── Chart data — keyed on both logs AND viewMode so graph updates on range change ──
    val calorieData = remember(weeklyLogs, viewMode) { buildCalorieChartData(weeklyLogs, viewMode) }
    val sodiumData = remember(weeklyLogs, viewMode) { buildSodiumChartData(weeklyLogs, viewMode) }
    val weightData = remember(userWeight, viewMode) { buildWeightChartData(userWeight, viewMode) }
    val topFoods = remember(weeklyLogs) {
        weeklyLogs.filter { it.type == "meal" }
            .groupBy { it.name }
            .map { (name, logs) ->
                TopFoodItem(
                    name = name,
                    frequency = logs.size,
                    avgCalories = if (logs.isNotEmpty()) logs.sumOf { it.calories } / logs.size else 0,
                    avgSodium = if (logs.isNotEmpty()) logs.sumOf { it.sodium } / logs.size else 0
                )
            }
            .sortedByDescending { it.frequency }
            .take(5)
    }

    // ── Entries history — one row per calendar day ──
    val dayEntries = remember(weeklyLogs) { buildDayEntries(weeklyLogs) }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                if (it != "progress") onNavigate(it)
            })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F5))
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ──
            item {
                ProgressHeaderSection(
                    selectedMetric = selectedMetric,
                    onMetricChange = { viewModel.setMetric(it) },
                    dateRange = viewMode,
                    onDateRangeChange = { viewModel.setViewMode(it) }
                )
            }

            // ── Chart ──
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AnimatedVisibility(
                        visible = dataLoaded,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                    ) {
                        when (selectedMetric) {
                            "Calorie Balance" -> CalorieBalanceCard(data = calorieData, viewMode = viewMode)
                            "Sodium Trend" -> SodiumTrendCard(data = sodiumData, dailyLimit = 2300, viewMode = viewMode)
                            "Weight & Body Metrics" -> WeightTrackingCard(data = weightData)
                            "Dietary Insights" -> DietaryInsightsCard(foods = topFoods)
                        }
                    }
                }
            }

            // ── Entries History (MFP-style) ──
            item {
                if (dataLoaded && selectedMetric != "Dietary Insights") {
                    EntriesHistorySection(
                        entries = dayEntries,
                        selectedMetric = selectedMetric,
                        userWeight = userWeight
                    )
                }
            }
        }
    }
}

// ==================== HEADER with PREMIUM DROPDOWNS ====================

@Composable
private fun ProgressHeaderSection(
    selectedMetric: String,
    onMetricChange: (String) -> Unit,
    dateRange: String,
    onDateRangeChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Your Progress",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Track, analyse, improve.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Metric picker ──
                StyledDropdownButton(
                    label = "METRIC",
                    value = selectedMetric,
                    options = listOf(
                        "Calorie Balance" to "Calorie Balance",
                        "Sodium Trend" to "Sodium Trend",
                        "Weight & Body Metrics" to "Weight & Body",
                        "Dietary Insights" to "Dietary Insights"
                    ),
                    selectedKey = selectedMetric,
                    onSelect = onMetricChange,
                    modifier = Modifier.weight(1f)
                )

                // ── Date-range picker ──
                StyledDropdownButton(
                    label = "RANGE",
                    value = when (dateRange) {
                        "30_days" -> "Last 30 Days"
                        "90_days" -> "Last 90 Days"
                        else -> "Last 7 Days"
                    },
                    options = listOf(
                        "7_days" to "Last 7 Days",
                        "30_days" to "Last 30 Days",
                        "90_days" to "Last 90 Days"
                    ),
                    selectedKey = dateRange,
                    onSelect = onDateRangeChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ==================== PREMIUM BOTTOM-SHEET DROPDOWN ====================

/**
 * A sleek pill-button that opens a MyFitnessPal-style dark bottom-sheet
 * picker when tapped.
 */
@Composable
private fun StyledDropdownButton(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (showSheet) 180f else 0f,
        animationSpec = tween(200),
        label = "arrow"
    )

    // ── Trigger pill ──
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showSheet = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF6C63FF).copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF6C63FF),
                modifier = Modifier
                    .size(18.dp)
                    .rotate(arrowRotation)
            )
        }
    }

    // ── Bottom-sheet Dialog ──
    if (showSheet) {
        Dialog(
            onDismissRequest = { showSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Semi-transparent scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showSheet = false }
                )

                // Sheet content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color(0xFF1C1C2E))
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 14.dp, bottom = 6.dp)
                            .width(44.dp)
                            .height(4.dp)
                            .background(Color(0xFF3D3D5C), RoundedCornerShape(2.dp))
                    )

                    // Header row: X · title · ✓
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSheet = false }) {
                            Icon(Icons.Filled.Close, null, tint = Color(0xFF8B8FA8))
                        }
                        Text(
                            text = "Select $label",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        IconButton(onClick = { showSheet = false }) {
                            Icon(Icons.Filled.Check, null, tint = Color(0xFF6C63FF))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2A2A40), thickness = 1.dp)

                    // Options list
                    options.forEach { (key, display) ->
                        val isSelected = key == selectedKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(key)
                                    showSheet = false
                                }
                                .background(
                                    if (isSelected)
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF6C63FF).copy(alpha = 0.25f),
                                                Color(0xFF6C63FF).copy(alpha = 0.05f)
                                            )
                                        )
                                    else
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color.Transparent)
                                        )
                                )
                                .padding(horizontal = 28.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left accent bar for selected
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .background(Color(0xFF6C63FF), RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(16.dp))
                            }

                            Text(
                                text = display,
                                fontSize = if (isSelected) 17.sp else 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF8B8FA8),
                                modifier = if (!isSelected) Modifier.padding(start = 19.dp) else Modifier
                            )

                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF6C63FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

// ==================== CALORIE BALANCE CHART ====================

@Composable
private fun CalorieBalanceCard(data: List<DayCalorieData>, viewMode: String) {
    val maxValue = (data.maxOfOrNull { maxOf(it.intake, it.burned) } ?: 2200).coerceAtLeast(100)
    val averageTDEE = if (data.isNotEmpty()) {
        data.sumOf { it.intake - it.burned } / data.size
    } else 0
    val yMax = ((maxValue / 550) + 1) * 550

    val subtitleText = when (viewMode) {
        "30_days" -> "Weekly Average Intake vs. Output"
        "90_days" -> "Monthly Average Intake vs. Output"
        else -> "Daily Intake vs. Output"
    }

    val footerLabel = when (viewMode) {
        "30_days" -> "Weekly Net Average"
        "90_days" -> "Monthly Net Average"
        else -> "Daily Net Average"
    }

    // Animate bars growing up
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Calorie Balance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitleText, fontSize = 12.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(16.dp))

            val density = LocalDensity.current
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val chartW = size.width; val chartH = size.height
                val leftPad = with(density) { 36.dp.toPx() }
                val bottomPad = with(density) { 24.dp.toPx() }
                val drawW = chartW - leftPad; val drawH = chartH - bottomPad
                val progress = animProgress.value

                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                }
                val yLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
                }

                // Grid lines
                val ySteps = 4
                for (i in 0..ySteps) {
                    val value = (yMax.toFloat() / ySteps) * i
                    val y = drawH - (drawH * (value / yMax.toFloat()))
                    drawLine(Color(0xFFF1F5F9), Offset(leftPad, y), Offset(chartW, y), 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    drawContext.canvas.nativeCanvas.drawText(
                        value.roundToInt().toString(),
                        leftPad - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() },
                        yLabelPaint
                    )
                }

                val barGroupW = drawW / data.size
                val barW = barGroupW * 0.28f
                val gap = barGroupW * 0.04f
                val cornerR = with(density) { 4.dp.toPx() }

                data.forEachIndexed { index, day ->
                    val cx = leftPad + barGroupW * (index + 0.5f)
                    val intakeH = (day.intake.toFloat() / yMax) * drawH * progress
                    if (intakeH > 0) drawRoundRect(Color(0xFF4CAF50),
                        Offset(cx - barW - gap / 2, drawH - intakeH), Size(barW, intakeH),
                        CornerRadius(cornerR, cornerR))
                    val burnedH = (day.burned.toFloat() / yMax) * drawH * progress
                    if (burnedH > 0) drawRoundRect(Color(0xFFFF9800),
                        Offset(cx + gap / 2, drawH - burnedH), Size(barW, burnedH),
                        CornerRadius(cornerR, cornerR))
                    drawContext.canvas.nativeCanvas.drawText(
                        day.dayLabel, cx, chartH - with(density) { 4.dp.toPx() }, labelPaint)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Color(0xFF4CAF50), CircleShape))
                Spacer(Modifier.width(6.dp)); Text("Intake", fontSize = 12.sp, color = Color(0xFF6B7280))
                Spacer(Modifier.width(20.dp))
                Box(Modifier.size(10.dp).background(Color(0xFFFF9800), CircleShape))
                Spacer(Modifier.width(6.dp)); Text("Burned", fontSize = 12.sp, color = Color(0xFF6B7280))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(footerLabel, fontSize = 13.sp, color = Color(0xFF94A3B8))
                Text("$averageTDEE kcal", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A))
            }
        }
    }
}

// ==================== SODIUM TREND CHART ====================

@Composable
private fun SodiumTrendCard(data: List<DaySodiumData>, dailyLimit: Int, viewMode: String) {
    val daysOverLimit = data.count { it.sodium > dailyLimit }
    val average = if (data.isNotEmpty()) data.sumOf { it.sodium } / data.size else 0
    val maxSodium = (data.maxOfOrNull { it.sodium } ?: 3000).coerceAtLeast(dailyLimit + 500)
    val yMax = ((maxSodium / 500) + 1) * 500

    val footerLabel = when (viewMode) {
        "30_days" -> "Weekly Avg Sodium"
        "90_days" -> "Monthly Avg Sodium"
        else -> "Daily Avg Sodium"
    }

    val lineProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        lineProgress.snapTo(0f)
        lineProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Column {
                    Text("Sodium Trend", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A))
                    Spacer(Modifier.height(2.dp))
                    Text("Hypertension Monitoring", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
                if (daysOverLimit > 0) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, Modifier.size(12.dp), Color(0xFFEF4444))
                        Spacer(Modifier.width(4.dp))
                        Text("$daysOverLimit day${if (daysOverLimit > 1) "s" else ""} over limit",
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val density = LocalDensity.current
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val chartW = size.width; val chartH = size.height
                val leftPad = with(density) { 40.dp.toPx() }
                val bottomPad = with(density) { 24.dp.toPx() }
                val drawW = chartW - leftPad; val drawH = chartH - bottomPad
                val progress = lineProgress.value

                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                }
                val yLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
                }
                val limitLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#EF4444")
                    textSize = with(density) { 9.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
                }

                for (i in 0..4) {
                    val value = (yMax.toFloat() / 4) * i
                    val y = drawH - (drawH * (value / yMax.toFloat()))
                    drawLine(Color(0xFFF1F5F9), Offset(leftPad, y), Offset(chartW, y), 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    drawContext.canvas.nativeCanvas.drawText(
                        value.roundToInt().toString(),
                        leftPad - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() }, yLabelPaint)
                }

                val limitY = drawH - (drawH * (dailyLimit.toFloat() / yMax.toFloat()))
                drawLine(Color(0xFFEF4444), Offset(leftPad, limitY), Offset(chartW, limitY),
                    with(density) { 2.dp.toPx() }, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                drawContext.canvas.nativeCanvas.drawText("Limit: ${dailyLimit}mg",
                    chartW - with(density) { 4.dp.toPx() },
                    limitY - with(density) { 6.dp.toPx() }, limitLabelPaint)

                if (data.isNotEmpty()) {
                    val points = data.mapIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        val y = drawH - (drawH * (day.sodium.toFloat() / yMax.toFloat()))
                        Offset(x, y)
                    }
                    val totalPoints = points.size
                    val drawnCount = (totalPoints * progress).toInt().coerceAtLeast(1)
                    val path = Path()
                    for (i in 0 until drawnCount.coerceAtMost(totalPoints)) {
                        if (i == 0) path.moveTo(points[i].x, points[i].y)
                        else path.lineTo(points[i].x, points[i].y)
                    }
                    if (drawnCount < totalPoints && progress > 0) {
                        val frac = (totalPoints * progress) - drawnCount
                        if (frac > 0 && drawnCount < totalPoints) {
                            val from = points[drawnCount - 1]; val to = points[drawnCount]
                            path.lineTo(from.x + (to.x - from.x) * frac, from.y + (to.y - from.y) * frac)
                        }
                    }
                    drawPath(path, Color(0xFFFF9800), style = Stroke(with(density) { 3.dp.toPx() }, cap = StrokeCap.Round))
                    for (i in 0 until drawnCount.coerceAtMost(totalPoints)) {
                        drawCircle(Color(0xFFFF9800), with(density) { 5.dp.toPx() }, points[i])
                        drawCircle(Color.White, with(density) { 2.5.dp.toPx() }, points[i])
                    }
                    data.forEachIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        drawContext.canvas.nativeCanvas.drawText(
                            day.dayLabel, x, chartH - with(density) { 4.dp.toPx() }, labelPaint)
                    }
                }
            }

            Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(footerLabel, fontSize = 13.sp, color = Color(0xFF94A3B8))
                Text("$average mg", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = if (average > dailyLimit) Color(0xFFDC2626) else Color(0xFF16A34A))
            }
        }
    }
}

// ==================== WEIGHT & BODY METRICS ====================

@Composable
private fun WeightTrackingCard(data: List<DayWeightData>) {
    val startWeight = data.firstOrNull()?.weight ?: 0.0
    val endWeight = data.lastOrNull()?.weight ?: 0.0
    val weightChange = endWeight - startWeight
    val weightChangeAbs = abs(weightChange)
    val trend = when {
        weightChange < -0.05 -> "down"
        weightChange > 0.05 -> "up"
        else -> "stable"
    }
    val minW = (data.minOfOrNull { it.weight } ?: 70.0) - 2.0
    val maxW = (data.maxOfOrNull { it.weight } ?: 76.0) + 2.0
    val range = (maxW - minW).coerceAtLeast(0.1)

    val lineProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        lineProgress.snapTo(0f)
        lineProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Weight & Body Metrics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp)); Text("Weight Changes Over Time", fontSize = 12.sp, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(16.dp))

            val density = LocalDensity.current
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val chartW = size.width; val chartH = size.height
                val leftPad = with(density) { 40.dp.toPx() }
                val bottomPad = with(density) { 24.dp.toPx() }
                val drawW = chartW - leftPad; val drawH = chartH - bottomPad
                val progress = lineProgress.value

                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                }
                val yLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
                }

                for (i in 0..3) {
                    val value = minW + (range / 3) * i
                    val y = drawH - (drawH * ((value - minW) / range)).toFloat()
                    drawLine(Color(0xFFF1F5F9), Offset(leftPad, y), Offset(chartW, y), 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", value),
                        leftPad - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() }, yLabelPaint)
                }

                if (data.isNotEmpty()) {
                    val points = data.mapIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        val y = drawH - (drawH * ((day.weight - minW) / range)).toFloat()
                        Offset(x, y)
                    }
                    val totalPts = points.size
                    val drawnCount = (totalPts * progress).toInt().coerceAtLeast(1)
                    val path = Path()
                    for (i in 0 until drawnCount.coerceAtMost(totalPts)) {
                        if (i == 0) path.moveTo(points[i].x, points[i].y)
                        else path.lineTo(points[i].x, points[i].y)
                    }
                    if (drawnCount < totalPts && progress > 0) {
                        val frac = (totalPts * progress) - drawnCount
                        if (frac > 0 && drawnCount < totalPts) {
                            val from = points[drawnCount - 1]; val to = points[drawnCount]
                            path.lineTo(from.x + (to.x - from.x) * frac, from.y + (to.y - from.y) * frac)
                        }
                    }
                    drawPath(path, Color(0xFF4CAF50), style = Stroke(with(density) { 3.dp.toPx() }, cap = StrokeCap.Round))
                    for (i in 0 until drawnCount.coerceAtMost(totalPts)) {
                        drawCircle(Color(0xFF4CAF50), with(density) { 5.dp.toPx() }, points[i])
                        drawCircle(Color.White, with(density) { 2.5.dp.toPx() }, points[i])
                    }
                    data.forEachIndexed { index, day ->
                        val x = leftPad + (drawW / (data.size - 1).coerceAtLeast(1)) * index
                        drawContext.canvas.nativeCanvas.drawText(
                            day.dayLabel, x, chartH - with(density) { 4.dp.toPx() }, labelPaint)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            val insightBg = when (trend) { "down" -> Color(0xFFF0FDF4); "up" -> Color(0xFFFFF7ED); else -> Color(0xFFF9FAFB) }
            val insightBorder = when (trend) { "down" -> Color(0xFFBBF7D0); "up" -> Color(0xFFFED7AA); else -> Color(0xFFE5E7EB) }
            val insightTextColor = when (trend) { "down" -> Color(0xFF15803D); "up" -> Color(0xFFC2410C); else -> Color(0xFF374151) }
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = insightBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, insightBorder),
                modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    when (trend) {
                        "down" -> Icon(Icons.AutoMirrored.Outlined.TrendingDown, null, Modifier.size(18.dp), Color(0xFF16A34A))
                        "up"   -> Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, Modifier.size(18.dp), Color(0xFFEA580C))
                        else   -> Icon(Icons.Outlined.TipsAndUpdates, null, Modifier.size(18.dp), Color(0xFF6B7280))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Insight", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = insightTextColor)
                        Spacer(Modifier.height(2.dp))
                        Text(when (trend) {
                            "down" -> "You're down ${String.format("%.1f", weightChangeAbs)}kg—great progress!"
                            "up"   -> "Weight increased by ${String.format("%.1f", weightChangeAbs)}kg."
                            else   -> "Your weight has remained stable."
                        }, fontSize = 13.sp, color = insightTextColor, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// ==================== DIETARY INSIGHTS ====================

@Composable
private fun DietaryInsightsCard(foods: List<TopFoodItem>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Dietary Insights", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp)); Text("Most Frequently Logged Foods", fontSize = 12.sp, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(16.dp))
            if (foods.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No meals logged in this period", fontSize = 14.sp, color = Color(0xFF94A3B8))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) { foods.forEach { FoodInsightCard(food = it) } }
            }
            Spacer(Modifier.height(14.dp))
            Text("💡 Tip: Identifying high-sodium foods helps manage hypertension",
                fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun FoodInsightCard(food: TopFoodItem) {
    val isHighSodium = food.avgSodium > 800
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.width(192.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (isHighSodium) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Warning, "High sodium", Modifier.size(16.dp), Color(0xFFF97316))
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.background(Color.White, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Logged ${food.frequency}x", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Avg. Calories", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text("${food.avgCalories} kcal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Avg. Sodium", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text("${food.avgSodium} mg", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isHighSodium) Color(0xFFEA580C) else Color(0xFF374151))
            }
            if (isHighSodium) {
                Spacer(Modifier.height(10.dp)); HorizontalDivider(color = Color(0xFFE2E8F0)); Spacer(Modifier.height(8.dp))
                Text("High sodium content", fontSize = 11.sp, color = Color(0xFFEA580C))
            }
        }
    }
}

// ==================== ENTRIES HISTORY (MFP-style) ====================

/**
 * Shows a MyFitnessPal-style daily history list below the chart.
 * Each row = one calendar day with a full date, day-name sub-label,
 * and the relevant metric value on the right.
 */
@Composable
private fun EntriesHistorySection(
    entries: List<DayEntry>,
    selectedMetric: String,
    userWeight: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section header
        Text(
            text = "Entries",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (entries.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No entries for this period",
                            fontSize = 14.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
            return
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, entry ->
                    EntryRow(
                        entry = entry,
                        selectedMetric = selectedMetric,
                        userWeight = userWeight
                    )
                    if (index < entries.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: DayEntry,
    selectedMetric: String,
    userWeight: Double
) {
    // Compute the display value + unit based on the active metric
    val (valueText, unitText, valueColor) = when (selectedMetric) {
        "Calorie Balance" -> {
            val net = (entry.intakeCalories - entry.burnedCalories).coerceAtLeast(0)
            Triple(
                "%,d".format(net),
                "kcal",
                if (entry.intakeCalories > 0) Color(0xFF0F172A) else Color(0xFF94A3B8)
            )
        }
        "Sodium Trend" -> {
            Triple(
                "%,d".format(entry.sodium),
                "mg",
                if (entry.sodium > 2300) Color(0xFFDC2626) else Color(0xFF0F172A)
            )
        }
        "Weight & Body Metrics" -> Triple(
            String.format("%.1f", userWeight),
            "kg",
            Color(0xFF0F172A)
        )
        else -> Triple("—", "", Color(0xFF94A3B8))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.fullDate,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.dayName,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = valueText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.End
            )
            if (unitText.isNotEmpty()) {
                Text(
                    text = unitText,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
