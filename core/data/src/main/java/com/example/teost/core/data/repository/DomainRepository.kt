package com.example.teost.data.repository

import com.example.teost.data.local.dao.DomainDao
import com.example.teost.data.model.ConnectionTestResult
import com.example.teost.data.model.Domain
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface DomainRepository {
    suspend fun getDomainByName(domain: String, userId: String): Domain?
    suspend fun upsertFromConnectionResult(result: ConnectionTestResult, userId: String)
    suspend fun setFavorite(domain: String, userId: String, isFavorite: Boolean)
    fun observeFavorites(userId: String): kotlinx.coroutines.flow.Flow<List<Domain>>
}

@Singleton
class DomainRepositoryImpl @Inject constructor(
    private val domainDao: DomainDao
) : DomainRepository {
    override suspend fun getDomainByName(domain: String, userId: String): Domain? =
        domainDao.getDomainByName(domain, userId)

    override suspend fun upsertFromConnectionResult(result: ConnectionTestResult, userId: String) {
        val existing = domainDao.getDomainByName(result.domain, userId)
        if (existing != null) {
            domainDao.updateDomain(
                existing.copy(
                    ipAddresses = result.ipAddresses,
                    lastTested = Date(),
                    testCount = existing.testCount + 1,
                    httpStatus = result.statusCode,
                    responseTime = result.responseTime
                )
            )
        } else {
            domainDao.insertDomain(
                Domain(
                    domain = result.domain,
                    ipAddresses = result.ipAddresses,
                    lastTested = Date(),
                    testCount = 1,
                    httpStatus = result.statusCode,
                    responseTime = result.responseTime,
                    userId = userId
                )
            )
        }
    }

    override suspend fun setFavorite(domain: String, userId: String, isFavorite: Boolean) {
        domainDao.updateFavoriteStatus(domain, userId, isFavorite)
    }

    override fun observeFavorites(userId: String): kotlinx.coroutines.flow.Flow<List<Domain>> =
        domainDao.getFavoriteDomains(userId)
}



