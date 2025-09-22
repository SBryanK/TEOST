package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.model.TestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

@HiltViewModel
class TestSuccessViewModel @Inject constructor(
    private val dao: TestResultDao,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _results = MutableStateFlow<List<TestResult>>(emptyList())
    val results: StateFlow<List<TestResult>> = _results.asStateFlow()

    fun load(testIds: List<String>) {
        viewModelScope.launch {
            try {
                // Use same userId resolution logic as TestExecutionWorker
                val prefsUserId = try { 
                    kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        prefs.userPreferences.first().userId
                    } ?: ""
                } catch (_: Exception) { "" }
                val authUid = try { 
                    val clazz = Class.forName("com.google.firebase.auth.FirebaseAuth")
                    val getInstance = clazz.getMethod("getInstance")
                    val inst = getInstance.invoke(null)
                    val userField = inst.javaClass.getMethod("getCurrentUser")
                    val user = userField.invoke(inst)
                    user?.javaClass?.getMethod("getUid")?.invoke(user) as? String ?: ""
                } catch (_: Exception) { "" }
                val userId = when {
                    authUid.isNotBlank() -> authUid
                    prefsUserId.isNotBlank() -> prefsUserId
                    else -> "default_user" // Same fallback as TestExecutionWorker
                }
                android.util.Log.d("TestSuccessViewModel", "User ID resolution: prefsUserId='$prefsUserId', authUid='$authUid', userId='$userId'")
                android.util.Log.d("TestSuccessViewModel", "Loading test results for testIds: $testIds")
                if (testIds.isNotEmpty()) {
                    // Retry logic to handle timing issues with database writes
                    var loaded = emptyList<TestResult>()
                    var attempts = 0
                    while (loaded.isEmpty() && attempts < 3) { // Reduced attempts to prevent long blocking
                        loaded = dao.getTestResultsByTestIds(testIds).filter { it.userId == userId }
                        android.util.Log.d("TestSuccessViewModel", "Attempt $attempts: Found ${loaded.size} results for userId='$userId'")
                        if (loaded.isEmpty()) {
                            delay(500) // Reduced delay
                            attempts++
                        }
                    }
                    android.util.Log.d("TestSuccessViewModel", "Final results: ${loaded.size} test results loaded")
                    _results.value = loaded.sortedByDescending { it.startTime }
                } else {
                    val recent = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        dao.getRecentTestResults(userId, limit = 10).first()
                    } ?: emptyList()
                    _results.value = recent
                }
            } catch (_: Exception) {
                _results.value = emptyList()
            }
        }
    }
}


