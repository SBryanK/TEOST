package com.example.teost.executor

import com.example.teost.config.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random

data class RequestLog(
    val method: String,
    val url: String,
    val statusCode: Int?,
    val durationMs: Long,
    val blocked: Boolean = false,
    val error: String? = null,
    val metadata: Map<String, String?> = emptyMap()
)

sealed class LogEvent {
    data class Info(val message: String): LogEvent()
    data class Error(val message: String): LogEvent()
    data class Request(val log: RequestLog): LogEvent()
    data class Summary(val message: String, val totals: Map<String, Any?>): LogEvent()
}

class ConfigDrivenTestExecutor(
    private val okHttpClient: OkHttpClient,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun runPlan(
        plan: TestPlan,
        onLog: (LogEvent) -> Unit
    ) = withContext(io) {
        onLog(LogEvent.Info("Starting TestPlan: ${plan.name}"))
        plan.tests.filter { it.enabled }.forEachIndexed { idx, spec ->
            onLog(LogEvent.Info("[$idx/${plan.tests.size}] ${spec.category} - ${spec.type}"))
            try {
                when (spec.category) {
                    TestCategory.DDOS_PROTECTION -> execDos(spec, onLog)
                    TestCategory.WEB_PROTECTION -> execWaf(spec, onLog)
                    TestCategory.BOT_MANAGEMENT -> execBot(spec, onLog)
                    TestCategory.API_PROTECTION -> execApi(spec, onLog)
                }
            } catch (e: Exception) {
                onLog(LogEvent.Error("Test failed: ${e.message}"))
            }
        }
        onLog(LogEvent.Summary("Plan finished", mapOf("testsExecuted" to plan.tests.count { it.enabled })))
    }

    // DoS / Network
    private suspend fun execDos(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        when (spec.type) {
            TestType.HTTP_SPIKE -> httpSpike(spec, onLog)
            TestType.IP_REGION_BLOCKING -> ipRegionBlocking(spec, onLog)
            TestType.TCP_PORT_REACHABILITY -> tcpPortReachability(spec, onLog)
            TestType.UDP_REACHABILITY -> udpReachability(spec, onLog)
            TestType.CONNECTION_FLOOD -> connectionFlood(spec, onLog)
            else -> onLog(LogEvent.Info("Unsupported DoS type: ${spec.type}"))
        }
    }

    private suspend fun httpSpike(spec: TestSpec, onLog: (LogEvent) -> Unit) = coroutineScope {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val total = spec.params.burst_requests ?: 100
        val intervalMs = spec.params.burst_interval_ms ?: 50
        val sustainedSec = spec.params.sustained_window_sec ?: 0
        val pattern = spec.params.burst_pattern ?: "linear"
        val cap = 64 // conservative cap; adjust if needed via config later
        val concurrency = max(1, minOf(total, cap))

        val dispatcher = okHttpClient.dispatcher
        dispatcher.maxRequests = max(dispatcher.maxRequests, concurrency * 2)
        dispatcher.maxRequestsPerHost = max(dispatcher.maxRequestsPerHost, concurrency)

        val sem = Semaphore(concurrency)
        if ((spec.params.concurrent_connections ?: 0) > cap) {
            onLog(LogEvent.Info("concurrent_connections capped to $cap for safety"))
        }
        val startNs = System.nanoTime()
        val jobs = (0 until total).map { i ->
            launch {
                sem.withPermit {
                    delay(computeBurstDelay(i, intervalMs, pattern))
                    val (code, duration, err) = httpGet(url, headers = mapOf("X-Test-Type" to "HTTP_SPIKE"))
                    onLog(LogEvent.Request(RequestLog(
                        method = "GET", url = url, statusCode = code, durationMs = duration, error = err
                    )))
                }
            }
        }
        if (sustainedSec > 0) delay(sustainedSec * 1000L)
        jobs.joinAll()
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        onLog(LogEvent.Summary("HTTP Spike done", mapOf("total" to total, "elapsedMs" to elapsedMs)))
    }

    private fun computeBurstDelay(index: Int, intervalMs: Int, pattern: String): Long {
        return when (pattern.lowercase()) {
            "exponential" -> (intervalMs * Math.pow(1.05, index.toDouble())).toLong().coerceAtMost(2000)
            else -> intervalMs.toLong()
        }
    }

    private suspend fun ipRegionBlocking(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        if (spec.params.use_vpn == true) {
            onLog(LogEvent.Info("Use VPN requested. Open the VPN link you configured in UI."))
        }
        // Without programmatic VPN/proxy, we cannot change device IP. We only test target reachability.
        val (code, duration, err) = httpGet(url)
        onLog(LogEvent.Request(RequestLog(
            method = "GET", url = url, statusCode = code, durationMs = duration, error = err
        )))
    }

    private suspend fun tcpPortReachability(spec: TestSpec, onLog: (LogEvent) -> Unit) = withContext(io) {
        val host = spec.target.host ?: error("host required")
        val ports = spec.params.port_list ?: spec.target.portList ?: error("port_list required")
        val timeout = (spec.params.timeout_ms ?: 2000).toInt()
        for (port in ports) {
            val t0 = System.nanoTime()
            val ok = try {
                Socket().use { s ->
                    s.connect(java.net.InetSocketAddress(host, port), timeout)
                    true
                }
            } catch (_: Exception) { false }
            val dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
            onLog(LogEvent.Request(RequestLog(
                method = "TCP_CONNECT", url = "$host:$port",
                statusCode = if (ok) 200 else null, durationMs = dur,
                error = if (ok) null else "Connect timeout"
            )))
            delay(50)
        }
    }

    private suspend fun udpReachability(spec: TestSpec, onLog: (LogEvent) -> Unit) = withContext(io) {
        val host = spec.target.host ?: error("host required")
        val port = (spec.target.portList?.firstOrNull() ?: spec.params.port_list?.firstOrNull())
            ?: error("port required")
        val payload = (spec.params.udp_payload ?: "PING").toByteArray()
        val addr = InetAddress.getByName(host)
        val socket = DatagramSocket()
        val packet = DatagramPacket(payload, payload.size, addr, port)
        val t0 = System.nanoTime()
        try {
            socket.send(packet)
            val dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
            onLog(LogEvent.Request(RequestLog("UDP_SEND", "$host:$port", 200, dur)))
        } catch (e: Exception) {
            val dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
            onLog(LogEvent.Request(RequestLog("UDP_SEND", "$host:$port", null, dur, error = e.message)))
        } finally { socket.close() }
    }

    private suspend fun connectionFlood(spec: TestSpec, onLog: (LogEvent) -> Unit) = coroutineScope {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val cap = 128
        val concurrency = max(1, minOf(spec.params.concurrent_connections ?: 50, cap))
        val connectRate = max(1, spec.params.connect_rate ?: 10)
        val windowSec = max(1, spec.params.window_sec ?: 10)
        val endAt = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(windowSec.toLong())
        val dispatcher = okHttpClient.dispatcher
        dispatcher.maxRequests = max(dispatcher.maxRequests, concurrency * 2)
        dispatcher.maxRequestsPerHost = max(dispatcher.maxRequestsPerHost, concurrency)

        val sem = Semaphore(concurrency)
        val jobs = (0 until concurrency).map {
            launch {
                while (System.nanoTime() < endAt) {
                    sem.withPermit {
                        val (code, duration, err) = httpGet(url, headers = mapOf("X-Test-Type" to "CONNECTION_FLOOD"))
                        onLog(LogEvent.Request(RequestLog("GET", url, code, duration, error = err)))
                    }
                    delay((1000L / connectRate).coerceAtLeast(1))
                }
            }
        }
        jobs.joinAll()
        onLog(LogEvent.Summary("Connection Flood done", mapOf("concurrency" to concurrency, "connectRate" to connectRate, "windowSec" to windowSec)))
    }

    // Web Protection
    private suspend fun execWaf(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        when (spec.type) {
            TestType.SQLI_XSS_SMOKE -> sqliXssSmoke(spec, onLog)
            TestType.REFLECTED_XSS -> reflectedXss(spec, onLog)
            TestType.PATH_TRAVERSAL_INJECTION_LOG4SHELL -> traversalInjection(spec, onLog)
            TestType.CUSTOM_RULES -> customRules(spec, onLog)
            TestType.EDGE_RATE_LIMITING -> edgeRateLimit(spec, onLog)
            TestType.OVERSIZED_PAYLOAD -> oversizedPayload(spec, onLog)
            else -> onLog(LogEvent.Info("Unsupported WAF type: ${spec.type}"))
        }
    }

    private suspend fun sqliXssSmoke(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val baseUrl = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val payloads = spec.params.payload_list.orEmpty()
        val enc = spec.params.encoding_mode ?: "raw"
        val injPoint = spec.params.injection_point ?: "query"
        val params = spec.params.target_params.orEmpty().ifEmpty { listOf("q") }
        for (p in payloads) {
            val payload = encodePayload(p, enc)
            when (injPoint.lowercase()) {
                "query" -> {
                    val url = appendQuery(baseUrl, params.associateWith { payload })
                    val (code, dur, err) = httpGet(url)
                    onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 403 || code == 406, error = err)))
                }
                "body" -> {
                    val body = "q=$payload".toRequestBody("application/x-www-form-urlencoded".toMediaType())
                    val (code, dur, err) = httpRequest("POST", baseUrl, body = body)
                    onLog(LogEvent.Request(RequestLog("POST", baseUrl, code, dur, blocked = code == 403 || code == 406, error = err)))
                }
                "header" -> {
                    val (code, dur, err) = httpGet(baseUrl, headers = mapOf("X-Injection" to payload))
                    onLog(LogEvent.Request(RequestLog("GET", baseUrl, code, dur, blocked = code == 403, error = err)))
                }
                "path" -> {
                    val url = baseUrl.trimEnd('/') + "/" + payload
                    val (code, dur, err) = httpGet(url)
                    onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 403, error = err)))
                }
                else -> onLog(LogEvent.Info("Unsupported injection_point=$injPoint"))
            }
            delay(100)
        }
    }

    private suspend fun reflectedXss(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val baseUrl = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val payloads = spec.params.payload_list.orEmpty()
        val enc = spec.params.encoding_mode ?: "urlencode"
        val targetParams = spec.params.target_params.orEmpty().ifEmpty { listOf("q") }
        for (p in payloads) {
            val payload = encodePayload(p, enc)
            val url = appendQuery(baseUrl, targetParams.associateWith { payload })
            val (code, dur, err) = httpGet(url, headers = spec.params.headers_overrides ?: emptyMap())
            onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = (code == 403), error = err)))
        }
    }

    private suspend fun traversalInjection(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val base = spec.target.targetUrl ?: error("targetUrl required")
        val paths = spec.params.target_paths.orEmpty()
        val payloads = spec.params.payload_list.orEmpty()
        for (path in paths) {
            for (p in payloads) {
                val url = base.trimEnd('/') + "/" + path.trimStart('/').replace("{payload}", encodePayload(p, spec.params.encoding_mode ?: "raw"))
                val (code, dur, err) = httpGet(url)
                onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 403, error = err)))
                delay(80)
            }
        }
    }

    private suspend fun customRules(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val method = (spec.params.method_override ?: "GET").uppercase()
        val headers = spec.params.headers_overrides ?: emptyMap()
        val (code, dur, err) = if (method == "GET") httpGet(url, headers) else httpRequest(method, url, headers = headers)
        onLog(LogEvent.Request(RequestLog(method, url, code, dur, blocked = code == 403, error = err)))
    }

    private suspend fun edgeRateLimit(spec: TestSpec, onLog: (LogEvent) -> Unit) = coroutineScope {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val rps = max(1, spec.params.rps_target ?: 10)
        val windowSec = max(1, spec.params.window_sec ?: 10)
        val total = rps * windowSec
        val jobs = (0 until total).map {
            launch {
                val (code, dur, err) = httpGet(url, headers = mapOf("X-Fingerprint" to (spec.params.fingerprint_mode ?: "none")))
                onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 429, error = err)))
            }
        }
        jobs.joinAll()
        onLog(LogEvent.Summary("Edge rate-limit test done", mapOf("rps" to rps, "windowSec" to windowSec, "total" to total)))
    }

    private suspend fun oversizedPayload(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val bodyKb = max(1, spec.params.body_size_kb ?: 64)
        val repeats = max(1, spec.params.field_repeats ?: 10)
        val json = buildLargeJson(repeats, bodyKb)
        val body = json.toRequestBody("application/json".toMediaType())
        val (code, dur, err) = httpRequest("POST", url, body = body)
        onLog(LogEvent.Request(RequestLog("POST", url, code, dur, blocked = code == 413 || code == 403, error = err)))
    }

    // Bot Management
    private suspend fun execBot(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        when (spec.type) {
            TestType.USER_AGENT_ANOMALY -> userAgentAnomaly(spec, onLog)
            TestType.COOKIE_JS_CHALLENGE -> cookieJsChallenge(spec, onLog)
            TestType.WEB_CRAWLER_SIM -> webCrawlerSim(spec, onLog)
            TestType.CLIENT_REPUTATION -> clientReputation(spec, onLog)
            else -> onLog(LogEvent.Info("Unsupported BOT type: ${spec.type}"))
        }
    }

    private suspend fun userAgentAnomaly(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val uas = spec.params.ua_profiles ?: listOf("curl/7.88.0", "python-requests/2.28.1", "Googlebot/2.1")
        for (ua in uas) {
            val (code, dur, err) = httpGet(url, headers = mapOf("User-Agent" to ua))
            onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 403, error = err, metadata = mapOf("ua" to ua))))
            delay(if (spec.params.humanization == true) Random.nextLong(100, 400) else 100)
        }
    }

    private suspend fun cookieJsChallenge(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val jar = if ((spec.params.cookie_policy ?: "enabled") == "enabled") JavaNetCookieJar(java.net.CookieManager()) else null
        val client = if (jar != null) okHttpClient.newBuilder().cookieJar(jar).build() else okHttpClient
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val uas = spec.params.ua_profiles ?: listOf("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "curl/7.88.0", "python-requests/2.28.1")
        for (ua in uas) {
            val (code, dur, err) = httpGet(url, headers = mapOf("User-Agent" to ua), client = client)
            onLog(
                LogEvent.Request(
                    RequestLog(
                        method = "GET",
                        url = url,
                        statusCode = code,
                        durationMs = dur,
                        blocked = code == 403 || code == 401,
                        error = err,
                        metadata = mapOf("ua" to ua)
                    )
                )
            )
            delay(if (spec.params.humanization == true) Random.nextLong(100, 400) else 80)
        }
    }

    private suspend fun webCrawlerSim(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val start = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val depth = max(1, spec.params.crawl_depth ?: 1).coerceAtMost(2)
        crawl(start, depth, onLog, human = spec.params.humanization == true)
    }

    private suspend fun clientReputation(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val regions = listOf("SG","US","EU")
        val rotateSec = max(1, spec.params.ip_rotation_sec ?: 5)
        for (region in regions) {
            val headers = mapOf("X-Forwarded-For" to randomIpForRegion(region))
            val (code, dur, err) = httpGet(url, headers)
            onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 403, error = err, metadata = mapOf("region" to region))))
            delay(rotateSec * 1000L)
        }
    }

    // API Protection
    private suspend fun execApi(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        when (spec.type) {
            TestType.CONTEXT_AWARE_RATE_LIMIT -> apiRateLimit(spec, onLog)
            TestType.AUTHENTICATION_TEST -> apiAuthTest(spec, onLog)
            TestType.BRUTE_FORCE -> apiBruteForce(spec, onLog)
            TestType.ENUMERATION_IDOR -> apiEnumeration(spec, onLog)
            TestType.SCHEMA_INPUT_VALIDATION -> apiSchemaFuzz(spec, onLog)
            TestType.BUSINESS_LOGIC_ABUSE -> apiBusinessLogic(spec, onLog)
            else -> onLog(LogEvent.Info("Unsupported API type: ${spec.type}"))
        }
    }

    private suspend fun apiRateLimit(spec: TestSpec, onLog: (LogEvent) -> Unit) = coroutineScope {
        val endpoints = spec.params.endpoint_list.orEmpty()
        val users = max(1, spec.params.parallel_users ?: 1)
        val rps = max(1, spec.params.rps_target ?: 10)
        val total = rps * max(1, spec.params.window_sec ?: 10)
        val jobs = (0 until users).map { u ->
            launch {
                repeat(total / users) {
                    for (e in endpoints) {
                        val url = buildUrl(spec, e)
                        val (code, dur, err) = httpGet(url, headers = tokenHeader(spec.params.token_list?.getOrNull(u)))
                        onLog(LogEvent.Request(RequestLog("GET", url, code, dur, blocked = code == 429, error = err)))
                    }
                    delay(1000L / rps)
                }
            }
        }
        jobs.joinAll()
    }

    private suspend fun apiAuthTest(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val endpoints = spec.params.request_endpoints.orEmpty()
        val modes = (spec.params.auth_header_mode ?: "header").lowercase()
        val tokens = spec.params.tokens ?: emptyMap()
        val tokenCases = listOf("valid","expired","malformed","missing")
        for (e in endpoints) {
            for (case in tokenCases) {
                val url = buildUrl(spec, e)
                val headers = authHeaders(modes, tokens[case], case)
                val (code, dur, err) = httpGet(url, headers)
                onLog(LogEvent.Request(RequestLog("GET", url, code, dur, error = err, metadata = mapOf("case" to case))))
                delay(120)
            }
        }
    }

    private suspend fun apiBruteForce(spec: TestSpec, onLog: (LogEvent) -> Unit) = coroutineScope {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required (auth endpoint)")
        val user = spec.params.username ?: "user@example.com"
        val pwList = spec.params.password_list.orEmpty()
        val apm = max(1, spec.params.attempts_per_minute ?: 30)
        val conc = max(1, spec.params.concurrency ?: 1)
        val sem = Semaphore(conc)
        pwList.forEach { pw ->
            launch {
                sem.withPermit {
                    val body = "username=$user&password=$pw".toRequestBody("application/x-www-form-urlencoded".toMediaType())
                    val (code, dur, err) = httpRequest("POST", url, body = body)
                    onLog(LogEvent.Request(RequestLog("POST", url, code, dur, error = err, metadata = mapOf("pw" to pw.take(3)+"***"))))
                }
            }
            delay((60_000L / apm).coerceAtLeast(50))
        }
    }

    private suspend fun apiEnumeration(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val template = spec.params.enum_template ?: error("enum_template required, like /api/object/{id}")
        val rangeList = spec.params.id_range ?: listOf(1, 10)
        val start = (rangeList.getOrNull(0) as? Number)?.toInt() ?: 1
        val end = (rangeList.getOrNull(1) as? Number)?.toInt() ?: start
        val step = ((spec.params.step_size ?: 1) as Number).toInt()
        val token = spec.params.auth_tokens?.firstOrNull()
        var id = start
        while (id <= end) {
            val endpoint = template.replace("{id}", id.toString())
            val url = buildUrl(spec, endpoint)
            val (code, dur, err) = httpGet(url, headers = tokenHeader(token))
            onLog(LogEvent.Request(RequestLog("GET", url, code, dur, error = err, metadata = mapOf("id" to id.toString()))))
            id += step
            delay(80)
        }
    }

    private suspend fun apiSchemaFuzz(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val url = spec.params.target_url ?: spec.target.targetUrl ?: error("target_url required")
        val ct = (spec.params.content_types ?: listOf("application/json")).first()
        val cases = spec.params.fuzz_cases.orEmpty().ifEmpty { listOf("null","wrong_type","oversized") }
        for (c in cases) {
            val bodyStr = when (c) {
                "null" -> "null"
                "wrong_type" -> "\"string_instead_of_object\""
                "oversized" -> buildOversizedField(spec.params.oversized_field_length ?: 4096)
                else -> "{}"
            }
            val body = bodyStr.toRequestBody(ct.toMediaType())
            val (code, dur, err) = httpRequest("POST", url, body = body, headers = mapOf("Content-Type" to ct))
            onLog(LogEvent.Request(RequestLog("POST", url, code, dur, error = err, metadata = mapOf("case" to c))))
            delay(100)
        }
    }

    private suspend fun apiBusinessLogic(spec: TestSpec, onLog: (LogEvent) -> Unit) {
        val steps = spec.params.workflow_steps.orEmpty()
        val baseDelay = spec.params.request_delay_ms ?: 150L
        val replay = max(1, spec.params.replay_count ?: 1)
        repeat(replay) {
            for (s in steps) {
                val url = buildUrl(spec, s.endpoint)
                val headers = s.headers.orEmpty()
                val body = s.bodyTemplate?.toRequestBody("application/json".toMediaType())
                val (code, dur, err) = httpRequest(s.method.uppercase(), url, headers = headers, body = body)
                onLog(LogEvent.Request(RequestLog(s.method.uppercase(), url, code, dur, error = err)))
                delay(baseDelay)
            }
        }
    }

    // Helpers
    private suspend fun httpGet(
        url: String,
        headers: Map<String, String> = emptyMap(),
        client: OkHttpClient = okHttpClient
    ): Triple<Int?, Long, String?> = httpRequest("GET", url, headers, null, client)

    private suspend fun httpRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null,
        client: OkHttpClient = okHttpClient
    ): Triple<Int?, Long, String?> {
        val reqBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
        when (method) {
            "GET" -> reqBuilder.get()
            "POST" -> reqBuilder.post(body ?: ByteArray(0).toRequestBody(null))
            "PUT" -> reqBuilder.put(body ?: ByteArray(0).toRequestBody(null))
            "DELETE" -> if (body != null) reqBuilder.delete(body) else reqBuilder.delete()
            "HEAD" -> reqBuilder.head()
            "PATCH" -> reqBuilder.patch(body ?: ByteArray(0).toRequestBody(null))
            "OPTIONS" -> reqBuilder.method("OPTIONS", null)
            else -> reqBuilder.get()
        }
        val request = reqBuilder.build()
        val t0 = System.nanoTime()
        return try {
            client.newCall(request).execute().use { resp ->
                val dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
                Triple(resp.code, dur, null)
            }
        } catch (e: Exception) {
            val dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)
            Triple(null, dur, e.message)
        }
    }

    private fun encodePayload(value: String, mode: String): String = when (mode.lowercase()) {
        "urlencode" -> java.net.URLEncoder.encode(value, "UTF-8")
        "base64" -> android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.NO_WRAP)
        "case-mix" -> value.flatMap { listOf(it.lowercaseChar(), it.uppercaseChar()) }.joinToString("")
        else -> value
    }

    private fun appendQuery(base: String, params: Map<String, String>): String {
        val sep = if (base.contains("?")) "&" else "?"
        val q = params.entries.joinToString("&") { (k, v) -> "${k}=${java.net.URLEncoder.encode(v, "UTF-8")}" }
        return base + sep + q
    }

    private fun buildLargeJson(fieldRepeats: Int, approxKb: Int): String {
        val sb = StringBuilder()
        sb.append("{")
        repeat(fieldRepeats) { idx ->
            sb.append("\"field").append(idx).append("\":\"")
            val size = (approxKb * 1024 / max(1, fieldRepeats))
            repeat(size.coerceAtMost(32_768)) { sb.append('A') }
            sb.append("\"")
            if (idx < fieldRepeats - 1) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun buildOversizedField(len: Int): String {
        val sb = StringBuilder("{\"field\":\"")
        repeat(len) { sb.append('Z') }
        sb.append("\"}")
        return sb.toString()
    }

    private fun buildUrl(spec: TestSpec, endpoint: String): String {
        val base = spec.params.target_url ?: spec.target.targetUrl
            ?: ("https://" + (spec.target.host ?: error("host or target_url required")))
        return if (endpoint.startsWith("http")) endpoint else base.trimEnd('/') + "/" + endpoint.trimStart('/')
    }

    private fun tokenHeader(token: String?): Map<String, String> =
        if (!token.isNullOrBlank()) mapOf("Authorization" to "Bearer $token") else emptyMap()

    private fun authHeaders(mode: String, token: String?, case: String): Map<String, String> {
        val adjusted = when (case) {
            "valid" -> token
            "expired" -> token?.let { "${it}_expired" }
            "malformed" -> "malformed"
            else -> null // missing
        }
        if (adjusted.isNullOrBlank()) return emptyMap()
        return when (mode.lowercase()) {
            "header" -> mapOf("Authorization" to "Bearer $adjusted")
            "cookie" -> mapOf("Cookie" to "auth=$adjusted")
            "both" -> mapOf("Authorization" to "Bearer $adjusted", "Cookie" to "auth=$adjusted")
            else -> mapOf("Authorization" to "Bearer $adjusted")
        }
    }

    private suspend fun crawl(startUrl: String, depth: Int, onLog: (LogEvent) -> Unit, human: Boolean) {
        var frontier = listOf(startUrl)
        val visited = mutableSetOf<String>()
        repeat(depth) {
            val next = mutableListOf<String>()
            for (u in frontier) {
                if (!visited.add(u)) continue
                val (code, dur, err) = httpGet(u, headers = mapOf("User-Agent" to "Mozilla/5.0 (compatible; BotSim/1.0)"))
                onLog(LogEvent.Request(RequestLog("GET", u, code, dur, error = err)))
                if (human) delay(Random.nextLong(150, 600))
                // Optional: extract links using lightweight heuristics or JSoup if added
            }
            frontier = next
        }
    }

    private fun randomIpForRegion(region: String): String {
        val base = when (region.uppercase()) { "US" -> 3; "EU" -> 45; "SG" -> 27; else -> 52 }
        return "${base}.${Random.nextInt(1,254)}.${Random.nextInt(1,254)}.${Random.nextInt(1,254)}"
    }
}


