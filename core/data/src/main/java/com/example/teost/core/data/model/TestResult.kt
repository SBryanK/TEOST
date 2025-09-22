package com.example.teost.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.teost.data.local.Converters
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "test_results",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["startTime"]),
        Index(value = ["domain"]),
        Index(value = ["status"]),
        Index(value = ["category"])
    ]
)
@TypeConverters(Converters::class)
@Parcelize
data class TestResult(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val testId: String,
    val testName: String,
    val category: TestCategory,
    val type: TestType,
    val domain: String,
    val ipAddress: String? = null,
    val status: TestStatus,
    val startTime: Date,
    val endTime: Date,
    val duration: Long, // in milliseconds
    val creditsUsed: Int,
    val resultDetails: TestResultDetails,
    val rawLogs: String? = null,
    val errorMessage: String? = null,
    val userId: String
) : Parcelable

@Parcelize
enum class TestStatus : Parcelable {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    TIMEOUT,
    BLOCKED
}

@Parcelize
data class TestResultDetails(
    // Network Metrics
    val statusCode: Int? = null,
    val headers: Map<String, String>? = null,
    val responseTime: Long? = null, // in milliseconds
    val requestId: String? = null,
    val dnsResolutionTime: Long? = null,
    val tcpHandshakeTime: Long? = null,
    val sslHandshakeTime: Long? = null,
    val ttfb: Long? = null, // Time to first byte
    val downloadTime: Long? = null,
    val totalResponseTime: Long? = null,
    
    // Security Metrics
    val blockedByWaf: Boolean = false,
    val challengeDetected: ChallengeType? = null,
    val retryAfterHeader: String? = null,
    val blockHeaders: List<String>? = null,
    val blockedIteration: Int? = null,
    val totalIterations: Int? = null,
    val blockReason: String? = null,
    val encryptedBody: String? = null,
    val encryptionAlgorithm: String? = null,
    
    // Performance Metrics
    val requestsPerSecond: Double? = null,
    val successRate: Double? = null,
    val errorRate: Double? = null,
    val latencyP50: Long? = null,
    val latencyP95: Long? = null,
    val latencyP99: Long? = null,
    val totalRequests: Int? = null,
    val failedRequests: Int? = null,
    val timeouts: Int? = null,
    
    // DoS Metrics
    val connectionsEstablished: Int? = null,
    val connectionsFailed: Int? = null,
    val connectionsReset: Int? = null,
    val packetsLost: Int? = null,
    
    // WAF Metrics
    val payloadsBlocked: List<String>? = null,
    val payloadsPassed: List<String>? = null,
    val wafRuleTriggered: String? = null,
    val wafScore: Int? = null,
    
    // Bot Management Metrics
    val botDetected: Boolean = false,
    val botScore: Int? = null,
    val fingerprintId: String? = null,
    val jsChallengePassed: Boolean? = null,
    val captchaRequired: Boolean? = null,
    
    // Block Detection & Security Analysis
    val blockingMethod: String? = null, // WAF, Bot Detection, Rate Limiting, etc.
    val securityEffectiveness: Double? = null, // Percentage of blocked requests
    val trafficAnalysis: String? = null, // Detailed analysis of when/how traffic was blocked
    
    // Network Diagnosis Logs
    val networkLogs: List<String>? = null, // ✅ ADD NETWORK LOGS FIELD
    
    // Parameters Snapshot
    val paramsSnapshot: TestParameters? = null
) : Parcelable

@Parcelize
enum class ChallengeType : Parcelable {
    CAPTCHA,
    JAVASCRIPT,
    RATE_LIMIT,
    GEO_BLOCK,
    IP_BLOCK,
    USER_AGENT_BLOCK
}

@Parcelize
data class ConnectionTestResult(
    val url: String,
    val domain: String,
    val ipAddresses: List<String>,
    val statusCode: Int,
    val httpProtocol: String? = null,
    val statusMessage: String? = null,
    val headers: Map<String, String>,
    val responseTime: Long,
    val requestId: String?,
    val isSuccessful: Boolean,
    val errorMessage: String? = null,
    val timestamp: Date = Date(),
    val dnsTime: Long? = null,
    val tcpHandshakeTime: Long? = null,
    val sslHandshakeTime: Long? = null,
    val ttfb: Long? = null,
    // Optional encrypted body details (client-side encryption for audit)
    val encryptionAlgorithm: String? = null,
    val encryptionKey: String? = null,
    val encryptionIv: String? = null,
    val encryptedBody: String? = null,
    // Network diagnosis (in-memory only, not persisted to Room)
    val logs: List<String>? = null
) : Parcelable
