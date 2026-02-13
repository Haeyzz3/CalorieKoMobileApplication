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

        // Screen 4: Goal Selection (Updated to navigate to Target Summary)
        composable("goalSelection") {
            GoalSelectionScreen(
                onContinue = {
                    navController.navigate("targetSummary")
                }
            )
        }

        // --- UPDATED: Target Summary goes to Scale Pairing ---
        composable("targetSummary") {
            TargetSummaryScreen(
                onContinue = {
                    navController.navigate("scalePairing")
                }
            )
        }

        // --- NEW: Scale Pairing Screen ---
        composable("scalePairing") {
            ScalePairingScreen(
                onComplete = {
                    // This is the end of onboarding!
                    // In a real app, you would navigate to "Dashboard"
                    println("Onboarding Complete! Navigate to Dashboard.")
                }
            )
        }
    }
}