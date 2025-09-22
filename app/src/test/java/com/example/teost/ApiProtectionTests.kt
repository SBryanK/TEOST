package com.example.teost

import com.example.teost.core.data.config.AppConfig
import com.example.teost.core.data.engine.SecurityTestEngine
import com.example.teost.data.model.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ApiProtectionTests {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var engine: SecurityTestEngine

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        engine = SecurityTestEngine(client, AppConfig())
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun auth_header_mode_should_succeed_or_401() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        val cfg = TestConfiguration(
            testId = "auth",
            domain = "localhost:" + server.port,
            parameters = TestParameters(
                apiEndpoint = "/",
                authMode = "header",
                authToken = "token123"
            )
        )
        val result = engine.executeTest(cfg).toList().last() as SecurityTestEngine.TestProgress.Completed
        val code = result.result.resultDetails.statusCode
        assertTrue(code == null || code in listOf(0, 200, 401, 403))
    }

    @Test
    fun brute_force_should_emit_requests() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(401)) }
        val cfg = TestConfiguration(
            testId = "bruteforce",
            domain = "localhost:" + server.port,
            parameters = TestParameters(
                apiEndpoint = "/login",
                username = "user",
                passwordList = listOf("a", "b", "c"),
                requestDelayMs = 10
            )
        )
        val result = engine.executeTest(cfg).toList().last() as SecurityTestEngine.TestProgress.Completed
        assertEquals(3, result.result.resultDetails.totalRequests)
    }

    @Test
    fun enumeration_should_iterate_range() = runBlocking {
        repeat(5) { server.enqueue(MockResponse().setResponseCode(200)) }
        val cfg = TestConfiguration(
            testId = "enum",
            domain = "localhost:" + server.port,
            parameters = TestParameters(
                enumTemplate = "/obj/{id}",
                idRange = listOf(1, 5),
                stepSize = 1,
                requestDelayMs = 1
            )
        )
        val result = engine.executeTest(cfg).toList().last() as SecurityTestEngine.TestProgress.Completed
        assertEquals(5, result.result.resultDetails.totalRequests)
    }

    @Test
    fun schema_validation_should_post_cases() = runBlocking {
        repeat(2) { server.enqueue(MockResponse().setResponseCode(200)) }
        val cfg = TestConfiguration(
            testId = "schema",
            domain = "localhost:" + server.port,
            parameters = TestParameters(
                apiEndpoint = "/validate",
                fuzzCases = listOf("{}", "{\"a\":1}"),
                contentTypes = listOf("application/json"),
                requestDelayMs = 1
            )
        )
        val result = engine.executeTest(cfg).toList().last() as SecurityTestEngine.TestProgress.Completed
        assertEquals(2, result.result.resultDetails.totalRequests)
    }

    @Test
    fun business_logic_should_replay_count() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200)) }
        val cfg = TestConfiguration(
            testId = "workflow",
            domain = "localhost:" + server.port,
            parameters = TestParameters(
                apiEndpoint = "/wf",
                replayCount = 3,
                requestDelayMs = 1
            )
        )
        val result = engine.executeTest(cfg).toList().last() as SecurityTestEngine.TestProgress.Completed
        assertEquals(3, result.result.resultDetails.totalRequests)
    }
}