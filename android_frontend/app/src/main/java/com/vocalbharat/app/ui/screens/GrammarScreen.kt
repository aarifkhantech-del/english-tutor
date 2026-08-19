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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.data.model.GrammarResponse
import com.vocalbharat.app.ui.GrammarUiState
import com.vocalbharat.app.ui.components.ScreenHeader
import com.vocalbharat.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarScreen(
    uiState: GrammarUiState,
    isPaid: Boolean,
    onTopicChange: (String) -> Unit,
    onExplain: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onClear: () -> Unit,
    onUpgrade: () -> Unit
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "micScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        ScreenHeader(
            title = "Grammar Explorer",
            subtitle = "Ask any English grammar topic & get clear Hindi explanations 📚",
            gradientColors = listOf(AppAccent, AppAccentEnd),
            extraContent = {
                if (isPaid) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Stars, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pro feature", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )

        if (!isPaid) {
            GrammarPaywallCard(onUpgrade = onUpgrade)
        } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

        // Input Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = AppAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Grammar Topic", color = AppAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Text field + mic row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.topic,
                    onValueChange = onTopicChange,
                    placeholder = {
                        Text(
                            "e.g. Past Tense, Articles, Prepositions...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = SurfaceBorder,
                        cursorColor = AppAccent,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.topic.isNotEmpty()) {
                            IconButton(onClick = { onTopicChange("") }) {
                                Icon(Icons.Default.Clear, null, tint = TextMuted)
                            }
                        }
                    }
                )

                // Mic button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(micScale)
                        .clip(CircleShape)
                        .background(
                            if (uiState.isRecording)
                                Brush.radialGradient(listOf(RecordingRed, Color(0xFFFF6D00)))
                            else
                                Brush.radialGradient(listOf(AppAccent, AppAccentEnd))
                        )
                        .clickable { if (uiState.isRecording) onStopVoice() else onStartVoice() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (uiState.isRecording) {
                Text(
                    text = "🎙 Listening... speak your topic",
                    color = RecordingRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Explain button
            Button(
                onClick = onExplain,
                enabled = uiState.topic.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppAccent,
                    disabledContainerColor = SurfaceBorder
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Explaining...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Explain Topic", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Error banner
        uiState.errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RecordingRed.copy(alpha = 0.10f))
                    .border(1.dp, RecordingRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = RecordingRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(error, color = RecordingRed, fontSize = 13.sp)
            }
        }

        // Result
        AnimatedVisibility(
            visible = uiState.result != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.result?.let { response ->
                GrammarResultCard(response = response, onClear = onClear)
            }
        }

        Spacer(Modifier.height(20.dp))
        }
        }
    }
}

@Composable
private fun GrammarPaywallCard(onUpgrade: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AppAccent, AppAccentEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(
                "Grammar Explorer is a Pro feature",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Subscribe to Monthly Pro to ask any English grammar topic and get clear Hindi explanations, examples, and tips.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Icon(Icons.Default.Stars, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Plans (₹120/mo)", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GrammarResultCard(response: GrammarResponse, onClear: () -> Unit) {
    val difficultyColor = when (response.difficulty) {
        "Beginner" -> Color(0xFF059669)
        "Intermediate" -> Color(0xFFD97706)
        "Advanced" -> Color(0xFFDC2626)
        else -> AppAccent
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Topic + difficulty + clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = response.topic,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Difficulty chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(difficultyColor.copy(alpha = 0.12f))
                    .border(1.dp, difficultyColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(response.difficulty, color = difficultyColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }

        // English definition card
        GlassCard(
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = AppAccent, modifier = Modifier.size(18.dp)) },
            label = "Definition"
        ) {
            Text(response.definition, color = TextPrimary, fontSize = 14.sp, lineHeight = 21.sp)
        }

        // Hindi definition card
        GlassCard(
            icon = { Text("🇮🇳", fontSize = 16.sp) },
            label = "हिंदी में"
        ) {
            Text(response.hindiDefinition, color = TextPrimary, fontSize = 14.sp, lineHeight = 21.sp)
        }

        // Examples
        if (response.examples.isNotEmpty()) {
            GlassCard(
                icon = { Icon(Icons.Default.FormatListNumbered, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp)) },
                label = "Examples"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    response.examples.forEachIndexed { index, example ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AppAccent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = AppAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = example.sentence,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = example.explanation,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tips
        if (response.tips.isNotEmpty()) {
            GlassCard(
                icon = { Icon(Icons.Default.TipsAndUpdates, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp)) },
                label = "Tips"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    response.tips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("✦", color = Color(0xFF059669), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tip, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    icon: @Composable () -> Unit,
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}
