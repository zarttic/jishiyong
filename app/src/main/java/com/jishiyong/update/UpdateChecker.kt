package com.jishiyong.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Release 更新检查器
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

object UpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/zarttic/jishiyong/releases/latest"

    /**
     * 检查是否有新版本
     * @param currentVersionCode 当前版本号
     * @return UpdateInfo 如果有更新，null 如果已是最新
     */
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "JiShiYong-Android")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                parseReleaseResponse(response, currentVersionCode)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseReleaseResponse(json: String, currentVersionCode: Int): UpdateInfo? {
        try {
            val jsonObject = org.json.JSONObject(json)
            val tagName = jsonObject.getString("tag_name") // 例如 "v2.0.0"
            val body = jsonObject.optString("body", "")
            val publishedAt = jsonObject.getString("published_at")

            // 解析版本号
            val versionName = tagName.removePrefix("v")
            val versionCode = parseVersionCode(versionName)

            // 查找 APK 下载链接
            val assets = jsonObject.getJSONArray("assets")
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (downloadUrl.isEmpty()) return null

            // 比较版本
            return if (versionCode > currentVersionCode) {
                UpdateInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = body,
                    publishedAt = publishedAt
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 将版本号字符串转换为数字进行比较
     * 例如 "2.0.0" -> 20000, "1.1.0" -> 10100
     */
    private fun parseVersionCode(version: String): Int {
        val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
        return when {
            parts.size >= 3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
            parts.size == 2 -> parts[0] * 10000 + parts[1] * 100
            parts.size == 1 -> parts[0] * 10000
            else -> 0
        }
    }

    /**
     * 获取 APK 文件名
     */
    fun getApkFileName(versionName: String): String {
        return "jishiyong-v$versionName.apk"
    }

    /**
     * 获取下载目录
     */
    fun getDownloadDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 安装 APK
     */
    fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
