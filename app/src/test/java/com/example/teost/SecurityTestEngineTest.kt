package com.example.teost

import com.example.teost.data.model.*
import com.example.teost.domain.engine.SecurityTestEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit Test untuk Security Test Engine
 * Memastikan semua fungsi testing bekerja dengan benar
 */
class SecurityTestEngineTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var testEngine: SecurityTestEngine
    
    @Before
    fun setup() {
        // Setup mock server untuk testing
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
            
        testEngine = SecurityTestEngine(okHttpClient)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `test DoS protection - should detect rate limiting`() = runBlocking {
        // Setup: Server akan block setelah 5 request
        repeat(5) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200))
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(429)) // Too Many Requests
        
        val config = TestConfiguration(
            testId = "test_dos",
            domain = mockWebServer.hostName + ":" + mockWebServer.port,
            parameters = TestParameters(
                burstRequests = 6,
                burstIntervalMs = 10,
                concurrencyLevel = 1
            )
        )
        
        val results = testEngine.executeTest(config).toList()
        val finalResult = results.last()
        
        assertTrue(finalResult is SecurityTestEngine.TestProgress.Completed)
        val testResult = (finalResult as SecurityTestEngine.TestProgress.Completed).result
        
        // Verify: Should detect rate limiting
        assertNotNull(testResult.resultDetails.failedRequests)
        assertTrue(testResult.resultDetails.failedRequests!! > 0)
    }
    
    @Test
    fun `test SQL injection - WAF should block malicious payloads`() = runBlocking {
        // Setup: WAF blocks SQL injection attempts
        val wafResponse = MockResponse()
            .setResponseCode(403) // Forbidden
            .setBody("Blocked by WAF")
        
        mockWebServer.enqueue(wafResponse)
        
        val config = TestConfiguration(
            testId = "test_sqli",
            domain = mockWebServer.hostName + ":" + mockWebServer.port,
            parameters = TestParameters(
                payloadList = listOf("' OR '1'='1"),
                encodingMode = EncodingMode.URL_ENCODE,
                injectionPoint = InjectionPoint.QUERY_PARAM
            )
        )
        
        val results = testEngine.executeTest(config).toList()
        val finalResult = results.last()
        
        assertTrue(finalResult is SecurityTestEngine.TestProgress.Completed)
        val testResult = (finalResult as SecurityTestEngine.TestProgress.Completed).result
        
        // Verify: WAF should block the payload
        assertTrue(testResult.resultDetails.blockedByWaf)
        assertNotNull(testResult.resultDetails.payloadsBlocked)
        assertTrue(testResult.resultDetails.payloadsBlocked!!.isNotEmpty())
    }
    
    @Test
    fun `test XSS - should detect XSS vulnerabilities`() = runBlocking {
        // Setup: Server doesn't filter XSS (vulnerable)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<html><body><script>alert('XSS')</script></body></html>")
        )
        
        val config = TestConfiguration(
            testId = "test_xss",
            domain = mockWebServer.hostName + ":" + mockWebServer.port,
            parameters = TestParameters(
                payloadList = listOf("<script>alert('XSS')</script>"),
                encode = false,
                injectionPoint = InjectionPoint.QUERY_PARAM
            )
        )
        
        val results = testEngine.executeTest(config).toList()
        val finalResult = results.last()
        
        assertTrue(finalResult is SecurityTestEngine.TestProgress.Completed)
        val testResult = (finalResult as SecurityTestEngine.TestProgress.Completed).result
        
        // Verify: Should detect that payload passed (vulnerability exists)
        assertNotNull(testResult.resultDetails.payloadsPassed)
        assertTrue(testResult.resultDetails.payloadsPassed!!.isNotEmpty())
    }
    
    @Test
    fun `test bot detection - should identify bot traffic`() = runBlocking {
        // Setup: Server detects bots
        mockWebServer.enqueue(MockResponse().setResponseCode(200)) // Normal browser OK
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .addHeader("X-Bot-Detected", "true")
        ) // Bot blocked
        
        val config = TestConfiguration(
            testId = "test_bot",
            domain = mockWebServer.hostName + ":" + mockWebServer.port,
            parameters = TestParameters(
                uaProfiles = listOf("Chrome", "Googlebot"),
                headerMinimal = false
            )
        )
        
        val results = testEngine.executeTest(config).toList()
        val finalResult = results.last()
        
        assertTrue(finalResult is SecurityTestEngine.TestProgress.Completed)
        val testResult = (finalResult as SecurityTestEngine.TestProgress.Completed).result
        
        // Verify: Should detect bot
        assertTrue(testResult.resultDetails.botDetected)
        assertTrue(testResult.resultDetails.botScore!! > 0)
    }
    
    @Test
    fun `test connection validation - should handle timeouts`() = runBlocking {
        // Setup: Server delays response (timeout scenario)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBodyDelay(10, TimeUnit.SECONDS) // Delay longer than timeout
        )
        
        val config = TestConfiguration(
            testId = "test_timeout",
            domain = mockWebServer.hostName + ":" + mockWebServer.port,
            parameters = TestParameters(
                timeoutMs = 1000 // 1 second timeout
            )
        )
        
        val results = testEngine.executeTest(config).toList()
        val finalResult = results.last()
        
        // Verify: Should fail due to timeout
        assertTrue(
            finalResult is SecurityTestEngine.TestProgress.Failed ||
            (finalResult is SecurityTestEngine.TestProgress.Completed && 
             (finalResult as SecurityTestEngine.TestProgress.Completed).result.status == TestStatus.TIMEOUT)
        )
    }
    
    @Test
    fun `test rate limiting - should respect rate limits`() = runBlocking {
        // Setup: Server has rate limiting
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200))
        }
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "60")
        )
        
        val config = TestConfiguration(
            testId = "test_rate",
            domain = mockWebServer.hostName + ":" + mockWebServer.port,
            parameters = TestParameters(
                rpsTarget = 100,
                windowSec = 1,
                burstMode = true
            )
        )
        
        val results = testEngine.executeTest(config).toList()
        val finalResult = results.last()
        
        assertTrue(finalResult is SecurityTestEngine.TestProgress.Completed)
        val testResult = (finalResult as SecurityTestEngine.TestProgress.Completed).result
        
        // Verify: Should detect rate limiting
        assertNotNull(testResult.resultDetails.retryAfterHeader)
    }
}

/**
 * Unit Test untuk Login ViewModel
 */
class LoginViewModelTest {
    
    @Test
    fun `test email validation - empty email should fail`() {
        val email = ""
        val isValid = validateEmail(email)
        assertFalse("Empty email should be invalid", isValid)
    }
    
    @Test
    fun `test email validation - invalid format should fail`() {
        val invalidEmails = listOf(
            "notanemail",
            "@example.com",
            "user@",
            "user@.com",
            "user..name@example.com"
        )
        
        invalidEmails.forEach { email ->
            assertFalse("$email should be invalid", validateEmail(email))
        }
    }
    
    @Test
    fun `test email validation - valid email should pass`() {
        val validEmails = listOf(
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.co.uk",
            "user123@test-domain.org"
        )
        
        validEmails.forEach { email ->
            assertTrue("$email should be valid", validateEmail(email))
        }
    }
    
    @Test
    fun `test password validation - too short should fail`() {
        val password = "12345" // Less than 6 characters
        assertFalse("Short password should be invalid", validatePassword(password))
    }
    
    @Test
    fun `test password validation - valid password should pass`() {
        val password = "SecurePass123!"
        assertTrue("Valid password should pass", validatePassword(password))
    }
    
    // Helper functions
    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && 
               android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }
}

/**
 * Unit Test untuk Connection Test Repository
 */
class ConnectionTestRepositoryTest {
    
    @Test
    fun `test URL normalization`() {
        val testCases = mapOf(
            "example.com" to "https://example.com",
            "http://example.com" to "http://example.com",
            "https://example.com" to "https://example.com",
            "192.168.1.1" to "http://192.168.1.1",
            "192.168.1.1:8080" to "http://192.168.1.1:8080"
        )
        
        testCases.forEach { (input, expected) ->
            val normalized = normalizeUrl(input)
            assertEquals("URL normalization failed for $input", expected, normalized)
        }
    }
    
    @Test
    fun `test domain extraction`() {
        val testCases = mapOf(
            "https://www.example.com/path" to "www.example.com",
            "http://example.com:8080" to "example.com",
            "example.com/path?query=1" to "example.com",
            "192.168.1.1" to "192.168.1.1"
        )
        
        testCases.forEach { (input, expected) ->
            val domain = extractDomain(input)
            assertEquals("Domain extraction failed for $input", expected, domain)
        }
    }
    
    @Test
    fun `test batch parsing`() {
        val input = """
            example.com,test.com
            192.168.1.1;google.com
            yahoo.com|bing.com
            facebook.com
        """.trimIndent()
        
        val targets = parseTargets(input)
        
        assertEquals(6, targets.size)
        assertTrue(targets.contains("example.com"))
        assertTrue(targets.contains("test.com"))
        assertTrue(targets.contains("192.168.1.1"))
        assertTrue(targets.contains("google.com"))
        assertTrue(targets.contains("yahoo.com"))
        assertTrue(targets.contains("facebook.com"))
    }
    
    // Helper functions (simplified versions)
    private fun normalizeUrl(input: String): String {
        return when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?$")) -> "http://$input"
            else -> "https://$input"
        }
    }
    
    private fun extractDomain(input: String): String {
        return try {
            java.net.URL(normalizeUrl(input)).host
        } catch (e: Exception) {
            input.replace(Regex("^(https?://)?"), "")
                .replace(Regex("/.*$"), "")
                .replace(Regex(":.*$"), "")
        }
    }
    
    private fun parseTargets(input: String): List<String> {
        return input.split(Regex("[,;|\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
