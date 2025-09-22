package com.example.teost.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.teost.data.model.Cart
import com.example.teost.data.model.ConnectionTestResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edgeone_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val dataStore = context.dataStore

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val LAST_ACTIVE_TIME = longPreferencesKey("last_active_time")
        val SPLASH_SHOWN_TIME = longPreferencesKey("splash_shown_time")
        val CART_DATA = stringPreferencesKey("cart_data")
        val THEME_MODE = stringPreferencesKey("theme_mode") // light, dark, system
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val AUTO_SAVE_RESULTS = booleanPreferencesKey("auto_save_results")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_TIMEOUT = intPreferencesKey("default_timeout")
        val DEFAULT_RETRY_COUNT = intPreferencesKey("default_retry_count")
        val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email")
        // Search cache keys
        val LAST_SEARCH_INPUT = stringPreferencesKey("last_search_input")
        val LAST_SEARCH_RESULTS_JSON = stringPreferencesKey("last_search_results_json")
        val LAST_SEARCH_TIMESTAMP_MS = longPreferencesKey("last_search_timestamp_ms")
        val ENCRYPTION_DISPLAY_ACKED = booleanPreferencesKey("encryption_display_acked")
        // Cloud sync meta
        val LAST_SYNC_TIMESTAMP_MS = longPreferencesKey("last_sync_timestamp_ms")
        val LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
        val LAST_SYNC_COUNT = intPreferencesKey("last_sync_count")
        val LAST_SYNC_WORK_ID = stringPreferencesKey("last_sync_work_id")
        
        const val SESSION_TIMEOUT_MINUTES = 20160L
        const val SPLASH_DISPLAY_INTERVAL_MINUTES = 30L
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                userId = preferences[USER_ID] ?: "",
                userEmail = preferences[USER_EMAIL] ?: "",
                userName = preferences[USER_NAME] ?: "",
                isLoggedIn = preferences[IS_LOGGED_IN] ?: false,
                authToken = preferences[AUTH_TOKEN] ?: "",
                lastActiveTime = preferences[LAST_ACTIVE_TIME] ?: 0L,
                splashShownTime = preferences[SPLASH_SHOWN_TIME] ?: 0L,
                cart = preferences[CART_DATA]?.let { 
                    try {
                        gson.fromJson(it, Cart::class.java)
                    } catch (e: Exception) {
                        Cart()
                    }
                } ?: Cart(),
                themeMode = preferences[THEME_MODE] ?: "system",
                notificationEnabled = preferences[NOTIFICATION_ENABLED] ?: true,
                autoSaveResults = preferences[AUTO_SAVE_RESULTS] ?: true,
                cloudSyncEnabled = preferences[CLOUD_SYNC_ENABLED] ?: true,
                encryptionDisplayAcked = preferences[ENCRYPTION_DISPLAY_ACKED] ?: false,
                selectedLanguage = preferences[SELECTED_LANGUAGE] ?: "en",
                onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
                defaultTimeout = preferences[DEFAULT_TIMEOUT] ?: 30000,
                defaultRetryCount = preferences[DEFAULT_RETRY_COUNT] ?: 3,
                rememberedEmail = preferences[REMEMBERED_EMAIL] ?: ""
            )
        }

    suspend fun saveUserSession(
        userId: String,
        email: String,
        name: String,
        authToken: String
    ) {
        dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USER_EMAIL] = email
            preferences[USER_NAME] = name
            preferences[AUTH_TOKEN] = authToken
            preferences[IS_LOGGED_IN] = true
            preferences[LAST_ACTIVE_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun setRememberedEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[REMEMBERED_EMAIL] = email
        }
    }

    // ----- Search cache (TTL-driven) -----
    suspend fun saveLastSearch(input: String, results: List<ConnectionTestResult>) {
        val safeResults = results.map {
            it.copy(
                encryptionAlgorithm = null,
                encryptionKey = null,
                encryptionIv = null,
                encryptedBody = null,
                logs = null
            )
        }
        val json = try { gson.toJson(safeResults) } catch (_: Exception) { "[]" }
        dataStore.edit { preferences ->
            preferences[LAST_SEARCH_INPUT] = input
            preferences[LAST_SEARCH_RESULTS_JSON] = json
            preferences[LAST_SEARCH_TIMESTAMP_MS] = System.currentTimeMillis()
        }
    }

    fun lastSearchFlow(): Flow<LastSearchCache?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val input = prefs[LAST_SEARCH_INPUT] ?: return@map null
            val json = prefs[LAST_SEARCH_RESULTS_JSON] ?: return@map null
            val ts = prefs[LAST_SEARCH_TIMESTAMP_MS] ?: 0L
            val type = object : TypeToken<List<ConnectionTestResult>>() {}.type
            val list: List<ConnectionTestResult> = try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
            LastSearchCache(input = input, results = list, timestampMs = ts)
        }

    suspend fun updateLastActiveTime() {
        dataStore.edit { preferences ->
            preferences[LAST_ACTIVE_TIME] = System.currentTimeMillis()
        }
    }

    // ----- Cloud sync meta -----
    fun syncMetaFlow(): Flow<SyncMeta> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { p ->
            SyncMeta(
                lastTimestampMs = p[LAST_SYNC_TIMESTAMP_MS] ?: 0L,
                lastStatus = p[LAST_SYNC_STATUS] ?: "NEVER",
                lastCount = p[LAST_SYNC_COUNT] ?: 0,
                lastWorkId = p[LAST_SYNC_WORK_ID] ?: ""
            )
        }

    suspend fun getLastSyncTimestamp(): Long {
        return try {
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[LAST_SYNC_TIMESTAMP_MS] ?: 0L }
                .first()
        } catch (_: Exception) { 0L }
    }

    suspend fun setLastSyncMeta(timestampMs: Long, status: String, count: Int, workId: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIMESTAMP_MS] = timestampMs
            preferences[LAST_SYNC_STATUS] = status
            preferences[LAST_SYNC_COUNT] = count
            preferences[LAST_SYNC_WORK_ID] = workId
        }
    }

    suspend fun setEncryptionDisplayAcked(acked: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENCRYPTION_DISPLAY_ACKED] = acked
        }
    }

    suspend fun updateSplashShownTime() {
        dataStore.edit { preferences ->
            preferences[SPLASH_SHOWN_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun saveCart(cart: Cart) {
        dataStore.edit { preferences ->
            preferences[CART_DATA] = gson.toJson(cart)
        }
    }

    suspend fun clearCart() {
        dataStore.edit { preferences ->
            preferences[CART_DATA] = gson.toJson(Cart())
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setLanguage(tag: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = tag
        }
    }

    suspend fun setAutoSaveResults(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SAVE_RESULTS] = enabled
        }
    }

    // Cloud sync toggle
    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_ENABLED] = enabled
        }
    }

    // ----- History filters persistence -----
    private val HISTORY_FILTERS_JSON = stringPreferencesKey("history_filters_json")
    suspend fun saveHistoryFilters(filters: HistoryFilters) {
        val json = try { gson.toJson(filters) } catch (_: Exception) { "" }
        dataStore.edit { it[HISTORY_FILTERS_JSON] = json }
    }
    fun historyFiltersFlow(): Flow<HistoryFilters?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val json = p[HISTORY_FILTERS_JSON] ?: return@map null
            return@map try { gson.fromJson(json, HistoryFilters::class.java) } catch (_: Exception) { null }
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDefaultTimeout(timeout: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_TIMEOUT] = timeout
        }
    }

    suspend fun setDefaultRetryCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_RETRY_COUNT] = count
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences[USER_ID] = ""
            preferences[USER_EMAIL] = ""
            preferences[USER_NAME] = ""
            preferences[AUTH_TOKEN] = ""
            preferences[CART_DATA] = gson.toJson(Cart())
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class UserPreferences(
    val userId: String,
    val userEmail: String,
    val userName: String,
    val isLoggedIn: Boolean,
    val authToken: String,
    val lastActiveTime: Long,
    val splashShownTime: Long,
    val cart: Cart,
    val themeMode: String,
    val notificationEnabled: Boolean,
    val autoSaveResults: Boolean,
    val cloudSyncEnabled: Boolean,
    val encryptionDisplayAcked: Boolean,
    val selectedLanguage: String,
    val onboardingCompleted: Boolean,
    val defaultTimeout: Int,
    val defaultRetryCount: Int,
    val rememberedEmail: String
) {
    fun isSessionValid(): Boolean {
        if (!isLoggedIn) return false
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastActiveTime
        val timeoutMillis = PreferencesManager.SESSION_TIMEOUT_MINUTES * 60 * 1000
        return timeDifference < timeoutMillis
    }
    
    fun shouldShowSplash(): Boolean {
        val currentTime = System.currentTimeMillis()
        val firstRun = splashShownTime == 0L
        val inactivityMs = currentTime - lastActiveTime
        val intervalMillis = PreferencesManager.SPLASH_DISPLAY_INTERVAL_MINUTES * 60 * 1000
        return firstRun || inactivityMs >= intervalMillis
    }
}

data class LastSearchCache(
    val input: String,
    val results: List<ConnectionTestResult>,
    val timestampMs: Long
)

data class SyncMeta(
    val lastTimestampMs: Long,
    val lastStatus: String,
    val lastCount: Int,
    val lastWorkId: String
)
