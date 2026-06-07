package com.jishiyong.agent

import android.content.Context

data class AiAgentConfiguration(
    val baseUrl: String,
    val model: String,
    val apiKey: String
) {
    val isComplete: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

class AiAgentSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadConfiguration(
        defaultBaseUrl: String,
        defaultModel: String
    ): AiAgentConfiguration {
        return AiAgentConfiguration(
            baseUrl = preferences.getString(KEY_BASE_URL, defaultBaseUrl).orEmpty().trim(),
            model = preferences.getString(KEY_MODEL, defaultModel).orEmpty().trim(),
            apiKey = preferences.getString(KEY_API_KEY, "").orEmpty().trim()
        )
    }

    companion object {
        const val PREFS_NAME = "ai_agent_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_API_KEY = "api_key"
    }
}
