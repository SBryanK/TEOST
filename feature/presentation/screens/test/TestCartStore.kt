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

    private var persistence: CartPersistence? = null

    fun attachPersistence(p: CartPersistence) {
        persistence = p
    }

    fun setAll(newItems: List<TestConfiguration>) {
        _items.value = newItems
        persistence?.persist(newItems)
    }

    fun add(configuration: TestConfiguration): Boolean {
        // Check for duplicates based on domain, category, type, and key parameters
        val existing = _items.value.find { existing ->
            isDuplicate(existing, configuration)
        }
        
        return if (existing != null) {
            // Duplicate found, don't add
            false
        } else {
            // No duplicate, add to cart
            val next = _items.value + configuration
            _items.value = next
            persistence?.persist(next)
            true
        }
    }
    
    fun addOrUpdate(configuration: TestConfiguration): Boolean {
        val existingIndex = _items.value.indexOfFirst { existing ->
            isDuplicate(existing, configuration)
        }
        
        return if (existingIndex >= 0) {
            // Update existing configuration
            val current = _items.value.toMutableList()
            current[existingIndex] = configuration
            _items.value = current
            persistence?.persist(current)
            false // Indicates update, not add
        } else {
            // Add new configuration
            val next = _items.value + configuration
            _items.value = next
            persistence?.persist(next)
            true // Indicates new add
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
        _items.value = emptyList()
        persistence?.persist(emptyList())
    }

    interface CartPersistence {
        fun persist(items: List<TestConfiguration>)
    }
}


