package com.adblock.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_rules")
data class UserRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val ruleType: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)
