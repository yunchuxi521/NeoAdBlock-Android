package com.adblock.app

import com.adblock.app.rules.Rule
import com.adblock.app.rules.RuleEngine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RuleEngineTest {

    private lateinit var engine: RuleEngine

    @BeforeEach
    fun setUp() {
        engine = RuleEngine()
        engine.loadRules(listOf(
            Rule("ads.example.com", Rule.Type.Block),
            Rule("tracker.example.com", Rule.Type.Block),
            Rule("doubleclick.net", Rule.Type.Block)
        ))
    }

    @Test
    fun `exact domain match returns blocked`() {
        assertTrue(engine.matches("ads.example.com"))
    }

    @Test
    fun `subdomain of blocked domain is also blocked`() {
        assertTrue(engine.matches("sub.ads.example.com"))
    }

    @Test
    fun `non-matching domain returns allowed`() {
        assertFalse(engine.matches("example.com"))
        assertFalse(engine.matches("google.com"))
    }

    @Test
    fun `allow rule overrides block for exact domain`() {
        engine.addRule(Rule("ads.example.com", Rule.Type.Allow))
        assertFalse(engine.matches("ads.example.com"))
    }

    @Test
    fun `subdomain matching with multiple labels`() {
        assertTrue(engine.matches("foo.bar.ads.example.com"))
    }

    @Test
    fun `empty domain returns false`() {
        assertFalse(engine.matches(""))
    }

    @Test
    fun `addRule and removeRule work correctly`() {
        engine.addRule(Rule("newad.net", Rule.Type.Block))
        assertTrue(engine.matches("newad.net"))
        engine.removeRule(Rule("newad.net", Rule.Type.Block))
        assertFalse(engine.matches("newad.net"))
    }

    @Test
    fun `load built-in ad rules from string list`() {
        val ruleTexts = listOf(
            "||ads.example.com^",
            "||doubleclick.net^",
            "||google-analytics.com^"
        )
        val rules = ruleTexts.map { Rule(it.removePrefix("||").removeSuffix("^"), Rule.Type.Block) }
        val engine2 = RuleEngine()
        engine2.loadRules(rules)
        assertTrue(engine2.matches("ads.example.com"))
        assertTrue(engine2.matches("doubleclick.net"))
        assertFalse(engine2.matches("google.com"))
    }
}
