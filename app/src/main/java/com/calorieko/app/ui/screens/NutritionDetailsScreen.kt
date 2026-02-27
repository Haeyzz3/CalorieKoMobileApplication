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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDetailsScreen(onBackClick: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calories", "Nutrients", "Macros")

    // Shared date navigation state
    var viewMode by remember { mutableStateOf("day") }
    var showViewDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dayOffset by remember { mutableIntStateOf(0) }
    var weekOffset by remember { mutableIntStateOf(0) }

    val today = LocalDate.now()
    val dateText = if (viewMode == "day") {
        if (dayOffset == 0) "Today"
        else today.plusDays(dayOffset.toLong())
            .format(DateTimeFormatter.ofPattern("EEEE MMM d", Locale.ENGLISH))
    } else {
        val weekStart = today.plusWeeks(weekOffset.toLong()).with(java.time.DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        val startStr = weekStart.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        val endStr = if (weekStart.month == weekEnd.month)
            weekEnd.format(DateTimeFormatter.ofPattern("d", Locale.ENGLISH))
        else weekEnd.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        "$startStr - $endStr"
    }
    val viewLabel = if (viewMode == "day") "Day View" else "Week View"

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
                    titleContentColor = Color(0xFF1F2937)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF1565C0),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF1565C0)
                        )
                    }
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
                                color = if (selectedTabIndex == index) Color(0xFF1565C0) else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // --- Shared Day/Week Navigation Bar ---
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
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
                            if (viewMode == "day") dayOffset-- else weekOffset--
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Previous",
                                tint = Color(0xFF616161)
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
                                        color = Color(0xFF616161),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF616161),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = dateText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF212121)
                                )
                            }

                            DropdownMenu(
                                expanded = showViewDropdown,
                                onDismissRequest = { showViewDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Day", modifier = Modifier.fillMaxWidth(),
                                            fontSize = 15.sp, color = Color(0xFF616161))
                                    },
                                    onClick = { viewMode = "day"; showViewDropdown = false }
                                )
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                DropdownMenuItem(
                                    text = {
                                        Text("Week", modifier = Modifier.fillMaxWidth(),
                                            fontSize = 15.sp, color = Color(0xFF616161))
                                    },
                                    onClick = { viewMode = "week"; showViewDropdown = false }
                                )
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                DropdownMenuItem(
                                    text = {
                                        Text("Pick a Date", modifier = Modifier.fillMaxWidth(),
                                            fontSize = 15.sp, color = Color(0xFF616161))
                                    },
                                    onClick = { showViewDropdown = false; showDatePicker = true }
                                )
                            }
                        }

                        IconButton(onClick = {
                            if (viewMode == "day") dayOffset++ else weekOffset++
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Next",
                                tint = Color(0xFF616161)
                            )
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> CaloriesTabContent(viewMode = viewMode)
                1 -> NutrientsTabContent()
                2 -> MacrosTabContent(viewMode = viewMode)
            }
        }
    }

    // --- Date Picker Dialog ---
    val context = androidx.compose.ui.platform.LocalContext.current
    if (showDatePicker) {
        val todayForPicker = LocalDate.now()
        val calendar = Calendar.getInstance()
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    dayOffset = (pickedDate.toEpochDay() - todayForPicker.toEpochDay()).toInt()
                    viewMode = "day"
                    showDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnCancelListener { showDatePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}