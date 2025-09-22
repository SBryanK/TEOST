package com.example.teost.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import android.content.pm.ServiceInfo
import com.example.teost.core.data.engine.SecurityTestEngine
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.model.TestConfiguration
import com.example.teost.data.model.TestResult
import com.example.teost.data.local.PreferencesManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import java.util.Date
import android.content.Intent
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@HiltWorker
class TestExecutionWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted workerParams: WorkerParameters,
	private val engine: SecurityTestEngine,
	private val testResultDao: TestResultDao,
	private val prefs: PreferencesManager,
	private val historyRepository: com.example.teost.data.repository.HistoryRepository,
	private val creditsRepository: com.example.teost.data.repository.CreditsRepository
) : CoroutineWorker(appContext, workerParams) {

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val notification = NotificationBuilder.create(applicationContext, "Running security test")
		return ForegroundInfo(
			NotificationBuilder.NOTIFICATION_ID, 
			notification,
			android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
		)
	}

	override suspend fun doWork(): Result {
		android.util.Log.d("TestExecutionWorker", "Starting test execution")
		
		// ✅ BATTERY OPTIMIZATION - Set foreground service efficiently
		try {
			setForeground(getForegroundInfo())
		} catch (e: Exception) {
			android.util.Log.w("TestExecutionWorker", "Failed to set foreground: ${e.message}")
			// Continue without foreground if it fails
		}

		val json = inputData.getString(KEY_CONFIG_JSON) ?: return Result.failure(
			workDataOf("error" to "Missing test configuration input")
		)
		android.util.Log.d("TestExecutionWorker", "Test configuration JSON: $json")
		val gson = Gson()
		val rawConfig = try {
			gson.fromJson(json, TestConfiguration::class.java)
		} catch (e: Exception) {
			android.util.Log.e("TestExecutionWorker", "Failed to parse test configuration", e)
			return Result.failure(workDataOf("error" to (e.message ?: "Invalid test configuration")))
		}
		
		val config = rawConfig

		android.util.Log.d("TestExecutionWorker", "Executing test for domain: ${config.domain}")
		var success = false
		var errorMessage: String? = null
		var result: TestResult? = null
		// Resolve current user for persistence
		val prefsUserId = try { 
			kotlinx.coroutines.withTimeoutOrNull(2000L) {
				prefs.userPreferences.first().userId
			} ?: ""
		} catch (_: Exception) { "" }
		val authUid = try { FirebaseAuth.getInstance().currentUser?.uid ?: "" } catch (_: Exception) { "" }
		val currentUserId = when {
			authUid.isNotBlank() -> authUid
			prefsUserId.isNotBlank() -> prefsUserId
			else -> "default_user" // Fallback untuk testing
		}
		android.util.Log.d("TestExecutionWorker", "User ID resolution: prefsUserId='$prefsUserId', authUid='$authUid', currentUserId='$currentUserId'")
		
		// Send test started broadcast
		try {
			val startedIntent = Intent("com.example.teost.ACTION_TEST_STARTED")
			startedIntent.setPackage(applicationContext.packageName)
			startedIntent.putExtra("test_id", config.testId)
			startedIntent.putExtra("domain", config.domain)
			applicationContext.sendBroadcast(startedIntent)
			android.util.Log.d("TestExecutionWorker", "Test started broadcast sent for testId: ${config.testId}")
		} catch (e: Exception) {
			android.util.Log.e("TestExecutionWorker", "Failed to send test started broadcast", e)
		}
		
		try {
			engine.executeTest(config).collect { progress ->
				when (progress) {
					is SecurityTestEngine.TestProgress.Starting -> {
						android.util.Log.d("TestExecutionWorker", "Test starting: ${progress.testName}")
						setProgress(workDataOf("message" to "Starting ${progress.testName}", "progress" to 0))
					}
					is SecurityTestEngine.TestProgress.Running -> {
						android.util.Log.d("TestExecutionWorker", "Test running: ${progress.message} (${progress.progress})")
						setProgress(workDataOf("message" to progress.message, "progress" to (progress.progress * 100).toInt()))
					}
					is SecurityTestEngine.TestProgress.Completed -> {
						android.util.Log.d("TestExecutionWorker", "Test completed successfully")
						android.util.Log.d("TestExecutionWorker", "Original result creditsUsed: ${progress.result.creditsUsed}")
							result = progress.result.copy(userId = currentUserId)
						android.util.Log.d("TestExecutionWorker", "After copy creditsUsed: ${result.creditsUsed}")
						android.util.Log.d("TestExecutionWorker", "Saving test result: id=${result.id}, testId=${result.testId}, userId=${result.userId}, domain=${result.domain}, creditsUsed=${result.creditsUsed}")
						try {
							testResultDao.insertTestResult(result)
							android.util.Log.d("TestExecutionWorker", "Test result saved to database successfully")
							
							// Verify the save by trying to read it back
							val savedResult = testResultDao.getTestResultById(result.id)
							if (savedResult != null) {
								android.util.Log.d("TestExecutionWorker", "Verification: Test result found in database with userId=${savedResult.userId}, status=${savedResult.status}, domain=${savedResult.domain}")
								
								// Also check if it appears in user's history
								val userResults = testResultDao.getRecentTestResultsList(result.userId, 5)
								android.util.Log.d("TestExecutionWorker", "User ${result.userId} now has ${userResults.size} recent results")
								userResults.forEach { r ->
									android.util.Log.d("TestExecutionWorker", "Recent result: id=${r.id}, domain=${r.domain}, status=${r.status}, testName=${r.testName}")
								}
							} else {
								android.util.Log.e("TestExecutionWorker", "Verification FAILED: Test result not found in database after save")
							}
						} catch (e: Exception) {
							android.util.Log.e("TestExecutionWorker", "Failed to save test result to database", e)
						}
								// Consume credits per recorded result; non-blocking
							android.util.Log.d("TestExecutionWorker", "Attempting to consume ${result.creditsUsed} credits")
							val consumed = runCatching {
								var ok = false
								var delayMs = 250L
								repeat(3) {
									try {
										Firebase.functions
											.getHttpsCallable("consumeCredits")
											.call(mapOf("amount" to result.creditsUsed))
											.await()
										ok = true
										android.util.Log.d("TestExecutionWorker", "Credits consumed successfully: ${result.creditsUsed}")
										return@repeat
									} catch (e: Exception) {
										android.util.Log.w("TestExecutionWorker", "Failed to consume credits (attempt $it): ${e.message}")
										kotlinx.coroutines.delay(delayMs)
										delayMs = (delayMs * 2).coerceAtMost(2000L)
									}
								}
								ok
							}.getOrDefault(false)
							android.util.Log.d("TestExecutionWorker", "Firebase credit consumption result: $consumed")
							
							// Fallback: try local credit consumption if Firebase failed
							if (!consumed && result != null) {
								try {
									val localConsumed = creditsRepository.consumeCredits(result.creditsUsed)
									android.util.Log.d("TestExecutionWorker", "Local credit consumption result: $localConsumed")
								} catch (e: Exception) {
									android.util.Log.w("TestExecutionWorker", "Local credit consumption also failed: ${e.message}")
								}
							}
							
						// AGGRESSIVE HISTORY REFRESH STRATEGY
						android.util.Log.d("TestExecutionWorker", "Starting aggressive history refresh strategy")
						
						// 1. Immediate repository notification
						try {
							historyRepository.notifyChanged()
							android.util.Log.d("TestExecutionWorker", "Step 1: History repository notified")
						} catch (e: Exception) {
							android.util.Log.e("TestExecutionWorker", "Failed to notify history repository", e)
						}
						
						// 2. Immediate broadcast
						try {
							val refreshIntent = Intent("com.example.teost.ACTION_REFRESH_HISTORY")
							refreshIntent.setPackage(applicationContext.packageName)
							applicationContext.sendBroadcast(refreshIntent)
							android.util.Log.d("TestExecutionWorker", "Step 2: History refresh broadcast sent")
						} catch (e: Exception) {
							android.util.Log.e("TestExecutionWorker", "Failed to send history refresh broadcast", e)
						}
						
						// 3. Multiple immediate refreshes to ensure UI updates
						try {
							repeat(3) {
								historyRepository.forceRefresh()
								android.util.Log.d("TestExecutionWorker", "Step 3.$it: Additional force refresh sent")
							}
						} catch (e: Exception) {
							android.util.Log.e("TestExecutionWorker", "Failed to send additional refreshes", e)
						}
						
						// Immediate cloud sync after test completion
						try {
							com.example.teost.services.SyncScheduler.runOneTimeNow(applicationContext)
							android.util.Log.d("TestExecutionWorker", "Cloud sync triggered after test completion")
						} catch (e: Exception) {
							android.util.Log.e("TestExecutionWorker", "Failed to trigger cloud sync", e)
						}
						success = true
					}
					is SecurityTestEngine.TestProgress.Failed -> {
						android.util.Log.e("TestExecutionWorker", "Test failed: ${progress.error}")
						errorMessage = progress.error
					}
				}
			}
		} catch (e: Exception) {
			android.util.Log.e("TestExecutionWorker", "Test execution error", e)
			errorMessage = e.message ?: "Execution error"
		}

		// Always send a completion broadcast so the UI can unlock
		try {
			// Small delay to ensure database transaction is committed
			kotlinx.coroutines.delay(500)
			
			val completionIntent = Intent("com.example.teost.ACTION_TEST_COMPLETED")
			completionIntent.setPackage(applicationContext.packageName)
			completionIntent.putExtra("success", success)
			completionIntent.putExtra("timestamp", System.currentTimeMillis())
			if (config != null) {
				completionIntent.putExtra("test_id", config.testId)
				completionIntent.putExtra("domain", config.domain)
				// Use actual credits used from result if available
				val creditsUsed = if (success && result != null) result!!.creditsUsed else 1
				completionIntent.putExtra("credits_used", creditsUsed)
			}
			android.util.Log.d("TestExecutionWorker", "Sending completion broadcast with action: ${completionIntent.action}, package: ${completionIntent.`package`}, success: $success, testId: ${config?.testId}")
			applicationContext.sendBroadcast(completionIntent)
			android.util.Log.d("TestExecutionWorker", "Completion broadcast sent successfully")
			
			// Send a second broadcast to ensure delivery
			kotlinx.coroutines.delay(100)
			applicationContext.sendBroadcast(completionIntent)
			android.util.Log.d("TestExecutionWorker", "Duplicate completion broadcast sent for reliability")
		} catch (e: Exception) {
			android.util.Log.e("TestExecutionWorker", "Failed to send completion broadcast", e)
		}

		android.util.Log.d("TestExecutionWorker", "Test execution finished. Success: $success")
		return if (success) Result.success() else Result.failure(workDataOf("error" to (errorMessage ?: "Unknown error")))
	}

	companion object {
		const val KEY_CONFIG_JSON = "key_config_json"
	}
}

