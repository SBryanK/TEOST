package com.example.teost.di

import android.content.Context
import androidx.room.Room
import com.example.teost.BuildConfig
import com.example.teost.data.local.EdgeOneDatabase
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.local.dao.DomainDao
import com.example.teost.data.repository.DomainRepository
import com.example.teost.data.repository.DomainRepositoryImpl
import com.example.teost.data.repository.TestResultRepository
import com.example.teost.data.repository.TestResultRepositoryImpl
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.repository.AuthRepository
import com.example.teost.data.repository.ConnectionTestRepository
import com.example.teost.data.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import okhttp3.logging.HttpLoggingInterceptor
import com.example.teost.core.data.config.AppConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.example.teost.core.data.remote.HttpTestService

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideEdgeOneDatabase(
        @ApplicationContext context: Context
    ): EdgeOneDatabase {
        return Room.databaseBuilder(
            context,
            EdgeOneDatabase::class.java,
            EdgeOneDatabase.DATABASE_NAME
        )
            // PROPER MIGRATION: Add indices without destroying user data
            .addMigrations(object : androidx.room.migration.Migration(5, 6) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    android.util.Log.d("Database", "Migration 5->6: Adding indices to preserve performance")
                    // Create indices manually to fix the mismatch
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_results_userId ON test_results (userId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_results_startTime ON test_results (startTime)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_results_domain ON test_results (domain)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_results_status ON test_results (status)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_results_category ON test_results (category)")
                        android.util.Log.d("Database", "Migration 5->6: All indices created successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("Database", "Migration 5->6: Failed to create indices", e)
                        throw e
                    }
                }
            })
            // ONLY as last resort - if proper migration fails, then fallback
            .fallbackToDestructiveMigration()
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    android.util.Log.d("Database", "Database v6 created with proper schema and indices")
                }
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    android.util.Log.d("Database", "Database v6 opened - user data preserved")
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideTestResultDao(database: EdgeOneDatabase): TestResultDao = database.testResultDao()

    @Provides
    @Singleton
    fun provideDomainDao(database: EdgeOneDatabase): DomainDao = database.domainDao()

    // Cloud sync removed: no SyncStateDao provider

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig()

    @Provides
    @Singleton
    fun provideOkHttpClient(config: AppConfig): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { original ->
            // Redact sensitive headers using AppConfig keys
            var out = original
            config.redactedHeaderKeys.forEach { key ->
                val pattern = Regex("(?i)(${Regex.escape(key)}): .*")
                out = out.replace(pattern, "$1: [REDACTED]")
            }
            out
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // ✅ OPTIMIZED TIMEOUTS for security testing
            .connectTimeout(10, TimeUnit.SECONDS) // Faster fail for unreachable targets
            .readTimeout(15, TimeUnit.SECONDS) // Adequate for response reading
            .writeTimeout(10, TimeUnit.SECONDS) // Faster write timeout
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false) // CRITICAL: Disable auto-retry to prevent loops
            // ✅ OPTIMIZED CONNECTION POOL for concurrent testing
            .connectionPool(okhttp3.ConnectionPool(8, 3, TimeUnit.MINUTES)) // Slightly larger pool for concurrent tests
            // ✅ CALL TIMEOUT optimization
            .callTimeout(25, TimeUnit.SECONDS) // Shorter overall timeout
            // ✅ NETWORK EFFICIENCY - Add connection specs for better performance
            .connectionSpecs(listOf(
                okhttp3.ConnectionSpec.MODERN_TLS,
                okhttp3.ConnectionSpec.COMPATIBLE_TLS,
                okhttp3.ConnectionSpec.CLEARTEXT
            ))
            
        // Optional cert pinning
        if (config.certificatePins.isNotEmpty()) {
            val pinnerBuilder = okhttp3.CertificatePinner.Builder()
            config.certificatePins.forEach { (host, pins) ->
                pins.forEach { pin -> pinnerBuilder.add(host, pin) }
            }
            clientBuilder.certificatePinner(pinnerBuilder.build())
        }
        return clientBuilder.build()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        preferencesManager: PreferencesManager,
        testResultDao: TestResultDao,
        domainDao: DomainDao
    ): AuthRepository = AuthRepository(auth, firestore, preferencesManager, testResultDao, domainDao)

    @Provides
    @Singleton
    fun provideConnectionTestRepository(
        okHttpClient: OkHttpClient,
        httpTestService: HttpTestService
    ): ConnectionTestRepository = ConnectionTestRepository(okHttpClient, httpTestService)

    @Provides
    @Singleton
    fun providePerformTestUseCase(
        repository: ConnectionTestRepository
    ): com.example.teost.feature.search.PerformTestUseCase = com.example.teost.feature.search.PerformTestUseCase(repository)

    @Provides
    @Singleton
    fun provideDomainRepository(
        domainDao: DomainDao
    ): DomainRepository = DomainRepositoryImpl(domainDao)

    @Provides
    @Singleton
    fun provideTestResultRepository(
        testResultDao: TestResultDao
    ): TestResultRepository = TestResultRepositoryImpl(testResultDao)

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideHttpTestService(retrofit: Retrofit): HttpTestService = retrofit.create(HttpTestService::class.java)

    @Provides
    @Singleton
    fun provideHistoryRepository(
        testResultDao: TestResultDao
    ): HistoryRepository = HistoryRepository(testResultDao)

    @Provides
    @Singleton
    fun provideCreditsRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): com.example.teost.data.repository.CreditsRepository = 
        com.example.teost.data.repository.CreditsRepository(auth, firestore)
}
