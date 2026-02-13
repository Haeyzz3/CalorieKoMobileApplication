package com.calorieko.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*

@Composable
fun GoalSelectionScreen(onContinue: () -> Unit) {
    // Local State
    var selectedGoal by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 1. Progress Bar (66%) ---
        val progress by animateFloatAsState(targetValue = 0.66f, label = "progress")

        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFF1F5F9))) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                        )
                    )
            )
        }

        // --- 2. Header ---
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Goal Icon Circle
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CalorieKoGreen, CalorieKoLightGreen)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges, // Target icon
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "STEP 2 OF 3",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "What is your goal?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We will calculate your daily calories",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }

        // --- 3. Goal Options ---
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoalCard(
                title = "Lose Weight",
                description = "Burn fat and get lean",
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                isSelected = selectedGoal == "lose_weight",
                onClick = { selectedGoal = "lose_weight" }
            )

            GoalCard(
                title = "Maintain Weight",
                description = "Keep your current physique",
                icon = Icons.Default.Remove, // Or a Balance icon if available
                isSelected = selectedGoal == "maintain_weight",
                onClick = { selectedGoal = "maintain_weight" }
            )

            GoalCard(
                title = "Gain Muscle",
                description = "Build mass and strength",
                icon = Icons.Default.FitnessCenter, // Dumbbell
                isSelected = selectedGoal == "gain_muscle",
                onClick = { selectedGoal = "gain_muscle" }
            )
        }

        // --- 4. Continue Button ---
        Button(
            onClick = onContinue,
            enabled = selectedGoal.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.LightGray
            ),
            contentPadding = PaddingValues()
        ) {
            // Gradient Background Logic
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (selectedGoal.isNotEmpty()) Brush.horizontalGradient(
                            colors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                        ) else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continue",
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

// --- Helper Component for Goal Cards ---
@Composable
fun GoalCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) CalorieKoGreen.copy(alpha = 0.05f) else Color.White,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) CalorieKoGreen else Color.LightGray.copy(alpha = 0.3f),
        label = "borderColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth().height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Surface(
                shape = CircleShape,
                color = if (isSelected) CalorieKoGreen else Color(0xFFF1F5F9),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isSelected) CalorieKoGreen else Color.Black
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}