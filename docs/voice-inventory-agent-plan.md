# 语音库存 Agent 开发计划

## 目标

在现有库存/保质期管理 App 中增加语音 Agent，让用户通过一句自然语言完成新增库存、消耗库存、丢弃库存等操作。

示例：

- “今天喝了一瓶蒙牛牛奶”
- “今天买了一箱东方树叶，有 15 瓶，7 月 20 号过期”
- “把那盒过期牛奶扔了”

目标体验：

```text
语音输入 -> 转文字 -> 解析为结构化动作 -> 本地匹配库存 -> 展示确认 -> 执行数据库操作
```

核心原则：

```text
AI 不直接写数据库，只输出结构化 Action。
App 负责校验、匹配、确认和执行。
```

## 第一版范围

MVP 只支持库存操作，不做通用聊天。

必须支持：

1. 新增物品  
   例：“今天买了两盒牛奶，6 月 12 号过期”

2. 消耗库存  
   例：“刚喝了一瓶蒙牛牛奶”

3. 丢弃库存  
   例：“把过期的牛奶扔了”

4. 操作确认  
   解析后先展示预览，用户确认后再写数据库。

暂不做：

- 拍照识别
- 自动扫描相册
- 通用聊天
- 药品/食品安全判断
- 多轮复杂对话

## 推荐架构

当前实现采用 **Planner + Tool/Executor** 的 agent 模式：

```text
语音输入
  -> LLM Planner 生成 InventoryAction
  -> App 校验字段、匹配库存、展示确认
  -> InventoryActionExecutor 执行本地数据库工具
```

选择这个模式的原因：

- LLM 适合处理自然语言、相对日期、别名和模糊表达。
- 数据库写入、库存数量校验、候选选择必须由 App 控制，避免模型直接改数据。
- 记忆通过 `AgentMemoryStore` 注入，不需要让 UI 和执行器理解记忆细节。

核心文件：

```text
agent/InventoryAction.kt
agent/InventoryAgent.kt
agent/InventoryActionPlanner.kt
AppContainer.kt
agent/InventoryAgentRequest.kt
agent/InventoryAgentMode.kt
agent/AgentMemory.kt
agent/InventoryAliasMemoryEngine.kt
agent/PreferenceAgentMemoryStore.kt
agent/InventoryCommandParser.kt
agent/InventoryMatchResult.kt
agent/VoiceInputState.kt
agent/llm/LlmClient.kt
agent/llm/OpenAiCompatibleLlmClient.kt
agent/llm/LlmInventoryActionPlanner.kt
agent/llm/LlmInventoryPromptBuilder.kt
agent/llm/LlmInventoryActionJsonParser.kt
```

运行策略：

```text
运行时未配置 AI key -> RuleBasedInventoryActionPlanner
运行时私有 SharedPreferences 中配置 AI key -> HybridInventoryActionPlanner(LLM 优先，规则解析兜底)
```

## 数据结构

```kotlin
sealed class InventoryAction {
    data class AddItem(val draft: ItemDraft) : InventoryAction()

    data class ConsumeItem(
        val itemName: String,
        val quantity: Int,
        val itemId: Long? = null
    ) : InventoryAction()

    data class DiscardItem(
        val itemName: String,
        val quantity: Int,
        val itemId: Long? = null
    ) : InventoryAction()

    data class AskClarification(
        val message: String
    ) : InventoryAction()
}
```

```kotlin
data class ItemDraft(
    val name: String,
    val category: ItemCategory,
    val quantity: Int,
    val purchaseDate: LocalDate,
    val expirationDate: LocalDate,
    val reminderDays: List<Int>,
    val note: String = ""
)
```

## AI 输出格式

模型必须输出严格 JSON，由 App 做校验。当前 action 名称使用显式 snake_case。

消耗库存：

```json
{
  "action": "consume_item",
  "item_id": 12,
  "item_name": "蒙牛牛奶",
  "quantity": 1
}
```

新增库存：

```json
{
  "action": "add_item",
  "item": {
    "name": "东方树叶",
    "category": "DRINK",
    "quantity": 15,
    "purchase_date": "2026-06-05",
    "expiration_date": "2026-07-20",
    "reminder_days": [7, 3, 1],
    "note": "一箱"
  }
}
```

解析失败或信息不足时：

```json
{
  "action": "ask_clarification",
  "message": "请确认要消耗哪一种牛奶"
}
```

## EdgeFN / DeepSeek 接入配置

当前项目预留以下 `BuildConfig` 配置：

```kotlin
BuildConfig.AI_API_BASE_URL // 默认 https://api.edgefn.net/v1
BuildConfig.AI_MODEL_NAME   // 默认 DeepSeek-V3.2
```

不要把真实 API Key 提交到 Git，也不要通过 Gradle 参数打包进 APK。App 运行时只应从私有 SharedPreferences 或后端代理获取 key；当前没有面向用户的设置入口，因此正式可配置化应补设置页或后端代理。开发真实 LLM smoke test 时可以使用环境变量：

```bash
RUN_REAL_LLM_SMOKE=true AI_API_KEY=your_api_key_here ./gradlew :app:testDebugUnitTest --tests com.jishiyong.agent.RealLlmSmokeTest
```

EdgeFN Chat Completions 请求示例：

```http
POST https://api.edgefn.net/v1/chat/completions
Authorization: Bearer ${AI_API_KEY}
Content-Type: application/json
```

请求体：

```json
{
  "model": "DeepSeek-V3.2",
  "messages": [
    {
      "role": "system",
      "content": "你是库存指令解析器。只输出严格 JSON，不输出解释。"
    },
    {
      "role": "user",
      "content": "今天喝了一瓶蒙牛牛奶"
    }
  ]
}
```

已实现 `OpenAiCompatibleLlmClient`，请求路径为 `${AI_API_BASE_URL}/chat/completions`，低温度调用并由 `LlmInventoryActionJsonParser` 校验 JSON 字段。即使是自用版本，也不要把 key 写死到源码；如果 key 已经暴露，先去平台后台轮换。

## 记忆能力

当前已经接入轻量记忆：

- `InventoryAliasMemoryEngine`：纯 Kotlin 规则引擎，负责从确认成功的动作中学习别名，并按用户输入召回相关记忆。
- `PreferenceAgentMemoryStore`：DataStore-backed 持久化实现，保存“用户说 X 时通常指库存 Y”的轻量别名记忆。
- `InventoryAgent.rememberSuccessfulAction()`：只在库存操作确认并执行成功后写入记忆，执行失败不会污染记忆。
- `LlmInventoryPromptBuilder`：把召回到的 `InventoryAgentRequest.memories` 放入 prompt，帮助 LLM 解析用户个人化表达。

后续可以继续演进：

1. 长期行为记忆：用 Room 表保存标准化 key/value、权重、更新时间。
2. 检索记忆：按输入文本、活跃库存和历史动作做关键词/向量召回，只把相关记忆放进 prompt。
3. 多轮工作记忆：在 `VoiceInputState` 中保留最近澄清上下文，支持“就是第二个”“改成两瓶”这类后续指令。

## 页面改动

主要修改：

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/jishiyong/ui/screens/HomeScreen.kt
app/src/main/java/com/jishiyong/viewmodel/MainViewModel.kt
app/src/main/java/com/jishiyong/data/repository/ItemRepository.kt
```

首页增加麦克风入口，建议放在搜索区域右侧或新增一个语音 FAB。

语音面板状态：

```text
待说话
识别中
解析中
待确认
执行成功
执行失败
```

确认面板示例：

```text
识别文本：今天喝了一瓶蒙牛牛奶

将执行：
消耗 蒙牛牛奶 x1

[确认执行] [取消]
```

## 语音识别

第一版使用 Android 系统 `SpeechRecognizer`。

新增权限：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

流程：

1. 用户点击麦克风。
2. App 请求录音权限。
3. 开始系统语音识别。
4. 识别完成得到文本。
5. 文本交给 `InventoryCommandParser`。
6. 展示 Action 预览。

## 库存匹配规则

消耗或丢弃时，根据 `activeItems` 匹配库存。

匹配优先级：

1. 名称完全匹配。
2. 名称包含匹配。
3. 分类相同且名称相似。
4. 多个候选时优先选择最早过期的。
5. 仍不确定时展示候选列表让用户选择。

示例：

```text
库存：
- 蒙牛纯牛奶 250ml
- 伊利纯牛奶

用户说：
喝了一瓶牛奶
```

如果匹配多个，弹出选择列表，不自动执行。

## 执行规则

新增：

1. 将 `ItemDraft` 转为 `Item`。
2. 调用 `repository.insert(item)`。

消耗：

1. 找到匹配物品。
2. 调用 DAO/Repository 层的条件 delta 更新，要求 `usedQuantity + quantity <= item.quantity` 且 `isConsumed = 0`。
3. 按 affected rows 和事务内复查结果判断成功、数量不足、已处理或并发冲突；不能用旧快照写入绝对 `usedQuantity`。

丢弃：

1. 找到匹配物品。
2. 使用同一套条件 delta 更新，只是完成时写入 `ConsumeType.DISCARDED`。
3. 按 affected rows 和事务内复查结果判断成功、数量不足、已处理或并发冲突。

所有写入操作都必须经过本地校验，不能直接相信 AI 输出。

## 当前实现状态

已经完成：

- 本地规则解析链路保留为 fallback。
- LLM planner 已接入 OpenAI-compatible Chat Completions。
- `HybridInventoryActionPlanner` 会先尝试 LLM；只有请求失败时回到本地规则，LLM 明确要求澄清时保留澄清。
- UI 能展示当前是 `AI agent` 解析还是 `本地规则` 解析。
- 所有新增、消耗、丢弃写库动作仍经过本地校验和用户确认。
- 多候选库存仍由用户选择。
- 轻量别名记忆已通过 DataStore 持久化。

后续可做：

- 使用后端代理 AI 请求，避免 API key 进入 APK。
- 增加操作历史表，支持撤销。
- 增加多轮工作记忆，支持“就是第二个”“改成两瓶”。
- 将 DataStore 轻量记忆升级为 Room/向量检索记忆。

### 自动执行与撤销

当解析准确率稳定后，再支持低风险操作自动执行。

目标体验：

```text
语音输入 -> 自动执行 -> 首页提示“已完成，可撤销”
```

撤销功能建议后续新增操作历史表，不在 MVP 内完成。

## 验收标准

第一版完成后应满足：

- 能通过语音新增一件物品。
- 能通过语音消耗已有库存。
- 能通过语音丢弃已有库存。
- 多候选物品时能让用户选择。
- 解析失败不影响原有手动功能。
- 所有写数据库操作都经过本地校验。
- GitHub Actions 能成功构建 APK。

## 推荐开发顺序

1. 用 GitHub workflow 或 x86_64 Android SDK 环境跑 `testDebugUnitTest` 和 `assembleDebug`。
2. 用真实 `AI_API_KEY` 做端到端 smoke test。
3. 如果要发布给更多用户，先把 AI 调用迁到后端代理。
4. 再做撤销、多轮澄清和更强的长期记忆。
