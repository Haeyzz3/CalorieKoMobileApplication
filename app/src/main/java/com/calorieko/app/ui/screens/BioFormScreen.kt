package com.calorieko.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoLightOrange
import com.calorieko.app.ui.theme.CalorieKoOrange
import kotlin.math.roundToInt

@Composable
fun BioFormScreen(onContinue: (String, String, String, String, String) -> Unit) {
    val scrollState = rememberScrollState()

    // Local State for the form
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") } // "Male" or "Female"
    var weight by remember { mutableStateOf("") }

    // Height States
    var heightUnit by remember { mutableStateOf("cm") } // "cm" or "ft"
    var heightCm by remember { mutableStateOf("") }
    var heightFt by remember { mutableStateOf("") }
    var heightIn by remember { mutableStateOf("") }

    // --- Validation Logic ---
    val ageInt = age.toIntOrNull()
    val isAgeValid = ageInt != null && ageInt in 13..120
    val showAgeError = age.isNotEmpty() && !isAgeValid

    val weightDouble = weight.toDoubleOrNull()
    val isWeightValid = weightDouble != null && weightDouble in 20.0..300.0
    val showWeightError = weight.isNotEmpty() && !isWeightValid

    // Calculate height dynamically based on unit
    val computedHeightCm: Double = if (heightUnit == "cm") {
        heightCm.toDoubleOrNull() ?: 0.0
    } else {
        val ft = heightFt.toDoubleOrNull() ?: 0.0
        val inches = heightIn.toDoubleOrNull() ?: 0.0
        (ft * 30.48) + (inches * 2.54)
    }

    val isHeightValid = computedHeightCm in 50.0..250.0
    val showHeightError = if (heightUnit == "cm") {
        heightCm.isNotEmpty() && !isHeightValid
    } else {
        (heightFt.isNotEmpty() || heightIn.isNotEmpty()) && !isHeightValid
    }

    // Form is valid only if all inputs meet the requirements
    val isFormValid = name.isNotBlank() && isAgeValid && sex.isNotBlank() && isWeightValid && isHeightValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 1. Progress Bar ---
        val progress by animateFloatAsState(targetValue = 0.25f, label = "progress")

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
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(CalorieKoGreen, CalorieKoLightGreen))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("STEP 1 OF 4", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Your Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Help us personalize your experience", color = Color.Gray, fontSize = 16.sp)
        }

        // --- 3. Form Fields ---
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Full Name
            NameInput(value = name, onValueChange = { name = it }, label = "Full Name", placeholder = "Enter your full name")

            // Age
            Column {
                CustomInput(
                    value = age,
                    onValueChange = { age = it },
                    label = "Age",
                    placeholder = "Enter your age",
                    isError = showAgeError
                )
                if (showAgeError) ErrorText("Age must be between 13 and 120")
            }

            // Sex Selection
            Column {
                Text("Sex", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SexButton(text = "Male", isSelected = sex == "Male", onClick = { sex = "Male" }, modifier = Modifier.weight(1f))
                    SexButton(text = "Female", isSelected = sex == "Female", onClick = { sex = "Female" }, modifier = Modifier.weight(1f))
                }
            }

            // --- HEIGHT SECTION WITH TOGGLE ---
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Height", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)

                    // Toggle Button
                    Surface(
                        onClick = {
                            heightUnit = if (heightUnit == "cm") "ft" else "cm"
                            // Clear inputs when switching units to prevent confusion
                            heightCm = ""; heightFt = ""; heightIn = ""
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = CalorieKoGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (heightUnit == "cm") "Switch to ft/in" else "Switch to cm",
                            fontSize = 12.sp,
                            color = CalorieKoGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (heightUnit == "cm") {
                    CustomInput(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        label = "Centimeters (cm)",
                        placeholder = "e.g., 170",
                        isError = showHeightError
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomInput(
                            value = heightFt,
                            onValueChange = { heightFt = it },
                            label = "Feet (ft)",
                            placeholder = "e.g., 5",
                            modifier = Modifier.weight(1f),
                            isError = showHeightError
                        )
                        CustomInput(
                            value = heightIn,
                            onValueChange = { heightIn = it },
                            label = "Inches (in)",
                            placeholder = "e.g., 7",
                            modifier = Modifier.weight(1f),
                            isError = showHeightError
                        )
                    }
                }
                if (showHeightError) ErrorText("Height must be between 50cm and 250cm (1'8\" - 8'2\")")
            }

            // Weight
            Column {
                CustomInput(
                    value = weight,
                    onValueChange = { weight = it },
                    label = "Weight (kg)",
                    placeholder = "Enter your weight",
                    isError = showWeightError
                )
                if (showWeightError) ErrorText("Weight must be between 20kg and 300kg")
            }
        }

        // --- 4. Continue Button ---
        Button(
            onClick = {
                // Pass the strictly calculated cm string to the next screen
                val finalHeightString = computedHeightCm.roundToInt().toString()
                onContinue(name, age, finalHeightString, weight, sex)
            },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isFormValid) Brush.horizontalGradient(
                            colors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                        ) else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, null, tint = Color.White)
                }
            }
        }
    }
}

// --- Helper Components ---

@Composable
fun ErrorText(text: String) {
    Text(
        text = text,
        color = Color(0xFFEF4444), // Tailwind Red 500
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
fun NameInput(
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
        leadingIcon = {
            Icon(Icons.Default.Person, null, tint = CalorieKoGreen, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CalorieKoGreen,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = CalorieKoGreen,
            unfocusedContainerColor = Color(0xFFFAFAFA),
            focusedContainerColor = Color.White
        ),
        singleLine = true
    )
}

@Composable
fun CustomInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.LightGray) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CalorieKoGreen,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = CalorieKoGreen,
            unfocusedContainerColor = Color(0xFFFAFAFA),
            focusedContainerColor = Color.White,
            errorBorderColor = Color(0xFFEF4444),
            errorLabelColor = Color(0xFFEF4444)
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