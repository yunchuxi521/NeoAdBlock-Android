package com.adblock.app

import com.adblock.app.accessibility.AccessibilityRuleParser
import com.adblock.app.accessibility.MatchCriterion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AccessibilityRuleParserTest {

    @Test
    fun `parse valid JSON rule`() {
        val json = """[{
            "package": "com.example.app",
            "enabled": true,
            "priority": 10,
            "match": [
                {"type": "id", "value": "skip_btn"},
                {"type": "text_regex", "value": ".*跳过.*"}
            ],
            "fallback": {"x": 0.9, "y": 0.1},
            "actions": ["click"]
        }]"""

        val rules = AccessibilityRuleParser.parseRules(json)
        assertEquals(1, rules.size)
        assertEquals("com.example.app", rules[0].packageName)
        assertEquals(2, rules[0].match.size)
        assertEquals("id", rules[0].match[0].type)
        assertEquals("skip_btn", rules[0].match[0].value)
        assertNotNull(rules[0].fallback)
        assertEquals(0.9, rules[0].fallback!!.x, 0.01)
        assertEquals(0.1, rules[0].fallback!!.y, 0.01)
    }

    @Test
    fun `parse empty array returns empty list`() {
        val rules = AccessibilityRuleParser.parseRules("[]")
        assertTrue(rules.isEmpty())
    }

    @Test
    fun `rules are sorted by priority descending`() {
        val json = """[
            {"package": "a", "priority": 5},
            {"package": "b", "priority": 10},
            {"package": "c", "priority": 1}
        ]"""
        val rules = AccessibilityRuleParser.parseRules(json)
        assertEquals(listOf("b", "a", "c"), rules.map { it.packageName })
    }

    @Test
    fun `serialize and deserialize preserves data`() {
        val rules = listOf(
            com.adblock.app.accessibility.AccessibilityRule(
                packageName = "com.test",
                priority = 5,
                match = listOf(MatchCriterion("id", "btn")),
                actions = listOf("click")
            )
        )
        val json = AccessibilityRuleParser.toJson(rules)
        val parsed = AccessibilityRuleParser.parseRules(json)
        assertEquals(1, parsed.size)
        assertEquals("com.test", parsed[0].packageName)
    }
}
