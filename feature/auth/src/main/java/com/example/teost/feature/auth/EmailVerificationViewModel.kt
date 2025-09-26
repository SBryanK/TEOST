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
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _verificationState = MutableStateFlow<Resource<Boolean>?>(null)
    val verificationState: StateFlow<Resource<Boolean>?> = _verificationState.asStateFlow()
    
    fun resendVerificationEmail() {
        authRepository.resendVerificationEmail()
            .onEach { result ->
                _verificationState.value = result
            }
            .launchIn(viewModelScope)
    }
    
    fun checkEmailVerification() {
        authRepository.checkEmailVerification()
            .onEach { result ->
                _verificationState.value = result
            }
            .launchIn(viewModelScope)
    }
}