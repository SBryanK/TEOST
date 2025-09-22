package com.example.teost

import com.example.teost.core.data.util.RedactionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionUtilsTest {
    @Test
    fun redactHeaders_removesSensitiveAndMasksLongValues() {
        val headers = mapOf(
            "Authorization" to "Bearer abcdefghijklmnopqrstuvwxyz0123456789",
            "Cookie" to "session=abc",
            "X-Api-Key" to "secret",
            "User-Agent" to "okhttp",
            "X-Trace-Id" to "12345"
        )
        val red = RedactionUtils.redactHeaders(headers)!!
        assertTrue(!red.containsKey("Authorization"))
        assertTrue(!red.containsKey("Cookie"))
        assertTrue(!red.containsKey("X-Api-Key"))
        assertEquals("okhttp", red["User-Agent"])
        assertEquals("12345", red["X-Trace-Id"])
    }

    @Test
    fun redactLogs_masksSensitiveHeaderLines() {
        val raw = """
            Authorization: Bearer abc
            Cookie: session=abc
            X-Api-Key: secret
            User-Agent: okhttp
        """.trimIndent()
        val out = RedactionUtils.redactLogs(raw)!!
        assertTrue(out.contains("Authorization: [REDACTED]"))
        assertTrue(out.contains("Cookie: [REDACTED]"))
        assertTrue(out.contains("X-Api-Key: [REDACTED]"))
        assertTrue(out.contains("User-Agent: okhttp"))
    }
}


