package com.calorieko.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*

// Data class to define our specific goals
data class HealthGoal(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@Composable
fun GoalSelectionScreen(onContinue: () -> Unit) {
    var selectedGoalId by remember { mutableStateOf<String?>(null) }

    // Define the 4 specific goals from your prototype
    val goals = listOf(
        HealthGoal(
            id = "hypertension",
            title = "Hypertension Management",
            description = "Control blood pressure through balanced nutrition",
            icon = Icons.Rounded.Favorite, // Heart
            gradientColors = listOf(Color(0xFFEF4444), Color(0xFFEC4899)) // Red to Pink
        ),
        HealthGoal(
            id = "weight",
            title = "Weight Control",
            description = "Achieve and maintain your ideal weight",
            icon = Icons.AutoMirrored.Rounded.TrendingDown,
            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)) // Blue to Cyan
        ),
        HealthGoal(
            id = "fitness",
            title = "Fitness & Performance",
            description = "Optimize nutrition for peak performance",
            icon = Icons.Rounded.MonitorHeart, // Activity
            gradientColors = listOf(Color(0xFFA855F7), Color(0xFF6366F1)) // Purple to Indigo
        ),
        HealthGoal(
            id = "general",
            title = "General Wellness",
            description = "Maintain a healthy, balanced lifestyle",
            icon = Icons.Rounded.TrackChanges, // Target
            gradientColors = listOf(CalorieKoGreen, CalorieKoLightGreen)
        )
    )

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
                // Target Icon Circle
                Surface(
                    shape = CircleShape,
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
                            imageVector = Icons.Rounded.TrackChanges,
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
                        text = "Health Goals",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What would you like to achieve?",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }

        // --- 3. Goals List ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp) // Extra padding at bottom
        ) {
            items(goals) { goal ->
                GoalItem(
                    goal = goal,
                    isSelected = selectedGoalId == goal.id,
                    onClick = { selectedGoalId = goal.id }
                )
            }
        }

        // --- 4. Continue Button ---
        Surface(
            color = Color.White,
            shadowElevation = 16.dp, // Subtle shadow for the bottom bar
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onContinue,
                enabled = selectedGoalId != null,
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
                // Gradient Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (selectedGoalId != null) Brush.horizontalGradient(
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- Helper Component: Detailed Goal Item ---
@Composable
fun GoalItem(
    goal: HealthGoal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) CalorieKoGreen else Color(0xFFE5E7EB), // gray-200
        label = "border"
    )
    val backgroundColor by animateColorAsState(
        if (isSelected) CalorieKoGreen.copy(alpha = 0.05f) else Color.White,
        label = "background"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top // Align top for better text wrapping
        ) {
            // Left: Gradient Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(goal.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = goal.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827) // gray-900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563), // gray-600
                    lineHeight = 20.sp
                )
            }

            // Right: Checkmark (Only if selected)
            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CalorieKoGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}