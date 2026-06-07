package com.jishiyong.agent

import com.jishiyong.data.db.dao.AgentMemoryDao
import com.jishiyong.data.db.entity.AgentMemoryEntity
import com.jishiyong.data.db.entity.Item
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

class RoomAgentMemoryStore(
    private val dao: AgentMemoryDao,
    private val engine: InventoryAliasMemoryEngine = InventoryAliasMemoryEngine(),
    private val logger: AgentLogger = NoOpAgentLogger
) : AgentMemoryStore {

    override suspend fun relevantMemoriesFor(input: String, activeItems: List<Item>): List<AgentMemory> {
        val query = AgentMemorySearchText.ftsQueryFor(input, activeItems)
        if (query.isBlank()) return emptyList()

        val records = try {
            dao.searchMemories(query, SEARCH_CANDIDATE_LIMIT)
                .mapNotNull { it.toAliasMemoryOrNull() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn("Failed to query Room agent memories", exception)
            emptyList()
        }

        return engine.relevantMemoriesFor(records, input, activeItems)
    }

    override suspend fun rememberSuccessfulAction(
        recognizedText: String,
        action: InventoryAction,
        matchedItem: Item?
    ) {
        val records = try {
            dao.getAllMemories().mapNotNull { it.toAliasMemoryOrNull() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn("Failed to load Room agent memories for update", exception)
            emptyList()
        }

        val updated = engine.learnFromAction(records, recognizedText, action, matchedItem)
        try {
            dao.replaceAllMemories(updated.map { it.toEntity() })
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn("Failed to persist Room agent memories", exception)
        }
    }

    private fun InventoryAliasMemory.toEntity(): AgentMemoryEntity {
        val searchableText = AgentMemorySearchText.searchableTextFor(this)
        val valueJson = JSONObject()
            .put("alias", alias)
            .put("canonical_name", canonicalName)
            .put("category_name", categoryName.orEmpty())
            .toString()

        return AgentMemoryEntity(
            type = MEMORY_TYPE_ALIAS,
            key = AgentMemorySearchText.keyFor(this),
            valueJson = valueJson,
            searchableText = searchableText,
            confidence = confidenceForHits(hits),
            hits = hits,
            updatedAt = updatedAt
        )
    }

    private fun AgentMemoryEntity.toAliasMemoryOrNull(): InventoryAliasMemory? {
        if (type != MEMORY_TYPE_ALIAS) return null
        val json = try {
            JSONObject(valueJson)
        } catch (_: Exception) {
            return null
        }

        val alias = json.optString("alias")
        val canonicalName = json.optString("canonical_name")
        if (alias.isBlank() || canonicalName.isBlank()) return null

        return InventoryAliasMemory(
            alias = alias,
            canonicalName = canonicalName,
            categoryName = json.optString("category_name").takeIf { it.isNotBlank() },
            hits = hits.coerceAtLeast(1),
            updatedAt = updatedAt
        )
    }

    private fun confidenceForHits(hits: Int): Float {
        return (0.5f + hits.coerceAtMost(10) * 0.05f).coerceAtMost(1f)
    }

    private companion object {
        private const val MEMORY_TYPE_ALIAS = "ALIAS"
        private const val SEARCH_CANDIDATE_LIMIT = 32
    }
}
