package com.jishiyong.update

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * 检查更新
     */
    fun checkForUpdate(showDialogWhenAvailable: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, downloadError = null)

            try {
                val context = getApplication<Application>()
                val updateInfo = UpdateChecker.checkForUpdate(context)

                Log.d(TAG, "Update check result: ${updateInfo?.versionName ?: "no update"}")

                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    updateInfo = updateInfo,
                    showDialog = updateInfo != null && showDialogWhenAvailable
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for update", e)
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
                val apkFile = File(dir, updateInfo.fileName)

                Log.d(TAG, "Downloading to: ${apkFile.absolutePath}")

                // 如果已下载过，直接安装
                if (apkFile.exists() && apkFile.length() > 0) {
                    Log.d(TAG, "APK already downloaded")
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        isDownloaded = true,
                        apkFile = apkFile
                    )
                    return@launch
                }

                // 下载 APK
                UpdateChecker.downloadFile(updateInfo.downloadUrl, apkFile) { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }

                Log.d(TAG, "Download complete")

                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    isDownloaded = true,
                    apkFile = apkFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
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

        try {
            UpdateChecker.installApk(context, apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            _uiState.value = _uiState.value.copy(
                downloadError = "安装失败: ${e.message}"
            )
        }
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
}
