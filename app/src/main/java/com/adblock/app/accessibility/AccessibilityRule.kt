// AccessibilityRule.kt
package com.adblock.app.accessibility

/**
 * JSON-driven rule for detecting and closing ad overlays / splash screens.
 *
 * JSON example:
 * {
 *   "package": "com.example.app",
 *   "enabled": true,
 *   "priority": 10,
 *   "match": [
 *     { "type": "id", "value": "tt_splash_skip_btn" },
 *     { "type": "text_regex", "value": ".*跳过.*" },
 *     { "type": "desc_contains", "value": "关闭" },
 *     { "type": "class", "value": "android.widget.Button", "extra": { "text_contains": "skip" } }
 *   ],
 *   "fallback": { "x": 0.9, "y": 0.1 },
 *   "actions": ["click"]
 * }
 */

data class AccessibilityRule(
    val packageName: String,
    val enabled: Boolean = true,
    val priority: Int = 10,
    val match: List<MatchCriterion> = emptyList(),
    val fallback: CoordinateFallback? = null,
    val actions: List<String> = listOf("click")
)

data class MatchCriterion(
    val type: String,
    val value: String? = null,
    val extra: Map<String, String>? = null
)

data class CoordinateFallback(
    val x: Double,
    val y: Double
)

object AccessibilityRuleParser {

    fun parseRules(json: String): List<AccessibilityRule> {
        val root = org.json.JSONArray(json)
        val rules = mutableListOf<AccessibilityRule>()
        for (i in 0 until root.length()) {
            val obj = root.getJSONObject(i)
            val rule = parseRule(obj)
            if (rule != null) rules.add(rule)
        }
        return rules.sortedByDescending { it.priority }
    }

    fun parseRule(obj: org.json.JSONObject): AccessibilityRule? {
        return try {
            val packageName = obj.getString("package") ?: return null
            val enabled = obj.optBoolean("enabled", true)
            val priority = obj.optInt("priority", 10)
            val matchArray = obj.optJSONArray("match")
            val match = if (matchArray != null) {
                (0 until matchArray.length()).mapNotNull { i ->
                    parseCriterion(matchArray.getJSONObject(i))
                }
            } else emptyList()

            val fallbackObj = obj.optJSONObject("fallback")
            val fallback = if (fallbackObj != null) {
                CoordinateFallback(
                    x = fallbackObj.getDouble("x"),
                    y = fallbackObj.getDouble("y")
                )
            } else null

            val actionsArray = obj.optJSONArray("actions")
            val actions = if (actionsArray != null) {
                (0 until actionsArray.length()).map { actionsArray.getString(it) }
            } else listOf("click")

            AccessibilityRule(packageName, enabled, priority, match, fallback, actions)
        } catch (e: Exception) { null }
    }

    private fun parseCriterion(obj: org.json.JSONObject): MatchCriterion? {
        return try {
            val type = obj.getString("type") ?: return null
            val value = obj.optString("value", null)
            val extraObj = obj.optJSONObject("extra")
            val extra = if (extraObj != null) {
                val map = mutableMapOf<String, String>()
                extraObj.keys().forEach { key -> map[key] = extraObj.getString(key) }
                map
            } else null
            MatchCriterion(type, value, extra)
        } catch (e: Exception) { null }
    }

    /** Serialize a list of rules to JSON string */
    fun toJson(rules: List<AccessibilityRule>): String {
        val arr = org.json.JSONArray()
        for (rule in rules) {
            val obj = org.json.JSONObject()
            obj.put("package", rule.packageName)
            obj.put("enabled", rule.enabled)
            obj.put("priority", rule.priority)

            val matchArr = org.json.JSONArray()
            for (c in rule.match) {
                val cObj = org.json.JSONObject()
                cObj.put("type", c.type)
                if (c.value != null) cObj.put("value", c.value)
                if (c.extra != null) {
                    val extraObj = org.json.JSONObject()
                    c.extra.forEach { (k, v) -> extraObj.put(k, v) }
                    cObj.put("extra", extraObj)
                }
                matchArr.put(cObj)
            }
            obj.put("match", matchArr)

            if (rule.fallback != null) {
                val fObj = org.json.JSONObject()
                fObj.put("x", rule.fallback.x)
                fObj.put("y", rule.fallback.y)
                obj.put("fallback", fObj)
            }

            val actionsArr = org.json.JSONArray()
            rule.actions.forEach { actionsArr.put(it) }
            obj.put("actions", actionsArr)

            arr.put(obj)
        }
        return arr.toString(2)
    }
}
