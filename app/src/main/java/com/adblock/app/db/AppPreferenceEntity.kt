package com.adblock.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val bypassVpn: Boolean = false
)
