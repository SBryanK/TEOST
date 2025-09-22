package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ApiConfigureViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun update(
        rpsTarget: String? = null,
        windowSec: String? = null,
        requestPath: String? = null,
        fingerprintMode: String? = null
    ) {
        _state.value = state.value.copy(
            rpsTarget = rpsTarget ?: state.value.rpsTarget,
            windowSec = windowSec ?: state.value.windowSec,
            requestPath = requestPath ?: state.value.requestPath,
            fingerprintMode = fingerprintMode ?: state.value.fingerprintMode
        )
    }

    data class UiState(
        val rpsTarget: String = "20",
        val windowSec: String = "10",
        val requestPath: String = "/api/v1/ping",
        val fingerprintMode: String = "IP",
        val isValid: Boolean = true // Basic validation for now
    )
}
