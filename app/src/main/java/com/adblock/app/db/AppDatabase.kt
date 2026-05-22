package com.adblock.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adblock.app.accessibility.AccessibilityRuleDao
import com.adblock.app.accessibility.AccessibilityRuleEntity
import com.adblock.app.stats.AdSkipLogDao
import com.adblock.app.stats.AdSkipLogEntity
import com.adblock.app.stats.StatsDao
import com.adblock.app.stats.StatsEntity

@Database(
    entities = [
        AppPreferenceEntity::class,
        DnsLogEntity::class,
        UserRuleEntity::class,
        RuleListMetaEntity::class,
        AccessibilityRuleEntity::class,
        StatsEntity::class,
        AdSkipLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun dnsLogDao(): DnsLogDao
    abstract fun userRuleDao(): UserRuleDao
    abstract fun ruleListMetaDao(): RuleListMetaDao
    abstract fun accessibilityRuleDao(): AccessibilityRuleDao
    abstract fun statsDao(): StatsDao
    abstract fun adSkipLogDao(): AdSkipLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adblock.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private val MIGRATION_3_4 = Migration(3, 4) { db ->
        db.execSQL("CREATE TABLE IF NOT EXISTS `accessibility_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `ruleJson` TEXT NOT NULL, `enabled` INTEGER NOT NULL DEFAULT 1, `createdAt` INTEGER NOT NULL, `source` TEXT NOT NULL DEFAULT 'builtin')")
        db.execSQL("ALTER TABLE `rule_list_meta` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'easylist'")
        db.execSQL("CREATE TABLE IF NOT EXISTS `hourly_stats` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `hourTimestamp` INTEGER NOT NULL, `blockedCount` INTEGER NOT NULL DEFAULT 0, `queryCount` INTEGER NOT NULL DEFAULT 0, `dataSavedBytes` INTEGER NOT NULL DEFAULT 0)")
    }

    private val MIGRATION_4_5 = Migration(4, 5) { db ->
        db.execSQL("CREATE TABLE IF NOT EXISTS `ad_skip_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `ruleSummary` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL)")
    }
}
