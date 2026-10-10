package com.adblock.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RuleListMetaDao {
    @Query("SELECT * FROM rule_list_meta")
    suspend fun getAll(): List<RuleListMetaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: RuleListMetaEntity)

    @Query("UPDATE rule_list_meta SET lastUpdated = :time, ruleCount = :count WHERE name = :name")
    suspend fun updateStats(name: String, time: Long, count: Int)

    @Query("DELETE FROM rule_list_meta WHERE name = :name")
    suspend fun delete(name: String)
}
