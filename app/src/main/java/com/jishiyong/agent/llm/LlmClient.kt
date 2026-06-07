package com.jishiyong.agent.llm

data class LlmMessage(
    val role: LlmRole,
    val content: String
)

enum class LlmRole(val apiName: String) {
    SYSTEM("system"),
    USER("user")
}

interface LlmClient {
    suspend fun complete(messages: List<LlmMessage>, temperature: Double = 0.0): String
}
