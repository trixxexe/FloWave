package com.trixxexe.trixxwave.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

    private val TAG = "EncryptedKeyManager"
    private val PREF_NAME = "trixxwave_secure_ai_prefs"

    private val prefs: SharedPreferences by lazy {
        val appContext = context.applicationContext
        try {
            createEncryptedPrefs(appContext)
        } catch (e1: Throwable) {
            Log.e(TAG, "EncryptedSharedPreferences init failed: ${e1.message}. Attempting recovery...", e1)
            try {
                // Recovery step: delete corrupted store and recreate
                appContext.deleteSharedPreferences(PREF_NAME)
                createEncryptedPrefs(appContext)
            } catch (e2: Throwable) {
                Log.e(TAG, "EncryptedSharedPreferences recovery failed: ${e2.message}. Falling back to in-memory store.", e2)
                InMemorySharedPreferences()
            }
        }
    }

    private fun createEncryptedPrefs(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
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

/**
 * In-memory non-persisted SharedPreferences fallback to ensure API keys and secrets
 * are never written to unencrypted disk storage if Android KeyStore initialization fails.
 */
private class InMemorySharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? {
        return (data[key] as? String) ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return (data[key] as? MutableSet<String>) ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return (data[key] as? Int) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return (data[key] as? Long) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return (data[key] as? Float) ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return (data[key] as? Boolean) ?: defValue
    }

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.add(it) }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.remove(it) }
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()
        private val removedKeys = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) {
                tempMap[key] = value
                removedKeys.remove(key)
            }
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) {
                tempMap[key] = values
                removedKeys.remove(key)
            }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) {
                tempMap[key] = value
                removedKeys.remove(key)
            }
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) {
                tempMap[key] = value
                removedKeys.remove(key)
            }
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) {
                tempMap[key] = value
                removedKeys.remove(key)
            }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) {
                tempMap[key] = value
                removedKeys.remove(key)
            }
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                removedKeys.add(key)
                tempMap.remove(key)
            }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            tempMap.clear()
            removedKeys.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            synchronized(data) {
                if (clearAll) data.clear()
                for (key in removedKeys) data.remove(key)
                data.putAll(tempMap)
            }
            for (listener in listeners) {
                for (key in tempMap.keys) {
                    listener.onSharedPreferenceChanged(this@InMemorySharedPreferences, key)
                }
            }
        }
    }
}
