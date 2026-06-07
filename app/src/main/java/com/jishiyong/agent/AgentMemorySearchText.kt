package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

object AgentMemorySearchText {

    fun searchableTextFor(record: InventoryAliasMemory): String {
        return buildTokens(
            record.alias,
            record.canonicalName,
            record.categoryName.orEmpty()
        ).joinToString(" ")
    }

    fun keyFor(record: InventoryAliasMemory): String {
        return compactForSearch(record.alias)
    }

    fun ftsQueryFor(input: String): String {
        return buildTokens(input)
            .take(MAX_QUERY_TOKENS)
            .joinToString(" OR ") { quoteFtsToken(it) }
    }

    fun ftsQueryFor(input: String, activeItems: List<Item>): String {
        val inputTokens = buildTokens(input)
        if (inputTokens.isEmpty()) return ""

        val activeItemTokens = activeItems
            .asSequence()
            .flatMap { buildTokens(it.name).asSequence() }
            .take(MAX_ACTIVE_ITEM_TOKENS)
            .toList()

        return (inputTokens + activeItemTokens)
            .distinct()
            .take(MAX_QUERY_TOKENS)
            .joinToString(" OR ") { quoteFtsToken(it) }
    }

    private fun buildTokens(vararg values: String): List<String> {
        return values
            .flatMap { value ->
                val compact = compactForSearch(value)
                listOf(value.trim(), compact) + ngrams(compact)
            }
            .map { it.trim() }
            .filter { it.length >= MIN_TOKEN_LENGTH }
            .distinct()
    }

    private fun compactForSearch(value: String): String {
        return InventoryTextNormalizer.compact(value)
            .lowercase()
            .replace("的", "")
    }

    private fun ngrams(value: String): List<String> {
        if (value.length <= NGRAM_SIZE) return listOf(value)
        return value.windowed(NGRAM_SIZE)
    }

    private fun quoteFtsToken(token: String): String {
        return "\"${token.replace("\"", "\"\"")}\""
    }

    private const val MIN_TOKEN_LENGTH = 2
    private const val NGRAM_SIZE = 2
    private const val MAX_QUERY_TOKENS = 24
    private const val MAX_ACTIVE_ITEM_TOKENS = 8
}
