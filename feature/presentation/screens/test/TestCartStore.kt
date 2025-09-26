package com.example.teost.presentation.screens.test

import com.example.teost.data.model.TestConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory cart store for queued test configurations.
 * Keeps the feature module compiling and the UI functional.
 */
object TestCartStore {
    private val _items: MutableStateFlow<List<TestConfiguration>> = MutableStateFlow(emptyList())
    val items: StateFlow<List<TestConfiguration>> = _items.asStateFlow()
    
    // ✅ Add error state for better error handling
    private val _errorState: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private var persistence: CartPersistence? = null

    fun attachPersistence(p: CartPersistence) {
        persistence = p
    }

    fun setAll(newItems: List<TestConfiguration>) {
        _items.value = newItems
        persistence?.persist(newItems)
    }

    fun add(configuration: TestConfiguration): Boolean {
        try {
            // ✅ Clear previous errors
            _errorState.value = null
            
            // ✅ Validate configuration
            val validationResult = validateConfiguration(configuration)
            if (!validationResult.isValid) {
                _errorState.value = validationResult.errorMessage
                return false
            }
            
            // Check for duplicates based on domain, category, type, and key parameters
            val existing = _items.value.find { existing ->
                isDuplicate(existing, configuration)
            }
            
            return if (existing != null) {
                // Duplicate found, don't add
                _errorState.value = "Test configuration already exists for domain: ${configuration.domain}"
                false
            } else {
                // No duplicate, add to cart
                val next = _items.value + configuration
                _items.value = next
                try {
                    persistence?.persist(next)
                } catch (e: Exception) {
                    _errorState.value = "Failed to persist cart: ${e.message}"
                    // Revert the change
                    _items.value = _items.value.dropLast(1)
                    return false
                }
                true
            }
        } catch (e: Exception) {
            _errorState.value = "Failed to add configuration: ${e.message}"
            return false
        }
    }
    
    fun addOrUpdate(configuration: TestConfiguration): Boolean {
        try {
            // ✅ Clear previous errors
            _errorState.value = null
            
            // ✅ Validate configuration
            val validationResult = validateConfiguration(configuration)
            if (!validationResult.isValid) {
                _errorState.value = validationResult.errorMessage
                return false
            }
            
            val existingIndex = _items.value.indexOfFirst { existing ->
                isDuplicate(existing, configuration)
            }
            
            return if (existingIndex >= 0) {
                // ✅ Update existing configuration with validation
                val current = _items.value.toMutableList()
                val existing = current[existingIndex]
                
                // ✅ Validate update
                val updateValidation = validateUpdate(existing, configuration)
                if (!updateValidation.isValid) {
                    _errorState.value = updateValidation.errorMessage
                    return false
                }
                
                // ✅ Update with timestamp
                current[existingIndex] = configuration.copy(
                    // Add lastModified timestamp if TestConfiguration has it
                )
                _items.value = current
                
                try {
                    persistence?.persist(current)
                } catch (e: Exception) {
                    _errorState.value = "Failed to persist cart update: ${e.message}"
                    // Revert the change
                    _items.value = _items.value.toMutableList().apply { 
                        this[existingIndex] = existing 
                    }
                    return false
                }
                false // Indicates update, not add
            } else {
                // ✅ Add new configuration
                val next = _items.value + configuration
                _items.value = next
                try {
                    persistence?.persist(next)
                } catch (e: Exception) {
                    _errorState.value = "Failed to persist cart: ${e.message}"
                    // Revert the change
                    _items.value = _items.value.dropLast(1)
                    return false
                }
                true // Indicates new add
            }
        } catch (e: Exception) {
            _errorState.value = "Failed to add or update configuration: ${e.message}"
            return false
        }
    }
    
    /**
     * Add multiple test configurations from JSON template import
     * Returns number of tests successfully added
     */
    fun addMultipleFromTemplate(configurations: List<TestConfiguration>): Int {
        var addedCount = 0
        val currentItems = _items.value.toMutableList()
        
        configurations.forEach { config ->
            val existing = currentItems.find { existing ->
                isDuplicate(existing, config)
            }
            
            if (existing == null) {
                currentItems.add(config)
                addedCount++
            }
        }
        
        if (addedCount > 0) {
            _items.value = currentItems
            persistence?.persist(currentItems)
        }
        
        return addedCount
    }
    
    fun isDuplicateConfig(existing: TestConfiguration, new: TestConfiguration): Boolean {
        return isDuplicate(existing, new)
    }
    
    private fun isDuplicate(existing: TestConfiguration, new: TestConfiguration): Boolean {
        // ✅ CRITICAL FIX: Must match domain AND exact test type
        if (existing.domain != new.domain) return false
        
        val existingParams = existing.parameters
        val newParams = new.parameters
        
        // ✅ EXACT TEST TYPE MATCH - This is the key for edit mode!
        if (existingParams.testTypeHint != newParams.testTypeHint) return false
        
        // If testTypeHint matches, it's the same test type on same domain = duplicate for edit
        return true
        
        // OLD LOGIC REMOVED - was too complex and caused false positives
        // Now we use simple: same domain + same testTypeHint = duplicate (for edit/update)
    }

    fun removeAt(index: Int) {
        val current = _items.value
        val next = if (index < 0 || index >= current.size) current else current.toMutableList().also { it.removeAt(index) }
        _items.value = next
        persistence?.persist(next)
    }

    fun clear() {
        try {
            _errorState.value = null
            _items.value = emptyList()
            persistence?.persist(emptyList())
        } catch (e: Exception) {
            _errorState.value = "Failed to clear cart: ${e.message}"
        }
    }
    
    // ✅ Clear error state
    fun clearError() {
        _errorState.value = null
    }
    
    // ✅ Validation functions
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
    
    private fun validateConfiguration(configuration: TestConfiguration): ValidationResult {
        // ✅ Domain validation
        if (configuration.domain.isBlank()) {
            return ValidationResult(false, "Domain cannot be empty")
        }
        
        // ✅ Domain format validation
        if (!isValidDomain(configuration.domain)) {
            return ValidationResult(false, "Invalid domain format: ${configuration.domain}")
        }
        
        // ✅ Test ID validation
        if (configuration.testId.isBlank()) {
            return ValidationResult(false, "Test ID cannot be empty")
        }
        
        // ✅ Parameters validation
        val params = configuration.parameters
        if (params.burstRequests != null && params.burstRequests!! < 1) {
            return ValidationResult(false, "Burst requests must be at least 1")
        }
        
        if (params.rpsTarget != null && params.rpsTarget!! < 1) {
            return ValidationResult(false, "RPS target must be at least 1")
        }
        
        if (params.concurrentConnections != null && params.concurrentConnections!! < 1) {
            return ValidationResult(false, "Concurrent connections must be at least 1")
        }
        
        if (params.durationSec != null && params.durationSec!! < 1) {
            return ValidationResult(false, "Duration must be at least 1 second")
        }
        
        // ✅ Payload validation
        if (params.payloadList != null && params.payloadList!!.isEmpty()) {
            return ValidationResult(false, "Payload list cannot be empty")
        }
        
        return ValidationResult(true)
    }
    
    private fun validateUpdate(existing: TestConfiguration, new: TestConfiguration): ValidationResult {
        // ✅ Basic validation first
        val basicValidation = validateConfiguration(new)
        if (!basicValidation.isValid) {
            return basicValidation
        }
        
        // ✅ Domain consistency check
        if (existing.domain != new.domain) {
            return ValidationResult(false, "Cannot change domain in update")
        }
        
        // ✅ Test type consistency check
        if (existing.parameters.testTypeHint != new.parameters.testTypeHint) {
            return ValidationResult(false, "Cannot change test type in update")
        }
        
        return ValidationResult(true)
    }
    
    private fun isValidDomain(domain: String): Boolean {
        return try {
            // Basic domain validation
            domain.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?\\.[a-zA-Z]{2,}$"))
        } catch (e: Exception) {
            false
        }
    }

    interface CartPersistence {
        fun persist(items: List<TestConfiguration>)
    }
}


