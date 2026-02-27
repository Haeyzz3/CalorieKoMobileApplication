package com.calorieko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDetailsScreen(onBackClick: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calories", "Nutrients", "Macros")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Details", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
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
                contentColor = Color(0xFF16A34A),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF16A34A)
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
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) Color(0xFF16A34A) else Color.Gray
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
    // Mock Data
    val targetCalories = 2000
    val foodCalories = 1450
    val exerciseCalories = 320
    val remainingCalories = targetCalories - foodCalories + exerciseCalories

    val meals = listOf(
        MealMockData("Breakfast", 450, 500),
        MealMockData("Lunch", 600, 600),
        MealMockData("Dinner", 400, 700),
        MealMockData("Snacks", 0, 200)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Summary Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Daily Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryColumn("Goal", targetCalories.toString())
                        Text("-", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                        SummaryColumn("Food", foodCalories.toString(), Color(0xFF3B82F6))
                        Text("+", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                        SummaryColumn("Exercise", exerciseCalories.toString(), Color(0xFFEA580C))
                        Text("=", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                        SummaryColumn("Remaining", remainingCalories.toString(), Color(0xFF16A34A))
                    }
                }
            }
        }

        // --- Meal Breakdown Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Meal Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(16.dp))

                    meals.forEach { meal ->
                        MealProgressRow(meal)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryColumn(label: String, value: String, valueColor: Color = Color(0xFF1F2937)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = valueColor)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

data class MealMockData(val name: String, val consumed: Int, val target: Int)

@Composable
fun MealProgressRow(meal: MealMockData) {
    val progress = if (meal.target > 0) meal.consumed.toFloat() / meal.target.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = meal.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF374151))
            Text(text = "${meal.consumed} / ${meal.target} kcal", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF16A34A),
            trackColor = Color(0xFFE5E7EB),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}