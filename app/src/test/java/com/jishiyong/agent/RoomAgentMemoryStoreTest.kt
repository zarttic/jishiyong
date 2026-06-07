package com.jishiyong.agent

import com.jishiyong.data.db.dao.AgentMemoryDao
import com.jishiyong.data.db.entity.AgentMemoryEntity
import com.jishiyong.data.db.entity.AgentMemoryFtsEntity
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoomAgentMemoryStoreTest {

    @Test
    fun remembersSuccessfulAliasAndRecallsThroughFtsCandidates() = runTest {
        val dao = FakeAgentMemoryDao()
        val store = RoomAgentMemoryStore(dao)
        val milk = item(id = 1, name = "蒙牛纯牛奶")

        store.rememberSuccessfulAction(
            recognizedText = "喝了常买的奶",
            action = InventoryAction.ConsumeItem(itemName = "常买的奶", quantity = 1),
            matchedItem = milk
        )

        assertEquals(1, dao.memories.size)
        assertTrue(dao.memories.single().searchableText.contains("常买"))
        assertTrue(dao.memories.single().searchableText.contains("蒙牛"))

        val memories = store.relevantMemoriesFor("今天喝一瓶常买的奶", listOf(milk))

        assertEquals(1, memories.size)
        assertTrue(memories.single().value.contains("蒙牛纯牛奶"))
        assertTrue(dao.lastSearchQuery.orEmpty().contains("\"常买\""))
    }

    private fun item(id: Long, name: String): Item {
        return Item(
            id = id,
            name = name,
            category = ItemCategory.DRINK,
            purchaseDate = LocalDate.of(2026, 6, 1),
            expirationDate = LocalDate.of(2026, 6, 12),
            quantity = 3
        )
    }

    private class FakeAgentMemoryDao : AgentMemoryDao {
        val memories = mutableListOf<AgentMemoryEntity>()
        val ftsEntries = mutableListOf<AgentMemoryFtsEntity>()
        var lastSearchQuery: String? = null
        private var nextId = 1L

        override suspend fun getAllMemories(): List<AgentMemoryEntity> {
            return memories.toList()
        }

        override suspend fun searchMemories(query: String, limit: Int): List<AgentMemoryEntity> {
            lastSearchQuery = query
            return memories.take(limit)
        }

        override suspend fun insertMemories(memories: List<AgentMemoryEntity>): List<Long> {
            return memories.map { memory ->
                val id = nextId++
                this.memories += memory.copy(id = id)
                id
            }
        }

        override suspend fun insertFtsEntries(entries: List<AgentMemoryFtsEntity>) {
            ftsEntries += entries
        }

        override suspend fun clearFtsEntries() {
            ftsEntries.clear()
        }

        override suspend fun clearMemories() {
            memories.clear()
        }
    }
}
