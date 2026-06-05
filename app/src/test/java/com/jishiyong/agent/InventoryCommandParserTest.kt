package com.jishiyong.agent

import com.jishiyong.data.db.entity.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryCommandParserTest {

    private val parser = InventoryCommandParser()
    private val today = LocalDate.of(2026, 6, 5)

    @Test
    fun parseAddItemWithChineseQuantityAndDayOnlyExpiration() {
        val action = parser.parse("今天买了两盒牛奶，6月12号过期", today)

        assertTrue(action is InventoryAction.AddItem)
        val draft = (action as InventoryAction.AddItem).draft
        assertEquals("牛奶", draft.name)
        assertEquals(ItemCategory.DRINK, draft.category)
        assertEquals(2, draft.quantity)
        assertEquals(today, draft.purchaseDate)
        assertEquals(LocalDate.of(2026, 6, 12), draft.expirationDate)
    }

    @Test
    fun parseAddItemUsesTotalQuantityAndKeepsPackageNote() {
        val action = parser.parse("今天买了一箱东方树叶，有15瓶，7月20号过期", today)

        assertTrue(action is InventoryAction.AddItem)
        val draft = (action as InventoryAction.AddItem).draft
        assertEquals("东方树叶", draft.name)
        assertEquals(ItemCategory.DRINK, draft.category)
        assertEquals(15, draft.quantity)
        assertEquals(LocalDate.of(2026, 7, 20), draft.expirationDate)
        assertEquals("一箱", draft.note)
    }

    @Test
    fun parseAddItemWithChineseMonthAndDay() {
        val action = parser.parse("今天买了一箱东方树叶，有十五瓶，七月二十号过期", today)

        assertTrue(action is InventoryAction.AddItem)
        val draft = (action as InventoryAction.AddItem).draft
        assertEquals("东方树叶", draft.name)
        assertEquals(15, draft.quantity)
        assertEquals(LocalDate.of(2026, 7, 20), draft.expirationDate)
    }

    @Test
    fun parseConsumeItem() {
        val action = parser.parse("今天喝了一瓶蒙牛牛奶", today)

        assertTrue(action is InventoryAction.ConsumeItem)
        val consume = action as InventoryAction.ConsumeItem
        assertEquals("蒙牛牛奶", consume.itemName)
        assertEquals(1, consume.quantity)
    }

    @Test
    fun parseDiscardItemFromBaSentence() {
        val action = parser.parse("把那盒过期牛奶扔了", today)

        assertTrue(action is InventoryAction.DiscardItem)
        val discard = action as InventoryAction.DiscardItem
        assertEquals("牛奶", discard.itemName)
        assertEquals(1, discard.quantity)
    }

    @Test
    fun parseClarificationWhenExpirationDateMissingForAdd() {
        val action = parser.parse("今天买了两盒牛奶", today)

        assertTrue(action is InventoryAction.AskClarification)
    }
}
