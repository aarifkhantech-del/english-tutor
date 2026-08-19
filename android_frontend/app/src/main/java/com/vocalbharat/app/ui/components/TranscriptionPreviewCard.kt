package com.vocalbharat.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.ui.theme.*

@Composable
fun TranscriptionPreviewCard(
    initialText: String,
    isTranslating: Boolean,
    onProceed: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedText by remember(initialText) { mutableStateOf(initialText) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.5.dp, PrimaryLight.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = PrimaryLight,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "आपकी आवाज़ (Speech Transcribed)",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    color = PrimaryLight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Editable Hindi Speech Text Field
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = { Text("Hindi Speech Text") },
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryLight,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedLabelColor = PrimaryLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                    unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Proceed (Translate) vs Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isTranslating,
                    border = BorderStroke(1.dp, RecordingRed),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RecordingRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel", fontSize = 14.sp)
                }

                // Proceed Button
                Button(
                    onClick = { onProceed(editedText) },
                    enabled = !isTranslating && editedText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = SurfaceBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translating...", fontSize = 14.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Proceed",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Proceed", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
