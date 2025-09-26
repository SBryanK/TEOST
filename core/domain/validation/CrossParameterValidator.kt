package com.example.teost.core.domain.validation

import com.example.teost.data.model.TestParameters

object CrossParameterValidator {
    
    fun validateCrossParameters(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        // Burst vs Concurrency validation
        errors.addAll(validateBurstConcurrency(params))
        
        // Timeout vs Duration validation
        errors.addAll(validateTimeoutDuration(params))
        
        // RPS vs Concurrency validation
        errors.addAll(validateRpsConcurrency(params))
        
        // Request delay vs Duration validation
        errors.addAll(validateRequestDelayDuration(params))
        
        // Payload vs Body size validation
        errors.addAll(validatePayloadBodySize(params))
        
        // ID range vs Replay count validation
        errors.addAll(validateIdRangeReplay(params))
        
        return errors
    }
    
    private fun validateBurstConcurrency(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.burstRequests != null && params.concurrencyLevel != null) {
            if (params.burstRequests!! < params.concurrencyLevel!!) {
                errors.add("Burst requests (${params.burstRequests}) must be >= concurrency level (${params.concurrencyLevel})")
            }
            
            // Check for reasonable ratio
            val ratio = params.burstRequests!!.toDouble() / params.concurrencyLevel!!
            if (ratio > 100) {
                errors.add("Burst requests to concurrency ratio (${ratio.toInt()}:1) is too high, consider reducing burst requests or increasing concurrency")
            }
        }
        
        return errors
    }
    
    private fun validateTimeoutDuration(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.timeoutMs != null && params.durationSec != null) {
            val timeoutSec = params.timeoutMs!! / 1000.0
            val durationSec = params.durationSec!!.toDouble()
            
            if (timeoutSec < durationSec) {
                errors.add("Timeout (${timeoutSec}s) must be >= duration (${durationSec}s)")
            }
            
            // Check for reasonable buffer
            if (timeoutSec < durationSec * 1.2) {
                errors.add("Timeout should be at least 20% higher than duration to allow for network delays")
            }
        }
        
        return errors
    }
    
    private fun validateRpsConcurrency(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.rpsTarget != null && params.concurrencyLevel != null) {
            val rpsPerConnection = params.rpsTarget!!.toDouble() / params.concurrencyLevel!!
            
            if (rpsPerConnection > 10) {
                errors.add("RPS per connection (${rpsPerConnection.toInt()}) is too high, consider increasing concurrency or reducing RPS target")
            }
            
            if (rpsPerConnection < 0.1) {
                errors.add("RPS per connection (${rpsPerConnection}) is too low, consider reducing concurrency or increasing RPS target")
            }
        }
        
        return errors
    }
    
    private fun validateRequestDelayDuration(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.requestDelayMs != null && params.durationSec != null) {
            val totalDelayMs = params.requestDelayMs!! * params.durationSec!! * 1000
            val maxReasonableDelay = params.durationSec!! * 1000 * 0.8 // 80% of duration
            
            if (totalDelayMs > maxReasonableDelay) {
                errors.add("Total request delay (${totalDelayMs}ms) exceeds 80% of test duration (${maxReasonableDelay}ms)")
            }
        }
        
        return errors
    }
    
    private fun validatePayloadBodySize(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.payloadList != null && params.bodySizeKb != null) {
            val maxPayloadSize = params.payloadList!!.maxOfOrNull { it.length } ?: 0
            val maxBodySizeBytes = params.bodySizeKb!! * 1024
            
            if (maxPayloadSize > maxBodySizeBytes) {
                errors.add("Largest payload (${maxPayloadSize} bytes) exceeds body size limit (${maxBodySizeBytes} bytes)")
            }
            
            // Check for reasonable payload size
            if (maxPayloadSize > maxBodySizeBytes * 0.8) {
                errors.add("Payload size (${maxPayloadSize} bytes) is close to body size limit (${maxBodySizeBytes} bytes), consider increasing body size")
            }
        }
        
        return errors
    }
    
    private fun validateIdRangeReplay(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        if (params.idRange != null && params.replayCount != null) {
            if (params.idRange!!.size >= 2) {
                val rangeSize = params.idRange!![1] - params.idRange!![0] + 1
                val totalRequests = rangeSize * params.replayCount!!
                
                if (totalRequests > 10000) {
                    errors.add("Total requests (${totalRequests}) would exceed 10,000 limit, consider reducing ID range or replay count")
                }
                
                if (totalRequests < 10) {
                    errors.add("Total requests (${totalRequests}) is very low, consider increasing ID range or replay count")
                }
            }
        }
        
        return errors
    }
    
    fun validateParameterConsistency(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        // Check for conflicting parameters
        if (params.burstRequests != null && params.rpsTarget != null) {
            errors.add("Cannot specify both burst requests and RPS target - choose one approach")
        }
        
        if (params.concurrentConnections != null && params.concurrencyLevel != null) {
            errors.add("Cannot specify both concurrent connections and concurrency level - choose one approach")
        }
        
        // Check for missing required combinations
        if (params.burstRequests != null && params.burstIntervalMs == null) {
            errors.add("Burst interval is required when specifying burst requests")
        }
        
        if (params.rpsTarget != null && params.sustainedRpsWindow == null) {
            errors.add("Sustained RPS window is required when specifying RPS target")
        }
        
        return errors
    }
    
    fun validateSecurityConstraints(params: TestParameters): List<String> {
        val errors = mutableListOf<String>()
        
        // Check for potentially dangerous configurations
        if (params.burstRequests != null && params.burstRequests!! > 500) {
            errors.add("High burst request count (${params.burstRequests}) may trigger aggressive rate limiting")
        }
        
        if (params.concurrentConnections != null && params.concurrentConnections!! > 50) {
            errors.add("High concurrent connections (${params.concurrentConnections}) may cause connection issues")
        }
        
        if (params.durationSec != null && params.durationSec!! > 120) {
            errors.add("Long test duration (${params.durationSec}s) may impact target server performance")
        }
        
        if (params.requestDelayMs != null && params.requestDelayMs!! < 50) {
            errors.add("Very low request delay (${params.requestDelayMs}ms) may be considered aggressive")
        }
        
        return errors
    }
    
    fun getValidationSummary(params: TestParameters): ValidationSummary {
        val crossErrors = validateCrossParameters(params)
        val consistencyErrors = validateParameterConsistency(params)
        val securityWarnings = validateSecurityConstraints(params)
        
        return ValidationSummary(
            isValid = crossErrors.isEmpty() && consistencyErrors.isEmpty(),
            errors = crossErrors + consistencyErrors,
            warnings = securityWarnings,
            severity = when {
                crossErrors.isNotEmpty() || consistencyErrors.isNotEmpty() -> ValidationSeverity.ERROR
                securityWarnings.isNotEmpty() -> ValidationSeverity.WARNING
                else -> ValidationSeverity.INFO
            }
        )
    }
    
    data class ValidationSummary(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val severity: ValidationSeverity
    )
    
    enum class ValidationSeverity {
        INFO, WARNING, ERROR
    }
}
