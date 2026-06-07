package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryAliasMemoryEngineTest {

    private val engine = InventoryAliasMemoryEngine()

    @Test
    fun learnConsumeActionStoresAliasForMatchedItem() {
        val item = item(name = "蒙牛纯牛奶")

        val records = engine.learnFromAction(
            records = emptyList(),
            recognizedText = "我喝了一瓶常买的奶",
            action = InventoryAction.ConsumeItem(itemName = "常买的奶", quantity = 1),
            matchedItem = item,
            now = 100L
        )

        assertTrue(records.any { it.alias == "常买的奶" && it.canonicalName == "蒙牛纯牛奶" })
        assertTrue(records.all { it.categoryName == ItemCategory.DRINK.name })
    }

    @Test
    fun relevantMemoriesReturnsAliasForMatchingInput() {
        val records = listOf(
            InventoryAliasMemory(
                alias = "常买的奶",
                canonicalName = "蒙牛纯牛奶",
                categoryName = ItemCategory.DRINK.name,
                hits = 2,
                updatedAt = 100L
            )
        )

        val memories = engine.relevantMemoriesFor(
            records = records,
            input = "喝一瓶常买的奶",
            activeItems = listOf(item(name = "蒙牛纯牛奶"))
        )

        assertEquals(1, memories.size)
        assertTrue(memories.single().value.contains("蒙牛纯牛奶"))
        assertTrue(memories.single().weight > 1f)
    }

    @Test
    fun unrelatedInputDoesNotReturnMemoryWithoutActiveItemSupport() {
        val records = listOf(
            InventoryAliasMemory(alias = "常买的奶", canonicalName = "蒙牛纯牛奶")
        )

        val memories = engine.relevantMemoriesFor(
            records = records,
            input = "丢掉一袋面包",
            activeItems = emptyList()
        )

        assertTrue(memories.isEmpty())
    }

    @Test
    fun repeatedLearningUpdatesExistingRecordHits() {
        val item = item(name = "蒙牛纯牛奶")
        val records = listOf(
            InventoryAliasMemory(alias = "常买的奶", canonicalName = "蒙牛纯牛奶", hits = 1, updatedAt = 100L)
        )

        val updated = engine.learnFromAction(
            records = records,
            recognizedText = "喝了常买的奶",
            action = InventoryAction.ConsumeItem(itemName = "常买的奶", quantity = 1),
            matchedItem = item,
            now = 200L
        )

        val aliasRecord = updated.single { it.alias == "常买的奶" }
        assertEquals(2, aliasRecord.hits)
        assertEquals(200L, aliasRecord.updatedAt)
    }

    private fun item(name: String): Item {
        return Item(
            id = 1,
            name = name,
            category = ItemCategory.DRINK,
            purchaseDate = LocalDate.of(2026, 6, 1),
            expirationDate = LocalDate.of(2026, 6, 12),
            quantity = 3
        )
    }
}
