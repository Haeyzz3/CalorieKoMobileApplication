package com.calorieko.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoLightOrange
import com.calorieko.app.ui.theme.CalorieKoOrange
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme

// Data class for activity levels
data class ActivityLevel(
    val id: String,
    val title: String,
    val description: String
)

@Composable
fun ActivityLevelScreen(onContinue: (String) -> Unit) {
    var selectedLevelId by remember { mutableStateOf<String?>(null) }

    val levels = listOf(
        ActivityLevel(
            id = "sedentary",
            title = "Sedentary",
            description = "Office worker, student, driver — mostly sitting throughout the day"
        ),
        ActivityLevel(
            id = "light",
            title = "Light Activity",
            description = "Teacher, nurse, housewife with chores — on your feet part of the day"
        ),
        ActivityLevel(
            id = "moderate",
            title = "Moderate Activity",
            description = "Farmer, manual laborer, regular athlete — sustained physical activity"
        ),
        ActivityLevel(
            id = "vigorous",
            title = "Vigorous Activity",
            description = "Logger, construction worker, athlete in training — heavy exertion daily"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Progress Bar (50%) ---
        val progress by animateFloatAsState(targetValue = 0.50f, label = "progress")

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
                // Activity Icon Circle
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
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "STEP 2 OF 4",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Activity Level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What is your baseline activity level?",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Not including workouts - we count that separately.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        // --- 3. Activity Level List ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(levels) { level ->
                ActivityLevelItem(
                    level = level,
                    isSelected = selectedLevelId == level.id,
                    onClick = { selectedLevelId = level.id }
                )
            }
        }

        // --- 4. Continue Button ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    selectedLevelId?.let { id -> onContinue(id) }
                },
                enabled = selectedLevelId != null,
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (selectedLevelId != null) Brush.horizontalGradient(
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

// --- Helper Component: Activity Level Item ---
@Composable
fun ActivityLevelItem(
    level: ActivityLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(300),
        label = "border"
    )
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
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
            verticalAlignment = Alignment.Top
        ) {
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = level.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio button indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
