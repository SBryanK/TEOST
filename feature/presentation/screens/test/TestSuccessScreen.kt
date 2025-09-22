package com.example.teost.presentation.screens.test

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teost.core.ui.components.TestResultCard
import com.example.teost.core.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSuccessScreen(
    testIds: List<String>,
    onGoToHistory: () -> Unit,
    onBackToTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: TestSuccessViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    LaunchedEffect(testIds) { vm.load(testIds) }
    val results by vm.results.collectAsState(initial = emptyList())
    // Calculate credits used for THIS test session
    val creditsUsedThisSession = if (results.isNotEmpty()) {
        // Use actual credits from results, but ensure minimum of 1 per test
        val actualCredits = results.sumOf { maxOf(it.creditsUsed, 1) }
        android.util.Log.d("TestSuccessScreen", "Credits calculated from ${results.size} results: $actualCredits (ensuring min 1 per test)")
        actualCredits
    } else {
        // Fallback: 1 credit per test ID
        val estimatedCredits = testIds.size
        android.util.Log.d("TestSuccessScreen", "Credits estimated from ${testIds.size} testIds: $estimatedCredits")
        estimatedCredits
    }
    
    // Refresh credits when results are loaded
    val creditsVm: com.example.teost.presentation.screens.profile.CreditsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    LaunchedEffect(results.size) {
        if (results.isNotEmpty()) {
            android.util.Log.d("TestSuccessScreen", "Forcing credit refresh after ${results.size} results loaded")
            creditsVm.refresh()
            // Force re-observation of credits to get latest state
            kotlinx.coroutines.delay(1000) // Give time for credit consumption to complete
            creditsVm.refresh()
        }
    }
    
    // Also refresh credits when testIds change (immediate)
    LaunchedEffect(testIds) {
        if (testIds.isNotEmpty()) {
            android.util.Log.d("TestSuccessScreen", "Initial credit refresh for ${testIds.size} tests")
            creditsVm.refresh()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.test_result_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToTest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back_to_test)
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
        val ctx = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(results.size) {
            if (results.isNotEmpty()) {
                // Trigger sync and history refresh
                val intent = android.content.Intent("com.example.teost.ACTION_SYNC_NOW")
                intent.setPackage(ctx.packageName)
                try { ctx.sendBroadcast(intent) } catch (_: Exception) {}
                
                // Also trigger history refresh
                val historyIntent = android.content.Intent("com.example.teost.ACTION_REFRESH_HISTORY")
                historyIntent.setPackage(ctx.packageName)
                try { ctx.sendBroadcast(historyIntent) } catch (_: Exception) {}
            }
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
                    Text(text = "Test Completed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val creditsState by creditsVm.uiState.collectAsState()
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Credits Used", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(creditsUsedThisSession.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Credits Remaining", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text((creditsState.remaining ?: 0).toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
            
            // Test Results Section
            if (results.isNotEmpty()) {
                Text(
                    text = "Test Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { result ->
                        TestResultCard(
                            domain = result.domain,
                            testName = result.testName,
                            status = result.status.name,
                            duration = result.duration,
                            creditsUsed = maxOf(result.creditsUsed, 1), // Ensure minimum 1 credit display
                            statusCode = result.resultDetails.statusCode,
                            startTime = result.startTime,
                            dnsTime = result.resultDetails.dnsResolutionTime,
                            tcpTime = result.resultDetails.tcpHandshakeTime,
                            sslTime = result.resultDetails.sslHandshakeTime,
                            ttfb = result.resultDetails.ttfb,
                            responseTime = result.resultDetails.responseTime,
                            headers = result.resultDetails.headers,
                            encryptedBody = result.resultDetails.encryptedBody,
                            encryptionAlgorithm = result.resultDetails.encryptionAlgorithm,
                            networkLogs = result.rawLogs?.split("\n")?.filter { it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            
            // Action buttons
            ActionButtons(
                onGoToHistory = onGoToHistory,
                onBackToTest = onBackToTest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

data class MockTestResult(
    val domain: String,
    val testName: String,
    val status: String,
    val duration: Long,
    val creditsUsed: Int,
    val statusCode: Int,
    val startTime: Date
)

@Composable
private fun SuccessHeader(
    successCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(600))
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Success icon with animation
                SuccessIcon()
                
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.tests_completed),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.tests_successful, successCount, totalCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SuccessIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF4CAF50))
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun ResultsSummary(
    testResults: List<MockTestResult>,
    modifier: Modifier = Modifier
) {
    val successCount = testResults.count { it.status == "SUCCESS" }
    val failedCount = testResults.count { it.status == "FAILED" }
    val totalDuration = testResults.sumOf { it.duration }
    val totalCredits = testResults.sumOf { it.creditsUsed }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryChip(
                    icon = Icons.Filled.CheckCircle,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.success),
                    value = successCount.toString(),
                    color = Color(0xFF4CAF50)
                )
                
                SummaryChip(
                    icon = Icons.Filled.Error,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.failed),
                    value = failedCount.toString(),
                    color = Color(0xFFF44336)
                )
                
                SummaryChip(
                    icon = Icons.Filled.Schedule,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.duration),
                    value = "${totalDuration}ms",
                    color = TencentBlue
                )
                
                SummaryChip(
                    icon = Icons.Filled.Star,
                    label = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.credits),
                    value = totalCredits.toString(),
                    color = StatusOrange
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionButtons(
    onGoToHistory: () -> Unit,
    onBackToTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBackToTest,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back_to_test))
        }
        
        Button(
            onClick = onGoToHistory,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TencentBlue
            )
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.go_to_history))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TestSuccessScreenPreview() {
    EdgeOneTheme {
        TestSuccessScreen(
            testIds = emptyList(),
            onGoToHistory = {},
            onBackToTest = {}
        )
    }
}