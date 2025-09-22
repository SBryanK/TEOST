package com.example.teost.presentation.screens.test

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.presentation.screens.test.TestCartStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationScreen(
    onStartTest: (List<String>) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onCompleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val creditsVm: com.example.teost.presentation.screens.profile.CreditsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val creditsState by creditsVm.uiState.collectAsState()
    val items by TestCartStore.items.collectAsState()
    var isOverlayVisible by remember { mutableStateOf(false) }
    var hasWarned by remember { mutableStateOf(false) }
    
    // Store the count locally to prevent showing 0 after cart is cleared
    val testCount = remember { items.size }
    var completedCount by remember { mutableStateOf(0) }
    
    // Fallback timeout to prevent infinite loading
    LaunchedEffect(isOverlayVisible) {
        if (isOverlayVisible) {
            android.util.Log.d("ConfirmationScreen", "Loading overlay shown, starting 30s timeout")
            kotlinx.coroutines.delay(30000) // 30 seconds timeout
            if (isOverlayVisible) {
                android.util.Log.w("ConfirmationScreen", "Loading timeout reached, dismissing overlay")
                isOverlayVisible = false
                TestCartStore.clear()
                onCompleted()
            }
        }
    }
    
    // Register broadcast receiver early to avoid race condition
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == "com.example.teost.ACTION_TEST_COMPLETED") {
                    completedCount++
                    android.util.Log.d("ConfirmationScreen", "Received test completion broadcast ($completedCount/$testCount)")
                    
                    // Only complete when all tests are done
                    if (completedCount >= testCount) {
                        android.util.Log.d("ConfirmationScreen", "All tests completed, clearing cart and navigating")
                        TestCartStore.clear()
                        isOverlayVisible = false
                        onCompleted()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.teost.ACTION_TEST_COMPLETED")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    val remaining = creditsState.remaining ?: 0

    LaunchedEffect(remaining, items.size) {
        if (remaining < items.size && !hasWarned) {
            hasWarned = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.confirmation)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content - centered vertically and horizontally
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "Ready to Start" - Large heading
                Text(
                    text = "Ready to Start",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Test count - Large number with proper pluralization
                val testText = if (testCount == 1) "Test" else "Tests"
                Text(
                    text = "$testCount $testText",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Start Test button - Large and prominent
                Button(
                    onClick = {
                        android.util.Log.d("ConfirmationScreen", "Start Tests clicked with ${items.size} items")
                        // Enqueue actual configurations via TestQueueReceiver
                        val queue = java.util.ArrayList<com.example.teost.data.model.TestConfiguration>(items)
                        val intent = Intent("com.example.teost.ACTION_EXECUTE_TEST_QUEUE").apply {
                            putParcelableArrayListExtra("extra_queue", queue)
                            setPackage(context.packageName)
                        }
                        android.util.Log.d("ConfirmationScreen", "Sending broadcast to start ${queue.size} tests")
                        context.sendBroadcast(intent)
                        isOverlayVisible = true
                        android.util.Log.d("ConfirmationScreen", "Loading overlay set to visible")
                        val ids = items.map { it.testId }
                        onStartTest(ids)
                    },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth(0.8f) // 80% width for better proportion
                        .height(64.dp), // Larger height
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Start Test",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isOverlayVisible) {
                Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                Text(
                                text = "Starting tests...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfirmationScreenPreview() { EdgeOneTheme { ConfirmationScreen() } }
