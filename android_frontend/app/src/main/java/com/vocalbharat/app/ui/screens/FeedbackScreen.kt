package com.vocalbharat.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.AppConfig
import com.vocalbharat.app.ui.FeedbackUiState
import com.vocalbharat.app.ui.theme.*

val StarAmber  = Color(0xFFFFB300)
val HelpOrange = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    uiState: FeedbackUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
    onDismissError: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        com.vocalbharat.app.ui.components.ScreenHeader(
            title = "Share Your Feedback",
            subtitle = "Your thoughts go directly to the developer ⚡",
            gradientColors = listOf(AppAccent, AppAccentEnd)
        )

        // ── Error Snackbar ───────────────────────────────────────────────────
        uiState.errorMessage?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .clickable { onDismissError() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null,
                    tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = err, color = Color(0xFFEF4444), fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
        }

        // ── Success State ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.isSuccess,
            enter = fadeIn() + slideInVertically()
        ) {
            SuccessCard(
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(36.dp)) },
                gradientColors = listOf(AppAccent, AppAccentEnd),
                title = "Thank You! 🎉",
                message = uiState.successMessage,
                ticketId = uiState.ticketId,
                onReset = onReset
            )
        }

        // ── Form ─────────────────────────────────────────────────────────────
        AnimatedVisibility(visible = !uiState.isSuccess) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {

                // Star Rating
                StarRatingSection(
                    rating = uiState.rating,
                    onRatingChange = onRatingChange
                )

                // Name + Email
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeedbackTextField(
                        label = "Your Name *",
                        value = uiState.name,
                        placeholder = "Rahul Sharma",
                        onValueChange = onNameChange,
                        modifier = Modifier.weight(1f)
                    )
                    FeedbackTextField(
                        label = "Email *",
                        value = uiState.email,
                        placeholder = "name@email.com",
                        onValueChange = onEmailChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category Dropdown
                CategoryDropdown(
                    label = "Category *",
                    selected = uiState.category,
                    options = listOf(
                        "general" to "General Feedback",
                        "feature" to "Feature Request",
                        "bug" to "Bug Report",
                        "praise" to "Praise / Love it!"
                    ),
                    onSelect = onCategoryChange
                )

                // Message
                FeedbackTextField(
                    label = "Your Feedback *",
                    value = uiState.message,
                    placeholder = "Tell us what you think about VocalBharat…",
                    onValueChange = onMessageChange,
                    singleLine = false,
                    minLines = 5
                )

                // Submit button
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppAccent,
                        disabledContainerColor = SurfaceBorder
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Submit Feedback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // Direct contact note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppAccent.copy(alpha = 0.07f))
                        .border(1.dp, AppAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, contentDescription = null,
                        tint = AppAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Or email directly: vocalbharat91@gmail.com",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// STAR RATING SECTION
// =============================================================================

@Composable
fun StarRatingSection(rating: Int, onRatingChange: (Int) -> Unit) {
    val labels = listOf("", "Poor", "Fair", "Good", "Very Good", "Excellent!")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "OVERALL RATING *",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..5) {
                val isActive = i <= rating
                val animScale by animateFloatAsState(
                    targetValue = if (isActive) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "star_scale"
                )
                Text(
                    text = "★",
                    fontSize = (32 * animScale).sp,
                    color = if (isActive) StarAmber else SurfaceBorder,
                    modifier = Modifier
                        .clickable { onRatingChange(i) }
                        .clip(CircleShape)
                        .padding(4.dp)
                )
            }
        }
        if (rating > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = labels.getOrElse(rating) { "" },
                color = StarAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================================
// HELP SCREEN
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    uiState: com.vocalbharat.app.ui.HelpUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onIssueTypeChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDeviceChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
    onDismissError: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        com.vocalbharat.app.ui.components.ScreenHeader(
            title = "Help & Support",
            subtitle = "We'll reply to your email within 24 hours 🆘",
            gradientColors = listOf(Color(0xFFFF5252), Color(0xFFFF8F00))
        )

        // ── FAQ Section ──────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(
                text = "Common Questions",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            FaqItem(
                question = "Microphone not working?",
                answer = "Allow mic permission: Settings → Apps → VocalBharat → Permissions → Microphone → Allow."
            )
            FaqItem(
                question = "Payment done but Pro not active?",
                answer = "Sign in with the same email used during payment. Still not working? File a help request below with your payment ID."
            )
            FaqItem(
                question = "How to cancel subscription?",
                answer = "Your plan is for 30 days and does not auto-renew. No action needed to cancel."
            )
            FaqItem(
                question = "How do I get a refund?",
                answer = "Email vocalbharat91@gmail.com with your Razorpay payment ID within 7 days."
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { uriHandler.openUri(AppConfig.PRIVACY_POLICY_URL) }) {
                Text("Privacy Policy", color = AccentCyan, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = SurfaceBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

        // ── Error ────────────────────────────────────────────────────────────
        uiState.errorMessage?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HelpOrange.copy(alpha = 0.12f))
                    .border(1.dp, HelpOrange.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .clickable { onDismissError() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null,
                    tint = HelpOrange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(err, color = HelpOrange, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
        }

        // ── Success State ────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.isSuccess, enter = fadeIn() + slideInVertically()) {
            SuccessCard(
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(36.dp)) },
                gradientColors = listOf(Color(0xFFFF5252), Color(0xFFFF8F00)),
                title = "Help Request Received!",
                message = uiState.successMessage,
                ticketId = uiState.ticketId,
                onReset = onReset
            )
        }

        // ── Help Form ────────────────────────────────────────────────────────
        AnimatedVisibility(visible = !uiState.isSuccess) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Contact Support",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeedbackTextField(
                        label = "Your Name *",
                        value = uiState.name,
                        placeholder = "Priya Sharma",
                        onValueChange = onNameChange,
                        modifier = Modifier.weight(1f)
                    )
                    FeedbackTextField(
                        label = "Email *",
                        value = uiState.email,
                        placeholder = "name@email.com",
                        onValueChange = onEmailChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryDropdown(
                        label = "Issue Type *",
                        selected = uiState.issueType,
                        options = listOf(
                            "payment" to "Payment / Subscription",
                            "account" to "Account / Login",
                            "technical" to "Technical / App",
                            "audio" to "Audio / Mic",
                            "other" to "Other"
                        ),
                        onSelect = onIssueTypeChange,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryDropdown(
                        label = "Device",
                        selected = uiState.device,
                        options = listOf(
                            "android" to "Android App",
                            "web" to "Web Browser",
                            "other" to "Other"
                        ),
                        onSelect = onDeviceChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                FeedbackTextField(
                    label = "Subject *",
                    value = uiState.subject,
                    placeholder = "e.g. Payment done but Pro not active",
                    onValueChange = onSubjectChange
                )

                FeedbackTextField(
                    label = "Describe Your Issue *",
                    value = uiState.description,
                    placeholder = "Describe what happened, any errors you saw, and steps you tried. More detail = faster help.",
                    onValueChange = onDescriptionChange,
                    singleLine = false,
                    minLines = 5
                )

                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        disabledContainerColor = SurfaceBorder
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Help Request", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// SHARED COMPOSABLE COMPONENTS
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.sp
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
            singleLine = singleLine,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AppAccent,
                unfocusedBorderColor = SurfaceBorder,
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.first == selected }?.second ?: selected

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.sp
        )
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = displayLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = SurfaceBorder,
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                options.forEach { (value, display) ->
                    DropdownMenuItem(
                        text = { Text(display, color = TextPrimary, fontSize = 14.sp) },
                        onClick = { onSelect(value); expanded = false },
                        modifier = Modifier.background(
                            if (value == selected) AppAccent.copy(alpha = 0.15f) else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (expanded) AppAccent.copy(alpha = 0.4f) else SurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (expanded) PrimaryLight else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
fun SuccessCard(
    icon: @Composable () -> Unit,
    gradientColors: List<Color>,
    title: String,
    message: String,
    ticketId: String,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        if (ticketId.isNotBlank()) {
            Text(
                text = "🎫 Ticket: $ticketId",
                color = AccentTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentTeal.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = gradientColors.first())
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Submit Another", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
