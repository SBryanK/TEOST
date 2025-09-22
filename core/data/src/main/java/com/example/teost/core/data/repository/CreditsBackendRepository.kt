package com.example.teost.data.repository

import com.example.teost.core.data.config.AppConfig
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsBackendRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val config: AppConfig,
) {
    private val gson = Gson()

    /**
     * Creates a credit request (amount default 50) via backend API and returns requestId.
     */
    @Throws(Exception::class)
    fun requestTokens(userId: String, email: String, amount: Int = 50): String {
        val base = config.creditsApiBaseUrl.trimEnd('/')
        require(base.isNotBlank()) { "creditsApiBaseUrl is not configured" }
        val url = "$base/request"
        val payload = mapOf(
            "userId" to userId,
            "email" to email,
            "amount" to amount
        )
        val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Backend error: ${resp.code}")
            val json = resp.body?.string().orEmpty()
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(json, Map::class.java) as? Map<String, Any?> ?: emptyMap()
            val id = map["request_id"] as? String
            return id ?: throw IllegalStateException("Missing request_id")
        }
    }

    /**
     * Returns status string for a given requestId (pending/approved/rejected/expired)
     */
    @Throws(Exception::class)
    fun getStatus(requestId: String): String {
        val base = config.creditsApiBaseUrl.trimEnd('/')
        require(base.isNotBlank()) { "creditsApiBaseUrl is not configured" }
        val url = "$base/status/$requestId"
        val req = Request.Builder().url(url).get().build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Backend error: ${resp.code}")
            val json = resp.body?.string().orEmpty()
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(json, Map::class.java) as? Map<String, Any?> ?: emptyMap()
            return (map["status"] as? String) ?: "unknown"
        }
    }
}


