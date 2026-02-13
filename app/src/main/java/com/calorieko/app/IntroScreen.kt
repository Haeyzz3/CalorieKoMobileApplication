package com.calorieko.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calorieko.app.ui.theme.*

@Composable
fun IntroScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- Top Hero Section with Image ---
        Box(
            modifier = Modifier
                .height(300.dp) // Adjusted height
                .fillMaxWidth()
        ) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                        )
                    )
            )

            // Image from URL (Using Coil)
            AsyncImage(
                model = "https://images.unsplash.com/photo-1645220559451-aaacbbd7bcc5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
                contentDescription = "Healthy Food",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.3f // Opacity 30%
            )

            // Text Overlay
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to CalorieKo",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Automated nutrition tracking through\nAI and Smart Scale integration",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // --- Features List ---
        Column(
            modifier = Modifier
                .weight(1f) // Takes up remaining space
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FeatureItem(
                icon = Icons.Default.Psychology, // Brain icon replacement
                title = "AI-Powered Tracking",
                description = "Smart food recognition and automatic logging"
            )
            FeatureItem(
                icon = Icons.Default.MonitorWeight, // Scale icon replacement
                title = "Smart Scale Integration",
                description = "Seamless IoT device connectivity"
            )
            FeatureItem(
                icon = Icons.Default.Smartphone,
                title = "Real-time Insights",
                description = "Track your nutrition goals effortlessly"
            )
        }

        // --- Get Started Button ---
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent // Transparent to show gradient
            ),
            contentPadding = PaddingValues() // Remove default padding
        ) {
            // Gradient Background for Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                        ),
                        shape = RoundedCornerShape(12.dp) // Button curve
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Helper Component for the Feature List
@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CalorieKoGreen,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}