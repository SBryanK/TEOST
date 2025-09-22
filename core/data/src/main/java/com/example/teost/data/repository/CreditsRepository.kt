package com.example.teost.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    data class UserCredits(val used: Int = 0, val remaining: Int = 0)

    fun observeCredits(): Flow<UserCredits> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(UserCredits())
            close()
            return@callbackFlow
        }
        val docRef = firestore.collection("users").document(uid).collection("meta").document("credits")
        val reg = docRef.addSnapshotListener { snap, _ ->
            val used = (snap?.getLong("used") ?: 0L).toInt()
            val remaining = (snap?.getLong("remaining") ?: 0L).toInt()
            trySend(UserCredits(used, remaining))
        }
        awaitClose { reg.remove() }
    }

    suspend fun requestTokens(amount: Int = 50): String {
        val user = auth.currentUser ?: error("Not authenticated")
        val doc = firestore.collection("token_requests").document()
        val data = mapOf(
            "id" to doc.id,
            "userId" to user.uid,
            "email" to (user.email ?: ""),
            "amount" to amount,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )
        doc.set(data).await()
        return doc.id
    }

    suspend fun getUserRequestCount(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        val snap = firestore.collection("token_requests")
            .whereEqualTo("userId", uid)
            .get().await()
        return snap.size()
    }

    // Client-side admin approval removed. Approval is handled by Cloud Functions/backend.

    suspend fun consumeCredits(amount: Int = 1): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val creditsRef = firestore.collection("users").document(uid).collection("meta").document("credits")
        return firestore.runTransaction { txn ->
            val snap = txn.get(creditsRef)
            val used = (snap.getLong("used") ?: 0L).toInt()
            val remaining = (snap.getLong("remaining") ?: 0L).toInt()
            if (remaining < amount) {
                // not enough credits
                false
            } else {
                val newRemaining = remaining - amount
                txn.set(creditsRef, mapOf(
                    "used" to (used + amount),
                    "remaining" to newRemaining
                ), com.google.firebase.firestore.SetOptions.merge())
                true
            }
        }.await()
    }
}


