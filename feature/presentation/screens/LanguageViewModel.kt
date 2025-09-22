package com.example.teost.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.presentation.screens.lang.LanguageManager
import com.example.teost.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    prefs: PreferencesManager
) : ViewModel() {

    val selectedLanguage: StateFlow<String> = prefs.userPreferences
        .map { it.selectedLanguage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en")

    fun setLanguage(tag: String) {
        viewModelScope.launch {
            languageManager.setLanguage(tag)
        }
    }
}


