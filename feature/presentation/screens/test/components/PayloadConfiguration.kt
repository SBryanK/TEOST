package com.example.teost.presentation.screens.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teost.data.model.EncodingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadConfiguration(
    modifier: Modifier = Modifier,
    payloadList: List<String>,
    onPayloadListChange: (List<String>) -> Unit,
    encodingMode: EncodingMode?,
    onEncodingModeChange: (EncodingMode) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Payload Injection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = payloadList.joinToString("\n"),
            onValueChange = { onPayloadListChange(it.split("\n")) },
            label = { Text("Payloads (one per line)") },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )

        var encodingExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = encodingExpanded,
            onExpandedChange = { encodingExpanded = !encodingExpanded }
        ) {
            OutlinedTextField(
                value = encodingMode?.name ?: "NONE",
                onValueChange = {},
                readOnly = true,
                label = { Text("Encoding Mode") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = encodingExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = encodingExpanded, onDismissRequest = { encodingExpanded = false }) {
                EncodingMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            onEncodingModeChange(mode)
                            encodingExpanded = false
                        }
                    )
                }
            }
        }
    }
}
