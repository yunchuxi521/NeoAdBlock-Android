package com.adblock.app.vpn

import com.adblock.app.db.AppDatabase
import com.adblock.app.db.DnsLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DnsRequestLogger(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val recentUnblockedDomains = linkedMapOf<String, Long>()
    private val maxRecentEntries = 200

    fun logQuery(
        domain: String,
        packageName: String = "",
        wasBlocked: Boolean
    ) {
        val timestamp = System.currentTimeMillis()

        // Write to Room async
        scope.launch {
            database.dnsLogDao().insert(
                DnsLogEntity(
                    timestamp = timestamp,
                    domain = domain,
                    packageName = packageName,
                    wasBlocked = wasBlocked
                )
            )
        }

        // Keep unblocked domains in memory for "Report Missing Ad"
        if (!wasBlocked) {
            synchronized(recentUnblockedDomains) {
                recentUnblockedDomains[domain] = timestamp
                if (recentUnblockedDomains.size > maxRecentEntries) {
                    val oldest = recentUnblockedDomains.keys.first()
                    recentUnblockedDomains.remove(oldest)
                }
            }
        }
    }

    fun getRecentUnblockedDomains(limit: Int = 50): List<String> {
        synchronized(recentUnblockedDomains) {
            return recentUnblockedDomains.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }
    }

    fun clearRecent() {
        synchronized(recentUnblockedDomains) {
            recentUnblockedDomains.clear()
        }
    }
}
