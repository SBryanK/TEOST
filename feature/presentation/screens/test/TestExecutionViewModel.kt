package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.local.dao.TestResultDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@HiltViewModel
class TestExecutionViewModel @Inject constructor(
    private val testResultDao: TestResultDao,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _message = MutableStateFlow("Preparing tests...")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _timeoutReached = MutableStateFlow(false)
    val timeoutReached: StateFlow<Boolean> = _timeoutReached.asStateFlow()

    private var timeoutJob: Job? = null
    private var trackingJob: Job? = null

    fun startTracking(testIds: List<String>) {
        resetState()
        if (testIds.isEmpty()) {
            // Nothing to track
            _isCompleted.value = true
            return
        }
        _isLoading.value = true
        _message.value = "Running security tests..."
        startTimeout()

        trackingJob = viewModelScope.launch {
            val uid = try { 
                kotlinx.coroutines.withTimeoutOrNull(2000L) {
                    prefs.userPreferences.first().userId
                } ?: ""
            } catch (_: Exception) { "" }
            val userId = if (uid.isBlank()) "default_user" else uid
            
            // Poll recent results with timeout protection
            var pollAttempts = 0
            val maxPollAttempts = 20 // Maximum 20 seconds of polling
            
            while (!_isCompleted.value && !_timeoutReached.value && pollAttempts < maxPollAttempts) {
                try {
                    val recent = kotlinx.coroutines.withTimeoutOrNull(1000L) {
                        testResultDao.getRecentTestResults(userId, limit = (testIds.size * 2).coerceAtLeast(10)).first()
                    } ?: emptyList()
                    
                    val foundCount = recent.count { it.testId in testIds }
                    val ratio = (foundCount.toFloat() / testIds.size.toFloat()).coerceIn(0f, 1f)
                    _progress.value = ratio
                    _message.value = if (ratio < 1f) "Processing $foundCount of ${testIds.size} tests..." else "Tests completed!"
                    
                    if (foundCount >= testIds.size) {
                        _isCompleted.value = true
                        _isLoading.value = false
                        timeoutJob?.cancel()
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TestExecutionViewModel", "Polling error: ${e.message}")
                    _errorMessage.value = e.message
                }
                delay(1000) // Poll every second
                pollAttempts++
            }
            
            // If we exit due to max attempts, mark as timeout
            if (pollAttempts >= maxPollAttempts && !_isCompleted.value) {
                android.util.Log.w("TestExecutionViewModel", "Polling timeout reached")
                _timeoutReached.value = true
                _isLoading.value = false
            }
        }
    }

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(60_000L)
            if (!_isCompleted.value) {
                _timeoutReached.value = true
                _isLoading.value = false
                _errorMessage.value = "Test execution timed out."
            }
        }
    }

    fun resetState() {
        timeoutJob?.cancel()
        trackingJob?.cancel()
        _isLoading.value = false
        _progress.value = 0f
        _message.value = "Preparing tests..."
        _isCompleted.value = false
        _errorMessage.value = null
        _timeoutReached.value = false
    }

    override fun onCleared() {
        timeoutJob?.cancel()
        trackingJob?.cancel()
        super.onCleared()
    }
}