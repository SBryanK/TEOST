package com.example.teost.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    val userPreferences = preferencesManager.userPreferences
    // Route persistence disabled in PreferencesManager; not used here
    
    fun updateLastActiveTime() {
        viewModelScope.launch {
            authRepository.updateLastActiveTime()
        }
    }
    
    fun updateSplashShownTime() {
        viewModelScope.launch {
            authRepository.updateSplashShownTime()
        }
    }

    // No-op placeholder for route persistence
    fun saveLastRoute(route: String) { }
}
