package com.calorieko.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.viewmodel.NutritionDetailsViewModel
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDetailsScreen(viewModel: NutritionDetailsViewModel, onBackClick: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calories", "Nutrients", "Macros", "Activities")

    // ── Collect ViewModel State ──
    val viewMode by viewModel.viewMode.collectAsState()
    val dayOffset by viewModel.dayOffset.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val targets by viewModel.targets.collectAsState()
    val daySummary by viewModel.daySummary.collectAsState()
    val weekSummaries by viewModel.weekSummaries.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()

    // ── Local UI State ──
    var showViewDropdown by remember { mutableStateOf(false) }
    val showDatePicker = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ── Derived Date Display ──
    val today = LocalDate.now()
    val selectedDate = today.plusDays(dayOffset.toLong())

    val dateText = if (viewMode == "day") {
        if (dayOffset == 0) "Today"
        else selectedDate.format(DateTimeFormatter.ofPattern("EEEE MMM d", Locale.ENGLISH))
    } else {
        val weekStart = today.plusWeeks(weekOffset.toLong()).with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        val startStr = weekStart.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        val endStr = if (weekStart.month == weekEnd.month)
            weekEnd.format(DateTimeFormatter.ofPattern("d", Locale.ENGLISH))
        else weekEnd.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        "$startStr - $endStr"
    }
    val viewLabel = if (viewMode == "day") "Day View" else "Week View"

    // Build day-by-day list for the selected week (Mon→Sun, 7 slots)
    val weekStartDate = today.plusWeeks(weekOffset.toLong()).with(DayOfWeek.MONDAY)
    val weekDaySummaries = remember(weekSummaries, weekOffset) {
        val map = weekSummaries.associateBy { it.dateEpochDay }
        (0L..6L).map { i ->
            val date = weekStartDate.plusDays(i)
            map[date.toEpochDay()]
        }
    }

    val weekDayLabels = remember(weekOffset) {
        (0L..6L).map { i ->
            weekStartDate.plusDays(i).format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
        } + "Avg"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DarkText
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IceGray)
                .padding(paddingValues)
        ) {
            // Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = CalorieKoGreen,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = CalorieKoGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title.uppercase(),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) CalorieKoGreen else SubtleText,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // --- Shared Day/Week Navigation Bar ---
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            if (viewMode == "day") viewModel.decrementDayOffset() else viewModel.decrementWeekOffset()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Previous",
                                tint = CalorieKoGreen
                            )
                        }

                        Box {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { showViewDropdown = true }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = viewLabel,
                                        fontSize = 13.sp,
                                        color = SubtleText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = SubtleText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = dateText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkText
                                )
                            }

                            DropdownMenu(
                                expanded = showViewDropdown,
                                onDismissRequest = { showViewDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Day", modifier = Modifier.fillMaxWidth(),
                                            fontSize = 15.sp, color = SubtleText)
                                    },
                                    onClick = { viewModel.setViewMode("day"); showViewDropdown = false }
                                )
                                HorizontalDivider(color = DividerGray)
                                DropdownMenuItem(
                                    text = {
                                        Text("Week", modifier = Modifier.fillMaxWidth(),
                                            fontSize = 15.sp, color = SubtleText)
                                    },
                                    onClick = { viewModel.setViewMode("week"); showViewDropdown = false }
                                )
                                HorizontalDivider(color = DividerGray)
                                DropdownMenuItem(
                                    text = {
                                        Text("Pick a Date", modifier = Modifier.fillMaxWidth(),
                                            fontSize = 15.sp, color = SubtleText)
                                    },
                                    onClick = { showViewDropdown = false; showDatePicker.value = true }
                                )
                            }
                        }

                        IconButton(onClick = {
                            if (viewMode == "day") viewModel.incrementDayOffset() else viewModel.incrementWeekOffset()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Next",
                                tint = CalorieKoGreen
                            )
                        }
                    }
                }
            }

            // Tab Content — pass real data
            when (selectedTabIndex) {
                0 -> CaloriesTabContent(
                    viewMode = viewMode,
                    daySummary = daySummary,
                    goalCalories = targets?.targetCalories ?: 2000,
                    weekDaySummaries = weekDaySummaries,
                    weekDayLabels = weekDayLabels
                )
                1 -> NutrientsTabContent(
                    viewMode = viewMode,
                    daySummary = daySummary,
                    targets = targets,
                    weekDaySummaries = weekDaySummaries
                )
                2 -> MacrosTabContent(
                    viewMode = viewMode,
                    daySummary = daySummary,
                    targetProtein = targets?.targetProtein ?: 150,
                    targetCarbs = targets?.targetCarbs ?: 200,
                    targetFats = targets?.targetFats ?: 65,
                    weekDaySummaries = weekDaySummaries,
                    weekDayLabels = weekDayLabels
                )
                3 -> ActivityHistoryTabContent(
                    activityLogs = activityLogs,
                    viewMode = viewMode,
                    dateText = dateText
                )
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker.value) {
        val calendar = Calendar.getInstance()
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    viewModel.pickDate(pickedDate)
                    showDatePicker.value = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnCancelListener { showDatePicker.value = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Epic 6 — Activity History Tab Content
// Renders a day's workout logs: icon, name, calories, duration.
// ────────────────────────────────────────────────────────────────────
@Composable
fun ActivityHistoryTabContent(
    activityLogs: List<ActivityLogEntity>,
    viewMode: String,
    dateText: String
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    if (viewMode == "week") {
        // Show a placeholder for week view — per-day breakdown not yet implemented
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Switch to Day View",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
                Text(
                    "Activity history shows per-day workouts only.",
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Workouts — $dateText",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (activityLogs.isEmpty()) Color(0xFFF3F4F6) else Color(0xFFDCFCE7)
                ) {
                    Text(
                        "${activityLogs.size} logged",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activityLogs.isEmpty()) Color(0xFF6B7280) else Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (activityLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(40.dp)
                        )
                        Text("No workouts logged", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        Text("Log a workout from the Dashboard to see it here.",
                            fontSize = 12.sp, color = Color(0xFF9CA3AF),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activityLogs) { log ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dynamic icon based on activity name
                        val iconVector = getActivityIcon(log.name)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFFEDD5), Color(0xFFFED7AA))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                iconVector,
                                contentDescription = log.name,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                log.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                timeFormatter.format(Date(log.timestamp)),
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            if (!log.weightOrDuration.isBlank()) {
                                Text(
                                    log.weightOrDuration,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        // Calories badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFF7ED)
                        ) {
                            Text(
                                "-${log.calories} kcal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Summary footer
            item {
                val totalCalories = activityLogs.sumOf { it.calories }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                            Text("Total burned today", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF15803D))
                        }
                        Text(
                            "$totalCalories kcal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}