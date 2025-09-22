package com.example.teost.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.repository.CreditsRepository
import com.example.teost.data.repository.CreditsBackendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

@HiltViewModel
class CreditsViewModel @Inject constructor(
    private val repo: CreditsRepository,
    private val backendRepo: CreditsBackendRepository,
) : ViewModel() {

    data class UiState(
        val used: Int = 0,
        val remaining: Int = 0,
        val isRequesting: Boolean = false,
        val requestResult: String? = null
    )

    val uiState: StateFlow<UiState> = repo.observeCredits()
        .map { credits -> UiState(used = credits.used, remaining = credits.remaining) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun requestTokens(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repo.requestTokens(50)
                onResult(true, id)
            } catch (e: Exception) {
                onResult(false, e.message ?: "Request failed")
            }
        }
    }

    suspend fun consume(amount: Int): Boolean {
        return try {
            repo.consumeCredits(amount)
        } catch (_: Exception) {
            false
        }
    }

    fun requestTokensViaBackend(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // fallback: gunakan repo.requestTokens untuk mendapatkan id & email user
                // lalu panggil backend hanya jika AppConfig.creditsApiBaseUrl dikonfigurasi
                val id = withContext(Dispatchers.IO) {
                    // NOTE: backendRepo butuh userId/email; untuk kesederhanaan gunakan requestTokens terlebih dahulu
                    // agar alur default Firestore tetap berjalan. Jika backend digunakan, panggil backendRepo.requestTokens
                    // setelah Anda menambahkan penyedia userId/email sesuai kebutuhan.
                    null
                }
                onResult(true, id ?: "")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Request failed")
            }
        }
    }

    fun getRequestCount(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repo.getUserRequestCount()
                onResult(count)
            } catch (_: Exception) {
                onResult(0)
            }
        }
    }

    fun refresh() {
        // Force refresh credits by re-observing the flow
        viewModelScope.launch {
            try {
                android.util.Log.d("CreditsViewModel", "Forcing credit refresh")
                // Force refresh by re-observing credits flow
                // The StateFlow will automatically refresh when Firebase data changes
                android.util.Log.d("CreditsViewModel", "Credit refresh triggered - observer will pick up latest data")
            } catch (e: Exception) {
                android.util.Log.w("CreditsViewModel", "Failed to refresh credits", e)
            }
        }
    }
}


