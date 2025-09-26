package com.example.teost.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.repository.TestResultRepository
import com.example.teost.data.model.TestResult
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.gson.Gson
import java.io.File

data class HistoryDetailUi(
    val domain: String = "",
    val testName: String = "",
    val category: String = "",
    val type: String = "",
    val status: String = "",
    val statusCode: Int? = null,
    val headers: Map<String, String> = emptyMap(),
    val dnsTime: Long? = null,
    val ttfb: Long? = null,
    val rawLogs: String? = null,
    val summary: String = "",
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val testResultRepository: TestResultRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(HistoryDetailUi())
    val ui: StateFlow<HistoryDetailUi> = _ui.asStateFlow()
    private var lastLoaded: TestResult? = null

    fun load(id: String) {
        viewModelScope.launch {
            val res: TestResult? = testResultRepository.getById(id)
            res?.let {
                lastLoaded = it
                val summary = buildSummary(it)
                _ui.value = HistoryDetailUi(
                    domain = it.domain,
                    testName = it.testName,
                    category = it.category.name,
                    type = it.type::class.simpleName ?: it.type.toString(),
                    status = it.status.name,
                    statusCode = it.resultDetails.statusCode,
                    headers = redactHeaders(it.resultDetails.headers) ?: emptyMap(),
                    dnsTime = it.resultDetails.dnsResolutionTime,
                    ttfb = it.resultDetails.ttfb,
                    rawLogs = it.rawLogs,
                    summary = summary
                )
            }
        }
    }

    fun getParamsSnapshot(): com.example.teost.data.model.TestParameters? = lastLoaded?.resultDetails?.paramsSnapshot

    fun getDetails(): com.example.teost.data.model.TestResultDetails? = lastLoaded?.resultDetails

    fun getRawLogs(): String? = lastLoaded?.rawLogs

    suspend fun exportResult(context: Context, id: String): Uri? {
        val result: TestResult = testResultRepository.getById(id) ?: return null
        // Redact before export as JSON (same policy as bulk export)
        val redactedHeaders = redactHeaders(result.resultDetails.headers)
        val redactedDetails = result.resultDetails.copy(
            headers = redactedHeaders,
            encryptedBody = null,
            encryptionAlgorithm = null,
            fingerprintId = null
        )
        val safe = result.copy(resultDetails = redactedDetails, rawLogs = null, errorMessage = null)
        val json = Gson().toJson(safe)
        val dir = File(context.filesDir, "export").apply { mkdirs() }
        val file = File(dir, "test_result_${id.take(8)}.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    suspend fun exportLogs(context: Context, id: String): Uri? {
        val result: TestResult = testResultRepository.getById(id) ?: return null
        val logs = (result.rawLogs ?: "").ifBlank { "(no logs)" }
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        val file = File(dir, "test_logs_${id.take(8)}.txt")
        file.writeText(logs)
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    suspend fun exportEncryptedBody(context: Context, id: String): Uri? {
        val result: TestResult = testResultRepository.getById(id) ?: return null
        val details = result.resultDetails
        val enc = mapOf(
            "algorithm" to (details.encryptionAlgorithm ?: ""),
            "ciphertext" to (details.encryptedBody ?: "")
        )
        val json = Gson().toJson(enc)
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        val file = File(dir, "encrypted_${id.take(8)}.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    private fun redactHeaders(headers: Map<String, String>?): Map<String, String>? {
        if (headers == null) return null
        val sensitive = setOf(
            "authorization","proxy-authorization","cookie","set-cookie","x-api-key","x-api-token",
            "x-auth-token","authentication","access-token","id-token","api-key","token"
        )
        return headers.mapNotNull { (k, v) ->
            val drop = k.lowercase() in sensitive
            if (drop) null else {
                val vv = v.trim()
                val red = if (vv.startsWith("Bearer ", true) || vv.length > 128 || looksLikeJwt(vv)) "REDACTED" else vv
                k to red
            }
        }.toMap()
    }

    private fun looksLikeJwt(v: String): Boolean {
        val parts = v.split('.')
        return parts.size == 3 && parts.all { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
    }

    private fun buildSummary(result: TestResult): String {
        val d = result.resultDetails
        val code = d.statusCode
        // OK: 2xx/3xx without block/challenge indicators
        val ok = code != null && code in 200..399 && d.challengeDetected == null && d.wafRuleTriggered.isNullOrBlank()
        if (ok) return "OK"

        // Rate limit
        if (code == 429 || !d.retryAfterHeader.isNullOrBlank() || d.blockReason?.equals("RateLimit", ignoreCase = true) == true) {
            val iter = d.blockedIteration?.let { " " + formatIter(it) } ?: ""
            return "Rate limited${iter}"
        }

        // WAF block
        if (code == 403 && (!d.wafRuleTriggered.isNullOrBlank() || d.blockReason?.equals("WAF", ignoreCase = true) == true)) {
            val rule = d.wafRuleTriggered?.let { " (rule: ${it})" } ?: ""
            val iter = d.blockedIteration?.let { " " + formatIter(it) } ?: ""
            return "Blocked by WAF${rule}${iter}"
        }

        // Bot challenge
        if (d.challengeDetected != null || d.blockReason?.equals("Bot", ignoreCase = true) == true) {
            val kind = d.challengeDetected?.name ?: "Bot"
            val iter = d.blockedIteration?.let { " " + formatIter(it) } ?: ""
            return "Challenge ${kind}${iter}"
        }

        // Region/IP blocking → per instruction: do not write "blocked", use "Cannot access"
        if (code == 403 || code == 451 || d.blockReason?.equals("GeoIP", ignoreCase = true) == true || d.blockReason?.equals("IP", ignoreCase = true) == true) {
            return "Cannot access"
        }

        // Network mitigation heuristic
        if ((d.connectionsFailed ?: 0) > (d.connectionsEstablished ?: 0)) {
            val iter = d.blockedIteration?.let { " " + formatIter(it) } ?: ""
            return "Network mitigation detected${iter}"
        }

        return "Unknown; check headers/logs"
    }

    private fun formatIter(i: Int): String = "at iteration ${i}"
}
