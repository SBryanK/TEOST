package com.example.teost.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.repository.AuthRepository
import com.example.teost.util.Resource
import com.example.teost.data.repository.CreditsRepository
import com.example.teost.data.repository.DomainRepository
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.model.Domain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val creditsRepository: CreditsRepository,
    private val preferencesManager: PreferencesManager,
    private val domainRepository: DomainRepository
) : ViewModel() {

    data class ProfileUiState(
        val userEmail: String = "",
        val userName: String = "",
        val creditsUsed: Int = 0,
        val creditsRemaining: Int = 0,
        val isLoading: Boolean = false
    )

    private val _email = MutableStateFlow(authRepository.getCurrentEmail() ?: "")
    val email: StateFlow<String> = _email

    val credits: StateFlow<CreditsRepository.UserCredits> =
        creditsRepository.observeCredits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditsRepository.UserCredits())

    // Favorites stream (domain list) for the current user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoriteDomains: Flow<List<Domain>> =
        preferencesManager.userPreferences
            .map { it.userId }
            .flatMapLatest { uid ->
                if (uid.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else domainRepository.observeFavorites(uid)
            }

    fun refreshEmail() {
        _email.value = authRepository.getCurrentEmail() ?: ""
    }

    fun logout(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
                .collect { res ->
                    when (res) {
                        is Resource.Success -> onResult(true)
                        is Resource.Error -> onResult(false)
                        else -> {}
                    }
                }
        }
    }

    fun requestMoreCredits(onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val requestId = creditsRepository.requestTokens(50)
                onDone(true, requestId)
            } catch (t: Throwable) {
                onDone(false, t.message ?: "")
            }
        }
    }
}


