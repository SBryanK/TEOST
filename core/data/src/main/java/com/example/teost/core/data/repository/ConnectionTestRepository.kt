package com.example.teost.data.repository

import com.example.teost.data.model.ConnectionTestResult
import com.example.teost.util.Resource
import com.example.teost.util.ErrorMapper
import com.example.teost.util.AppError
import com.example.teost.util.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.EventListener
import com.example.teost.core.data.remote.HttpTestService
import com.example.teost.core.domain.model.TestRequest
import com.example.teost.core.domain.model.TestResponse
import java.net.InetAddress
import java.net.URL
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionTestRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val httpTestService: HttpTestService
) {
    
    fun testConnections(input: String): Flow<Resource<List<ConnectionTestResult>>> = flow {
        try {
            emit(Resource.Loading())
            
            // Parse multiple targets using UrlValidator
            val targets = UrlValidator.parseMultipleInputs(input)
                .take(20)
            
            if (targets.isEmpty()) {
                emit(Resource.Error(AppError.Validation("No valid targets found")))
                return@flow
            }
            
            val concurrency = 3 // Reduced concurrency to prevent overwhelming device
            val chunks = targets.chunked(concurrency)
            val results = mutableListOf<ConnectionTestResult>()
            for (batch in chunks) {
                try {
                    val deferred = kotlinx.coroutines.coroutineScope {
                        batch.map { t -> async(Dispatchers.IO) { 
                            try {
                                testSingleConnection(t)
                            } catch (e: Exception) {
                                android.util.Log.w("ConnectionTest", "Failed to test $t: ${e.message}")
                                // Return error result instead of crashing
                                ConnectionTestResult(
                                    url = t,
                                    domain = try { java.net.URL(normalizeUrl(t)).host } catch (_: Exception) { t },
                                    ipAddresses = emptyList(),
                                    statusCode = 0,
                                    httpProtocol = "unknown",
                                    statusMessage = e.message ?: "Connection failed",
                                    responseTime = 0,
                                    headers = emptyMap(),
                                    requestId = java.util.UUID.randomUUID().toString(),
                                    isSuccessful = false,
                                    errorMessage = e.message,
                                    timestamp = java.util.Date(),
                                    encryptionAlgorithm = null,
                                    encryptionKey = null,
                                    encryptionIv = null,
                                    encryptedBody = null,
                                    logs = null
                                )
                            }
                        } }
                    }
                    results += deferred.awaitAll()
                    
                    // Add delay between batches to prevent overwhelming
                    if (chunks.indexOf(batch) < chunks.size - 1) {
                        delay(500)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ConnectionTest", "Batch processing failed: ${e.message}")
                    // Continue with next batch instead of crashing
                }
            }
            
            emit(Resource.Success(results.toList()))
        } catch (e: Exception) {
            val appError = ErrorMapper.toAppError(e)
            emit(Resource.Error(appError, message = appError.message ?: "Connection test failed"))
        }
    }.flowOn(Dispatchers.IO)
    
    // Parsing centralized in UrlValidator
    
    private fun isLikelyValidTarget(target: String): Boolean {
        if (target.contains(' ')) return false
        val simpleHost = Regex("^[A-Za-z0-9.-]+(:\\d+)?(/.*)?$")
        val ipv4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?$")
        return target.startsWith("http://") || target.startsWith("https://") || simpleHost.matches(target) || ipv4.matches(target)
    }
    
    private suspend fun testSingleConnection(target: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(target)
            val domain = URL(url).host
            
            val logger = com.example.teost.core.data.util.ConnectionTestLogger()
            val targetId = url
            logger.start(targetId)
            
            // DNS Resolution
            val startDns = System.currentTimeMillis()
            val ipAddresses = try {
                InetAddress.getAllByName(domain).map { it.hostAddress ?: "" }
            } catch (e: Exception) {
                emptyList()
            }
            val dnsTime = System.currentTimeMillis() - startDns
            
            // HTTP Request with EventListener timing
            var dnsStart: Long = 0
            var dnsEnd: Long = 0
            var connectStart: Long = 0
            var connectEnd: Long = 0
            var secureConnectStart: Long = 0
            var secureConnectEnd: Long = 0
            var requestHeadersStart: Long = 0
            var responseHeadersStart: Long = 0
            var ttfb: Long = 0
            var connectedHost: String? = null
            var connectedPort: Int? = null

            val timingClient = okHttpClient.newBuilder()
                .eventListener(object : EventListener() {
                    override fun callStart(call: okhttp3.Call) {
                        logger.append(targetId, "callStart")
                    }
                    override fun callEnd(call: okhttp3.Call) {
                        logger.append(targetId, "callEnd")
                    }
                    override fun dnsStart(call: okhttp3.Call, domainName: String) {
                        dnsStart = System.currentTimeMillis()
                        logger.append(targetId, "=== DNS ===")
                        logger.append(targetId, "Host: $domainName")
                    }

                    override fun dnsEnd(call: okhttp3.Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
                        dnsEnd = System.currentTimeMillis()
                        val addrs = try { inetAddressList.map { it.hostAddress ?: "" } } catch (_: Exception) { emptyList() }
                        logger.append(targetId, "Resolved to ${addrs.joinToString(", ")} in ${dnsEnd - dnsStart}ms")
                    }

                    override fun connectStart(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
                        connectStart = System.currentTimeMillis()
                        connectedHost = inetSocketAddress.hostString
                        connectedPort = inetSocketAddress.port
                        logger.append(targetId, "connectStart ${inetSocketAddress.hostString}:${inetSocketAddress.port}")
                    }

                    override fun connectEnd(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: okhttp3.Protocol?) {
                        connectEnd = System.currentTimeMillis()
                        logger.append(targetId, "connectEnd protocol=${protocol}")
                    }

                    override fun secureConnectStart(call: okhttp3.Call) {
                        secureConnectStart = System.currentTimeMillis()
                        logger.append(targetId, "=== SSL ===")
                        logger.append(targetId, "secureConnectStart")
                    }

                    override fun secureConnectEnd(call: okhttp3.Call, handshake: okhttp3.Handshake?) {
                        secureConnectEnd = System.currentTimeMillis()
                        val tlsVersion = handshake?.tlsVersion?.javaName
                        val cipher = handshake?.cipherSuite?.javaName
                        logger.append(targetId, "secureConnectEnd tls=$tlsVersion cipher=$cipher")
                        try {
                            val cert = handshake?.peerCertificates?.firstOrNull() as? java.security.cert.X509Certificate
                            cert?.let {
                                logger.append(targetId, "Issuer: ${it.issuerX500Principal.name}")
                                logger.append(targetId, "Name: ${it.subjectX500Principal.name}")
                                try {
                                    val alt = it.subjectAlternativeNames?.mapNotNull { entry ->
                                        try { entry?.let { e -> e[1]?.toString() } } catch (_: Exception) { null }
                                    }?.filterNotNull()?.joinToString(", ")
                                    if (!alt.isNullOrBlank()) logger.append(targetId, "AltName: $alt")
                                } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}
                    }
                    override fun connectionAcquired(call: okhttp3.Call, connection: okhttp3.Connection) {
                        logger.append(targetId, "connectionAcquired ${connection.route()?.address?.url?.host}:${connection.route()?.socketAddress?.port}")
                    }
                    override fun connectionReleased(call: okhttp3.Call, connection: okhttp3.Connection) {
                        logger.append(targetId, "connectionReleased")
                    }
                    override fun requestHeadersStart(call: okhttp3.Call) {
                        requestHeadersStart = System.currentTimeMillis()
                        logger.append(targetId, "=== HTTP ===")
                        logger.append(targetId, "requestHeadersStart")
                    }

                    override fun responseHeadersStart(call: okhttp3.Call) {
                        responseHeadersStart = System.currentTimeMillis()
                        ttfb = if (requestHeadersStart > 0) responseHeadersStart - requestHeadersStart else 0
                        logger.append(targetId, "responseHeadersStart ttfb=${ttfb}ms")
                    }
                })
                .build()

            val clientRequestId = java.util.UUID.randomUUID().toString()
            val traceId = (1..16).joinToString("") { java.lang.Long.toHexString((kotlin.random.Random.nextLong() ushr 4) or 0x1).takeLast(2) }.take(32)
            val spanId = (1..8).joinToString("") { java.lang.Long.toHexString((kotlin.random.Random.nextLong() ushr 4) or 0x1).takeLast(2) }.take(16)
            val traceparent = "00-${traceId}-${spanId}-01"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "EdgeOne-Security-Test/1.0")
                .header("X-Request-ID", clientRequestId)
                .header("X-Correlation-ID", clientRequestId)
                .header("traceparent", traceparent)
                .build()
            
            val startTime = System.currentTimeMillis()
            timingClient.newCall(request).execute().use { response ->
                logger.append(targetId, "request ${request.method} ${request.url}")
                val responseTime = System.currentTimeMillis() - startTime
                val headers = try {
                    response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
                } catch (_: Exception) { emptyMap<String, String>() }
                val tcpMs = if (connectStart > 0 && connectEnd >= connectStart) connectEnd - connectStart else null
                val sslMs = if (secureConnectStart > 0 && secureConnectEnd >= secureConnectStart) secureConnectEnd - secureConnectStart else null
                val dnsMs = if (dnsStart > 0 && dnsEnd >= dnsStart) dnsEnd - dnsStart else dnsTime
                logger.append(targetId, "response ${response.code} ${response.message}")
                logger.append(targetId, "Protocol: ${response.protocol}")
                connectedHost?.let { h -> logger.append(targetId, "IP: $h:${connectedPort ?: 0}") }
                logger.append(targetId, "DNS Time: ${dnsMs}ms")
                tcpMs?.let { logger.append(targetId, "Connect Time: ${it}ms") }
                sslMs?.let { logger.append(targetId, "TLS Time: ${it}ms") }
                logger.append(targetId, "Result: ${response.isSuccessful} in ${responseTime}ms")
                val bodyText = try { response.peekBody(64 * 1024).string() } catch (_: Exception) { null }
                var encAlg: String? = null
                var encKey: String? = null
                var encIv: String? = null
                var encCipher: String? = null
                if (!bodyText.isNullOrEmpty()) {
                    val enc = com.example.teost.core.data.util.ClientSideEncryptor.encryptAesGcm(bodyText.toByteArray())
                    encAlg = enc.algorithm
                    encKey = enc.keyBase64
                    encIv = enc.ivBase64
                    encCipher = enc.cipherTextBase64
                    logger.append(targetId, "encrypted body captured (len=${encCipher.length})")
                }
                val logs = logger.finish(targetId)
                ConnectionTestResult(
                    url = url,
                    domain = domain,
                    ipAddresses = ipAddresses,
                    statusCode = response.code,
                    httpProtocol = response.protocol.toString(),
                    statusMessage = response.message,
                    headers = headers,
                    responseTime = responseTime,
                    requestId = extractRequestId(response) ?: clientRequestId,
                    isSuccessful = response.isSuccessful,
                    errorMessage = if (!response.isSuccessful) "HTTP ${response.code}: ${response.message}" else null,
                    timestamp = Date(),
                    dnsTime = dnsMs,
                    tcpHandshakeTime = tcpMs,
                    sslHandshakeTime = sslMs,
                    ttfb = if (ttfb > 0) ttfb else null,
                    encryptionAlgorithm = encAlg,
                    encryptionKey = encKey,
                    encryptionIv = encIv,
                    encryptedBody = encCipher,
                    logs = logs
                )
            }
        } catch (e: java.net.UnknownHostException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "UnknownHost ${extractDomain(target)}"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "SERVER_NOT_FOUND", // Special marker for UI
                timestamp = Date(),
                logs = logs
            )
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "SSLHandshake failed"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "SSL handshake failed",
                timestamp = Date(),
                logs = logs
            )
        } catch (e: java.net.SocketTimeoutException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "Request timed out"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "Request timed out",
                timestamp = Date(),
                logs = logs
            )
        } catch (e: IllegalArgumentException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "Invalid URL"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "Invalid URL",
                timestamp = Date(),
                logs = logs
            )
        } catch (e: java.net.ConnectException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "Connection refused: ${e.message}"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "CONNECTION_REFUSED",
                timestamp = Date(),
                logs = logs
            )
        } catch (e: java.net.NoRouteToHostException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "No route to host: ${e.message}"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "NETWORK_UNREACHABLE",
                timestamp = Date(),
                logs = logs
            )
        } catch (e: java.io.IOException) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, "Network error: ${e.message}"); l.finish(tid)
            }
            val errorType = when {
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "SERVER_NOT_FOUND"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "NETWORK_UNREACHABLE" 
                e.message?.contains("Connection reset", ignoreCase = true) == true -> "CONNECTION_RESET"
                else -> "NETWORK_ERROR"
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = errorType,
                timestamp = Date(),
                logs = logs
            )
        } catch (e: Exception) {
            val logs = run {
                val l = com.example.teost.core.data.util.ConnectionTestLogger(); val tid = target; l.start(tid); l.append(tid, e.message ?: "Connection failed"); l.finish(tid)
            }
            ConnectionTestResult(
                url = target,
                domain = extractDomain(target),
                ipAddresses = emptyList(),
                statusCode = 0,
                headers = emptyMap(),
                responseTime = 0,
                requestId = null,
                isSuccessful = false,
                errorMessage = "CONNECTION_FAILED",
                timestamp = Date(),
                logs = logs
            )
        }
    }

    private fun extractRequestId(response: okhttp3.Response): String? {
        fun h(name: String): String? = response.header(name)
        // Try common variants (case-insensitive handled by OkHttp)
        val direct = h("X-Request-ID")
            ?: h("X-Request-Id")
            ?: h("Request-Id")
            ?: h("X-Correlation-Id")
            ?: h("X-Trace-Id")
            ?: h("x-amzn-RequestId")
            ?: h("x-amz-request-id")
            ?: h("CF-Ray")
            ?: h("X-Amz-Request-Id")
            ?: h("EO-LOG-UUID")
        if (direct != null && direct.isNotBlank()) return direct
        val traceparent = h("traceparent")
        if (!traceparent.isNullOrBlank()) {
            // W3C traceparent: version-traceId-spanId-flags => 00-<32 hex>-<16 hex>-<2 hex>
            val parts = traceparent.split('-')
            if (parts.size >= 4) {
                val traceId = parts[1]
                if (traceId.length == 32) return traceId
            }
        }
        return null
    }
    
    private fun normalizeUrl(input: String): String {
        return when (val validation = UrlValidator.validate(input)) {
            is UrlValidator.ValidationResult.Valid -> validation.normalized
            is UrlValidator.ValidationResult.Invalid -> {
                if (input.startsWith("http://") || input.startsWith("https://")) input
                else if (input.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?$"))) "https://$input" else "https://$input"
            }
        }
    }
    
    private fun extractDomain(input: String): String {
        return try {
            URL(normalizeUrl(input)).host
        } catch (e: Exception) {
            input.replace(Regex("^(https?://)?"), "")
                .replace(Regex("/.*$"), "")
                .replace(Regex(":.*$"), "")
        }
    }

    // Request limiting to prevent resource exhaustion
    private val requestSemaphore = Semaphore(5) // Max 5 concurrent requests
    
    // New single-test API used by the use case flow - with proper resource management
    suspend fun performTest(request: TestRequest): kotlin.Result<TestResponse> {
        return requestSemaphore.withPermit {
            performTestInternal(request)
        }
    }
    
    private suspend fun performTestInternal(request: TestRequest): kotlin.Result<TestResponse> {
        return try {
            // Check memory pressure before making request
            if (checkMemoryPressure()) {
                android.util.Log.w("ConnectionTest", "High memory pressure detected, forcing GC")
                System.gc()
                kotlinx.coroutines.delay(100) // Brief pause after GC
            }
            
            val start = System.currentTimeMillis()
            
            // Use response with proper resource management to prevent memory leaks
            val response = httpTestService.testGet(request.url)
            val responseTimeMs = System.currentTimeMillis() - start
            
            // Safely extract headers with exception handling
            val headers = try { 
                response.headers().toMultimap().mapValues { entry -> 
                    entry.value.joinToString(", ") 
                } 
            } catch (e: Exception) { 
                android.util.Log.w("ConnectionTest", "Failed to extract headers: ${e.message}")
                emptyMap() 
            }
            
            // CRITICAL FIX: Properly close response body to prevent memory leak
            val bodyText = try { 
                response.body()?.use { responseBody ->
                    // Limit body size to prevent excessive memory usage
                    val contentLength = responseBody.contentLength()
                    if (contentLength > 1024 * 1024) { // 1MB limit
                        android.util.Log.w("ConnectionTest", "Response body too large (${contentLength} bytes), truncating")
                        responseBody.byteStream().use { stream ->
                            val buffer = ByteArray(1024 * 1024)
                            val bytesRead = stream.read(buffer)
                            if (bytesRead > 0) String(buffer, 0, bytesRead) else null
                        }
                    } else {
                        responseBody.string()
                    }
                }
            } catch (e: Exception) { 
                android.util.Log.w("ConnectionTest", "Failed to read response body: ${e.message}")
                null 
            }
            
            // Encryption handling with proper error management
            var encAlg: String? = null
            var encKey: String? = null
            var encIv: String? = null
            var encCipher: String? = null
            
            if (!bodyText.isNullOrEmpty() && bodyText.length <= 64 * 1024) { // Only encrypt small bodies
                try {
                    val enc = com.example.teost.core.data.util.ClientSideEncryptor.encryptAesGcm(bodyText.toByteArray())
                    encAlg = enc.algorithm
                    encKey = enc.keyBase64
                    encIv = enc.ivBase64
                    encCipher = enc.cipherTextBase64
                } catch (e: Exception) {
                    android.util.Log.w("ConnectionTest", "Failed to encrypt response body: ${e.message}")
                }
            }
            
            val resp = TestResponse(
                id = request.id,
                url = request.url,
                responseCode = response.code(),
                responseMessage = response.message(),
                httpProtocol = response.raw().protocol.toString(),
                headers = headers,
                responseTimeMs = responseTimeMs,
                encryptionAlgorithm = encAlg,
                encryptionKey = encKey,
                encryptionIv = encIv,
                encryptedBody = encCipher
            )
            
            android.util.Log.d("ConnectionTest", "Request completed successfully: ${request.url} -> ${response.code()}")
            kotlin.Result.success(resp)
            
        } catch (e: Exception) {
            android.util.Log.w("ConnectionTest", "Request failed for ${request.url}: ${e.message}")
            
            // Enhanced error handling with specific error types
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    kotlin.Result.failure<TestResponse>(Exception("Connection timeout after 30 seconds"))
                e is java.net.SocketTimeoutException -> 
                    kotlin.Result.failure<TestResponse>(Exception("Socket timeout: ${e.message}"))
                e is java.net.ConnectException -> 
                    kotlin.Result.failure<TestResponse>(Exception("Connection refused: ${e.message}"))
                e is java.net.UnknownHostException -> 
                    kotlin.Result.failure<TestResponse>(Exception("Unknown host: ${e.message}"))
                e is java.io.IOException -> 
                    kotlin.Result.failure<TestResponse>(Exception("Network I/O error: ${e.message}"))
                e is retrofit2.HttpException -> 
                    kotlin.Result.failure<TestResponse>(Exception("HTTP error ${e.code()}: ${e.message()}"))
                else -> 
                    kotlin.Result.failure<TestResponse>(Exception("Request failed: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }
    
    private fun checkMemoryPressure(): Boolean {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val pressure = usedMemory.toDouble() / maxMemory
            
            if (pressure > 0.75) { // 75% threshold
                android.util.Log.w("ConnectionTest", "High memory pressure detected: ${(pressure * 100).toInt()}%")
                return true
            }
            false
        } catch (e: Exception) {
            android.util.Log.w("ConnectionTest", "Failed to check memory pressure: ${e.message}")
            false
        }
    }
}
