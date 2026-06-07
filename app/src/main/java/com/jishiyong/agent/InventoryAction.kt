package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import java.time.LocalDate

sealed class InventoryAction {
    data class AddItem(val draft: ItemDraft) : InventoryAction()

    data class ConsumeItem(
        val itemName: String,
        val quantity: Int
    ) : InventoryAction()

    data class DiscardItem(
        val itemName: String,
        val quantity: Int
    ) : InventoryAction()

    data class AskClarification(
        val message: String
    ) : InventoryAction()
}

data class ItemDraft(
    val name: String,
    val category: ItemCategory,
    val quantity: Int,
    val purchaseDate: LocalDate,
    val expirationDate: LocalDate,
    val reminderDays: List<Int>,
    val note: String = ""
)

fun ItemDraft.toItem(): Item = Item(
    name = name.trim(),
    category = category,
    purchaseDate = purchaseDate,
    expirationDate = expirationDate,
    note = note.trim(),
    quantity = quantity.coerceAtLeast(1),
    reminderDays = reminderDays.filter { it > 0 }.distinct().sorted()
)
