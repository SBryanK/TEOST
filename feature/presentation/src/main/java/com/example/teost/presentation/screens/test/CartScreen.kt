package com.example.teost.presentation.screens.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.GsonBuilder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.data.model.displayTestName
import com.example.teost.data.model.summary
import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestType
import com.example.teost.presentation.screens.test.TestCartStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateToConfirmation: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToConfiguration: (category: String, type: String, target: String) -> Unit = { _, _, _ -> }
) {
    val items by TestCartStore.items.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.cart_title)) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)) } },
            actions = {
                // Removed bulk import for now - use individual test import
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.cart_empty))
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items.size) { idx ->
                        val item = items[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    // Navigate to configuration page for editing
                                    val category = inferCategoryString(item.parameters)
                                    val type = inferTypeString(item.parameters)
                                    onNavigateToConfiguration(category, type, item.domain)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val name = item.parameters.displayTestName()
                                        Text(
                                            text = name, 
                                            style = MaterialTheme.typography.titleSmall, 
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.domain, 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val summary = item.parameters.summary()
                                        if (summary.isNotBlank()) {
                                            Text(
                                                text = summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.one_credit),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { 
                                                val category = inferCategoryString(item.parameters)
                                                val type = inferTypeString(item.parameters)
                                                onNavigateToConfiguration(category, type, item.domain)
                                            }
                                        ) { 
                                            Icon(
                                                Icons.Filled.Edit, 
                                                contentDescription = "Edit configuration",
                                                tint = MaterialTheme.colorScheme.primary
                                            ) 
                                        }
                                        IconButton(onClick = { TestCartStore.removeAt(idx) }) { 
                                            Icon(
                                                Icons.Filled.Delete, 
                                                contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.remove),
                                                tint = MaterialTheme.colorScheme.error
                                            ) 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Button(onClick = onNavigateToConfirmation, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.proceed_confirmation)) }
            }
        }
    }
}

// IMPROVED: Store category and type in TestConfiguration for accurate edit navigation
private fun inferCategoryString(parameters: com.example.teost.data.model.TestParameters): String {
    // Use testTypeHint if available (should be stored during configuration)
    parameters.testTypeHint?.let { hint ->
        when {
            hint.contains("HttpSpike") || hint.contains("ConnectionFlood") || 
            hint.contains("TcpPort") || hint.contains("UdpReach") || hint.contains("BasicConnectivity") -> return "ddos_protection"
            
            hint.contains("SqlInjection") || hint.contains("XssTest") || hint.contains("PathTraversal") || 
            hint.contains("CommandInjection") || hint.contains("CustomRules") || hint.contains("LongQuery") || 
            hint.contains("OversizedBody") -> return "web_protection"
            
            hint.contains("UserAgent") || hint.contains("CookieJs") || hint.contains("WebCrawler") -> return "bot_management"
            
            hint.contains("Authentication") || hint.contains("BruteForce") || hint.contains("Enumeration") || 
            hint.contains("Schema") || hint.contains("BusinessLogic") -> return "api_protection"
        }
    }
    
    // Fallback to parameter-based inference
    return when {
        // DoS Protection - more specific detection
        parameters.burstRequests != null || parameters.concurrentConnections != null || 
        parameters.rpsTarget != null -> "ddos_protection"
        
        // WAF Testing - improved detection
        parameters.payloadList?.isNotEmpty() == true || parameters.encodingMode != null ||
        parameters.injectionPoint != null || parameters.paramLength != null || 
        parameters.bodySizeKb != null -> "web_protection"
        
        // Bot Management - better detection
        parameters.uaProfiles?.isNotEmpty() == true || parameters.crawlDepth != null ||
        parameters.respectRobotsTxt != null || parameters.cookiePolicy != null -> "bot_management"
        
        // API Protection - enhanced detection
        parameters.username != null || parameters.authMode != null || parameters.enumTemplate != null ||
        parameters.fuzzCases?.isNotEmpty() == true || parameters.replayCount != null ||
        parameters.authToken != null -> "api_protection"
        
        else -> "ddos_protection"
    }
}

private fun inferTypeString(parameters: com.example.teost.data.model.TestParameters): String {
    // Use testTypeHint if available for precise type detection
    parameters.testTypeHint?.let { hint ->
        when {
            hint.contains("HttpSpike") -> return "httpspike"
            hint.contains("ConnectionFlood") -> return "connectionflood" 
            hint.contains("TcpPortReachability") -> return "tcpportreachability"
            hint.contains("UdpReachability") -> return "udpreachability"
            hint.contains("BasicConnectivity") -> return "basicconnectivity"
            
            hint.contains("SqlInjection") -> return "sqlinjection"
            hint.contains("XssTest") -> return "xsstest"
            hint.contains("ReflectedXss") -> return "reflectedxss"
            hint.contains("PathTraversal") -> return "pathtraversal"
            hint.contains("CommandInjection") -> return "commandinjection"
            hint.contains("CustomRulesValidation") -> return "customrulesvalidation"
            hint.contains("EdgeRateLimiting") -> return "edgeratelimiting"
            hint.contains("LongQuery") -> return "longquery"
            hint.contains("OversizedBody") -> return "oversizedbody"
            
            hint.contains("UserAgentAnomaly") -> return "useragentanomaly"
            hint.contains("CookieJsChallenge") -> return "cookiejschallenge"
            hint.contains("WebCrawlerSimulation") -> return "webcrawlersimulation"
            
            hint.contains("AuthenticationTest") -> return "authenticationtest"
            hint.contains("BruteForce") -> return "bruteforce"
            hint.contains("EnumerationIdor") -> return "enumerationidor"
            hint.contains("SchemaInputValidation") -> return "schemainputvalidation"
            hint.contains("BusinessLogicAbuse") -> return "businesslogicabuse"
        }
    }
    
    // Fallback to parameter-based inference with improved logic
    return when {
        // DoS Types - more precise detection
        parameters.burstRequests != null && parameters.burstIntervalMs != null -> "httpspike"
        parameters.concurrentConnections != null && parameters.durationSec != null -> "connectionflood"
        parameters.timeoutMs != null -> "tcpportreachability"
        parameters.payloadList?.isNotEmpty() == true -> "udpreachability"
        parameters.rpsTarget != null && parameters.windowSec != null -> "edgeratelimiting"
        
        // WAF Types - enhanced detection
        parameters.payloadList?.isNotEmpty() == true -> {
            when (parameters.injectionPoint) {
                com.example.teost.data.model.InjectionPoint.PATH_PARAM -> "pathtraversal"
                com.example.teost.data.model.InjectionPoint.QUERY_PARAM -> "sqlinjection" 
                com.example.teost.data.model.InjectionPoint.HEADER -> "xsstest"
                com.example.teost.data.model.InjectionPoint.BODY -> "commandinjection"
                else -> "sqlinjection"
            }
        }
        parameters.encodingMode != null -> "reflectedxss"
        parameters.headersOverrides?.isNotEmpty() == true -> "customrulesvalidation"
        parameters.paramLength != null -> "longquery"
        parameters.bodySizeKb != null -> "oversizedbody"
        
        // Bot Types - better detection
        parameters.uaProfiles?.isNotEmpty() == true -> "useragentanomaly"
        parameters.cookiePolicy != null -> "cookiejschallenge"
        parameters.crawlDepth != null -> "webcrawlersimulation"
        
        // API Types - improved detection
        parameters.username != null && parameters.passwordList?.isNotEmpty() == true -> "bruteforce"
        parameters.enumTemplate != null -> "enumerationidor"
        parameters.fuzzCases?.isNotEmpty() == true -> "schemainputvalidation"
        parameters.replayCount != null -> "businesslogicabuse"
        parameters.authMode != null || parameters.authToken != null -> "authenticationtest"
        
        else -> "basicconnectivity"
    }
}

@Preview(showBackground = true)
@Composable
fun CartScreenPreview() { EdgeOneTheme { CartScreen() } }
