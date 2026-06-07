package com.jishiyong.agent

import com.jishiyong.agent.llm.LlmInventoryActionJsonParser
import com.jishiyong.agent.llm.LlmInventoryActionPlanner
import com.jishiyong.agent.llm.LlmInventoryPromptBuilder
import com.jishiyong.agent.llm.OpenAiCompatibleLlmClient
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate

class RealLlmSmokeTest {

    @Test
    fun realLlmPlannerParsesConsumeAction() {
        val apiKey = System.getenv("AI_API_KEY").orEmpty()
        assumeTrue(
            "Set RUN_REAL_LLM_SMOKE=true and AI_API_KEY to run the real LLM smoke test.",
            System.getenv("RUN_REAL_LLM_SMOKE") == "true" && apiKey.isNotBlank()
        )

        runBlocking {
            val milk = item(id = 1, name = "蒙牛纯牛奶")
            val agent = InventoryAgent(
                planner = LlmInventoryActionPlanner(
                    client = OpenAiCompatibleLlmClient(
                        baseUrl = System.getenv("AI_API_BASE_URL").orEmpty()
                            .ifBlank { DEFAULT_AI_API_BASE_URL },
                        model = System.getenv("AI_MODEL_NAME").orEmpty()
                            .ifBlank { DEFAULT_AI_MODEL_NAME },
                        apiKey = apiKey
                    ),
                    promptBuilder = LlmInventoryPromptBuilder(),
                    actionParser = LlmInventoryActionJsonParser()
                ),
                mode = InventoryAgentMode.LLM_WITH_RULE_FALLBACK
            )

            val state = agent.previewWithPlanning(
                text = System.getenv("REAL_LLM_RECOGNIZED_TEXT").orEmpty()
                    .ifBlank { "今天喝了一瓶蒙牛纯牛奶" },
                activeItems = listOf(milk)
            )

            assertTrue(state is VoiceInputState.PendingConfirmation)
            val pending = state as VoiceInputState.PendingConfirmation
            assertEquals(milk, pending.matchedItem)
            assertTrue(pending.action is InventoryAction.ConsumeItem)
            assertEquals(1, (pending.action as InventoryAction.ConsumeItem).quantity)
        }
    }

    private fun item(id: Long, name: String): Item {
        return Item(
            id = id,
            name = name,
            category = ItemCategory.DRINK,
            purchaseDate = LocalDate.of(2026, 6, 1),
            expirationDate = LocalDate.of(2026, 6, 12),
            quantity = 3
        )
    }

    private companion object {
        private const val DEFAULT_AI_API_BASE_URL = "https://api.edgefn.net/v1"
        private const val DEFAULT_AI_MODEL_NAME = "DeepSeek-V3.2"
    }
}
