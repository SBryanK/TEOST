package com.example.teost

import app.cash.turbine.test
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.model.ConnectionTestResult
import com.example.teost.data.repository.ConnectionTestRepository
import com.example.teost.data.repository.DomainRepository
import com.example.teost.feature.search.PerformTestUseCase
import com.example.teost.feature.search.SearchViewModel
import com.example.teost.util.Resource
import com.example.teost.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testConnection_happyPath_emitsSuccess() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repo = object : ConnectionTestRepository {}
        val domainRepo = object : DomainRepository {
            override suspend fun upsertFromConnectionResult(result: ConnectionTestResult, userId: String) {}
            override suspend fun setFavorite(domain: String, userId: String, favorite: Boolean) {}
            override fun observeFavorites(userId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.teost.data.model.Domain>())
        }
        val prefs = TestPrefs()
        val useCase = PerformTestUseCase { url ->
            flow {
                emit(Result.Loading)
                emit(Result.Success(PerformTestUseCase.Response(
                    url = if (url.startsWith("http")) url else "https://$url",
                    responseCode = 200,
                    responseMessage = "OK",
                    responseTimeMs = 123,
                    headers = mapOf("Server" to "unit"),
                    httpProtocol = "HTTP/2",
                    encryptionAlgorithm = null,
                    encryptionKey = null,
                    encryptionIv = null,
                    encryptedBody = null
                )))
            }
        }
        val vm = SearchViewModel(
            connectionTestRepository = repo,
            domainRepository = domainRepo,
            prefs = prefs,
            performTestUseCase = useCase
        )
        vm.testConnection("example.com")
        // Expect final state to be success
        assertTrue(vm.testState.value is Resource.Success)
        assertTrue(vm.testResults.value.isNotEmpty())
    }
}

private class TestPrefs : PreferencesManager(androidx.test.core.app.ApplicationProvider.getApplicationContext()) {
    // Inherit defaults; no-op overrides needed for this simple test
}


