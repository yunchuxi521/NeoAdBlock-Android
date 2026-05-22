package com.adblock.app.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.adblock.app.accessibility.AccessibilityRuleEntity
import com.adblock.app.accessibility.AccessibilityRuleParser
import com.adblock.app.db.AppDatabase
import com.adblock.app.db.RuleListMetaEntity
import com.adblock.app.db.UserRuleEntity
import com.adblock.app.rules.EasyListParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.TimeUnit

class RuleSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(applicationContext)
            val metas = database.ruleListMetaDao().getAll()

            for (meta in metas) {
                if (!meta.enabled) continue
                try {
                    when (meta.type) {
                        "accessibility" -> syncAccessibilityRules(database, meta.url)
                        else -> syncEasyListRules(database, meta)
                    }
                } catch (e: Exception) {
                    Log.w("RuleSync", "Failed to sync ${meta.name}", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("RuleSync", "Sync failed", e)
            Result.retry()
        }
    }

    private suspend fun syncEasyListRules(database: AppDatabase, meta: RuleListMetaEntity) {
        val content = URL(meta.url).readText()
        val result = EasyListParser.parse(content)
        val totalCount = result.blockDomains.size + result.allowDomains.size
        if (totalCount == 0) return

        database.userRuleDao().clearUpstreamRules()

        val entities = result.blockDomains.map { domain ->
            UserRuleEntity(domain = domain, ruleType = "block", source = "upstream")
        } + result.allowDomains.map { domain ->
            UserRuleEntity(domain = domain, ruleType = "allow", source = "upstream")
        }
        database.userRuleDao().insertAll(entities)
        database.ruleListMetaDao().updateStats(meta.name, System.currentTimeMillis(), totalCount)
    }

    private suspend fun syncAccessibilityRules(database: AppDatabase, url: String) {
        val json = URL(url).readText()
        val rules = AccessibilityRuleParser.parseRules(json)
        if (rules.isEmpty()) return

        val entities = rules.map { rule ->
            AccessibilityRuleEntity(
                packageName = rule.packageName,
                ruleJson = AccessibilityRuleParser.toJson(listOf(rule)),
                enabled = rule.enabled,
                source = "upstream"
            )
        }
        database.accessibilityRuleDao().clearUpstream()
        database.accessibilityRuleDao().insertAll(entities)
    }

    companion object {
        private const val WORK_NAME = "rule_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RuleSyncWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RuleSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
