package com.example.teost.presentation.screens.runner

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.config.Params
import com.example.teost.config.Target
import com.example.teost.config.TestPlan
import com.example.teost.config.TestSpec
import com.example.teost.config.TestCategory
import com.example.teost.config.TestType
import com.example.teost.executor.ConfigDrivenTestExecutor
import com.example.teost.executor.LogEvent
import com.example.teost.io.JsonConfigIO
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.teost.data.model.TestConfiguration
import com.example.teost.data.model.TestParameters
import com.example.teost.data.model.HttpMethod
import com.example.teost.data.model.InjectionPoint
import com.example.teost.data.model.EncodingMode
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RunnerUiState(
    val selectedTargets: List<String> = emptyList(),
    val plan: TestPlan? = null,
    val vpnLink: String = "",
    val isRunning: Boolean = false,
    val lastMessage: String = "",
    val domainLogs: Map<String, List<String>> = emptyMap(),
    val testDomainSelections: Map<Int, List<String>> = emptyMap()
)

@HiltViewModel
class ConfigRunnerViewModel @Inject constructor(
    application: Application,
    private val okHttpClient: OkHttpClient
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RunnerUiState())
    val uiState: StateFlow<RunnerUiState> = _uiState.asStateFlow()

    fun setSelectedTargets(targets: List<String>) {
        _uiState.value = _uiState.value.copy(selectedTargets = targets)
    }

    fun addTarget(target: String) {
        if (target.isBlank()) return
        val updated = _uiState.value.selectedTargets.toMutableList().apply { add(target.trim()) }.distinct()
        _uiState.value = _uiState.value.copy(selectedTargets = updated)
    }

    fun removeTarget(target: String) {
        val updated = _uiState.value.selectedTargets.filterNot { it.equals(target, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(selectedTargets = updated)
    }

    fun setVpnLink(link: String) {
        _uiState.value = _uiState.value.copy(vpnLink = link)
    }

    fun setPlan(plan: TestPlan?) {
        _uiState.value = _uiState.value.copy(plan = plan)
    }

    fun setTestDomainSelection(testIndex: Int, domains: List<String>) {
        val updated = _uiState.value.testDomainSelections.toMutableMap()
        updated[testIndex] = domains
        _uiState.value = _uiState.value.copy(testDomainSelections = updated)
    }

    fun importPlan(contentResolver: ContentResolver, uri: Uri, onError: (String) -> Unit) {
        JsonConfigIO.parse(contentResolver, uri)
            .onSuccess { setPlan(it) }
            .onFailure { onError(it.message ?: "Failed to import plan") }
    }

    fun exportPlan(contentResolver: ContentResolver, uri: Uri, onError: (String) -> Unit) {
        val current = _uiState.value.plan ?: return
        val planForExport = try {
            buildExportablePlan(current)
        } catch (e: Exception) {
            onError(e.message ?: "Plan is missing targets. Add targets or select at least one target.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    val json = JsonConfigIO.toJson(planForExport)
                    out.write(json.toByteArray())
                    out.flush()
                } ?: error("Cannot open output stream")
            }.onFailure { onError(it.message ?: "Failed to export plan") }
        }
    }

    fun persistLogsPerDomain(): Map<String, File> {
        val ctx = getApplication<Application>()
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = sdf.format(Date())
        val files = mutableMapOf<String, File>()
        _uiState.value.domainLogs.forEach { (domain, lines) ->
            val dir = File(ctx.filesDir, "logs")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${domain.replace(Regex("[^A-Za-z0-9._-]"), "_")}_$timestamp.log")
            file.writeText(lines.joinToString("\n"))
            files[domain] = file
        }
        _uiState.value = _uiState.value.copy(lastMessage = "Logs saved to app files/logs")
        return files
    }

    fun runTests(onError: (String) -> Unit) {
        val plan = _uiState.value.plan
        val targets = _uiState.value.selectedTargets
        if (plan == null) { onError("No plan loaded"); return }

        _uiState.value = _uiState.value.copy(isRunning = true, lastMessage = "Running...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selections = _uiState.value.testDomainSelections
                val executor = ConfigDrivenTestExecutor(okHttpClient)

                // Prepare domain -> list of TestSpec
                val domainToTests = mutableMapOf<String, MutableList<TestSpec>>()
                if (targets.isNotEmpty()) {
                    val singleTarget = targets.size == 1
                    plan.tests.forEachIndexed { idx, spec ->
                        val chosenDomains: List<String> = when {
                            singleTarget -> targets
                            selections[idx].isNullOrEmpty() -> targets // if user didn't choose, default to all
                            else -> selections[idx]!!.distinct()
                        }
                        chosenDomains.forEach { d ->
                            val ts = spec.copy(
                                target = ensureTargetUrl(spec.target, d),
                                params = ensureTargetUrlParam(spec.params, d)
                            )
                            domainToTests.getOrPut(d) { mutableListOf() }.add(ts)
                        }
                    }
                } else {
                    // Fallback: use targets embedded in the plan
                    plan.tests.forEach { spec ->
                        val tgt: String = spec.target.host ?: spec.target.toString()
                        val domain = try {
                            java.net.URI(tgt).host ?: tgt
                        } catch (_: Exception) {
                            val s = tgt
                            val idx = s.indexOf("://")
                            val after = if (idx >= 0) s.substring(idx + 3) else s
                            val slash = after.indexOf('/')
                            if (slash >= 0) after.substring(0, slash) else after
                        }
                        if (domain.isNotEmpty()) {
                            val ts = spec.copy(
                                target = ensureTargetUrl(spec.target, domain),
                                params = ensureTargetUrlParam(spec.params, domain)
                            )
                            domainToTests.getOrPut(domain) { mutableListOf() }.add(ts)
                        }
                    }
                }

                val newLogs = mutableMapOf<String, MutableList<String>>()
                domainToTests.forEach { (domain, tests) ->
                    val domainPlan = TestPlan(name = "${plan.name} [$domain]", description = plan.description, tests = tests)
                    newLogs.getOrPut(domain) { mutableListOf("=== Start domain: $domain ===") }
                    executor.runPlan(domainPlan) { ev ->
                        when (ev) {
                            is LogEvent.Info -> newLogs.getOrPut(domain) { mutableListOf() }.add("INFO: ${ev.message}")
                            is LogEvent.Error -> newLogs.getOrPut(domain) { mutableListOf() }.add("ERROR: ${ev.message}")
                            is LogEvent.Request -> {
                                val l = ev.log
                                newLogs.getOrPut(domain) { mutableListOf() }.add(
                                    "REQ ${l.method} ${l.url} -> ${l.statusCode} in ${l.durationMs}ms block=${l.blocked} err=${l.error}"
                                )
                            }
                            is LogEvent.Summary -> newLogs.getOrPut(domain) { mutableListOf() }.add("SUMMARY: ${ev.message} ${ev.totals}")
                        }
                    }
                    newLogs.getOrPut(domain) { mutableListOf() }.add("=== End domain: $domain ===")
                }

                val immutableLogs = newLogs.mapValues { it.value.toList() }
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    lastMessage = "Completed",
                    domainLogs = immutableLogs
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRunning = false, lastMessage = e.message ?: "Failed")
                onError(e.message ?: "Failed to run tests")
            }
        }
    }

    /**
     * Generate TestConfiguration items for the Test Wizard cart from the currently loaded plan
     * and selected targets/domain selections. This does not mutate any global state.
     */
    fun generateCartConfigurations(onError: (String) -> Unit): List<TestConfiguration> {
        val plan = _uiState.value.plan
        val targets = _uiState.value.selectedTargets
        if (plan == null) { onError("No plan loaded"); return emptyList() }
        if (targets.isEmpty()) { onError("Please select at least one target"); return emptyList() }

        val selections = _uiState.value.testDomainSelections
        val singleTarget = targets.size == 1
        val result = mutableListOf<TestConfiguration>()
        plan.tests.forEachIndexed { idx, spec ->
            val chosenDomains: List<String> = when {
                singleTarget -> targets
                selections[idx].isNullOrEmpty() -> targets
                else -> selections[idx]!!.distinct()
            }
            chosenDomains.forEach { domainOrUrl ->
                result += mapSpecToConfigs(spec, domainOrUrl)
            }
        }
        return result
    }

    private fun mapSpecToConfigs(spec: TestSpec, domainOrUrl: String): List<TestConfiguration> {
        val host = hostFrom(domainOrUrl)
        val pathFromTargetUrl = pathFromUrl(spec.params.target_url)
        fun cfg(params: TestParameters, port: Int? = null): TestConfiguration =
            TestConfiguration(
                testId = java.util.UUID.randomUUID().toString(),
                domain = host,
                port = port,
                parameters = params
            )

        return when (spec.category) {
            TestCategory.DDOS_PROTECTION -> when (spec.type) {
                TestType.HTTP_SPIKE -> listOf(
                    cfg(
                        TestParameters(
                            burstRequests = spec.params.burst_requests,
                            burstIntervalMs = spec.params.burst_interval_ms,
                            sustainedRpsWindow = spec.params.sustained_window_sec,
                            concurrencyLevel = spec.params.concurrent_connections,
                            targetPath = pathFromTargetUrl,
                            headersOverrides = spec.params.headers_overrides,
                            timeoutMs = spec.params.timeout_ms
                        )
                    )
                )
                TestType.CONNECTION_FLOOD -> listOf(
                    cfg(
                        TestParameters(
                            // Trigger flood path: rpsTarget or (concurrentConnections + durationSec)
                            rpsTarget = spec.params.connect_rate,
                            durationSec = spec.params.window_sec,
                            concurrentConnections = spec.params.concurrent_connections,
                            targetPath = pathFromTargetUrl
                        )
                    )
                )
                TestType.TCP_PORT_REACHABILITY -> {
                    val ports = spec.params.port_list ?: spec.target.portList.orEmpty()
                    if (ports.isEmpty()) emptyList() else ports.map { p -> cfg(TestParameters(), port = p) }
                }
                TestType.UDP_REACHABILITY -> {
                    val ports = spec.params.port_list ?: spec.target.portList.orEmpty()
                    val udpPayload = spec.params.udp_payload ?: "PING"
                    if (ports.isEmpty()) emptyList() else ports.map { p ->
                        cfg(TestParameters(payloadList = listOf(udpPayload)), port = p)
                    }
                }
                TestType.IP_REGION_BLOCKING -> listOf(
                    cfg(TestParameters(targetPath = pathFromTargetUrl))
                )
                else -> emptyList()
            }
            TestCategory.WEB_PROTECTION -> when (spec.type) {
                TestType.SQLI_XSS_SMOKE, TestType.REFLECTED_XSS -> listOf(
                    cfg(
                        TestParameters(
                            payloadList = spec.params.payload_list,
                            encodingMode = when (spec.params.encoding_mode?.lowercase()) {
                                "urlencode" -> EncodingMode.URL_ENCODE
                                "base64" -> EncodingMode.BASE64
                                "case-mix" -> EncodingMode.MIXED_CASE
                                else -> null
                            },
                            injectionPoint = when (spec.params.injection_point?.lowercase()) {
                                "query" -> InjectionPoint.QUERY_PARAM
                                "path" -> InjectionPoint.PATH_PARAM
                                "header" -> InjectionPoint.HEADER
                                "body" -> InjectionPoint.BODY
                                else -> InjectionPoint.QUERY_PARAM
                            },
                            targetParam = spec.params.target_params?.firstOrNull() ?: "q",
                            requestPath = pathFromTargetUrl,
                            headersOverrides = spec.params.headers_overrides
                        )
                    )
                )
                TestType.PATH_TRAVERSAL_INJECTION_LOG4SHELL -> listOf(
                    cfg(
                        TestParameters(
                            payloadList = spec.params.payload_list,
                            injectionPoint = InjectionPoint.PATH_PARAM,
                            requestPath = if (!pathFromTargetUrl.isNullOrBlank()) pathFromTargetUrl else "/download"
                        )
                    )
                )
                TestType.CUSTOM_RULES -> listOf(
                    cfg(
                        TestParameters(
                            headersOverrides = spec.params.headers_overrides,
                            requestPath = if (!pathFromTargetUrl.isNullOrBlank()) pathFromTargetUrl else "/",
                            httpMethod = when ((spec.params.method_override ?: "GET").uppercase()) {
                                "POST" -> HttpMethod.POST
                                "PUT" -> HttpMethod.PUT
                                "DELETE" -> HttpMethod.DELETE
                                "PATCH" -> HttpMethod.PATCH
                                "HEAD" -> HttpMethod.HEAD
                                "OPTIONS" -> HttpMethod.OPTIONS
                                else -> HttpMethod.GET
                            }
                        )
                    )
                )
                TestType.EDGE_RATE_LIMITING -> emptyList() // Unsupported in wizard; skip
                TestType.OVERSIZED_PAYLOAD -> listOf(
                    cfg(
                        TestParameters(
                            bodySizeKb = spec.params.body_size_kb,
                            jsonFieldCount = spec.params.field_repeats,
                            requestPath = if (!pathFromTargetUrl.isNullOrBlank()) pathFromTargetUrl else "/api/upload",
                            httpMethod = HttpMethod.POST
                        )
                    )
                )
                else -> emptyList()
            }
            TestCategory.BOT_MANAGEMENT -> when (spec.type) {
                TestType.USER_AGENT_ANOMALY -> listOf(
                    cfg(
                        TestParameters(
                            uaProfiles = spec.params.ua_profiles,
                            respectRobotsTxt = null,
                            crawlDepth = null,
                            acceptLanguage = null
                        )
                    )
                )
                TestType.COOKIE_JS_CHALLENGE -> listOf(
                    cfg(
                        TestParameters(
                            cookiePolicy = if ((spec.params.cookie_policy ?: "enabled") == "enabled") com.example.teost.data.model.CookiePolicy.ENABLED else com.example.teost.data.model.CookiePolicy.DISABLED,
                            jsRuntimeMode = spec.params.js_exec_mode
                        )
                    )
                )
                TestType.WEB_CRAWLER_SIM -> listOf(
                    cfg(
                        TestParameters(
                            uaProfiles = spec.params.ua_profiles,
                            crawlDepth = spec.params.crawl_depth,
                            respectRobotsTxt = false
                        )
                    )
                )
                else -> emptyList()
            }
            TestCategory.API_PROTECTION -> when (spec.type) {
                TestType.CONTEXT_AWARE_RATE_LIMIT -> listOf(
                    cfg(
                        TestParameters(
                            rpsTarget = spec.params.rps_target,
                            durationSec = spec.params.window_sec,
                            targetPath = firstEndpointPath(spec.params.endpoint_list)
                        )
                    )
                )
                TestType.AUTHENTICATION_TEST -> listOf(
                    cfg(
                        TestParameters(
                            authMode = spec.params.auth_header_mode,
                            authToken = spec.params.tokens?.get("valid"),
                            apiEndpoint = spec.params.request_endpoints?.firstOrNull()
                        )
                    )
                )
                TestType.BRUTE_FORCE -> listOf(
                    cfg(
                        TestParameters(
                            username = spec.params.username,
                            passwordList = spec.params.password_list,
                            apiEndpoint = if (!pathFromTargetUrl.isNullOrBlank()) pathFromTargetUrl else "/auth/login"
                        )
                    )
                )
                TestType.ENUMERATION_IDOR -> listOf(
                    cfg(
                        TestParameters(
                            enumTemplate = spec.params.enum_template,
                            idRange = spec.params.id_range,
                            stepSize = spec.params.step_size?.toInt()
                        )
                    )
                )
                TestType.SCHEMA_INPUT_VALIDATION -> listOf(
                    cfg(
                        TestParameters(
                            fuzzCases = spec.params.fuzz_cases,
                            contentTypes = spec.params.content_types
                        )
                    )
                )
                TestType.BUSINESS_LOGIC_ABUSE -> listOf(
                    cfg(
                        TestParameters(
                            replayCount = spec.params.replay_count,
                            requestDelayMs = spec.params.request_delay_ms?.toInt()
                        )
                    )
                )
                else -> emptyList()
            }
        }
    }

    private fun hostFrom(input: String): String {
        return try {
            val uri = URI(if (input.contains("://")) input else "https://$input")
            val host = uri.host ?: input
            val port = if (uri.port > 0) ":${uri.port}" else ""
            (host + port).trim()
        } catch (_: Exception) {
            input.replace(Regex("^https?://"), "").trimEnd('/')
        }
    }

    private fun pathFromUrl(url: String?): String {
        if (url.isNullOrBlank()) return "/"
        return try {
            val uri = URI(url)
            val p = uri.path
            if (p.isNullOrBlank()) "/" else p
        } catch (_: Exception) { "/" }
    }

    private fun firstEndpointPath(list: List<String>?): String? = list?.firstOrNull()?.let {
        try {
            val uri = URI(if (it.startsWith("http")) it else "https://dummy$it")
            uri.path
        } catch (_: Exception) { it }
    }

    /**
     * Ensure every TestSpec in a plan has a concrete target (either targetUrl or host).
     * If the current UI has selectedTargets, we fan-out specs across those and normalize targetUrl.
     * Otherwise, we validate existing targets in the plan and throw if missing.
     */
    private fun buildExportablePlan(current: TestPlan): TestPlan {
        val chosen = _uiState.value.selectedTargets
        if (chosen.isNotEmpty()) {
            val normalized = current.tests.flatMap { spec ->
                chosen.map { d ->
                    spec.copy(
                        target = ensureTargetUrl(spec.target, d),
                        params = ensureTargetUrlParam(spec.params, d)
                    )
                }
            }
            return current.copy(tests = normalized)
        }

        // No UI targets selected; validate embedded targets
        current.tests.forEach { spec ->
            val url = spec.params.target_url ?: spec.target.targetUrl
            val host = spec.target.host
            if (url.isNullOrBlank() && host.isNullOrBlank()) {
                error("Spec ${spec.type} in category ${spec.category} has no target. Add targets or set targetUrl/host in the plan.")
            }
        }
        return current
    }

    private fun ensureTargetUrl(target: Target, domainOrUrl: String): Target {
        // If domainOrUrl is a URL, use directly; else construct https://domain
        val url = if (domainOrUrl.startsWith("http://") || domainOrUrl.startsWith("https://")) domainOrUrl else "https://$domainOrUrl/"
        return target.copy(targetUrl = url)
    }

    private fun ensureTargetUrlParam(params: Params, domainOrUrl: String): Params {
        val url = if (domainOrUrl.startsWith("http://") || domainOrUrl.startsWith("https://")) domainOrUrl else "https://$domainOrUrl/"
        return params.copy(target_url = url)
    }
}


