package com.calorieko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calorieko.app.ui.components.*
import com.calorieko.app.ui.screens.*
import com.calorieko.app.ui.theme.CalorieKoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieKoTheme { // Matches the fixed Theme name
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        // 1. Splash — checks if user is already logged in
        composable("splash") {
            SplashScreen(
                onAlreadyLoggedIn = {
                    // User is logged in → skip to Dashboard, clear back stack
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNotLoggedIn = {
                    // No user → show Intro
                    navController.navigate("intro") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // 2. Intro — both buttons go to Login
        composable("intro") {
            IntroScreen(onNavigate = { action ->
                when (action) {
                    "GET_STARTED" -> navController.navigate("login")
                    "LOGIN" -> navController.navigate("login")
                }
            })
        }

        // 3. Login Screen (Firebase Auth)
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Clear entire back stack so user can't go back to login
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

        // 3. Bio Form (Matches Step 3)
        composable("bioForm") {
            BioFormScreen(onContinue = { age, height, weight, sex ->
                // In a real app, save these values here
                navController.navigate("goalSelection")
            })
        }

        // 4. Goal Selection
        composable("goalSelection") {
            GoalSelectionScreen(onContinue = { goalId ->
                navController.navigate("targetSummary")
            })
        }

        // 5. Target Summary (Matches Step 4)
        composable("targetSummary") {
            TargetSummaryScreen(
                targetCalories = 2000, // Placeholder data
                targetSodium = 2300,
                goalTitle = "Weight Control",
                onContinue = { navController.navigate("scalePairing") }
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
        composable("settings") {
            SettingsScreen(
                onNavigate = { dest ->
                    val route = if (dest == "home") "dashboard" else dest
                    if (route != "settings") {
                        navController.navigate(route) {
                            launchSingleTop = true
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