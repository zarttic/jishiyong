package com.jishiyong.agent

import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item

class InventoryActionExecutor {

    suspend fun execute(
        pending: VoiceInputState.PendingConfirmation,
        store: InventoryActionStore
    ): VoiceInputState {
        return when (val action = pending.action) {
            is InventoryAction.AddItem -> executeAdd(action, pending.recognizedText, store)
            is InventoryAction.ConsumeItem -> {
                val item = pending.matchedItem
                    ?: return VoiceInputState.Error("请先选择要消耗的库存", pending.recognizedText)
                consumeOrDiscardItem(
                    item = item,
                    quantity = action.quantity,
                    consumeType = ConsumeType.USED_UP,
                    partialSuccessMessage = "已消耗 ${item.name} x${action.quantity}",
                    fullSuccessMessage = "已用完 ${item.name}",
                    recognizedText = pending.recognizedText,
                    store = store
                )
            }
            is InventoryAction.DiscardItem -> {
                val item = pending.matchedItem
                    ?: return VoiceInputState.Error("请先选择要丢弃的库存", pending.recognizedText)
                consumeOrDiscardItem(
                    item = item,
                    quantity = action.quantity,
                    consumeType = ConsumeType.DISCARDED,
                    partialSuccessMessage = "已丢弃 ${item.name} x${action.quantity}",
                    fullSuccessMessage = "已丢弃 ${item.name}",
                    recognizedText = pending.recognizedText,
                    store = store
                )
            }
            is InventoryAction.AskClarification -> VoiceInputState.Error(action.message, pending.recognizedText)
        }
    }

    private suspend fun executeAdd(
        action: InventoryAction.AddItem,
        recognizedText: String,
        store: InventoryActionStore
    ): VoiceInputState {
        val draft = action.draft
        return when {
            draft.name.isBlank() -> VoiceInputState.Error("请确认要新增的物品名称", recognizedText)
            draft.quantity <= 0 -> VoiceInputState.Error("数量必须大于 0", recognizedText)
            draft.expirationDate.isBefore(draft.purchaseDate) -> {
                VoiceInputState.Error("过期日期不能早于购买日期", recognizedText)
            }
            else -> {
                store.insert(draft.toItem())
                VoiceInputState.Success("已新增 ${draft.name} x${draft.quantity}")
            }
        }
    }

    private suspend fun consumeOrDiscardItem(
        item: Item,
        quantity: Int,
        consumeType: ConsumeType,
        partialSuccessMessage: String,
        fullSuccessMessage: String,
        recognizedText: String,
        store: InventoryActionStore
    ): VoiceInputState {
        if (quantity <= 0) {
            return VoiceInputState.Error("数量必须大于 0", recognizedText)
        }

        val currentItem = store.getItemById(item.id)
            ?: return VoiceInputState.Error("库存不存在或已被删除", recognizedText)
        if (currentItem.isConsumed) {
            return VoiceInputState.Error("该库存已经处理完成", recognizedText)
        }

        val remainingQuantity = currentItem.remainingQuantity()
        if (remainingQuantity <= 0) {
            store.updateUsedQuantity(currentItem.id, currentItem.quantity)
            store.markAsConsumed(currentItem.id, consumeType)
            return VoiceInputState.Success(fullSuccessMessage)
        }

        if (quantity > remainingQuantity) {
            return VoiceInputState.Error(
                "“${currentItem.name}”剩余 $remainingQuantity，不能操作 $quantity",
                recognizedText
            )
        }

        return if (quantity >= remainingQuantity) {
            store.updateUsedQuantity(currentItem.id, currentItem.quantity)
            store.markAsConsumed(currentItem.id, consumeType)
            VoiceInputState.Success(fullSuccessMessage)
        } else {
            store.updateUsedQuantity(currentItem.id, currentItem.usedQuantity + quantity)
            VoiceInputState.Success(partialSuccessMessage)
        }
    }
}

interface InventoryActionStore {
    suspend fun insert(item: Item): Long
    suspend fun getItemById(id: Long): Item?
    suspend fun markAsConsumed(id: Long, type: ConsumeType)
    suspend fun updateUsedQuantity(id: Long, quantity: Int)
}
