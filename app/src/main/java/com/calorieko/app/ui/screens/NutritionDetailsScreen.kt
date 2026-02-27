package com.calorieko.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

            // Tab Content
            when (selectedTabIndex) {
                0 -> CaloriesTabContent()
                1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nutrients Tab (WIP)") }
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Macros Tab (WIP)") }
            }
        }
    }
}

@Composable
fun CaloriesTabContent() {
    // View mode: "day" or "week"
    var viewMode by remember { mutableStateOf("day") }
    var showViewDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Offset state: in day mode, offset by days; in week mode, offset by weeks
    var dayOffset by remember { mutableIntStateOf(0) }
    var weekOffset by remember { mutableIntStateOf(0) }

    // Calculate the displayed date text
    val today = LocalDate.now()
    val dateText = if (viewMode == "day") {
        if (dayOffset == 0) {
            "Today"
        } else {
            val displayDate = today.plusDays(dayOffset.toLong())
            displayDate.format(DateTimeFormatter.ofPattern("EEEE MMM d", Locale.ENGLISH))
        }
    } else {
        // Week view: show range like "Feb 21 - 27"
        val weekStart = today.plusWeeks(weekOffset.toLong())
            .with(java.time.DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        val startStr = weekStart.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        val endStr = if (weekStart.month == weekEnd.month) {
            weekEnd.format(DateTimeFormatter.ofPattern("d", Locale.ENGLISH))
        } else {
            weekEnd.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        }
        "$startStr - $endStr"
    }

    val viewLabel = if (viewMode == "day") "Day View" else "Week View"

    // Mock data for meals
    val breakfastCal = 0
    val lunchCal = 0
    val dinnerCal = 0
    val snacksCal = 0
    val totalCalories = breakfastCal + lunchCal + dinnerCal + snacksCal
    val goalCalories = 2170
    val netCalories = totalCalories // simplified mock

    // Calculate percentages
    val totalForPercent = if (totalCalories > 0) totalCalories.toFloat() else 1f
    val breakfastPct = if (totalCalories > 0) (breakfastCal / totalForPercent * 100).toInt() else 0
    val lunchPct = if (totalCalories > 0) (lunchCal / totalForPercent * 100).toInt() else 0
    val dinnerPct = if (totalCalories > 0) (dinnerCal / totalForPercent * 100).toInt() else 0
    val snacksPct = if (totalCalories > 0) (snacksCal / totalForPercent * 100).toInt() else 0

    // Meal colors
    val breakfastColor = Color(0xFF1565C0) // Dark blue
    val lunchColor = Color(0xFF0D47A1) // Darker blue
    val dinnerColor = Color(0xFF42A5F5) // Medium blue
    val snacksColor = Color(0xFF90CAF9) // Light blue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // --- Day/Week Navigation Bar ---
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
                                    Text(
                                        "Day",
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 15.sp,
                                        color = Color(0xFF616161)
                                    )
                                },
                                onClick = {
                                    viewMode = "day"
                                    showViewDropdown = false
                                }
                            )
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Week",
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 15.sp,
                                        color = Color(0xFF616161)
                                    )
                                },
                                onClick = {
                                    viewMode = "week"
                                    showViewDropdown = false
                                }
                            )
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Pick a Date",
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 15.sp,
                                        color = Color(0xFF616161)
                                    )
                                },
                                onClick = {
                                    showViewDropdown = false
                                    showDatePicker = true
                                }
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

        // --- Scrollable content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // --- Pie Chart Card ---
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(180.dp)) {
                            val strokeWidth = 40f
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - diameter) / 2,
                                (size.height - diameter) / 2
                            )
                            val arcSize = Size(diameter, diameter)

                            if (totalCalories == 0) {
                                // Draw empty grey circle
                                drawArc(
                                    color = Color(0xFFE0E0E0),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                            } else {
                                // Draw meal segments
                                var startAngle = -90f
                                val meals = listOf(
                                    breakfastCal to breakfastColor,
                                    lunchCal to lunchColor,
                                    dinnerCal to dinnerColor,
                                    snacksCal to snacksColor
                                )
                                meals.forEach { (cal, color) ->
                                    if (cal > 0) {
                                        val sweep = (cal.toFloat() / totalCalories) * 360f
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Meal Legend (2x2 grid) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MealLegendItem(
                            color = breakfastColor,
                            label = "Breakfast",
                            percent = breakfastPct,
                            calories = breakfastCal
                        )
                        MealLegendItem(
                            color = lunchColor,
                            label = "Lunch",
                            percent = lunchPct,
                            calories = lunchCal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MealLegendItem(
                            color = dinnerColor,
                            label = "Dinner",
                            percent = dinnerPct,
                            calories = dinnerCal
                        )
                        MealLegendItem(
                            color = snacksColor,
                            label = "Snacks",
                            percent = snacksPct,
                            calories = snacksCal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Calorie Summary Rows ---
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Total Calories
                    CalorieSummaryRow(
                        label = "Total Calories",
                        value = totalCalories.toString(),
                        valueColor = Color(0xFF212121)
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    // Net Calories
                    CalorieSummaryRow(
                        label = "Net Calories",
                        value = netCalories.toString(),
                        valueColor = Color(0xFF212121)
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    // Goal
                    CalorieSummaryRow(
                        label = "Goal",
                        value = goalCalories.toFormattedString(),
                        valueColor = Color(0xFF1565C0)
                    )
                }
            }
        }
    }

    // --- Date Picker Dialog ---
    val context = androidx.compose.ui.platform.LocalContext.current
    if (showDatePicker) {
        val today = LocalDate.now()
        val calendar = Calendar.getInstance()
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    dayOffset = (pickedDate.toEpochDay() - today.toEpochDay()).toInt()
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

@Composable
private fun MealLegendItem(
    color: Color,
    label: String,
    percent: Int,
    calories: Int
) {
    Row(
        modifier = Modifier.width(140.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(14.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
            Text(
                text = "$percent% ($calories cal)",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun CalorieSummaryRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF424242)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun Int.toFormattedString(): String {
    return String.format(Locale.US, "%,d", this)
}