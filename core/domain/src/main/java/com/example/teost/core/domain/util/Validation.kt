package com.example.teost.util

object Validation {
    private val simpleHost = Regex("^[A-Za-z0-9.-]+(:\\d+)?(/.*)?$")
    private val ipv4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?$")
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isLikelyValidTarget(input: String): Boolean {
        if (input.isBlank() || input.contains(' ')) return false
        return input.startsWith("http://") || input.startsWith("https://") || simpleHost.matches(input) || ipv4.matches(input)
    }

    // Lightweight client-side email validator for UX only (server/Firebase is source of truth)
    fun isLikelyEmail(input: String): Boolean {
        if (input.isBlank()) return false
        if (input.contains("..")) return false
        if (!emailRegex.matches(input)) return false
        val parts = input.split('@')
        if (parts.size != 2) return false
        val (local, domain) = parts
        if (local.isBlank() || domain.isBlank()) return false
        if (domain.startsWith('.') || domain.endsWith('.')) return false
        return true
    }
}



