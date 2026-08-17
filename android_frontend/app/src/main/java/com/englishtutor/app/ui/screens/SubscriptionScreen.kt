package com.englishtutor.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtutor.app.data.model.PlanInfo
import com.englishtutor.app.ui.SubscriptionUiState
import com.englishtutor.app.ui.theme.AccentTeal

@Composable
fun SubscriptionScreen(
    uiState: SubscriptionUiState,
    isLoggedIn: Boolean,
    userEmail: String?,
    serverUrl: String,
    onSelectPlan: (String) -> Unit,
    onSubscribe: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onOpenWebCheckout: () -> Unit = {},
    onCheckStatus: () -> Unit = {}
) {

    val scrollState = rememberScrollState()

    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0E1A), Color(0xFF0D1F3C), Color(0xFF0A2744))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Text(
            text = "Upgrade Your Learning",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Master spoken English with unlimited AI coaching",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        // ── Active Subscription Status Card (if user has active/past sub) ──
        if (isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (uiState.status.isActive) Color(0xFF00E676).copy(alpha = 0.12f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (uiState.status.isActive) Color(0xFF00E676).copy(alpha = 0.35f)
                        else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.status.isActive) Color(0xFF00E676) else Color(0xFFFF5252))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.status.isActive) "Subscription Active" else "No Active Plan",
                                color = if (uiState.status.isActive) Color(0xFF00E676) else Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (uiState.status.isActive) {
                            Text(
                                text = "${uiState.status.daysRemaining} days left",
                                color = AccentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Account: ${userEmail ?: "Logged in"}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )

                    if (uiState.status.isActive && uiState.status.plan != null) {
                        Text(
                            text = "Plan: Monthly Pro (₹120/mo) · Active ⚡",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        val startFormatted = formatIsoDate(uiState.status.startsAt)
                        val endFormatted = formatIsoDate(uiState.status.expiresAt)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (startFormatted.isNotEmpty()) {
                                Text(
                                    text = "Started: $startFormatted",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            if (endFormatted.isNotEmpty()) {
                                Text(
                                    text = "Valid Until: $endFormatted",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Free Tier: ${uiState.status.requestsUsed} / ${uiState.status.requestsLimit} requests used (${uiState.status.requestsRemaining} left)",
                            color = if (uiState.status.quotaExceeded) Color(0xFFFF5252) else AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        } else {
            // Guest warning banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2979FF).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF2979FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(Icons.Default.Info, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sign in with email to activate your 8 free requests and keep your subscription across devices.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        // Messages
        AnimatedVisibility(visible = uiState.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        AnimatedVisibility(visible = uiState.successMessage != null, enter = fadeIn(), exit = fadeOut()) {
            uiState.successMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color(0xFF00E676),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // ── Plan Cards ──
        val plans = if (uiState.plans.isNotEmpty()) uiState.plans else listOf(
            PlanInfo("monthly", "Monthly Pro Plan", "Unlimited access to all AI coaching and grammar tools.", 120, "INR", 30, "Recommended")
        )

        plans.forEach { plan ->
            PlanCard(
                plan = plan,
                isSelected = uiState.selectedPlanId == plan.id,
                isActive = uiState.status.isActive,
                daysRemaining = uiState.status.daysRemaining,
                expiresAt = uiState.status.expiresAt,
                isProcessing = uiState.isProcessingPayment && uiState.selectedPlanId == plan.id,
                onSelect = { onSelectPlan(plan.id) },
                onSubscribe = { onSubscribe(plan.id) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Already Paid Check Button (only when not active) ──
        if (!uiState.status.isActive) {
            OutlinedButton(
                onClick = onCheckStatus,
                enabled = !uiState.isProcessingPayment,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF5393FF)
                ),
                border = BorderStroke(1.dp, Color(0xFF2979FF).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Already Paid via UPI? Check Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = "🔒 Secured by Razorpay · UPI, Cards, NetBanking",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

    }
}

@Composable
fun PlanCard(
    plan: PlanInfo,
    isSelected: Boolean,
    isActive: Boolean = false,
    daysRemaining: Int = 0,
    expiresAt: String? = null,
    isProcessing: Boolean,
    onSelect: () -> Unit,
    onSubscribe: () -> Unit
) {
    val endFormatted = formatIsoDate(expiresAt)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) Color(0xFF00E676).copy(alpha = 0.08f)
                else if (isSelected) Color(0xFF2979FF).copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                width = if (isActive || isSelected) 2.dp else 1.dp,
                color = if (isActive) Color(0xFF00E676) else if (isSelected) AccentTeal else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !isActive, onClick = onSelect)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isActive) "✔ ACTIVE PLAN" else (plan.badge ?: "Unlimited Pro"),
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Price
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹${plan.amount}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " / month",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = plan.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = plan.description,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            // Feature bullets
            val features = listOf(
                "Unlimited Hindi to English spoken translation",
                "Real-time AI grammar & vocabulary coaching",
                "High-quality text-to-speech pronunciation audio",
                "Full access to Grammar Explorer with tips",
                "Zero advertisements & priority AI compute"
            )

            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = feature,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isActive) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFF00E676).copy(alpha = 0.15f),
                        disabledContentColor = Color(0xFF00E676)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (endFormatted.isNotEmpty()) "Active Plan — Valid until $endFormatted" else "Active Plan ($daysRemaining days left)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2979FF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "🚀 Upgrade to Monthly Pro (₹120)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        }
    }
}

fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val cleaned = isoString.substringBefore(".").substringBefore("Z")
        val parsed = java.time.LocalDateTime.parse(cleaned, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    } catch (_: Exception) {
        try {
            val dateOnly = isoString.take(10)
            val parsedDate = java.time.LocalDate.parse(dateOnly)
            parsedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
        } catch (_: Exception) {
            isoString
        }
    }
}


