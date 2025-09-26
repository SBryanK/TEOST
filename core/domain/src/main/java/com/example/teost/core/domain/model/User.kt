package com.example.teost.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val emailVerified: Boolean = false,
    val credits: Int = 0, // No credits until email verified
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val lastLoginAt: Date = Date(),
    val favoriteDomainsCount: Int = 0,
    val totalTestsRun: Int = 0,
    val accountType: AccountType = AccountType.FREE,
    val needsVerification: Boolean = false // Flag for email verification flow
) : Parcelable

enum class AccountType {
    FREE,
    BASIC,
    PRO,
    ENTERPRISE
}

@Parcelize
data class UserCredits(
    val available: Int = 0,
    val used: Int = 0,
    val total: Int = 0,
    val lastUpdated: Date = Date(),
    val nextRefreshDate: Date? = null
) : Parcelable
