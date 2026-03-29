package com.calorieko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.* // Automatically grabs standard runtime components
import androidx.compose.runtime.getValue // REQUIRED for 'by' delegate reading
import androidx.compose.runtime.setValue // REQUIRED for 'by' delegate writing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calorieko.app.ble.BleScaleManager
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.remote.CloudRestoreManager
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.RestoreResult
import com.calorieko.app.ui.components.*
import com.calorieko.app.ui.screens.*
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Firestore sync repository for cloud persistence
    val firestoreSyncRepo = remember { FirestoreSyncRepository() }

    // Cloud restore manager for pull-from-cloud on login
    val cloudRestoreManager = remember {
        CloudRestoreManager(
            firestoreSyncRepo = firestoreSyncRepo,
            userDao = userDao,
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            mealLogItemDao = db.mealLogItemDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
            pantryDao = db.pantryDao(),
            mealPlanDao = db.mealPlanDao()
        )
    }

    // Restore-in-progress state for the syncing overlay
    var restoreInProgress by remember { mutableStateOf(false) }


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



    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(navController = navController, startDestination = "splash") {

        // 1. Splash
        composable("splash") {
            SplashScreen(
                onAlreadyLoggedIn = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        restoreInProgress = true
                        scope.launch(Dispatchers.IO) {
                            cloudRestoreManager.restoreIfNeeded(uid)
                            withContext(Dispatchers.Main) {
                                restoreInProgress = false
                                navController.navigate("dashboard") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
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
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        restoreInProgress = true
                        scope.launch(Dispatchers.IO) {
                            cloudRestoreManager.restoreIfNeeded(uid)
                            withContext(Dispatchers.Main) {
                                restoreInProgress = false
                                navController.navigate("dashboard") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
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

                            // Sync new profile to Firestore
                            firestoreSyncRepo.syncProfile(currentUser.uid, userProfile)

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
            val pantryViewModel: com.calorieko.app.viewmodel.PantryViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.PantryViewModel.provideFactory(
                    pantryDao = db.pantryDao(),
                    mealPlanDao = db.mealPlanDao(),
                    foodDao = db.foodDao(),
                    firestoreSyncRepo = firestoreSyncRepo
                )
            )
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
            val logMealViewModel: com.calorieko.app.viewmodel.LogMealViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.LogMealViewModel.provideFactory(
                    foodDao = db.foodDao(),
                    mealLogDao = db.mealLogDao(),
                    mealLogItemDao = db.mealLogItemDao(),
                    dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
                    auth = auth,
                    firestoreSyncRepo = firestoreSyncRepo
                )
            )
            LogMealScreen(
                viewModel = logMealViewModel,
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

    // ═══ Syncing Overlay ═══
    AnimatedVisibility(
        visible = restoreInProgress,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = "Logo",
                            tint = CalorieKoGreen,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Syncing your data...",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Restoring from cloud",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
    } // end Box
}
