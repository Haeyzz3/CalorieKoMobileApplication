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
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoLightOrange
import com.calorieko.app.ui.theme.CalorieKoOrange

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
            id = "not_very_active",
            title = "Not Very Active",
            description = "Spend most of the day sitting (e.g. bankteller, desk job)"
        ),
        ActivityLevel(
            id = "lightly_active",
            title = "Lightly Active",
            description = "Spend a good part of the day on your feet (e.g. teacher, salesperson)"
        ),
        ActivityLevel(
            id = "active",
            title = "Active",
            description = "Spend a good part of the day doing some physical activity (e.g. food server, postal carrier)"
        ),
        ActivityLevel(
            id = "very_active",
            title = "Very Active",
            description = "Spend most of the day doing heavy physical activity (e.g. bike messenger, carpenter)"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 1. Progress Bar (50%) ---
        val progress by animateFloatAsState(targetValue = 0.50f, label = "progress")

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
                // Activity Icon Circle
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
                            imageVector = Icons.Default.DirectionsRun,
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
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Activity Level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What is your baseline activity level?",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Not including workouts - we count that separately.",
                color = Color.Gray,
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
            color = Color.White,
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
        if (isSelected) CalorieKoGreen else Color(0xFFE5E7EB),
        animationSpec = tween(300),
        label = "border"
    )
    val backgroundColor by animateColorAsState(
        if (isSelected) CalorieKoGreen.copy(alpha = 0.05f) else Color.White,
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
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = level.description,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio button indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CalorieKoGreen,
                    unselectedColor = Color(0xFFD1D5DB)
                )
            )
        }
    }
}
