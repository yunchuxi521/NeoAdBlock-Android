package com.adblock.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

object NodeMatcher {

    fun findMatchingNode(
        root: AccessibilityNodeInfo,
        criteria: List<MatchCriterion>
    ): AccessibilityNodeInfo? {
        if (criteria.isEmpty()) return null

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (matchesAll(node, criteria)) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return null
    }

    fun matchesAll(node: AccessibilityNodeInfo, criteria: List<MatchCriterion>): Boolean {
        return criteria.all { matchesSingle(node, it) }
    }

    private fun matchesSingle(node: AccessibilityNodeInfo, c: MatchCriterion): Boolean {
        return when (c.type) {
            "id" -> matchId(node, c.value ?: return false)
            "text_regex" -> matchTextRegex(node, c.value ?: return false)
            "text_contains" -> matchTextContains(node, c.value ?: return false)
            "class" -> matchClass(node, c.value ?: return false, c.extra)
            "desc_contains" -> matchDescContains(node, c.value ?: return false)
            "desc_regex" -> matchDescRegex(node, c.value ?: return false)
            "clickable" -> node.isClickable
            else -> false
        }
    }

    private fun matchId(node: AccessibilityNodeInfo, expectedId: String): Boolean {
        val viewId = node.viewIdResourceName ?: return false
        return viewId.contains(expectedId, ignoreCase = true) ||
               viewId.endsWith(":$expectedId", ignoreCase = true)
    }

    private fun matchTextRegex(node: AccessibilityNodeInfo, regex: String): Boolean {
        val text = node.text?.toString() ?: return false
        return try {
            Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).matches()
        } catch (e: Exception) { false }
    }

    private fun matchTextContains(node: AccessibilityNodeInfo, text: String): Boolean {
        val nodeText = node.text?.toString() ?: return false
        return nodeText.contains(text, ignoreCase = true)
    }

    private fun matchClass(node: AccessibilityNodeInfo, className: String, extra: Map<String, String>?): Boolean {
        val clazz = node.className?.toString() ?: return false
        if (!clazz.equals(className, ignoreCase = true) &&
            !clazz.endsWith(className, ignoreCase = true)) return false

        if (extra != null) {
            for ((key, value) in extra) {
                when (key) {
                    "text_contains" -> if (!matchTextContains(node, value)) return false
                    "text_regex" -> if (!matchTextRegex(node, value)) return false
                    "desc_contains" -> if (!matchDescContains(node, value)) return false
                    "clickable" -> if (node.isClickable != value.toBoolean()) return false
                }
            }
        }
        return true
    }

    private fun matchDescContains(node: AccessibilityNodeInfo, text: String): Boolean {
        val desc = node.contentDescription?.toString() ?: return false
        return desc.contains(text, ignoreCase = true)
    }

    private fun matchDescRegex(node: AccessibilityNodeInfo, regex: String): Boolean {
        val desc = node.contentDescription?.toString() ?: return false
        return try {
            Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(desc).matches()
        } catch (e: Exception) { false }
    }
}
