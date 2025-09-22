package com.example.teost.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestResult
import com.example.teost.data.model.TestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val testResultDao: TestResultDao
) {
    // Repo-triggered invalidation signal for UI to refresh Paging without recreating Pager
    private val _invalidationFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val invalidationFlow: SharedFlow<Unit> = _invalidationFlow.asSharedFlow()

    fun notifyChanged() {
        android.util.Log.d("HistoryRepository", "notifyChanged() called - triggering paging refresh")
        _invalidationFlow.tryEmit(Unit)
    }
    
    /**
     * Force immediate refresh of history data - clears any caching
     */
    fun forceRefresh() {
        android.util.Log.d("HistoryRepository", "forceRefresh() called - forcing immediate data reload")
        _invalidationFlow.tryEmit(Unit)
    }
    suspend fun getById(id: String) = testResultDao.getTestResultById(id)

    fun pagerAll(userId: String, pageSize: Int = 20): Flow<PagingData<TestResult>> {
        android.util.Log.d("HistoryRepository", "Creating pagerAll for userId='$userId'")
        return Pager(
            PagingConfig(
                pageSize = pageSize, 
                enablePlaceholders = false,
                prefetchDistance = 5,  // Prefetch more items for smoother scrolling
                initialLoadSize = pageSize * 2  // Load more items initially
            )
        ) {
            testResultDao.getAllTestResults(userId)
        }.flow
    }

    fun pagerSearch(userId: String, query: String, pageSize: Int = 20): Flow<PagingData<TestResult>> =
        Pager(PagingConfig(pageSize = pageSize, enablePlaceholders = false)) {
            testResultDao.searchTestResults(userId, query)
        }.flow

    fun pagerByCategory(userId: String, category: TestCategory, pageSize: Int = 20): Flow<PagingData<TestResult>> =
        Pager(PagingConfig(pageSize = pageSize, enablePlaceholders = false)) {
            testResultDao.getTestResultsByCategory(userId, category)
        }.flow

    fun pagerFiltered(
        userId: String,
        category: TestCategory?,
        status: TestStatus?,
        type: com.example.teost.data.model.TestType?,
        pageSize: Int = 20
    ): Flow<PagingData<TestResult>> =
        Pager(PagingConfig(pageSize = pageSize, enablePlaceholders = false)) {
            testResultDao.getTestResultsFiltered(userId, category, status, type)
        }.flow

    fun pagerFilteredAdvanced(
        userId: String,
        category: TestCategory?,
        status: TestStatus?,
        type: com.example.teost.data.model.TestType?,
        from: java.util.Date?,
        to: java.util.Date?,
        domain: String?,
        pageSize: Int = 20
    ): Flow<PagingData<TestResult>> {
        android.util.Log.d("HistoryRepository", "Creating pagerFilteredAdvanced for userId='$userId', filters: category=$category, status=$status, type=$type")
        return Pager(
            PagingConfig(
                pageSize = pageSize, 
                enablePlaceholders = false,
                prefetchDistance = 5,  // Prefetch more items
                initialLoadSize = pageSize * 2  // Load more items initially
            )
        ) {
            testResultDao.getTestResultsFilteredAdvanced(userId, category, status, type, from, to, domain)
        }.flow
    }

    suspend fun listFilteredAdvanced(
        userId: String,
        category: TestCategory?,
        status: TestStatus?,
        type: com.example.teost.data.model.TestType?,
        from: java.util.Date?,
        to: java.util.Date?,
        domain: String?
    ): List<TestResult> =
        testResultDao.getTestResultsFilteredAdvancedList(userId, category, status, type, from, to, domain)

    // Live recent results for auto-refresh triggers in History
    fun recent(userId: String, limit: Int = 1): Flow<List<TestResult>> =
        testResultDao.getRecentTestResults(userId, limit)
}




