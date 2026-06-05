package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

sealed class InventoryMatchResult {
    data class Matched(val item: Item) : InventoryMatchResult()

    data class NeedsSelection(
        val candidates: List<Item>,
        val message: String
    ) : InventoryMatchResult()

    data class NotFound(val message: String) : InventoryMatchResult()
}
