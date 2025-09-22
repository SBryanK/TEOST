package com.example.teost.core.data.config

import javax.inject.Singleton

/**
 * Centralized application configuration for engine defaults, network timeouts,
 * endpoint fallbacks, and redaction policies.
 */
@Singleton
data class AppConfig(
    // Network timeouts (ms)
    val connectTimeoutMs: Long = 30_000,
    val readTimeoutMs: Long = 30_000,
    val writeTimeoutMs: Long = 30_000,
    val tcpConnectTimeoutMs: Int = 3_000,
    val udpSoTimeoutMs: Int = 2_000,

    // Default API paths used by engine when none provided by parameters
    val defaultAuthPath: String = "/api/auth",
    val defaultLoginPath: String = "/api/login",
    val defaultValidatePath: String = "/api/validate",
    val defaultUploadPath: String = "/api/upload",
    val defaultWorkflowPath: String = "/api/workflow",

    // Engine pacing/limits
    val minBurstIntervalMs: Long = 50,
    val maxConcurrentConnections: Int = 16,
    val maxCrawlerFetches: Int = 10,

    // Redaction: header keys considered sensitive (case-insensitive)
    val redactedHeaderKeys: Set<String> = setOf(
        "authorization", "cookie", "x-auth-token", "x-api-key"
    ),

    // Optional certificate pinning: host -> list of pins (sha256/BASE64 of SPKI)
    val certificatePins: Map<String, List<String>> = emptyMap(),

    // Default user id (to be replaced by session-derived value later)
    val currentUserId: String = "current_user",

    // Backend base URL for Credits approval APIs (if using custom backend)
    val creditsApiBaseUrl: String = ""
)





