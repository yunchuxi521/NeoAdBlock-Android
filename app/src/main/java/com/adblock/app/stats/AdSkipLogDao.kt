package com.adblock.app.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AdSkipLogDao {

    @Query("SELECT * FROM ad_skip_logs ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecent(): List<AdSkipLogEntity>

    @Query("SELECT COUNT(*) FROM ad_skip_logs WHERE timestamp >= :since")
    suspend fun getCountSince(since: Long): Int

    @Insert
    suspend fun insert(log: AdSkipLogEntity)
}
