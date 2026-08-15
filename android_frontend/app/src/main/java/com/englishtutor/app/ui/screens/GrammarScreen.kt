package com.englishtutor.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtutor.app.data.model.GrammarResponse
import com.englishtutor.app.ui.GrammarUiState
import com.englishtutor.app.ui.theme.AccentTeal
import com.englishtutor.app.ui.theme.RecordingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarScreen(
    uiState: GrammarUiState,
    serverUrl: String,
    onTopicChange: (String) -> Unit,
    onExplain: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onClear: () -> Unit
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
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Grammar Explorer",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "कोई भी English Grammar topic पूछें",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Input Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Grammar Topic", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                        Text("e.g. Past Tense, Articles, Prepositions...",
                            color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = AccentTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.topic.isNotEmpty()) {
                            IconButton(onClick = { onTopicChange("") }) {
                                Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.5f))
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
                                Brush.radialGradient(listOf(Color(0xFF2979FF), AccentTeal))
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
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2979FF),
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Explaining...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Explain Topic", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Error banner
        uiState.errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RecordingRed.copy(alpha = 0.12f))
                    .border(1.dp, RecordingRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = RecordingRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(error, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
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

@Composable
fun GrammarResultCard(response: GrammarResponse, onClear: () -> Unit) {
    val difficultyColor = when (response.difficulty) {
        "Beginner" -> Color(0xFF00E676)
        "Intermediate" -> Color(0xFFFFD740)
        "Advanced" -> Color(0xFFFF6D00)
        else -> AccentTeal
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Topic + difficulty + clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = response.topic,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Difficulty chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(difficultyColor.copy(alpha = 0.15f))
                    .border(1.dp, difficultyColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(response.difficulty, color = difficultyColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }

        // English definition card
        GlassCard(
            icon = { Icon(Icons.Default.MenuBook, null, tint = AccentTeal, modifier = Modifier.size(16.dp)) },
            label = "Definition"
        ) {
            Text(response.definition, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
        }

        // Hindi definition card
        GlassCard(
            icon = { Text("🇮🇳", fontSize = 14.sp) },
            label = "हिंदी में"
        ) {
            Text(response.hindiDefinition, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
        }

        // Examples
        if (response.examples.isNotEmpty()) {
            GlassCard(
                icon = { Icon(Icons.Default.FormatListNumbered, null, tint = Color(0xFFFFD740), modifier = Modifier.size(16.dp)) },
                label = "Examples"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    response.examples.forEachIndexed { index, example ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2979FF).copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = example.sentence,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = example.explanation,
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
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
                icon = { Icon(Icons.Default.TipsAndUpdates, null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp)) },
                label = "Tips"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    response.tips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("✦", color = Color(0xFF00E676), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tip, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp)
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
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}
