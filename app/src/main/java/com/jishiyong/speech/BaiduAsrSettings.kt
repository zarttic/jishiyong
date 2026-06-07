package com.jishiyong.speech

import android.content.Context
import android.provider.Settings
import android.util.Base64
import com.jishiyong.BuildConfig

object BaiduAsrSettings {
    fun load(context: Context): BaiduAsrConfiguration {
        return BaiduAsrConfiguration(
            appId = BuildConfig.BAIDU_ASR_APP_ID,
            apiKey = decodeBuildSecret(BuildConfig.BAIDU_ASR_API_KEY_OBFUSCATED),
            secretKey = decodeBuildSecret(BuildConfig.BAIDU_ASR_SECRET_KEY_OBFUSCATED),
            cuid = deviceCuid(context),
            devPid = BuildConfig.BAIDU_ASR_DEV_PID
        )
    }

    private fun decodeBuildSecret(value: String): String {
        if (value.isBlank()) return ""
        val decoded = try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: Exception) {
            return ""
        }
        val plain = decoded
            .mapIndexed { index, byte ->
                (byte.toInt() xor 0x5A xor ((index * 31 + 17) and 0xFF)).toByte()
            }
            .toByteArray()
        return plain.toString(Charsets.UTF_8)
    }

    private fun deviceCuid(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return "jishiyong_${androidId.orEmpty().ifBlank { "android" }}".take(60)
    }
}
