package com.trixxexe.trixxwave.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AiConfig(
    val provider: String = "Groq", // Groq / NVIDIA NIM / Custom
    val apiKey: String = "",
    val modelName: String = "llama-3.3-70b-versatile",
    val customEndpoint: String = "https://api.groq.com/openai/v1/",
    val autoTaggingEnabled: Boolean = true,
    val smartMixesEnabled: Boolean = true,
    val trackInsightsEnabled: Boolean = true
)

class EncryptedKeyManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                "trixxwave_secure_ai_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            // Fallback for emulator or older crypto provider
            try {
                context.getSharedPreferences("trixxwave_secure_ai_prefs_fallback", Context.MODE_PRIVATE)
            } catch (e2: Throwable) {
                context.getSharedPreferences("trixxwave_prefs_basic", Context.MODE_PRIVATE)
            }
        }
    }

    fun getAiConfig(): AiConfig {
        val provider = prefs.getString("ai_provider", "Groq") ?: "Groq"
        val defaultModel = when (provider) {
            "NVIDIA NIM" -> "meta/llama-3.1-70b-instruct"
            "Custom" -> "gpt-4o-mini"
            else -> "llama-3.3-70b-versatile"
        }
        val defaultEndpoint = when (provider) {
            "NVIDIA NIM" -> "https://integrate.api.nvidia.com/v1/"
            "Custom" -> "https://api.openai.com/v1/"
            else -> "https://api.groq.com/openai/v1/"
        }

        return AiConfig(
            provider = provider,
            apiKey = prefs.getString("ai_api_key", "") ?: "",
            modelName = prefs.getString("ai_model_name", defaultModel) ?: defaultModel,
            customEndpoint = prefs.getString("ai_custom_endpoint", defaultEndpoint) ?: defaultEndpoint,
            autoTaggingEnabled = prefs.getBoolean("ai_auto_tagging", true),
            smartMixesEnabled = prefs.getBoolean("ai_smart_mixes", true),
            trackInsightsEnabled = prefs.getBoolean("ai_track_insights", true)
        )
    }

    fun saveAiConfig(config: AiConfig) {
        prefs.edit()
            .putString("ai_provider", config.provider)
            .putString("ai_api_key", config.apiKey)
            .putString("ai_model_name", config.modelName)
            .putString("ai_custom_endpoint", config.customEndpoint)
            .putBoolean("ai_auto_tagging", config.autoTaggingEnabled)
            .putBoolean("ai_smart_mixes", config.smartMixesEnabled)
            .putBoolean("ai_track_insights", config.trackInsightsEnabled)
            .apply()
    }
}
