package com.jishiyong.agent.llm

import com.jishiyong.agent.InventoryAgentRequest
import com.jishiyong.agent.remainingQuantity
import com.jishiyong.data.db.entity.Item
import org.json.JSONArray
import org.json.JSONObject

class LlmInventoryPromptBuilder {

    fun buildMessages(request: InventoryAgentRequest): List<LlmMessage> {
        return listOf(
            LlmMessage(LlmRole.SYSTEM, systemPrompt),
            LlmMessage(LlmRole.USER, buildUserPrompt(request))
        )
    }

    private fun buildUserPrompt(request: InventoryAgentRequest): String {
        val payload = JSONObject()
            .put("today", request.today.toString())
            .put("recognized_text", request.recognizedText)
            .put("active_inventory", activeInventoryJson(request))
            .put("memories", memoriesJson(request))

        return "请把下面 JSON 中的 recognized_text 转成一个库存操作计划：\n$payload"
    }

    private fun activeInventoryJson(request: InventoryAgentRequest): JSONArray {
        val items = JSONArray()
        request.activeItems
            .sortedWith(compareBy<Item> { it.expirationDate })
            .take(MAX_ACTIVE_ITEMS_IN_PROMPT)
            .forEach { item ->
                items.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("name", item.name)
                        .put("category", item.category.name)
                        .put("category_display_name", item.category.displayName)
                        .put("remaining_quantity", item.remainingQuantity())
                        .put("expiration_date", item.expirationDate.toString())
                )
            }
        return items
    }

    private fun memoriesJson(request: InventoryAgentRequest): JSONArray {
        val memories = JSONArray()
        request.memories
            .take(MAX_MEMORIES_IN_PROMPT)
            .forEach { memory ->
                memories.put(
                    JSONObject()
                        .put("key", memory.key)
                        .put("value", memory.value)
                        .put("weight", memory.weight)
                )
            }
        return memories
    }

    private companion object {
        private const val MAX_ACTIVE_ITEMS_IN_PROMPT = 20
        private const val MAX_MEMORIES_IN_PROMPT = 10

        private val systemPrompt = """
            你是“及时用”的库存 agent。你采用工具规划模式：只负责把用户自然语言转成一个待确认的结构化库存动作，不直接执行数据库写入。
            只能输出一个 JSON 对象，不要输出 Markdown、解释或额外文本。
            支持的 action：
            1. add_item：{"action":"add_item","item":{"name":"牛奶","category":"DRINK","quantity":2,"purchase_date":"2026-06-05","expiration_date":"2026-06-12","reminder_days":[7,3,1],"note":""}}
            2. consume_item：{"action":"consume_item","item_id":12,"item_name":"蒙牛牛奶","quantity":1}
            3. discard_item：{"action":"discard_item","item_id":12,"item_name":"蒙牛牛奶","quantity":1}
            4. ask_clarification：{"action":"ask_clarification","message":"请补充物品的过期日期"}
            category 必须是 FOOD、DRINK、DAILY、MEDICINE、COSMETICS、ELECTRONICS、CLOTHING、OTHER 之一。
            日期必须是 YYYY-MM-DD。相对日期必须基于 today 计算。新增物品缺少过期日期时必须 ask_clarification。
            消耗或丢弃时优先使用 active_inventory 中最匹配库存的 id 和 name，但不要编造库存。
            如果用户是在否定、取消、阻止或提问，例如“不要喝牛奶”“别把牛奶扔了”“要不要丢掉牛奶”，必须输出 ask_clarification，不要转成执行动作。
        """.trimIndent()
    }
}
