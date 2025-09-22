package com.example.teost.presentation.screens.test.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SliderConfig(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int = 1,
    unitSuffix: String? = null,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    LaunchedEffect(value) {
        if (text != value.toString()) text = value.toString()
    }

    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val sliderSteps = if (step <= 0) 0 else ((valueRange.last - valueRange.first) / step) - 1
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = (raw.roundToInt() / step) * step
                val bounded = snapped.coerceIn(valueRange.first, valueRange.last)
                text = bounded.toString()
                onValueChange(bounded)
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = sliderSteps.coerceAtLeast(0),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = if (unitSuffix != null) "$text" else text,
            onValueChange = { s ->
                val digits = s.filter { it.isDigit() }
                val parsed = digits.toIntOrNull()
                if (parsed != null) {
                    val bounded = parsed.coerceIn(valueRange.first, valueRange.last)
                    text = bounded.toString()
                    onValueChange(bounded)
                } else {
                    text = digits
                }
            },
            singleLine = true,
            label = { Text(unitSuffix ?: "Value") },
            modifier = Modifier.width(120.dp)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    val suffix = unitSuffix ?: ""
    Text(
        text = "${value}${if (suffix.isNotBlank()) " $suffix" else ""}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}


