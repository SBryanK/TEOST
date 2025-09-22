package com.example.teost

import com.example.teost.util.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test
    fun parseMultipleInputs_supportsSeparators() {
        val input = "example.com, a.com; b.com|c.com\nhttps://d.com\r\n1.1.1.1"
        val list = UrlValidator.parseMultipleInputs(input)
        assertTrue(list.contains("example.com"))
        assertTrue(list.contains("a.com"))
        assertTrue(list.contains("b.com"))
        assertTrue(list.contains("c.com"))
        assertTrue(list.contains("https://d.com"))
        assertTrue(list.contains("1.1.1.1"))
    }

    @Test
    fun validate_normalizesDomain() {
        val res = UrlValidator.validate("example.com")
        assertTrue(res is UrlValidator.ValidationResult.Valid)
        res as UrlValidator.ValidationResult.Valid
        assertEquals("https://example.com", res.normalized.take(19))
    }

    @Test
    fun validate_acceptsIpv4() {
        val res = UrlValidator.validate("1.2.3.4:8080")
        assertTrue(res is UrlValidator.ValidationResult.Valid)
    }

    @Test
    fun validate_acceptsUrl() {
        val res = UrlValidator.validate("https://example.com/path")
        assertTrue(res is UrlValidator.ValidationResult.Valid)
    }
}


