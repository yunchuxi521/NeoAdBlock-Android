package com.adblock.app.accessibility

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accessibility_rules")
data class AccessibilityRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val ruleJson: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "builtin"
)
