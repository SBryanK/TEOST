package com.example.teost.presentation.screens.test

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.teost.core.ui.theme.EdgeOneTheme
import com.example.teost.data.model.HttpMethod
import com.example.teost.data.model.TestParameters
import com.example.teost.data.model.EncodingMode
import com.example.teost.data.model.InjectionPoint
import com.example.teost.presentation.screens.test.TestCartStore
import com.example.teost.presentation.screens.test.components.BruteForceConfig
import com.example.teost.presentation.screens.test.components.CrawlerConfig
import com.example.teost.presentation.screens.test.components.EnumerationIdorConfig
import com.example.teost.presentation.screens.test.components.HttpRequestEditor
import com.example.teost.presentation.screens.test.components.OversizedBodyConfig
// import com.example.teost.presentation.screens.test.components.PayloadConfiguration // Component not found
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.UUID

private fun normalizeDomain(input: String): String {
    return try {
        val u = URI(input)
        (u.host ?: input).trim().trimEnd('/')
    } catch (_: Exception) {
        input.removePrefix("http://").removePrefix("https://").substringBefore('/')
    }
}

private fun getTestTypeDisplayName(testType: String): String {
    return when (testType) {
        // DoS Protection Tests
        "HttpSpike" -> "HTTP Spike Test"
        "ConnectionFlood" -> "Connection Flood Test"
        "TcpPortReachability" -> "TCP Port Test"
        "UdpPortReachability" -> "UDP Port Test"
        "BasicConnectivity" -> "Basic Connectivity Test"
        
        // WAF Testing
        "SqlInjection" -> "SQL Injection Test"
        "XssTest" -> "XSS Test"
        "PathTraversal" -> "Path Traversal Test"
        "CommandInjection" -> "Command Injection Test"
        "ReflectedXss" -> "Reflected XSS Test"
        "CustomRulesValidation" -> "Custom WAF Rules Test"
        "Log4ShellProbe" -> "Log4Shell Probe Test"
        "EdgeRateLimiting" -> "Rate Limiting Test"
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
        "CookieJsChallenge" -> "Cookie/JS Challenge Test"
        "WebCrawlerSimulation" -> "Web Crawler Test"
        
        // Default fallback
        else -> "$testType Test"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestConfigureScreen(
    category: String,
    testType: String,
    target: String?,
    onAddToCart: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val configVm: GenericTestConfigureViewModel = hiltViewModel()
    val state by configVm.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // IP Resolution State
    var resolvedIPs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isResolvingIP by remember { mutableStateOf(false) }
    var selectedIP by remember { mutableStateOf<String?>(null) }

    // --- ActivityResultLaunchers for Save/Load ---
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val jsonString = gson.toJson(state.params)
                        outputStream.write(jsonString.toByteArray())
                    }
                } catch (e: Exception) {
                    // Handle exception
                }
            }
        }
    }

    val loadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        
                        // Import single TestParameters and auto-add to cart
                        try {
                        val loadedParams = gson.fromJson(jsonString, TestParameters::class.java)
                        withContext(Dispatchers.Main) {
                                // Update parameters in UI
                            configVm.updateParams(loadedParams)
                                
                                // ✅ ADDITIVE AUTO-ADD to cart with imported parameters
                                val cfg = com.example.teost.data.model.TestConfiguration(
                                    testId = java.util.UUID.randomUUID().toString(),
                                    domain = state.domain,
                                    ipAddress = selectedIP,
                                    parameters = loadedParams.copy(
                                        testTypeHint = testType // Ensure correct test type
                                    )
                                )
                                val wasAdded = TestCartStore.addOrUpdate(cfg)
                                
                                android.widget.Toast.makeText(
                                    context,
                                    if (wasAdded) "✅ Imported and added $testType to cart (additive)" 
                                    else "✅ Imported and updated $testType in cart",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                
                                // Stay on current screen after import (no auto-navigation)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to load JSON: ${e.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Failed to import: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Seed the initial domain from navigation arguments
    LaunchedEffect(target) {
        target?.let { 
            val normalizedDomain = normalizeDomain(it)
            configVm.updateDomain(normalizedDomain)
            
            // Auto-resolve IP when target is provided
            if (normalizedDomain.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                isResolvingIP = true
                coroutineScope.launch {
                    try {
                        val ips = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            java.net.InetAddress.getAllByName(normalizedDomain)
                                .map { it.hostAddress ?: "" }
                                .filter { it.isNotBlank() }
                        }
                        resolvedIPs = ips
                        
                        // Auto-prefill IP for single resolution
                        if (ips.size == 1) {
                            selectedIP = ips.first()
                            android.util.Log.d("TestConfigureScreen", "Auto-prefilled single IP: ${ips.first()}")
                        }
                    } catch (e: Exception) {
                        resolvedIPs = emptyList()
                        android.util.Log.w("TestConfigureScreen", "Failed to resolve IPs for $normalizedDomain: ${e.message}")
                    } finally {
                        isResolvingIP = false
                    }
                }
            }
        }
    }

    // Load the template for the specific test type once
    LaunchedEffect(testType) {
        configVm.loadTemplateFor(testType)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(getTestTypeDisplayName(testType)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.back)) }
            },
            actions = {
                TextButton(onClick = {
                    val fileName = "teo_template_${testType}.json"
                    saveFileLauncher.launch(fileName)
                }) { Text("Save") }
                TextButton(onClick = {
                    loadFileLauncher.launch(arrayOf("application/json"))
                }) { Text("Import") }
            }
        )
    }, contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Primary Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                value = state.domain,
                onValueChange = { configVm.updateDomain(it) },
                    singleLine = true,
                label = { Text("Target Domain/IP")},
                modifier = Modifier.fillMaxWidth()
            )
            
            // IP Resolution Card
            if (resolvedIPs.isNotEmpty() || isResolvingIP) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                text = "DNS Resolution: ${state.domain}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        when {
                            isResolvingIP -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
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
                                    text = "✅ Resolved to: ${resolvedIPs.first()} (Auto-prefilled)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            else -> {
                                Text(
                                    text = "Multiple IPs resolved:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                resolvedIPs.forEach { ip ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedIP == ip,
                                            onClick = { 
                                                selectedIP = ip
                                                android.util.Log.d("TestConfigureScreen", "Selected IP: $ip")
                                            }
                                        )
                                        Text(
                                            text = ip,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Import Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📄 Import Templates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Import TestParameters JSON to auto-add test to cart\n• Use example templates: sql_injection_template.json, connection_flood_template.json, oversized_body_template.json\n• Additive Import: New tests added to existing cart (no override)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            HorizontalDivider()

            // Conditional HTTP Request Editor based on test type
            when (testType) {
                // TIER 1: WAF Tests - CRITICAL HTTP customization for injection testing
                "SqlInjection" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/login",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("id" to "{{PAYLOAD}}", "action" to "login"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf(
                            "Content-Type" to "application/x-www-form-urlencoded",
                            "X-Forwarded-For" to "127.0.0.1"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "username={{PAYLOAD}}&password=test123",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = configVm.isFieldRelevant("httpMethod"),
                        showPath = configVm.isFieldRelevant("requestPath"),
                        showQueryParams = configVm.isFieldRelevant("queryParams"),
                        showHeaders = configVm.isFieldRelevant("headers"),
                        showBody = configVm.isFieldRelevant("bodyTemplate"),
                        title = "SQL Injection Parameters"
                    )
                }
                
                "XssTest" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/comment",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("q" to "{{PAYLOAD}}", "page" to "1"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf(
                            "Content-Type" to "application/x-www-form-urlencoded",
                            "Referer" to "https://trusted-site.com"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "comment={{PAYLOAD}}&submit=true",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = configVm.isFieldRelevant("httpMethod"),
                        showPath = configVm.isFieldRelevant("requestPath"),
                        showQueryParams = configVm.isFieldRelevant("queryParams"),
                        showHeaders = configVm.isFieldRelevant("headers"),
                        showBody = configVm.isFieldRelevant("bodyTemplate"),
                        title = "XSS Testing Parameters"
                    )
                }
                
                "PathTraversal" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.GET,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/download",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("file" to "{{PAYLOAD}}", "path" to "../"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf("X-Real-IP" to "192.168.1.1"),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = null,
                        onBodyChange = { },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                        showBody = false,
                        title = "Path Traversal Parameters"
                    )
                }
                
                "CommandInjection" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/exec",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("cmd" to "{{PAYLOAD}}", "exec" to "true"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf(
                            "Content-Type" to "application/json",
                            "X-Forwarded-For" to "10.0.0.1"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "{\"command\": \"{{PAYLOAD}}\", \"args\": []}",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                        showBody = true,
                        title = "Command Injection Parameters"
                    )
                }
                
                // Other WAF tests with minimal customization
                "ReflectedXss", "CustomRulesValidation", "Log4ShellProbe" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("param" to "{{PAYLOAD}}"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "data={{PAYLOAD}}",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                        showBody = true,
                        title = "WAF Testing Parameters"
                    )
                }
                
                // TIER 1: API Protection Tests - CRITICAL for authentication and enumeration
                "BruteForce" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.apiEndpoint ?: "/api/auth/login",
                        onPathChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        queryParams = emptyMap(),
                        onQueryParamsChange = { },
                        headers = state.params.customHeaders ?: mapOf(
                            "Content-Type" to "application/json",
                            "X-API-Key" to "test-key"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "{\"username\": \"admin\", \"password\": \"{{PAYLOAD}}\"}",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = false,
                        showHeaders = true,
                        showBody = true,
                        title = "Brute Force Parameters"
                    )
                }
                
                "EnumerationIdor" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.GET,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.apiEndpoint ?: "/api/users",
                        onPathChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        queryParams = state.params.queryParams ?: mapOf("id" to "{{PAYLOAD}}", "format" to "json"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf(
                            "Authorization" to "Bearer {{TOKEN}}",
                            "X-API-Key" to "sk-test123"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = null,
                        onBodyChange = { },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                        showBody = false,
                        title = "IDOR Enumeration Parameters"
                    )
                }
                
                "AuthenticationTest" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.apiEndpoint ?: "/api/auth/validate",
                        onPathChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        queryParams = emptyMap(),
                        onQueryParamsChange = { },
                        headers = state.params.customHeaders ?: mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer {{PAYLOAD}}"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "{\"token\": \"{{PAYLOAD}}\", \"action\": \"validate\"}",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = false,
                        showHeaders = true,
                        showBody = true,
                        title = "Authentication Testing Parameters"
                    )
                }
                
                // Other API tests with generic configuration
                "SchemaInputValidation" -> {
                    HorizontalDivider()
                    Text("Schema Input Validation Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ SCHEMA VALIDATION SPECIFIC PARAMETERS
                    // Fuzz Cases - Malformed data
                    Text("Fuzzing Test Cases:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = (state.params.fuzzCases ?: listOf(
                            "{}",
                            "{\"malformed\": json}",
                            "{\"oversized\": \"" + "A".repeat(1000) + "\"}",
                            "null",
                            "[]",
                            "\"string_instead_of_object\"",
                            "{\"number\": \"not_a_number\"}"
                        )).joinToString("\n"),
                        onValueChange = { value ->
                            val payloads = value.split("\n").filter { it.isNotBlank() }
                            configVm.updateParams(state.params.copy(fuzzCases = payloads))
                        },
                        label = { Text("Fuzz Cases (one per line)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 8
                    )
                    
                    // Content Types - Different MIME types to test
                    var contentTypes by remember { 
                        mutableStateOf(state.params.contentTypes ?: listOf(
                            "application/json",
                            "application/xml", 
                            "text/plain",
                            "application/x-www-form-urlencoded"
                        ))
                    }
                    
                    Text("Content Types to Test", style = MaterialTheme.typography.labelMedium)
                    contentTypes.forEachIndexed { index, contentType ->
                        OutlinedTextField(
                            value = contentType,
                            onValueChange = { newValue ->
                                contentTypes = contentTypes.toMutableList().apply { set(index, newValue) }
                                configVm.updateParams(state.params.copy(contentTypes = contentTypes))
                            },
                            label = { Text("Content-Type ${index + 1}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // API Endpoint
                    OutlinedTextField(
                        value = state.params.apiEndpoint ?: "/api/validate",
                        onValueChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        label = { Text("Validation Endpoint") },
                        supportingText = { Text("API endpoint that validates input schema") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "BusinessLogicAbuse" -> {
                    HorizontalDivider()
                    Text("Business Logic Abuse Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ BUSINESS LOGIC SPECIFIC PARAMETERS
                    // Replay Count - How many times to repeat
                    OutlinedTextField(
                        value = (state.params.replayCount ?: 10).toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { count ->
                                configVm.updateParams(state.params.copy(replayCount = count.coerceIn(1, 1000)))
                            }
                        },
                        label = { Text("Replay Count") },
                        supportingText = { Text("Number of times to repeat the request (1-1000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Request Delay - Timing between replays
                    OutlinedTextField(
                        value = (state.params.requestDelayMs ?: 100).toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { delay ->
                                configVm.updateParams(state.params.copy(requestDelayMs = delay.coerceIn(0, 10000)))
                            }
                        },
                        label = { Text("Request Delay (ms)") },
                        supportingText = { Text("Delay between replays to test race conditions (0-10000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // API Endpoint
                    OutlinedTextField(
                        value = state.params.apiEndpoint ?: "/api/workflow",
                        onValueChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        label = { Text("Business Logic Endpoint") },
                        supportingText = { Text("API endpoint with business logic to abuse") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "CustomRulesValidation" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.apiEndpoint ?: "/api/data",
                        onPathChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        queryParams = state.params.queryParams ?: mapOf("type" to "test"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf("Content-Type" to "application/json"),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate ?: "{\"data\": \"{{PAYLOAD}}\", \"type\": \"test\"}",
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                        showBody = true,
                        title = "API Testing Parameters"
                    )
                }
                
                // TIER 2: Bot Management Tests - MODERATE customization for bot detection
                "UserAgentAnomaly" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.GET,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: emptyMap(),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf("User-Agent" to "{{PAYLOAD}}"),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate,
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = configVm.isFieldRelevant("httpMethod"),
                        showPath = configVm.isFieldRelevant("requestPath"),
                        showQueryParams = configVm.isFieldRelevant("queryParams"),
                        showHeaders = configVm.isFieldRelevant("headers"),
                        showBody = configVm.isFieldRelevant("bodyTemplate"),
                        title = "User-Agent Anomaly Parameters"
                    )
                }
                
                "CookieJsChallenge" -> {
                    HttpRequestEditor(
                        httpMethod = HttpMethod.GET,
                        onMethodChange = { },
                        requestPath = "/",
                        onPathChange = { },
                        queryParams = emptyMap(),
                        onQueryParamsChange = { },
                        headers = state.params.customHeaders ?: mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                            "Cookie" to "challenge={{PAYLOAD}}; session=test"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = null,
                        onBodyChange = { },
                        showMethod = false,
                        showPath = false,
                        showQueryParams = false,
                        showHeaders = true,
                        showBody = false,
                        title = "Cookie/JS Challenge Parameters"
                    )
                }
                
                "WebCrawlerSimulation" -> {
                    HttpRequestEditor(
                        httpMethod = HttpMethod.GET,
                        onMethodChange = { },
                        requestPath = state.params.requestPath ?: "/",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("crawl" to "true", "depth" to "{{PAYLOAD}}"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: mapOf(
                            "User-Agent" to "Googlebot/2.1 (+http://www.google.com/bot.html)",
                            "Accept" to "text/html,application/xhtml+xml"
                        ),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = null,
                        onBodyChange = { },
                        showMethod = false,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                        showBody = false,
                        title = "Web Crawler Parameters"
                    )
                }
                
                // TIER 3: DoS Tests - MINIMAL customization (volume/rate focus, not content)
                "HttpSpike" -> {
                    HttpRequestEditor(
                        httpMethod = state.params.httpMethod ?: HttpMethod.GET,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: emptyMap(),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                        headers = state.params.customHeaders ?: emptyMap(),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = state.params.bodyTemplate,
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = configVm.isFieldRelevant("httpMethod"),
                        showPath = configVm.isFieldRelevant("requestPath"),
                        showQueryParams = configVm.isFieldRelevant("queryParams"),
                        showHeaders = configVm.isFieldRelevant("headers"),
                        showBody = configVm.isFieldRelevant("bodyTemplate"),
                        title = "HTTP Spike Parameters"
                    )
                }
                
                "ConnectionFlood" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Connection Flood Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Duration Configuration - CRITICAL PARAMETER
                        OutlinedTextField(
                            value = (state.params.durationSec ?: 10).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { duration ->
                                    configVm.updateParams(state.params.copy(durationSec = duration.coerceIn(5, 300)))
                                }
                            },
                            label = { Text("Test Duration (seconds)") },
                            supportingText = { Text("How long to run the flood attack (5-300 seconds)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Concurrent Connections - CRITICAL PARAMETER  
                        OutlinedTextField(
                            value = (state.params.concurrentConnections ?: 10).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { connections ->
                                    configVm.updateParams(state.params.copy(concurrentConnections = connections.coerceIn(1, 100)))
                                }
                            },
                            label = { Text("Concurrent Connections") },
                            supportingText = { Text("Number of parallel connections (1-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // RPS Target - OPTIONAL PARAMETER
                        OutlinedTextField(
                            value = (state.params.rpsTarget ?: 50).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { rps ->
                                    configVm.updateParams(state.params.copy(rpsTarget = rps.coerceIn(1, 1000)))
                                }
                            },
                            label = { Text("Requests Per Second (RPS)") },
                            supportingText = { Text("Target request rate (1-1000 RPS)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // HTTP Configuration - ACTUAL PARAMETERS USED
                    HttpRequestEditor(
                            httpMethod = state.params.httpMethod ?: HttpMethod.GET,
                            onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                            requestPath = state.params.requestPath ?: "/",
                            onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                            queryParams = state.params.queryParams ?: emptyMap(),
                            onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                            headers = state.params.customHeaders ?: mapOf("User-Agent" to "TEO-SecurityTest/1.0"),
                            onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                            bodyTemplate = state.params.bodyTemplate,
                            onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                            showMethod = true,
                            showPath = true,
                            showQueryParams = true,
                            showHeaders = true,
                            showBody = true,
                            title = "HTTP Request Configuration"
                        )
                    }
                }
                
                // Rate limiting with IP simulation
                "EdgeRateLimiting" -> {
                    HttpRequestEditor(
                        httpMethod = HttpMethod.GET,
                        onMethodChange = { },
                        requestPath = "/",
                        onPathChange = { },
                        queryParams = emptyMap(),
                        onQueryParamsChange = { },
                        headers = state.params.customHeaders ?: mapOf("X-Forwarded-For" to "{{PAYLOAD}}"),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                        bodyTemplate = null,
                        onBodyChange = { },
                        showMethod = false,
                        showPath = false,
                        showQueryParams = false,
                        showHeaders = true,
                        showBody = false,
                        title = "Rate Limiting Parameters"
                    )
                }
                
                "LongQuery" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Long Query Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Parameter Length - CRITICAL PARAMETER
                        OutlinedTextField(
                            value = (state.params.paramLength ?: 8192).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { length ->
                                    configVm.updateParams(state.params.copy(paramLength = length.coerceIn(1024, 65536)))
                                }
                            },
                            label = { Text("Query Parameter Length (bytes)") },
                            supportingText = { Text("Length of query string to generate (1024-65536 bytes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // HTTP Configuration
                    HttpRequestEditor(
                            httpMethod = state.params.httpMethod ?: HttpMethod.GET,
                        onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                        requestPath = state.params.requestPath ?: "/",
                        onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                        queryParams = state.params.queryParams ?: mapOf("data" to "{{PAYLOAD}}"),
                        onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                            headers = state.params.customHeaders ?: mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                        onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                            bodyTemplate = state.params.bodyTemplate,
                        onBodyChange = { configVm.updateParams(state.params.copy(bodyTemplate = it)) },
                        showMethod = true,
                        showPath = true,
                        showQueryParams = true,
                        showHeaders = true,
                            showBody = false,
                            title = "HTTP Request Configuration"
                        )
                    }
                }
                
                "OversizedBody" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Oversized Body Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Body Size - CRITICAL PARAMETER
                        OutlinedTextField(
                            value = (state.params.bodySizeKb ?: 256).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { sizeKb ->
                                    configVm.updateParams(state.params.copy(bodySizeKb = sizeKb.coerceIn(1, 10240)))
                                }
                            },
                            label = { Text("Body Size (KB)") },
                            supportingText = { Text("Size of request body to generate (1-10240 KB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // JSON Field Count - CRITICAL PARAMETER
                        OutlinedTextField(
                            value = (state.params.jsonFieldCount ?: 100).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { fieldCount ->
                                    configVm.updateParams(state.params.copy(jsonFieldCount = fieldCount.coerceIn(1, 1000)))
                                }
                            },
                            label = { Text("JSON Field Count") },
                            supportingText = { Text("Number of JSON fields to generate (1-1000)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // HTTP Configuration
                        HttpRequestEditor(
                            httpMethod = state.params.httpMethod ?: HttpMethod.POST,
                            onMethodChange = { configVm.updateParams(state.params.copy(httpMethod = it)) },
                            requestPath = state.params.requestPath ?: "/api/upload",
                            onPathChange = { configVm.updateParams(state.params.copy(requestPath = it)) },
                            queryParams = state.params.queryParams ?: emptyMap(),
                            onQueryParamsChange = { configVm.updateParams(state.params.copy(queryParams = it)) },
                            headers = state.params.customHeaders ?: mapOf("Content-Type" to "application/json"),
                            onHeadersChange = { configVm.updateParams(state.params.copy(customHeaders = it)) },
                            bodyTemplate = null, // Body is auto-generated based on size parameters
                            onBodyChange = { },
                            showMethod = true,
                            showPath = true,
                            showQueryParams = true,
                            showHeaders = true,
                            showBody = false, // Body is auto-generated
                            title = "HTTP Request Configuration"
                        )
                    }
                }
                
                // TIER 4: Network Tests - NO HTTP customization needed
                "TcpPortReachability" -> {
                    Text(
                        text = "TCP Port Connectivity Testing\n\nThis test checks TCP port reachability at the network level. No HTTP configuration is required - only specify the target port in the test parameters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                "UdpReachability" -> {
                    Text(
                        text = "UDP Port Reachability Testing\n\nThis test checks UDP port connectivity at the network level. No HTTP configuration is required - only specify the target port in the test parameters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                "BasicConnectivity" -> {
                    Text(
                        text = "Basic Network Connectivity Test\n\nThis test performs basic network connectivity checks (ping, DNS resolution). No HTTP configuration is required.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                "IpRegionBlocking" -> {
                    Text(
                        text = "IP/Region Blocking Test\n\nThis test checks geographic and IP-based access restrictions. No HTTP configuration is required - the test simulates requests from different regions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            when (testType) {
                "SqlInjection" -> {
                    HorizontalDivider()
                    Text("SQL Injection Attack Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ SQL INJECTION SPECIFIC PARAMETERS
                    Text("SQL Injection Payloads:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = (state.params.payloadList ?: listOf(
                            "1' OR 1=1--",
                            "'; DROP TABLE users--", 
                            "1' UNION SELECT NULL,NULL,NULL--",
                            "1' AND SLEEP(5)--",
                            "1' OR '1'='1"
                        )).joinToString("\n"),
                        onValueChange = { value ->
                            val payloads = value.split("\n").filter { it.isNotBlank() }
                            configVm.updateParams(state.params.copy(payloadList = payloads))
                        },
                        label = { Text("SQL Payloads (one per line)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 8
                    )
                    
                    // ✅ INJECTION POINT - Where to inject SQL
                    var selectedInjectionPoint by remember { mutableStateOf(state.params.injectionPoint ?: InjectionPoint.QUERY_PARAM) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Injection Point:", style = MaterialTheme.typography.labelMedium)
                        FilterChip(
                            onClick = { 
                                selectedInjectionPoint = when (selectedInjectionPoint) {
                                    InjectionPoint.QUERY_PARAM -> InjectionPoint.HEADER
                                    InjectionPoint.HEADER -> InjectionPoint.BODY
                                    else -> InjectionPoint.QUERY_PARAM
                                }
                                configVm.updateParams(state.params.copy(injectionPoint = selectedInjectionPoint))
                            },
                            label = { Text(selectedInjectionPoint.name.replace('_', ' ')) },
                            selected = true
                        )
                    }
                    
                    // ✅ TARGET PARAMETER - Which parameter to inject
                    OutlinedTextField(
                        value = state.params.targetParam ?: "id",
                        onValueChange = { configVm.updateParams(state.params.copy(targetParam = it)) },
                        label = { Text("Target Parameter") },
                        supportingText = { Text("Parameter name to inject SQL payloads into") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "XssTest" -> {
                    HorizontalDivider()
                    Text("Cross-Site Scripting (XSS) Attack Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ XSS SPECIFIC PARAMETERS
                    Text("XSS Payloads:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = (state.params.payloadList ?: listOf(
                            "<script>alert('XSS')</script>",
                            "<img src=x onerror=alert('XSS')>",
                            "javascript:alert('XSS')",
                            "<svg onload=alert('XSS')>",
                            "'><script>alert('XSS')</script>"
                        )).joinToString("\n"),
                        onValueChange = { value ->
                            val payloads = value.split("\n").filter { it.isNotBlank() }
                            configVm.updateParams(state.params.copy(payloadList = payloads))
                        },
                        label = { Text("XSS Payloads (one per line)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 8
                    )
                    
                    // ✅ INJECTION POINT - Where to inject XSS
                    var selectedInjectionPoint by remember { mutableStateOf(state.params.injectionPoint ?: InjectionPoint.QUERY_PARAM) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Injection Point:", style = MaterialTheme.typography.labelMedium)
                        FilterChip(
                            onClick = { 
                                selectedInjectionPoint = when (selectedInjectionPoint) {
                                    InjectionPoint.QUERY_PARAM -> InjectionPoint.HEADER
                                    InjectionPoint.HEADER -> InjectionPoint.BODY
                                    else -> InjectionPoint.QUERY_PARAM
                                }
                                configVm.updateParams(state.params.copy(injectionPoint = selectedInjectionPoint))
                            },
                            label = { Text(selectedInjectionPoint.name.replace('_', ' ')) },
                            selected = true
                        )
                    }
                }
                "PathTraversal" -> {
                    HorizontalDivider()
                    Text("Path Traversal Attack Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ PATH TRAVERSAL SPECIFIC PARAMETERS
                    Text("Path Traversal Payloads:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = (state.params.payloadList ?: listOf(
                            "../../../etc/passwd",
                            "..\\..\\..\\windows\\system32\\config\\sam",
                            "....//....//....//etc//passwd",
                            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
                            "../../../../../../etc/passwd%00"
                        )).joinToString("\n"),
                        onValueChange = { value ->
                            val payloads = value.split("\n").filter { it.isNotBlank() }
                            configVm.updateParams(state.params.copy(payloadList = payloads))
                        },
                        label = { Text("Path Traversal Payloads (one per line)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 8
                    )
                }
                "UserAgentAnomaly" -> {
                    HorizontalDivider()
                    Text("User-Agent Anomaly Detection Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ USER AGENT PROFILES - Bot simulation
                    var selectedProfiles by remember { 
                        mutableStateOf(state.params.uaProfiles ?: listOf(
                            "Googlebot/2.1 (+http://www.google.com/bot.html)",
                            "curl/7.68.0",
                            "python-requests/2.28.1",
                            "PostmanRuntime/7.29.2"
                        ))
                    }
                    
                    Text("Bot User-Agent Profiles", style = MaterialTheme.typography.labelMedium)
                    selectedProfiles.forEachIndexed { index, profile ->
                        OutlinedTextField(
                            value = profile,
                            onValueChange = { newValue ->
                                selectedProfiles = selectedProfiles.toMutableList().apply { set(index, newValue) }
                                configVm.updateParams(state.params.copy(uaProfiles = selectedProfiles))
                            },
                            label = { Text("User-Agent ${index + 1}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // ✅ HEADER MINIMAL MODE - Stealth detection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.params.headerMinimal ?: false,
                            onCheckedChange = { configVm.updateParams(state.params.copy(headerMinimal = it)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Use minimal headers (stealth mode)")
                    }
                    
                    // ✅ ACCEPT LANGUAGE - Browser simulation
                    OutlinedTextField(
                        value = state.params.acceptLanguage ?: "en-US,en;q=0.9",
                        onValueChange = { configVm.updateParams(state.params.copy(acceptLanguage = it)) },
                        label = { Text("Accept-Language Header") },
                        supportingText = { Text("Language preference for browser simulation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "BruteForce" -> {
                    HorizontalDivider()
                    Text("Brute Force Attack Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ BRUTE FORCE SPECIFIC PARAMETERS
                    // Target Username
                    OutlinedTextField(
                        value = state.params.username ?: "admin",
                        onValueChange = { configVm.updateParams(state.params.copy(username = it)) },
                        label = { Text("Target Username") },
                        supportingText = { Text("Username to brute force") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Password List
                    Text("Password Dictionary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = (state.params.passwordList ?: listOf(
                            "password", "123456", "admin", 
                            "Password123!", "qwerty", "letmein",
                            "password1", "123456789", "welcome"
                        )).joinToString("\n"),
                        onValueChange = { value ->
                            val passwords = value.split("\n").filter { it.isNotBlank() }
                            configVm.updateParams(state.params.copy(passwordList = passwords))
                        },
                        label = { Text("Passwords (one per line)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 8
                    )
                    
                    // Request Delay - Anti-detection
                    OutlinedTextField(
                        value = (state.params.requestDelayMs ?: 200).toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { delay ->
                                configVm.updateParams(state.params.copy(requestDelayMs = delay.coerceIn(0, 10000)))
                            }
                        },
                        label = { Text("Request Delay (ms)") },
                        supportingText = { Text("Delay between attempts to avoid detection (0-10000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // API Endpoint
                    OutlinedTextField(
                        value = state.params.apiEndpoint ?: "/api/auth/login",
                        onValueChange = { configVm.updateParams(state.params.copy(apiEndpoint = it)) },
                        label = { Text("API Endpoint") },
                        supportingText = { Text("Authentication endpoint to attack") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "HttpSpike" -> {
                    HorizontalDivider()
                    Text("HTTP Spike Attack Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    // ✅ BURST REQUESTS - Primary parameter (was missing!)
                    OutlinedTextField(
                        value = (state.params.burstRequests ?: 100).toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { requests ->
                                configVm.updateParams(state.params.copy(burstRequests = requests.coerceIn(10, 1000)))
                            }
                        },
                        label = { Text("Burst Requests") },
                        supportingText = { Text("Total number of requests to send (10-1000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // ✅ BURST INTERVAL - Critical timing parameter (was missing!)
                    OutlinedTextField(
                        value = (state.params.burstIntervalMs ?: 100).toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { interval ->
                                configVm.updateParams(state.params.copy(burstIntervalMs = interval.coerceIn(10, 5000)))
                            }
                        },
                        label = { Text("Request Interval (ms)") },
                        supportingText = { Text("Delay between requests in milliseconds (10-5000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // ✅ CONCURRENCY LEVEL - Thread count
                    com.example.teost.presentation.screens.test.components.SliderConfig(
                        label = "Concurrent Threads",
                        value = state.params.concurrencyLevel ?: 5,
                        onValueChange = { v -> configVm.updateParams(state.params.copy(concurrencyLevel = v)) },
                        valueRange = 1..20,
                        step = 1
                    )
                    
                    // ✅ DURATION - Maximum test duration (hybrid approach)
                    com.example.teost.presentation.screens.test.components.SliderConfig(
                        label = "Maximum Duration",
                        value = state.params.durationSec ?: 60,
                        onValueChange = { v -> configVm.updateParams(state.params.copy(durationSec = v)) },
                        valueRange = 10..300,
                        step = 10,
                        unitSuffix = "seconds"
                    )
                }
                "ConnectionFlood" -> {
                    HorizontalDivider()
                    Text("Traffic Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    com.example.teost.presentation.screens.test.components.SliderConfig(
                        label = "Concurrent Users",
                        value = state.params.concurrencyLevel ?: 5, // Median of 1-10
                        onValueChange = { v -> configVm.updateParams(state.params.copy(concurrencyLevel = v)) },
                        valueRange = 1..10, // Safe range for testing
                        step = 1
                    )
                    com.example.teost.presentation.screens.test.components.SliderConfig(
                        label = "Test Duration",
                        value = state.params.durationSec ?: 30, // Median of 10-60
                        onValueChange = { v -> configVm.updateParams(state.params.copy(durationSec = v)) },
                        valueRange = 10..60, // Safe range for testing
                        step = 5,
                        unitSuffix = "seconds"
                    )
                }
                "EnumerationIdor" -> {
                    HorizontalDivider()
                    Text("Enumeration / IDOR Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    EnumerationIdorConfig(
                        params = state.params,
                        onParamsChange = { configVm.updateParams(it) }
                    )
                }
                "WebCrawlerSimulation" -> {
                    HorizontalDivider()
                    Text("Crawler Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    CrawlerConfig(
                        params = state.params,
                        onParamsChange = { configVm.updateParams(it) }
                    )
                }
                "OversizedBody" -> {
                    HorizontalDivider()
                    Text("Oversized Body Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OversizedBodyConfig(
                        params = state.params,
                        onParamsChange = { configVm.updateParams(it) }
                    )
                }
            }

            // Validation errors display
            val validationErrors = configVm.validateCurrentParams()
            if (validationErrors.isNotEmpty()) {
                HorizontalDivider()
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Configuration Issues:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        validationErrors.forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                    Button(
                        onClick = {
                            val cfg = com.example.teost.data.model.TestConfiguration(
                                testId = UUID.randomUUID().toString(),
                                domain = state.domain,
                                ipAddress = selectedIP, // ✅ USE SELECTED/AUTO-PREFILLED IP
                                parameters = state.params.copy(
                                    testTypeHint = testType // ✅ STORE TEST TYPE FOR ACCURATE EDIT NAVIGATION
                                )
                            )
                            val wasAdded = TestCartStore.addOrUpdate(cfg)
                            if (wasAdded) {
                                // New configuration added - show toast but DON'T auto-navigate
                                coroutineScope.launch {
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Test added to cart", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                // DON'T call onAddToCart() - let user manually navigate to cart
                            } else {
                                // Parameters updated - show toast but DON'T auto-navigate
                                coroutineScope.launch {
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Test configuration updated in cart", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                // DON'T call onAddToCart() - let user manually navigate to cart
                            }
                        },
                enabled = state.domain.isNotBlank() && validationErrors.isEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) { 
                        val existingConfig = TestCartStore.items.collectAsState().value.find { existing ->
                            existing.domain == state.domain
                        }
                        Text(
                            if (existingConfig != null) "Update in Cart" else androidx.compose.ui.res.stringResource(id = com.example.teost.core.ui.R.string.add_to_cart)
                        )
                    }
        }
    }
}

// Helper functions removed - now using specific configurations per test type

@Preview(showBackground = true)
@Composable
fun TestConfigureScreenPreview() { EdgeOneTheme { TestConfigureScreen(category = "DDOS_PROTECTION", testType = "HttpSpike", target = "example.com") } }
