package com.calorieko.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calorieko.app.ui.theme.* @Composable
fun IntroScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Top Hero Section with Logo & Image ---
        Box(
            modifier = Modifier
                .height(350.dp)
                .fillMaxWidth()
        ) {
            // 1. Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50), // CalorieKoGreen
                                Color(0xFF81C784)  // Lighter Green
                            )
                        )
                    )
            )

            // 2. Background Image
            AsyncImage(
                model = "https://images.unsplash.com/photo-1645220559451-aaacbbd7bcc5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
                contentDescription = "Healthy Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.2f
            )

            // 3. Logo & Text Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- MODIFIED LOGO SECTION ---
                // Removed the white background and padding to make it seamless
                Image(
                    painter = painterResource(id = R.drawable.calorieko_logo),
                    contentDescription = "CalorieKo Logo",
                    modifier = Modifier
                        .size(120.dp) // Increased size slightly
                        .clip(CircleShape) // Optional: Clips to circle if your image is square
                    // Removed .background(Color.White)
                    // Removed .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to CalorieKo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Automated nutrition tracking through\nAI and Smart Scale integration",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // --- Features List ---
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FeatureItem(
                icon = Icons.Default.Psychology,
                title = "AI-Powered Tracking",
                description = "Smart food recognition and automatic logging"
            )
            FeatureItem(
                icon = Icons.Default.MonitorWeight,
                title = "Smart Scale Integration",
                description = "Seamless IoT device connectivity"
            )
            FeatureItem(
                icon = Icons.Default.Smartphone,
                title = "Real-time Insights",
                description = "Track your nutrition goals effortlessly"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Bottom Section (Buttons) ---
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Get Started Button
            Button(
                onClick = { onNavigate("GET_STARTED") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                // Gradient Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFDB05E), // Orange
                                    Color(0xFFFF9800)  // Deep Orange
                                )
                            )
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

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            Row(
                modifier = Modifier.clickable { onNavigate("LOGIN") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Log In",
                    color = Color(0xFF4CAF50), // Green
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
            color = Color(0xFFE8F5E9),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
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
                color = Color(0xFF333333)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}