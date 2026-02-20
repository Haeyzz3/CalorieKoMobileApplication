package com.calorieko.app.ui.screens

import com.calorieko.app.R
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── Brand Colors (reused from theme) ───
private val CalorieKoGreen = Color(0xFF4CAF50)
private val CalorieKoLightGreen = Color(0xFF81C784)
private val CalorieKoOrange = Color(0xFFFDB05E)
private val CalorieKoDeepOrange = Color(0xFFFF9800)
private val TextDark = Color(0xFF2D2D2D)
private val TextGray = Color(0xFF8E8E8E)
private val FieldBackground = Color(0xFFF7F8FA)
private val FieldBorder = Color(0xFFE0E0E0)
private val GoogleRed = Color(0xFFDB4437)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ─── Email/Password Sign In ───
    fun signInWithEmail() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
        isLoading = true
        errorMessage = null
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    errorMessage = task.exception?.message ?: "Login failed. Please try again."
                }
            }
    }

    // ─── Google Sign In ───
    // ─── Google Sign In ───
    fun signInWithGoogle() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val credentialManager = CredentialManager.create(context)

                // TODO: Replace with your actual Web Client ID from Firebase Console
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("945279693201-4i2r0vmiuo743h58rdhm80uh75vpfk96.apps.googleusercontent.com")
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity
                )

                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                val firebaseCredential = GoogleAuthProvider
                    .getCredential(googleIdTokenCredential.idToken, null)

                // Capture the authentication result
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                isLoading = false

                // Determine if this is a first-time Google login
                val isNewUser = authResult.additionalUserInfo?.isNewUser == true

                if (isNewUser) {
                    // Redirects to bioForm to set up profile
                    onNavigateToSignUp()
                } else {
                    // Redirects straight to the dashboard
                    onLoginSuccess()
                }

            } catch (e: GetCredentialCancellationException) {
                isLoading = false
                // User cancelled — do nothing
            } catch (e: Exception) {
                isLoading = false
                Log.e("LoginScreen", "Google Sign-In failed", e)
                errorMessage = "Google Sign-In failed. Please try again."
            }
        }
    }

    // ═══════════════════════ UI ═══════════════════════

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Top Hero Banner ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Gradient Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CalorieKoGreen,
                                    CalorieKoLightGreen
                                )
                            )
                        )
                )

                // Logo + Welcome Text
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo with white circle background
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.calorieko_logo),
                                contentDescription = "CalorieKo Logo",
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Welcome Back!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sign in to continue your journey",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── Login Form Card ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error Message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                // ─── Email Field ───
                Text(
                    text = "Email",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter your email", color = TextGray, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = CalorieKoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen,
                        unfocusedBorderColor = FieldBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = FieldBackground,
                        cursorColor = CalorieKoGreen
                    )
                )

                // ─── Password Field ───
                Text(
                    text = "Password",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter your password", color = TextGray, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = CalorieKoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalorieKoGreen,
                        unfocusedBorderColor = FieldBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = FieldBackground,
                        cursorColor = CalorieKoGreen
                    )
                )

                // ─── Forgot Password ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = CalorieKoGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToForgotPassword() }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ─── Sign In Button (Orange Gradient) ───
                Button(
                    onClick = { signInWithEmail() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CalorieKoOrange, CalorieKoDeepOrange)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // ─── Divider ───
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FieldBorder
                    )
                    Text(
                        text = "  or continue with  ",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FieldBorder
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ─── Google Sign-In Button ───
                OutlinedButton(
                    onClick = { signInWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    enabled = !isLoading
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Google "G" icon using text (no external asset needed)
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Sign Up Link ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSignUp() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sign Up",
                        color = CalorieKoGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
