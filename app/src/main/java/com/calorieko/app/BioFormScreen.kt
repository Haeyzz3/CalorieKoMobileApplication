package com.calorieko.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.*

@Composable
fun BioFormScreen(onContinue: (String, String, String, String) -> Unit) {
    // Local State for the form
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") } // "Male" or "Female"
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    // Check if form is valid to enable button
    val isFormValid = age.isNotEmpty() && sex.isNotEmpty() && height.isNotEmpty() && weight.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 1. Progress Bar ---
        // We animate the progress just like Framer Motion
        val progress by animateFloatAsState(targetValue = 0.33f, label = "progress")

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
                // User Icon Circle
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent, // We'll use a box for gradient
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "STEP 1 OF 3",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Help us personalize your experience",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }

        // --- 3. Form Fields ---
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .weight(1f), // Takes up remaining space
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Age
            CustomInput(
                value = age,
                onValueChange = { age = it },
                label = "Age",
                placeholder = "Enter your age"
            )

            // Sex Selection
            Column {
                Text(
                    text = "Sex",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SexButton(
                        text = "Male",
                        isSelected = sex == "Male",
                        onClick = { sex = "Male" },
                        modifier = Modifier.weight(1f)
                    )
                    SexButton(
                        text = "Female",
                        isSelected = sex == "Female",
                        onClick = { sex = "Female" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Height
            CustomInput(
                value = height,
                onValueChange = { height = it },
                label = "Height (cm)",
                placeholder = "Enter your height"
            )

            // Weight
            CustomInput(
                value = weight,
                onValueChange = { weight = it },
                label = "Weight (kg)",
                placeholder = "Enter your weight"
            )
        }

        // --- 4. Continue Button ---
        Button(
            onClick = { onContinue(age, height, weight, sex) }, // <--- PASS DATA HERE
            enabled = isFormValid,
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
            // Gradient Background (only visible if enabled)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isFormValid) Brush.horizontalGradient(
                            colors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                        ) else Brush.linearGradient(listOf(Color.Gray, Color.Gray)), // Fallback for disabled
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

// --- Helper Components ---

@Composable
fun CustomInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.LightGray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CalorieKoGreen,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = CalorieKoGreen,
            unfocusedContainerColor = Color(0xFFFAFAFA), // Slightly gray background like design
            focusedContainerColor = Color.White
        ),
        singleLine = true
    )
}

@Composable
fun SexButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) CalorieKoGreen else Color.LightGray.copy(alpha = 0.3f)
        ),
        color = if (isSelected) CalorieKoGreen.copy(alpha = 0.05f) else Color.White,
        modifier = modifier.height(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) CalorieKoGreen else Color.Gray
            )
        }
    }
}