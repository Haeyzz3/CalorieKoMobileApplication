package com.calorieko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

        // 1. Splash
        composable("splash") {
            SplashScreen(onComplete = {
                navController.navigate("intro") { popUpTo("splash") { inclusive = true } }
            })
        }

        // 2. Intro (Matches Step 2)
        composable("intro") {
            IntroScreen(onNavigate = { action ->
                if (action == "GET_STARTED" || action == "LOGIN") {
                    navController.navigate("bioForm")
                }
            })
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
    }
}