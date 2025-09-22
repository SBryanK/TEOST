package com.example.teost.services

import com.example.teost.core.data.engine.SecurityTestEngine
import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.repository.CreditsRepository
import com.example.teost.data.repository.HistoryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestExecutionWorkerEntryPoint {
    fun engine(): SecurityTestEngine
    fun testResultDao(): TestResultDao
    fun prefs(): PreferencesManager
    fun historyRepository(): HistoryRepository
    fun creditsRepository(): CreditsRepository
}


