package com.calorieko.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoLightOrange
import com.calorieko.app.ui.theme.CalorieKoOrange
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme

// Data class to define our specific goals
data class HealthGoal(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@Composable
fun GoalSelectionScreen(onContinue: (String) -> Unit) {
    var selectedGoalId by remember { mutableStateOf<String?>(null) }

    // Define the 3 specific goals
    val goals = listOf(
        HealthGoal(
            id = "gain_muscle",
            title = "Gain Muscle",
            description = "Build lean muscle through optimized protein and nutrition",
            icon = Icons.Default.FitnessCenter,
            gradientColors = listOf(Color(0xFFEF4444), Color(0xFFEC4899)) // Red to Pink
        ),
        HealthGoal(
            id = "weight_loss",
            title = "Weight Control",
            description = "Achieve and maintain your ideal weight",
            icon = Icons.Default.MonitorWeight,
            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)) // Blue to Cyan
        ),
        HealthGoal(
            id = "general",
            title = "General Health & Wellness",
            description = "Maintain a healthy, balanced lifestyle",
            icon = Icons.Default.Favorite, // Heart
            gradientColors = listOf(CalorieKoGreen, CalorieKoLightGreen)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Progress Bar (75%) ---
        val progress by animateFloatAsState(targetValue = 0.75f, label = "progress")

        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
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
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White,
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
                        text = "STEP 3 OF 4",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Health Goals",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What would you like to achieve?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp, // Subtle shadow for the bottom bar
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    // Pass the selected ID to the parent activity
                    selectedGoalId?.let { id -> onContinue(id) }
                },
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
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, // gray-200 replacement
        label = "border"
    )
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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