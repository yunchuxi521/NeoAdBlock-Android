package com.adblock.app.rules

object EasyListParser {

    fun parse(content: String): ParseResult {
        val blockRules = mutableListOf<String>()
        val allowRules = mutableListOf<String>()
        var commentLines = 0
        var skippedLines = 0

        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> return@forEach
                trimmed.startsWith("!") -> { commentLines++; return@forEach }
                trimmed.startsWith("@@") -> {
                    val domain = extractDomain(trimmed.removePrefix("@@"))
                    if (domain != null) allowRules.add(domain)
                    else skippedLines++
                }
                trimmed.startsWith("||") -> {
                    val domain = extractDomain(trimmed)
                    if (domain != null) blockRules.add(domain)
                    else skippedLines++
                }
                trimmed.startsWith("##") || trimmed.startsWith("#@#") ||
                trimmed.startsWith(".") || trimmed.startsWith("#") -> {
                    skippedLines++
                }
                trimmed.contains(".") && !trimmed.contains("/") && !trimmed.contains("=") -> {
                    blockRules.add(trimmed)
                }
                else -> skippedLines++
            }
        }

        return ParseResult(blockRules, allowRules, commentLines, skippedLines)
    }

    private fun extractDomain(raw: String): String? {
        var domain = raw
            .removeSuffix("^")
            .removeSuffix("|")
            .removePrefix("|")
            .trim()

        val slashIndex = domain.indexOf('/')
        if (slashIndex > 0) domain = domain.substring(0, slashIndex)

        if (domain.contains('.') && !domain.contains(' ') && domain.isNotBlank()) {
            return domain.lowercase()
        }
        return null
    }

    data class ParseResult(
        val blockDomains: List<String>,
        val allowDomains: List<String>,
        val commentLines: Int,
        val skippedLines: Int
    )
}
