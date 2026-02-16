package com.calorieko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calorieko.app.ui.theme.CalorieKoMobileApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieKoMobileApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // Screen 1: Splash
        composable("splash") {
            SplashScreen(
                onComplete = {
                    navController.navigate("intro") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Screen 2: Intro
        composable("intro") {
            IntroScreen(
                onContinue = {
                    navController.navigate("bioForm")
                }
            )
        }

        // Screen 3: Bio Form
        composable("bioForm") {
            BioFormScreen(
                onContinue = {
                    // Navigate to Goal Selection
                    navController.navigate("goalSelection")
                }
            )
        }

        // Screen 4: Goal Selection
        composable("goalSelection") {
            GoalSelectionScreen(
                onContinue = {
                    navController.navigate("targetSummary")
                }
            )
        }

        // Screen 5: Target Summary
        composable("targetSummary") {
            TargetSummaryScreen(
                onContinue = {
                    navController.navigate("scalePairing")
                }
            )
        }

        // Screen 6: Scale Pairing
        composable("scalePairing") {
            ScalePairingScreen(
                onComplete = {
                    navController.navigate("success")
                }
            )
        }

        // Screen 7: Success
        composable("success") {
            SuccessScreen(
                onEnterDashboard = {
                    navController.navigate("dashboard")
                }
            )
        }

        // Screen 8: Dashboard
        composable("dashboard") {
            DashboardScreen(
                onNavigate = { destination ->
                    // Handle inner navigation here later (logMeal, etc.)
                    println("Navigating to: $destination")
                }
            )
        }
    }
}