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
        android.util.Log.d("CloudSyncWorker", "Starting atomic cloud sync for user: $uid")
        
        try {
            // ✅ Atomic sync operation with conflict resolution
            val syncResult = performAtomicSync(uid)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    android.util.Log.i("CloudSyncWorker", "Atomic sync completed successfully")
                    Result.success()
                }
                is SyncResult.Conflict -> {
                    android.util.Log.w("CloudSyncWorker", "Sync conflict resolved: ${syncResult.message}")
                    Result.success()
                }
                is SyncResult.Error -> {
                    android.util.Log.e("CloudSyncWorker", "Sync failed: ${syncResult.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSyncWorker", "Cloud sync failed", e)
            Result.retry()
        }
    }
    
    // ✅ Atomic sync with conflict resolution
    private suspend fun performAtomicSync(uid: String): SyncResult {
        return try {
            // Step 1: Get local data with timestamps
            val localData = getLocalDataWithTimestamps(uid)
            android.util.Log.d("CloudSyncWorker", "Local data: ${localData.size} items")
            
            // Step 2: Get remote data with timestamps
            val remoteData = getRemoteDataWithTimestamps(uid)
            android.util.Log.d("CloudSyncWorker", "Remote data: ${remoteData.size} items")
            
            // Step 3: Resolve conflicts and determine sync strategy
            val syncStrategy = resolveConflicts(localData, remoteData)
            android.util.Log.d("CloudSyncWorker", "Sync strategy: ${syncStrategy.operations.size} operations")
            
            // Step 4: Execute atomic operations
            executeAtomicOperations(uid, syncStrategy)
            
            SyncResult.Success("Sync completed successfully")
        } catch (e: Exception) {
            SyncResult.Error("Sync failed: ${e.message}")
        }
    }
    
    // ✅ Data structures for atomic sync
    private data class LocalDataItem(
        val result: TestResult,
        val lastModified: Long,
        val version: Int
    )
    
    private data class RemoteDataItem(
        val id: String,
        val data: Map<String, Any?>,
        val lastModified: Long,
        val version: Int
    )
    
    private data class SyncOperation(
        val type: OperationType,
        val localItem: LocalDataItem? = null,
        val remoteItem: RemoteDataItem? = null,
        val resolvedData: Map<String, Any?>? = null
    )
    
    private enum class OperationType {
        UPLOAD_LOCAL,      // Upload local to remote
        DOWNLOAD_REMOTE,   // Download remote to local
        RESOLVE_CONFLICT,  // Resolve conflict with strategy
        DELETE_REMOTE,     // Delete from remote
        DELETE_LOCAL       // Delete from local
    }
    
    private data class SyncStrategy(
        val operations: List<SyncOperation>
    )
    
    private sealed class SyncResult {
        data class Success(val message: String) : SyncResult()
        data class Conflict(val message: String) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
    
    // ✅ Implementation of atomic sync methods
    private suspend fun getLocalDataWithTimestamps(uid: String): List<LocalDataItem> {
        val localResults = historyRepository.listFilteredAdvanced(
            userId = uid,
            category = null,
            status = null,
            type = null,
            from = null,
            to = null,
            domain = null
        )
        
        return localResults.map { result ->
            LocalDataItem(
                result = result,
                lastModified = result.endTime.time,
                version = 1 // TODO: Add version field to TestResult
            )
        }
    }
    
    private suspend fun getRemoteDataWithTimestamps(uid: String): List<RemoteDataItem> {
        val col = firestore.collection("users").document(uid).collection("history")
        val snapshot = col.get().await()
        
        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                RemoteDataItem(
                    id = doc.id,
                    data = data,
                    lastModified = (data["lastModified"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
                    version = (data["version"] as? Number)?.toInt() ?: 1
                )
            } catch (e: Exception) {
                android.util.Log.w("CloudSyncWorker", "Failed to parse remote data for doc ${doc.id}", e)
                null
            }
        }
    }
    
    private fun resolveConflicts(localData: List<LocalDataItem>, remoteData: List<RemoteDataItem>): SyncStrategy {
        val operations = mutableListOf<SyncOperation>()
        
        // Create maps for easier lookup
        val localMap = localData.associateBy { it.result.id }
        val remoteMap = remoteData.associateBy { it.id }
        
        // Check for local-only items (upload to remote)
        localData.forEach { localItem ->
            val remoteItem = remoteMap[localItem.result.id]
            if (remoteItem == null) {
                operations.add(SyncOperation(OperationType.UPLOAD_LOCAL, localItem = localItem))
            } else {
                // Check for conflicts
                if (localItem.lastModified > remoteItem.lastModified) {
                    // Local is newer, upload to remote
                    operations.add(SyncOperation(OperationType.UPLOAD_LOCAL, localItem = localItem))
                } else if (remoteItem.lastModified > localItem.lastModified) {
                    // Remote is newer, download to local
                    operations.add(SyncOperation(OperationType.DOWNLOAD_REMOTE, remoteItem = remoteItem))
                } else if (localItem.version != remoteItem.version) {
                    // Version conflict, resolve with strategy
                    operations.add(SyncOperation(
                        OperationType.RESOLVE_CONFLICT,
                        localItem = localItem,
                        remoteItem = remoteItem,
                        resolvedData = resolveVersionConflict(localItem, remoteItem)
                    ))
                }
            }
        }
        
        // Check for remote-only items (download to local)
        remoteData.forEach { remoteItem ->
            if (!localMap.containsKey(remoteItem.id)) {
                operations.add(SyncOperation(OperationType.DOWNLOAD_REMOTE, remoteItem = remoteItem))
            }
        }
        
        return SyncStrategy(operations)
    }
    
    private fun resolveVersionConflict(localItem: LocalDataItem, remoteItem: RemoteDataItem): Map<String, Any?> {
        // ✅ Conflict resolution strategy: prefer local data for test results
        // In a real app, you might want more sophisticated conflict resolution
        android.util.Log.w("CloudSyncWorker", "Resolving version conflict for ${localItem.result.id}")
        
        return mapOf(
            "strategy" to "prefer_local",
            "localVersion" to localItem.version,
            "remoteVersion" to remoteItem.version,
            "resolvedData" to localItem.result
        )
    }
    
    private suspend fun executeAtomicOperations(uid: String, strategy: SyncStrategy) {
        val batch = firestore.batch()
        val col = firestore.collection("users").document(uid).collection("history")
        
        strategy.operations.forEach { operation ->
            when (operation.type) {
                OperationType.UPLOAD_LOCAL -> {
                    operation.localItem?.let { localItem ->
                        val doc = col.document(localItem.result.id)
                        val data = convertTestResultToMap(localItem.result)
                        batch.set(doc, data)
                        android.util.Log.d("CloudSyncWorker", "Queued upload for ${localItem.result.id}")
                    }
                }
                OperationType.DOWNLOAD_REMOTE -> {
                    operation.remoteItem?.let { remoteItem ->
                        // Convert remote data to TestResult and save locally
                        val testResult = convertMapToTestResult(remoteItem.data, uid)
                        testResultDao.insertTestResult(testResult)
                        android.util.Log.d("CloudSyncWorker", "Downloaded ${remoteItem.id} to local")
                    }
                }
                OperationType.RESOLVE_CONFLICT -> {
                    operation.resolvedData?.let { resolvedData ->
                        val strategy = resolvedData["strategy"] as? String
                        when (strategy) {
                            "prefer_local" -> {
                                operation.localItem?.let { localItem ->
                                    val doc = col.document(localItem.result.id)
                                    val data = convertTestResultToMap(localItem.result)
                                    batch.set(doc, data)
                                    android.util.Log.d("CloudSyncWorker", "Resolved conflict by preferring local for ${localItem.result.id}")
                                }
                            }
                            "prefer_remote" -> {
                                operation.remoteItem?.let { remoteItem ->
                                    val testResult = convertMapToTestResult(remoteItem.data, uid)
                                    testResultDao.insertTestResult(testResult)
                                    android.util.Log.d("CloudSyncWorker", "Resolved conflict by preferring remote for ${remoteItem.id}")
                                }
                            }
                        }
                    }
                }
                OperationType.DELETE_REMOTE -> {
                    operation.remoteItem?.let { remoteItem ->
                        val doc = col.document(remoteItem.id)
                        batch.delete(doc)
                        android.util.Log.d("CloudSyncWorker", "Queued delete for remote ${remoteItem.id}")
                    }
                }
                OperationType.DELETE_LOCAL -> {
                    operation.localItem?.let { localItem ->
                        testResultDao.deleteTestResult(localItem.result)
                        android.util.Log.d("CloudSyncWorker", "Deleted local ${localItem.result.id}")
                    }
                }
            }
        }
        
        // Execute batch operation atomically
        if (strategy.operations.any { it.type == OperationType.UPLOAD_LOCAL || it.type == OperationType.DELETE_REMOTE }) {
            batch.commit().await()
            android.util.Log.i("CloudSyncWorker", "Batch operations committed successfully")
        }
        
        // Notify repository of changes
        historyRepository.notifyChanged()
    }
    
    private fun convertTestResultToMap(result: TestResult): Map<String, Any?> {
        val d = result.resultDetails
        return hashMapOf<String, Any?>(
            "id" to result.id,
            "testId" to result.testId,
            "testName" to result.testName,
            "category" to result.category.name,
            "type" to result.type.toString(),
            "domain" to result.domain,
            "ipAddress" to result.ipAddress,
            "status" to result.status.name,
            "startTime" to result.startTime,
            "endTime" to result.endTime,
            "duration" to result.duration,
            "creditsUsed" to result.creditsUsed,
            "userId" to result.userId,
            "lastModified" to com.google.firebase.Timestamp.now(),
            "version" to 1,
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
    }
    
    private fun convertMapToTestResult(data: Map<String, Any?>, uid: String): TestResult {
        val details = data["details"] as? Map<String, Any?> ?: emptyMap()
        
        return TestResult(
            id = data["id"] as? String ?: "",
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


