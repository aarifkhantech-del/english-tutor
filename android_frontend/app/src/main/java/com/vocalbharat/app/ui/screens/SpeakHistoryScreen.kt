package com.vocalbharat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.data.local.toCorrection
import com.vocalbharat.app.data.model.SpeakHistoryEntry
import com.vocalbharat.app.ui.components.CorrectionCard
import com.vocalbharat.app.ui.components.PracticeCard
import com.vocalbharat.app.ui.components.ScreenHeader
import com.vocalbharat.app.ui.components.TranscriptionCard
import com.vocalbharat.app.ui.theme.AppAccent
import com.vocalbharat.app.ui.theme.AppAccentEnd
import com.vocalbharat.app.ui.theme.LightBackground
import com.vocalbharat.app.ui.theme.SurfaceBorder
import com.vocalbharat.app.ui.theme.SurfaceDark
import com.vocalbharat.app.ui.theme.TextMuted
import com.vocalbharat.app.ui.theme.TextPrimary
import com.vocalbharat.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeakHistoryScreen(
    entries: List<SpeakHistoryEntry>,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear speak history?", fontWeight = FontWeight.Bold) },
            text = { Text("This removes all Hindi to English practice saved on this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("Clear all", color = AppAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        ScreenHeader(
            title = "Speak History",
            subtitle = "Saved on this phone only — not in the cloud",
            gradientColors = listOf(AppAccent, AppAccentEnd),
            extraContent = {
                if (entries.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.DeleteSweep, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear all", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = TextMuted, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No practice saved yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Speak in Hindi on the home screen. Each translation is stored locally on your phone.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    SpeakHistoryItem(
                        entry = entry,
                        expanded = expandedId == entry.id,
                        onToggle = { expandedId = if (expandedId == entry.id) null else entry.id },
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpeakHistoryItem(
    entry: SpeakHistoryEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = remember(entry.createdAt) {
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(entry.createdAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateLabel, color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.hindi.ifBlank { "(no Hindi text)" },
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (expanded) 8 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.english.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.english,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = if (expanded) 8 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entry.hindi.isNotBlank()) TranscriptionCard(text = entry.hindi)
                CorrectionCard(correction = entry.toCorrection(), isPlayingAudio = false, onPlayAudio = null)
                PracticeCard(correction = entry.toCorrection())
            }
        }
    }
}
