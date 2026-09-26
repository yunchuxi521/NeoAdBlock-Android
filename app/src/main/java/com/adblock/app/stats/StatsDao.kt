package com.adblock.app.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StatsDao {

    @Query("SELECT * FROM hourly_stats WHERE hourTimestamp = :hourTs LIMIT 1")
    suspend fun getByHour(hourTs: Long): StatsEntity?

    @Query("SELECT * FROM hourly_stats ORDER BY hourTimestamp DESC LIMIT 24")
    suspend fun getRecentHours(): List<StatsEntity>

    @Query("SELECT COALESCE(SUM(blockedCount), 0) FROM hourly_stats WHERE hourTimestamp >= :todayStart")
    suspend fun getTodayBlockedCount(todayStart: Long): Int

    @Query("SELECT COALESCE(SUM(blockedCount), 0) FROM hourly_stats")
    suspend fun getTotalBlockedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: StatsEntity)
}
