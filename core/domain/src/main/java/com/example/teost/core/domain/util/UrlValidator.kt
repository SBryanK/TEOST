package com.example.teost.util

/**
 * URL/Domain/IP validation and multi-target parsing utilities used by Search.
 * - Parsing supports comma, semicolon, pipe, and newline separators.
 * - Validation normalizes to https:// for DOMAIN/IP when scheme missing.
 */
object UrlValidator {
    fun parseMultipleInputs(input: String): List<String> {
        return input
            // Support comma, semicolon, pipe, and both LF/CRLF newlines
            .split(Regex("[,;|\\r\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun validate(input: String): ValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("Input cannot be empty")

        // Case 1: already has scheme
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return try {
                java.net.URL(trimmed)
                ValidationResult.Valid(UrlType.URL, trimmed)
            } catch (e: Exception) {
                ValidationResult.Invalid("Invalid URL format")
            }
        }

        // Case 2: no scheme provided; try normalizing with https:// while preserving path/query
        val normalized = run {
            // Bracket IPv6 literals if present and not already bracketed
            val needsBrackets = trimmed.contains(":") && !trimmed.startsWith("[") && !trimmed.contains("//")
            if (needsBrackets) "https://[$trimmed]" else "https://$trimmed"
        }
        return try {
            val url = java.net.URL(normalized)
            val host = url.host ?: ""
            val type = if (isValidIpAddress(host)) UrlType.IP_ADDRESS else UrlType.DOMAIN
            ValidationResult.Valid(type, normalized)
        } catch (e: Exception) {
            // Fallback to previous validators for clearer message
            when {
                isValidIpAddress(trimmed) -> ValidationResult.Valid(UrlType.IP_ADDRESS, "https://$trimmed")
                isValidDomain(trimmed) -> ValidationResult.Valid(UrlType.DOMAIN, "https://$trimmed")
                else -> ValidationResult.Invalid("Please enter a valid URL, domain, or IP address")
            }
        }
    }

    fun isValidDomain(input: String): Boolean {
        if (!input.contains('.')) return false
        val domainPattern = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
        return input.matches(Regex(domainPattern))
    }

    fun isValidIpAddress(input: String): Boolean {
        // IPv4 with optional :port
        val ipv4 = Regex("^(?:25[0-5]|2[0-4]\\d|1?\\d{1,2})(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d{1,2})){3}(?::\\d{1,5})?$")
        // Simple IPv6 (not exhaustive) with optional brackets and :port
        val ipv6 = Regex("^\\[?[0-9a-fA-F:]+]?(?::\\d{1,5})?$")
        return ipv4.matches(input) || ipv6.matches(input)
    }

    sealed class ValidationResult {
        data class Valid(val type: UrlType, val normalized: String) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    enum class UrlType {
        URL,
        DOMAIN,
        IP_ADDRESS
    }
}


