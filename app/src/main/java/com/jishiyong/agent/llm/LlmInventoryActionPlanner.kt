package com.jishiyong.agent.llm

import com.jishiyong.agent.InventoryAction
import com.jishiyong.agent.InventoryActionPlanner
import com.jishiyong.agent.InventoryAgentRequest

class LlmInventoryActionPlanner(
    private val client: LlmClient,
    private val promptBuilder: LlmInventoryPromptBuilder,
    private val actionParser: LlmInventoryActionJsonParser
) : InventoryActionPlanner {

    override suspend fun plan(request: InventoryAgentRequest): InventoryAction {
        val response = client.complete(
            messages = promptBuilder.buildMessages(request),
            temperature = 0.0
        )
        return actionParser.parse(response, request.today)
    }
}
