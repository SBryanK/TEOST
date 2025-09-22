package com.example.teost.core.domain.model

data class TestRequest(
    val id: String,
    val url: String
)

data class TestResponse(
    val id: String,
    val url: String,
    val responseCode: Int,
    val responseMessage: String?,
    val httpProtocol: String?,
    val headers: Map<String, String>,
    val responseTimeMs: Long,
    val encryptionAlgorithm: String? = null,
    val encryptionKey: String? = null,
    val encryptionIv: String? = null,
    val encryptedBody: String? = null
)


