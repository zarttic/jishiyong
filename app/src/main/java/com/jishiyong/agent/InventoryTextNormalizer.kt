package com.jishiyong.agent

object InventoryTextNormalizer {
    fun compact(text: String): String {
        return text
            .replace(Regex("\\s+"), "")
            .replace(Regex("[,.;:!?，。；：！？、]"), "")
            .trim()
    }
}
