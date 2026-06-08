package com.jishiyong.agent.llm

import com.jishiyong.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OpenAiCompatibleLlmClient(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String,
    private val callFactory: Call.Factory = defaultClient
) : LlmClient {

    override suspend fun complete(messages: List<LlmMessage>, temperature: Double): String {
        if (apiKey.isBlank()) {
            throw IOException("AI API key is not configured")
        }
        if (baseUrl.isBlank() || model.isBlank()) {
            throw IOException("AI API endpoint or model is not configured")
        }

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", "JiShiYong/${BuildConfig.VERSION_NAME}")
            .post(buildRequestBody(messages, temperature).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return parseContent(executeWithRetry(request))
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

    private suspend fun executeWithRetry(request: Request): String {
        var lastException: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val response = callFactory.newCall(request).awaitText()
                if (response.isSuccessful) {
                    return response.body
                }

                val exception = LlmHttpException(response.statusCode)
                if (!exception.isRetryable || attempt == MAX_ATTEMPTS - 1) {
                    throw exception
                }
                lastException = exception
            } catch (exception: IOException) {
                lastException = exception
                if (exception is LlmHttpException && !exception.isRetryable) {
                    throw exception
                }
                if (attempt == MAX_ATTEMPTS - 1) {
                    throw exception
                }
            }
            delay(RETRY_DELAY_MILLIS * (attempt + 1))
        }
        throw lastException ?: IOException("AI request failed")
    }

    private suspend fun Call.awaitText(): LlmHttpResponse {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel()
            }
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    val result = try {
                        response.use {
                            LlmHttpResponse(
                                statusCode = it.code,
                                isSuccessful = it.isSuccessful,
                                body = it.body?.string().orEmpty()
                            )
                        }
                    } catch (exception: IOException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                        return
                    }

                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
            })
        }
    }

    private data class LlmHttpResponse(
        val statusCode: Int,
        val isSuccessful: Boolean,
        val body: String
    )

    private class LlmHttpException(
        private val statusCode: Int
    ) : IOException("AI request failed: HTTP $statusCode (${safeHttpStatusCategory(statusCode)})") {
        val isRetryable: Boolean = statusCode == 408 || statusCode == 429 || statusCode in 500..599
    }

    private companion object {
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_DELAY_MILLIS = 350L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

private fun safeHttpStatusCategory(statusCode: Int): String {
    return when (statusCode) {
        400 -> "bad_request"
        401, 403 -> "authentication"
        404 -> "not_found"
        408 -> "timeout"
        429 -> "rate_limited"
        in 500..599 -> "server_error"
        else -> "http_error"
    }
}
