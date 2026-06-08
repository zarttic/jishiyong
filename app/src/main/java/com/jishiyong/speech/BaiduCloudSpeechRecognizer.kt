package com.jishiyong.speech

import android.content.Context

class BaiduCloudSpeechRecognizer(
    context: Context,
    private val recorder: PcmSpeechRecorder = PcmSpeechRecorder(),
    private val client: BaiduAsrClient = BaiduAsrClient()
) {
    private val appContext = context.applicationContext

    fun isConfigured(): Boolean {
        return BaiduAsrSettings.load(appContext).isComplete
    }

    suspend fun recognizeOnce(
        onRecordingFinished: () -> Unit = {}
    ): String {
        val configuration = BaiduAsrSettings.load(appContext)
        if (!configuration.isComplete) {
            throw IllegalStateException("百度语音识别未配置")
        }
        val audio = recorder.recordSpeechPcm(appContext)
        onRecordingFinished()
        return client.recognizePcm(audio, configuration)
    }
}
