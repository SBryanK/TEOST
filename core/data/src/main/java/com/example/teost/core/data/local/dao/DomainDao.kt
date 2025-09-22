package com.example.teost.data.local.dao

import androidx.room.*
import com.example.teost.data.model.Domain
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainDao {
    
    @Query("SELECT * FROM domains WHERE userId = :userId ORDER BY lastTested DESC")
    fun getAllDomains(userId: String): Flow<List<Domain>>
    
    @Query("SELECT * FROM domains WHERE userId = :userId AND isFavorite = 1 ORDER BY lastTested DESC")
    fun getFavoriteDomains(userId: String): Flow<List<Domain>>
    
    @Query("SELECT * FROM domains WHERE domain = :domain AND userId = :userId")
    suspend fun getDomainByName(domain: String, userId: String): Domain?
    
    @Query("""
        SELECT * FROM domains 
        WHERE userId = :userId 
        AND (domain LIKE '%' || :query || '%' 
            OR notes LIKE '%' || :query || '%')
        ORDER BY lastTested DESC
    """)
    fun searchDomains(userId: String, query: String): Flow<List<Domain>>
    
    @Query("SELECT * FROM domains WHERE userId = :userId ORDER BY testCount DESC LIMIT :limit")
    fun getMostTestedDomains(userId: String, limit: Int = 10): Flow<List<Domain>>
    
    @Query("SELECT COUNT(*) FROM domains WHERE userId = :userId")
    suspend fun getTotalDomainCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM domains WHERE userId = :userId AND isFavorite = 1")
    suspend fun getFavoriteDomainCount(userId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: Domain)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<Domain>)
    
    @Update
    suspend fun updateDomain(domain: Domain)
    
    @Query("UPDATE domains SET isFavorite = :isFavorite WHERE domain = :domain AND userId = :userId")
    suspend fun updateFavoriteStatus(domain: String, userId: String, isFavorite: Boolean)
    
    @Query("UPDATE domains SET testCount = testCount + 1 WHERE domain = :domain AND userId = :userId")
    suspend fun incrementTestCount(domain: String, userId: String)
    
    @Delete
    suspend fun deleteDomain(domain: Domain)
    
    @Query("DELETE FROM domains WHERE userId = :userId")
    suspend fun deleteAllDomains(userId: String)
}
