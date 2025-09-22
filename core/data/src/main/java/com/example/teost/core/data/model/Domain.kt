package com.example.teost.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.teost.data.local.Converters
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(
    tableName = "domains",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["domain"])
    ]
)
@TypeConverters(Converters::class)
@Parcelize
data class Domain(
    @PrimaryKey
    val domain: String,
    val ipAddresses: List<String> = emptyList(),
    val lastTested: Date = Date(),
    val testCount: Int = 0,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val sslEnabled: Boolean = true,
    val httpStatus: Int? = null,
    val responseTime: Long? = null,
    val cdnProvider: String? = null,
    val wafEnabled: Boolean? = null,
    val userId: String
) : Parcelable

@Parcelize
data class DomainValidation(
    val domain: String,
    val isValid: Boolean,
    val isReachable: Boolean,
    val ipAddresses: List<String> = emptyList(),
    val sslCertificate: SslCertificateInfo? = null,
    val headers: Map<String, String> = emptyMap(),
    val cdnDetected: CdnProvider? = null,
    val validationTime: Date = Date()
) : Parcelable

@Parcelize
data class SslCertificateInfo(
    val issuer: String,
    val subject: String,
    val validFrom: Date,
    val validTo: Date,
    val isValid: Boolean,
    val fingerprint: String
) : Parcelable

@Parcelize
enum class CdnProvider : Parcelable {
    EDGEONE,
    CLOUDFLARE,
    AKAMAI,
    CLOUDFRONT,
    FASTLY,
    UNKNOWN
}

@Parcelize
data class TestTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: TestCategory,
    val tests: List<SecurityTest>,
    val defaultConfiguration: TestParameters,
    val estimatedCredits: Int,
    val tags: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val userId: String
) : Parcelable
