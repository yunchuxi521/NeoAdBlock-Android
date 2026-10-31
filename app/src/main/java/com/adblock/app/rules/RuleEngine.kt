package com.adblock.app.rules

class RuleEngine {

    private val blockedDomains = mutableSetOf<String>()
    private val allowedDomains = mutableSetOf<String>()

    fun loadRules(rules: List<Rule>) {
        for (rule in rules) {
            when (rule.type) {
                Rule.Type.Block -> blockedDomains.add(rule.pattern)
                Rule.Type.Allow -> allowedDomains.add(rule.pattern)
            }
        }
    }

    fun matches(domain: String): Boolean {
        if (allowedDomains.contains(domain)) return false

        var current = domain
        while (current.isNotEmpty()) {
            if (blockedDomains.contains(current)) return true
            val dotIndex = current.indexOf('.')
            if (dotIndex < 0) break
            current = current.substring(dotIndex + 1)
        }
        return false
    }

    fun addRule(rule: Rule) {
        when (rule.type) {
            Rule.Type.Block -> blockedDomains.add(rule.pattern)
            Rule.Type.Allow -> allowedDomains.add(rule.pattern)
        }
    }

    fun removeRule(rule: Rule) {
        when (rule.type) {
            Rule.Type.Block -> blockedDomains.remove(rule.pattern)
            Rule.Type.Allow -> allowedDomains.remove(rule.pattern)
        }
    }

    fun getBlockedCount(): Int = blockedDomains.size
}
