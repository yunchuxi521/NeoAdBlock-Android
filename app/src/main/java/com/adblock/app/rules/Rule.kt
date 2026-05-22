package com.adblock.app.rules

data class Rule(
    val pattern: String,
    val type: Type
) {
    enum class Type { Block, Allow }
}
