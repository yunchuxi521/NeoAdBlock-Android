package com.adblock.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.adblock.app.db.AppDatabase
import com.adblock.app.stats.AdSkipLogEntity
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class CapturedInteraction(
    val packageName: String,
    val viewId: String?,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val isClickable: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AdSkipperService : AccessibilityService() {

    private lateinit var database: AppDatabase
    private lateinit var gestureExecutor: GestureExecutor
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ruleCache = ConcurrentHashMap<String, List<AccessibilityRule>>()
    private var lastProcessedPackage = ""
    private var lastProcessedTime = 0L
    private val debounceMs = 2000L

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        gestureExecutor = GestureExecutor(this)
        loadRules()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                val now = System.currentTimeMillis()

                if (packageName == lastProcessedPackage &&
                    (now - lastProcessedTime) < debounceMs) return

                lastProcessedPackage = packageName
                lastProcessedTime = now

                scope.launch {
                    delay(800)
                    withContext(Dispatchers.Main) {
                        processPackage(packageName)
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                if (ruleCache.containsKey(packageName)) {
                    scope.launch {
                        delay(300)
                        withContext(Dispatchers.Main) {
                            processPackage(packageName)
                        }
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (!isRecording) return
                captureInteraction(event)
            }
        }
    }

    private fun captureInteraction(event: AccessibilityEvent) {
        val source = event.source ?: return
        try {
            val interaction = CapturedInteraction(
                packageName = event.packageName?.toString() ?: "",
                viewId = source.viewIdResourceName,
                text = source.text?.toString(),
                contentDescription = source.contentDescription?.toString(),
                className = source.className?.toString(),
                isClickable = source.isClickable
            )
            Companion.recordedInteractions.add(interaction)
            Log.i("AdSkipper", "Captured interaction: ${interaction.packageName} / ${interaction.viewId}")
        } finally {
            source.recycle()
        }
    }

    private fun processPackage(packageName: String) {
        val exactRules = ruleCache[packageName].orEmpty()
        val wildcardRules = ruleCache["*"].orEmpty()
        val allRules = exactRules + wildcardRules
        if (allRules.isEmpty()) return

        val root = rootInActiveWindow ?: return

        try {
            for (rule in allRules) {
                if (!rule.enabled) continue

                if (rule.match.isNotEmpty()) {
                    val matched = NodeMatcher.findMatchingNode(root, rule.match)
                    if (matched != null) {
                        try {
                            Log.i("AdSkipper", "Match found: ${rule.packageName} via '${rule.match[0].type}'")
                            gestureExecutor.clickNode(matched)
                            logAdSkip(packageName, rule.match[0].type)
                        } finally {
                            matched.recycle()
                        }
                        return
                    }
                }

                if (rule.fallback != null) {
                    Log.i("AdSkipper", "Fallback tap: ${rule.packageName} at ${rule.fallback.x},${rule.fallback.y}")
                    scope.launch {
                        gestureExecutor.tapAtFraction(rule.fallback.x, rule.fallback.y)
                        logAdSkip(packageName, "fallback")
                    }
                    return
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun logAdSkip(packageName: String, ruleSummary: String) {
        scope.launch {
            try {
                database.adSkipLogDao().insert(
                    AdSkipLogEntity(
                        packageName = packageName,
                        ruleSummary = ruleSummary
                    )
                )
            } catch (e: Exception) {
                Log.w("AdSkipper", "Failed to log ad skip", e)
            }
        }
    }

    private fun loadRules() {
        scope.launch {
            try {
                val entities = database.accessibilityRuleDao().getEnabledRules()
                val parsed = entities.mapNotNull { entity ->
                    try {
                        val rules = AccessibilityRuleParser.parseRules("[${entity.ruleJson}]")
                        rules.firstOrNull()
                    } catch (e: Exception) { null }
                }

                ruleCache.clear()
                for (rule in parsed) {
                    ruleCache.getOrPut(rule.packageName) { mutableListOf() }
                    ruleCache[rule.packageName] = ruleCache[rule.packageName]!! + rule
                }

                Log.i("AdSkipper", "Loaded ${parsed.size} rules for ${ruleCache.size} packages")
            } catch (e: Exception) {
                Log.e("AdSkipper", "Failed to load rules", e)
            }
        }
    }

    fun reloadRules() {
        loadRules()
    }

    override fun onInterrupt() {
        Log.d("AdSkipper", "Service interrupted")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile var isRecording: Boolean = false
        val recordedInteractions = CopyOnWriteArrayList<CapturedInteraction>()

        fun startRecording() {
            recordedInteractions.clear()
            isRecording = true
        }

        fun stopRecording() {
            isRecording = false
        }

        fun generateRule(interaction: CapturedInteraction): AccessibilityRule {
            val match = mutableListOf<MatchCriterion>()

            if (!interaction.viewId.isNullOrBlank()) {
                match.add(MatchCriterion(type = "id", value = interaction.viewId))
            }
            if (!interaction.text.isNullOrBlank()) {
                match.add(MatchCriterion(type = "text_contains", value = interaction.text))
            }
            if (!interaction.contentDescription.isNullOrBlank()) {
                match.add(MatchCriterion(type = "desc_contains", value = interaction.contentDescription))
            }
            if (!interaction.className.isNullOrBlank()) {
                val extra = mutableMapOf<String, String>()
                if (!interaction.text.isNullOrBlank()) {
                    extra["text_contains"] = interaction.text
                }
                match.add(MatchCriterion(type = "class", value = interaction.className, extra = extra.ifEmpty { null }))
            }

            return AccessibilityRule(
                packageName = interaction.packageName,
                priority = 10,
                match = match,
                fallback = null,
                actions = listOf("click")
            )
        }
    }
}
