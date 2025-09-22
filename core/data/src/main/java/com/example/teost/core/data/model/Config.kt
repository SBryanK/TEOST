package com.example.teost.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val network_protection: NetworkProtectionConfig? = null,
    val web_protection: WebProtectionConfig? = null,
    val bot_management: BotManagementConfig? = null,
    val api_protection: ApiProtectionConfig? = null
)

@Serializable
data class NetworkProtectionConfig(
    val burstRequests: Int? = null,
    val burstIntervalMs: Int? = null,
    val concurrencyLevel: Int? = null,
    val targetPath: String? = null,
    val customHeaders: Map<String, String>? = null,
    val targetDomain: String? = null,
    val httpMethod: String? = null,
    val durationSec: Int? = null,
    val rampUpSec: Int? = null
)

@Serializable
data class WebProtectionConfig(
    val payloadList: List<String>? = null,
    val encodingMode: String? = null, // URL_ENCODE, BASE64, etc
    val httpMethod: String? = null, // GET, POST
    val injectionPoint: String? = null, // QUERY_PARAM, PATH_PARAM, HEADER, BODY
    val targetParam: String? = null,
    val targetDomain: String? = null,
    val targetPath: String? = null,
    val queryParams: Map<String, String>? = null,
    val customHeaders: Map<String, String>? = null,
    val bodyTemplate: String? = null
)

@Serializable
data class BotManagementConfig(
    val uaProfiles: List<String>? = null,
    val headerMinimal: Boolean? = null,
    val acceptLanguage: String? = null,
    val cookiePolicy: String? = null, // DISABLED, ENABLED
    val targetDomain: String? = null,
    val followRedirects: Boolean? = null,
    val crawlDepth: Int? = null,
    val respectRobotsTxt: Boolean? = null
)

@Serializable
data class ApiProtectionConfig(
    val baseUrl: String? = null,
    val endpoints: List<ApiEndpointConfig>? = null,
    val authHeader: String? = null
)

@Serializable
data class ApiEndpointConfig(
    val path: String,
    val method: String = "GET",
    val headers: Map<String, String>? = null,
    val body: String? = null,
    val targetDomain: String? = null,
    val queryParams: Map<String, String>? = null,
    val payloadList: List<String>? = null,
    val requestDelayMs: Int? = null,
    val concurrency: Int? = null,
    val durationSec: Int? = null,
    val username: String? = null,
    val idRange: List<Long>? = null,
    val stepSize: Int? = null
)



