package com.example.teost.presentation.screens.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.teost.data.model.*
import java.util.UUID

@HiltViewModel
class TestFlowViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(savedStateHandle.get<String>(KEY_CATEGORY))
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedType = MutableStateFlow(savedStateHandle.get<String>(KEY_TYPE))
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _target = MutableStateFlow(savedStateHandle.get<String>(KEY_TARGET))
    val target: StateFlow<String?> = _target.asStateFlow()

    private val _configParams = MutableStateFlow(savedStateHandle.get<Map<String, String>>(KEY_CONFIG) ?: emptyMap())
    val configParams: StateFlow<Map<String, String>> = _configParams.asStateFlow()

    private val _lastTestIds = MutableStateFlow(savedStateHandle.get<List<String>>(KEY_LAST_IDS) ?: emptyList())
    val lastTestIds: StateFlow<List<String>> = _lastTestIds.asStateFlow()

    private val _availableTargets = MutableStateFlow(savedStateHandle.get<List<String>>(KEY_AVAILABLE_TARGETS) ?: emptyList())
    val availableTargets: StateFlow<List<String>> = _availableTargets.asStateFlow()

    private val _selectedTargets = MutableStateFlow((savedStateHandle.get<List<String>>(KEY_SELECTED_TARGETS) ?: emptyList()).toSet())
    val selectedTargets: StateFlow<Set<String>> = _selectedTargets.asStateFlow()
    
    // Navigation flow tracking
    private val _navigationStack = MutableStateFlow(savedStateHandle.get<List<String>>(KEY_NAV_STACK) ?: listOf("CategorySelect"))
    val navigationStack: StateFlow<List<String>> = _navigationStack.asStateFlow()

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        savedStateHandle[KEY_CATEGORY] = category
    }

    fun setType(type: String?) {
        _selectedType.value = type
        savedStateHandle[KEY_TYPE] = type
    }

    fun setTarget(value: String?) {
        _target.value = value
        savedStateHandle[KEY_TARGET] = value
    }

    fun updateConfigParam(key: String, value: String) {
        val next = _configParams.value.toMutableMap()
        next[key] = value
        _configParams.value = next
        savedStateHandle[KEY_CONFIG] = next
    }

    fun setLastTestIds(ids: List<String>) {
        _lastTestIds.value = ids
        savedStateHandle[KEY_LAST_IDS] = ids
    }

    fun setAvailableTargets(domains: List<String>) {
        _availableTargets.value = domains.distinct()
        savedStateHandle[KEY_AVAILABLE_TARGETS] = _availableTargets.value
    }

    fun toggleTargetSelection(domain: String) {
        val next = _selectedTargets.value.toMutableSet()
        if (!next.add(domain)) next.remove(domain)
        _selectedTargets.value = next
        savedStateHandle[KEY_SELECTED_TARGETS] = next.toList()
    }

    fun clearSelectedTargets() {
        _selectedTargets.value = emptySet()
        savedStateHandle[KEY_SELECTED_TARGETS] = emptyList<String>()
    }

    fun setSelectedTargets(domains: Collection<String>) {
        val next = domains.toSet()
        _selectedTargets.value = next
        savedStateHandle[KEY_SELECTED_TARGETS] = next.toList()
    }

    /**
     * Reads a parsed Config object and populates the TestCartStore.
     * Returns the number of tests successfully imported.
     */
    fun importConfigIntoCart(config: Config): Int {
        val tests = mutableListOf<TestConfiguration>()
        val defaultDomain = availableTargets.value.firstOrNull() ?: selectedTargets.value.firstOrNull() ?: "example.com"

        config.network_protection?.let { np ->
            tests.add(TestConfiguration(
                testId = UUID.randomUUID().toString(),
                domain = defaultDomain,
                parameters = TestParameters(
                    httpMethod = HttpMethod.GET,
                    requestPath = np.targetPath ?: "/",
                    customHeaders = np.customHeaders ?: emptyMap(),
                    concurrencyLevel = np.concurrencyLevel,
                    durationSec = np.durationSec
                )
            ))
        }
        config.web_protection?.let { wp ->
            tests.add(TestConfiguration(
                testId = UUID.randomUUID().toString(),
                domain = defaultDomain,
                parameters = TestParameters(
                    httpMethod = wp.httpMethod?.let { HttpMethod.valueOf(it) } ?: HttpMethod.GET,
                    requestPath = "/",
                    payloadList = wp.payloadList,
                    encodingMode = wp.encodingMode?.let { EncodingMode.valueOf(it) }
                )
            ))
        }
        config.bot_management?.let { bm ->
            tests.add(TestConfiguration(
                testId = UUID.randomUUID().toString(),
                domain = defaultDomain,
                parameters = TestParameters(
                    customHeaders = mapOf("User-Agent" to "{{PAYLOAD}}"),
                    payloadList = bm.uaProfiles,
                    crawlDepth = 2,
                    respectRobotsTxt = true
                )
            ))
        }
        config.api_protection?.endpoints?.forEach { endpoint ->
            tests.add(TestConfiguration(
                testId = UUID.randomUUID().toString(),
                domain = defaultDomain,
                parameters = TestParameters(
                    httpMethod = endpoint.method?.let { HttpMethod.valueOf(it) } ?: HttpMethod.GET,
                    requestPath = endpoint.path,
                    customHeaders = endpoint.headers ?: emptyMap()
                )
            ))
        }

        if (tests.isNotEmpty()) {
            val currentTests = TestCartStore.items.value.toMutableList()
            currentTests.addAll(tests)
            TestCartStore.setAll(currentTests)
        }
        return tests.size
    }

    fun clearFlow() {
        setCategory(null)
        setType(null)
        setTarget(null)
        _configParams.value = emptyMap()
        savedStateHandle[KEY_CONFIG] = emptyMap<String, String>()
        clearSelectedTargets()
        _navigationStack.value = listOf("CategorySelect")
        savedStateHandle[KEY_NAV_STACK] = listOf("CategorySelect")
    }
    
    fun pushToNavigationStack(screen: String) {
        val current = _navigationStack.value.toMutableList()
        if (current.lastOrNull() != screen) {
            current.add(screen)
            _navigationStack.value = current
            savedStateHandle[KEY_NAV_STACK] = current
        }
    }
    
    fun popFromNavigationStack(): String? {
        val current = _navigationStack.value.toMutableList()
        return if (current.size > 1) {
            current.removeLastOrNull()
            _navigationStack.value = current
            savedStateHandle[KEY_NAV_STACK] = current
            current.lastOrNull()
        } else {
            null
        }
    }
    
    fun getLastScreen(): String? {
        val stack = _navigationStack.value
        return if (stack.size > 1) stack[stack.size - 2] else null
    }

    companion object {
        private const val KEY_CATEGORY = "tf_category"
        private const val KEY_TYPE = "tf_type"
        private const val KEY_TARGET = "tf_target"
        private const val KEY_CONFIG = "tf_config"
        private const val KEY_LAST_IDS = "tf_last_ids"
        private const val KEY_AVAILABLE_TARGETS = "tf_available_targets"
        private const val KEY_SELECTED_TARGETS = "tf_selected_targets"
        private const val KEY_NAV_STACK = "tf_nav_stack"
    }
}



