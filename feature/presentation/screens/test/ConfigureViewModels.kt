package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HttpSpikeState(
    val burstRequests: String = "50",
    val burstIntervalMs: String = "100",
    val sustainedWindowSec: String = "5",
    val concurrencyLevel: String = "6",
    val targetPath: String = "/",
) {
    val isValid: Boolean =
        burstRequests.toIntOrNull() != null &&
        burstIntervalMs.toIntOrNull() != null &&
        sustainedWindowSec.toIntOrNull() != null &&
        concurrencyLevel.toIntOrNull() != null &&
        targetPath.isNotBlank()
}

@HiltViewModel
class HttpSpikeConfigureViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(HttpSpikeState())
    val state: StateFlow<HttpSpikeState> = _state.asStateFlow()

    fun update(
        burstRequests: String? = null,
        burstIntervalMs: String? = null,
        sustainedWindowSec: String? = null,
        concurrencyLevel: String? = null,
        targetPath: String? = null,
    ) {
        _state.update { s ->
            s.copy(
                burstRequests = burstRequests ?: s.burstRequests,
                burstIntervalMs = burstIntervalMs ?: s.burstIntervalMs,
                sustainedWindowSec = sustainedWindowSec ?: s.sustainedWindowSec,
                concurrencyLevel = concurrencyLevel ?: s.concurrencyLevel,
                targetPath = targetPath ?: s.targetPath,
            )
        }
    }
}

data class WafState(
    val wafPayloads: String = "' OR 1=1 --\n<script>alert(1)</script>",
    val encodingMode: String = "NONE", // NONE|URL_ENCODE|BASE64
    val injectionPoint: String = "QUERY_PARAM", // QUERY_PARAM|PATH_PARAM|BODY
    val targetParam: String = "q",
    val httpMethod: String = "GET", // GET|POST
    val requestPath: String = "/search",
) {
    val isValid: Boolean = wafPayloads.isNotBlank() && requestPath.isNotBlank()
}

@HiltViewModel
class WafConfigureViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(WafState())
    val state: StateFlow<WafState> = _state.asStateFlow()

    fun update(
        wafPayloads: String? = null,
        encodingMode: String? = null,
        injectionPoint: String? = null,
        targetParam: String? = null,
        httpMethod: String? = null,
        requestPath: String? = null,
    ) {
        _state.update { s ->
            s.copy(
                wafPayloads = wafPayloads ?: s.wafPayloads,
                encodingMode = encodingMode ?: s.encodingMode,
                injectionPoint = injectionPoint ?: s.injectionPoint,
                targetParam = targetParam ?: s.targetParam,
                httpMethod = httpMethod ?: s.httpMethod,
                requestPath = requestPath ?: s.requestPath,
            )
        }
    }
}

data class ApiRateState(
    val rpsTarget: String = "10",
    val windowSec: String = "10",
    val requestPath: String = "/api/ping",
    val fingerprintMode: String = "cookie", // cookie|device
) {
    val isValid: Boolean =
        rpsTarget.toIntOrNull() != null &&
        windowSec.toIntOrNull() != null &&
        requestPath.isNotBlank() &&
        fingerprintMode.isNotBlank()
}

@HiltViewModel
class ApiRateConfigureViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(ApiRateState())
    val state: StateFlow<ApiRateState> = _state.asStateFlow()

    fun update(
        rpsTarget: String? = null,
        windowSec: String? = null,
        requestPath: String? = null,
        fingerprintMode: String? = null,
    ) {
        _state.update { s ->
            s.copy(
                rpsTarget = rpsTarget ?: s.rpsTarget,
                windowSec = windowSec ?: s.windowSec,
                requestPath = requestPath ?: s.requestPath,
                fingerprintMode = fingerprintMode ?: s.fingerprintMode,
            )
        }
    }
}

data class BotUaState(
    val uaProfilesText: String = "Chrome\nFirefox\nSafari",
    val headerMinimal: String = "false",
) {
    val isValid: Boolean = uaProfilesText.lines().any { it.trim().isNotEmpty() }
}

@HiltViewModel
class BotUaConfigureViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(BotUaState())
    val state: StateFlow<BotUaState> = _state.asStateFlow()

    fun update(
        uaProfilesText: String? = null,
        headerMinimal: String? = null,
    ) {
        _state.update { s ->
            s.copy(
                uaProfilesText = uaProfilesText ?: s.uaProfilesText,
                headerMinimal = headerMinimal ?: s.headerMinimal,
            )
        }
    }
}

data class UserCredits(
    val remaining: Int = 100,
    val used: Int = 0,
)

@HiltViewModel
class TestCreditsViewModel @Inject constructor() : ViewModel() {
    private val _credits = MutableStateFlow(UserCredits())
    val credits: StateFlow<UserCredits> = _credits.asStateFlow()
}


