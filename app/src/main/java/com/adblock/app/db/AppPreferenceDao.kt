package com.adblock.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppPreferenceDao {
    @Query("SELECT * FROM app_preferences ORDER BY appName ASC")
    suspend fun getAll(): List<AppPreferenceEntity>

    @Query("SELECT * FROM app_preferences WHERE bypassVpn = 1")
    suspend fun getBypassApps(): List<AppPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: AppPreferenceEntity)

    @Query("UPDATE app_preferences SET bypassVpn = :bypass WHERE packageName = :packageName")
    suspend fun setBypass(packageName: String, bypass: Boolean)

    @Query("DELETE FROM app_preferences")
    suspend fun clearAll()
}
