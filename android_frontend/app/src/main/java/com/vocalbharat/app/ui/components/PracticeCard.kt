package com.vocalbharat.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.data.model.CorrectionResult
import com.vocalbharat.app.ui.theme.*

@Composable
fun PracticeCard(
    correction: CorrectionResult,
    modifier: Modifier = Modifier
) {
    if (correction.practice.isBlank() && correction.encouragement.isBlank()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Practice Sentence Card
        if (correction.practice.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Try Speaking This Sentence",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 13.sp,
                            color = AccentTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = correction.practice,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Encouragement Card
        if (correction.encouragement.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = PrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = correction.encouragement,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = PrimaryLight
                    )
                }
            }
        }
    }
}
