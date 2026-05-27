package com.calorieko.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoLightGreen
import com.calorieko.app.ui.theme.CalorieKoLightOrange
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.calorieko.app.viewmodel.VerificationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationPendingScreen(
    viewModel: VerificationViewModel,
    initialVerificationEmailSent: Boolean = true,
    initialMessage: String? = null,
    onVerificationSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isVerified by viewModel.isVerified.collectAsState()
    val resendCooldown by viewModel.resendCooldown.collectAsState()
    val message by viewModel.message.collectAsState()

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(initialVerificationEmailSent, initialMessage) {
        viewModel.setInitialVerificationState(initialVerificationEmailSent, initialMessage)
    }

    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
        
        viewModel.events.collect { event ->
            when (event) {
                is VerificationViewModel.Event.VerificationSuccess -> onVerificationSuccess()
                is VerificationViewModel.Event.LoggedOut -> onCancel()
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        if (isVerified) listOf(Color(0xFF38A169), Color(0xFF68D391))
                                        else listOf(CalorieKoGreen, CalorieKoLightGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVerified) Icons.Rounded.CheckCircle else Icons.Rounded.MarkEmailUnread,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Verify Your Email",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "We've sent a verification link to your email address. Please click the link in that email to continue.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { 40 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Check Status Button
                    Button(
                        onClick = { viewModel.checkVerificationStatus() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading && !isVerified,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        if (isVerified) listOf(Color(0xFF38A169), Color(0xFF68D391))
                                        else listOf(CalorieKoOrange, CalorieKoLightOrange)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (isVerified) Icons.Rounded.CheckCircle else Icons.Rounded.Refresh, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isVerified) "Verified!" else "Check Status", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // Resend Email
                    OutlinedButton(
                        onClick = { viewModel.resendVerification() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading && !isVerified && resendCooldown == 0,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (resendCooldown > 0) Color.LightGray else CalorieKoGreen)
                    ) {
                        Text(
                            text = if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend Verification Email",
                            color = if (resendCooldown > 0) Color.LightGray else CalorieKoGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Cancel / Logout
                    TextButton(
                        onClick = { viewModel.cancelRegistration() },
                        enabled = !isLoading && !isVerified,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Registration", color = Color.Gray)
                    }
                }
            }

            // Message / Error
            message?.let { msg ->
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = if (msg.contains("sent")) Color(0xFFE6FFFA) else Color(0xFFFFEBEB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = if (msg.contains("sent")) Color(0xFF2C7A7B) else Color(0xFFC53030),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
