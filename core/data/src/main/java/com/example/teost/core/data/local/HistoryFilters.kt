package com.example.teost.data.local

import com.example.teost.data.model.TestCategory
import com.example.teost.data.model.TestStatus
import com.example.teost.data.model.TestType

data class HistoryFilters(
    val category: TestCategory? = null,
    val status: TestStatus? = null,
    val type: TestType? = null,
    val fromEpochMs: Long? = null,
    val toEpochMs: Long? = null,
    val domainContains: String? = null
)


