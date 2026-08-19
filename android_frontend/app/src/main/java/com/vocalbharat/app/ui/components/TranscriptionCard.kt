package com.vocalbharat.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocalbharat.app.ui.theme.*

@Composable
fun TranscriptionCard(
    text: String,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    text = "आपने क्या कहा (Hindi Input)",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    color = PrimaryLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                color = TextPrimary
            )
        }
    }
}
