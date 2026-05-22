package com.adblock.app

import android.app.Application
import com.adblock.app.accessibility.AccessibilityRuleEntity
import com.adblock.app.db.AppDatabase
import com.adblock.app.db.RuleListMetaEntity
import com.adblock.app.sync.RuleSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdBlockApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        // Seed default rule list sources
        CoroutineScope(Dispatchers.IO).launch {
            val existing = database.ruleListMetaDao().getAll()
            if (existing.isEmpty()) {
                database.ruleListMetaDao().upsert(
                    RuleListMetaEntity(
                        name = "EasyList China",
                        url = "https://easylist-downloads.adblockplus.org/easylistchina+easylist.txt",
                        enabled = true
                    )
                )
            }
        }

        // Seed default accessibility rules
        CoroutineScope(Dispatchers.IO).launch {
            val existingAccRules = database.accessibilityRuleDao().getAll()
            if (existingAccRules.isEmpty()) {
                seedDefaultAccessibilityRules()
            }
        }

        // Seed default accessibility rule list URL
        CoroutineScope(Dispatchers.IO).launch {
            val existing = database.ruleListMetaDao().getAll()
            if (existing.none { it.type == "accessibility" }) {
                database.ruleListMetaDao().upsert(
                    RuleListMetaEntity(
                        name = "Accessibility Rules",
                        url = "https://raw.githubusercontent.com/AdblockAccessibility/rules/main/rules.json",
                        enabled = false,
                        type = "accessibility"
                    )
                )
            }
        }

        // Schedule daily sync
        RuleSyncWorker.schedule(this)
    }

    private suspend fun seedDefaultAccessibilityRules() {
        val defaultRules = listOf(
            AccessibilityRuleEntity(
                packageName = "com.ss.android.ugc.aweme",  // 抖音
                ruleJson = """{"package":"com.ss.android.ugc.aweme","priority":10,"match":[{"type":"text_contains","value":"跳过"},{"type":"id","value":"skip"}],"fallback":{"x":0.9,"y":0.1},"actions":["click"]}""",
                source = "builtin"
            ),
            AccessibilityRuleEntity(
                packageName = "com.taobao.taobao",  // 淘宝
                ruleJson = """{"package":"com.taobao.taobao","priority":10,"match":[{"type":"id","value":"skip"},{"type":"text_contains","value":"跳过"}],"fallback":{"x":0.95,"y":0.05},"actions":["click"]}""",
                source = "builtin"
            ),
            AccessibilityRuleEntity(
                packageName = "*",
                ruleJson = """{"package":"*","priority":1,"match":[{"type":"class","value":"android.widget.Button","extra":{"text_regex":".*[跳过关闭广告skip].*"}},{"type":"desc_regex","value":".*[关闭].*"}],"fallback":{"x":0.9,"y":0.1},"actions":["click"]}""",
                source = "builtin"
            )
        )
        for (rule in defaultRules) {
            database.accessibilityRuleDao().insert(rule)
        }
    }
}
