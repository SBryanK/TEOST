package com.example.teost.presentation.screens.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teost.data.model.TestParameters

@Composable
fun EnumerationIdorConfig(
    params: TestParameters,
    onParamsChange: (TestParameters) -> Unit
) {
    var idStart by remember { mutableStateOf(params.idRange?.getOrNull(0)?.toString() ?: "1") }
    var idEnd by remember { mutableStateOf(params.idRange?.getOrNull(1)?.toString() ?: "10") } // Safer default
    var step by remember { mutableStateOf(params.stepSize?.toString() ?: "1") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = idStart,
                onValueChange = {
                    idStart = it
                    onParamsChange(params.copy(idRange = listOf(it.toLongOrNull() ?: 1L, idEnd.toLongOrNull() ?: 10L)))
                },
                label = { Text("Start ID") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = idEnd,
                onValueChange = {
                    idEnd = it
                    onParamsChange(params.copy(idRange = listOf(idStart.toLongOrNull() ?: 1L, it.toLongOrNull() ?: 10L)))
                },
                label = { Text("End ID") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = step,
            onValueChange = {
                step = it
                onParamsChange(params.copy(stepSize = it.toIntOrNull() ?: 1))
            },
            label = { Text("Step") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CrawlerConfig(
    params: TestParameters,
    onParamsChange: (TestParameters) -> Unit
) {
    var crawlDepth by remember { mutableStateOf(params.crawlDepth?.toString() ?: "3") } // Median of 1-5
    var respectRobots by remember { mutableStateOf(params.respectRobotsTxt ?: true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SliderConfig(
            label = "Crawl Depth",
            value = params.crawlDepth ?: 3, // Median of 1-5
            onValueChange = { v -> onParamsChange(params.copy(crawlDepth = v)) },
            valueRange = 1..5, // Safe range for web crawling
            step = 1,
            unitSuffix = "levels"
        )
        Column {
            Text(
                text = "Respect Robots.txt",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = respectRobots,
                    onClick = { 
                        respectRobots = true
                        onParamsChange(params.copy(respectRobotsTxt = true))
                    },
                    label = { Text("Yes") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !respectRobots,
                    onClick = { 
                        respectRobots = false
                        onParamsChange(params.copy(respectRobotsTxt = false))
                    },
                    label = { Text("No") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BruteForceConfig(
    params: TestParameters,
    onParamsChange: (TestParameters) -> Unit
) {
    var username by remember { mutableStateOf(params.username ?: "admin") }

    OutlinedTextField(
        value = username,
        onValueChange = {
            username = it
            onParamsChange(params.copy(username = it))
        },
        label = { Text("Username") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun OversizedBodyConfig(
    params: TestParameters,
    onParamsChange: (TestParameters) -> Unit
) {
    // This test type is now implicitly handled by the HttpRequestEditor's body field
    // and the SecurityTestEngine's logic. We can show a hint here.
    Text(
        "Configure the oversized body via the 'Request Body' section. The test engine will automatically generate a large body based on its content length.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
