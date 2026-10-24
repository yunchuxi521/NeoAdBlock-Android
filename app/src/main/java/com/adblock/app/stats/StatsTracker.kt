package com.adblock.app.stats

import com.adblock.app.db.AppDatabase
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class StatsTracker(private val database: AppDatabase) {

    private val totalBlocked = AtomicLong(0)
    private val totalDnsQueries = AtomicLong(0)
    private var currentHourStart = truncateHour(System.currentTimeMillis())

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var flushJob: Job? = null

    init {
        startPeriodicFlush()
    }

    fun incrementBlocked() {
        totalBlocked.incrementAndGet()
    }

    fun incrementQueries() {
        totalDnsQueries.incrementAndGet()
    }

    fun getBlockedCount(): Long = totalBlocked.get()

    fun getQueryCount(): Long = totalDnsQueries.get()

    fun reset() {
        totalBlocked.set(0)
        totalDnsQueries.set(0)
        currentHourStart = truncateHour(System.currentTimeMillis())
    }

    /** Flush current counters to Room and start a new hour bucket */
    fun flushCurrentHour() {
        val blocked = totalBlocked.getAndSet(0)
        val queries = totalDnsQueries.getAndSet(0)
        if (blocked == 0L && queries == 0L) return
        scope.launch {
            persistHourStats(currentHourStart, blocked.toInt(), queries.toInt())
        }
        currentHourStart = truncateHour(System.currentTimeMillis())
    }

    /** Shut down the periodic flush and do a final write */
    fun shutdown() {
        flushJob?.cancel()
        flushCurrentHour()
        scope.cancel()
    }

    private fun startPeriodicFlush() {
        flushJob = scope.launch {
            while (isActive) {
                delay(60_000L) // flush every 60 seconds
                flushCurrentHour()
            }
        }
    }

    private suspend fun persistHourStats(hourTs: Long, blocked: Int, queries: Int) {
        val existing = database.statsDao().getByHour(hourTs)
        if (existing != null) {
            database.statsDao().upsert(existing.copy(
                blockedCount = existing.blockedCount + blocked,
                queryCount = existing.queryCount + queries,
                dataSavedBytes = existing.dataSavedBytes + (blocked.toLong() * AVG_DATA_PER_BLOCK)
            ))
        } else {
            database.statsDao().upsert(StatsEntity(
                hourTimestamp = hourTs,
                blockedCount = blocked,
                queryCount = queries,
                dataSavedBytes = blocked.toLong() * AVG_DATA_PER_BLOCK
            ))
        }
    }

    private fun truncateHour(millis: Long): Long {
        return millis / 3_600_000L * 3_600_000L
    }

    companion object {
        const val AVG_DATA_PER_BLOCK = 150_000L   // ~150KB per blocked ad
        const val AVG_TIME_PER_BLOCK = 500L        // ~0.5s per blocked ad

        fun estimateDataSavedBytes(blockedCount: Long): Long = blockedCount * AVG_DATA_PER_BLOCK
        fun estimateTimeSavedSec(blockedCount: Long): Long = blockedCount * AVG_TIME_PER_BLOCK / 1000
    }
}
