package com.example.teost

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import com.example.teost.BuildConfig
import com.example.teost.presentation.screens.lang.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.work.WorkManager

@HiltAndroidApp
class EdgeOneSecurityApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var languageManager: LanguageManager
    @Inject lateinit var prefs: com.example.teost.data.local.PreferencesManager
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // Cloud sync removed
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved app language as early as possible (non-blocking)
        try { 
            languageManager.applySavedLanguage(this) 
        } catch (e: Exception) {
            android.util.Log.w("EdgeOneApp", "Language manager failed: ${e.message}")
        }
        
        // Disable StrictMode to prevent ANR in production-like testing
        // if (BuildConfig.DEBUG) {
        //     StrictMode.setThreadPolicy(
        //         StrictMode.ThreadPolicy.Builder()
        //             .detectAll()
        //             .penaltyLog()
        //             .build()
        //     )
        //     StrictMode.setVmPolicy(
        //         StrictMode.VmPolicy.Builder()
        //             .detectAll()
        //             .penaltyLog()
        //             .build()
        //     )
        //     Thread.setDefaultUncaughtExceptionHandler { t, e ->
        //         Log.e("EdgeOneApp", "Uncaught exception in ${t.name}", e)
        //     }
        // }
        
        // Initialize WorkManager manually since we disabled automatic initialization
        try {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
                android.util.Log.d("EdgeOneApp", "WorkManager initialized successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("EdgeOneApp", "Failed to initialize WorkManager", e)
        }
        
        createNotificationChannels()
        // Conditional periodic cloud sync based on saved preference (with timeout)
        appScope.launch(Dispatchers.IO) {
            try {
                val syncEnabled = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    prefs.userPreferences.first().cloudSyncEnabled
                } ?: false // Default to disabled if timeout
                
                if (syncEnabled) {
                    com.example.teost.services.SyncScheduler.schedulePeriodic(this@EdgeOneSecurityApp)
                } else {
                    com.example.teost.services.SyncScheduler.cancelPeriodic(this@EdgeOneSecurityApp)
                }
                android.util.Log.d("EdgeOneApp", "Cloud sync setup completed, enabled: $syncEnabled")
            } catch (e: Exception) {
                android.util.Log.w("EdgeOneApp", "Cloud sync setup failed: ${e.message}")
                // If preferences can't be loaded, default to disabled
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channelName = getString(com.example.teost.core.ui.R.string.security_tests)
            val channel = NotificationChannel(
                com.example.teost.services.NotificationBuilder.CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(channel)
        }
    }
}
