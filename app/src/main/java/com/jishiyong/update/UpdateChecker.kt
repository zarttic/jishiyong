package com.jishiyong.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
    val publishedAt: String,
    val fileName: String
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/zarttic/jishiyong/releases/latest"

    /**
     * 获取当前版本信息
     */
    fun getCurrentVersionInfo(context: Context): Pair<String, Int> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionName = packageInfo.versionName ?: "1.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version info", e)
            Pair("1.0.0", 1)
        }
    }

    /**
     * 检查是否有新版本
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val (_, currentVersionCode) = getCurrentVersionInfo(context)
            Log.d(TAG, "Current version code: $currentVersionCode")

            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "JiShiYong-Android")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = inputStream.bufferedReader().readText()
                parseReleaseResponse(response, currentVersionCode)
            } else {
                Log.e(TAG, "API request failed: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Check update failed", e)
            null
        }
    }

    private fun parseReleaseResponse(json: String, currentVersionCode: Int): UpdateInfo? {
        try {
            val jsonObject = JSONObject(json)
            val tagName = jsonObject.getString("tag_name")
            val body = jsonObject.optString("body", "")
            val publishedAt = jsonObject.getString("published_at")

            val versionName = tagName.removePrefix("v")
            val versionCode = parseVersionCode(versionName)

            Log.d(TAG, "Remote: $versionName ($versionCode), Local: $currentVersionCode")

            val assets = jsonObject.getJSONArray("assets")
            var downloadUrl = ""
            var fileName = ""

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    fileName = name
                    Log.d(TAG, "Found APK: $name")
                    break
                }
            }

            if (downloadUrl.isEmpty()) return null

            return if (versionCode > currentVersionCode) {
                UpdateInfo(versionName, versionCode, downloadUrl, body, publishedAt, fileName)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed", e)
            return null
        }
    }

    private fun parseVersionCode(version: String): Int {
        val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
        return when {
            parts.size >= 3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
            parts.size == 2 -> parts[0] * 10000 + parts[1] * 100
            parts.size == 1 -> parts[0] * 10000
            else -> 0
        }
    }

    fun getDownloadDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 下载文件 - 支持重定向
     */
    fun downloadFile(fileUrl: String, outputFile: File, onProgress: (Int) -> Unit) {
        Log.d(TAG, "Downloading: $fileUrl")

        var currentUrl = fileUrl
        var connection: HttpURLConnection? = null

        try {
            // 处理重定向
            for (i in 0..10) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "JiShiYong-Android")
                    setRequestProperty("Accept", "application/octet-stream")
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = false
                }

                val code = connection.responseCode
                Log.d(TAG, "Response $code for: $currentUrl")

                // 处理重定向
                if (code in 301..308) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        currentUrl = location
                        Log.d(TAG, "Redirect to: $currentUrl")
                        connection.disconnect()
                        continue
                    }
                }

                // 成功响应
                if (code == HttpURLConnection.HTTP_OK) {
                    val fileLength = connection.contentLength
                    Log.d(TAG, "File size: $fileLength bytes")

                    connection.inputStream.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var total = 0L
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                total += bytesRead
                                if (fileLength > 0) {
                                    onProgress((total * 100 / fileLength).toInt())
                                }
                            }

                            Log.d(TAG, "Downloaded: $total bytes")
                        }
                    }

                    // 验证文件
                    if (outputFile.length() > 0) {
                        Log.d(TAG, "Download success: ${outputFile.absolutePath}")
                        return
                    } else {
                        throw Exception("Downloaded file is empty")
                    }
                } else {
                    throw Exception("HTTP error: $code")
                }
            }

            throw Exception("Too many redirects")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            // 清理失败的文件
            if (outputFile.exists()) outputFile.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 安装 APK
     */
    fun installApk(context: Context, apkFile: File) {
        Log.d(TAG, "Installing: ${apkFile.absolutePath}")

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
