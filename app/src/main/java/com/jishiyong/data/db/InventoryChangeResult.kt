package com.jishiyong.data.db

import com.jishiyong.data.db.entity.Item

sealed class InventoryChangeResult {
    data class Applied(val item: Item) : InventoryChangeResult()
    data object Missing : InventoryChangeResult()
    data object AlreadyConsumed : InventoryChangeResult()
    data class InsufficientQuantity(val item: Item, val remainingQuantity: Int) : InventoryChangeResult()
    data object InvalidQuantity : InventoryChangeResult()
    data object Conflict : InventoryChangeResult()
}
