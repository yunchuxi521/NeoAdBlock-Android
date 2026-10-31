package com.adblock.app.stats

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hourly_stats")
data class StatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hourTimestamp: Long,
    val blockedCount: Int = 0,
    val queryCount: Int = 0,
    val dataSavedBytes: Long = 0L
)
