package com.example.teost.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val test: SecurityTest,
    val configuration: TestConfiguration,
    val quantity: Int = 1,
    val totalCredits: Int = test.creditCost * quantity,
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class Cart(
    val items: List<CartItem> = emptyList(),
    val totalItems: Int = items.sumOf { it.quantity },
    val totalCredits: Int = items.sumOf { it.totalCredits },
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun addItem(item: CartItem): Cart {
        val existingItem = items.find { 
            it.test.id == item.test.id && 
            it.configuration.domain == item.configuration.domain &&
            it.configuration.ipAddress == item.configuration.ipAddress
        }
        
        return if (existingItem != null) {
            copy(
                items = items.map {
                    if (it.id == existingItem.id) {
                        it.copy(quantity = it.quantity + 1, totalCredits = it.test.creditCost * (it.quantity + 1))
                    } else it
                },
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            copy(
                items = items + item,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    fun removeItem(itemId: String): Cart {
        return copy(
            items = items.filter { it.id != itemId },
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    fun updateQuantity(itemId: String, newQuantity: Int): Cart {
        return if (newQuantity <= 0) {
            removeItem(itemId)
        } else {
            copy(
                items = items.map {
                    if (it.id == itemId) {
                        it.copy(quantity = newQuantity, totalCredits = it.test.creditCost * newQuantity)
                    } else it
                },
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    fun clear(): Cart {
        return Cart()
    }
}
