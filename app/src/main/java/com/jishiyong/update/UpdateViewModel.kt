package com.jishiyong.update

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadError: String? = null,
    val isDownloaded: Boolean = false,
    val apkFile: File? = null,
    val showDialog: Boolean = false
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * 检查更新
     */
    fun checkForUpdate(showDialogWhenAvailable: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, downloadError = null)

            try {
                val currentVersionCode = getCurrentVersionCode()
                val updateInfo = UpdateChecker.checkForUpdate(currentVersionCode)

                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    updateInfo = updateInfo,
                    showDialog = updateInfo != null && showDialogWhenAvailable
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    downloadError = "检查更新失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 下载更新
     */
    fun downloadUpdate() {
        val updateInfo = _uiState.value.updateInfo ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0,
                downloadError = null
            )

            try {
                val context = getApplication<Application>()
                val dir = UpdateChecker.getDownloadDir(context)
                val fileName = UpdateChecker.getApkFileName(updateInfo.versionName)
                val apkFile = File(dir, fileName)

                // 如果已下载过，直接安装
                if (apkFile.exists() && apkFile.length() > 0) {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        isDownloaded = true,
                        apkFile = apkFile
                    )
                    return@launch
                }

                // 下载 APK
                downloadFile(updateInfo.downloadUrl, apkFile) { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }

                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    isDownloaded = true,
                    apkFile = apkFile
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadError = "下载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 安装更新
     */
    fun installUpdate() {
        val apkFile = _uiState.value.apkFile ?: return
        val context = getApplication<Application>()
        UpdateChecker.installApk(context, apkFile)
    }

    /**
     * 关闭对话框
     */
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(downloadError = null)
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val context = getApplication<Application>()
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } catch (e: Exception) {
            1
        }
    }

    private fun downloadFile(
        fileUrl: String,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) {
        val url = URL(fileUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "JiShiYong-Android")
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        val fileLength = connection.contentLength

        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    total += bytesRead

                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        onProgress(progress)
                    }
                }
            }
        }
    }
}
