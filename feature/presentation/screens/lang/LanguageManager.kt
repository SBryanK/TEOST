package com.example.teost.presentation.screens.lang

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.teost.data.local.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    private val prefs: PreferencesManager,
    @ApplicationContext private val appContext: Context
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun readCachedLanguage(): String {
        return try {
            val sp = appContext.getSharedPreferences("lang_cache", Context.MODE_PRIVATE)
            sp.getString("last_language", null)
        } catch (_: Exception) { null } ?: "en"
    }

    private fun cacheLanguage(tag: String) {
        try {
            val sp = appContext.getSharedPreferences("lang_cache", Context.MODE_PRIVATE)
            sp.edit().putString("last_language", tag).apply()
        } catch (_: Exception) {}
    }

    fun applySavedLanguage(context: Context) {
        // Fast apply from cache to avoid blocking startup
        val cached = readCachedLanguage()
        val current = AppCompatDelegate.getApplicationLocales()?.toLanguageTags()
        if (!cached.equals(current, ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(cached))
        }
        // Sync with DataStore asynchronously (with timeout to prevent blocking)
        appScope.launch {
            try {
                val saved = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                    prefs.userPreferences.first().selectedLanguage
                } ?: cached // Use cached if DataStore times out
                
                val applied = AppCompatDelegate.getApplicationLocales()?.toLanguageTags()
                if (!saved.equals(applied, ignoreCase = true)) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
                    android.util.Log.d("LanguageManager", "Language applied: $saved")
                }
            } catch (e: Exception) {
                android.util.Log.w("LanguageManager", "Failed to sync language from DataStore: ${e.message}")
                // Continue with cached language
            }
        }
    }

    suspend fun setLanguage(tag: String) {
        // Apply first on main (to minimize perceived delay/flicker)
        withContext(Dispatchers.Main.immediate) {
            // Disable window/activity animations briefly to avoid noticeable transition
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Use new API for Android 14+
                    (appContext as? android.app.Activity)?.overrideActivityTransition(
                        android.app.Activity.OVERRIDE_TRANSITION_OPEN, 0, 0
                    )
                } else {
                    @Suppress("DEPRECATION")
                    (appContext as? android.app.Activity)?.overridePendingTransition(0, 0)
                }
            } catch (_: Exception) {}
            val current = AppCompatDelegate.getApplicationLocales()?.toLanguageTags()
            if (!tag.equals(current, ignoreCase = true)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
        // Persist and cache asynchronously on IO (no blocking UI)
        withContext(Dispatchers.IO) {
            runCatching { prefs.setLanguage(tag) }
            cacheLanguage(tag)
        }
    }
}


