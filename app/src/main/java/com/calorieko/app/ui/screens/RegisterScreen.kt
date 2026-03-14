package com.calorieko.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoLightOrange
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Validation
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 6
    val doPasswordsMatch = password == confirmPassword && password.isNotEmpty()
    val isFormValid = isEmailValid && isPasswordValid && doPasswordsMatch

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top
        ) {
            
            // --- 1. Progress Bar (100%) ---
            val progress by animateFloatAsState(targetValue = 1.00f, label = "progress")

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
                    // Lock Icon Circle
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
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "STEP 4 OF 4",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Register",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Almost there! Let's secure your profile.",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }

            // Inner content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CalorieKoGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen, unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedLabelColor = CalorieKoGreen, unfocusedContainerColor = Color(0xFFFAFAFA), focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
                if (email.isNotEmpty() && !isEmailValid) {
                    ErrorText("Please enter a valid email address")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    placeholder = { Text("At least 6 characters", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CalorieKoGreen) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Toggle Password Visibility", tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen, unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedLabelColor = CalorieKoGreen, unfocusedContainerColor = Color(0xFFFAFAFA), focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
                if (password.isNotEmpty() && !isPasswordValid) {
                    ErrorText("Password must be at least 6 characters")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CalorieKoGreen) },
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = "Toggle Password Visibility", tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen, unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedLabelColor = CalorieKoGreen, unfocusedContainerColor = Color(0xFFFAFAFA), focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
                if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                    ErrorText("Passwords do not match")
                }

                // Error Message from Firebase
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = it, color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Sign Up Button
                Button(
                    onClick = {
                        if (isFormValid) {
                            isLoading = true
                            errorMessage = null
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onSignUpSuccess()
                                    } else {
                                        errorMessage = task.exception?.localizedMessage ?: "Registration failed. Please try again."
                                    }
                                }
                        }
                    },
                    enabled = isFormValid && !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.LightGray),
                    contentPadding = PaddingValues()
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (isFormValid && !isLoading) Brush.horizontalGradient(
                                    colors = listOf(CalorieKoOrange, CalorieKoLightOrange)
                                ) else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Complete Registration", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
