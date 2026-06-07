package com.jishiyong.agent.llm

import com.jishiyong.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class OpenAiCompatibleLlmClient(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String
) : LlmClient {

    override suspend fun complete(messages: List<LlmMessage>, temperature: Double): String {
        return withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                throw IOException("AI API key is not configured")
            }
            if (baseUrl.isBlank() || model.isBlank()) {
                throw IOException("AI API endpoint or model is not configured")
            }

            val endpoint = URL("${baseUrl.trimEnd('/')}/chat/completions")
            val connection = endpoint.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("User-Agent", "JiShiYong/${BuildConfig.VERSION_NAME}")

            try {
                val requestBody = buildRequestBody(messages, temperature).toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { output ->
                    output.write(requestBody)
                }

                val responseCode = connection.responseCode
                val responseText = connection.readResponseText(responseCode)
                if (responseCode !in 200..299) {
                    throw IOException("AI request failed: HTTP $responseCode $responseText")
                }

                parseContent(responseText)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildRequestBody(messages: List<LlmMessage>, temperature: Double): JSONObject {
        val jsonMessages = JSONArray()
        messages.forEach { message ->
            jsonMessages.put(
                JSONObject()
                    .put("role", message.role.apiName)
                    .put("content", message.content)
            )
        }

        return JSONObject()
            .put("model", model)
            .put("temperature", temperature)
            .put("messages", jsonMessages)
    }

    private fun parseContent(responseText: String): String {
        val root = JSONObject(responseText)
        val choices = root.optJSONArray("choices")
            ?: throw IOException("AI response missing choices")
        val firstChoice = choices.optJSONObject(0)
            ?: throw IOException("AI response missing first choice")
        val message = firstChoice.optJSONObject("message")
            ?: throw IOException("AI response missing message")
        return message.optString("content").takeIf { it.isNotBlank() }
            ?: throw IOException("AI response content is empty")
    }

    private fun HttpURLConnection.readResponseText(responseCode: Int): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
}
