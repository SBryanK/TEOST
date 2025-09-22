package com.example.teost.presentation.screens.test

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.data.model.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    onNavigateToCategory: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val flowVm: TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    var lastImported by remember { mutableStateOf<Config?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        val cfg = com.example.teost.data.util.ConfigIO.importConfig(context, uri)
        if (cfg != null) {
            lastImported = cfg
            snackbarMessage = "Configuration imported successfully"
            } else {
            snackbarMessage = "Failed to import configuration"
            }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snack.showSnackbar(message)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
                .padding(padding)
            .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Centered Header
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.security_tests),
                style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Import Config Button
            TextButton(
                onClick = { openDoc.launch(arrayOf("application/json")) }
            ) {
                Icon(Icons.Filled.FileOpen, contentDescription = "Import")
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.import_config))
            }
            
            // Test Categories Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            items(
                listOf(
                    "DDOS_PROTECTION" to "DoS Protection",
                    "WEB_PROTECTION" to "Web Protection", 
                    "BOT_MANAGEMENT" to "Bot Management",
                    "API_PROTECTION" to "API Protection"
                )
            ) { (category, displayName) ->
                Card(
                    onClick = { onNavigateToCategory(category) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                when (category) {
                                    "DDOS_PROTECTION" -> Icons.Filled.Speed
                                    "WEB_PROTECTION" -> Icons.Filled.Shield
                                    "BOT_MANAGEMENT" -> Icons.Filled.BugReport
                                    else -> Icons.Filled.Security
                                },
                                contentDescription = displayName,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Preview(showBackground = true)
@Composable
fun TestScreenPreview() { EdgeOneTheme { TestScreen() } }
