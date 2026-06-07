package com.jishiyong.agent

import kotlinx.coroutines.CancellationException

interface InventoryActionPlanner {
    suspend fun plan(request: InventoryAgentRequest): InventoryAction
}

class RuleBasedInventoryActionPlanner(
    private val parser: InventoryCommandParser = InventoryCommandParser()
) : InventoryActionPlanner {
    override suspend fun plan(request: InventoryAgentRequest): InventoryAction {
        return parser.parse(request.recognizedText, request.today)
    }
}

class HybridInventoryActionPlanner(
    private val primary: InventoryActionPlanner,
    private val fallback: InventoryActionPlanner
) : InventoryActionPlanner {
    override suspend fun plan(request: InventoryAgentRequest): InventoryAction {
        return try {
            when (val action = primary.plan(request)) {
                is InventoryAction.AskClarification -> {
                    val fallbackAction = fallback.plan(request)
                    if (fallbackAction is InventoryAction.AskClarification) action else fallbackAction
                }
                else -> action
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            fallback.plan(request)
        }
    }
}
