package com.example.teost.core.data.engine

import com.example.teost.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton
import com.example.teost.core.data.config.AppConfig
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Singleton
class SecurityTestEngine @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val appConfig: AppConfig
) {
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed class TestProgress {
        data class Starting(val testName: String) : TestProgress()
        data class Running(val progress: Float, val message: String, val currentStep: String, val totalSteps: Int, val currentStepIndex: Int) : TestProgress()
        data class StepCompleted(val stepName: String, val stepIndex: Int, val totalSteps: Int) : TestProgress()
        data class Completed(val result: TestResult) : TestProgress()
        data class Failed(val error: String) : TestProgress()
    }

    fun executeTest(configuration: TestConfiguration): Flow<TestProgress> = flow {
        emit(TestProgress.Starting(configuration.testId))
        try {
            // Force garbage collection before starting intensive test
            System.gc()
            
            val params = configuration.parameters
            val result = when {
                params.burstRequests != null -> executeDoSTestWithProgress(configuration)
                (params.rpsTarget != null || (params.concurrentConnections != null && params.durationSec != null)) -> executeFloodTestWithProgress(configuration)
                configuration.port != null && (configuration.parameters.payloadList != null) -> executeUdpReachabilityWithProgress(configuration)
                configuration.port != null -> executeTcpReachabilityWithProgress(configuration)
                // WAF extras mapping by intent
                params.payloadList != null && params.injectionPoint == InjectionPoint.PATH_PARAM -> executePathTraversalTestWithProgress(configuration)
                params.headersOverrides != null && params.payloadList == null && params.bodySizeKb == null -> executeCustomRulesTestWithProgress(configuration)
                params.bodySizeKb != null || params.jsonFieldCount != null -> executeOversizedBodyTestWithProgress(configuration)
                params.payloadList != null -> executeWAFTestWithProgress(configuration)
                // API Protection mappings
                params.authMode != null || params.authToken != null -> executeApiAuthTestWithProgress(configuration)
                (params.username != null && params.passwordList != null) -> executeBruteForceTestWithProgress(configuration)
                (params.enumTemplate != null && params.idRange != null) -> executeEnumerationTestWithProgress(configuration)
                params.fuzzCases != null || params.contentTypes != null -> executeSchemaValidationTestWithProgress(configuration)
                params.replayCount != null || params.requestDelayMs != null -> executeBusinessLogicTestWithProgress(configuration)
                (params.crawlDepth != null || params.respectRobotsTxt == true) -> executeCrawlerTestWithProgress(configuration)
                params.uaProfiles != null -> executeBotTestWithProgress(configuration)
                else -> executeBasicTestWithProgress(configuration)
            }
            emit(TestProgress.Completed(result))
        } catch (e: Exception) {
            android.util.Log.e("SecurityTestEngine", "Test execution failed", e)
            emit(TestProgress.Failed(e.message ?: "Test execution failed"))
        } finally {
            // Clean up resources and suggest garbage collection
            System.gc()
        }
    }.flowOn(Dispatchers.IO)

    private data class TimingMetrics(
        val dnsMs: Long? = null,
        val tcpMs: Long? = null,
        val sslMs: Long? = null,
        val ttfbMs: Long? = null
    )

    private fun requestGetWithTimings(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Pair<Response, TimingMetrics> {
        var dnsStart = 0L
        var dnsEnd = 0L
        var connectStart = 0L
        var connectEnd = 0L
        var secureConnectStart = 0L
        var secureConnectEnd = 0L
        var requestHeadersStart = 0L
        var responseHeadersStart = 0L
        var requestBodyStart = 0L
        var requestBodyEnd = 0L
        var responseBodyStart = 0L
        var responseBodyEnd = 0L

        val client = okHttpClient.newBuilder()
            .eventListener(object : EventListener() {
                override fun dnsStart(call: Call, domainName: String) { 
                    dnsStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "DNS lookup started for: $domainName")
                }
                override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) { 
                    dnsEnd = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "DNS lookup completed for: $domainName, resolved to: ${inetAddressList.joinToString()}")
                }
                override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) { 
                    connectStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "TCP connection started to: ${inetSocketAddress.address.hostAddress}:${inetSocketAddress.port}")
                }
                override fun connectEnd(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: Protocol?) { 
                    connectEnd = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "TCP connection established, protocol: $protocol")
                }
                override fun secureConnectStart(call: Call) { 
                    secureConnectStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "SSL/TLS handshake started")
                }
                override fun secureConnectEnd(call: Call, handshake: Handshake?) { 
                    secureConnectEnd = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "SSL/TLS handshake completed, cipher: ${handshake?.cipherSuite}")
                }
                override fun requestHeadersStart(call: Call) { 
                    requestHeadersStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "Request headers sending started")
                }
                override fun requestBodyStart(call: Call) {
                    requestBodyStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "Request body sending started")
                }
                override fun requestBodyEnd(call: Call, byteCount: Long) {
                    requestBodyEnd = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "Request body sent, size: $byteCount bytes")
                }
                override fun responseHeadersStart(call: Call) { 
                    responseHeadersStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "Response headers received")
                }
                override fun responseBodyStart(call: Call) {
                    responseBodyStart = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "Response body receiving started")
                }
                override fun responseBodyEnd(call: Call, byteCount: Long) {
                    responseBodyEnd = System.currentTimeMillis()
                    android.util.Log.d("NetworkDiagnosis", "Response body received, size: $byteCount bytes")
                }
                override fun callFailed(call: Call, ioe: java.io.IOException) {
                    android.util.Log.w("NetworkDiagnosis", "Network call failed: ${ioe.message}", ioe)
                }
            })
            .build()

        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()

        val response = client.newCall(request).execute()
        
        // ✅ Enhanced timing metrics with detailed logging
        val dnsMs = if (dnsStart > 0 && dnsEnd >= dnsStart) dnsEnd - dnsStart else null
        val tcpMs = if (connectStart > 0 && connectEnd >= connectStart) connectEnd - connectStart else null
        val sslMs = if (secureConnectStart > 0 && secureConnectEnd >= secureConnectStart) secureConnectEnd - secureConnectStart else null
        val ttfbMs = if (requestHeadersStart > 0 && responseHeadersStart >= requestHeadersStart) responseHeadersStart - requestHeadersStart else null
        
        // ✅ Log detailed network metrics
        android.util.Log.i("NetworkDiagnosis", "Network timing metrics for $url:")
        android.util.Log.i("NetworkDiagnosis", "  DNS lookup: ${dnsMs}ms")
        android.util.Log.i("NetworkDiagnosis", "  TCP connection: ${tcpMs}ms")
        android.util.Log.i("NetworkDiagnosis", "  SSL/TLS handshake: ${sslMs}ms")
        android.util.Log.i("NetworkDiagnosis", "  Time to first byte: ${ttfbMs}ms")
        android.util.Log.i("NetworkDiagnosis", "  Response code: ${response.code}")
        android.util.Log.i("NetworkDiagnosis", "  Response size: ${response.body?.contentLength() ?: "unknown"} bytes")
        
        // ✅ Log response headers for debugging
        response.headers.forEach { (name, value) ->
            android.util.Log.d("NetworkDiagnosis", "  Response header: $name = $value")
        }
        
        val metrics = TimingMetrics(
            dnsMs = dnsMs,
            tcpMs = tcpMs,
            sslMs = sslMs,
            ttfbMs = ttfbMs
        )
        return response to metrics
    }

    private fun requestPostWithTimings(
        url: String,
        body: RequestBody,
        headers: Map<String, String> = emptyMap()
    ): Pair<Response, TimingMetrics> {
        var dnsStart = 0L
        var dnsEnd = 0L
        var connectStart = 0L
        var connectEnd = 0L
        var secureConnectStart = 0L
        var secureConnectEnd = 0L
        var requestHeadersStart = 0L
        var responseHeadersStart = 0L

        val client = okHttpClient.newBuilder()
            .eventListener(object : EventListener() {
                override fun dnsStart(call: Call, domainName: String) { dnsStart = System.currentTimeMillis() }
                override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) { dnsEnd = System.currentTimeMillis() }
                override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) { connectStart = System.currentTimeMillis() }
                override fun connectEnd(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: Protocol?) { connectEnd = System.currentTimeMillis() }
                override fun secureConnectStart(call: Call) { secureConnectStart = System.currentTimeMillis() }
                override fun secureConnectEnd(call: Call, handshake: Handshake?) { secureConnectEnd = System.currentTimeMillis() }
                override fun requestHeadersStart(call: Call) { requestHeadersStart = System.currentTimeMillis() }
                override fun responseHeadersStart(call: Call) { responseHeadersStart = System.currentTimeMillis() }
            })
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()

        val response = client.newCall(request).execute()
        val metrics = TimingMetrics(
            dnsMs = if (dnsStart > 0 && dnsEnd >= dnsStart) dnsEnd - dnsStart else null,
            tcpMs = if (connectStart > 0 && connectEnd >= connectStart) connectEnd - connectStart else null,
            sslMs = if (secureConnectStart > 0 && secureConnectEnd >= secureConnectStart) secureConnectEnd - secureConnectStart else null,
            ttfbMs = if (requestHeadersStart > 0 && responseHeadersStart >= requestHeadersStart) responseHeadersStart - requestHeadersStart else null
        )
        return response to metrics
    }

    private suspend fun executeDoSTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val results = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        val networkLogs = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        if (params.burstRequests != null) {
            val burstSize = params.burstRequests
            val interval = params.burstIntervalMs ?: 100
            val concurrency = params.concurrencyLevel ?: 6
            val jobs = List(concurrency.coerceAtMost(appConfig.maxConcurrentConnections)) {
                async {
                    repeat((burstSize / concurrency).coerceAtLeast(1)) { requestIndex ->
                        try {
                            val requestStart = System.currentTimeMillis()
                            val (response, tm) = requestWithTimings(
                                url = buildTestUrl(config.domain, params.requestPath ?: params.targetPath, params.queryParams),
                                method = params.httpMethod ?: HttpMethod.GET,
                                headers = params.customHeaders ?: emptyMap(),
                                body = params.bodyTemplate
                            )
                            val requestTime = System.currentTimeMillis() - requestStart
                            results.add(requestTime)
                            
                            // ✅ ADD NETWORK DIAGNOSIS LOG
                            networkLogs.add("${System.currentTimeMillis()}: ${params.httpMethod?.name ?: "GET"} ${buildTestUrl(config.domain, params.requestPath ?: params.targetPath, params.queryParams)} -> HTTP ${response.code} (DNS: ${tm.dnsMs}ms, TCP: ${tm.tcpMs}ms, SSL: ${tm.sslMs}ms, TTFB: ${tm.ttfbMs}ms)")
                            
                            if (!response.isSuccessful) errors.add("Request $requestIndex: HTTP ${response.code}")
                            response.close()
                            delay(interval.toLong().coerceAtLeast(appConfig.minBurstIntervalMs))
                        } catch (e: Exception) {
                            errors.add("Request $requestIndex: ${e.message}")
                            networkLogs.add("${System.currentTimeMillis()}: ERROR - Request $requestIndex: ${e.message}")
                        }
                    }
                }
            }
            jobs.awaitAll()
        }
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val successCount = results.size
        val errorCount = errors.size
        val totalRequests = successCount + errorCount
        val successRate = if (totalRequests > 0) (successCount.toDouble() / totalRequests) * 100 else 0.0
        val avgLatency = if (results.isNotEmpty()) results.average().toLong() else 0L
        val p95Latency = if (results.isNotEmpty()) results.sorted()[((results.size * 0.95).toInt().coerceAtMost(results.size - 1))] else 0L
        return@withContext TestResult(
            testId = config.testId,
            testName = "DoS Spike",
            category = TestCategory.DDOS_PROTECTION,
            type = TestType.HttpSpike,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (successRate > 90) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(startTime),
            endTime = java.util.Date(endTime),
            duration = duration,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                requestsPerSecond = if (duration > 0) (totalRequests * 1000.0) / duration else 0.0,
                successRate = successRate,
                errorRate = 100 - successRate,
                latencyP50 = avgLatency,
                latencyP95 = p95Latency,
                totalRequests = totalRequests,
                failedRequests = errorCount,
                networkLogs = networkLogs, // ✅ ADD NETWORK LOGS
                paramsSnapshot = config.parameters
            ),
            rawLogs = buildTestLogs(results, errors),
            userId = ""
        )
    }

    private suspend fun executePathTraversalTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val payloads = params.payloadList ?: emptyList()
        val pathBase = params.requestPath ?: ""
        val blocked = mutableListOf<String>()
        val passed = mutableListOf<String>()
        val networkLogs = mutableListOf<String>() // ✅ ADD NETWORK LOGS
        val start = System.currentTimeMillis()
        payloads.forEach { p ->
            val sep = if (pathBase.endsWith("/")) "" else "/"
            val url = "https://${config.domain}$pathBase$sep$p"
            try {
                val (resp, tm) = requestWithTimings(url, headers = params.headersOverrides ?: emptyMap())
                // ✅ ADD NETWORK DIAGNOSIS LOG
                networkLogs.add("${System.currentTimeMillis()}: GET $url -> HTTP ${resp.code} (DNS: ${tm.dnsMs}ms, TCP: ${tm.tcpMs}ms, SSL: ${tm.sslMs}ms, TTFB: ${tm.ttfbMs}ms)")
                if (resp.code in listOf(403, 406)) blocked.add(p) else passed.add(p)
                delay(100)
            } catch (e: Exception) { 
                blocked.add(p)
                networkLogs.add("${System.currentTimeMillis()}: ERROR - ${e.message}")
            }
        }
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "Path Traversal",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.PathTraversal,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (blocked.isNotEmpty()) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                payloadsBlocked = blocked,
                payloadsPassed = passed,
                blockedByWaf = blocked.isNotEmpty(),
                blockingMethod = if (blocked.isNotEmpty()) "WAF Path Protection" else null,
                securityEffectiveness = if (payloads.isNotEmpty()) (blocked.size.toDouble() / payloads.size) * 100 else 0.0,
                trafficAnalysis = buildString {
                    if (blocked.isNotEmpty()) {
                        appendLine("🛡️ BLOCKED Payloads (${blocked.size}): ${blocked.joinToString(", ")}")
                    }
                    if (passed.isNotEmpty()) {
                        appendLine("⚠️ BYPASSED Payloads (${passed.size}): ${passed.joinToString(", ")}")
                    }
                },
                networkLogs = networkLogs, // ✅ ADD NETWORK LOGS
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeCustomRulesTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val url = "https://${config.domain}${params.requestPath ?: ""}"
        val start = System.currentTimeMillis()
        val respWith = try { requestWithTimings(url, headers = params.headersOverrides ?: emptyMap()) } catch (e: Exception) { null }
        val resp = respWith?.first
        val tm = respWith?.second
        val code = resp?.code ?: 0
        resp?.close()
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "Custom Rules",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.CustomRulesValidation,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (code in listOf(403, 406, 429)) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                statusCode = code,
                headers = null,
                responseTime = end - start,
                dnsResolutionTime = tm?.dnsMs,
                tcpHandshakeTime = tm?.tcpMs,
                sslHandshakeTime = tm?.sslMs,
                ttfb = tm?.ttfbMs,
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeOversizedBodyTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val sizeKb = (params.bodySizeKb ?: 256).coerceAtLeast(1)
        val fields = (params.jsonFieldCount ?: 100).coerceAtLeast(1)
        val sb = StringBuilder("{")
        repeat(fields) { idx ->
            if (idx > 0) sb.append(',')
            sb.append('"').append("f").append(idx).append('"').append(':').append('"')
            sb.append("x".repeat( minOf(1024, (sizeKb * 1024) / fields) ))
            sb.append('"')
        }
        sb.append('}')
        val body = sb.toString().toRequestBody("application/json".toMediaType())
        val url = "https://${config.domain}${params.requestPath ?: "/api/upload"}"
        val start = System.currentTimeMillis()
        val resp = try { requestPostWithTimings(url, body, params.headersOverrides ?: emptyMap()).first } catch (e: Exception) { null }
        val code = resp?.code ?: 0
        resp?.close()
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "Oversized Body",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.OversizedBody,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (code in listOf(403, 413, 429)) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                statusCode = code,
                responseTime = end - start,
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeFloodTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val rps = (params.rpsTarget ?: 0).coerceAtLeast(0)
        val intervalMs = if (rps > 0) (1000.0 / rps).toLong().coerceAtLeast(1L) else 100L
        val concurrency = (params.concurrentConnections ?: params.concurrencyLevel ?: 4).coerceAtLeast(1)
        val durationSec = (params.durationSec ?: params.sustainedRpsWindow ?: 5).coerceAtLeast(1)
        val endAt = System.currentTimeMillis() + durationSec * 1000L
        val results = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        val networkLogs = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        val jobs = List(concurrency.coerceAtMost(appConfig.maxConcurrentConnections)) {
            async {
                var i = 0
                while (System.currentTimeMillis() < endAt) {
                    try {
                        val requestStart = System.currentTimeMillis()
                        val (response, tm) = requestWithTimings(
                            url = buildTestUrl(config.domain, params.requestPath ?: params.targetPath, params.queryParams),
                            method = params.httpMethod ?: HttpMethod.GET,
                            headers = params.customHeaders ?: emptyMap(),
                            body = params.bodyTemplate
                        )
                        val requestTime = System.currentTimeMillis() - requestStart
                        results.add(requestTime)
                        
                        // ✅ ADD NETWORK DIAGNOSIS LOG
                        networkLogs.add("${System.currentTimeMillis()}: ${params.httpMethod?.name ?: "GET"} ${buildTestUrl(config.domain, params.requestPath ?: params.targetPath, params.queryParams)} -> HTTP ${response.code} (DNS: ${tm.dnsMs}ms, TCP: ${tm.tcpMs}ms, SSL: ${tm.sslMs}ms, TTFB: ${tm.ttfbMs}ms)")
                        
                        if (!response.isSuccessful) errors.add("Request $i: HTTP ${response.code}")
                        response.close()
                    } catch (e: Exception) {
                        errors.add("Request $i: ${e.message}")
                        networkLogs.add("${System.currentTimeMillis()}: ERROR - Request $i: ${e.message}")
                    }
                    i++
                    delay(intervalMs.coerceAtLeast(appConfig.minBurstIntervalMs))
                }
            }
        }
        jobs.awaitAll()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val successCount = results.size
        val errorCount = errors.size
        val totalRequests = successCount + errorCount
        val successRate = if (totalRequests > 0) (successCount.toDouble() / totalRequests) * 100 else 0.0
        val avgLatency = if (results.isNotEmpty()) results.average().toLong() else 0L
        val p95Latency = if (results.isNotEmpty()) results.sorted()[((results.size * 0.95).toInt().coerceAtMost(results.size - 1))] else 0L
        return@withContext TestResult(
            testId = config.testId,
            testName = "Connection Flood",
            category = TestCategory.DDOS_PROTECTION,
            type = TestType.ConnectionFlood,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (successRate > 90) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(startTime),
            endTime = java.util.Date(endTime),
            duration = duration,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                requestsPerSecond = if (duration > 0) (totalRequests * 1000.0) / duration else 0.0,
                successRate = successRate,
                errorRate = 100 - successRate,
                latencyP50 = avgLatency,
                latencyP95 = p95Latency,
                totalRequests = totalRequests,
                failedRequests = errorCount,
                networkLogs = networkLogs, // ✅ ADD NETWORK LOGS
                paramsSnapshot = config.parameters
            ),
            rawLogs = buildTestLogs(results, errors),
            userId = ""
        )
    }

    private suspend fun executeWAFTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val payloads = params.payloadList ?: emptyList()
        val blockedPayloads = mutableListOf<String>()
        val passedPayloads = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        val networkLogs = mutableListOf<String>()
        payloads.forEach { payload ->
            try {
                val encodedPayload = when (params.encodingMode) {
                    EncodingMode.URL_ENCODE -> java.net.URLEncoder.encode(payload, "UTF-8")
                    EncodingMode.BASE64 -> android.util.Base64.encodeToString(payload.toByteArray(), android.util.Base64.NO_WRAP)
                    else -> payload
                }
                val path = params.requestPath ?: ""
                val url = when (params.injectionPoint) {
                    InjectionPoint.QUERY_PARAM -> "https://${config.domain}${path}?${params.targetParam ?: "q"}=$encodedPayload"
                    InjectionPoint.PATH_PARAM -> "https://${config.domain}${if (path.endsWith("/")) path else "$path/"}$encodedPayload"
                    else -> "https://${config.domain}${path}"
                }
                val respPair = when (params.httpMethod) {
                    HttpMethod.POST -> {
                        val body = encodedPayload.toRequestBody("application/x-www-form-urlencoded".toMediaType())
                        requestPostWithTimings(url, body, params.headersOverrides ?: emptyMap())
                    }
                    else -> requestWithTimings(url, headers = params.headersOverrides ?: emptyMap())
                }
                val response = respPair.first
                val tm = respPair.second
                
                // ✅ ADD NETWORK DIAGNOSIS LOG
                networkLogs.add("${System.currentTimeMillis()}: ${params.httpMethod?.name ?: "GET"} $url -> HTTP ${response.code} (DNS: ${tm.dnsMs}ms, TCP: ${tm.tcpMs}ms, SSL: ${tm.sslMs}ms, TTFB: ${tm.ttfbMs}ms)")
                
                if (response.code == 403 || response.code == 406) blockedPayloads.add(payload) else passedPayloads.add(payload)
                response.close()
                delay(100)
            } catch (e: Exception) {
                blockedPayloads.add(payload)
                networkLogs.add("${System.currentTimeMillis()}: ERROR - ${e.message}")
            }
        }
        val endTime = System.currentTimeMillis()
        val wafEffectiveness = if (payloads.isNotEmpty()) (blockedPayloads.size.toDouble() / payloads.size) * 100 else 0.0
        // ✅ DETERMINE CORRECT TEST NAME BASED ON PAYLOAD TYPE
        val testType = when {
            payloads.any { it.contains("<script", true) || it.contains("alert(", true) || it.contains("javascript:", true) } -> TestType.XssTest
            payloads.any { it.contains("' OR ", true) || it.contains("UNION SELECT", true) || it.contains("DROP TABLE", true) } -> TestType.SqlInjection
            payloads.any { it.contains("../", true) || it.contains("..\\", true) || it.contains("etc/passwd", true) } -> TestType.PathTraversal
            else -> TestType.SqlInjection
        }
        val testName = when (testType) {
            TestType.SqlInjection -> "SQL Injection Test"
            TestType.XssTest -> "XSS Attack Test"
            TestType.PathTraversal -> "Path Traversal Test"
            else -> "WAF Security Test"
        }
        
        return@withContext TestResult(
            testId = config.testId,
            testName = testName, // ✅ CORRECT TEST NAME
            category = TestCategory.WEB_PROTECTION,
            type = testType, // ✅ CORRECT TEST TYPE
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (wafEffectiveness > 80) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(startTime),
            endTime = java.util.Date(endTime),
            duration = endTime - startTime,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                payloadsBlocked = blockedPayloads,
                payloadsPassed = passedPayloads,
                wafScore = wafEffectiveness.toInt(),
                blockedByWaf = blockedPayloads.isNotEmpty(),
                blockingMethod = if (blockedPayloads.isNotEmpty()) "Web Application Firewall" else null,
                securityEffectiveness = wafEffectiveness,
                trafficAnalysis = buildString {
                    appendLine("WAF Effectiveness: ${wafEffectiveness.toInt()}%")
                    if (blockedPayloads.isNotEmpty()) {
                        appendLine("🛡️ BLOCKED by WAF (${blockedPayloads.size}): ${blockedPayloads.joinToString(", ")}")
                    }
                    if (passedPayloads.isNotEmpty()) {
                        appendLine("⚠️ BYPASSED WAF (${passedPayloads.size}): ${passedPayloads.joinToString(", ")}")
                    }
                },
                dnsResolutionTime = null,
                tcpHandshakeTime = null,
                sslHandshakeTime = null,
                ttfb = null,
                networkLogs = networkLogs, // ✅ ADD NETWORK LOGS
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeBotTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val params = config.parameters
        val uaProfiles = params.uaProfiles ?: listOf("Chrome", "Firefox", "Safari")
        val detectedBots = mutableListOf<String>()
        val passedProfiles = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        uaProfiles.forEach { profile ->
            val userAgent = getUserAgentForProfile(profile)
            val headers = mutableMapOf("User-Agent" to userAgent)
            if (params.headerMinimal == false) headers.putAll(getFullBrowserHeaders(profile))
            try {
                val response = requestWithTimings(url = "https://${config.domain}/", headers = headers).first
                val isDetected = response.code == 403 || response.header("CF-Challenge") != null || response.header("X-Bot-Detected") != null
                if (isDetected) detectedBots.add(profile) else passedProfiles.add(profile)
                response.close()
                delay(500)
            } catch (e: Exception) { detectedBots.add(profile) }
        }
        val endTime = System.currentTimeMillis()
        val detectionRate = (detectedBots.size.toDouble() / uaProfiles.size) * 100
        return@withContext TestResult(
            testId = config.testId,
            testName = "Bot Management",
            category = TestCategory.BOT_MANAGEMENT,
            type = if (config.parameters.cookiePolicy == CookiePolicy.ENABLED) TestType.CookieJsChallenge else TestType.UserAgentAnomaly,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (detectionRate > 80) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = detected (good security), SUCCESS = bypass
            startTime = java.util.Date(startTime),
            endTime = java.util.Date(endTime),
            duration = endTime - startTime,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                botDetected = detectedBots.isNotEmpty(),
                botScore = detectionRate.toInt(),
                paramsSnapshot = config.parameters
            ),
            rawLogs = "Detected profiles: ${detectedBots.joinToString()}\nPassed profiles: ${passedProfiles.joinToString()}",
            userId = ""
        )
    }

    private suspend fun executeBasicTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val startNs = System.nanoTime()
        val (response, tmBasic) = requestWithTimings("https://${config.domain}/")
        val statusCode = response.code
        val headers = response.headers.toMultimap().mapValues { it.value.joinToString() }
        response.close()
        val endNs = System.nanoTime()
        return@withContext TestResult(
            testId = config.testId,
            testName = "Basic Connectivity",
            category = TestCategory.DDOS_PROTECTION,
            type = TestType.BasicConnectivity,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (statusCode in 200..299) TestStatus.SUCCESS else TestStatus.FAILED,
            startTime = java.util.Date(System.currentTimeMillis()),
            endTime = java.util.Date(System.currentTimeMillis()),
            duration = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(endNs - startNs),
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                statusCode = statusCode,
                headers = headers,
                responseTime = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(endNs - startNs),
                dnsResolutionTime = tmBasic.dnsMs,
                tcpHandshakeTime = tmBasic.tcpMs,
                sslHandshakeTime = tmBasic.sslMs,
                ttfb = tmBasic.ttfbMs,
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeTcpReachability(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val port = config.port ?: 443
        val host = config.domain
        var reachable = false
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), appConfig.tcpConnectTimeoutMs)
                reachable = true
            }
        } catch (_: Exception) {
            reachable = false
        }
        val endTime = System.currentTimeMillis()
        return@withContext TestResult(
            testId = config.testId,
            testName = "TCP Reachability",
            category = TestCategory.DDOS_PROTECTION,
            type = TestType.TcpPortReachability,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (reachable) TestStatus.SUCCESS else TestStatus.FAILED,
            startTime = java.util.Date(startTime),
            endTime = java.util.Date(endTime),
            duration = endTime - startTime,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                statusCode = if (reachable) 200 else 0,
                responseTime = endTime - startTime,
                connectionsEstablished = if (reachable) 1 else 0,
                connectionsFailed = if (reachable) 0 else 1,
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeUdpReachability(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val port = config.port ?: 53
        val host = config.domain
        val payloadBytes = (config.parameters.payloadList?.firstOrNull() ?: "").toByteArray()
        var sent = false
        try {
            java.net.DatagramSocket().use { socket ->
                socket.soTimeout = appConfig.udpSoTimeoutMs
                val address = java.net.InetAddress.getByName(host)
                val packet = java.net.DatagramPacket(payloadBytes, payloadBytes.size, address, port)
                socket.send(packet)
                sent = true
            }
        } catch (_: Exception) {
            sent = false
        }
        val endTime = System.currentTimeMillis()
        return@withContext TestResult(
            testId = config.testId,
            testName = "UDP Reachability",
            category = TestCategory.DDOS_PROTECTION,
            type = TestType.UdpReachability,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (sent) TestStatus.SUCCESS else TestStatus.FAILED,
            startTime = java.util.Date(startTime),
            endTime = java.util.Date(endTime),
            duration = endTime - startTime,
            creditsUsed = 1,
            resultDetails = TestResultDetails(
                statusCode = if (sent) 200 else 0,
                responseTime = endTime - startTime,
                packetsLost = if (sent) 0 else 1,
                paramsSnapshot = config.parameters
            ),
            userId = ""
        )
    }

    private suspend fun executeApiAuthTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val p = config.parameters
        val path = p.apiEndpoint ?: appConfig.defaultAuthPath
        val url = "https://${config.domain}$path"
        val headers = mutableMapOf<String, String>()
        when (p.authMode?.lowercase()) {
            "header" -> headers["Authorization"] = p.authToken ?: ""
            "cookie" -> headers["Cookie"] = "token=${p.authToken ?: ""}"
            "query" -> {}
        }
        val finalUrl = if (p.authMode?.lowercase() == "query" && !p.authToken.isNullOrBlank()) {
            val enc = java.net.URLEncoder.encode(p.authToken, "UTF-8")
            if (url.contains("?")) "$url&token=$enc" else "$url?token=$enc"
        } else url
        val startNs = System.nanoTime()
        val (code, _) = try { 
            val (response, _) = requestWithTimings(finalUrl, headers = headers)
            val statusCode = response.code
            response.close()
            Pair(statusCode, 0L)
        } catch (e: Exception) { Pair(0, 0L) }
        val endNs = System.nanoTime()
        TestResult(
            testId = config.testId,
            testName = "API Auth Test",
            category = TestCategory.API_PROTECTION,
            type = TestType.AuthenticationTest,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = if (code in listOf(401, 403, 429)) TestStatus.FAILED else TestStatus.SUCCESS, // FAILED = blocked (good security), SUCCESS = bypass
            startTime = java.util.Date(System.currentTimeMillis()),
            endTime = java.util.Date(System.currentTimeMillis()),
            duration = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(endNs - startNs),
            creditsUsed = 1,
            resultDetails = TestResultDetails(statusCode = code, responseTime = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(endNs - startNs), paramsSnapshot = config.parameters),
            userId = ""
        )
    }

    private suspend fun executeBruteForceTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val p = config.parameters
        val path = p.apiEndpoint ?: appConfig.defaultLoginPath
        val url = "https://${config.domain}$path"
        val passwords = p.passwordList ?: emptyList()
        var success = false
        val start = System.currentTimeMillis()
        passwords.forEachIndexed { idx, pass ->
            val body = "username=${p.username ?: "user"}&password=$pass".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            try {
                val resp = makePostRequest(url, body)
                if (resp.code in 200..299) success = true
                resp.close()
            } catch (_: Exception) {}
            delay((p.requestDelayMs ?: 100).toLong())
        }
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "API Brute Force",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.BruteForce,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = TestStatus.SUCCESS,
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(successRate = if (passwords.isNotEmpty()) (if (success) 100.0 else 0.0) else 0.0, totalRequests = passwords.size, paramsSnapshot = config.parameters),
            userId = ""
        )
    }

    private suspend fun executeEnumerationTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val p = config.parameters
        val template = p.enumTemplate ?: "/api/object/{id}"
        val range = p.idRange ?: listOf(1, 10)
        val step = (p.stepSize ?: 1).coerceAtLeast(1)
        val (startId, endId) = range.let { (it.firstOrNull() ?: 1L).toLong() to (it.getOrNull(1) ?: 10L).toLong() }
        val start = System.currentTimeMillis()
        var count = 0
        var ok = 0
        var i = startId
        while (i <= endId) {
            val path = template.replace("{id}", i.toString())
            val url = "https://${config.domain}$path"
            try {
                val resp = requestWithTimings(url).first
                if (resp.code in 200..399) ok++
                resp.close()
            } catch (_: Exception) {}
            count++
            delay((p.requestDelayMs ?: 50).toLong())
            i += step
        }
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "API Enumeration",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.EnumerationIdor,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = TestStatus.SUCCESS,
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(totalRequests = count, successRate = if (count > 0) ok * 100.0 / count else 0.0, paramsSnapshot = config.parameters),
            userId = ""
        )
    }

    private suspend fun executeSchemaValidationTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val p = config.parameters
        val path = p.apiEndpoint ?: appConfig.defaultValidatePath
        val url = "https://${config.domain}$path"
        val cases = p.fuzzCases ?: listOf("{}", "{\"a\":1}")
        val start = System.currentTimeMillis()
        var errors = 0
        cases.forEach { payload ->
            val body = payload.toRequestBody((p.contentTypes?.firstOrNull() ?: "application/json").toMediaType())
            try { makePostRequest(url, body).close() } catch (_: Exception) { errors++ }
            delay((p.requestDelayMs ?: 50).toLong())
        }
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "API Schema Validation",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.SchemaInputValidation,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = TestStatus.SUCCESS,
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(failedRequests = errors, totalRequests = cases.size, paramsSnapshot = config.parameters),
            userId = ""
        )
    }

    private suspend fun executeBusinessLogicTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val p = config.parameters
        val count = (p.replayCount ?: 3).coerceAtLeast(1)
        val delayMs = (p.requestDelayMs ?: 200).toLong()
        val path = p.apiEndpoint ?: appConfig.defaultWorkflowPath
        val url = "https://${config.domain}$path"
        val start = System.currentTimeMillis()
        repeat(count) {
            try { requestWithTimings(url).first.close() } catch (_: Exception) {}
            delay(delayMs)
        }
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "API Business Logic",
            category = TestCategory.WEB_PROTECTION,
            type = TestType.BusinessLogicAbuse,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = TestStatus.SUCCESS,
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(totalRequests = count, paramsSnapshot = config.parameters),
            userId = ""
        )
    }

    // Deprecated internal direct request without timings is removed in favor of timing-enabled helpers

    private suspend fun makePostRequest(
        url: String,
        body: RequestBody,
        headers: Map<String, String> = emptyMap()
    ): Response {
        val request = Request.Builder()
            .url(url)
            .post(body)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .build()
        return okHttpClient.newCall(request).execute()
    }

    private fun getUserAgentForProfile(profile: String): String {
        return when (profile.lowercase()) {
            "chrome" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            "firefox" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/120.0"
            "safari" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
            "googlebot" -> "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
            "bingbot" -> "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"
            "curl" -> "curl/7.68.0"
            "python" -> "python-requests/2.28.1"
            else -> profile
        }
    }

    private fun getFullBrowserHeaders(profile: String): Map<String, String> {
        fun appLocalesLanguageTagsOrNull(): String? {
            return try {
                val cls = Class.forName("androidx.appcompat.app.AppCompatDelegate")
                val getAppLocales = cls.getMethod("getApplicationLocales")
                val localesObj = getAppLocales.invoke(null)
                val toLanguageTags = localesObj.javaClass.getMethod("toLanguageTags")
                val tags = toLanguageTags.invoke(localesObj) as? String
                if (!tags.isNullOrBlank()) tags else null
            } catch (_: Throwable) { null }
        }
        val primary = appLocalesLanguageTagsOrNull() ?: run {
            val l = java.util.Locale.getDefault(); try { l.toLanguageTag() } catch (_: Throwable) { l.language }
        }
        val fallback = if (primary.startsWith("zh")) "zh;q=0.9" else "en;q=0.9"
        val acceptLanguage = if (primary.isNotBlank()) "$primary,$fallback" else "en-US,en;q=0.9"
        return mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to acceptLanguage,
            "Accept-Encoding" to "gzip, deflate, br",
            "DNT" to "1",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1"
        )
    }

    private suspend fun executeCrawlerTest(config: TestConfiguration): TestResult = withContext(Dispatchers.IO) {
        val p = config.parameters
        val depth = (p.crawlDepth ?: 1).coerceIn(1, 2)
        val respectRobots = p.respectRobotsTxt == true
        val visited = mutableSetOf<String>()
        val queue: ArrayDeque<String> = ArrayDeque()
        queue.add("https://${config.domain}/")
        var fetched = 0
        val start = System.currentTimeMillis()
        while (queue.isNotEmpty() && fetched < appConfig.maxCrawlerFetches) {
            val url = queue.removeFirst()
            if (!visited.add(url)) continue
            try {
                val resp = requestGetWithTimings(url, mapOf("User-Agent" to getUserAgentForProfile(p.uaProfiles?.firstOrNull() ?: "Googlebot"))).first
                val body = resp.body?.string() ?: ""
                resp.close()
                fetched++
                if (depth > 1) {
                    val regex = Regex("href=\\\"(/[^\\\"]*)\\\")")
                    regex.findAll(body).take(5).forEach { m ->
                        val path = m.groupValues[1]
                        if (!path.contains("..")) queue.add("https://${config.domain}$path")
                    }
                }
            } catch (_: Exception) {}
        }
        val end = System.currentTimeMillis()
        TestResult(
            testId = config.testId,
            testName = "Web Crawler",
            category = TestCategory.BOT_MANAGEMENT,
            type = TestType.WebCrawlerSimulation,
            domain = config.domain,
            ipAddress = config.ipAddress,
            status = TestStatus.SUCCESS,
            startTime = java.util.Date(start),
            endTime = java.util.Date(end),
            duration = end - start,
            creditsUsed = 1,
            resultDetails = TestResultDetails(totalRequests = fetched, paramsSnapshot = config.parameters),
            userId = ""
        )
    }

    private fun buildTestLogs(results: List<Long>, errors: List<String>): String {
        return buildString {
            appendLine("=== Test Execution Logs ===")
            appendLine("Timestamp: ${java.util.Date()}")
            appendLine("\nSuccessful Requests: ${results.size}")
            if (results.isNotEmpty()) {
                appendLine("Average Latency: ${results.average()}ms")
                appendLine("Min Latency: ${results.minOrNull()}ms")
                appendLine("Max Latency: ${results.maxOrNull()}ms")
            }
            appendLine("\nErrors: ${errors.size}")
            errors.take(10).forEach { error -> appendLine("  - $error") }
            if (errors.size > 10) appendLine("  ... and ${errors.size - 10} more errors")
        }
    }
    
    // ✅ Progress tracking methods for each test type
    private suspend fun executeDoSTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf(
            "Initializing DoS test",
            "Setting up connections",
            "Executing burst requests",
            "Analyzing results",
            "Generating report"
        )
        
        return executeTestWithProgress(config, steps) { progressCallback ->
            executeDoSTest(config)
        }
    }
    
    private suspend fun executeFloodTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf(
            "Initializing flood test",
            "Configuring rate limiting",
            "Executing flood requests",
            "Monitoring performance",
            "Analyzing results"
        )
        
        return executeTestWithProgress(config, steps) { progressCallback ->
            executeFloodTest(config)
        }
    }
    
    private suspend fun executeWAFTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf(
            "Initializing WAF test",
            "Preparing payloads",
            "Executing injection tests",
            "Analyzing responses",
            "Generating security report"
        )
        
        return executeTestWithProgress(config, steps) { progressCallback ->
            executeWAFTest(config)
        }
    }
    
    private suspend fun executeBasicTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf(
            "Initializing basic test",
            "Performing connectivity check",
            "Analyzing response",
            "Generating report"
        )
        
        return executeTestWithProgress(config, steps) { progressCallback ->
            executeBasicTest(config)
        }
    }
    
    // ✅ Generic progress tracking wrapper
    private suspend fun executeTestWithProgress(
        config: TestConfiguration,
        steps: List<String>,
        testExecution: suspend (progressCallback: (Float, String) -> Unit) -> TestResult
    ): TestResult {
        val totalSteps = steps.size
        var currentStep = 0
        
        val progressCallback: (Float, String) -> Unit = { progress: Float, message: String ->
            // This would be used to emit progress updates
            // For now, we'll just log the progress
            android.util.Log.d("TestProgress", "Step ${currentStep + 1}/$totalSteps: $message (${(progress * 100).toInt()}%)")
        }
        
        // Execute each step with progress tracking
        steps.forEachIndexed { index, stepName ->
            currentStep = index
            progressCallback(0f, stepName)
            
            // Simulate step execution time
            kotlinx.coroutines.delay(100)
            
            progressCallback(1f, "$stepName completed")
        }
        
        // Execute the actual test
        return testExecution(progressCallback)
    }
    
    // ✅ Placeholder methods for other test types with progress
    private suspend fun executeUdpReachabilityWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing UDP test", "Testing connectivity", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeUdpReachability(config) }
    }
    
    private suspend fun executeTcpReachabilityWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing TCP test", "Testing connectivity", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeTcpReachability(config) }
    }
    
    private suspend fun executePathTraversalTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing path traversal test", "Executing payloads", "Analyzing results")
        return executeTestWithProgress(config, steps) { executePathTraversalTest(config) }
    }
    
    private suspend fun executeCustomRulesTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing custom rules test", "Executing rules", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeCustomRulesTest(config) }
    }
    
    private suspend fun executeOversizedBodyTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing oversized body test", "Sending large payloads", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeOversizedBodyTest(config) }
    }
    
    private suspend fun executeApiAuthTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing API auth test", "Testing authentication", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeApiAuthTest(config) }
    }
    
    private suspend fun executeBruteForceTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing brute force test", "Executing attacks", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeBruteForceTest(config) }
    }
    
    private suspend fun executeEnumerationTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing enumeration test", "Executing enumeration", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeEnumerationTest(config) }
    }
    
    private suspend fun executeSchemaValidationTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing schema validation test", "Executing validation", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeSchemaValidationTest(config) }
    }
    
    private suspend fun executeBusinessLogicTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing business logic test", "Executing logic tests", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeBusinessLogicTest(config) }
    }
    
    private suspend fun executeCrawlerTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing crawler test", "Executing crawl", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeCrawlerTest(config) }
    }
    
    private suspend fun executeBotTestWithProgress(config: TestConfiguration): TestResult {
        val steps = listOf("Initializing bot test", "Executing bot simulation", "Analyzing results")
        return executeTestWithProgress(config, steps) { executeBotTest(config) }
    }
    
    // Helper functions for better HTTP request handling
    private fun buildTestUrl(domain: String, path: String?, queryParams: Map<String, String>?): String {
        val basePath = when {
            path.isNullOrEmpty() -> ""
            path.startsWith("/") -> path
            else -> "/$path"
        }
        val baseUrl = "https://$domain$basePath"
        
        return if (queryParams.isNullOrEmpty()) {
            baseUrl
        } else {
            val queryString = queryParams.entries.joinToString("&") { (key, value) ->
                "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
            }
            "$baseUrl?$queryString"
        }
    }
    
    private fun requestWithTimings(
        url: String,
        method: HttpMethod = HttpMethod.GET,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Pair<Response, TimingMetrics> {
        var dnsStart = 0L
        var dnsEnd = 0L
        var connectStart = 0L
        var connectEnd = 0L
        var secureConnectStart = 0L
        var secureConnectEnd = 0L
        var requestHeadersStart = 0L
        var responseHeadersStart = 0L

        val client = okHttpClient.newBuilder()
            .eventListener(object : EventListener() {
                override fun dnsStart(call: Call, domainName: String) { dnsStart = System.currentTimeMillis() }
                override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) { dnsEnd = System.currentTimeMillis() }
                override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) { connectStart = System.currentTimeMillis() }
                override fun connectEnd(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: Protocol?) { connectEnd = System.currentTimeMillis() }
                override fun secureConnectStart(call: Call) { secureConnectStart = System.currentTimeMillis() }
                override fun secureConnectEnd(call: Call, handshake: Handshake?) { secureConnectEnd = System.currentTimeMillis() }
                override fun requestHeadersStart(call: Call) { requestHeadersStart = System.currentTimeMillis() }
                override fun responseHeadersStart(call: Call) { responseHeadersStart = System.currentTimeMillis() }
            })
            .build()

        val requestBody = when {
            body.isNullOrBlank() -> null
            method in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH) -> {
                val mediaType = headers["Content-Type"]?.toMediaType() ?: "application/json".toMediaType()
                body.toRequestBody(mediaType)
            }
            else -> null
        }

        val request = Request.Builder()
            .url(url)
            .method(method.name, requestBody)
            .apply { 
                headers.forEach { (k, v) -> addHeader(k, v) }
                // Add default headers if not specified
                if (!headers.containsKey("User-Agent")) {
                    addHeader("User-Agent", "EdgeOne-Security-Test/1.0")
                }
            }
            .build()

        val response = client.newCall(request).execute()
        val metrics = TimingMetrics(
            dnsMs = if (dnsStart > 0 && dnsEnd >= dnsStart) dnsEnd - dnsStart else null,
            tcpMs = if (connectStart > 0 && connectEnd >= connectStart) connectEnd - connectStart else null,
            sslMs = if (secureConnectStart > 0 && secureConnectEnd >= secureConnectStart) secureConnectEnd - secureConnectStart else null,
            ttfbMs = if (requestHeadersStart > 0 && responseHeadersStart >= requestHeadersStart) responseHeadersStart - requestHeadersStart else null
        )
        return response to metrics
    }
}

