package com.example.teost.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.teost.data.model.TestConfiguration
import com.example.teost.services.TestExecutionWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.teost.presentation.screens.test.TestCartStore

@AndroidEntryPoint
class TestQueueReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		android.util.Log.d("TestQueueReceiver", "Received broadcast: ${intent.action}")
		if (intent.action == ACTION_EXECUTE_TEST_QUEUE) {
			val list: ArrayList<TestConfiguration> = if (Build.VERSION.SDK_INT >= 33) {
				intent.getParcelableArrayListExtra(EXTRA_QUEUE, TestConfiguration::class.java) ?: arrayListOf()
			} else {
				@Suppress("DEPRECATION")
				intent.getParcelableArrayListExtra<TestConfiguration>(EXTRA_QUEUE) ?: arrayListOf()
			}
			android.util.Log.d("TestQueueReceiver", "Processing ${list.size} test configurations")
            val wm = WorkManager.getInstance(context)
			val gson = com.google.gson.Gson()
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			list.forEach { config ->
				android.util.Log.d("TestQueueReceiver", "Enqueuing test: ${config.testId} for domain: ${config.domain}")
				val input = Data.Builder()
					.putString(TestExecutionWorker.KEY_CONFIG_JSON, gson.toJson(config))
					.build()
				val typeTag = config.parameters.requestPath?.let { path ->
					when {
						config.parameters.burstRequests != null -> "dos_http_spike"
						config.parameters.payloadList != null -> "waf_test"
						config.parameters.uaProfiles != null -> "bot_test"
						config.parameters.authMode != null -> "api_test"
						else -> "security_test_generic"
					}
				} ?: "security_test_generic"
                val req = OneTimeWorkRequestBuilder<TestExecutionWorker>()
					.addTag("security_test")
					.addTag(typeTag)
					.addTag(config.testId)
					.setConstraints(constraints)
					.setInputData(input)
					.build()
				wm.enqueue(req)
				android.util.Log.d("TestQueueReceiver", "WorkManager job enqueued with tags: security_test, $typeTag, ${config.testId}")
			}
		}
	}

	companion object {
		const val ACTION_EXECUTE_TEST_QUEUE = "com.example.teost.ACTION_EXECUTE_TEST_QUEUE"
		const val EXTRA_QUEUE = "extra_queue"
	}
}

