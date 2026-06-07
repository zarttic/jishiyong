package com.jishiyong.speech

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class BaiduAsrClient(
    private val client: OkHttpClient = defaultClient
) {
    private var cachedToken: BaiduAccessToken? = null

    suspend fun recognizePcm(
        pcmAudio: ByteArray,
        configuration: BaiduAsrConfiguration
    ): String {
        if (!configuration.isComplete) {
            throw IOException("Baidu ASR is not configured")
        }
        if (pcmAudio.isEmpty()) {
            throw IOException("Audio is empty")
        }

        val token = accessToken(configuration)
        val body = JSONObject()
            .put("format", "pcm")
            .put("rate", SAMPLE_RATE)
            .put("dev_pid", configuration.devPid)
            .put("channel", 1)
            .put("cuid", configuration.cuid)
            .put("token", token)
            .put("len", pcmAudio.size)
            .put("speech", Base64.encodeToString(pcmAudio, Base64.NO_WRAP))

        val request = Request.Builder()
            .url(ASR_URL)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val responseJson = executeJson(request)
        val errorNumber = responseJson.optInt("err_no", -1)
        if (errorNumber != 0) {
            val message = responseJson.optString("err_msg").ifBlank { "Baidu ASR failed: $errorNumber" }
            throw IOException(message)
        }
        return responseJson.optJSONArray("result")
            ?.optString(0)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("Baidu ASR returned empty result")
    }

    private suspend fun accessToken(configuration: BaiduAsrConfiguration): String {
        val cached = cachedToken
        if (cached != null && !cached.isExpired()) {
            return cached.value
        }

        val url = TOKEN_URL.toHttpUrl().newBuilder()
            .addQueryParameter("grant_type", "client_credentials")
            .addQueryParameter("client_id", configuration.apiKey)
            .addQueryParameter("client_secret", configuration.secretKey)
            .build()

        val request = Request.Builder().url(url).get().build()
        val json = executeJson(request)
        val token = json.optString("access_token")
            .takeIf { it.isNotBlank() }
            ?: throw IOException(json.optString("error_description").ifBlank { "Failed to get Baidu access token" })
        val expiresInSeconds = json.optLong("expires_in", DEFAULT_TOKEN_TTL_SECONDS)
        cachedToken = BaiduAccessToken(
            value = token,
            expiresAtMillis = System.currentTimeMillis() + expiresInSeconds * 1000L
        )
        return token
    }

    private suspend fun executeJson(request: Request): JSONObject {
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: $body")
                }
                try {
                    JSONObject(body)
                } catch (exception: Exception) {
                    throw IOException("Invalid JSON response", exception)
                }
            }
        }
    }

    private data class BaiduAccessToken(
        val value: String,
        val expiresAtMillis: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() + TOKEN_REFRESH_SKEW_MILLIS >= expiresAtMillis
        }
    }

    private companion object {
        private const val TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
        private const val ASR_URL = "https://vop.baidu.com/server_api"
        private const val SAMPLE_RATE = 16000
        private const val DEFAULT_TOKEN_TTL_SECONDS = 2_592_000L
        private const val TOKEN_REFRESH_SKEW_MILLIS = 5 * 60 * 1000L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
