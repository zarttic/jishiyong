package com.jishiyong.agent

import com.jishiyong.agent.llm.LlmClient
import com.jishiyong.agent.llm.LlmInventoryActionJsonParser
import com.jishiyong.agent.llm.LlmInventoryActionPlanner
import com.jishiyong.agent.llm.LlmInventoryPromptBuilder
import com.jishiyong.agent.llm.LlmMessage
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class LlmBackedInventoryAgentTest {

    @Test
    fun previewWithPlanningUsesLlmPlannerAndRequiresConfirmation() = runTest {
        val milk = item(id = 1, name = "蒙牛纯牛奶")
        val agent = InventoryAgent(
            planner = LlmInventoryActionPlanner(
                client = FakeLlmClient("""{"action":"consume_item","item_id":1,"item_name":"牛奶","quantity":1}"""),
                promptBuilder = LlmInventoryPromptBuilder(),
                actionParser = LlmInventoryActionJsonParser()
            )
        )

        val state = agent.previewWithPlanning("我喝了一瓶常买的奶", listOf(milk))

        assertTrue(state is VoiceInputState.PendingConfirmation)
        val pending = state as VoiceInputState.PendingConfirmation
        assertEquals(milk, pending.matchedItem)
        assertTrue(pending.action is InventoryAction.ConsumeItem)
    }

    @Test
    fun previewWithPlanningUsesItemIdBeforeNameMatching() = runTest {
        val selected = item(id = 1, name = "蒙牛纯牛奶", expirationDate = LocalDate.of(2026, 6, 12))
        val other = item(id = 2, name = "伊利纯牛奶", expirationDate = LocalDate.of(2026, 6, 8))
        val agent = InventoryAgent(
            planner = LlmInventoryActionPlanner(
                client = FakeLlmClient("""{"action":"consume_item","item_id":1,"item_name":"牛奶","quantity":1}"""),
                promptBuilder = LlmInventoryPromptBuilder(),
                actionParser = LlmInventoryActionJsonParser()
            )
        )

        val state = agent.previewWithPlanning("喝一瓶牛奶", listOf(selected, other))

        assertTrue(state is VoiceInputState.PendingConfirmation)
        assertEquals(selected, (state as VoiceInputState.PendingConfirmation).matchedItem)
    }

    @Test
    fun hybridPlannerFallsBackWhenLlmRequestFails() = runTest {
        val logger = RecordingAgentLogger()
        val planner = HybridInventoryActionPlanner(
            primary = LlmInventoryActionPlanner(
                client = FakeLlmClient(error = IOException("network failed")),
                promptBuilder = LlmInventoryPromptBuilder(),
                actionParser = LlmInventoryActionJsonParser()
            ),
            fallback = RuleBasedInventoryActionPlanner(),
            logger = logger
        )

        val plan = planner.planWithDiagnostics(
            InventoryAgentRequest(
                recognizedText = "今天喝了一瓶蒙牛牛奶",
                activeItems = emptyList(),
                today = LocalDate.of(2026, 6, 5)
            )
        )

        val action = plan.action
        assertTrue(action is InventoryAction.ConsumeItem)
        assertEquals("蒙牛牛奶", (action as InventoryAction.ConsumeItem).itemName)
        assertEquals(InventoryPlanningDiagnosticKind.LLM_FALLBACK, plan.diagnostics.single().kind)
        assertTrue(plan.diagnostics.single().message.contains("本地规则"))
        assertTrue(logger.warnings.single().contains("LLM inventory planner failed"))
    }

    @Test
    fun promptIncludesInventoryAndMemoryContext() = runTest {
        val client = FakeLlmClient("""{"action":"consume_item","item_name":"蒙牛纯牛奶","quantity":1}""")
        val planner = LlmInventoryActionPlanner(
            client = client,
            promptBuilder = LlmInventoryPromptBuilder(),
            actionParser = LlmInventoryActionJsonParser()
        )

        planner.plan(
            InventoryAgentRequest(
                recognizedText = "喝一瓶牛奶",
                activeItems = listOf(item(id = 1, name = "蒙牛纯牛奶")),
                today = LocalDate.of(2026, 6, 5),
                memories = listOf(AgentMemory(key = "preferred_milk", value = "用户常说牛奶指蒙牛纯牛奶"))
            )
        )

        val prompt = client.messages.joinToString("\n") { it.content }
        assertTrue(prompt.contains("蒙牛纯牛奶"))
        assertTrue(prompt.contains("preferred_milk"))
    }

    @Test
    fun rememberSuccessfulActionDelegatesToMemoryStore() = runTest {
        val memoryStore = RecordingMemoryStore()
        val agent = InventoryAgent(memoryStore = memoryStore)
        val item = item(id = 1, name = "蒙牛纯牛奶")
        val pending = VoiceInputState.PendingConfirmation(
            recognizedText = "喝了常买的奶",
            action = InventoryAction.ConsumeItem(itemName = "常买的奶", quantity = 1),
            matchedItem = item
        )

        agent.rememberSuccessfulAction(pending)

        assertEquals("喝了常买的奶", memoryStore.rememberedText)
        assertEquals(item, memoryStore.rememberedItem)
        assertTrue(memoryStore.rememberedAction is InventoryAction.ConsumeItem)
    }

    @Test
    fun agentModeCanDescribeLlmBackedParsing() {
        val agent = InventoryAgent(
            planner = LlmInventoryActionPlanner(
                client = FakeLlmClient("""{"action":"ask_clarification","message":"请补充信息"}"""),
                promptBuilder = LlmInventoryPromptBuilder(),
                actionParser = LlmInventoryActionJsonParser()
            ),
            mode = InventoryAgentMode.LLM_WITH_RULE_FALLBACK
        )

        assertEquals(InventoryAgentMode.LLM_WITH_RULE_FALLBACK, agent.mode)
        assertTrue(agent.mode.parsingMessagePrefix.contains("AI agent"))
    }

    private fun item(
        id: Long,
        name: String,
        quantity: Int = 3,
        usedQuantity: Int = 0,
        expirationDate: LocalDate = LocalDate.of(2026, 6, 12)
    ): Item {
        return Item(
            id = id,
            name = name,
            category = ItemCategory.DRINK,
            purchaseDate = LocalDate.of(2026, 6, 1),
            expirationDate = expirationDate,
            quantity = quantity,
            usedQuantity = usedQuantity
        )
    }

    private class FakeLlmClient(
        private val response: String = "",
        private val error: Exception? = null
    ) : LlmClient {
        val messages = mutableListOf<LlmMessage>()

        override suspend fun complete(messages: List<LlmMessage>, temperature: Double): String {
            this.messages += messages
            error?.let { throw it }
            return response
        }
    }

    private class RecordingMemoryStore : AgentMemoryStore {
        var rememberedText: String? = null
        var rememberedAction: InventoryAction? = null
        var rememberedItem: Item? = null

        override suspend fun relevantMemoriesFor(input: String, activeItems: List<Item>): List<AgentMemory> {
            return emptyList()
        }

        override suspend fun rememberSuccessfulAction(
            recognizedText: String,
            action: InventoryAction,
            matchedItem: Item?
        ) {
            rememberedText = recognizedText
            rememberedAction = action
            rememberedItem = matchedItem
        }
    }

    private class RecordingAgentLogger : AgentLogger {
        val warnings = mutableListOf<String>()

        override fun warn(message: String, throwable: Throwable?) {
            warnings += message
        }
    }
}
