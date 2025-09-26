package com.example.teost.core.domain.validation

import com.example.teost.data.model.TestParameters

object ParameterValidator {
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
    
    fun validateTestParameters(testType: String, params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        // Basic parameter validation
        errors.addAll(validateBasicParameters(params))
        
        // Test-specific validation
        errors.addAll(validateTestSpecificParameters(testType, params))
        
        return errors
    }
    
    private fun validateBasicParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        // Timeout validation
        if (params.timeoutMs != null) {
            when {
                params.timeoutMs!! <= 0 -> errors.add("Timeout must be greater than 0")
                params.timeoutMs!! > 300000 -> errors.add("Timeout cannot exceed 5 minutes (300,000ms)")
                params.timeoutMs!! < 1000 -> errors.add("Timeout should be at least 1 second (1,000ms)")
            }
        }
        
        // Request delay validation
        if (params.requestDelayMs != null) {
            when {
                params.requestDelayMs!! < 0 -> errors.add("Request delay cannot be negative")
                params.requestDelayMs!! > 10000 -> errors.add("Request delay cannot exceed 10 seconds")
            }
        }
        
        return errors
    }
    
    private fun validateTestSpecificParameters(testType: String, params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        when (testType) {
            "HttpSpike" -> {
                errors.addAll(validateHttpSpikeParameters(params))
            }
            "ConnectionFlood" -> {
                errors.addAll(validateConnectionFloodParameters(params))
            }
            "BruteForce" -> {
                errors.addAll(validateBruteForceParameters(params))
            }
            "UserAgentAnomaly" -> {
                errors.addAll(validateUserAgentParameters(params))
            }
            "EnumerationIdor" -> {
                errors.addAll(validateEnumerationParameters(params))
            }
            "SchemaInputValidation" -> {
                errors.addAll(validateSchemaValidationParameters(params))
            }
            "BusinessLogicAbuse" -> {
                errors.addAll(validateBusinessLogicParameters(params))
            }
            "SqlInjection", "XssTest", "PathTraversal", "CommandInjection" -> {
                errors.addAll(validateWafTestParameters(params))
            }
            "OversizedBody" -> {
                errors.addAll(validateOversizedBodyParameters(params))
            }
        }
        
        return errors
    }
    
    private fun validateHttpSpikeParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.burstRequests != null) {
            when {
                params.burstRequests!! <= 0 -> errors.add("Burst requests must be greater than 0")
                params.burstRequests!! > 1000 -> errors.add("Burst requests cannot exceed 1000")
                params.burstRequests!! < 1 -> errors.add("Burst requests must be at least 1")
            }
        }
        
        if (params.burstIntervalMs != null) {
            when {
                params.burstIntervalMs!! <= 0 -> errors.add("Burst interval must be greater than 0")
                params.burstIntervalMs!! > 60000 -> errors.add("Burst interval cannot exceed 60 seconds")
                params.burstIntervalMs!! < 10 -> errors.add("Burst interval must be at least 10ms")
            }
        }
        
        if (params.concurrencyLevel != null) {
            when {
                params.concurrencyLevel!! <= 0 -> errors.add("Concurrency level must be greater than 0")
                params.concurrencyLevel!! > 50 -> errors.add("Concurrency level cannot exceed 50")
                params.concurrencyLevel!! < 1 -> errors.add("Concurrency level must be at least 1")
            }
        }
        
        if (params.sustainedRpsWindow != null) {
            when {
                params.sustainedRpsWindow!! <= 0 -> errors.add("Sustained RPS window must be greater than 0")
                params.sustainedRpsWindow!! > 300 -> errors.add("Sustained RPS window cannot exceed 5 minutes")
            }
        }
        
        return errors
    }
    
    private fun validateConnectionFloodParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.concurrentConnections != null) {
            when {
                params.concurrentConnections!! <= 0 -> errors.add("Concurrent connections must be greater than 0")
                params.concurrentConnections!! > 100 -> errors.add("Concurrent connections cannot exceed 100")
                params.concurrentConnections!! < 1 -> errors.add("Concurrent connections must be at least 1")
            }
        }
        
        if (params.durationSec != null) {
            when {
                params.durationSec!! <= 0 -> errors.add("Duration must be greater than 0")
                params.durationSec!! > 300 -> errors.add("Duration cannot exceed 5 minutes")
                params.durationSec!! < 1 -> errors.add("Duration must be at least 1 second")
            }
        }
        
        if (params.rpsTarget != null) {
            when {
                params.rpsTarget!! <= 0 -> errors.add("RPS target must be greater than 0")
                params.rpsTarget!! > 1000 -> errors.add("RPS target cannot exceed 1000")
            }
        }
        
        return errors
    }
    
    private fun validateBruteForceParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.username.isNullOrBlank()) {
            errors.add("Username is required for brute force test")
        }
        
        if (params.passwordList.isNullOrEmpty()) {
            errors.add("Password list is required for brute force test")
        } else {
            if (params.passwordList!!.size > 100) {
                errors.add("Password list cannot exceed 100 entries")
            }
            if (params.passwordList!!.any { it.isBlank() }) {
                errors.add("Password list cannot contain empty passwords")
            }
        }
        
        if (params.apiEndpoint.isNullOrBlank()) {
            errors.add("API endpoint is required for brute force test")
        }
        
        if (params.requestDelayMs != null && params.requestDelayMs!! < 100) {
            errors.add("Request delay should be at least 100ms for brute force test")
        }
        
        return errors
    }
    
    private fun validateUserAgentParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.uaProfiles.isNullOrEmpty()) {
            errors.add("User-Agent profiles are required for bot detection test")
        } else {
            if (params.uaProfiles!!.size > 20) {
                errors.add("User-Agent profiles cannot exceed 20 entries")
            }
            if (params.uaProfiles!!.any { it.isBlank() }) {
                errors.add("User-Agent profiles cannot contain empty entries")
            }
        }
        
        return errors
    }
    
    private fun validateEnumerationParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.enumTemplate.isNullOrBlank()) {
            errors.add("Enumeration template is required for IDOR test")
        }
        
        if (params.idRange.isNullOrEmpty() || params.idRange!!.size < 2) {
            errors.add("ID range (start and end) is required for IDOR test")
        } else {
            val range = params.idRange!!
            if (range.size != 2) {
                errors.add("ID range must contain exactly 2 values (start and end)")
            } else {
                val start = range[0]
                val end = range[1]
                if (start >= end) {
                    errors.add("ID range start must be less than end")
                }
                if (end - start > 10000) {
                    errors.add("ID range cannot exceed 10,000 entries")
                }
            }
        }
        
        return errors
    }
    
    private fun validateSchemaValidationParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.fuzzCases.isNullOrEmpty()) {
            errors.add("Fuzz cases are required for schema validation test")
        } else {
            if (params.fuzzCases!!.size > 50) {
                errors.add("Fuzz cases cannot exceed 50 entries")
            }
        }
        
        if (params.contentTypes.isNullOrEmpty()) {
            errors.add("Content types are required for schema validation test")
        }
        
        return errors
    }
    
    private fun validateBusinessLogicParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.replayCount != null) {
            when {
                params.replayCount!! <= 0 -> errors.add("Replay count must be greater than 0")
                params.replayCount!! > 100 -> errors.add("Replay count cannot exceed 100")
            }
        }
        
        if (params.requestDelayMs != null && params.requestDelayMs!! < 50) {
            errors.add("Request delay should be at least 50ms for business logic test")
        }
        
        return errors
    }
    
    private fun validateWafTestParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.payloadList.isNullOrEmpty()) {
            errors.add("Payload list is required for WAF test")
        } else {
            if (params.payloadList!!.size > 100) {
                errors.add("Payload list cannot exceed 100 entries")
            }
            if (params.payloadList!!.any { it.isBlank() }) {
                errors.add("Payload list cannot contain empty payloads")
            }
        }
        
        return errors
    }
    
    private fun validateOversizedBodyParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.bodySizeKb != null) {
            when {
                params.bodySizeKb!! <= 0 -> errors.add("Body size must be greater than 0")
                params.bodySizeKb!! > 10240 -> errors.add("Body size cannot exceed 10MB (10,240KB)")
                params.bodySizeKb!! < 1 -> errors.add("Body size must be at least 1KB")
            }
        }
        
        if (params.jsonFieldCount != null) {
            when {
                params.jsonFieldCount!! <= 0 -> errors.add("JSON field count must be greater than 0")
                params.jsonFieldCount!! > 1000 -> errors.add("JSON field count cannot exceed 1000")
            }
        }
        
        return errors
    }
}
