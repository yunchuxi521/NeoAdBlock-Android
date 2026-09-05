package com.adblock.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_logs")
data class DnsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val domain: String,
    val packageName: String = "",
    val wasBlocked: Boolean,
    val sourceApp: String = ""
)
