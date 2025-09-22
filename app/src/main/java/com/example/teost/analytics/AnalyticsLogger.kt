package com.example.teost.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsLogger @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle()
        params.forEach { (k, v) -> bundle.putString(k, v) }
        analytics.logEvent(name, bundle)
    }
}




