package com.adblock.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DnsLogDao {
    @Query("SELECT * FROM dns_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<DnsLogEntity>

    @Query("SELECT DISTINCT domain FROM dns_logs WHERE wasBlocked = 0 AND timestamp > :since ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentUnblockedDomains(since: Long): List<String>

    @Insert
    suspend fun insert(log: DnsLogEntity)

    @Query("DELETE FROM dns_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM dns_logs")
    suspend fun clearAll()
}
