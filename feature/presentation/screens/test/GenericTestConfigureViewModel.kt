package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import com.example.teost.data.model.TestConfiguration
import com.example.teost.data.model.TestParameters
import com.example.teost.data.model.Config
import com.example.teost.data.model.EncodingMode
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.example.teost.data.model.HttpMethod

@HiltViewModel
class GenericTestConfigureViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    private var currentTestType: String = ""

    fun loadTemplateFor(testType: String) {
        currentTestType = testType
        val templateParams = when (testType) {
            "SqlInjection" -> TestParameters(
                payloadList = listOf(
                    "1' OR 1=1--",
                    "1' UNION SELECT NULL,NULL,NULL--", 
                    "'; DROP TABLE users--",
                    "1' AND SLEEP(5)--"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                // injectionPoint = InjectionPoint.QUERY_PARAM,  // ← Removed - enum not found
                targetParam = "id",
                httpMethod = HttpMethod.POST,
                requestPath = "/login",
                queryParams = mapOf("id" to "{{PAYLOAD}}", "action" to "login"),
                customHeaders = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "X-Forwarded-For" to "127.0.0.1"
                ),
                bodyTemplate = "username={{PAYLOAD}}&password=test123"
            )
            "XssTest" -> TestParameters(
                payloadList = listOf(
                    "<script>alert('XSS')</script>",
                    "<img src=x onerror=alert('XSS')>",
                    "javascript:alert('XSS')",
                    "<svg onload=alert('XSS')>"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                // injectionPoint = InjectionPoint.QUERY_PARAM,  // ← Removed - enum not found
                httpMethod = HttpMethod.POST,
                requestPath = "/comment",
                queryParams = mapOf("q" to "{{PAYLOAD}}", "page" to "1"),
                customHeaders = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Referer" to "https://trusted-site.com"
                ),
                bodyTemplate = "comment={{PAYLOAD}}&submit=true"
            )
            "HttpSpike" -> TestParameters(
                burstRequests = 100,
                burstIntervalMs = 100,
                concurrencyLevel = 10,
                sustainedRpsWindow = 5,
                httpMethod = HttpMethod.GET,
                requestPath = "/"
            )
            "ConnectionFlood" -> TestParameters(
                concurrentConnections = 50,
                durationSec = 30,
                rpsTarget = 20,
                requestPath = "/"
            )
            "UserAgentAnomaly" -> TestParameters(
                uaProfiles = listOf(
                    "Googlebot/2.1 (+http://www.google.com/bot.html)",
                    "curl/7.68.0",
                    "python-requests/2.28.1"
                ),
                headerMinimal = false,
                acceptLanguage = "en-US,en;q=0.9",
                customHeaders = mapOf("User-Agent" to "{{PAYLOAD}}")
            )
            "BruteForce" -> TestParameters(
                username = "admin",
                passwordList = listOf("password", "123456", "admin", "Password123!", "qwerty"),
                requestDelayMs = 200,
                apiEndpoint = "/api/auth/login",
                httpMethod = HttpMethod.POST,
                customHeaders = mapOf("Content-Type" to "application/json"),
                bodyTemplate = "{\"username\": \"admin\", \"password\": \"{{PAYLOAD}}\"}"
            )
            else -> TestParameters(
                httpMethod = HttpMethod.GET,
                timeoutMs = 15000,
                requestPath = "/"
            )
        }
        _state.value = _state.value.copy(params = templateParams)
    }

    fun updateParams(newParams: TestParameters) {
        _state.value = _state.value.copy(params = newParams)
    }

    fun updateDomain(newDomain: String) {
        _state.value = _state.value.copy(domain = newDomain)
    }
    
    fun validateCurrentParams(): List<String> {
        val params = _state.value.params
        val errors = mutableListOf<String>()
        
        // Basic validation
        if (params.timeoutMs != null && params.timeoutMs!! <= 0) {
            errors.add("Timeout must be greater than 0")
        }
        
        // Test-specific validation based on current test type
        when (currentTestType) {
            "SqlInjection", "XssTest", "PathTraversal", "CommandInjection" -> {
                if (params.payloadList.isNullOrEmpty()) {
                    errors.add("Payload list is required for ${currentTestType}")
                }
                if (currentTestType == "SqlInjection" && params.targetParam.isNullOrBlank()) {
                    errors.add("Target parameter is required for SQL injection test")
                }
            }
            "HttpSpike" -> {
                if (params.burstRequests == null || params.burstRequests!! < 1) {
                    errors.add("Burst requests must be at least 1")
                }
                if (params.concurrencyLevel == null || params.concurrencyLevel!! < 1) {
                    errors.add("Concurrency level must be at least 1")
                }
            }
            "ConnectionFlood" -> {
                if (params.concurrentConnections == null || params.concurrentConnections!! < 1) {
                    errors.add("Concurrent connections must be at least 1")
                }
                if (params.durationSec == null || params.durationSec!! < 1) {
                    errors.add("Duration must be at least 1 second")
                }
            }
            "BruteForce" -> {
                if (params.username.isNullOrBlank()) {
                    errors.add("Username is required for brute force test")
                }
                if (params.passwordList.isNullOrEmpty()) {
                    errors.add("Password list is required for brute force test")
                }
                if (params.apiEndpoint.isNullOrBlank()) {
                    errors.add("API endpoint is required for brute force test")
                }
            }
            "UserAgentAnomaly" -> {
                if (params.uaProfiles.isNullOrEmpty()) {
                    errors.add("User-Agent profiles are required for bot detection test")
                }
            }
            "EnumerationIdor" -> {
                if (params.enumTemplate.isNullOrBlank()) {
                    errors.add("Enumeration template is required for IDOR test")
                }
                if (params.idRange.isNullOrEmpty() || params.idRange!!.size < 2) {
                    errors.add("ID range (start and end) is required for IDOR test")
                }
            }
            "SchemaInputValidation" -> {
                if (params.fuzzCases.isNullOrEmpty()) {
                    errors.add("Fuzz cases are required for schema validation test")
                }
            }
            "BusinessLogicAbuse" -> {
                if (params.replayCount == null || params.replayCount!! < 1) {
                    errors.add("Replay count must be at least 1")
                }
            }
        }
        
        return errors
    }
    
    fun isFieldRelevant(fieldName: String): Boolean {
        return when (currentTestType) {
            "SqlInjection" -> fieldName in setOf(
                "httpMethod", "requestPath", "queryParams", "customHeaders", "bodyTemplate"
            )
            "XssTest" -> fieldName in setOf(
                "httpMethod", "requestPath", "queryParams", "customHeaders", "bodyTemplate"
            )
            "PathTraversal" -> fieldName in setOf(
                "requestPath", "customHeaders"
            )
            "HttpSpike" -> fieldName in setOf(
                "httpMethod", "requestPath"
            )
            "ConnectionFlood" -> fieldName in setOf(
                "requestPath"
            )
            "UserAgentAnomaly" -> fieldName in setOf(
                "customHeaders"
            )
            "BruteForce" -> fieldName in setOf(
                "httpMethod", "customHeaders", "bodyTemplate"
            )
            "CustomRulesValidation" -> fieldName in setOf(
                "requestPath", "customHeaders"
            )
            "OversizedBody" -> fieldName in setOf(
                "httpMethod", "requestPath", "customHeaders"
            )
            "CookieJsChallenge", "WebCrawlerSimulation" -> fieldName in setOf(
                "customHeaders"
            )
            "EnumerationIdor", "SchemaInputValidation", "BusinessLogicAbuse" -> fieldName in setOf(
                "httpMethod"
            )
            else -> true // Show all for unknown test types
        }
    }
    
    fun isValidConfiguration(): Boolean {
        val state = _state.value
        return state.domain.isNotBlank() && validateCurrentParams().isEmpty()
    }

    data class UiState(
        val domain: String = "",
        val params: TestParameters = TestParameters()
    )
}
