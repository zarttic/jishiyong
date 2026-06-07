package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

sealed class InventoryActionPreview {
    data class Ready(val confirmation: InventoryActionConfirmation) : InventoryActionPreview()
    data class RequiresSelection(val selection: InventoryCandidateSelection) : InventoryActionPreview()
    data class Failed(val message: String) : InventoryActionPreview()
}

data class InventoryActionConfirmation(
    val action: InventoryAction,
    val matchedItem: Item? = null
)

data class InventoryCandidateSelection(
    val action: InventoryAction,
    val candidates: List<Item>,
    val message: String
)

class InventoryActionPreviewer(
    private val validator: InventoryActionValidator = InventoryActionValidator(),
    private val itemMatcher: InventoryItemMatcher = InventoryItemMatcher()
) {

    fun preview(action: InventoryAction, activeItems: List<Item>): InventoryActionPreview {
        return when (action) {
            is InventoryAction.AddItem -> {
                val validationError = validator.validate(action.draft)
                if (validationError == null) {
                    InventoryActionPreview.Ready(InventoryActionConfirmation(action))
                } else {
                    InventoryActionPreview.Failed(validationError)
                }
            }
            is InventoryAction.ConsumeItem -> previewInventoryChange(
                action = action,
                activeItems = activeItems,
                itemName = action.itemName,
                quantity = action.quantity,
                itemId = action.itemId
            )
            is InventoryAction.DiscardItem -> previewInventoryChange(
                action = action,
                activeItems = activeItems,
                itemName = action.itemName,
                quantity = action.quantity,
                itemId = action.itemId
            )
            is InventoryAction.AskClarification -> InventoryActionPreview.Failed(action.message)
        }
    }

    private fun previewInventoryChange(
        action: InventoryAction,
        activeItems: List<Item>,
        itemName: String,
        quantity: Int,
        itemId: Long? = null
    ): InventoryActionPreview {
        return when (val match = itemMatcher.matchItem(itemName, quantity, activeItems, itemId)) {
            is InventoryMatchResult.Matched -> {
                val remainingQuantity = match.item.remainingQuantity()
                if (quantity > remainingQuantity) {
                    InventoryActionPreview.Failed("“${match.item.name}”剩余 $remainingQuantity，不能操作 $quantity")
                } else {
                    InventoryActionPreview.Ready(
                        InventoryActionConfirmation(
                            action = action,
                            matchedItem = match.item
                        )
                    )
                }
            }
            is InventoryMatchResult.NeedsSelection -> {
                InventoryActionPreview.RequiresSelection(
                    InventoryCandidateSelection(
                        action = action,
                        candidates = match.candidates,
                        message = match.message
                    )
                )
            }
            is InventoryMatchResult.NotFound -> InventoryActionPreview.Failed(match.message)
        }
    }
}
