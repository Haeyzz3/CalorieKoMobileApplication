package com.calorieko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.* // Automatically grabs standard runtime components
import androidx.compose.runtime.getValue // REQUIRED for 'by' delegate reading
import androidx.compose.runtime.setValue // REQUIRED for 'by' delegate writing
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.ui.components.*
import com.calorieko.app.ui.screens.*
import com.calorieko.app.ui.theme.CalorieKoTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.remote.SyncRepository
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

    // BLE manager hoisted to AppNavigation scope so it survives screen transitions
    val bleScaleManager = remember { BleScaleManager(context) }
    val nutritionalRepo = remember { com.calorieko.app.data.repository.NutritionalValuesRepository(context) }

    // Sync repository for pushing data to the Laravel backend
    val syncRepository = remember {
        SyncRepository(
            userDao = db.userDao(),
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            mealLogItemDao = db.mealLogItemDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao()
        )
    }



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

    var setupGoalId by remember { mutableStateOf("") } // Add this to hold the goal ID temporarily



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

        // 2. Intro screen
        composable("intro") {
            IntroScreen(onNavigate = { action ->
                when (action) {
                    "LOGIN" -> navController.navigate("login")
                }
            })
        }

        // 3. Auth Screen
        composable("login") {
            AuthScreen(
                onLoginSuccess = {
                    // Sync profile to backend after email/password login
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        scope.launch {
                            syncRepository.syncProfile(uid)
                        }
                    }
                    navController.navigate("dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("bioForm")
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

        // 6. Goal Selection (Remove database logic from here)
        composable("goalSelection") {
            GoalSelectionScreen(onContinue = { goalId ->
                // ONLY save the goal ID to state for now. Do NOT write to the database yet.
                setupGoalId = goalId

                setupGoalTitle = when (goalId) {
                    "weight_loss" -> "Weight Control"
                    "gain_muscle" -> "Gain Muscle"
                    else -> "General Health & Wellness"
                }

                // Navigate to the signup screen instead of the summary
                navController.navigate("register")
            })
        }

        // 7. NEW: Sign Up Screen (After Goals)
        composable("register") {
            RegisterScreen(
                onSignUpSuccess = {
                    // --- 1. Metric calculations ---
                    val targets = nutritionalRepo.getTargets(
                        age = setupAge,
                        sex = setupSex,
                        heightCm = setupHeight,
                        weightKg = setupWeight,
                        activityLevel = setupActivityLevel,
                        goal = setupGoalId
                    )
                    targetCalories = targets.targetCalories
                    targetProtein = targets.targetProtein
                    targetCarbs = targets.targetCarbs
                    targetFats = targets.targetFats
                    targetSodium = targets.targetSodium

                    // --- 2. Database Save & Navigation ---
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val userProfile = UserProfile(
                            uid = currentUser.uid,
                            name = setupName.ifEmpty { currentUser.displayName ?: "User" },
                            email = currentUser.email ?: "",
                            age = setupAge,
                            weight = setupWeight,
                            height = setupHeight,
                            sex = setupSex,
                            activityLevel = setupActivityLevel,
                            goal = setupGoalId
                        )
                        scope.launch {
                            userDao.insertUser(userProfile)

                            // Sync the new profile to the Laravel backend
                            syncRepository.syncProfile(currentUser.uid)

                            navController.navigate("targetSummary") {
                                popUpTo("intro") { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
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
                    navController.navigate("scalePairing/signup")
                }
            )
        }

        // 6. Scale Pairing
        composable(
            route = "scalePairing/{source}",
            arguments = listOf(navArgument("source") { type = NavType.StringType })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "signup"
            ScalePairingScreen(
                bleScaleManager = bleScaleManager,
                onComplete = {
                    if (source == "settings") {
                        navController.popBackStack()
                    } else {
                        navController.navigate("success")
                    }
                },
                onSkip = {
                    if (source == "settings") {
                        navController.popBackStack()
                    } else {
                        navController.navigate("success")
                    }
                }
            )
        }

        // 7. Success
        composable("success") {
            SuccessScreen(onEnterDashboard = { navController.navigate("dashboard") })
        }

        // 8. Dashboard
        composable("dashboard") {
            DashboardScreen(
                bleScaleManager = bleScaleManager,
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

        // --- NEW: Nutrition Details Screen ---
        composable("nutritionDetails") {
            NutritionDetailsScreen(
                onBackClick = {
                    navController.popBackStack()
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
                    val route = if (dest == "home") "dashboard" else dest
                    if (route != "settings") {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                },
                onLogout = {
                    navController.navigate("intro") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                bleScaleManager = bleScaleManager // Pasing the scale manager back!
            )
        }

        // --- NEW: Pantry Screen ---
        composable("pantry") {
            val pantryViewModel: com.calorieko.app.viewmodel.PantryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            PantryScreen(
                viewModel = pantryViewModel,
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

        // --- Log Meal Screen ---
        composable("logMeal") {
            LogMealScreen(
                bleScaleManager = bleScaleManager,
                onBack = { navController.popBackStack() },
                onMealConfirmed = { navController.popBackStack() }
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

        // --- NEW: Activity Details Screen ---
        composable(
            route = "activity_details/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")?.toIntOrNull()
            if (activityId != null) {
                var activity by remember { mutableStateOf<ActivityLogEntity?>(null) }
                LaunchedEffect(activityId) {
                    withContext(Dispatchers.IO) {
                        activity = db.activityLogDao().getLogById(activityId)
                    }
                }
                activity?.let {
                    ActivityDetailsScreen(activity = it, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

