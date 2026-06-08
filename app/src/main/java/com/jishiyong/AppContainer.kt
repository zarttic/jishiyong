package com.jishiyong

import android.content.Context
import com.jishiyong.agent.AndroidAgentLogger
import com.jishiyong.agent.AiAgentSettings
import com.jishiyong.agent.CategoryInferencer
import com.jishiyong.agent.HybridInventoryActionPlanner
import com.jishiyong.agent.InventoryActionExecutor
import com.jishiyong.agent.InventoryActionPreviewer
import com.jishiyong.agent.InventoryAgent
import com.jishiyong.agent.InventoryAgentMode
import com.jishiyong.agent.InventoryCommandParser
import com.jishiyong.agent.InventoryItemMatcher
import com.jishiyong.agent.RoomAgentMemoryStore
import com.jishiyong.agent.RuleBasedInventoryActionPlanner
import com.jishiyong.agent.llm.LlmInventoryActionJsonParser
import com.jishiyong.agent.llm.LlmInventoryActionPlanner
import com.jishiyong.agent.llm.LlmInventoryPromptBuilder
import com.jishiyong.agent.llm.OpenAiCompatibleLlmClient
import com.jishiyong.data.db.AppDatabase
import com.jishiyong.data.repository.ItemRepository
import com.jishiyong.data.repository.SystemTodayProvider
import com.jishiyong.data.repository.TodayProvider
import com.jishiyong.update.AppUpdateChecker

interface AppContainerProvider {
    val container: AppContainer
}

class AppContainer(
    context: Context,
    val todayProvider: TodayProvider = SystemTodayProvider
) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }

    val repository: ItemRepository by lazy {
        ItemRepository(
            itemDao = database.itemDao(),
            todayProvider = todayProvider
        )
    }

    val updateChecker: AppUpdateChecker by lazy {
        AppUpdateChecker()
    }

    val actionExecutor: InventoryActionExecutor by lazy {
        InventoryActionExecutor()
    }

    val inventoryAgent: InventoryAgent by lazy {
        createInventoryAgent()
    }

    private fun createInventoryAgent(): InventoryAgent {
        val logger = AndroidAgentLogger()
        val categoryInferencer = CategoryInferencer()
        val parser = InventoryCommandParser(categoryInferencer)
        val itemMatcher = InventoryItemMatcher(categoryInferencer)
        val previewer = InventoryActionPreviewer(itemMatcher = itemMatcher)
        val fallbackPlanner = RuleBasedInventoryActionPlanner(parser)
        val aiConfiguration = AiAgentSettings(appContext).loadConfiguration(
            defaultBaseUrl = BuildConfig.AI_API_BASE_URL,
            defaultModel = BuildConfig.AI_MODEL_NAME
        )
        val mode: InventoryAgentMode
        val planner = if (aiConfiguration.isComplete) {
            mode = InventoryAgentMode.LLM_WITH_RULE_FALLBACK
            HybridInventoryActionPlanner(
                primary = LlmInventoryActionPlanner(
                    client = OpenAiCompatibleLlmClient(
                        baseUrl = aiConfiguration.baseUrl,
                        model = aiConfiguration.model,
                        apiKey = aiConfiguration.apiKey
                    ),
                    promptBuilder = LlmInventoryPromptBuilder(),
                    actionParser = LlmInventoryActionJsonParser(categoryInferencer)
                ),
                fallback = fallbackPlanner,
                logger = logger
            )
        } else {
            mode = InventoryAgentMode.LOCAL_RULES
            fallbackPlanner
        }

        return InventoryAgent(
            planner = planner,
            memoryStore = RoomAgentMemoryStore(
                dao = database.agentMemoryDao(),
                logger = logger
            ),
            parser = parser,
            itemMatcher = itemMatcher,
            previewer = previewer,
            logger = logger,
            mode = mode
        )
    }
}
