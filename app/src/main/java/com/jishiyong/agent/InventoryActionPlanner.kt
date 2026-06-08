package com.jishiyong.agent

import kotlinx.coroutines.CancellationException

interface InventoryActionPlanner {
    suspend fun plan(request: InventoryAgentRequest): InventoryAction {
        return planWithDiagnostics(request).action
    }

    suspend fun planWithDiagnostics(request: InventoryAgentRequest): InventoryPlan
}

class RuleBasedInventoryActionPlanner(
    private val parser: InventoryCommandParser = InventoryCommandParser()
) : InventoryActionPlanner {
    override suspend fun planWithDiagnostics(request: InventoryAgentRequest): InventoryPlan {
        return InventoryPlan(parser.parse(request.recognizedText, request.today))
    }
}

class HybridInventoryActionPlanner(
    private val primary: InventoryActionPlanner,
    private val fallback: InventoryActionPlanner,
    private val logger: AgentLogger = NoOpAgentLogger
) : InventoryActionPlanner {
    override suspend fun planWithDiagnostics(request: InventoryAgentRequest): InventoryPlan {
        return try {
            val primaryPlan = primary.planWithDiagnostics(request)
            when (val action = primaryPlan.action) {
                is InventoryAction.AskClarification -> {
                    primaryPlan.withDiagnostic(
                        InventoryPlanningDiagnostic(
                            kind = InventoryPlanningDiagnosticKind.LLM_CLARIFICATION,
                            message = "AI 解析需要补充信息",
                            technicalMessage = action.message
                        )
                    )
                }
                else -> primaryPlan
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn("LLM inventory planner failed; using rule fallback", exception)
            fallback.planWithDiagnostics(request).withDiagnostic(
                InventoryPlanningDiagnostic(
                    kind = InventoryPlanningDiagnosticKind.LLM_FALLBACK,
                    message = "AI 解析失败，已使用本地规则解析",
                    technicalMessage = exception.message
                )
            )
        }
    }
}
