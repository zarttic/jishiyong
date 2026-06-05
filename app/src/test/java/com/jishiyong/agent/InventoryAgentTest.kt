package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryAgentTest {

    private val agent = InventoryAgent()

    @Test
    fun previewConsumeWithExactMatchRequiresConfirmation() {
        val milk = item(id = 1, name = "蒙牛牛奶")

        val state = agent.preview("今天喝了一瓶蒙牛牛奶", listOf(milk))

        assertTrue(state is VoiceInputState.PendingConfirmation)
        val pending = state as VoiceInputState.PendingConfirmation
        assertEquals(milk, pending.matchedItem)
        assertTrue(pending.action is InventoryAction.ConsumeItem)
    }

    @Test
    fun previewConsumeWithMultipleContainsMatchesNeedsSelection() {
        val mengniu = item(id = 1, name = "蒙牛纯牛奶 250ml", expirationDate = LocalDate.of(2026, 6, 10))
        val yili = item(id = 2, name = "伊利纯牛奶", expirationDate = LocalDate.of(2026, 6, 8))

        val state = agent.preview("喝了一瓶牛奶", listOf(mengniu, yili))

        assertTrue(state is VoiceInputState.NeedsSelection)
        val selection = state as VoiceInputState.NeedsSelection
        assertEquals(listOf(yili, mengniu), selection.candidates)
    }

    @Test
    fun previewDiscardWithContainsMatchRequiresConfirmation() {
        val milk = item(id = 1, name = "蒙牛牛奶")

        val state = agent.preview("把那盒过期牛奶扔了", listOf(milk))

        assertTrue(state is VoiceInputState.PendingConfirmation)
        val pending = state as VoiceInputState.PendingConfirmation
        assertEquals(milk, pending.matchedItem)
        assertTrue(pending.action is InventoryAction.DiscardItem)
    }

    @Test
    fun matchIgnoresItemsWithoutRemainingQuantity() {
        val emptyMilk = item(id = 1, name = "蒙牛牛奶", quantity = 2, usedQuantity = 2)

        val result = agent.matchItem("蒙牛牛奶", quantity = 1, activeItems = listOf(emptyMilk))

        assertTrue(result is InventoryMatchResult.NotFound)
    }

    @Test
    fun previewAddRequiresConfirmationWithoutMatchedItem() {
        val state = agent.preview("今天买了两盒牛奶，6月12号过期", emptyList())

        assertTrue(state is VoiceInputState.PendingConfirmation)
        val pending = state as VoiceInputState.PendingConfirmation
        assertEquals(null, pending.matchedItem)
        assertTrue(pending.action is InventoryAction.AddItem)
    }

    private fun item(
        id: Long,
        name: String,
        quantity: Int = 3,
        usedQuantity: Int = 0,
        expirationDate: LocalDate = LocalDate.of(2026, 6, 12)
    ): Item {
        return Item(
            id = id,
            name = name,
            category = ItemCategory.DRINK,
            purchaseDate = LocalDate.of(2026, 6, 1),
            expirationDate = expirationDate,
            quantity = quantity,
            usedQuantity = usedQuantity
        )
    }
}
