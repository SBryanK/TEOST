package com.example.teost.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Centralized tokens for colors applied to common components.
 */
object Tokens

@Composable
fun PrimaryCtaColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
fun OutlinedBrandBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)


