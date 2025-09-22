package com.example.teost.core.data.engine

import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.text.SimpleDateFormat
import java.util.*

/**
 * PERFECT REAL network logger - Uses EXACT timestamps from when events actually occur
 * No timestamp discrepancies, everything captured at the exact moment it happens
 */
class PerfectRealEventListener : okhttp3.EventListener() {
    var dnsStart: Long = 0
    var dnsEnd: Long = 0
    var tcpStart: Long = 0
    var tcpEnd: Long = 0
    var sslStart: Long = 0
    var sslEnd: Long = 0
    var requestStart: Long = 0
    var responseStart: Long = 0
    var responseEnd: Long = 0
    var statusCode: Int = -1
    var exception: Exception? = null
    
    // Event log with EXACT timestamps from when events occur - THREAD-SAFE
    val eventLog = java.util.concurrent.CopyOnWriteArrayList<Pair<Long, String>>()
    
    /**
     * Log event with EXACT timestamp provided (not current time) - CRASH-SAFE
     */
    fun logEventAtExactTime(exactTimestamp: Long, message: String) {
        try {
            // Thread-safe add operation
            eventLog.add(Pair(exactTimestamp, message))
        } catch (e: Exception) {
            // Fallback logging - prevent crash during testing
            try {
                eventLog.add(Pair(System.currentTimeMillis(), "Log error: ${e.message}"))
            } catch (fallbackError: Exception) {
                // Ultimate fallback - do nothing to prevent crash
            }
        }
    }

    override fun dnsStart(call: Call, domainName: String) {
        dnsStart = System.currentTimeMillis()
        // Use EXACT timestamp from when DNS started
        logEventAtExactTime(dnsStart, "DNS lookup started for $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        dnsEnd = System.currentTimeMillis()
        val ips = inetAddressList.map { it.hostAddress }.joinToString(", ")
        // Use EXACT timestamp from when DNS ended
        logEventAtExactTime(dnsEnd, "DNS resolved to $ips (${dnsEnd - dnsStart}ms)")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        tcpStart = System.currentTimeMillis()
        // Use EXACT timestamp from when TCP started
        logEventAtExactTime(tcpStart, "TCP connection initiated to ${inetSocketAddress.address?.hostAddress}:${inetSocketAddress.port}")
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        tcpEnd = System.currentTimeMillis()
        // Use EXACT timestamp from when TCP ended
        logEventAtExactTime(tcpEnd, "TCP handshake completed with protocol ${protocol?.toString() ?: "unknown"} (${tcpEnd - tcpStart}ms)")
    }

    override fun secureConnectStart(call: Call) {
        sslStart = System.currentTimeMillis()
        // Use EXACT timestamp from when SSL started
        logEventAtExactTime(sslStart, "SSL handshake started")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        sslEnd = System.currentTimeMillis()
        val cipher = handshake?.cipherSuite?.javaName ?: "unknown"
        // Use EXACT timestamp from when SSL ended
        logEventAtExactTime(sslEnd, "SSL handshake completed with cipher $cipher (${sslEnd - sslStart}ms)")
    }

    override fun requestHeadersStart(call: Call) {
        requestStart = System.currentTimeMillis()
        // Use EXACT timestamp from when request started
        logEventAtExactTime(requestStart, "HTTP request headers sent")
    }
    
    override fun requestBodyStart(call: Call) {
        val bodyStartTime = System.currentTimeMillis()
        // Use EXACT timestamp from when body started
        logEventAtExactTime(bodyStartTime, "HTTP request body started")
    }
    
    override fun requestBodyEnd(call: Call, byteCount: Long) {
        val bodyEndTime = System.currentTimeMillis()
        // Use EXACT timestamp from when body ended
        logEventAtExactTime(bodyEndTime, "HTTP request body sent ($byteCount bytes)")
    }

    override fun responseHeadersStart(call: Call) {
        responseStart = System.currentTimeMillis()
        val ttfb = responseStart - requestStart
        // Use EXACT timestamp from when response started
        logEventAtExactTime(responseStart, "First byte received (TTFB: ${ttfb}ms)")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        responseEnd = System.currentTimeMillis()
        statusCode = response.code
        
        // Use EXACT timestamp from when response headers ended
        logEventAtExactTime(responseEnd, "Response headers received: ${response.code} ${response.message}")
        
        // Analyze headers IMMEDIATELY with EXACT timestamp
        analyzeHeadersRealTime(response, responseEnd)
    }
    
    override fun responseBodyStart(call: Call) {
        val bodyStartTime = System.currentTimeMillis()
        // Use EXACT timestamp from when body started
        logEventAtExactTime(bodyStartTime, "Response body started")
    }
    
    override fun responseBodyEnd(call: Call, byteCount: Long) {
        val bodyEndTime = System.currentTimeMillis()
        // Use EXACT timestamp from when body ended
        logEventAtExactTime(bodyEndTime, "Response body completed ($byteCount bytes)")
    }
    
    override fun callFailed(call: Call, ioe: IOException) {
        exception = ioe
        val failTime = System.currentTimeMillis()
        // Use EXACT timestamp from when call failed
        logEventAtExactTime(failTime, "Network call failed: ${ioe.message}")
    }
    
    override fun callStart(call: Call) {
        val startTime = System.currentTimeMillis()
        // Use EXACT timestamp from when call started
        logEventAtExactTime(startTime, "Network call started")
    }
    
    override fun connectionAcquired(call: Call, connection: okhttp3.Connection) {
        val acquiredTime = System.currentTimeMillis()
        // Use EXACT timestamp from when connection acquired
        logEventAtExactTime(acquiredTime, "Connection acquired from pool")
    }
    
    override fun connectionReleased(call: Call, connection: okhttp3.Connection) {
        val releasedTime = System.currentTimeMillis()
        // Use EXACT timestamp from when connection released
        logEventAtExactTime(releasedTime, "Connection released to pool")
    }
    
    override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
        val headersEndTime = System.currentTimeMillis()
        // Use EXACT timestamp from when request headers ended
        logEventAtExactTime(headersEndTime, "HTTP request headers completed")
    }
    
    override fun callEnd(call: Call) {
        val endTime = System.currentTimeMillis()
        // Use EXACT timestamp from when call ended
        logEventAtExactTime(endTime, "Network call completed")
    }
    
    /**
     * Analyze headers in real-time with EXACT timestamp - EXPANDED CDN DETECTION
     */
    private fun analyzeHeadersRealTime(response: Response, exactTimestamp: Long) {
        try {
            // WAF/CDN detection with EXACT timestamp from when headers were received
            response.headers.forEach { (name, value) ->
                when (name.lowercase()) {
                    // Cloudflare
                    "cf-ray" -> logEventAtExactTime(exactTimestamp, "Cloudflare detected: $name = $value")
                    "cf-cache-status" -> logEventAtExactTime(exactTimestamp, "Cloudflare cache: $value")
                    
                    // Security WAFs
                    "x-sucuri-id" -> logEventAtExactTime(exactTimestamp, "Sucuri WAF detected: $name = $value")
                    "x-akamai-request-id" -> logEventAtExactTime(exactTimestamp, "Akamai detected: $name = $value")
                    "x-amz-cf-id" -> logEventAtExactTime(exactTimestamp, "AWS CloudFront detected: $name = $value")
                    "x-azure-ref" -> logEventAtExactTime(exactTimestamp, "Azure Front Door detected: $name = $value")
                    "x-incap-request-id" -> logEventAtExactTime(exactTimestamp, "Incapsula detected: $name = $value")
                    
                    // Additional CDNs (EXPANDED DETECTION)
                    "x-served-by" -> logEventAtExactTime(exactTimestamp, "Fastly CDN detected: $name = $value")
                    "x-cache" -> {
                        if (value.contains("varnish", ignoreCase = true)) {
                            logEventAtExactTime(exactTimestamp, "Varnish cache detected: $value")
                        } else {
                            logEventAtExactTime(exactTimestamp, "Cache status: $value")
                        }
                    }
                    "via" -> {
                        when {
                            value.contains("huaweicloudcdn", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Huawei CDN detected: $value")
                            value.contains("aliyun", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Alibaba CDN detected: $value")
                            else -> logEventAtExactTime(exactTimestamp, "Proxy/CDN via: $value")
                        }
                    }
                    
                    "server" -> {
                        when {
                            value.contains("cloudflare", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Cloudflare server detected: $value")
                            value.contains("akamai", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Akamai server detected: $value")
                            value.contains("aws", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "AWS server detected: $value")
                            value.contains("tcdn", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Tencent CDN detected: $value")
                            value.contains("nginx", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Nginx server detected: $value")
                            value.contains("apache", ignoreCase = true) -> 
                                logEventAtExactTime(exactTimestamp, "Apache server detected: $value")
                            else -> logEventAtExactTime(exactTimestamp, "Server: $value")
                        }
                    }
                }
            }
            
            // Bot protection headers with EXACT timestamp
            response.header("CF-Challenge")?.let { challenge ->
                logEventAtExactTime(exactTimestamp, "Cloudflare bot challenge detected: $challenge")
            }
            
            response.header("X-Bot-Detected")?.let { botHeader ->
                logEventAtExactTime(exactTimestamp, "Bot detection header found: $botHeader")
            }
            
            // Rate limiting headers with EXACT timestamp
            response.header("X-RateLimit-Remaining")?.let { limit ->
                logEventAtExactTime(exactTimestamp, "Rate limit remaining: $limit")
            }
            
            response.header("Retry-After")?.let { retryAfter ->
                logEventAtExactTime(exactTimestamp, "Rate limited - retry after: $retryAfter seconds")
            }
            
            // Unusual response time detection with EXACT timestamp
            val responseTime = responseEnd - requestStart
            if (responseTime > 5000) {
                logEventAtExactTime(exactTimestamp, "Unusual response delay detected: ${responseTime}ms (possible bot challenge)")
            }
            
        } catch (e: Exception) {
            // CRASH-SAFE: Don't let header analysis crash the test
            logEventAtExactTime(exactTimestamp, "Header analysis error: ${e.message}")
        }
    }
    
    /**
     * Analyze response body with EXACT timestamp from when analysis starts - CRASH-SAFE
     */
    fun analyzeResponseBodyAtExactTime(responseBody: String, exactAnalysisTime: Long) {
        try {
            // Limit response body size for analysis to prevent memory issues
            val safeResponseBody = responseBody.take(50000) // Max 50KB for analysis
            val responseBodyLower = safeResponseBody.lowercase()
            
            // WAF signatures analysis with EXACT timestamp (no artificial offsets)
            val wafSignatures = mapOf(
                "cloudflare" to "Cloudflare",
                "access denied" to "Generic WAF", 
                "blocked by security policy" to "Security Policy WAF",
                "mod_security" to "ModSecurity",
                "modsecurity" to "ModSecurity",
                "web application firewall" to "Generic WAF",
                "request blocked" to "Generic WAF",
                "security violation" to "Security WAF",
                "forbidden" to "Generic WAF",
                "not authorized" to "Authorization WAF"
            )
            
            wafSignatures.forEach { (signature, waf) ->
                if (responseBodyLower.contains(signature)) {
                    // Use EXACT analysis timestamp (no artificial increments)
                    logEventAtExactTime(exactAnalysisTime, "WAF signature detected in response body: '$signature' ($waf)")
                }
            }
            
            // Bot detection signatures analysis with EXACT timestamp
            val botSignatures = listOf(
                "verify you are human",
                "challenge-platform", 
                "jschl_vc",
                "jschl_answer",
                "captcha",
                "recaptcha", 
                "hcaptcha",
                "bot detection",
                "please complete the security check",
                "checking your browser",
                "cloudflare ray id",
                "why am i seeing this page"
            )
            
            botSignatures.forEach { signature ->
                if (responseBodyLower.contains(signature)) {
                    // Use EXACT analysis timestamp (no artificial increments)
                    logEventAtExactTime(exactAnalysisTime, "Bot protection signature detected: '$signature'")
                }
            }
            
        } catch (e: Exception) {
            // CRASH-SAFE: Don't let body analysis crash the test
            logEventAtExactTime(exactAnalysisTime, "Response body analysis error: ${e.message}")
        }
    }
}

object PerfectRealLogger {
    
    /**
     * Run network test with PERFECT real event logging
     * Every timestamp is EXACTLY when the event occurred, no approximations
     */
    fun runPerfectRealNetworkTest(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null,
        testType: String = "Generic"
    ): Pair<Response?, String> {
        val listener = PerfectRealEventListener()
        
        val client = OkHttpClient.Builder()
            .eventListener(listener)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBuilder = Request.Builder()
            .url(url)
            .method(method, body)
        
        // Add headers
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // Add default User-Agent if not provided
        if (!headers.containsKey("User-Agent")) {
            requestBuilder.addHeader("User-Agent", "EdgeOne-Security-Test/1.0")
        }

        val request = requestBuilder.build()
        var responseBody = ""
        var response: Response? = null

        try {
            client.newCall(request).execute().use { resp ->
                response = resp
                
                // Capture EXACT timestamp when body reading starts
                val bodyReadStartTime = System.currentTimeMillis()
                
                // CRASH-SAFE response body reading with size limits
                responseBody = try {
                    val body = resp.body
                    if (body != null) {
                        val contentLength = body.contentLength()
                        // Limit response body size to prevent OOM (max 10MB)
                        if (contentLength > 0 && contentLength > 10 * 1024 * 1024) {
                            "Response body too large (${contentLength} bytes) - truncated for safety"
                        } else {
                            // Read response body safely (no timeout needed, OkHttp has built-in timeouts)
                            try {
                                body.string()
                            } catch (readEx: Exception) {
                                "Response body read error: ${readEx.message}"
                            }
                        }
                    } else {
                        ""
                    }
                } catch (bodyEx: Exception) {
                    "Error reading response body: ${bodyEx.message}"
                }
                
                val bodyReadEndTime = System.currentTimeMillis()
                
                // Log body reading with EXACT timestamps
                listener.logEventAtExactTime(bodyReadStartTime, "Response body reading started")
                listener.logEventAtExactTime(bodyReadEndTime, "Response body reading completed (${responseBody.length} bytes)")
                
                // Analyze response body with EXACT timestamp (no artificial offset)
                listener.analyzeResponseBodyAtExactTime(responseBody, bodyReadEndTime)
            }
        } catch (e: IOException) {
            listener.exception = e
            val errorTime = System.currentTimeMillis()
            listener.logEventAtExactTime(errorTime, "IOException occurred: ${e.message}")
        } catch (e: Exception) {
            listener.exception = e
            val errorTime = System.currentTimeMillis()
            listener.logEventAtExactTime(errorTime, "Exception occurred: ${e.message}")
        }

        // Build logs from perfect real event timeline
        val logs = buildPerfectRealLogs(listener, url, response, responseBody, testType)
        return Pair(response, logs)
    }
    
    /**
     * Build logs from perfect real event timeline - CRASH-SAFE
     * Every timestamp is exactly when the event occurred
     */
    private fun buildPerfectRealLogs(
        listener: PerfectRealEventListener, 
        url: String, 
        response: Response?, 
        responseBody: String,
        testType: String
    ): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            
            val sb = StringBuilder()
            
            // Safe string operations
            val safeUrl = url.take(500) // Limit URL length
            val safeTestType = testType.take(100) // Limit test type length
            val safeResponseBody = responseBody.take(10000) // Limit response body for display
            
            sb.appendLine("=== $safeTestType Test - PERFECT Real Network Event Timeline ===")
            sb.appendLine("Target: $safeUrl")
            sb.appendLine("Events captured with EXACT timestamps from when they occurred")
            sb.appendLine("")
            
            // CRASH-SAFE: Sort events with error handling
            try {
                val sortedEvents = listener.eventLog.sortedBy { it.first }
                
                sortedEvents.forEach { (exactTimestamp, message) ->
                    try {
                        val safeMessage = message.take(500) // Limit message length
                        sb.appendLine("[${formatter.format(Date(exactTimestamp))}] $safeMessage")
                    } catch (formatEx: Exception) {
                        sb.appendLine("[ERROR] Failed to format event: ${formatEx.message}")
                    }
                }
            } catch (sortEx: Exception) {
                sb.appendLine("Error sorting events: ${sortEx.message}")
                // Fallback: display events without sorting
                listener.eventLog.forEach { (timestamp, message) ->
                    try {
                        sb.appendLine("[${formatter.format(Date(timestamp))}] ${message.take(500)}")
                    } catch (ex: Exception) {
                        sb.appendLine("[ERROR] Event display failed")
                    }
                }
            }
            
            // Add final analysis with real data - CRASH-SAFE
            try {
                if (response != null) {
                    sb.appendLine("")
                    sb.appendLine("=== Final Analysis (All Timing Data 100% Real) ===")
                    sb.appendLine("Response Size: ${safeResponseBody.length} bytes")
                    sb.appendLine("Total Events Captured: ${listener.eventLog.size}")
                    
                    if (listener.dnsStart > 0 && listener.dnsEnd > 0 && listener.dnsEnd >= listener.dnsStart) {
                        sb.appendLine("DNS Resolution Time: ${listener.dnsEnd - listener.dnsStart}ms")
                    }
                    
                    if (listener.tcpStart > 0 && listener.tcpEnd > 0 && listener.tcpEnd >= listener.tcpStart) {
                        sb.appendLine("TCP Handshake Time: ${listener.tcpEnd - listener.tcpStart}ms")
                    }
                    
                    if (listener.sslStart > 0 && listener.sslEnd > 0 && listener.sslEnd >= listener.sslStart) {
                        sb.appendLine("SSL Handshake Time: ${listener.sslEnd - listener.sslStart}ms")
                    }
                    
                    if (listener.responseStart > 0 && listener.requestStart > 0 && listener.responseStart >= listener.requestStart) {
                        sb.appendLine("Time To First Byte: ${listener.responseStart - listener.requestStart}ms")
                    }
                    
                    if (listener.responseEnd > 0 && listener.requestStart > 0 && listener.responseEnd >= listener.requestStart) {
                        sb.appendLine("Total Response Time: ${listener.responseEnd - listener.requestStart}ms")
                    }
                }
            } catch (analysisEx: Exception) {
                sb.appendLine("Final analysis error: ${analysisEx.message}")
            }
            
            sb.toString()
            
        } catch (e: Exception) {
            // ULTIMATE CRASH-SAFE: If everything fails, return minimal safe logs
            buildString {
                appendLine("=== $testType Test - Error occurred during log generation ===")
                appendLine("Target: ${url.take(200)}")
                appendLine("[${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { 
                    timeZone = TimeZone.getTimeZone("UTC") 
                }.format(Date())}] Log generation error: ${e.message?.take(200)}")
                appendLine("Network test completed with logging errors")
                
                // Try to get basic response info safely
                response?.let { resp ->
                    try {
                        appendLine("Response code: ${resp.code}")
                    } catch (ex: Exception) {
                        appendLine("Response code: Unable to read")
                    }
                }
                
                listener.exception?.let { netEx ->
                    try {
                        appendLine("Network error: ${netEx.message?.take(200)}")
                    } catch (ex: Exception) {
                        appendLine("Network error: Unable to read")
                    }
                }
            }
        }
    }
}
