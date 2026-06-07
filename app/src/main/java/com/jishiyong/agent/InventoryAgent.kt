package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item
import kotlinx.coroutines.CancellationException

class InventoryAgent(
    private val planner: InventoryActionPlanner = RuleBasedInventoryActionPlanner(),
    private val memoryStore: AgentMemoryStore = EmptyAgentMemoryStore,
    private val parser: InventoryCommandParser = InventoryCommandParser(),
    val mode: InventoryAgentMode = InventoryAgentMode.LOCAL_RULES
) {

    fun preview(text: String, activeItems: List<Item>): VoiceInputState {
        val action = parser.parse(text)
        return preview(text, action, activeItems)
    }

    suspend fun previewWithPlanning(
        text: String,
        activeItems: List<Item>
    ): VoiceInputState {
        val memories = try {
            memoryStore.relevantMemoriesFor(text, activeItems)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            emptyList()
        }
        val request = InventoryAgentRequest(
            recognizedText = text,
            activeItems = activeItems,
            memories = memories
        )
        return preview(request)
    }

    suspend fun preview(request: InventoryAgentRequest): VoiceInputState {
        val action = planner.plan(request)
        return preview(request.recognizedText, action, request.activeItems)
    }

    suspend fun rememberSuccessfulAction(pending: VoiceInputState.PendingConfirmation) {
        try {
            memoryStore.rememberSuccessfulAction(
                recognizedText = pending.recognizedText,
                action = pending.action,
                matchedItem = pending.matchedItem
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // A memory write should never affect an already confirmed inventory operation.
        }
    }

    fun preview(
        recognizedText: String,
        action: InventoryAction,
        activeItems: List<Item>
    ): VoiceInputState {
        return when (action) {
            is InventoryAction.AddItem -> {
                val validationError = action.draft.validate()
                if (validationError == null) {
                    VoiceInputState.PendingConfirmation(recognizedText, action)
                } else {
                    VoiceInputState.Error(validationError, recognizedText)
                }
            }
            is InventoryAction.ConsumeItem -> {
                previewInventoryChange(recognizedText, action, activeItems, action.itemName, action.quantity, action.itemId)
            }
            is InventoryAction.DiscardItem -> {
                previewInventoryChange(recognizedText, action, activeItems, action.itemName, action.quantity, action.itemId)
            }
            is InventoryAction.AskClarification -> VoiceInputState.Error(action.message, recognizedText)
        }
    }

    fun matchItem(
        itemName: String,
        quantity: Int,
        activeItems: List<Item>,
        itemId: Long? = null
    ): InventoryMatchResult {
        if (quantity <= 0) {
            return InventoryMatchResult.NotFound("数量必须大于 0")
        }

        val itemById = itemId?.let { id -> activeItems.firstOrNull { it.id == id } }
        if (itemById != null) {
            val remainingQuantity = itemById.remainingQuantity()
            return if (remainingQuantity >= quantity) {
                InventoryMatchResult.Matched(itemById)
            } else {
                InventoryMatchResult.NotFound("“${itemById.name}”剩余 $remainingQuantity，不能操作 $quantity")
            }
        }

        val normalizedName = normalize(itemName)
        if (normalizedName.isBlank()) {
            return InventoryMatchResult.NotFound("请确认物品名称")
        }

        val availableItems = activeItems
            .filter { it.remainingQuantity() >= quantity }
            .sortedWith(compareBy<Item> { it.expirationDate }.thenBy { it.name })

        val exactMatches = availableItems.filter {
            normalize(it.name) == normalizedName
        }
        exactMatches.singleOrNull()?.let { return InventoryMatchResult.Matched(it) }
        if (exactMatches.size > 1) {
            return InventoryMatchResult.NeedsSelection(
                candidates = exactMatches,
                message = "找到多个“$itemName”，请选择要操作的库存"
            )
        }

        val containsMatches = availableItems.filter {
            val itemNameNormalized = normalize(it.name)
            itemNameNormalized.contains(normalizedName) || normalizedName.contains(itemNameNormalized)
        }
        containsMatches.singleOrNull()?.let { return InventoryMatchResult.Matched(it) }
        if (containsMatches.size > 1) {
            return InventoryMatchResult.NeedsSelection(
                candidates = containsMatches,
                message = "找到多个相关物品，请选择要操作的库存"
            )
        }

        val inferredCategory = parser.inferCategory(itemName)
        val similarMatches = availableItems.filter {
            it.category == inferredCategory && isSimilarName(normalizedName, normalize(it.name))
        }
        similarMatches.singleOrNull()?.let { return InventoryMatchResult.Matched(it) }
        if (similarMatches.size > 1) {
            return InventoryMatchResult.NeedsSelection(
                candidates = similarMatches,
                message = "找到多个相似物品，请选择要操作的库存"
            )
        }

        return InventoryMatchResult.NotFound("没有找到“$itemName”的可用库存")
    }

    private fun previewInventoryChange(
        recognizedText: String,
        action: InventoryAction,
        activeItems: List<Item>,
        itemName: String,
        quantity: Int,
        itemId: Long? = null
    ): VoiceInputState {
        return when (val match = matchItem(itemName, quantity, activeItems, itemId)) {
            is InventoryMatchResult.Matched -> {
                val remainingQuantity = match.item.remainingQuantity()
                if (quantity > remainingQuantity) {
                    return VoiceInputState.Error(
                        message = "“${match.item.name}”剩余 $remainingQuantity，不能操作 $quantity",
                        recognizedText = recognizedText
                    )
                }

                VoiceInputState.PendingConfirmation(
                    recognizedText = recognizedText,
                    action = action,
                    matchedItem = match.item
                )
            }
            is InventoryMatchResult.NeedsSelection -> {
                VoiceInputState.NeedsSelection(
                    recognizedText = recognizedText,
                    action = action,
                    candidates = match.candidates,
                    message = match.message
                )
            }
            is InventoryMatchResult.NotFound -> VoiceInputState.Error(match.message, recognizedText)
        }
    }

    private fun ItemDraft.validate(): String? {
        return when {
            name.isBlank() -> "请确认要新增的物品名称"
            quantity <= 0 -> "数量必须大于 0"
            expirationDate.isBefore(purchaseDate) -> "过期日期不能早于购买日期"
            reminderDays.any { it <= 0 } -> "提醒天数必须大于 0"
            else -> null
        }
    }

    private fun normalize(text: String): String {
        return text
            .replace(Regex("\\s+"), "")
            .replace(Regex("[,.;:!?，。；：！？、]"), "")
            .trim()
    }

    private fun isSimilarName(query: String, candidate: String): Boolean {
        if (query.length < 2 || candidate.length < 2) return false
        val queryChars = query.toSet()
        val candidateChars = candidate.toSet()
        val overlap = queryChars.intersect(candidateChars).size
        return overlap >= 2 && overlap.toFloat() / queryChars.size >= 0.5f
    }
}

fun Item.remainingQuantity(): Int = (quantity - usedQuantity).coerceAtLeast(0)
