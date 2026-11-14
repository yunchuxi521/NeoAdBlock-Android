package com.adblock.app

import com.adblock.app.accessibility.MatchCriterion
import com.adblock.app.accessibility.NodeMatcher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NodeMatcherTest {

    @Test
    fun `criterion stores type and value correctly`() {
        val criterion = MatchCriterion("id", "skip_btn")
        assertEquals("id", criterion.type)
        assertEquals("skip_btn", criterion.value)
    }

    @Test
    fun `criterion with extra map stores correctly`() {
        val criterion = MatchCriterion(
            type = "class",
            value = "android.widget.Button",
            extra = mapOf("text_contains" to "skip")
        )
        assertEquals("class", criterion.type)
        assertEquals("android.widget.Button", criterion.value)
        assertEquals("skip", criterion.extra!!["text_contains"])
    }
}
