package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item
import kotlinx.coroutines.CancellationException

class InventoryAgent(
    private val planner: InventoryActionPlanner = RuleBasedInventoryActionPlanner(),
    private val memoryStore: AgentMemoryStore = EmptyAgentMemoryStore,
    private val parser: InventoryCommandParser = InventoryCommandParser(),
    private val itemMatcher: InventoryItemMatcher = InventoryItemMatcher(),
    private val previewer: InventoryActionPreviewer = InventoryActionPreviewer(itemMatcher = itemMatcher),
    private val logger: AgentLogger = NoOpAgentLogger,
    val mode: InventoryAgentMode = InventoryAgentMode.LOCAL_RULES
) {

    fun preview(text: String, activeItems: List<Item>): VoiceInputState {
        val action = parser.parse(text)
        return preview(text, InventoryPlan(action), activeItems)
    }

    suspend fun previewWithPlanning(
        text: String,
        activeItems: List<Item>
    ): VoiceInputState {
        val memories = try {
            memoryStore.relevantMemoriesFor(text, activeItems)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn("Failed to load inventory agent memories", exception)
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
        val plan = try {
            planner.planWithDiagnostics(request)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn("Inventory action planning failed", exception)
            return VoiceInputState.Error("${mode.displayName}解析失败，请稍后再试", request.recognizedText)
        }
        return preview(request.recognizedText, plan, request.activeItems)
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
        } catch (exception: Exception) {
            logger.warn("Failed to persist inventory agent memory", exception)
            // A memory write should never affect an already confirmed inventory operation.
        }
    }

    fun preview(
        recognizedText: String,
        action: InventoryAction,
        activeItems: List<Item>
    ): VoiceInputState {
        return preview(recognizedText, InventoryPlan(action), activeItems)
    }

    private fun preview(
        recognizedText: String,
        plan: InventoryPlan,
        activeItems: List<Item>
    ): VoiceInputState {
        val context = VoiceCommandContext(
            recognizedText = recognizedText,
            diagnostics = plan.diagnostics
        )
        return when (val preview = previewer.preview(plan.action, activeItems)) {
            is InventoryActionPreview.Ready -> {
                VoiceInputState.PendingConfirmation(
                    context = context,
                    confirmation = preview.confirmation
                )
            }
            is InventoryActionPreview.RequiresSelection -> {
                VoiceInputState.NeedsSelection(
                    context = context,
                    selection = preview.selection
                )
            }
            is InventoryActionPreview.Failed -> {
                VoiceInputState.Error(preview.message, recognizedText)
            }
        }
    }

    fun matchItem(
        itemName: String,
        quantity: Int,
        activeItems: List<Item>,
        itemId: Long? = null
    ): InventoryMatchResult {
        return itemMatcher.matchItem(itemName, quantity, activeItems, itemId)
    }
}
