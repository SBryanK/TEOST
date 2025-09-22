package com.example.teost.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestResult
import com.example.teost.data.model.TestStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TestResultDao {
    
    @Query("SELECT * FROM test_results WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllTestResults(userId: String): PagingSource<Int, TestResult>
    
    @Query("SELECT * FROM test_results WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    fun getRecentTestResults(userId: String, limit: Int = 10): Flow<List<TestResult>>
    
    @Query("SELECT * FROM test_results WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentTestResultsList(userId: String, limit: Int = 10): List<TestResult>
    
    @Query("SELECT * FROM test_results WHERE id = :testId")
    suspend fun getTestResultById(testId: String): TestResult?

    @Query("SELECT * FROM test_results WHERE testId = :testId ORDER BY startTime DESC LIMIT 1")
    suspend fun getTestResultByTestId(testId: String): TestResult?

    @Query("SELECT * FROM test_results WHERE testId IN (:testIds)")
    suspend fun getTestResultsByTestIds(testIds: List<String>): List<TestResult>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND (domain LIKE '%' || :query || '%' 
            OR ipAddress LIKE '%' || :query || '%' 
            OR testName LIKE '%' || :query || '%')
        ORDER BY startTime DESC
    """)
    fun searchTestResults(userId: String, query: String): PagingSource<Int, TestResult>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND category = :category 
        ORDER BY startTime DESC
    """)
    fun getTestResultsByCategory(userId: String, category: TestCategory): PagingSource<Int, TestResult>

    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category)
        AND (:status IS NULL OR status = :status)
        AND (:typeJson IS NULL OR type = :typeJson)
        ORDER BY startTime DESC
    """)
    fun getTestResultsFiltered(
        userId: String,
        category: TestCategory?,
        status: TestStatus?,
        typeJson: com.example.teost.data.model.TestType?
    ): PagingSource<Int, TestResult>

    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category)
        AND (:status IS NULL OR status = :status)
        AND (:typeJson IS NULL OR type = :typeJson)
        AND (:fromDate IS NULL OR startTime >= :fromDate)
        AND (:toDate IS NULL OR startTime <= :toDate)
        AND (:domainFilter IS NULL OR domain LIKE '%' || :domainFilter || '%')
        ORDER BY startTime DESC
    """)
    fun getTestResultsFilteredAdvanced(
        userId: String,
        category: TestCategory?,
        status: TestStatus?,
        typeJson: com.example.teost.data.model.TestType?,
        fromDate: java.util.Date?,
        toDate: java.util.Date?,
        domainFilter: String?
    ): PagingSource<Int, TestResult>

    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category)
        AND (:status IS NULL OR status = :status)
        AND (:typeJson IS NULL OR type = :typeJson)
        AND (:fromDate IS NULL OR startTime >= :fromDate)
        AND (:toDate IS NULL OR startTime <= :toDate)
        AND (:domainFilter IS NULL OR domain LIKE '%' || :domainFilter || '%')
        ORDER BY startTime DESC
    """)
    suspend fun getTestResultsFilteredAdvancedList(
        userId: String,
        category: TestCategory?,
        status: TestStatus?,
        typeJson: com.example.teost.data.model.TestType?,
        fromDate: java.util.Date?,
        toDate: java.util.Date?,
        domainFilter: String?
    ): List<TestResult>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND status = :status 
        ORDER BY startTime DESC
    """)
    fun getTestResultsByStatus(userId: String, status: TestStatus): Flow<List<TestResult>>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND domain = :domain 
        ORDER BY startTime DESC
    """)
    fun getTestResultsByDomain(userId: String, domain: String): Flow<List<TestResult>>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE userId = :userId 
        AND startTime BETWEEN :startDate AND :endDate 
        ORDER BY startTime DESC
    """)
    fun getTestResultsByDateRange(
        userId: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<TestResult>>
    
    @Query("SELECT COUNT(*) FROM test_results WHERE userId = :userId")
    suspend fun getTotalTestCount(userId: String): Int
    
    @Query("SELECT SUM(creditsUsed) FROM test_results WHERE userId = :userId")
    suspend fun getTotalCreditsUsed(userId: String): Int?
    
    @Query("""
        SELECT COUNT(*) FROM test_results 
        WHERE userId = :userId 
        AND status = :status
    """)
    suspend fun getTestCountByStatus(userId: String, status: TestStatus): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResult(testResult: TestResult)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResults(testResults: List<TestResult>)
    
    @Update
    suspend fun updateTestResult(testResult: TestResult)
    
    @Delete
    suspend fun deleteTestResult(testResult: TestResult)
    
    @Query("DELETE FROM test_results WHERE userId = :userId")
    suspend fun deleteAllTestResults(userId: String)
    
    @Query("DELETE FROM test_results WHERE startTime < :date")
    suspend fun deleteTestResultsOlderThan(date: Date)

    // Cloud sync removed: no unsynced results query
}
