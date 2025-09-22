package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BotConfigureViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun update(
        uaProfilesText: String? = null,
        headerMinimal: String? = null
    ) {
        _state.value = state.value.copy(
            uaProfilesText = uaProfilesText ?: state.value.uaProfilesText,
            headerMinimal = headerMinimal ?: state.value.headerMinimal
        )
    }

    data class UiState(
        val uaProfilesText: String = "Googlebot\nBingbot",
        val headerMinimal: String = "false",
        val isValid: Boolean = true // Basic validation
    )
}
