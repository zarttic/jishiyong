package com.jishiyong.agent

import com.jishiyong.data.db.InventoryChangeResult
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryActionExecutorTest {

    private val executor = InventoryActionExecutor()

    @Test
    fun executeAddInsertsValidatedItem() = runTest {
        val store = FakeActionStore()
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "今天买了两盒牛奶，6月12号过期",
            action = InventoryAction.AddItem(
                ItemDraft(
                    name = "牛奶",
                    category = ItemCategory.DRINK,
                    quantity = 2,
                    purchaseDate = LocalDate.of(2026, 6, 5),
                    expirationDate = LocalDate.of(2026, 6, 12),
                    reminderDays = listOf(7, 3, 1)
                )
            )
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Success)
        assertEquals("牛奶", store.inserted.single().name)
        assertEquals(2, store.inserted.single().quantity)
    }

    @Test
    fun executePartialConsumeUpdatesUsedQuantity() = runTest {
        val milk = item(id = 1, name = "蒙牛牛奶", quantity = 5, usedQuantity = 1)
        val store = FakeActionStore(items = mutableMapOf(milk.id to milk))
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "喝了两瓶蒙牛牛奶",
            action = InventoryAction.ConsumeItem(itemName = "蒙牛牛奶", quantity = 2),
            matchedItem = milk
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Success)
        assertEquals(3, store.updatedUsedQuantity[milk.id])
        assertNull(store.consumed[milk.id])
    }

    @Test
    fun executeFullConsumeMarksUsedUp() = runTest {
        val milk = item(id = 1, name = "蒙牛牛奶", quantity = 2, usedQuantity = 1)
        val store = FakeActionStore(items = mutableMapOf(milk.id to milk))
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "喝了一瓶蒙牛牛奶",
            action = InventoryAction.ConsumeItem(itemName = "蒙牛牛奶", quantity = 1),
            matchedItem = milk
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Success)
        assertEquals(ConsumeType.USED_UP, store.consumed[milk.id])
        assertEquals(2, store.updatedUsedQuantity[milk.id])
    }

    @Test
    fun executeDiscardMarksDiscardedWhenDiscardingRemainingQuantity() = runTest {
        val milk = item(id = 1, name = "蒙牛牛奶", quantity = 2, usedQuantity = 0)
        val store = FakeActionStore(items = mutableMapOf(milk.id to milk))
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "把过期牛奶扔了",
            action = InventoryAction.DiscardItem(itemName = "牛奶", quantity = 2),
            matchedItem = milk
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Success)
        assertEquals(ConsumeType.DISCARDED, store.consumed[milk.id])
        assertEquals(2, store.updatedUsedQuantity[milk.id])
    }

    @Test
    fun executeConsumeRefusesConsumedItem() = runTest {
        val milk = item(id = 1, name = "蒙牛牛奶", isConsumed = true)
        val store = FakeActionStore(items = mutableMapOf(milk.id to milk))
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "喝了一瓶蒙牛牛奶",
            action = InventoryAction.ConsumeItem(itemName = "蒙牛牛奶", quantity = 1),
            matchedItem = milk
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Error)
        assertTrue(store.consumed.isEmpty())
        assertTrue(store.updatedUsedQuantity.isEmpty())
    }

    @Test
    fun executeConsumeRefusesQuantityGreaterThanRemainingQuantity() = runTest {
        val milk = item(id = 1, name = "蒙牛牛奶", quantity = 3, usedQuantity = 1)
        val store = FakeActionStore(items = mutableMapOf(milk.id to milk))
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "喝了三瓶蒙牛牛奶",
            action = InventoryAction.ConsumeItem(itemName = "蒙牛牛奶", quantity = 3),
            matchedItem = milk
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Error)
        assertTrue(store.consumed.isEmpty())
        assertTrue(store.updatedUsedQuantity.isEmpty())
    }

    @Test
    fun executeConsumeReportsConcurrentConflict() = runTest {
        val milk = item(id = 1, name = "蒙牛牛奶", quantity = 3, usedQuantity = 1)
        val store = FakeActionStore(
            items = mutableMapOf(milk.id to milk),
            forcedChangeResult = InventoryChangeResult.Conflict
        )
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "喝了一瓶蒙牛牛奶",
            action = InventoryAction.ConsumeItem(itemName = "蒙牛牛奶", quantity = 1),
            matchedItem = milk
        )

        val state = executor.execute(pending, store)

        assertTrue(state is VoiceInputState.Error)
        assertTrue((state as VoiceInputState.Error).message.contains("重新确认"))
    }

    private fun item(
        id: Long,
        name: String,
        quantity: Int = 2,
        usedQuantity: Int = 0,
        isConsumed: Boolean = false
    ): Item {
        return Item(
            id = id,
            name = name,
            category = ItemCategory.DRINK,
            purchaseDate = LocalDate.of(2026, 6, 1),
            expirationDate = LocalDate.of(2026, 6, 12),
            quantity = quantity,
            usedQuantity = usedQuantity,
            isConsumed = isConsumed
        )
    }

    private class FakeActionStore(
        private val items: MutableMap<Long, Item> = mutableMapOf(),
        private val forcedChangeResult: InventoryChangeResult? = null
    ) : InventoryActionStore {
        val inserted = mutableListOf<Item>()
        val consumed = mutableMapOf<Long, ConsumeType>()
        val updatedUsedQuantity = mutableMapOf<Long, Int>()

        override suspend fun insert(item: Item): Long {
            val id = (items.keys.maxOrNull() ?: 0L) + 1L
            inserted += item.copy(id = id)
            items[id] = item.copy(id = id)
            return id
        }

        override suspend fun applyInventoryChange(
            id: Long,
            quantity: Int,
            consumeType: ConsumeType
        ): InventoryChangeResult {
            forcedChangeResult?.let { return it }
            if (quantity <= 0) return InventoryChangeResult.InvalidQuantity
            val item = items[id] ?: return InventoryChangeResult.Missing
            if (item.isConsumed) return InventoryChangeResult.AlreadyConsumed

            val remainingQuantity = item.remainingQuantity()
            if (remainingQuantity <= 0) {
                val updated = item.copy(
                    usedQuantity = item.quantity,
                    isConsumed = true,
                    consumeType = consumeType
                )
                items[id] = updated
                updatedUsedQuantity[id] = updated.usedQuantity
                consumed[id] = consumeType
                return InventoryChangeResult.Applied(updated)
            }
            if (quantity > remainingQuantity) {
                return InventoryChangeResult.InsufficientQuantity(item, remainingQuantity)
            }

            val updated = if (quantity >= remainingQuantity) {
                item.copy(
                    usedQuantity = item.quantity,
                    isConsumed = true,
                    consumeType = consumeType
                )
            } else {
                item.copy(usedQuantity = item.usedQuantity + quantity)
            }
            items[id] = updated
            updatedUsedQuantity[id] = updated.usedQuantity
            if (updated.isConsumed) {
                consumed[id] = consumeType
            }
            return InventoryChangeResult.Applied(updated)
        }
    }
}
