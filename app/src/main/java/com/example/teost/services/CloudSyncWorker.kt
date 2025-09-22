package com.example.teost.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.teost.data.repository.HistoryRepository
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val historyRepository: com.example.teost.data.repository.HistoryRepository,
    private val testResultDao: TestResultDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.success()
        val uid = user.uid
        android.util.Log.d("CloudSyncWorker", "Starting cloud sync for user: $uid")
        
        try {
            // First, check if local database is empty and restore from Firestore if needed
            val localCount = testResultDao.getTotalTestCount(uid)
            android.util.Log.d("CloudSyncWorker", "Local database has $localCount test results")
            
            if (localCount == 0) {
                android.util.Log.d("CloudSyncWorker", "Local database empty, attempting to restore from Firestore")
                restoreHistoryFromFirestore(uid)
            }
            
            // Then, sync local data to Firestore
            syncLocalToFirestore(uid)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("CloudSyncWorker", "Cloud sync failed", e)
            Result.retry()
        }
    }
    
    private suspend fun restoreHistoryFromFirestore(uid: String) {
        try {
            val col = firestore.collection("users").document(uid).collection("history")
            val snapshot = col.get().await()
            android.util.Log.d("CloudSyncWorker", "Found ${snapshot.size()} test results in Firestore")
            
            val restoredResults = mutableListOf<TestResult>()
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    val details = data["details"] as? Map<String, Any?> ?: emptyMap()
                    
                    val testResult = TestResult(
                        id = data["id"] as? String ?: doc.id,
                        testId = data["testId"] as? String ?: "",
                        testName = data["testName"] as? String ?: "Unknown Test",
                        category = TestCategory.valueOf(data["category"] as? String ?: "DDOS_PROTECTION"),
                        type = parseTestType(data["type"] as? String ?: "HttpSpike"),
                        domain = data["domain"] as? String ?: "",
                        ipAddress = data["ipAddress"] as? String,
                        status = TestStatus.valueOf(data["status"] as? String ?: "SUCCESS"),
                        startTime = (data["startTime"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        endTime = (data["endTime"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        duration = (data["duration"] as? Number)?.toLong() ?: 0L,
                        creditsUsed = (data["creditsUsed"] as? Number)?.toInt() ?: 1,
                        userId = uid,
                        resultDetails = TestResultDetails(
                            statusCode = (details["statusCode"] as? Number)?.toInt(),
                            headers = details["headers"] as? Map<String, String>,
                            responseTime = (details["responseTime"] as? Number)?.toLong(),
                            requestId = details["requestId"] as? String,
                            dnsResolutionTime = (details["dnsResolutionTime"] as? Number)?.toLong(),
                            tcpHandshakeTime = (details["tcpHandshakeTime"] as? Number)?.toLong(),
                            sslHandshakeTime = (details["sslHandshakeTime"] as? Number)?.toLong(),
                            ttfb = (details["ttfb"] as? Number)?.toLong()
                        )
                    )
                    restoredResults.add(testResult)
                } catch (e: Exception) {
                    android.util.Log.w("CloudSyncWorker", "Failed to restore result from doc ${doc.id}", e)
                }
            }
            
            if (restoredResults.isNotEmpty()) {
                testResultDao.insertTestResults(restoredResults)
                android.util.Log.d("CloudSyncWorker", "Restored ${restoredResults.size} test results from Firestore")
                historyRepository.notifyChanged()
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSyncWorker", "Failed to restore history from Firestore", e)
        }
    }
    
    private suspend fun syncLocalToFirestore(uid: String) {
        try {
            val all = historyRepository.listFilteredAdvanced(
                userId = uid,
                category = null,
                status = null,
                type = null,
                from = null,
                to = null,
                domain = null
            )
            if (all.isEmpty()) {
                android.util.Log.d("CloudSyncWorker", "No local data to sync")
                return
            }
            
            android.util.Log.d("CloudSyncWorker", "Syncing ${all.size} test results to Firestore")
            val batch = firestore.batch()
            val col = firestore.collection("users").document(uid).collection("history")
            
            all.forEach { r ->
                val doc = col.document(r.id)
                val d = r.resultDetails
                val map = hashMapOf<String, Any?>(
                    "id" to r.id,
                    "testId" to r.testId,
                    "testName" to r.testName,
                    "category" to r.category.name,
                    "type" to r.type.toString(),
                    "domain" to r.domain,
                    "ipAddress" to r.ipAddress,
                    "status" to r.status.name,
                    "startTime" to r.startTime,
                    "endTime" to r.endTime,
                    "duration" to r.duration,
                    "creditsUsed" to r.creditsUsed,
                    "userId" to r.userId,
                    "details" to hashMapOf(
                        "statusCode" to d.statusCode,
                        "headers" to d.headers,
                        "responseTime" to d.responseTime,
                        "requestId" to d.requestId,
                        "dnsResolutionTime" to d.dnsResolutionTime,
                        "tcpHandshakeTime" to d.tcpHandshakeTime,
                        "sslHandshakeTime" to d.sslHandshakeTime,
                        "ttfb" to d.ttfb,
                        "encryptedBody" to d.encryptedBody,
                        "encryptionAlgorithm" to d.encryptionAlgorithm
                    )
                )
                batch.set(doc, map)
            }
            batch.commit().await()
            android.util.Log.d("CloudSyncWorker", "Successfully synced ${all.size} results to Firestore")
        } catch (e: Exception) {
            android.util.Log.e("CloudSyncWorker", "Failed to sync to Firestore", e)
        }
    }
    
    private fun parseTestType(typeString: String): TestType {
        return when (typeString) {
            "HttpSpike" -> TestType.HttpSpike
            "ConnectionFlood" -> TestType.ConnectionFlood
            "SqlInjection" -> TestType.SqlInjection
            "XssTest" -> TestType.XssTest
            "PathTraversal" -> TestType.PathTraversal
            "CustomRulesValidation" -> TestType.CustomRulesValidation
            "OversizedBody" -> TestType.OversizedBody
            "UserAgentAnomaly" -> TestType.UserAgentAnomaly
            "CookieJsChallenge" -> TestType.CookieJsChallenge
            "BruteForce" -> TestType.BruteForce
            "EnumerationIdor" -> TestType.EnumerationIdor
            "SchemaInputValidation" -> TestType.SchemaInputValidation
            "BusinessLogicAbuse" -> TestType.BusinessLogicAbuse
            else -> TestType.HttpSpike // Default fallback
        }
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it, onCancellation = null) }
        addOnFailureListener { e -> cont.cancel(e) }
    }
}


