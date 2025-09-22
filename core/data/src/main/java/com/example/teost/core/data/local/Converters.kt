package com.example.teost.data.local

import androidx.room.TypeConverter
import com.example.teost.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }
    }

    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, mapType)
        }
    }

    @TypeConverter
    fun fromMapString(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun fromTestCategory(category: TestCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toTestCategory(name: String?): TestCategory? {
        return name?.let { TestCategory.valueOf(it) }
    }

    @TypeConverter
    fun fromTestType(type: TestType?): String? {
        return type?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toTestType(json: String?): TestType? {
        return json?.let {
            when {
                it.contains("HttpSpike") -> TestType.HttpSpike
                it.contains("ConnectionFlood") -> TestType.ConnectionFlood
                it.contains("BasicConnectivity") -> TestType.BasicConnectivity
                it.contains("SqlInjection") -> TestType.SqlInjection
                it.contains("XssTest") -> TestType.XssTest
                it.contains("PathTraversal") -> TestType.PathTraversal
                it.contains("CustomRulesValidation") -> TestType.CustomRulesValidation
                it.contains("OversizedBody") -> TestType.OversizedBody
                it.contains("UserAgentAnomaly") -> TestType.UserAgentAnomaly
                it.contains("CookieJsChallenge") -> TestType.CookieJsChallenge
                it.contains("WebCrawlerSimulation") -> TestType.WebCrawlerSimulation
                it.contains("AuthenticationTest") -> TestType.AuthenticationTest
                it.contains("BruteForce") -> TestType.BruteForce
                it.contains("EnumerationIdor") -> TestType.EnumerationIdor
                it.contains("SchemaInputValidation") -> TestType.SchemaInputValidation
                it.contains("BusinessLogicAbuse") -> TestType.BusinessLogicAbuse
                else -> TestType.HttpSpike // Default fallback
            }
        }
    }

    @TypeConverter
    fun fromTestStatus(status: TestStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toTestStatus(name: String?): TestStatus? {
        return name?.let { TestStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromTestResultDetails(details: TestResultDetails?): String? {
        return details?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toTestResultDetails(json: String?): TestResultDetails? {
        return json?.let { gson.fromJson(it, TestResultDetails::class.java) }
    }

    @TypeConverter
    fun fromChallengeType(type: ChallengeType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toChallengeType(name: String?): ChallengeType? {
        return name?.let { ChallengeType.valueOf(it) }
    }
}
