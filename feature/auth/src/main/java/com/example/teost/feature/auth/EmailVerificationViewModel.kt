
package com.example.teost.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.util.Resource
import com.example.teost.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _status = MutableStateFlow<Resource<Boolean>?>(null)
    val status: StateFlow<Resource<Boolean>?> = _status.asStateFlow()

    private val _resendCooldownSec = MutableStateFlow(0)
    val resendCooldownSec: StateFlow<Int> = _resendCooldownSec.asStateFlow()

    private var pollJob: Job? = null
    private var cooldownJob: Job? = null

    fun startPolling(intervalMs: Long = 4000L) {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (!isVerified.value) {
                authRepository.checkEmailVerification().collect { res ->
                    _status.value = res
                    if (res is Resource.Success && res.data == true) {
                        _isVerified.value = true
                    }
                }
                if (!isVerified.value) delay(intervalMs)
            }
        }
    }

    fun resendEmail() {
        if (_resendCooldownSec.value > 0) return
        viewModelScope.launch {
            authRepository.resendVerificationEmail().collect { res -> _status.value = res }
            startCooldown()
        }
    }

    private fun startCooldown(seconds: Int = 60) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _resendCooldownSec.value = seconds
            while (_resendCooldownSec.value > 0) {
                delay(1000)
                _resendCooldownSec.value = _resendCooldownSec.value - 1
            }
        }
    }
}




