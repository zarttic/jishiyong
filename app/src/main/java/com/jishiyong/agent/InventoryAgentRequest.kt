package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item
import java.time.LocalDate

data class InventoryAgentRequest(
    val recognizedText: String,
    val activeItems: List<Item>,
    val today: LocalDate = LocalDate.now(),
    val memories: List<AgentMemory> = emptyList()
)
