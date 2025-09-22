package com.example.teost.presentation.screens.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.model.Cart
import com.example.teost.data.model.CartItem
import com.example.teost.data.model.SecurityTest
import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestConfiguration
import com.example.teost.data.model.TestParameters
import com.example.teost.data.model.TestType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class CartPersistenceViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel(), TestCartStore.CartPersistence {

    init {
        TestCartStore.attachPersistence(this)
    }

    fun hydrate() {
        viewModelScope.launch {
            try {
                val up = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                    prefs.userPreferences.first()
                } ?: return@launch // Exit if timeout
                
                val cart: Cart = up.cart
                val items: List<TestConfiguration> = cart.items.map { it.configuration }
                if (items.isNotEmpty()) TestCartStore.setAll(items)
            } catch (_: Exception) { /* ignore */ }
        }
    }

    override fun persist(items: List<TestConfiguration>) {
        viewModelScope.launch {
            try {
                val cartItems = items.map { cfg ->
                    // Minimal SecurityTest placeholder for persistence totals
                    val test = SecurityTest(
                        name = "Security Test",
                        category = TestCategory.WEB_PROTECTION,
                        type = TestType.CustomRulesValidation,
                        description = "",
                        creditCost = 1,
                        estimatedDuration = 1
                    )
                    CartItem(test = test, configuration = cfg)
                }
                val cart = Cart(items = cartItems)
                prefs.saveCart(cart)
            } catch (_: Exception) { /* ignore */ }
        }
    }
}

