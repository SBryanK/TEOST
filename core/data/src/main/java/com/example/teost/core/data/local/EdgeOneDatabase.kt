package com.example.teost.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.teost.data.local.dao.DomainDao
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.model.Domain
import com.example.teost.data.model.TestResult

@Database(
    entities = [
        TestResult::class,
        Domain::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EdgeOneDatabase : RoomDatabase() {
    
    abstract fun testResultDao(): TestResultDao
    abstract fun domainDao(): DomainDao
    // Cloud sync removed: no SyncStateDao
    
    companion object {
        const val DATABASE_NAME = "edgeone_security_db"
    }
}
