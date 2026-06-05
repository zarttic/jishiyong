package com.jishiyong.agent

import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.util.Constants
import java.time.LocalDate

class InventoryCommandParser {

    fun parse(text: String, today: LocalDate = LocalDate.now()): InventoryAction {
        val normalized = normalize(text)
        if (normalized.isBlank()) {
            return InventoryAction.AskClarification("没有识别到语音内容，请重试")
        }

        return when {
            normalized.containsAny(addKeywords) -> parseAdd(normalized, today)
            normalized.containsAny(discardKeywords) -> parseDiscard(normalized)
            normalized.containsAny(consumeKeywords) -> parseConsume(normalized)
            else -> InventoryAction.AskClarification("暂时只能处理新增、消耗或丢弃库存")
        }
    }

    fun inferCategory(name: String): ItemCategory {
        val normalizedName = normalize(name)
        return when {
            normalizedName.containsAny(drinkKeywords) -> ItemCategory.DRINK
            normalizedName.containsAny(medicineKeywords) -> ItemCategory.MEDICINE
            normalizedName.containsAny(cosmeticsKeywords) -> ItemCategory.COSMETICS
            normalizedName.containsAny(dailyKeywords) -> ItemCategory.DAILY
            normalizedName.containsAny(electronicsKeywords) -> ItemCategory.ELECTRONICS
            normalizedName.containsAny(clothingKeywords) -> ItemCategory.CLOTHING
            normalizedName.containsAny(foodKeywords) -> ItemCategory.FOOD
            else -> ItemCategory.OTHER
        }
    }

    private fun parseAdd(text: String, today: LocalDate): InventoryAction {
        val expirationDate = parseExpirationDate(text, today)
            ?: return InventoryAction.AskClarification("请补充物品的过期日期")

        val purchaseDate = when {
            text.contains("昨天") -> today.minusDays(1)
            text.contains("明天") -> today.plusDays(1)
            else -> today
        }

        val tail = substringAfterFirstKeyword(text, addKeywords)
            .removeExpirationDateText()
            .removeTotalQuantityText()
            .trimPunctuation()

        val leadingQuantity = findLeadingQuantity(tail)
        val totalQuantity = findTotalQuantity(text)
        val quantity = totalQuantity?.quantity
            ?: leadingQuantity?.quantity
            ?: 1

        val name = tail
            .removePrefixQuantity()
            .removeTemporalWords()
            .trimPunctuation()

        if (name.isBlank()) {
            return InventoryAction.AskClarification("请确认要新增的物品名称")
        }

        return InventoryAction.AddItem(
            ItemDraft(
                name = name,
                category = inferCategory(name),
                quantity = quantity.coerceAtLeast(1),
                purchaseDate = purchaseDate,
                expirationDate = expirationDate,
                reminderDays = Constants.DEFAULT_REMINDER_DAYS,
                note = if (totalQuantity != null && leadingQuantity != null) {
                    "${leadingQuantity.rawNumber}${leadingQuantity.unit}"
                } else {
                    ""
                }
            )
        )
    }

    private fun parseConsume(text: String): InventoryAction {
        val tail = substringAfterFirstKeyword(text, consumeKeywords)
            .trimPunctuation()
        val quantity = findFirstQuantity(tail)?.quantity ?: 1
        val name = tail
            .removePrefixQuantity()
            .removeTemporalWords()
            .trimPunctuation()

        if (name.isBlank()) {
            return InventoryAction.AskClarification("请确认要消耗的物品名称")
        }

        return InventoryAction.ConsumeItem(
            itemName = name,
            quantity = quantity.coerceAtLeast(1)
        )
    }

    private fun parseDiscard(text: String): InventoryAction {
        val beforeVerb = substringBetweenHandleAndKeyword(text, discardKeywords)
        val candidate = if (beforeVerb.isNotBlank()) {
            beforeVerb
        } else {
            substringAfterFirstKeyword(text, discardKeywords)
        }

        val quantity = findFirstQuantity(candidate)?.quantity
            ?: findFirstQuantity(text)?.quantity
            ?: 1
        val name = candidate
            .removePrefixQuantity()
            .removeTemporalWords()
            .removeDiscardDescriptors()
            .trimPunctuation()

        if (name.isBlank()) {
            return InventoryAction.AskClarification("请确认要丢弃的物品名称")
        }

        return InventoryAction.DiscardItem(
            itemName = name,
            quantity = quantity.coerceAtLeast(1)
        )
    }

    private fun substringAfterFirstKeyword(text: String, keywords: List<String>): String {
        val match = keywords
            .mapNotNull { keyword ->
                val index = text.indexOf(keyword)
                if (index >= 0) keyword to index else null
            }
            .minByOrNull { it.second }
        return if (match == null) text else text.substring(match.second + match.first.length)
    }

    private fun substringBetweenHandleAndKeyword(text: String, keywords: List<String>): String {
        val handleIndex = text.indexOf("把")
        if (handleIndex < 0) return ""
        val verbMatch = keywords
            .mapNotNull { keyword ->
                val index = text.indexOf(keyword, startIndex = handleIndex + 1)
                if (index >= 0) keyword to index else null
            }
            .minByOrNull { it.second }
            ?: return ""
        return text.substring(handleIndex + 1, verbMatch.second)
    }

    private fun parseExpirationDate(text: String, today: LocalDate): LocalDate? {
        yearMonthDayRegex.find(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = parseNumber(match.groupValues[2]) ?: return null
            val day = parseNumber(match.groupValues[3]) ?: return null
            return safeDate(year, month, day)
        }

        monthDayRegex.find(text)?.let { match ->
            val month = parseNumber(match.groupValues[1]) ?: return null
            val day = parseNumber(match.groupValues[2]) ?: return null
            val thisYear = safeDate(today.year, month, day) ?: return null
            return if (thisYear.isBefore(today)) thisYear.plusYears(1) else thisYear
        }

        dayOnlyExpirationRegex.find(text)?.let { match ->
            val day = parseNumber(match.groupValues[1]) ?: return null
            var date = safeDate(today.year, today.monthValue, day) ?: return null
            if (date.isBefore(today)) {
                date = date.plusMonths(1)
            }
            return date
        }

        relativeDayExpirationRegex.find(text)?.let { match ->
            val days = parseNumber(match.groupValues[1]) ?: return null
            return today.plusDays(days.toLong())
        }

        return when {
            text.contains("今天过期") -> today
            text.contains("明天过期") -> today.plusDays(1)
            text.contains("后天过期") -> today.plusDays(2)
            else -> null
        }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? {
        return try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private fun findTotalQuantity(text: String): QuantityToken? {
        val match = totalQuantityRegex.find(text) ?: return null
        val rawNumber = match.groupValues[1]
        val unit = match.groupValues[2]
        val quantity = parseNumber(rawNumber) ?: return null
        return QuantityToken(quantity, rawNumber, unit)
    }

    private fun findLeadingQuantity(text: String): QuantityToken? {
        val match = leadingQuantityRegex.find(text) ?: return null
        val rawNumber = match.groupValues[1]
        val unit = match.groupValues[2]
        val quantity = parseNumber(rawNumber) ?: return null
        return QuantityToken(quantity, rawNumber, unit)
    }

    private fun findFirstQuantity(text: String): QuantityToken? {
        val match = quantityRegex.find(text) ?: return null
        val rawNumber = match.groupValues[1]
        val unit = match.groupValues[2]
        val quantity = parseNumber(rawNumber) ?: return null
        return QuantityToken(quantity, rawNumber, unit)
    }

    private fun parseNumber(raw: String): Int? {
        val value = raw.trim()
        value.toIntOrNull()?.let { return it }
        if (value.isBlank()) return null

        val digits = mapOf(
            '零' to 0,
            '〇' to 0,
            '一' to 1,
            '二' to 2,
            '两' to 2,
            '三' to 3,
            '四' to 4,
            '五' to 5,
            '六' to 6,
            '七' to 7,
            '八' to 8,
            '九' to 9
        )
        var result = 0
        var section = 0
        for (char in value) {
            when (char) {
                '百' -> {
                    section = (section.takeIf { it > 0 } ?: 1) * 100
                    result += section
                    section = 0
                }
                '十' -> {
                    section = (section.takeIf { it > 0 } ?: 1) * 10
                    result += section
                    section = 0
                }
                else -> {
                    val digit = digits[char] ?: return null
                    section = digit
                }
            }
        }
        return (result + section).takeIf { it > 0 }
    }

    private fun normalize(text: String): String {
        return text
            .replace(Regex("\\s+"), "")
            .replace('，', ',')
            .replace('。', '.')
            .replace('；', ';')
            .replace('：', ':')
            .replace('！', '!')
            .replace('？', '?')
            .trim()
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { contains(it) }
    }

    private fun String.removeExpirationDateText(): String {
        return replace(yearMonthDayRegex, "")
            .replace(monthDayRegex, "")
            .replace(dayOnlyExpirationRegex, "")
            .replace(relativeDayExpirationRegex, "")
            .replace("今天过期", "")
            .replace("明天过期", "")
            .replace("后天过期", "")
            .replace("过期", "")
    }

    private fun String.removeTotalQuantityText(): String {
        return replace(totalQuantityRegex, "")
    }

    private fun String.removePrefixQuantity(): String {
        return replace(leadingQuantityRegex, "")
    }

    private fun String.removeTemporalWords(): String {
        return replace("今天", "")
            .replace("昨天", "")
            .replace("刚刚", "")
            .replace("刚", "")
            .replace("了", "")
    }

    private fun String.removeDiscardDescriptors(): String {
        return replace("过期的", "")
            .replace("过期", "")
            .replace("那盒", "")
            .replace("那瓶", "")
            .replace("那袋", "")
            .replace("那个", "")
            .replace("这盒", "")
            .replace("这瓶", "")
            .replace("这袋", "")
            .replace("这个", "")
            .replace("的", "")
    }

    private fun String.trimPunctuation(): String {
        return trim { it in " ,.;:!?，。；：！？、" }
    }

    private data class QuantityToken(
        val quantity: Int,
        val rawNumber: String,
        val unit: String
    )

    private companion object {
        private val addKeywords = listOf("买了", "购买了", "采购了", "新增了", "添加了", "入手了", "囤了", "买", "购买", "采购", "新增", "添加", "入手", "囤")
        private val consumeKeywords = listOf("喝了", "吃了", "用了", "用掉了", "消耗了", "喝", "吃", "用掉", "使用了", "使用", "消耗")
        private val discardKeywords = listOf("扔掉了", "丢掉了", "扔了", "丢了", "倒掉了", "处理掉了", "扔掉", "丢掉", "丢弃", "废弃", "扔", "丢", "倒掉", "处理掉")

        private const val numberPattern = "([0-9]+|[零〇一二两三四五六七八九十百]+)"
        private const val unitPattern = "(瓶|盒|箱|袋|包|个|件|支|罐|杯|片|板|条|桶|听)"
        private val quantityRegex = Regex("$numberPattern$unitPattern")
        private val leadingQuantityRegex = Regex("^$numberPattern$unitPattern")
        private val totalQuantityRegex = Regex("(?:有|共|总共|一共|里面有)$numberPattern$unitPattern")

        private val yearMonthDayRegex = Regex("([0-9]{4})年${numberPattern}月${numberPattern}(?:日|号)?")
        private val monthDayRegex = Regex("${numberPattern}月${numberPattern}(?:日|号)?")
        private val dayOnlyExpirationRegex = Regex("$numberPattern(?:日|号)过期")
        private val relativeDayExpirationRegex = Regex("${numberPattern}天后过期")

        private val drinkKeywords = listOf("奶", "牛奶", "酸奶", "饮料", "茶", "水", "咖啡", "果汁", "可乐", "啤酒", "东方树叶")
        private val foodKeywords = listOf("饭", "面", "肉", "菜", "蛋", "水果", "面包", "饼干", "零食", "米", "油", "盐", "糖")
        private val medicineKeywords = listOf("药", "片", "胶囊", "口服液", "感冒", "退烧", "消炎")
        private val cosmeticsKeywords = listOf("面霜", "口红", "粉底", "精华", "乳液", "防晒", "洗面奶", "化妆")
        private val dailyKeywords = listOf("纸", "洗衣", "牙膏", "牙刷", "沐浴", "洗发", "肥皂", "清洁")
        private val electronicsKeywords = listOf("电池", "充电", "耳机", "数据线", "手机")
        private val clothingKeywords = listOf("衣", "裤", "袜", "鞋", "帽")
    }
}
