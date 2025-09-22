package com.example.teost.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.teost.core.ui.theme.*

@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    progress: Float,
    message: String? = null,
    modifier: Modifier = Modifier,
    modal: Boolean = true
) {
    if (isVisible) {
        if (modal) {
            Dialog(
                onDismissRequest = { /* Prevent dismiss during loading */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                OverlayCard(modifier, progress, message)
            }
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                OverlayCard(Modifier, progress, message)
            }
        }
    }
}

@Composable
private fun OverlayCard(modifier: Modifier, progress: Float, message: String?) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(0.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loading icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = TencentBlue,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }
                Text(
                    text = message ?: androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.preparing_security_tests),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = TencentBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}