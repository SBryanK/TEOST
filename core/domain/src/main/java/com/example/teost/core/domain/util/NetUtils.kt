package com.example.teost.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DnsResolver {
    private fun extractHost(normalizedUrlOrHost: String): String {
        return try {
            java.net.URL(
                if (normalizedUrlOrHost.startsWith("http")) normalizedUrlOrHost else "https://$normalizedUrlOrHost"
            ).host
        } catch (_: Exception) {
            normalizedUrlOrHost
        }
    }

    suspend fun resolveToIpList(normalizedUrlOrHost: String): List<String> = withContext(Dispatchers.IO) {
        val host = extractHost(normalizedUrlOrHost)
        return@withContext try {
            java.net.InetAddress.getAllByName(host).mapNotNull { it.hostAddress }.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object TargetRegistry {
    private val resolved: MutableMap<String, List<String>> = mutableMapOf()
    private var lastTarget: String? = null

    fun putResolved(url: String, ipList: List<String>) { resolved[url] = ipList }
    fun setLastTarget(url: String) { lastTarget = url }
    fun getLastTarget(): String? = lastTarget
    fun getResolved(url: String): List<String>? = resolved[url]
}


