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
                    // When timer finishes, go to 'intro' and remove 'splash' from back history
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
                    // For now, we just print to console, or we can add the next screen later
                    println("Navigate to BioForm")
                }
            )
        }
    }
}