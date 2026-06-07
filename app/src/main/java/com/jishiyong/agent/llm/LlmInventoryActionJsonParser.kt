package com.jishiyong.agent.llm

import com.jishiyong.agent.CategoryInferencer
import com.jishiyong.agent.InventoryAction
import com.jishiyong.agent.ItemDraft
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.util.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class LlmInventoryActionJsonParser(
    private val categoryInferencer: CategoryInferencer = CategoryInferencer()
) {

    fun parse(content: String, today: LocalDate): InventoryAction {
        val jsonText = extractFirstJsonObject(content)
            ?: return InventoryAction.AskClarification("AI 未返回可执行的库存操作")
        val root = try {
            JSONObject(jsonText)
        } catch (_: Exception) {
            return InventoryAction.AskClarification("AI 未返回可执行的库存操作")
        }
        return when (root.optString("action").trim().lowercase()) {
            "add_item" -> parseAdd(root, today)
            "consume_item" -> parseInventoryChange(root, isDiscard = false)
            "discard_item" -> parseInventoryChange(root, isDiscard = true)
            "ask_clarification" -> {
                InventoryAction.AskClarification(
                    root.optString("message").ifBlank { "请补充库存操作信息" }
                )
            }
            else -> InventoryAction.AskClarification("暂时只能处理新增、消耗或丢弃库存")
        }
    }

    private fun parseAdd(root: JSONObject, today: LocalDate): InventoryAction {
        val item = root.optJSONObject("item") ?: root
        val name = item.optString("name").trim()
        if (name.isBlank()) {
            return InventoryAction.AskClarification("请确认要新增的物品名称")
        }

        val expirationDate = parseDate(item.optString("expiration_date"))
            ?: return InventoryAction.AskClarification("请补充物品的过期日期")
        val purchaseDate = parseDate(item.optString("purchase_date")) ?: today
        val quantity = item.optPositiveInt("quantity") ?: 1
        val reminderDays = item.optJSONArray("reminder_days").toReminderDays()

        return InventoryAction.AddItem(
            ItemDraft(
                name = name,
                category = parseCategory(item.optString("category"), name),
                quantity = quantity.coerceAtLeast(1),
                purchaseDate = purchaseDate,
                expirationDate = expirationDate,
                reminderDays = reminderDays,
                note = item.optString("note")
            )
        )
    }

    private fun parseInventoryChange(root: JSONObject, isDiscard: Boolean): InventoryAction {
        val item = root.optJSONObject("item")
        val itemId = root.optPositiveLong("item_id")
            ?: root.optPositiveLong("id")
            ?: item?.optPositiveLong("id")
        val itemName = listOf(
            root.optString("item_name"),
            root.optString("name"),
            item?.optString("name").orEmpty()
        ).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (itemName.isBlank() && itemId == null) {
            val message = if (isDiscard) "请确认要丢弃的物品名称" else "请确认要消耗的物品名称"
            return InventoryAction.AskClarification(message)
        }

        val quantity = (root.optPositiveInt("quantity") ?: item?.optPositiveInt("quantity") ?: 1)
            .coerceAtLeast(1)
        return if (isDiscard) {
            InventoryAction.DiscardItem(itemName = itemName, quantity = quantity, itemId = itemId)
        } else {
            InventoryAction.ConsumeItem(itemName = itemName, quantity = quantity, itemId = itemId)
        }
    }

    private fun parseCategory(rawCategory: String, name: String): ItemCategory {
        val normalized = rawCategory.trim()
        return ItemCategory.entries.firstOrNull {
            it.name.equals(normalized, ignoreCase = true) || it.displayName == normalized
        } ?: categoryInferencer.infer(name)
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            value.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.optPositiveInt(name: String): Int? {
        return when (val value = opt(name)) {
            is Number -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }?.takeIf { it > 0 }
    }

    private fun JSONObject.optPositiveLong(name: String): Long? {
        return when (val value = opt(name)) {
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        }?.takeIf { it > 0 }
    }

    private fun JSONArray?.toReminderDays(): List<Int> {
        if (this == null) return Constants.DEFAULT_REMINDER_DAYS
        val days = mutableListOf<Int>()
        for (index in 0 until length()) {
            when (val value = opt(index)) {
                is Number -> days += value.toInt()
                is String -> value.trim().toIntOrNull()?.let(days::add)
            }
        }
        return days.filter { it > 0 }.distinct().sorted().ifEmpty { Constants.DEFAULT_REMINDER_DAYS }
    }

    private fun extractFirstJsonObject(content: String): String? {
        val fenced = Regex("""```(?:json)?\s*([\s\S]*?)```""")
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        val candidate = fenced ?: content.trim()
        if (candidate.startsWith("{") && candidate.endsWith("}")) return candidate

        var start = -1
        var depth = 0
        var inString = false
        var escaped = false
        candidate.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }
            when {
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> {
                    if (depth == 0) start = index
                    depth += 1
                }
                !inString && char == '}' && depth > 0 -> {
                    depth -= 1
                    if (depth == 0 && start >= 0) {
                        return candidate.substring(start, index + 1)
                    }
                }
            }
        }
        return null
    }
}
