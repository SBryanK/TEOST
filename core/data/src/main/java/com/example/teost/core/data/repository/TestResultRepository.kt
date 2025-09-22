package com.example.teost.data.repository

import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.model.TestResult
import javax.inject.Inject
import javax.inject.Singleton

interface TestResultRepository {
    suspend fun getById(id: String): TestResult?
}

@Singleton
class TestResultRepositoryImpl @Inject constructor(
    private val dao: TestResultDao
) : TestResultRepository {
    override suspend fun getById(id: String): TestResult? = dao.getTestResultById(id)
}


