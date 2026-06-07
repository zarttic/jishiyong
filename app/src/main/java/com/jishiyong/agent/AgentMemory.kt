package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

data class AgentMemory(
    val key: String,
    val value: String,
    val weight: Float = 1f
)

interface AgentMemoryStore {
    suspend fun relevantMemoriesFor(input: String, activeItems: List<Item>): List<AgentMemory>
    suspend fun rememberSuccessfulAction(
        recognizedText: String,
        action: InventoryAction,
        matchedItem: Item?
    )
}

object EmptyAgentMemoryStore : AgentMemoryStore {
    override suspend fun relevantMemoriesFor(input: String, activeItems: List<Item>): List<AgentMemory> {
        return emptyList()
    }

    override suspend fun rememberSuccessfulAction(
        recognizedText: String,
        action: InventoryAction,
        matchedItem: Item?
    ) = Unit
}
