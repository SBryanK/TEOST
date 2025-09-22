package com.example.teost.core.data.util

import java.util.Locale

object RedactionUtils {
    private val sensitiveHeaderKeys = setOf(
        "authorization", "proxy-authorization", "cookie", "set-cookie",
        "x-api-key", "x-api-token", "x-auth-token", "authentication",
        "access-token", "id-token", "api-key", "token"
    )

    private fun looksLikeJwt(v: String): Boolean {
        val parts = v.split('.')
        return parts.size == 3 && parts.all { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
    }

    fun redactHeaders(headers: Map<String, String>?): Map<String, String>? {
        if (headers == null) return null
        return headers
            .filterKeys { key -> key.lowercase(Locale.ROOT) !in sensitiveHeaderKeys }
            .mapValues { (_, value) ->
                val v = value.trim()
                if (v.startsWith("Bearer ", ignoreCase = true) || v.length > 128 || looksLikeJwt(v)) "REDACTED" else v
            }
    }

    fun redactLogs(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        var out = raw!!
        sensitiveHeaderKeys.forEach { key ->
            // Case-insensitive match for header lines starting with the key, allow optional spaces
            val pattern = Regex("(?im)^(?:${'$'}{Regex.escape(key)})\\s*:\\s*.*$")
            out = out.replace(pattern, "$key: [REDACTED]")
        }
        return out
    }
}


