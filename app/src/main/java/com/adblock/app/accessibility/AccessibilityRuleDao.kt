package com.adblock.app.accessibility

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccessibilityRuleDao {
    @Query("SELECT * FROM accessibility_rules WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledRules(): List<AccessibilityRuleEntity>

    @Query("SELECT * FROM accessibility_rules ORDER BY createdAt DESC")
    suspend fun getAll(): List<AccessibilityRuleEntity>

    @Query("SELECT * FROM accessibility_rules WHERE packageName = :pkg AND enabled = 1")
    suspend fun getRulesForPackage(pkg: String): List<AccessibilityRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AccessibilityRuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<AccessibilityRuleEntity>)

    @Query("DELETE FROM accessibility_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accessibility_rules WHERE source = 'upstream'")
    suspend fun clearUpstream()

    @Query("UPDATE accessibility_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM accessibility_rules")
    suspend fun count(): Int
}
