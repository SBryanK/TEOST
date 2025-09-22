package com.example.teost.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _resetPasswordState = MutableStateFlow<Resource<Boolean>?>(null)
    val resetPasswordState: StateFlow<Resource<Boolean>?> = _resetPasswordState.asStateFlow()
    
    fun sendPasswordResetEmail(email: String) {
        if (!validateInput(email)) {
            return
        }
        
        authRepository.forgotPassword(email.trim())
            .onEach { result ->
                _resetPasswordState.value = result
            }
            .launchIn(viewModelScope)
    }
    
    private fun validateInput(email: String): Boolean {
        return when {
            email.isBlank() -> {
                _resetPasswordState.value = Resource.Error(com.example.teost.util.AppError.Validation("Email is required"))
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _resetPasswordState.value = Resource.Error(com.example.teost.util.AppError.Validation("Please enter a valid email address"))
                false
            }
            else -> true
        }
    }
    
    fun clearError() {
        if (_resetPasswordState.value is Resource.Error) {
            _resetPasswordState.value = null
        }
    }
}