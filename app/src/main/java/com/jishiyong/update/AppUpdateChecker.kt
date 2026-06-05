package com.jishiyong.update

import com.jishiyong.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val releaseUrl: String,
    val downloadUrl: String,
    val releaseName: String
)

class AppUpdateChecker {

    suspend fun checkLatestRelease(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val connection = URL(githubLatestReleaseApi()).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "JiShiYong/${BuildConfig.VERSION_NAME}")

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext null
            }
            if (responseCode !in 200..299) {
                throw IOException("GitHub release request failed: HTTP $responseCode")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(response)
            val tagName = release.optString("tag_name")
            val latestVersionName = normalizeVersionName(tagName)

            if (!isNewerVersion(latestVersionName, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            val releaseUrl = release.optString("html_url")
            val assets = release.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            AppUpdateInfo(
                versionName = latestVersionName,
                releaseUrl = releaseUrl,
                downloadUrl = apkUrl?.takeIf { it.isNotBlank() } ?: releaseUrl,
                releaseName = release.optString("name").ifBlank { "及时用 $tagName" }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun githubLatestReleaseApi(): String {
        return "https://api.github.com/repos/${BuildConfig.GITHUB_REPOSITORY_NAME}/releases/latest"
    }

    private fun normalizeVersionName(value: String): String {
        return versionRegex.find(value)?.value ?: value.removePrefix("v").trim()
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = versionRegex.find(latest)?.value.orEmpty().split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = versionRegex.find(current)?.value.orEmpty().split(".").mapNotNull { it.toIntOrNull() }
        val maxSize = maxOf(latestParts.size, currentParts.size)

        for (index in 0 until maxSize) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) {
                return latestPart > currentPart
            }
        }
        return false
    }

    private companion object {
        val versionRegex = Regex("""\d+(?:\.\d+)*""")
    }
}

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    object UpToDate : UpdateCheckState()
    data class Available(val update: AppUpdateInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}
