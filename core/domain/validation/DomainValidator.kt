package com.example.teost.core.domain.validation

import java.net.InetAddress
import java.net.URL
import java.util.regex.Pattern

object DomainValidator {
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
    
    fun validateDomain(domain: String): ValidationResult {
        return when {
            domain.isBlank() -> ValidationResult.Invalid("Domain cannot be empty")
            domain.length > 253 -> ValidationResult.Invalid("Domain too long (max 253 characters)")
            !isValidFormat(domain) -> ValidationResult.Invalid("Invalid domain format")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateIpAddress(ip: String): ValidationResult {
        return when {
            ip.isBlank() -> ValidationResult.Invalid("IP address cannot be empty")
            !isValidIp(ip) -> ValidationResult.Invalid("Invalid IP address format")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateUrl(url: String): ValidationResult {
        return when {
            url.isBlank() -> ValidationResult.Invalid("URL cannot be empty")
            !isValidUrl(url) -> ValidationResult.Invalid("Invalid URL format")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateTarget(target: String): ValidationResult {
        return when {
            target.isBlank() -> ValidationResult.Invalid("Target cannot be empty")
            isValidIp(target) -> ValidationResult.Valid
            isValidUrl(target) -> ValidationResult.Valid
            isValidHostname(target) -> ValidationResult.Valid
            else -> ValidationResult.Invalid("Invalid target format (must be IP, URL, or hostname)")
        }
    }
    
    private fun isValidFormat(domain: String): Boolean {
        // Check for valid characters
        if (!domain.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
            return false
        }
        
        // Check for consecutive dots
        if (domain.contains("..")) {
            return false
        }
        
        // Check for leading/trailing dots
        if (domain.startsWith(".") || domain.endsWith(".")) {
            return false
        }
        
        // Check for valid TLD (at least 2 characters)
        val parts = domain.split(".")
        if (parts.size < 2) {
            return false
        }
        
        val tld = parts.last()
        if (tld.length < 2 || !tld.matches(Regex("^[a-zA-Z]+$"))) {
            return false
        }
        
        return true
    }
    
    private fun isValidIp(ip: String): Boolean {
        return try {
            // Check IPv4 format
            if (ip.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
                val parts = ip.split(".")
                parts.all { part ->
                    val num = part.toIntOrNull()
                    num != null && num in 0..255
                }
            } else {
                // Check IPv6 format (basic validation)
                ip.contains(":") && ip.matches(Regex("^[0-9a-fA-F:]+$"))
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isValidHostname(hostname: String): Boolean {
        // RFC 1123 hostname validation
        val hostnamePattern = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?))*$"
        )
        return hostnamePattern.matcher(hostname).matches() && hostname.length <= 253
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            val urlObj = URL(url)
            urlObj.protocol in listOf("http", "https") && 
            urlObj.host.isNotBlank() &&
            isValidHostname(urlObj.host)
        } catch (e: Exception) {
            false
        }
    }
    
    fun parseMultipleInputs(input: String): List<String> {
        return input.split(Regex("[,;|\\n\\r]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    
    fun validateMultipleTargets(targets: List<String>): Map<String, ValidationResult> {
        return targets.associateWith { validateTarget(it) }
    }
    
    fun getValidTargets(targets: List<String>): List<String> {
        return targets.filter { validateTarget(it) is ValidationResult.Valid }
    }
    
    fun getInvalidTargets(targets: List<String>): List<String> {
        return targets.filter { validateTarget(it) is ValidationResult.Invalid }
    }
}
