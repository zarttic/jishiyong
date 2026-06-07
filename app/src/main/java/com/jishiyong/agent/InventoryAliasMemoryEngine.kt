package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

data class InventoryAliasMemory(
    val alias: String,
    val canonicalName: String,
    val categoryName: String? = null,
    val hits: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

class InventoryAliasMemoryEngine {

    fun relevantMemoriesFor(
        records: List<InventoryAliasMemory>,
        input: String,
        activeItems: List<Item>,
        limit: Int = DEFAULT_RELEVANT_LIMIT
    ): List<AgentMemory> {
        val inputKey = memoryKey(input)
        if (inputKey.isBlank() || records.isEmpty()) return emptyList()

        val activeItemKeys = activeItems.map { memoryKey(it.name) }.filter { it.isNotBlank() }
        return records
            .mapNotNull { record ->
                val aliasKey = memoryKey(record.alias)
                val canonicalKey = memoryKey(record.canonicalName)
                if (aliasKey.isBlank() || canonicalKey.isBlank()) return@mapNotNull null

                var score = 0f
                if (inputKey.contains(aliasKey) || aliasKey.contains(inputKey)) score += 6f
                if (inputKey.contains(canonicalKey) || canonicalKey.contains(inputKey)) score += 4f
                if (score <= 0f && charOverlap(inputKey, aliasKey) >= MIN_OVERLAP_SCORE) score += 1f
                if (score <= 0f) return@mapNotNull null
                if (activeItemKeys.any { it.contains(canonicalKey) || canonicalKey.contains(it) }) score += 3f

                AgentMemory(
                    key = "inventory_alias:${record.alias}",
                    value = buildMemoryValue(record),
                    weight = score + record.hits.coerceAtMost(10) / 10f
                )
            }
            .sortedByDescending { it.weight }
            .take(limit)
    }

    fun learnFromAction(
        records: List<InventoryAliasMemory>,
        recognizedText: String,
        action: InventoryAction,
        matchedItem: Item?,
        now: Long = System.currentTimeMillis()
    ): List<InventoryAliasMemory> {
        val canonicalName = canonicalNameFor(action, matchedItem).trim()
        if (canonicalName.isBlank()) return records

        val categoryName = matchedItem?.category?.name ?: categoryNameFor(action)
        val aliases = aliasCandidates(recognizedText, action, canonicalName)
        if (aliases.isEmpty()) return records

        val mutableRecords = records.toMutableList()
        aliases.forEach { alias ->
            val existingIndex = mutableRecords.indexOfFirst {
                memoryKey(it.alias) == memoryKey(alias) &&
                        memoryKey(it.canonicalName) == memoryKey(canonicalName)
            }
            if (existingIndex >= 0) {
                val existing = mutableRecords[existingIndex]
                mutableRecords[existingIndex] = existing.copy(
                    hits = existing.hits + 1,
                    updatedAt = now,
                    categoryName = existing.categoryName ?: categoryName
                )
            } else {
                mutableRecords += InventoryAliasMemory(
                    alias = alias,
                    canonicalName = canonicalName,
                    categoryName = categoryName,
                    hits = 1,
                    updatedAt = now
                )
            }
        }

        return mutableRecords
            .sortedWith(compareByDescending<InventoryAliasMemory> { it.updatedAt }.thenByDescending { it.hits })
            .take(MAX_MEMORY_RECORDS)
    }

    private fun canonicalNameFor(action: InventoryAction, matchedItem: Item?): String {
        return when (action) {
            is InventoryAction.AddItem -> action.draft.name
            is InventoryAction.ConsumeItem -> matchedItem?.name ?: action.itemName
            is InventoryAction.DiscardItem -> matchedItem?.name ?: action.itemName
            is InventoryAction.AskClarification -> ""
        }
    }

    private fun categoryNameFor(action: InventoryAction): String? {
        return when (action) {
            is InventoryAction.AddItem -> action.draft.category.name
            else -> null
        }
    }

    private fun aliasCandidates(
        recognizedText: String,
        action: InventoryAction,
        canonicalName: String
    ): List<String> {
        val canonicalKey = memoryKey(canonicalName)
        val candidates = mutableListOf<String>()
        when (action) {
            is InventoryAction.ConsumeItem -> candidates += action.itemName
            is InventoryAction.DiscardItem -> candidates += action.itemName
            is InventoryAction.AddItem -> candidates += action.draft.name
            is InventoryAction.AskClarification -> Unit
        }

        candidates += cleanedAlias(recognizedText)

        return candidates
            .map { it.trim() }
            .filter { it.length in MIN_ALIAS_LENGTH..MAX_ALIAS_LENGTH }
            .filter { memoryKey(it) != canonicalKey }
            .distinctBy { memoryKey(it) }
    }

    private fun cleanedAlias(text: String): String {
        var value = text
            .replace(Regex("\\s+"), "")
            .replace(Regex("[,.;:!?，。；：！？、]"), "")
            .replace(yearMonthDayRegex, "")
            .replace(monthDayRegex, "")
            .replace(relativeExpirationRegex, "")
            .replace(quantityRegex, "")

        commandNoiseWords.forEach { word ->
            value = value.replace(word, "")
        }
        return value
    }

    private fun memoryKey(text: String): String {
        var value = text
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[,.;:!?，。；：！？、]"), "")
            .replace("的", "")
        memoryKeyNoiseWords.forEach { word ->
            value = value.replace(word, "")
        }
        return value
    }

    private fun buildMemoryValue(record: InventoryAliasMemory): String {
        val categoryText = record.categoryName?.let { "，分类 $it" }.orEmpty()
        return "用户说“${record.alias}”时通常指库存“${record.canonicalName}”$categoryText。"
    }

    private fun charOverlap(left: String, right: String): Float {
        if (left.length < MIN_ALIAS_LENGTH || right.length < MIN_ALIAS_LENGTH) return 0f
        val leftChars = left.toSet()
        val rightChars = right.toSet()
        return leftChars.intersect(rightChars).size.toFloat() / leftChars.size.coerceAtLeast(1)
    }

    private companion object {
        private const val DEFAULT_RELEVANT_LIMIT = 8
        private const val MAX_MEMORY_RECORDS = 120
        private const val MIN_ALIAS_LENGTH = 2
        private const val MAX_ALIAS_LENGTH = 24
        private const val MIN_OVERLAP_SCORE = 0.6f

        private const val numberPattern = "([0-9]+|[零〇一二两三四五六七八九十百]+)"
        private const val unitPattern = "(瓶|盒|箱|袋|包|个|件|支|罐|杯|片|板|条|桶|听)?"
        private val quantityRegex = Regex("$numberPattern$unitPattern")
        private val yearMonthDayRegex = Regex("([0-9]{4}年)?${numberPattern}月${numberPattern}(日|号)?")
        private val monthDayRegex = Regex("${numberPattern}月${numberPattern}(日|号)?")
        private val relativeExpirationRegex = Regex("${numberPattern}天后")

        private val commandNoiseWords = listOf(
            "帮我", "请", "今天", "昨天", "前天", "刚刚", "刚",
            "新增了", "添加了", "购买了", "采购了", "买了", "入手了", "囤了",
            "新增", "添加", "购买", "采购", "买", "入手", "囤",
            "喝了", "吃了", "用了", "用掉了", "使用了", "消耗了",
            "喝", "吃", "用掉", "使用", "消耗",
            "扔掉了", "丢掉了", "处理掉了", "倒掉了", "扔了", "丢了",
            "扔掉", "丢掉", "处理掉", "倒掉", "丢弃", "废弃", "扔", "丢",
            "过期的", "过期", "到期", "保质期", "里面有", "总共", "一共", "共", "有",
            "我", "把", "了", "的"
        ).sortedByDescending { it.length }

        private val memoryKeyNoiseWords = listOf("的")
    }
}
