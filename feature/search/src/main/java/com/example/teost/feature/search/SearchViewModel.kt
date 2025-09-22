package com.example.teost.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.repository.DomainRepository
import com.example.teost.data.model.ConnectionTestResult
import com.example.teost.data.model.Domain
import com.example.teost.data.repository.ConnectionTestRepository
import com.example.teost.util.Resource
import com.example.teost.util.UrlValidator
import com.example.teost.util.Result
import com.example.teost.util.AppException
import com.example.teost.util.toAppException
import com.example.teost.util.DnsResolver
import com.example.teost.util.TargetRegistry
import com.example.teost.core.domain.model.TestRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import com.example.teost.data.local.PreferencesManager

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val connectionTestRepository: ConnectionTestRepository,
    private val domainRepository: DomainRepository,
    private val prefs: PreferencesManager,
    private val performTestUseCase: PerformTestUseCase
) : ViewModel() {
    
    private val _testState = MutableStateFlow<Resource<List<ConnectionTestResult>>?>(null)
    val testState: StateFlow<Resource<List<ConnectionTestResult>>?> = _testState.asStateFlow()
    
    private val _testResults = MutableStateFlow<List<ConnectionTestResult>>(emptyList())
    val testResults: StateFlow<List<ConnectionTestResult>> = _testResults.asStateFlow()
    
    private val _currentUserId = MutableStateFlow("")
    private val _encryptionAcked = MutableStateFlow(true)
    val encryptionAcked: StateFlow<Boolean> = _encryptionAcked.asStateFlow()

    init {
        // Auto-initialize current user id
        viewModelScope.launch {
            prefs.userPreferences.collect { up ->
                _currentUserId.value = up.userId
            }
        }
    }
    
    fun testConnection(input: String) {
        val urls = UrlValidator.parseMultipleInputs(input)
        if (urls.isEmpty()) {
            _testState.value = Resource.Error(com.example.teost.util.AppError.Validation("Please enter at least one valid URL, domain, or IP address"))
            return
        }
        viewModelScope.launch {
            _testResults.value = emptyList()
            _testState.value = Resource.Loading()
            val results = mutableListOf<ConnectionTestResult>()
            
            android.util.Log.d("SearchViewModel", "Starting connection test for ${urls.size} URLs")
            
            for (url in urls) {
                try {
                    performTestUseCase(url).collect { result ->
                        when (result) {
                            is Result.Loading -> _testState.value = Resource.Loading()
                            is Result.Success -> {
                                val r = result.data
                                val ipList = try { DnsResolver.resolveToIpList(r.url) } catch (_: Exception) { emptyList() }
                                TargetRegistry.putResolved(r.url, ipList)
                                TargetRegistry.setLastTarget(r.url)
                                val mapped = ConnectionTestResult(
                                    url = r.url,
                                    domain = try { java.net.URL(r.url).host } catch (_: Exception) { r.url },
                                    ipAddresses = ipList,
                                    statusCode = r.responseCode,
                                    httpProtocol = r.httpProtocol,
                                    statusMessage = r.responseMessage,
                                    headers = r.headers,
                                    responseTime = r.responseTimeMs,
                                    requestId = null,
                                    isSuccessful = r.responseCode in 200..399,
                                    errorMessage = if (r.responseCode !in 200..399) "HTTP ${r.responseCode}: ${r.responseMessage}" else null,
                                    timestamp = java.util.Date(),
                                    encryptionAlgorithm = r.encryptionAlgorithm,
                                    encryptionKey = r.encryptionKey,
                                    encryptionIv = r.encryptionIv,
                                    encryptedBody = r.encryptedBody,
                                    logs = null
                                )
                                results.add(mapped)
                                _testResults.value = results.toList()
                            }
                            is Result.Error -> {
                                android.util.Log.w("SearchViewModel", "Test failed for $url: ${result.exception.message}")
                                _testState.value = Resource.Error(com.example.teost.util.AppError.Network(result.exception.message))
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SearchViewModel", "Failed to test $url: ${e.message}")
                }
            }
            _testState.value = Resource.Success(results)
            results.filter { it.isSuccessful }.forEach { saveDomain(it) }
            runCatching { prefs.saveLastSearch(input, results) }
        }
    }
    
    private suspend fun saveDomain(result: ConnectionTestResult) {
        val uid = _currentUserId.value
        if (uid.isBlank()) return
        domainRepository.upsertFromConnectionResult(result, uid)
    }
    
    fun setUserId(userId: String) {
        _currentUserId.value = userId
    }
    
    fun clearResults() {
        _testResults.value = emptyList()
        _testState.value = null
    }

    fun clearError() {
        if (_testState.value is Resource.Error) {
            _testState.value = null
        }
    }

    fun addFavorite(domain: String) {
        val uid = _currentUserId.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            try { domainRepository.setFavorite(domain, uid, true) } catch (_: Exception) {}
        }
    }

    fun ackEncryptionDisplay() {}

    fun restoreLastResultsIfFresh(ttlMs: Long = 60 * 60 * 1000L) {
        viewModelScope.launch {
            prefs.lastSearchFlow().take(1).collect { cache ->
                if (cache == null) return@collect
                val age = System.currentTimeMillis() - cache.timestampMs
                if (age in 0..ttlMs) {
                    _testResults.value = cache.results
                    _testState.value = Resource.Success(cache.results)
                }
            }
        }
    }
}

