package com.example.teost

import com.example.teost.core.data.util.ConnectionTestLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionTestLoggerTest {
    @Test
    fun truncatesWhenExceedingLimits() {
        val logger = ConnectionTestLogger(maxLinesPerTarget = 5, maxBytesPerTarget = 64)
        val id = "t1"
        logger.start(id)
        repeat(10) { i -> logger.append(id, "line-$i") }
        val lines = logger.finish(id)
        // Expect start + some lines + truncated + end markers
        assertTrue(lines.first().contains("Start"))
        assertTrue(lines.last().contains("End"))
        assertTrue(lines.any { it.contains("truncated") })
        // Never exceed configured lines
        assertTrue(lines.size <= 5)
    }
}


