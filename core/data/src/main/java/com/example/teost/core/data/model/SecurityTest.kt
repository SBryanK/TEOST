package com.example.teost.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.util.Date
import java.util.UUID

@Parcelize
data class SecurityTest(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: TestCategory,
    val type: TestType,
    val description: String,
    val creditCost: Int,
    val estimatedDuration: Int, // in seconds
    val parameters: Map<String, @kotlinx.parcelize.RawValue Any> = emptyMap(),
    val isConfigurable: Boolean = true,
    val isPremium: Boolean = false,
    val tags: List<String> = emptyList()
) : Parcelable

@Parcelize
enum class TestCategory : Parcelable {
    DDOS_PROTECTION,
    WEB_PROTECTION,
    BOT_MANAGEMENT,
    API_PROTECTION
}

@Parcelize
sealed class TestType : Parcelable {
    // DoS / Network Protection Tests
    @Parcelize
    data object HttpSpike : TestType()
    @Parcelize
    data object IpRegionBlocking : TestType()
    @Parcelize
    data object TcpPortReachability : TestType()
    @Parcelize
    data object UdpReachability : TestType()
    @Parcelize
    data object ConnectionFlood : TestType()
    @Parcelize
    data object BasicConnectivity : TestType()
    
    // Web Protection (WAF) Tests
    @Parcelize
    data object SqlInjection : TestType()
    @Parcelize
    data object XssTest : TestType()
    @Parcelize
    data object ReflectedXss : TestType()
    @Parcelize
    data object PathTraversal : TestType()
    @Parcelize
    data object CommandInjection : TestType()
    @Parcelize
    data object Log4ShellProbe : TestType()
    @Parcelize
    data object CustomRulesValidation : TestType()
    @Parcelize
    data object EdgeRateLimiting : TestType()
    @Parcelize
    data object LongQuery : TestType()
    @Parcelize
    data object OversizedBody : TestType()
    
    // Bot Management Tests
    @Parcelize
    data object UserAgentAnomaly : TestType()
    @Parcelize
    data object CookieJsChallenge : TestType()
    @Parcelize
    data object WebCrawlerSimulation : TestType()

    // API Protection Tests
    @Parcelize
    data object AuthenticationTest : TestType()
    @Parcelize
    data object BruteForce : TestType()
    @Parcelize
    data object EnumerationIdor : TestType()
    @Parcelize
    data object SchemaInputValidation : TestType()
    @Parcelize
    data object BusinessLogicAbuse : TestType()
}

@Parcelize
data class TestConfiguration(
    val testId: String,
    val domain: String,
    val ipAddress: String? = null,
    val port: Int? = null,
    val parameters: TestParameters,
    val scheduledTime: Date? = null,
    val priority: TestPriority = TestPriority.NORMAL
) : Parcelable {
    // Defensive copy to prevent NULL string issues in Parcel
    fun safeForParcel(): TestConfiguration = copy(
        testId = testId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
        domain = domain.takeIf { it.isNotBlank() } ?: "unknown.domain",
        ipAddress = ipAddress?.takeIf { it.isNotBlank() },
        parameters = parameters.safeForParcel()
    )
}

@Parcelize
data class TestParameters(
    // DoS Parameters
    val burstRequests: Int? = null,
    val burstIntervalMs: Int? = null,
    val sustainedRpsWindow: Int? = null,
    val concurrencyLevel: Int? = null,
    val useVpnProfile: String? = null,
    val targetPath: String? = null,
    val customHeaders: Map<String, String>? = null,
    val timeoutMs: Int? = null,
    val retryCount: Int? = null,
    val payloadSize: PayloadSize? = null,
    val concurrentConnections: Int? = null,
    val durationSec: Int? = null,
    val rampUpStrategy: RampUpStrategy? = null,
    
    // WAF Parameters
    val payloadList: List<String>? = null,
    val encodingMode: EncodingMode? = null,
    val httpMethod: HttpMethod? = null,
    val targetParam: String? = null,
    val encode: Boolean? = null,
    val doubleEncode: Boolean? = null,
    val injectionPoint: InjectionPoint? = null,
    val obfuscation: String? = null,
    val headersOverrides: Map<String, String>? = null,
    val requestPath: String? = null,
    val rpsTarget: Int? = null,
    val windowSec: Int? = null,
    val burstMode: Boolean? = null,
    val fingerprintStrategy: String? = null,
    val paramLength: Int? = null,
    val bodySizeKb: Int? = null,
    val jsonFieldCount: Int? = null,
    val multipartFieldSize: Int? = null,
    
    // Bot Management Parameters
    val uaProfiles: List<String>? = null,
    val headerMinimal: Boolean? = null,
    val acceptLanguage: String? = null,
    val cookiePolicy: CookiePolicy? = null,
    val jsRuntimeMode: String? = null,
    val respectRobotsTxt: Boolean? = null,
    val crawlDepth: Int? = null,

    // API Protection Parameters
    val authMode: String? = null, // header|query|cookie
    val authToken: String? = null,
    val apiEndpoint: String? = null,
    val username: String? = null,
    val passwordList: List<String>? = null,
    val enumTemplate: String? = null, // e.g., /api/object/{id}
    val idRange: List<Long>? = null, // [start, end]
    val stepSize: Int? = null,
    val fuzzCases: List<String>? = null,
    val contentTypes: List<String>? = null,
    val replayCount: Int? = null,
    val requestDelayMs: Int? = null,
    
    // Additional fields for UI compatibility
    val queryParams: Map<String, String>? = null,
    val headers: Map<String, String>? = null,
    val bodyTemplate: String? = null,
    val rampUpSec: Int? = null,
    val concurrency: Int? = null,
    
    // Test type hint for accurate edit navigation
    val testTypeHint: String? = null
) : Parcelable {
    // Defensive copy to prevent NULL string issues in Parcel
    fun safeForParcel(): TestParameters = copy(
        useVpnProfile = useVpnProfile?.takeIf { it.isNotBlank() },
        targetPath = targetPath?.takeIf { it.isNotBlank() },
        targetParam = targetParam?.takeIf { it.isNotBlank() },
        obfuscation = obfuscation?.takeIf { it.isNotBlank() },
        requestPath = requestPath?.takeIf { it.isNotBlank() },
        fingerprintStrategy = fingerprintStrategy?.takeIf { it.isNotBlank() },
        acceptLanguage = acceptLanguage?.takeIf { it.isNotBlank() },
        jsRuntimeMode = jsRuntimeMode?.takeIf { it.isNotBlank() },
        authMode = authMode?.takeIf { it.isNotBlank() },
        authToken = authToken?.takeIf { it.isNotBlank() },
        apiEndpoint = apiEndpoint?.takeIf { it.isNotBlank() },
        username = username?.takeIf { it.isNotBlank() },
        enumTemplate = enumTemplate?.takeIf { it.isNotBlank() },
        bodyTemplate = bodyTemplate?.takeIf { it.isNotBlank() },
        // Safe copy for collections - filter out empty/null strings
        customHeaders = customHeaders?.filterValues { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        headersOverrides = headersOverrides?.filterValues { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        queryParams = queryParams?.filterValues { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        headers = headers?.filterValues { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        payloadList = payloadList?.filter { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        passwordList = passwordList?.filter { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        uaProfiles = uaProfiles?.filter { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        contentTypes = contentTypes?.filter { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() },
        fuzzCases = fuzzCases?.filter { !it.isNullOrBlank() }?.takeIf { it.isNotEmpty() }
    )
}

// Helper extensions for UI summaries
fun TestParameters.summary(): String {
    val parts = mutableListOf<String>()
    
    // HTTP Method (if not GET)
    httpMethod?.let { if (it != HttpMethod.GET) parts += "Method: ${it.name}" }
    
    // Request Path (if not root)
    requestPath?.let { if (it != "/") parts += "Path: $it" }
    
    // Concurrency/Virtual Users
    concurrency?.let { parts += "Users: $it" }
    concurrentConnections?.let { parts += "Connections: $it" }
    
    // Duration
    durationSec?.let { parts += "Duration: ${it}s" }
    
    // Ramp up time
    rampUpSec?.let { if (it != 0) parts += "Ramp: ${it}s" }
    
    // DoS specific parameters
    burstRequests?.let { parts += "Burst: $it req" }
    burstIntervalMs?.let { parts += "Interval: ${it}ms" }
    rpsTarget?.let { parts += "RPS: $it" }
    
    // WAF specific parameters
    payloadList?.let { if (it.isNotEmpty()) parts += "Payloads: ${it.size}" }
    encodingMode?.let { parts += "Encoding: ${it.name.lowercase().replace('_', ' ')}" }
    injectionPoint?.let { parts += "Injection: ${it.name.lowercase().replace('_', ' ')}" }
    
    // API specific parameters
    apiEndpoint?.let { if (it.isNotBlank()) parts += "Endpoint: $it" }
    authMode?.let { parts += "Auth: $it" }
    username?.let { if (it.isNotBlank()) parts += "User: $it" }
    passwordList?.let { if (it.isNotEmpty()) parts += "Passwords: ${it.size}" }
    
    // Bot Management parameters
    uaProfiles?.let { if (it.isNotEmpty()) parts += "User-Agents: ${it.size}" }
    crawlDepth?.let { parts += "Crawl Depth: $it" }
    
    // Enumeration parameters
    enumTemplate?.let { if (it.isNotBlank()) parts += "Template: $it" }
    idRange?.let { if (it.size >= 2) parts += "Range: ${it[0]}-${it[1]}" }
    stepSize?.let { parts += "Step: $it" }
    
    // Schema validation
    fuzzCases?.let { if (it.isNotEmpty()) parts += "Fuzz Cases: ${it.size}" }
    contentTypes?.let { if (it.isNotEmpty()) parts += "Content Types: ${it.size}" }
    
    // Business logic
    replayCount?.let { parts += "Replays: $it" }
    requestDelayMs?.let { parts += "Delay: ${it}ms" }
    
    // General parameters
    queryParams?.let { if (it.isNotEmpty()) parts += "Query Params: ${it.size}" }
    headers?.let { if (it.isNotEmpty()) parts += "Headers: ${it.size}" }
    bodyTemplate?.let { if (it.isNotBlank()) parts += "Custom Body: Yes" }
    
    return parts.joinToString(" · ")
}

fun TestParameters.displayTestName(): String {
    return when {
        // DoS Protection Tests
        burstRequests != null && burstIntervalMs != null -> "HTTP Spike Attack"
        concurrentConnections != null && durationSec != null -> "Connection Flood Test"
        (concurrency != null && concurrency!! > 1) || durationSec != null -> "HTTP Load Test"
        
        // WAF Tests - Specific payload types
        payloadList?.isNotEmpty() == true -> {
            when {
                injectionPoint == InjectionPoint.PATH_PARAM -> "Path Traversal Test"
                encodingMode != null -> "Encoded Payload Test"
                bodyTemplate?.contains("{{PAYLOAD}}") == true -> "Body Injection Test"
                queryParams?.values?.any { it.contains("{{PAYLOAD}}") } == true -> "Query Parameter Injection"
                headers?.values?.any { it.contains("{{PAYLOAD}}") } == true -> "Header Injection Test"
                else -> "WAF Payload Test"
            }
        }
        
        // Bot Management Tests
        uaProfiles?.isNotEmpty() == true -> "User-Agent Anomaly Test"
        crawlDepth != null || respectRobotsTxt != null -> "Web Crawler Simulation"
        
        // API Protection Tests
        username?.isNotBlank() == true && passwordList?.isNotEmpty() == true -> "Brute Force Attack"
        enumTemplate != null || idRange != null -> "API Enumeration Test"
        fuzzCases?.isNotEmpty() == true -> "Schema Validation Test"
        replayCount != null -> "Business Logic Abuse Test"
        authMode != null || authToken != null -> "API Authentication Test"
        
        // Network Tests
        rpsTarget != null -> "Rate Limiting Test"
        
        // Custom HTTP test
        httpMethod != null && httpMethod != HttpMethod.GET -> "${httpMethod!!.name} Request Test"
        requestPath != null && requestPath != "/" -> "Custom Path Test"
        queryParams?.isNotEmpty() == true || headers?.isNotEmpty() == true -> "Custom HTTP Test"
        
        else -> "Basic Connectivity Test"
    }
}

@Parcelize
enum class TestPriority : Parcelable {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

@Parcelize
enum class PayloadSize : Parcelable {
    SMALL_64B,
    MEDIUM_512B,
    LARGE_1KB
}

@Parcelize
enum class RampUpStrategy : Parcelable {
    QUICK,
    GRADUAL
}

@Parcelize
enum class EncodingMode : Parcelable {
    URL_ENCODE,
    BASE64,
    MIXED_CASE
}

@Parcelize
enum class HttpMethod : Parcelable {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS
}

@Parcelize
enum class InjectionPoint : Parcelable {
    QUERY_PARAM,
    PATH_PARAM,
    HEADER,
    BODY
}

@Parcelize
enum class CookiePolicy : Parcelable {
    DISABLED,
    ENABLED
}

// Helper function to get relevant fields for each test type
fun getRelevantFieldsFor(testType: String): Set<String> {
    return when (testType) {
        "SqlInjection" -> setOf(
            "payloadList", "encodingMode", "injectionPoint", "targetParam",
            "httpMethod", "requestPath", "queryParams", "customHeaders", "bodyTemplate"
        )
        "XssTest" -> setOf(
            "payloadList", "encodingMode", "injectionPoint", 
            "httpMethod", "requestPath", "queryParams", "customHeaders", "bodyTemplate"
        )
        "PathTraversal" -> setOf(
            "payloadList", "injectionPoint", "httpMethod", 
            "requestPath", "queryParams", "customHeaders"
        )
        "CommandInjection" -> setOf(
            "payloadList", "encodingMode", "injectionPoint",
            "httpMethod", "requestPath", "queryParams", "customHeaders", "bodyTemplate"
        )
        "HttpSpike" -> setOf(
            "burstRequests", "burstIntervalMs", "concurrencyLevel", 
            "sustainedRpsWindow", "httpMethod", "requestPath"
        )
        "ConnectionFlood" -> setOf(
            "concurrentConnections", "durationSec", "rpsTarget", 
            "rampUpStrategy", "requestPath"
        )
        "UserAgentAnomaly" -> setOf(
            "uaProfiles", "headerMinimal", "acceptLanguage", "customHeaders"
        )
        "BruteForce" -> setOf(
            "username", "passwordList", "requestDelayMs", 
            "apiEndpoint", "httpMethod", "customHeaders", "bodyTemplate"
        )
        "AuthenticationTest" -> setOf(
            "authMode", "authToken", "apiEndpoint", 
            "httpMethod", "customHeaders", "bodyTemplate"
        )
        else -> emptySet()
    }
}

// Helper function to create default parameters for each test type
fun TestParameters.withDefaults(testType: String): TestParameters {
    return when (testType) {
        "SqlInjection" -> this.copy(
            payloadList = payloadList ?: listOf(
                "1' OR 1=1--",
                "1' UNION SELECT NULL,NULL,NULL--", 
                "'; DROP TABLE users--",
                "1' AND SLEEP(5)--",
                "1' OR '1'='1"
            ),
            encodingMode = encodingMode ?: EncodingMode.URL_ENCODE,
            injectionPoint = injectionPoint ?: InjectionPoint.QUERY_PARAM,
            targetParam = targetParam ?: "id",
            httpMethod = httpMethod ?: HttpMethod.POST,
            requestPath = requestPath ?: "/login",
            queryParams = queryParams ?: mapOf("id" to "{{PAYLOAD}}", "action" to "login"),
            customHeaders = customHeaders ?: mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "X-Forwarded-For" to "127.0.0.1"
            ),
            bodyTemplate = bodyTemplate ?: "username={{PAYLOAD}}&password=test123"
        )
        "XssTest" -> this.copy(
            payloadList = payloadList ?: listOf(
                "<script>alert('XSS')</script>",
                "<img src=x onerror=alert('XSS')>",
                "javascript:alert('XSS')",
                "<svg onload=alert('XSS')>",
                "'><script>alert('XSS')</script>"
            ),
            encodingMode = encodingMode ?: EncodingMode.URL_ENCODE,
            injectionPoint = injectionPoint ?: InjectionPoint.QUERY_PARAM,
            httpMethod = httpMethod ?: HttpMethod.POST,
            requestPath = requestPath ?: "/comment",
            queryParams = queryParams ?: mapOf("q" to "{{PAYLOAD}}", "page" to "1"),
            customHeaders = customHeaders ?: mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Referer" to "https://trusted-site.com"
            ),
            bodyTemplate = bodyTemplate ?: "comment={{PAYLOAD}}&submit=true"
        )
        "HttpSpike" -> this.copy(
            burstRequests = burstRequests ?: 100,
            burstIntervalMs = burstIntervalMs ?: 100,
            concurrencyLevel = concurrencyLevel ?: 10,
            sustainedRpsWindow = sustainedRpsWindow ?: 5,
            httpMethod = httpMethod ?: HttpMethod.GET,
            requestPath = requestPath ?: "/"
        )
        "UserAgentAnomaly" -> this.copy(
            uaProfiles = uaProfiles ?: listOf(
                "Googlebot/2.1 (+http://www.google.com/bot.html)",
                "curl/7.68.0",
                "python-requests/2.28.1", 
                "PostmanRuntime/7.29.2"
            ),
            headerMinimal = headerMinimal ?: false,
            acceptLanguage = acceptLanguage ?: "en-US,en;q=0.9",
            customHeaders = customHeaders ?: mapOf("User-Agent" to "{{PAYLOAD}}")
        )
        "BruteForce" -> this.copy(
            username = username ?: "admin",
            passwordList = passwordList ?: listOf(
                "password", "123456", "admin", 
                "Password123!", "qwerty", "letmein"
            ),
            requestDelayMs = requestDelayMs ?: 200,
            apiEndpoint = apiEndpoint ?: "/api/auth/login",
            httpMethod = httpMethod ?: HttpMethod.POST,
            customHeaders = customHeaders ?: mapOf(
                "Content-Type" to "application/json",
                "X-API-Key" to "test-key"
            ),
            bodyTemplate = bodyTemplate ?: "{\"username\": \"{{USERNAME}}\", \"password\": \"{{PAYLOAD}}\"}"
        )
        else -> this
    }
}
