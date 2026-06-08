package com.jishiyong.agent.llm

import com.jishiyong.agent.InventoryAction
import com.jishiyong.data.db.entity.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LlmInventoryActionJsonParserTest {

    private val parser = LlmInventoryActionJsonParser()
    private val today = LocalDate.of(2026, 6, 5)

    @Test
    fun parseAddItemFromFencedJson() {
        val action = parser.parse(
            """
                ```json
                {
                  "action": "add_item",
                  "item": {
                    "name": "低温酸奶",
                    "category": "DRINK",
                    "quantity": 4,
                    "purchase_date": "2026-06-05",
                    "expiration_date": "2026-06-12",
                    "reminder_days": [7, 3, 1],
                    "note": "一组"
                  }
                }
                ```
            """.trimIndent(),
            today
        )

        assertTrue(action is InventoryAction.AddItem)
        val draft = (action as InventoryAction.AddItem).draft
        assertEquals("低温酸奶", draft.name)
        assertEquals(ItemCategory.DRINK, draft.category)
        assertEquals(4, draft.quantity)
        assertEquals(today, draft.purchaseDate)
        assertEquals(LocalDate.of(2026, 6, 12), draft.expirationDate)
        assertEquals(listOf(1, 3, 7), draft.reminderDays)
        assertEquals("一组", draft.note)
    }

    @Test
    fun parseAddItemMissingExpirationAsksClarification() {
        val action = parser.parse(
            """{"action":"add_item","item":{"name":"牛奶","quantity":2}}""",
            today
        )

        assertTrue(action is InventoryAction.AskClarification)
    }

    @Test
    fun parseAddItemInvalidQuantityAsksClarification() {
        val action = parser.parse(
            """{"action":"add_item","item":{"name":"牛奶","quantity":0,"expiration_date":"2026-06-12"}}""",
            today
        )

        assertTrue(action is InventoryAction.AskClarification)
    }

    @Test
    fun parseMalformedJsonAsksClarification() {
        val action = parser.parse("""{"action":"consume_item","item_name":""", today)

        assertTrue(action is InventoryAction.AskClarification)
    }

    @Test
    fun parseConsumeItem() {
        val action = parser.parse(
            """{"action":"consume_item","item_id":9,"item_name":"蒙牛牛奶","quantity":2}""",
            today
        )

        assertTrue(action is InventoryAction.ConsumeItem)
        val consume = action as InventoryAction.ConsumeItem
        assertEquals("蒙牛牛奶", consume.itemName)
        assertEquals(2, consume.quantity)
        assertEquals(9L, consume.itemId)
    }

    @Test
    fun parseConsumeItemWithOnlyItemId() {
        val action = parser.parse(
            """{"action":"consume_item","item_id":9,"quantity":2}""",
            today
        )

        assertTrue(action is InventoryAction.ConsumeItem)
        val consume = action as InventoryAction.ConsumeItem
        assertEquals("", consume.itemName)
        assertEquals(2, consume.quantity)
        assertEquals(9L, consume.itemId)
    }

    @Test
    fun parseConsumeItemInvalidQuantityAsksClarification() {
        val action = parser.parse(
            """{"action":"consume_item","item_id":9,"quantity":"two"}""",
            today
        )

        assertTrue(action is InventoryAction.AskClarification)
    }

    @Test
    fun parseConsumeItemNegativeQuantityAsksClarification() {
        val action = parser.parse(
            """{"action":"consume_item","item_id":9,"quantity":-2}""",
            today
        )

        assertTrue(action is InventoryAction.AskClarification)
    }

    @Test
    fun parseConsumeItemMissingQuantityDefaultsToOne() {
        val action = parser.parse(
            """{"action":"consume_item","item_id":9,"item_name":"蒙牛牛奶"}""",
            today
        )

        assertTrue(action is InventoryAction.ConsumeItem)
        assertEquals(1, (action as InventoryAction.ConsumeItem).quantity)
    }
}
