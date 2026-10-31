package com.adblock.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_list_meta")
data class RuleListMetaEntity(
    @PrimaryKey val name: String,
    val url: String,
    val lastUpdated: Long = 0,
    val ruleCount: Int = 0,
    val enabled: Boolean = true,
    val type: String = "easylist"   // "easylist" or "accessibility"
)
