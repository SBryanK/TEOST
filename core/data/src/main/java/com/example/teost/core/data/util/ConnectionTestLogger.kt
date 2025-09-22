package com.example.teost.core.data.util

import java.util.concurrent.ConcurrentHashMap

class ConnectionTestLogger(
    private val maxLinesPerTarget: Int = 200,
    private val maxBytesPerTarget: Int = 16 * 1024
) {
    private data class Buffer(
        val lines: MutableList<String> = mutableListOf(),
        var bytes: Int = 0,
        var truncated: Boolean = false
    )

    private val buffers = ConcurrentHashMap<String, Buffer>()

    fun start(targetId: String) {
        buffers[targetId] = Buffer()
        append(targetId, "=== Network Diagnosis Start ===")
        append(targetId, timestampLine("session started"))
    }

    fun append(targetId: String, line: String) {
        val b = buffers[targetId] ?: return
        if (b.truncated) return
        val isoNow = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())
        val withTs = "[$isoNow] $line"
        val sanitized = RedactionUtils.redactLogs(withTs) ?: withTs
        val inc = sanitized.toByteArray().size + 1
        if (b.lines.size + 1 > maxLinesPerTarget || b.bytes + inc > maxBytesPerTarget) {
            b.truncated = true
            b.lines.add("[truncated]")
            return
        }
        b.lines.add(sanitized)
        b.bytes += inc
    }

    fun finish(targetId: String): List<String> {
        append(targetId, timestampLine("session finished"))
        append(targetId, "=== Network Diagnosis End ===")
        return buffers[targetId]?.lines?.toList() ?: emptyList()
    }

    private fun timestampLine(msg: String): String {
        val isoNow = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())
        return "[$isoNow] $msg"
    }
}


