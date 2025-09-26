package com.example.teost.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _signUpState = MutableStateFlow<Resource<User>?>(null)
    val signUpState: StateFlow<Resource<User>?> = _signUpState.asStateFlow()
    
    fun signUp(email: String, password: String, confirmPassword: String, displayName: String) {
        if (!validateInput(email, password, "", displayName)) {
            return
        }
        
        authRepository.signUp(email.trim(), password, displayName.trim())
            .onEach { result ->
                _signUpState.value = result
            }
            .launchIn(viewModelScope)
    }
    
    private fun validateInput(email: String, password: String, confirmPassword: String, displayName: String): Boolean {
        return when {
            email.isBlank() -> {
                _signUpState.value = Resource.Error(com.example.teost.util.AppError.Validation("Email is required"))
                false
            }
            displayName.isBlank() -> {
                _signUpState.value = Resource.Error(com.example.teost.util.AppError.Validation("Name is required"))
                false
            }
            password.isBlank() -> {
                _signUpState.value = Resource.Error(com.example.teost.util.AppError.Validation("Password is required"))
                false
            }
            !com.example.teost.util.Validation.isLikelyEmail(email) -> {
                _signUpState.value = Resource.Error(com.example.teost.util.AppError.Validation("Please enter a valid email address"))
                false
            }
            password.length < 6 -> {
                _signUpState.value = Resource.Error(com.example.teost.util.AppError.Validation("Password must be at least 6 characters"))
                false
            }
            displayName.length < 2 -> {
                _signUpState.value = Resource.Error(com.example.teost.util.AppError.Validation("Name must be at least 2 characters"))
                false
            }
            else -> true
        }
    }
    
    fun clearError() {
        if (_signUpState.value is Resource.Error) {
            _signUpState.value = null
        }
    }
}