package com.calorieko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onAlreadyLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit
) {
    // ── Handle one-shot events ──
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SplashViewModel.Event.AlreadyLoggedIn -> onAlreadyLoggedIn()
                is SplashViewModel.Event.NotLoggedIn -> onNotLoggedIn()
            }
        }
    }

    // 2. The UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // The Gradient Background
                brush = Brush.verticalGradient(
                    colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // White Circle Background
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(120.dp)
            ) {
                // The Leaf Icon (Using a built-in Android icon close to yours)
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Eco,
                        contentDescription = "Logo",
                        tint = CalorieKoGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "CalorieKo",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Tagline
            Text(
                text = "Smart Nutrition Tracking",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}