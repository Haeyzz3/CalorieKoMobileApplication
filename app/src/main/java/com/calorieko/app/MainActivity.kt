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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.remote.CloudRestoreManager
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
import com.calorieko.app.ui.components.*
import com.calorieko.app.ui.screens.*
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoTheme
import com.calorieko.app.viewmodel.RestoreViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.TextButton
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

private suspend fun triggerFirestoreCatchUpIfNeeded(
    db: AppDatabase,
    context: android.content.Context,
    uid: String
) {
    val pendingCount = withContext(Dispatchers.IO) {
        db.firestoreOutboxDao().pendingCount(uid)
    }
    if (pendingCount > 0) {
        FirestoreAutoSyncManager.triggerSync(context.applicationContext, uid, immediate = true)
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

    val userRepo = remember { com.calorieko.app.data.repository.UserRepository(db, context.applicationContext) }
    val activityRepo = remember { com.calorieko.app.data.repository.ActivityRepository(db, context.applicationContext) }
    val mealRepo = remember { com.calorieko.app.data.repository.MealRepository(db, context.applicationContext) }
    val pantryRepository = remember { com.calorieko.app.data.repository.PantryRepository(db, context.applicationContext) }
    val mealPlanRepository = remember { com.calorieko.app.data.repository.MealPlanRepository(db, context.applicationContext) }

    // Auth repository for all authentication screens
    val authRepo = remember { com.calorieko.app.data.repository.AuthRepository(auth) }

    // Cloud restore manager for pull-from-cloud on login
    val cloudRestoreManager = remember {
        CloudRestoreManager(
            db = db,
            restoreSource = firestoreSyncRepo,
            userDao = userDao,
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            mealLogItemDao = db.mealLogItemDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
            pantryDao = db.pantryDao(),
            mealPlanDao = db.mealPlanDao(),
            weightLogDao = db.weightLogDao()
        )
    }

    val restoreViewModel: RestoreViewModel = viewModel(
        factory = RestoreViewModel.provideFactory(cloudRestoreManager)
    )
    val restoreState by restoreViewModel.state.collectAsState()

    LaunchedEffect(restoreState) {
        when (restoreState) {
            is RestoreViewModel.RestoreState.Success -> {
                auth.currentUser?.uid?.let { uid ->
                    triggerFirestoreCatchUpIfNeeded(db, context.applicationContext, uid)
                }
                val completed = (restoreState as RestoreViewModel.RestoreState.Success).onboardingCompleted
                if (completed) {
                    navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
                } else {
                    navController.navigate("targetSummary") { popUpTo(0) { inclusive = true } }
                }
            }
            is RestoreViewModel.RestoreState.NotNeeded -> {
                auth.currentUser?.uid?.let { uid ->
                    triggerFirestoreCatchUpIfNeeded(db, context.applicationContext, uid)
                }
                val completed = (restoreState as RestoreViewModel.RestoreState.NotNeeded).onboardingCompleted
                if (completed) {
                    navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
                } else {
                    navController.navigate("targetSummary") { popUpTo(0) { inclusive = true } }
                }
            }
            is RestoreViewModel.RestoreState.NoCloudData -> {
                navController.navigate("targetSummary") { popUpTo(0) { inclusive = true } }
            }
            else -> {}
        }
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



    // ── Global keyboard dismissal on navigation ──
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(navController = navController, startDestination = "splash") {

        // 1. Splash
        composable("splash") {
            val splashViewModel: com.calorieko.app.viewmodel.SplashViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.SplashViewModel.provideFactory(
                    authRepository = authRepo
                )
            )
            SplashScreen(
                viewModel = splashViewModel,
                onAlreadyLoggedIn = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        restoreViewModel.restore(uid)
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                },
                onVerificationRequired = {
                    navController.navigate("verificationPending") {
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
            val authViewModel: com.calorieko.app.viewmodel.AuthViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.AuthViewModel.provideFactory(
                    authRepository = authRepo
                )
            )
            AuthScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        restoreViewModel.restore(uid)
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
            val forgotPasswordViewModel: com.calorieko.app.viewmodel.ForgotPasswordViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ForgotPasswordViewModel.provideFactory(
                    authRepository = authRepo
                )
            )
            ForgotPasswordScreen(
                viewModel = forgotPasswordViewModel,
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
            val registerViewModel: com.calorieko.app.viewmodel.RegisterViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.RegisterViewModel.provideFactory(
                    authRepository = authRepo
                )
            )
            RegisterScreen(
                viewModel = registerViewModel,
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
                            goal = setupGoalId,
                            onboardingCompleted = false // New user, must complete onboarding
                        )
                        scope.launch {
                            userRepo.saveInitialProfile(currentUser.uid, userProfile)
                            navController.navigate("verificationPending") {
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

        // 7b. NEW: Verification Pending Screen
        composable("verificationPending") {
            val verificationViewModel: com.calorieko.app.viewmodel.VerificationViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.VerificationViewModel.provideFactory(
                    authRepository = authRepo
                )
            )
            VerificationPendingScreen(
                viewModel = verificationViewModel,
                onVerificationSuccess = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        // After verification, we try to restore (which checks onboarding status)
                        restoreViewModel.restore(uid)
                    } else {
                        navController.navigate("intro") { popUpTo(0) { inclusive = true } }
                    }
                },
                onCancel = {
                    navController.navigate("intro") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // 5b. Target Summary Screen
        composable("targetSummary") {
            TargetSummaryScreen(
                targetCalories = targetCalories,
                targetSodium = targetSodium,
                targetProtein = targetProtein,
                targetCarbs = targetCarbs,
                targetFats = targetFats,
                goalTitle = setupGoalTitle,
                activityLevel = setupActivityLevel,
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
                    val isConnected = bleScaleManager.connectionState.value is com.calorieko.app.ble.BleConnectionState.Connected
                    if (source == "settings") {
                        navController.popBackStack()
                    } else {
                        navController.navigate("success/$isConnected")
                    }
                },
                onSkip = {
                    if (source == "settings") {
                        navController.popBackStack()
                    } else {
                        navController.navigate("success/false")
                    }
                }
            )
        }

        composable(
            route = "success/{isScaleConnected}",
            arguments = listOf(navArgument("isScaleConnected") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isScaleConnected = backStackEntry.arguments?.getBoolean("isScaleConnected") ?: false
            SuccessScreen(
                isScaleConnected = isScaleConnected,
                onEnterDashboard = {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        scope.launch {
                            userRepo.markOnboardingCompleted(uid)
                            navController.navigate("dashboard") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 8. Dashboard
        composable("dashboard") {
            val dashboardRepo = com.calorieko.app.data.repository.DashboardRepository(
                userDao = db.userDao(),
                dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
                mealLogDao = db.mealLogDao(),
                activityLogDao = db.activityLogDao(),
                nutritionalValuesRepo = nutritionalRepo
            )
            val dashboardViewModel: com.calorieko.app.viewmodel.DashboardViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.DashboardViewModel.provideFactory(
                    auth = auth,
                    dashboardRepository = dashboardRepo,
                    mealPlanDao = db.mealPlanDao(),
                    dishRecipeDao = db.dishRecipeDao(),
                    appContext = context.applicationContext
                )
            )
            DashboardScreen(
                viewModel = dashboardViewModel,
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

        // --- NEW: Profile Summary (Reusable TargetSummaryScreen) ---
        composable("profileSummary") {
            val dashboardRepo = com.calorieko.app.data.repository.DashboardRepository(
                userDao = db.userDao(),
                dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
                mealLogDao = db.mealLogDao(),
                activityLogDao = db.activityLogDao(),
                nutritionalValuesRepo = nutritionalRepo
            )
            val dashboardViewModel: com.calorieko.app.viewmodel.DashboardViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.DashboardViewModel.provideFactory(
                    auth = auth,
                    dashboardRepository = dashboardRepo,
                    mealPlanDao = db.mealPlanDao(),
                    dishRecipeDao = db.dishRecipeDao(),
                    appContext = context.applicationContext
                )
            )

            val profile by dashboardViewModel.userProfile.collectAsState()
            val calories by dashboardViewModel.targetCalories.collectAsState()
            val sodium by dashboardViewModel.targetSodium.collectAsState()
            val protein by dashboardViewModel.targetProtein.collectAsState()
            val carbs by dashboardViewModel.targetCarbs.collectAsState()
            val fats by dashboardViewModel.targetFats.collectAsState()
            val goalTitle by dashboardViewModel.goalTitle.collectAsState()

            TargetSummaryScreen(
                targetCalories = calories,
                targetSodium = sodium,
                targetProtein = protein,
                targetCarbs = carbs,
                targetFats = fats,
                goalTitle = goalTitle,
                activityLevel = profile?.activityLevel ?: "sedentary",
                subtitle = null,
                headerIcon = Icons.Rounded.Person,
                buttonText = "Edit Profile",
                buttonIcon = null,
                onSecondaryAction = {
                    navController.popBackStack()
                },
                onContinue = {
                    navController.navigate("editProfile")
                }
            )
        }

        // --- Diary Screen ---
        composable("diary") {
            val dashboardRepo = com.calorieko.app.data.repository.DashboardRepository(
                userDao = db.userDao(),
                dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
                mealLogDao = db.mealLogDao(),
                activityLogDao = db.activityLogDao(),
                nutritionalValuesRepo = nutritionalRepo
            )
            val diaryViewModel: com.calorieko.app.viewmodel.DiaryViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.DiaryViewModel.provideFactory(
                    auth = auth,
                    dashboardRepository = dashboardRepo,
                    activityLogDao = db.activityLogDao(),
                    mealLogDao = db.mealLogDao(),
                    activityRepository = activityRepo,
                    mealRepository = mealRepo
                )
            )
            DiaryScreen(
                viewModel = diaryViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { activityId ->
                    navController.navigate("logWorkout?activityId=$activityId")
                }
            )
        }

        // --- NEW: Progress Screen ---
        composable("progress") {
            val progressViewModel: com.calorieko.app.viewmodel.ProgressViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ProgressViewModel.provideFactory(
                    auth = auth,
                    activityRepository = activityRepo,
                    nutritionSummaryDao = db.dailyNutritionSummaryDao(),
                    mealLogDao = db.mealLogDao(),
                    weightLogDao = db.weightLogDao()
                )
            )
            ProgressScreen(
                viewModel = progressViewModel,
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
            val profileViewModel: com.calorieko.app.viewmodel.ProfileViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ProfileViewModel.provideFactory(
                    auth = auth,
                    userRepository = userRepo,
                    activityRepository = activityRepo,
                    mealLogDao = db.mealLogDao(),
                    activityLogDao = db.activityLogDao()
                )
            )
            ProfileScreen(
                viewModel = profileViewModel,
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
            val editProfileViewModel: com.calorieko.app.viewmodel.EditProfileViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.EditProfileViewModel.provideFactory(
                    auth = auth,
                    userRepository = userRepo
                )
            )
            EditProfileScreen(
                viewModel = editProfileViewModel,
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
            val settingsViewModel: com.calorieko.app.viewmodel.SettingsViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.SettingsViewModel.provideFactory(
                    auth = auth,
                    db = db,
                    firestoreSyncRepo = firestoreSyncRepo,
                    appContext = context.applicationContext
                )
            )
            SettingsScreen(
                viewModel = settingsViewModel,
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
                bleScaleManager = bleScaleManager
            )
        }

        // --- How We Calculate Screen ---
        composable("howWeCalculate") {
            HowWeCalculateScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // --- NEW: Pantry Screen ---
        composable("pantry") {
            val pantryCalculator = remember {
                com.calorieko.app.data.local.RecipeNutritionCalculator(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    recipeIngredientDao = db.recipeIngredientDao()
                )
            }
            val pantryViewModel: com.calorieko.app.viewmodel.PantryViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.PantryViewModel.provideFactory(
                    auth = auth,
                    pantryDao = db.pantryDao(),
                    mealPlanDao = db.mealPlanDao(),
                    pantryRepository = pantryRepository,
                    mealPlanRepository = mealPlanRepository,
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    calculator = pantryCalculator,
                    userDao = db.userDao(),
                    nutritionalValuesRepo = nutritionalRepo,
                    appContext = context.applicationContext
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

        // --- Explore Dishes Screen ---
        composable("explore") {
            val exploreViewModel: com.calorieko.app.viewmodel.ExploreViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ExploreViewModel.provideFactory(
                    auth = auth,
                    dishRecipeDao = db.dishRecipeDao(),
                    pantryDao = db.pantryDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    foodDao = db.foodDao(),
                    pantryRepository = pantryRepository,
                    appContext = context.applicationContext
                )
            )
            ExploreScreen(
                viewModel = exploreViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // --- Log Meal Screen ---
        composable("logMeal") {
            val calculator = remember {
                com.calorieko.app.data.local.RecipeNutritionCalculator(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    recipeIngredientDao = db.recipeIngredientDao()
                )
            }
            val logMealViewModel: com.calorieko.app.viewmodel.LogMealViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.LogMealViewModel.provideFactory(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    auth = auth,
                    mealRepository = mealRepo,
                    calculator = calculator,
                    pantryDao = db.pantryDao(),
                    pantryRepository = pantryRepository,
                    appContext = context.applicationContext
                )
            )
            val manualLogViewModel: com.calorieko.app.viewmodel.ManualLogViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ManualLogViewModel.provideFactory(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    foodDao = db.foodDao(),
                    auth = auth,
                    mealRepository = mealRepo,
                    calculator = calculator,
                    pantryDao = db.pantryDao(),
                    pantryRepository = pantryRepository,
                    appContext = context.applicationContext
                )
            )
            LogMealScreenWithManual(
                viewModel = logMealViewModel,
                manualLogViewModel = manualLogViewModel,
                bleScaleManager = bleScaleManager,
                onBack = { navController.popBackStack() },
                onMealConfirmed = { navController.popBackStack() },
                onNavigateToPairing = { navController.navigate("scalePairing/settings") }
            )
        }

        // Quick-log from planned meal (legacy single-dish route — kept for backward compatibility)
        composable("logMeal/quick/{dishLabel}/{mealSlot}") { backStackEntry ->
            val dishLabel = backStackEntry.arguments?.getString("dishLabel") ?: ""
            val mealSlot = backStackEntry.arguments?.getString("mealSlot") ?: "Lunch"
            val calculator = remember {
                com.calorieko.app.data.local.RecipeNutritionCalculator(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    recipeIngredientDao = db.recipeIngredientDao()
                )
            }
            val manualLogViewModel: com.calorieko.app.viewmodel.ManualLogViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ManualLogViewModel.provideFactory(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    foodDao = db.foodDao(),
                    auth = auth,
                    mealRepository = mealRepo,
                    calculator = calculator,
                    pantryDao = db.pantryDao(),
                    pantryRepository = pantryRepository,
                    appContext = context.applicationContext
                )
            )
            // Pre-select the planned dish
            androidx.compose.runtime.LaunchedEffect(dishLabel) {
                manualLogViewModel.quickLogFromPlan(dishLabel, mealSlot)
            }
            QuickLogScreen(
                viewModel = manualLogViewModel,
                dishLabel = dishLabel,
                mealSlot = mealSlot,
                onBack = { navController.popBackStack() },
                onMealConfirmed = { navController.popBackStack() },
                bleScaleManager = bleScaleManager,
                onNavigateToPairing = { navController.navigate("scalePairing/settings") }
            )
        }

        // Quick-log entire meal slot (multi-dish, reads from QuickLogBridge)
        composable("logMeal/quickSlot") {
            val calculator = remember {
                com.calorieko.app.data.local.RecipeNutritionCalculator(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    recipeIngredientDao = db.recipeIngredientDao()
                )
            }
            val manualLogViewModel: com.calorieko.app.viewmodel.ManualLogViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.ManualLogViewModel.provideFactory(
                    dishRecipeDao = db.dishRecipeDao(),
                    rawIngredientDao = db.rawIngredientDao(),
                    foodDao = db.foodDao(),
                    auth = auth,
                    mealRepository = mealRepo,
                    calculator = calculator,
                    pantryDao = db.pantryDao(),
                    pantryRepository = pantryRepository,
                    appContext = context.applicationContext
                )
            )

            // Read bridge data and pre-load all dishes for the slot
            val bridgeSlot = remember { com.calorieko.app.viewmodel.QuickLogBridge.pendingMealSlot }
            val bridgeDishes = remember { com.calorieko.app.viewmodel.QuickLogBridge.pendingDishes.toList() }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (bridgeSlot.isNotEmpty() && bridgeDishes.isNotEmpty()) {
                    manualLogViewModel.quickLogSlotFromPlan(bridgeSlot, bridgeDishes)
                }
                com.calorieko.app.viewmodel.QuickLogBridge.clear()
            }

            QuickLogScreen(
                viewModel = manualLogViewModel,
                dishLabel = bridgeDishes.firstOrNull()?.dishLabel ?: "",
                mealSlot = bridgeSlot,
                onBack = { navController.popBackStack() },
                onMealConfirmed = { navController.popBackStack() },
                bleScaleManager = bleScaleManager,
                onNavigateToPairing = { navController.navigate("scalePairing/settings") }
            )
        }

        // --- NEW: Log Workout Screen ---
        composable(
            route = "logWorkout?activityId={activityId}",
            arguments = listOf(navArgument("activityId") { 
                type = NavType.IntType 
                defaultValue = -1 
            })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getInt("activityId") ?: -1
            val logWorkoutViewModel: com.calorieko.app.viewmodel.LogWorkoutViewModel = viewModel(
                factory = com.calorieko.app.viewmodel.LogWorkoutViewModel.provideFactory(
                    auth = auth,
                    activityRepository = activityRepo
                )
            )
            LogWorkoutScreen(
                viewModel = logWorkoutViewModel,
                activityIdToEdit = if (activityId != -1) activityId else null,
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
                val activityDetailsViewModel: com.calorieko.app.viewmodel.ActivityDetailsViewModel = viewModel(
                    factory = com.calorieko.app.viewmodel.ActivityDetailsViewModel.provideFactory(
                        auth = auth,
                        activityRepository = activityRepo,
                        activityId = activityId
                    )
                )
                val activityLog by activityDetailsViewModel.activityLog.collectAsState()
                activityLog?.let {
                    ActivityDetailsScreen(
                        viewModel = activityDetailsViewModel,
                        activity = it,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    // ═══ Syncing Overlay ═══
    AnimatedVisibility(
        visible = restoreState is RestoreViewModel.RestoreState.Restoring || restoreState is RestoreViewModel.RestoreState.Failed,
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

                if (restoreState is RestoreViewModel.RestoreState.Failed) {
                    val errorMsg = (restoreState as RestoreViewModel.RestoreState.Failed).error
                    Text(
                        text = "Restore failed",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = errorMsg,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    androidx.compose.material3.Button(
                        onClick = { restoreViewModel.retry() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = CalorieKoGreen
                        )
                    ) {
                        Text("Retry")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
                        }
                    ) {
                        Text("Skip for now", color = Color.White)
                    }
                } else {
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
    }
    } // end Box
}
