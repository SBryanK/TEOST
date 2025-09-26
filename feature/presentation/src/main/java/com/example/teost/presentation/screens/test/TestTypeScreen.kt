package com.example.teost.presentation.screens.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teost.core.ui.theme.EdgeOneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestTypeScreen(
    category: String,
    target: String? = null,
    onNavigateToConfigure: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var resolvedIPs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isResolvingIP by remember { mutableStateOf(false) }
    
    // Auto-resolve IP when target is provided
    LaunchedEffect(target) {
        if (!target.isNullOrBlank() && target.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
            isResolvingIP = true
            scope.launch {
                try {
                    val ips = withContext(Dispatchers.IO) {
                        InetAddress.getAllByName(target).map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
                    }
                    resolvedIPs = ips
                } catch (e: Exception) {
                    resolvedIPs = emptyList()
                } finally {
                    isResolvingIP = false
                }
            }
        }
    }
    val testTypes = when (category) {
        "DDOS_PROTECTION" -> listOf("HttpSpike", "ConnectionFlood")
        "WEB_PROTECTION" -> listOf("SqlInjection", "XssTest", "PathTraversal", "OversizedBody")
        "BOT_MANAGEMENT" -> listOf("UserAgentAnomaly", "WebCrawlerSimulation")
        "API_PROTECTION" -> listOf("BruteForce", "EnumerationIdor", "SchemaInputValidation", "BusinessLogicAbuse")
        else -> emptyList()
    }
    
    // Get the icon for this category
    val categoryIcon = when (category) {
        "DDOS_PROTECTION" -> Icons.Filled.Shield
        "WEB_PROTECTION" -> Icons.Filled.Security
        "BOT_MANAGEMENT" -> Icons.Filled.BugReport
        "API_PROTECTION" -> Icons.Filled.Speed
        else -> Icons.Filled.PlayArrow
    }

    Scaffold(
        topBar = {
        TopAppBar(
                title = { Text("Select Test Type") },
            navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back))
                    }
            }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Target IP Resolution Info (if target provided)
            if (!target.isNullOrBlank()) {
                item {
                    TargetIPResolutionCard(
                        target = target,
                        resolvedIPs = resolvedIPs,
                        isResolving = isResolvingIP
                    )
                }
            }
            items(testTypes) { testType ->
                Card(
                    onClick = { onNavigateToConfigure(testType) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            categoryIcon,
                            contentDescription = testType,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = getTestTypeDisplayName(testType),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        // Chevron removed for cleaner card design
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetIPResolutionCard(
    target: String,
    resolvedIPs: List<String>,
    isResolving: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Target: $target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            when {
                isResolving -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Resolving IP addresses...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                resolvedIPs.isEmpty() -> {
                    Text(
                        text = "Unable to resolve IP addresses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                resolvedIPs.size == 1 -> {
                    Text(
                        text = "Resolved to: ${resolvedIPs.first()} (Single IP → Auto-prefill)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = "Multiple IPs resolved: ${resolvedIPs.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "→ IP list saved for configuration selection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getTestTypeDisplayName(testType: String): String {
    return when (testType) {
        // DoS Protection Tests
        "HttpSpike" -> "HTTP Spike Test"
        "ConnectionFlood" -> "Connection Flood Test"
        "EdgeRateLimiting" -> "Rate Limiting Test"
        "TcpPortReachability" -> "TCP Port Test"
        "UdpReachability" -> "UDP Port Test"
        "BasicConnectivity" -> "Basic Connectivity Test"
        "IpRegionBlocking" -> "IP Region Blocking Test"
        
        // WAF Testing
        "SqlInjection" -> "SQL Injection Test"
        "XssTest" -> "XSS Test"
        "PathTraversal" -> "Path Traversal Test"
        "CommandInjection" -> "Command Injection Test"
        "ReflectedXss" -> "Reflected XSS Test"
        "Log4ShellProbe" -> "Log4Shell Probe Test"
        "LongQuery" -> "Long Query Test"
        "OversizedBody" -> "Oversized Body Test"
        
        // API Protection Tests
        "BruteForce" -> "Brute Force Test"
        "EnumerationIdor" -> "IDOR Enumeration Test"
        "AuthenticationTest" -> "Authentication Test"
        "SchemaInputValidation" -> "Schema Validation Test"
        "BusinessLogicAbuse" -> "Business Logic Test"
        
        // Bot Management Tests
        "UserAgentAnomaly" -> "User-Agent Anomaly Test"
        "WebCrawlerSimulation" -> "Web Crawler Test"
        
        // Default fallback
        else -> "$testType Test"
    }
}

@Preview(showBackground = true)
@Composable
fun TestTypeScreenPreview() { EdgeOneTheme { TestTypeScreen(category = "DDOS_PROTECTION", target = "example.com") } }
