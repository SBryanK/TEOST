package com.example.teost.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import com.example.teost.data.model.User
import com.example.teost.data.repository.AuthRepository
import com.example.teost.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesManager: com.example.teost.data.local.PreferencesManager
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<Resource<User>?>(null)
    val loginState: StateFlow<Resource<User>?> = _loginState.asStateFlow()
    
    fun signIn(email: String, password: String, rememberMe: Boolean = false) {
        if (!validateInput(email, password)) return
        
        authRepository.signIn(email.trim(), password)
            .onEach { result ->
                _loginState.value = result
                if (result is Resource.Success && rememberMe) {
                    // Persist email for next time
                    withContext(Dispatchers.IO) {
                        preferencesManager.setRememberedEmail(email.trim())
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadRememberedEmail(onLoaded: (String) -> Unit) {
        viewModelScope.launch {
            val email = preferencesManager.userPreferences
                .first().rememberedEmail
            onLoaded(email)
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _loginState.value = Resource.Error(com.example.teost.util.AppError.Validation("Email is required"))
                false
            }
            !com.example.teost.util.Validation.isLikelyEmail(email) -> {
                _loginState.value = Resource.Error(com.example.teost.util.AppError.Validation("Please enter a valid email address"))
                false
            }
            password.isBlank() -> {
                _loginState.value = Resource.Error(com.example.teost.util.AppError.Validation("Password is required"))
                false
            }
            password.length < 6 -> {
                _loginState.value = Resource.Error(com.example.teost.util.AppError.Validation("Password must be at least 6 characters"))
                false
            }
            else -> true
        }
    }
    
    fun clearError() {
        if (_loginState.value is Resource.Error) {
            _loginState.value = null
        }
    }
}
