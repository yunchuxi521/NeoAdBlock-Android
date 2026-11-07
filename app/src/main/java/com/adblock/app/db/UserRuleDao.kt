package com.adblock.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserRuleDao {
    @Query("SELECT * FROM user_rules ORDER BY createdAt DESC")
    suspend fun getAll(): List<UserRuleEntity>

    @Query("SELECT * FROM user_rules WHERE source = 'upstream'")
    suspend fun getUpstreamRules(): List<UserRuleEntity>

    @Query("SELECT * FROM user_rules WHERE source != 'upstream'")
    suspend fun getUserRules(): List<UserRuleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: UserRuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<UserRuleEntity>)

    @Query("DELETE FROM user_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_rules WHERE source = 'upstream'")
    suspend fun clearUpstreamRules()

    @Query("SELECT COUNT(*) FROM user_rules")
    suspend fun count(): Int
}
