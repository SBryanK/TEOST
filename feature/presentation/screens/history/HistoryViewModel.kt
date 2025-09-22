package com.example.teost.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.teost.data.model.TestResult
import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestStatus
import com.example.teost.data.model.TestType
import com.example.teost.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.teost.data.local.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.FileProvider
import com.google.gson.Gson
import java.io.File
import java.util.Locale

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository,
    private val prefs: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: String get() = _query.value
    fun setQuery(v: String) { _query.value = v }

    private val userIdFlow = MutableStateFlow("")
    fun setUserId(uid: String) { userIdFlow.value = uid }

    // Filters
    private val _category = MutableStateFlow<TestCategory?>(null)
    private val _status = MutableStateFlow<TestStatus?>(null)
    private val _type = MutableStateFlow<TestType?>(null)
    private val _fromDate = MutableStateFlow<java.util.Date?>(null)
    private val _toDate = MutableStateFlow<java.util.Date?>(null)
    private val _domainFilter = MutableStateFlow<String?>(null)
    fun setCategory(c: TestCategory?) { _category.value = c }
    fun setStatus(s: TestStatus?) { _status.value = s }
    fun setType(t: TestType?) { _type.value = t }
    fun setFromDate(d: java.util.Date?) { _fromDate.value = d }
    fun setToDate(d: java.util.Date?) { _toDate.value = d }
    fun setDomainFilter(d: String?) { _domainFilter.value = d }
    fun resetFilters() {
        _category.value = null
        _status.value = null
        _type.value = null
        _fromDate.value = null
        _toDate.value = null
        _domainFilter.value = null
    }

    private var historyReceiver: android.content.BroadcastReceiver? = null

    init {
        // Listen for history refresh broadcasts
        viewModelScope.launch {
            try {
                historyReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                        android.util.Log.d("HistoryViewModel", "Broadcast received: ${intent?.action}")
                        if (intent?.action == "com.example.teost.ACTION_REFRESH_HISTORY") {
                            android.util.Log.d("HistoryViewModel", "History refresh broadcast received, notifying repository")
                            
                            // IMMEDIATE REFRESH STRATEGY
                            android.util.Log.d("HistoryViewModel", "History refresh broadcast received - implementing immediate refresh")
                            
                            // 1. Clear filters to ensure latest results are visible
                            android.util.Log.d("HistoryViewModel", "Step 1: Clearing all filters")
                            resetFilters()
                            
                            // 2. Reset query to show all results
                            android.util.Log.d("HistoryViewModel", "Step 2: Clearing search query")
                            _query.value = ""
                            
                            // 3. Force repository refresh
                            android.util.Log.d("HistoryViewModel", "Step 3: Forcing repository refresh")
                            repo.forceRefresh()
                            
                            // 4. Additional delay and second refresh to ensure data appears
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(500) // Wait for database write to complete
                                android.util.Log.d("HistoryViewModel", "Step 4: Second refresh after delay")
                                repo.forceRefresh()
                            }
                        }
                    }
                }
                val filter = android.content.IntentFilter("com.example.teost.ACTION_REFRESH_HISTORY")
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(historyReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("DEPRECATION")
                    context.registerReceiver(historyReceiver, filter)
                }
            } catch (_: Exception) {}
        }
        
        // Restore persisted filters when available
        viewModelScope.launch {
            prefs.historyFiltersFlow().collectLatest { f ->
                if (f != null) {
                    _category.value = f.category
                    _status.value = f.status
                    _type.value = f.type
                    _fromDate.value = f.fromEpochMs?.let { java.util.Date(it) }
                    _toDate.value = f.toEpochMs?.let { java.util.Date(it) }
                    _domainFilter.value = f.domainContains
                }
            }
        }
        // Auto-scope to current user - collect only once to prevent loops
        viewModelScope.launch {
            try {
                val initialPrefs = prefs.userPreferences.first()
                val authUid = try {
                    val clazz = Class.forName("com.google.firebase.auth.FirebaseAuth")
                    val getInstance = clazz.getMethod("getInstance")
                    val inst = getInstance.invoke(null)
                    val userField = inst.javaClass.getMethod("getCurrentUser")
                    val user = userField.invoke(inst)
                    user?.javaClass?.getMethod("getUid")?.invoke(user) as? String
                } catch (_: Exception) { null }
                val resolved = when {
                    authUid != null && authUid.isNotBlank() -> authUid
                    !initialPrefs.userId.isNullOrBlank() -> initialPrefs.userId
                    else -> "default_user" // Same fallback as TestExecutionWorker
                }
                android.util.Log.d("HistoryViewModel", "User ID resolution (initial): prefsUserId='${initialPrefs.userId}', authUid='$authUid', resolved='$resolved'")
                userIdFlow.value = resolved
                
                // Debug: Check how many results exist for this user
                try {
                    val totalCount = repo.listFilteredAdvanced(resolved, null, null, null, null, null, null).size
                    android.util.Log.d("HistoryViewModel", "User $resolved has $totalCount total test results in database")
                    
                    // Force immediate refresh to ensure latest data is displayed
                    android.util.Log.d("HistoryViewModel", "Forcing immediate refresh on ViewModel init")
                    repo.forceRefresh()
                } catch (e: Exception) {
                    android.util.Log.e("HistoryViewModel", "Failed to count user results", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Failed to initialize user ID", e)
                userIdFlow.value = "default_user"
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val paged = _query
        .flatMapLatest { q ->
            userIdFlow.flatMapLatest { uid ->
                if (uid.isBlank()) {
                    repo.pagerAll("")
                } else if (q.isNotBlank()) {
                    // Pager for search; trigger external refresh from UI if needed
                    repo.pagerSearch(uid, q)
                } else {
                    // Pager for filters only; external refresh controlled by UI/ViewModel
                    _category
                        .combine(_status) { c, s -> c to s }
                        .combine(_type) { (c, s), t -> Triple(c, s, t) }
                        .combine(_fromDate) { (c, s, t), f -> arrayOf(c, s, t, f) }
                        .combine(_toDate) { arr, to -> arr + to }
                        .combine(_domainFilter) { arr, d ->
                            @Suppress("UNCHECKED_CAST")
                            val c = arr[0] as TestCategory?
                            val s = arr[1] as TestStatus?
                            val t = arr[2] as TestType?
                            val f = arr[3] as java.util.Date?
                            val toDate = arr[4] as java.util.Date?
                            // Combine with repo invalidation signal so UI can call refresh without rebuilding Pager
                            repo.invalidationFlow
                                .onStart { emit(Unit) }
                                .flatMapLatest {
                                    repo.pagerFilteredAdvanced(uid, c, s, t, f, toDate, d)
                                }
                        }
                        .flatMapLatest { it }
                }
            }
        }
        .cachedIn(viewModelScope)

    fun persistFilters(
        category: TestCategory?,
        status: TestStatus?,
        type: TestType?,
        from: java.util.Date?,
        to: java.util.Date?,
        domainContains: String?
    ) {
        viewModelScope.launch {
            try {
                val filters = com.example.teost.data.local.HistoryFilters(
                    category = category,
                    status = status,
                    type = type,
                    fromEpochMs = from?.time,
                    toEpochMs = to?.time,
                    domainContains = domainContains
                )
                prefs.saveHistoryFilters(filters)
            } catch (_: Exception) {}
        }
    }

    suspend fun exportFiltered(
        context: Context,
        format: String = "csv"
    ): Uri? {
        val uid = userIdFlow.value
        if (uid.isBlank()) return null
        val list = repo.listFilteredAdvanced(
            userId = uid,
            category = _category.value,
            status = _status.value,
            type = _type.value,
            from = _fromDate.value,
            to = _toDate.value,
            domain = _domainFilter.value
        )
        if (list.isEmpty()) return null

        val dir = File(context.filesDir, "export").apply { mkdirs() }
        val nameSuffix = System.currentTimeMillis().toString().takeLast(6)
        return if (format.lowercase(Locale.ROOT) == "json") {
            val file = File(dir, "history_${nameSuffix}.json")
            // Redaction: drop sensitive headers and logs before JSON export
            val redacted = list.map { result ->
                val details = result.resultDetails
                val redactedHeaders = redactHeaders(details.headers)
                val redactedDetails = details.copy(
                    headers = redactedHeaders,
                    encryptedBody = null,
                    encryptionAlgorithm = null,
                    fingerprintId = null
                )
                result.copy(
                    resultDetails = redactedDetails,
                    rawLogs = null,
                    errorMessage = null
                )
            }
            file.writeText(Gson().toJson(redacted))
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } else {
            val file = File(dir, "history_${nameSuffix}.csv")
            val header = "id,domain,testName,category,status,statusCode,startTime,durationMs\n"
            val rows = buildString {
                append(header)
                list.forEach { r ->
                    val code = r.resultDetails.statusCode ?: 0
                    val dur = r.duration
                    append("\"${r.id}\",\"${r.domain}\",\"${r.testName}\",\"${r.category.name}\",\"${r.status.name}\",${code},${r.startTime.time},${dur}\n")
                }
            }
            file.writeText(rows)
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        }
    }

    private fun redactHeaders(headers: Map<String, String>?): Map<String, String>? {
        if (headers == null) return null
        val sensitive = setOf(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-api-token",
            "x-auth-token",
            "authentication",
            "access-token",
            "id-token",
            "api-key",
            "token"
        )
        return headers
            .filterKeys { key -> key.lowercase(Locale.ROOT) !in sensitive }
            .mapValues { (_, value) ->
                val v = value.trim()
                if (v.startsWith("Bearer ", ignoreCase = true) || v.length > 128 || looksLikeJwt(v)) "REDACTED" else v
            }
    }

    private fun looksLikeJwt(v: String): Boolean {
        // rudimentary JWT detection: three dot-separated base64url-ish segments
        val parts = v.split('.')
        return parts.size == 3 && parts.all { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister broadcast receiver to prevent memory leak
        historyReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
                android.util.Log.d("HistoryViewModel", "Broadcast receiver unregistered")
            } catch (_: Exception) {
                android.util.Log.w("HistoryViewModel", "Failed to unregister broadcast receiver")
            }
        }
    }
}
