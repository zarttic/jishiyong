package com.jishiyong.agent

enum class InventoryAgentMode(
    val displayName: String,
    val parsingMessagePrefix: String
) {
    LLM_WITH_RULE_FALLBACK(
        displayName = "AI agent",
        parsingMessagePrefix = "正在通过 AI agent 解析"
    ),
    LOCAL_RULES(
        displayName = "本地规则",
        parsingMessagePrefix = "正在使用本地规则解析"
    )
}
