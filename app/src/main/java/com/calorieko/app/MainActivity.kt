package com.calorieko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.* // Automatically grabs standard runtime components
import androidx.compose.runtime.getValue // REQUIRED for 'by' delegate reading
import androidx.compose.runtime.setValue // REQUIRED for 'by' delegate writing
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.ui.components.*
import com.calorieko.app.ui.screens.*
import com.calorieko.app.ui.theme.CalorieKoTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieKoTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize Database and Firebase Auth
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val userDao = db.userDao()
    val auth = remember { FirebaseAuth.getInstance() }



    // Use mutableStateOf() instead of mutableIntStateOf/mutableDoubleStateOf for universal compatibility
    var setupAge by remember { mutableStateOf(25) }
    var setupHeight by remember { mutableStateOf(170.0) }
    var setupWeight by remember { mutableStateOf(70.0) }
    var setupSex by remember { mutableStateOf("") }
    var setupActivityLevel by remember { mutableStateOf("") }
    var setupName by remember { mutableStateOf("") }

    // --- ADD THESE NEW VARIABLES ---
    var targetCalories by remember { mutableStateOf(2000) }
    var targetSodium by remember { mutableStateOf(2300) }
    var setupGoalTitle by remember { mutableStateOf("General Health") }


    // --- 1. ADD NEW MACRO VARIABLES ---
    var targetProtein by remember { mutableStateOf(150) }
    var targetCarbs by remember { mutableStateOf(200) }
    var targetFats by remember { mutableStateOf(65) }

    NavHost(navController = navController, startDestination = "splash") {

        // 1. Splash
        composable("splash") {
            SplashScreen(
                onAlreadyLoggedIn = {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNotLoggedIn = {
                    navController.navigate("intro") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // 2. Intro
        composable("intro") {
            IntroScreen(onNavigate = { action ->
                when (action) {
                    "GET_STARTED" -> navController.navigate("login")
                    "LOGIN" -> navController.navigate("login")
                }
            })
        }

        // 3. Login Screen
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("bioForm") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgotPassword")
                }
            )
        }

        // 3b. Forgot Password
        composable("forgotPassword") {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 4. Bio Form (UPDATED to capture data)
        composable("bioForm") {
            BioFormScreen(onContinue = { name, age, height, weight, sex ->
                setupName = name
                setupAge = age.toIntOrNull() ?: 25
                setupHeight = height.toDoubleOrNull() ?: 170.0
                setupWeight = weight.toDoubleOrNull() ?: 70.0
                setupSex = sex

                navController.navigate("activityLevel")
            })
        }

        // 4b. Activity Level Selection
        composable("activityLevel") {
            ActivityLevelScreen(onContinue = { activityLevelId ->
                setupActivityLevel = activityLevelId
                navController.navigate("goalSelection")
            })
        }

        // 5. Goal Selection & Database Insertion
        composable("goalSelection") {
            GoalSelectionScreen(onContinue = { goalId ->

                setupGoalTitle = when (goalId) {
                    "lose_weight" -> "Lose Weight"
                    "gain_muscle" -> "Build Muscle"
                    "maintain" -> "Maintain Weight"
                    else -> "General Health"
                }

                // --- 2. STANDARD PROTOCOL METRICS CALCULATION ---

                // A. Calculate BMR (Mifflin-St Jeor)
                val bmr = if (setupSex.equals("Male", ignoreCase = true)) {
                    (10 * setupWeight) + (6.25 * setupHeight) - (5 * setupAge) + 5
                } else {
                    (10 * setupWeight) + (6.25 * setupHeight) - (5 * setupAge) - 161
                }

                // B. Apply Activity Multiplier for TDEE
                val activityMultiplier = when (setupActivityLevel) {
                    "Lightly Active" -> 1.375
                    "Moderately Active" -> 1.55
                    "Very Active" -> 1.725
                    else -> 1.2 // Sedentary
                }
                val tdee = bmr * activityMultiplier

                // C. Calculate Final Calories based on Goal
                targetCalories = when (goalId) {
                    "lose_weight" -> (tdee - 500).toInt().coerceAtLeast(1200) // Minimum 1200 kcal for safety
                    "gain_muscle" -> (tdee + 300).toInt()
                    else -> tdee.toInt()
                }

                // D. Assign macro percentages based on goal
                val (proteinPct, carbsPct, fatsPct) = when (goalId) {
                    "lose_weight" -> Triple(0.35, 0.35, 0.30) // Higher protein for muscle retention & satiety
                    "gain_muscle" -> Triple(0.30, 0.45, 0.25) // Higher carbs for workout energy
                    else -> Triple(0.30, 0.40, 0.30)          // Balanced maintenance
                }

                // E. Convert Calories to Grams (Protein/Carbs = 4 kcal/g, Fat = 9 kcal/g)
                targetProtein = ((targetCalories * proteinPct) / 4).toInt()
                targetCarbs = ((targetCalories * carbsPct) / 4).toInt()
                targetFats = ((targetCalories * fatsPct) / 9).toInt()

                // Standard Sodium Limit
                targetSodium = 2300

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Create the UserProfile object
                    val userProfile = UserProfile(
                        uid = currentUser.uid,
                        name = setupName.ifEmpty { currentUser.displayName ?: "User" },
                        email = currentUser.email ?: "",
                        age = setupAge,
                        weight = setupWeight,
                        height = setupHeight,
                        sex = setupSex,
                        activityLevel = setupActivityLevel,
                        goal = goalId
                    )
                    scope.launch {
                        userDao.insertUser(userProfile)
                        navController.navigate("targetSummary")
                    }
                } else {
                    navController.navigate("targetSummary")
                }
            })
        }

        // 5b. Target Summary Screen
        composable("targetSummary") {
            TargetSummaryScreen(
                targetCalories = targetCalories,
                targetSodium = targetSodium,
                targetProtein = targetProtein,  // PASS NEW VARS
                targetCarbs = targetCarbs,      // PASS NEW VARS
                targetFats = targetFats,        // PASS NEW VARS
                goalTitle = setupGoalTitle,
                onContinue = {
                    navController.navigate("scalePairing")
                }
            )
        }

        // 6. Scale Pairing
        composable("scalePairing") {
            ScalePairingScreen(onComplete = { navController.navigate("success") })
        }

        // 7. Success
        composable("success") {
            SuccessScreen(onEnterDashboard = { navController.navigate("dashboard") })
        }

        // 8. Dashboard
        composable("dashboard") {
            DashboardScreen(
                onNavigate = { dest ->
                    val route = if (dest == "home") "dashboard" else dest
                    if (route != "dashboard") {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // --- NEW: Progress Screen ---
        composable("progress") {
            ProgressScreen(
                onNavigate = { dest ->
                    val route = if (dest == "home") "dashboard" else dest
                    if (route != "progress") {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // --- NEW: Profile Screen ---
        composable("profile") {
            ProfileScreen(
                onNavigate = { dest ->
                    val route = if (dest == "home") "dashboard" else dest
                    if (route != "profile") {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                },
                onEditProfile = {
                    navController.navigate("editProfile")
                }
            )
        }

        // --- NEW: Edit Profile Screen ---
        composable("editProfile") {
            EditProfileScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSave = {
                    navController.popBackStack()
                }
            )
        }

        // --- NEW: Settings Screen ---
        // --- Settings Screen ---
        composable("settings") {
            SettingsScreen(
                onNavigate = { dest ->
                    when (dest) {
                        "logout" -> {
                            // Clear the entire back stack and go to intro
                            navController.navigate("intro") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        "home" -> navController.navigate("dashboard") { launchSingleTop = true }
                        else -> {
                            if (dest != "settings") {
                                navController.navigate(dest) { launchSingleTop = true }
                            }
                        }
                    }
                }
            )
        }

        // --- NEW: Pantry Screen ---
        composable("pantry") {
            PantryScreen(
                onNavigate = { dest ->
                    val route = if (dest == "home") "dashboard" else dest
                    if (route != "pantry") {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // --- NEW: Log Meal Screen ---
        composable("logMeal") {
            LogMealScreen(
                onBack = {
                    navController.popBackStack()
                },
                onMealLogged = { dishName, weight ->
                    // In a real app, save the logged meal here
                    // Note: LogMealScreen already calls onBack() after this callback,
                    // so we do NOT call popBackStack() here to avoid a double pop.
                }
            )
        }

        // --- NEW: Log Workout Screen ---
        composable("logWorkout") {
            LogWorkoutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}