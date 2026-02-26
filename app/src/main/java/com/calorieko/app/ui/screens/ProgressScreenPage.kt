package com.calorieko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.components.ProgressRings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProgressScreen(onNavigate: (String) -> Unit) {

    // 1. Get the current authenticated user
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // 2. Local Context & Database Setup
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val userDao = db.userDao()
    val activityLogDao = db.activityLogDao()

    var activeTab by remember { mutableStateOf("progress") }

    // --- Dynamic Target State ---
    var targetCalories by remember { mutableStateOf(2000) }
    var targetBurned by remember { mutableStateOf(500) }
    var targetSodium by remember { mutableStateOf(2300) }
    var targetProtein by remember { mutableStateOf(150) }
    var targetCarbs by remember { mutableStateOf(200) }
    var targetFats by remember { mutableStateOf(65) }

    // --- Real Activity Log State ---
    var realActivityLog by remember { mutableStateOf<List<com.calorieko.app.data.model.ActivityLogEntity>>(emptyList()) }

    // --- Calculated Values ---
    val caloriesConsumed = realActivityLog.filter { it.type == "meal" }.sumOf { it.calories }
    val caloriesBurned = realActivityLog.filter { it.type == "workout" }.sumOf { it.calories }
    val currentCalories = caloriesConsumed
    val currentSodium = realActivityLog.filter { it.type == "meal" }.sumOf { it.sodium }
    val currentProtein = realActivityLog.filter { it.type == "meal" }.sumOf { it.protein }
    val currentCarbs = realActivityLog.filter { it.type == "meal" }.sumOf { it.carbs }
    val currentFats = realActivityLog.filter { it.type == "meal" }.sumOf { it.fats }

    // --- Fetch User Profile & Activity Log ---
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            withContext(Dispatchers.IO) {
                val profile = userDao.getUser(uid)

                if (profile != null) {
                    val bmr = if (profile.sex.equals("Male", ignoreCase = true)) {
                        (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) + 5
                    } else {
                        (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) - 161
                    }

                    val activityMultiplier = when (profile.activityLevel) {
                        "lightly_active" -> 1.375
                        "active" -> 1.55
                        "very_active" -> 1.725
                        "not_very_active" -> 1.2
                        else -> 1.2
                    }
                    val tdee = bmr * activityMultiplier

                    targetCalories = when (profile.goal) {
                        "lose_weight", "weight_loss", "weight" -> (tdee - 500).toInt().coerceAtLeast(1200)
                        "gain_muscle" -> (tdee + 300).toInt()
                        else -> tdee.toInt()
                    }

                    val (proteinPct, carbsPct, fatsPct) = when (profile.goal) {
                        "lose_weight", "weight_loss", "weight" -> Triple(0.35, 0.35, 0.30)
                        "gain_muscle" -> Triple(0.30, 0.45, 0.25)
                        else -> Triple(0.30, 0.40, 0.30)
                    }

                    targetProtein = ((targetCalories * proteinPct) / 4).toInt()
                    targetCarbs = ((targetCalories * carbsPct) / 4).toInt()
                    targetFats = ((targetCalories * fatsPct) / 9).toInt()
                    targetSodium = 2300
                    targetBurned = 500
                }

                // Fetch today's activity logs
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis

                realActivityLog = activityLogDao.getLogsForToday(uid, startOfDay)
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                if (it != "progress") {
                    onNavigate(it)
                }
            })
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // --- Header ---
            Text(
                text = "Progress",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            // --- Progress Rings ---
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
