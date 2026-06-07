package com.jishiyong.agent

import android.content.Context
import com.jishiyong.BuildConfig
import com.jishiyong.agent.llm.LlmInventoryActionJsonParser
import com.jishiyong.agent.llm.LlmInventoryActionPlanner
import com.jishiyong.agent.llm.LlmInventoryPromptBuilder
import com.jishiyong.agent.llm.OpenAiCompatibleLlmClient

object InventoryAgentFactory {
    fun createDefault(context: Context): InventoryAgent {
        val fallbackPlanner = RuleBasedInventoryActionPlanner()
        val hasLlmConfiguration = BuildConfig.AI_API_KEY.isNotBlank() &&
                BuildConfig.AI_API_BASE_URL.isNotBlank() &&
                BuildConfig.AI_MODEL_NAME.isNotBlank()
        val mode: InventoryAgentMode
        val planner = if (hasLlmConfiguration) {
            mode = InventoryAgentMode.LLM_WITH_RULE_FALLBACK
            HybridInventoryActionPlanner(
                primary = LlmInventoryActionPlanner(
                    client = OpenAiCompatibleLlmClient(
                        baseUrl = BuildConfig.AI_API_BASE_URL,
                        model = BuildConfig.AI_MODEL_NAME,
                        apiKey = BuildConfig.AI_API_KEY
                    ),
                    promptBuilder = LlmInventoryPromptBuilder(),
                    actionParser = LlmInventoryActionJsonParser()
                ),
                fallback = fallbackPlanner
            )
        } else {
            mode = InventoryAgentMode.LOCAL_RULES
            fallbackPlanner
        }
        return InventoryAgent(
            planner = planner,
            memoryStore = PreferenceAgentMemoryStore(context),
            mode = mode
        )
    }
}
