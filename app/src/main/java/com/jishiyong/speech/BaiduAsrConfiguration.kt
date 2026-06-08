package com.jishiyong.speech

data class BaiduAsrConfiguration(
    val appId: String,
    val apiKey: String,
    val secretKey: String,
    val cuid: String,
    val devPid: Int
) {
    val isComplete: Boolean
        get() = appId.isNotBlank() && apiKey.isNotBlank() && secretKey.isNotBlank() && cuid.isNotBlank()
}
