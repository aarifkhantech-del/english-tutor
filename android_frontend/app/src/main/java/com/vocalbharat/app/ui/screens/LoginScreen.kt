package com.vocalbharat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.ui.AuthUiState
import com.vocalbharat.app.R
import com.vocalbharat.app.ui.components.ScreenHeader
import com.vocalbharat.app.ui.theme.*

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onGoogleSignIn: () -> Unit,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    onResetOtp: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        ScreenHeader(
            title = "My Account",
            subtitle = "Sign in to sync your progress and Pro plan across devices 🔐",
            gradientColors = listOf(AppAccent, AppAccentEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AppAccent, AppAccentEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (uiState.isLoggedIn) {
                // Already Logged In View
                Text(
                    text = "Account Signed In",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = uiState.userProfile?.fullName?.ifBlank { null }
                        ?: listOf(uiState.userProfile?.firstName, uiState.userProfile?.lastName)
                            .filterNotNull().filter { it.isNotBlank() }.joinToString(" ").ifBlank { uiState.userEmail ?: "Signed in" },
                    color = AppAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(24.dp))

                uiState.userProfile?.takeIf { it.email.isNotBlank() }?.let { profile ->
                    Text(profile.email, color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = RecordingRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Back to App", color = TextPrimary)
                }
            } else if (!uiState.isOtpSent) {
                // ── Primary Google Sign-In, with OTP as a fallback ───────────
                Text(
                    text = "Sign in to VocalBharat",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Use your Google account to sync your progress and Pro plan.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onGoogleSignIn,
                    enabled = !uiState.isSigningInWithGoogle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (uiState.isSigningInWithGoogle) {
                        CircularProgressIndicator(color = AppAccent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_google_g),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Continue with Google", color = Color(0xFF202124), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("or use email OTP", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("you@example.com", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (emailInput.isNotBlank()) onRequestOtp(emailInput) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = AppAccent,
                        unfocusedLabelColor = TextSecondary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { onRequestOtp(emailInput) },
                    enabled = emailInput.isNotBlank() && !uiState.isSendingOtp,
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (uiState.isSendingOtp) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Continue with Email OTP", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // ── STEP 2: Enter OTP ──
                Text(
                    text = "Enter Verification Code",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Code sent to $emailInput",
                    color = AppAccent,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { if (it.length <= 6) otpInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("6-Digit OTP") },
                    placeholder = { Text("123456", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (otpInput.length == 6) onVerifyOtp(emailInput, otpInput) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = AppAccent,
                        unfocusedLabelColor = TextSecondary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { onVerifyOtp(emailInput, otpInput) },
                    enabled = otpInput.length == 6 && !uiState.isVerifyingOtp,
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (uiState.isVerifyingOtp) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Verify & Sign In ✓", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onResetOtp) {
                        Text("← Change Email", color = TextSecondary, fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = { onRequestOtp(emailInput) },
                        enabled = uiState.resendCountdown == 0 && !uiState.isSendingOtp
                    ) {
                        val text = if (uiState.resendCountdown > 0) "Resend (${uiState.resendCountdown}s)" else "Resend OTP"
                        Text(text, color = if (uiState.resendCountdown == 0) AppAccent else TextMuted, fontSize = 12.sp)
                    }
                }
            }

            // Error / Success Messages
            AnimatedVisibility(visible = uiState.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                uiState.errorMessage?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = RecordingRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(visible = uiState.successMessage != null, enter = fadeIn(), exit = fadeOut()) {
                uiState.successMessage?.let { msg ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = msg,
                        color = Color(0xFF059669),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
}
