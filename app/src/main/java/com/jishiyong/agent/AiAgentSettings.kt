package com.jishiyong.agent

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AiAgentConfiguration(
    val baseUrl: String,
    val model: String,
    val apiKey: String
) {
    val isComplete: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

class AiAgentSettings(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val securePreferences = appContext.getSharedPreferences(
        SECURE_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val apiKeyStore = KeystoreBackedStringStore(securePreferences)

    fun loadConfiguration(
        defaultBaseUrl: String,
        defaultModel: String
    ): AiAgentConfiguration {
        migratePlainApiKeyIfNeeded()
        return AiAgentConfiguration(
            baseUrl = preferences.getString(KEY_BASE_URL, defaultBaseUrl).orEmpty().trim(),
            model = preferences.getString(KEY_MODEL, defaultModel).orEmpty().trim(),
            apiKey = apiKeyStore.get(KEY_API_KEY).orEmpty().trim()
        )
    }

    fun saveApiKey(apiKey: String) {
        try {
            if (apiKey.isBlank()) {
                apiKeyStore.remove(KEY_API_KEY)
            } else {
                apiKeyStore.put(KEY_API_KEY, apiKey.trim())
            }
        } catch (_: Exception) {
            apiKeyStore.remove(KEY_API_KEY)
        } finally {
            preferences.edit().remove(KEY_API_KEY).apply()
        }
    }

    private fun migratePlainApiKeyIfNeeded() {
        val plainApiKey = preferences.getString(KEY_API_KEY, null)?.trim().orEmpty()
        if (plainApiKey.isBlank()) return

        try {
            apiKeyStore.put(KEY_API_KEY, plainApiKey)
        } catch (_: Exception) {
            apiKeyStore.remove(KEY_API_KEY)
        } finally {
            preferences.edit().remove(KEY_API_KEY).apply()
        }
    }

    companion object {
        const val PREFS_NAME = "ai_agent_settings"
        const val SECURE_PREFS_NAME = "ai_agent_secure_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_API_KEY = "api_key"
    }
}

private class KeystoreBackedStringStore(
    private val preferences: SharedPreferences
) {
    fun get(key: String): String? {
        val iv = preferences.getString("$key.iv", null) ?: return null
        val cipherText = preferences.getString("$key.ciphertext", null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv.decodeBase64())
            )
            String(cipher.doFinal(cipherText.decodeBase64()), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun put(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("$key.iv", cipher.iv.encodeBase64())
            .putString("$key.ciphertext", cipherText.encodeBase64())
            .apply()
    }

    fun remove(key: String) {
        preferences.edit()
            .remove("$key.iv")
            .remove("$key.ciphertext")
            .apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun ByteArray.encodeBase64(): String {
        return Base64.encodeToString(this, Base64.NO_WRAP)
    }

    private fun String.decodeBase64(): ByteArray {
        return Base64.decode(this, Base64.NO_WRAP)
    }

    private companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "jishiyong_ai_agent_api_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
