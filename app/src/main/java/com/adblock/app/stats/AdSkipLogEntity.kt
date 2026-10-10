package com.adblock.app.stats

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ad_skip_logs")
data class AdSkipLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val ruleSummary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
