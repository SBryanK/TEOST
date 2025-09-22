package com.example.teost.presentation.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teost.core.ui.components.LoadingOverlay
import com.example.teost.core.ui.theme.EdgeOneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestExecutionScreen(
    onNavigateToSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TestExecutionViewModel = hiltViewModel()
    val flowVm: TestFlowViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val lastIds by flowVm.lastTestIds.collectAsState()
    LaunchedEffect(lastIds) { if (lastIds.isNotEmpty()) viewModel.startTracking(lastIds) }
    
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isCompleted by viewModel.isCompleted.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val timeoutReached by viewModel.timeoutReached.collectAsStateWithLifecycle()
    
    // Navigate to success screen when completed
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            onNavigateToSuccess()
        }
    }
    
    // Handle timeout
    LaunchedEffect(timeoutReached) {
        if (timeoutReached) {
            // Could show error dialog or navigate back
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = com.example.teost.core.ui.R.string.test_execution_title.let { androidx.compose.ui.res.stringResource(id = it) },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (errorMessage != null) {
                    // Error state
                    ErrorState(
                        errorMessage = errorMessage!!,
                        onRetry = { viewModel.resetState() }
                    )
                } else if (timeoutReached) {
                    // Timeout state
                    TimeoutState(
                        onRetry = { viewModel.resetState() }
                    )
                } else {
                    // Loading state (content will be hidden by overlay)
                    LoadingContent()
                }
            }
            
            // Loading overlay
            LoadingOverlay(
                isVisible = isLoading,
                progress = progress,
                message = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.preparing_security_tests)
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.preparing_security_tests),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.tests_begin_shortly),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.error_label),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.test_execution_failed),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRetry,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.retry))
        }
    }
}

@Composable
private fun TimeoutState(
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.timeout_label),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.test_execution_timeout),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.tests_timeout_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRetry,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.try_again))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TestExecutionScreenPreview() {
    EdgeOneTheme {
        TestExecutionScreen(
            onNavigateToSuccess = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultDetailsScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.results_header_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Placeholder details UI (static) – kept for previews and future wiring
            ResultCard(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.http_response_title)) {
                Text("Status: 200", fontWeight = FontWeight.SemiBold)
                Text("Request-ID: abc123")
                Text("Response Time: 123ms")
                Text("TTFB: 60ms, DNS: 20ms, TCP: 25ms, SSL: 18ms")
            }

            ResultCard(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.target_title)) {
                Text("Domain: example.com")
                Text("Type: DoS Spike Test")
            }

            ResultCard(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.headers)) {
                Text("Server: edgeone")
                Text("Cache-Control: no-cache")
            }

            ResultCard(title = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.raw_logs)) {
                Text("GET / -> 200 in 123ms")
                Text("metrics: dns=20,tcp=25,ssl=18,ttfb=60")
            }
        }
    }
}

@Composable
private fun ResultCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

