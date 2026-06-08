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
    fun remembersSuccessfulAlias() = runTest {
        val dao = FakeAgentMemoryDao()
        val store = RoomAgentMemoryStore(dao)
        val milk = item(id = 1, name = "蒙牛纯牛奶")

        store.rememberSuccessfulAction(
            recognizedText = "喝了常买的奶",
            action = InventoryAction.ConsumeItem(itemName = "常买的奶", quantity = 1),
            matchedItem = milk
        )

        assertTrue(dao.memories.any { it.searchableText.contains("常买") })
        assertTrue(dao.memories.all { it.searchableText.contains("蒙牛") })
        assertEquals(dao.memories.size, dao.ftsEntries.size)
        assertEquals(1, dao.rewriteCalls)
    }

    @Test
    fun recallsThroughFtsCandidates() = runTest {
        val dao = FakeAgentMemoryDao()
        val store = RoomAgentMemoryStore(dao)
        val milk = item(id = 1, name = "蒙牛纯牛奶")

        dao.replaceAllMemories(
            listOf(
                AgentMemoryEntity(
                    type = "ALIAS",
                    key = "常买奶",
                    valueJson = """{"alias":"常买的奶","canonical_name":"蒙牛纯牛奶","category_name":"DRINK"}""",
                    searchableText = "常买的奶 常买奶 常买 买奶 蒙牛纯牛奶 蒙牛 牛纯 纯牛 牛奶 DRINK",
                    confidence = 0.55f,
                    hits = 1,
                    updatedAt = 100L
                )
            )
        )
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
        var rewriteCalls = 0
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

        override suspend fun replaceAllMemories(memories: List<AgentMemoryEntity>) {
            clearFtsEntries()
            clearMemories()
            if (memories.isEmpty()) return

            val ids = insertMemories(memories.map { it.copy(id = 0) })
            insertFtsEntries(
                ids.zip(memories).map { (id, memory) ->
                    AgentMemoryFtsEntity(
                        rowId = id,
                        searchableText = memory.searchableText
                    )
                }
            )
        }

        override suspend fun rewriteAllMemories(
            transform: (List<AgentMemoryEntity>) -> List<AgentMemoryEntity>
        ) {
            rewriteCalls += 1
            replaceAllMemories(transform(getAllMemories()))
        }
    }
}
