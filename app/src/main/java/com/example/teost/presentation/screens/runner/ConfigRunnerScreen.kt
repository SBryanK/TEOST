package com.example.teost.presentation.screens.runner

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ConfigRunnerScreen(
    viewModel: ConfigRunnerViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val ui by viewModel.uiState.collectAsState()

    var addTargetField by remember { mutableStateOf(TextFieldValue("")) }
    var errorSnack by remember { mutableStateOf("") }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importPlan(ctx.contentResolver, it) { errorSnack = it } }
    }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { viewModel.exportPlan(ctx.contentResolver, it) { errorSnack = it } }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Targets
        Text("Targets (selected from Search screen or add manually):", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = addTargetField,
                onValueChange = { addTargetField = it },
                modifier = Modifier.weight(1f),
                label = { Text("https://domain or domain.com") }
            )
            Button(onClick = {
                viewModel.addTarget(addTargetField.text)
                addTargetField = TextFieldValue("")
            }) { Text("Add") }
        }

        if (ui.selectedTargets.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                items(ui.selectedTargets) { t ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t)
                        TextButton(onClick = { viewModel.removeTarget(t) }) { Text("Remove") }
                    }
                }
            }
        }

        // VPN
        Text("VPN Link (optional)", style = MaterialTheme.typography.titleMedium)
        var vpnField by remember { mutableStateOf(TextFieldValue(ui.vpnLink)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vpnField,
                onValueChange = { vpnField = it; viewModel.setVpnLink(it.text) },
                modifier = Modifier.weight(1f),
                label = { Text("https://your-vpn-link") }
            )
            Button(onClick = {
                if (vpnField.text.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vpnField.text))
                    ctx.startActivity(intent)
                }
            }) { Text("Use VPN") }
        }

        // Plan actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { openDoc.launch(arrayOf("application/json")) }) { Text("Import JSON") }
            Button(onClick = {
                ui.plan?.let {
                    createDoc.launch("test_plan.json")
                } ?: run { errorSnack = "No plan to export" }
            }) { Text("Export JSON") }
        }

        // Per-test domain mapping
        ui.plan?.let { plan ->
            Text("Per-test domain mapping (for multiple targets):", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                items(plan.tests.size) { idx ->
                    val spec = plan.tests[idx]
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("#${idx + 1} ${spec.category} / ${spec.type}")
                        if (ui.selectedTargets.size > 1) {
                            // Simple checklist: toggles for each target
                            ui.selectedTargets.forEach { domain ->
                                val selected = ui.testDomainSelections[idx]?.contains(domain) == true
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(domain)
                                    Switch(checked = selected, onCheckedChange = { checked ->
                                        val current = ui.testDomainSelections[idx]?.toMutableList() ?: mutableListOf()
                                        if (checked) current.add(domain) else current.remove(domain)
                                        viewModel.setTestDomainSelection(idx, current.distinct())
                                    })
                                }
                            }
                        } else {
                            Text("Single target mode: the single selected target will be used.")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        // Cart & Run & Logs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val configs = viewModel.generateCartConfigurations { errorSnack = it }
                if (configs.isNotEmpty()) {
                    configs.forEach { com.example.teost.presentation.screens.test.TestCartStore.add(it) }
                }
            }) { Text("Add to Cart") }
            Button(onClick = { viewModel.runTests { errorSnack = it } }, enabled = !ui.isRunning) { Text(if (ui.isRunning) "Running..." else "Run") }
            Button(onClick = { viewModel.persistLogsPerDomain() }) { Text("Save Logs") }
        }

        if (ui.lastMessage.isNotBlank()) Text(ui.lastMessage)

        // Show logs per domain
        LazyColumn(Modifier.fillMaxSize()) {
            ui.domainLogs.forEach { (domain, logs) ->
                item { Text("Logs for $domain", style = MaterialTheme.typography.titleMedium) }
                items(logs) { line -> Text(line) }
                item { HorizontalDivider(thickness = 2.dp) }
            }
        }
    }

    if (errorSnack.isNotBlank()) {
        SnackbarHost(hostState = remember { SnackbarHostState() })
        // In a real app, you'd show a Snackbar; here we just reset for brevity
        LaunchedEffect(errorSnack) { errorSnack = "" }
    }
}


