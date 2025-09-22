package com.example.teost.presentation.screens.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teost.data.model.HttpMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpRequestEditor(
    modifier: Modifier = Modifier,
    httpMethod: HttpMethod,
    onMethodChange: (HttpMethod) -> Unit,
    requestPath: String,
    onPathChange: (String) -> Unit,
    queryParams: Map<String, String>,
    onQueryParamsChange: (Map<String, String>) -> Unit,
    headers: Map<String, String>,
    onHeadersChange: (Map<String, String>) -> Unit,
    bodyTemplate: String?,
    onBodyChange: (String) -> Unit,
    // Conditional display options
    showMethod: Boolean = true,
    showPath: Boolean = true,
    showQueryParams: Boolean = true,
    showHeaders: Boolean = true,
    showBody: Boolean = true,
    title: String = "HTTP Request Configuration"
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Section Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Method and Path (conditional)
        if (showMethod || showPath) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showMethod) {
                    var methodExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = !methodExpanded },
                        modifier = Modifier.weight(if (showPath) 0.4f else 1f)
                    ) {
                        OutlinedTextField(
                            value = httpMethod.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("HTTP Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                            HttpMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        onMethodChange(method)
                                        methodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (showPath) {
                    OutlinedTextField(
                        value = requestPath,
                        onValueChange = onPathChange,
                        label = { Text("Request Path") },
                        placeholder = { Text("/api/endpoint") },
                        singleLine = true,
                        modifier = Modifier.weight(if (showMethod) 0.6f else 1f)
                    )
                }
            }
        }

        // Query Parameters (conditional)
        if (showQueryParams) {
            KeyValueEditor(
                title = "Query Parameters",
                items = queryParams,
                onItemsChange = onQueryParamsChange,
            
            )
        }

        // Headers (conditional)
        if (showHeaders) {
            KeyValueEditor(
                title = "HTTP Headers",
                items = headers,
                onItemsChange = onHeadersChange,
                suggestions = listOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "User-Agent" to "Custom-Bot/1.0",
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer {{TOKEN}}",
                    "X-Forwarded-For" to "192.168.1.1",
                    "Referer" to "https://example.com"
                )
            )    
        
        }
        
        // Body (conditional)
        if (showBody && bodyTemplate != null) {
            OutlinedTextField(
                value = bodyTemplate,
                onValueChange = onBodyChange,
                label = { Text("Request Body") },
                placeholder = { Text("{\"username\": \"{{PAYLOAD}}\", \"password\": \"test\"}") },
                supportingText = { Text("Use {{PAYLOAD}} as injection placeholder") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyValueEditor(
    title: String,
    items: Map<String, String>,
    onItemsChange: (Map<String, String>) -> Unit,
    suggestions: List<Pair<String, String>> = emptyList(),
    helpText: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            var sugExpanded by remember { mutableStateOf(false) }
            if (suggestions.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = sugExpanded, onExpandedChange = { sugExpanded = !sugExpanded }) {
                    IconButton(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true), 
                        onClick = { sugExpanded = !sugExpanded }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add header")
                    }
                    ExposedDropdownMenu(expanded = sugExpanded, onDismissRequest = { sugExpanded = false }) {
                        suggestions.forEach { (k, v) ->
                            DropdownMenuItem(text = { Text(k) }, onClick = {
                                onItemsChange(items + (k to v))
                                sugExpanded = false
                            })
                        }
                    }
                }
            } else {
                IconButton(onClick = {
                    onItemsChange(items + ("key${items.size + 1}" to "value"))
                }) { Icon(Icons.Default.Add, contentDescription = "Add Item") }
            }
        }
        
        // Help text
        helpText?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items.forEach { (key, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { newKey ->
                        val updated = items.toMutableMap()
                        updated.remove(key)
                        updated[newKey] = value
                        onItemsChange(updated)
                    },
                    label = { Text("Key") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onItemsChange(items + (key to newValue))
                    },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    onItemsChange(items.toMutableMap().apply { remove(key) })
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                }
            }
        }
    }
}
