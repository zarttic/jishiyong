package com.jishiyong.agent.llm

import com.jishiyong.agent.InventoryActionPlanner
import com.jishiyong.agent.InventoryAgentRequest
import com.jishiyong.agent.InventoryPlan

class LlmInventoryActionPlanner(
    private val client: LlmClient,
    private val promptBuilder: LlmInventoryPromptBuilder,
    private val actionParser: LlmInventoryActionJsonParser
) : InventoryActionPlanner {

    override suspend fun planWithDiagnostics(request: InventoryAgentRequest): InventoryPlan {
        val response = client.complete(
            messages = promptBuilder.buildMessages(request),
            temperature = 0.0
        )
        return InventoryPlan(actionParser.parse(response, request.today))
    }
}
