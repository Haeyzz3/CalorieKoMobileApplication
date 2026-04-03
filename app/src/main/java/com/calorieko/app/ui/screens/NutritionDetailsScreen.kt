package com.calorieko.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.calorieko.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.viewmodel.NutritionDetailsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDetailsScreen(viewModel: NutritionDetailsViewModel, onBackClick: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calories", "Nutrients", "Macros")

    // ── Collect ViewModel State ──
    val viewMode by viewModel.viewMode.collectAsState()
    val dayOffset by viewModel.dayOffset.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val targets by viewModel.targets.collectAsState()
    val daySummary by viewModel.daySummary.collectAsState()
    val weekSummaries by viewModel.weekSummaries.collectAsState()

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