package com.jishiyong.agent

import com.jishiyong.data.db.InventoryChangeResult
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item

class InventoryActionExecutor(
    private val validator: InventoryActionValidator = InventoryActionValidator()
) {

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
        val validationError = validator.validate(draft)
        return if (validationError == null) {
            store.insert(draft.toItem())
            VoiceInputState.Success("已新增 ${draft.name} x${draft.quantity}")
        } else {
            VoiceInputState.Error(validationError, recognizedText)
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

        return when (val result = store.applyInventoryChange(item.id, quantity, consumeType)) {
            is InventoryChangeResult.Applied -> {
                if (result.item.isConsumed) {
                    VoiceInputState.Success(fullSuccessMessage)
                } else {
                    VoiceInputState.Success(partialSuccessMessage)
                }
            }
            InventoryChangeResult.Missing -> {
                VoiceInputState.Error("库存不存在或已被删除", recognizedText)
            }
            InventoryChangeResult.AlreadyConsumed -> {
                VoiceInputState.Error("该库存已经处理完成", recognizedText)
            }
            is InventoryChangeResult.InsufficientQuantity -> {
                VoiceInputState.Error(
                    "“${result.item.name}”剩余 ${result.remainingQuantity}，不能操作 $quantity",
                    recognizedText
                )
            }
            InventoryChangeResult.InvalidQuantity -> {
                VoiceInputState.Error("数量必须大于 0", recognizedText)
            }
            InventoryChangeResult.Conflict -> {
                VoiceInputState.Error("库存刚刚发生变化，请重新确认", recognizedText)
            }
        }
    }
}

interface InventoryActionStore {
    suspend fun insert(item: Item): Long
    suspend fun applyInventoryChange(id: Long, quantity: Int, consumeType: ConsumeType): InventoryChangeResult
}
