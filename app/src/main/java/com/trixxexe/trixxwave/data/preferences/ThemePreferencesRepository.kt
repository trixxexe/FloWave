package com.trixxexe.trixxwave.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trixxwave_preferences")

data class ThemeConfig(
    val preset: String = "AMOLED Minimal",
    val mode: String = "AMOLED",
    val blurIntensity: Float = 0f,
    val glassOpacity: Float = 0.0f,
    val cornerRadiusDp: Int = 12,
    val animatedOrbsEnabled: Boolean = false,
    val accentColorHex: String = "#00F5D4",
    val primaryColorHex: String = "#000000",
    val customBgUri: String? = null,
    val contrastSafeMode: Boolean = true,
    val miniPlayerPosition: String = "Bottom",
    val miniPlayerShape: String = "Clean Rectangle",
    val visualizerStyle: String = "Spectrum",
    val crossfadeDurationSec: Int = 3,
    val hapticIntensity: Float = 0.8f,
    val widgetStyle: String = "Standard Card",
    val widgetOpacity: Float = 0.85f,
    val widgetShowSkip: Boolean = true,
    val widgetShowAlbumArt: Boolean = true,
    val widgetShowWaveform: Boolean = true,
    val widgetShowFavorite: Boolean = true,
    val dynamicIslandEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val gaplessSilenceThresholdDb: Float = -45f,
    val autoScanGaplessOnImport: Boolean = true,
    val trackTransitionAnimation: String = "Crossfade",
    val staticBlurMode: Boolean = false,
    val autoResumeEnabled: Boolean = true,
    val userName: String = "Main Listener",
    val userAvatar: String = "Male 1",
    val aiProvider: String = "Groq",
    val aiApiKey: String = "",
    val aiModel: String = "llama-3.3-70b-versatile",
    val pureAmoledBlack: Boolean = true,
    val streamingQuality: String = "High (256k AAC)",
    val downloadLocation: String = "Internal Audio",
    val autoLyrics: Boolean = true,
    val cornerStyle: String = "Balanced (12dp)"
)

class ThemePreferencesRepository(private val context: Context) {

    private object PreferenceKeys {
        val PRESET = stringPreferencesKey("theme_preset")
        val MODE = stringPreferencesKey("theme_mode")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val ANIMATED_ORBS = booleanPreferencesKey("animated_orbs")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color")
        val CUSTOM_BG_URI = stringPreferencesKey("custom_bg_uri")
        val CONTRAST_SAFE = booleanPreferencesKey("contrast_safe")
        val MINI_PLAYER_POSITION = stringPreferencesKey("mini_player_position")
        val MINI_PLAYER_SHAPE = stringPreferencesKey("mini_player_shape")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val HAPTIC_INTENSITY = floatPreferencesKey("haptic_intensity")
        val FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
        val WIDGET_STYLE = stringPreferencesKey("widget_style")
        val WIDGET_OPACITY = floatPreferencesKey("widget_opacity")
        val WIDGET_SHOW_SKIP = booleanPreferencesKey("widget_show_skip")
        val WIDGET_SHOW_ALBUM_ART = booleanPreferencesKey("widget_show_album_art")
        val WIDGET_SHOW_WAVEFORM = booleanPreferencesKey("widget_show_waveform")
        val WIDGET_SHOW_FAVORITE = booleanPreferencesKey("widget_show_favorite")
        val DYNAMIC_ISLAND_ENABLED = booleanPreferencesKey("dynamic_island_enabled")
        val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
        val GAPLESS_SILENCE_THRESHOLD_DB = floatPreferencesKey("gapless_silence_threshold_db")
        val AUTO_SCAN_GAPLESS_ON_IMPORT = booleanPreferencesKey("auto_scan_gapless_on_import")
        val TRACK_TRANSITION_ANIMATION = stringPreferencesKey("track_transition_animation")
        val STATIC_BLUR_MODE = booleanPreferencesKey("static_blur_mode")
        val AUTO_RESUME_ENABLED = booleanPreferencesKey("auto_resume_enabled")
        val LAST_PLAYED_SONG_ID = longPreferencesKey("last_played_song_id")
        val LAST_SEEK_POSITION_MS = longPreferencesKey("last_seek_position_ms")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val PURE_AMOLED_BLACK = booleanPreferencesKey("pure_amoled_black")
        val CORNER_STYLE = stringPreferencesKey("corner_style")
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
    }

    val isFirstRunFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !(prefs[PreferenceKeys.FIRST_RUN_COMPLETED] ?: false)
    }

    suspend fun setFirstRunCompleted(completed: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.FIRST_RUN_COMPLETED] = completed
        }
    }

    val themeConfigFlow: Flow<ThemeConfig> = context.dataStore.data.map { prefs ->
        ThemeConfig(
            preset = prefs[PreferenceKeys.PRESET] ?: "AMOLED Minimal",
            mode = prefs[PreferenceKeys.MODE] ?: "AMOLED",
            blurIntensity = prefs[PreferenceKeys.BLUR_INTENSITY] ?: 0f,
            glassOpacity = prefs[PreferenceKeys.GLASS_OPACITY] ?: 0.0f,
            cornerRadiusDp = prefs[PreferenceKeys.CORNER_RADIUS] ?: 12,
            animatedOrbsEnabled = prefs[PreferenceKeys.ANIMATED_ORBS] ?: false,
            accentColorHex = prefs[PreferenceKeys.ACCENT_COLOR] ?: "#00F5D4",
            primaryColorHex = prefs[PreferenceKeys.PRIMARY_COLOR] ?: "#000000",
            customBgUri = prefs[PreferenceKeys.CUSTOM_BG_URI],
            contrastSafeMode = prefs[PreferenceKeys.CONTRAST_SAFE] ?: true,
            miniPlayerPosition = prefs[PreferenceKeys.MINI_PLAYER_POSITION] ?: "Bottom",
            miniPlayerShape = prefs[PreferenceKeys.MINI_PLAYER_SHAPE] ?: "Clean Rectangle",
            visualizerStyle = prefs[PreferenceKeys.VISUALIZER_STYLE] ?: "Spectrum",
            crossfadeDurationSec = prefs[PreferenceKeys.CROSSFADE_DURATION] ?: 3,
            hapticIntensity = prefs[PreferenceKeys.HAPTIC_INTENSITY] ?: 0.8f,
            widgetStyle = prefs[PreferenceKeys.WIDGET_STYLE] ?: "Standard Card",
            widgetOpacity = prefs[PreferenceKeys.WIDGET_OPACITY] ?: 0.85f,
            widgetShowSkip = prefs[PreferenceKeys.WIDGET_SHOW_SKIP] ?: true,
            widgetShowAlbumArt = prefs[PreferenceKeys.WIDGET_SHOW_ALBUM_ART] ?: true,
            widgetShowWaveform = prefs[PreferenceKeys.WIDGET_SHOW_WAVEFORM] ?: true,
            widgetShowFavorite = prefs[PreferenceKeys.WIDGET_SHOW_FAVORITE] ?: true,
            dynamicIslandEnabled = prefs[PreferenceKeys.DYNAMIC_ISLAND_ENABLED] ?: false,
            gaplessEnabled = prefs[PreferenceKeys.GAPLESS_ENABLED] ?: true,
            gaplessSilenceThresholdDb = prefs[PreferenceKeys.GAPLESS_SILENCE_THRESHOLD_DB] ?: -45f,
            autoScanGaplessOnImport = prefs[PreferenceKeys.AUTO_SCAN_GAPLESS_ON_IMPORT] ?: true,
            trackTransitionAnimation = prefs[PreferenceKeys.TRACK_TRANSITION_ANIMATION] ?: "Crossfade",
            staticBlurMode = prefs[PreferenceKeys.STATIC_BLUR_MODE] ?: false,
            autoResumeEnabled = prefs[PreferenceKeys.AUTO_RESUME_ENABLED] ?: true,
            userName = prefs[PreferenceKeys.USER_NAME] ?: "Main Listener",
            userAvatar = prefs[PreferenceKeys.USER_AVATAR] ?: "Male 1",
            aiProvider = prefs[PreferenceKeys.AI_PROVIDER] ?: "Groq",
            aiApiKey = prefs[PreferenceKeys.AI_API_KEY] ?: "",
            aiModel = prefs[PreferenceKeys.AI_MODEL] ?: "llama-3.3-70b-versatile",
            pureAmoledBlack = prefs[PreferenceKeys.PURE_AMOLED_BLACK] ?: true,
            cornerStyle = prefs[PreferenceKeys.CORNER_STYLE] ?: "Balanced (12dp)",
            streamingQuality = prefs[PreferenceKeys.STREAMING_QUALITY] ?: "High (256k AAC)"
        )
    }

    val lastPlaybackStateFlow: Flow<Pair<Long?, Long>> = context.dataStore.data.map { prefs ->
        Pair(
            prefs[PreferenceKeys.LAST_PLAYED_SONG_ID],
            prefs[PreferenceKeys.LAST_SEEK_POSITION_MS] ?: 0L
        )
    }

    suspend fun setAutoResumeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_RESUME_ENABLED] = enabled
        }
    }

    suspend fun saveLastPlaybackState(songId: Long, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_PLAYED_SONG_ID] = songId
            prefs[PreferenceKeys.LAST_SEEK_POSITION_MS] = positionMs
        }
    }

    suspend fun setStaticBlurMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.STATIC_BLUR_MODE] = enabled
        }
    }

    suspend fun setTrackTransitionAnimation(animation: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.TRACK_TRANSITION_ANIMATION] = animation
        }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.GAPLESS_ENABLED] = enabled
        }
    }

    suspend fun setGaplessSilenceThresholdDb(thresholdDb: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.GAPLESS_SILENCE_THRESHOLD_DB] = thresholdDb
        }
    }

    suspend fun setAutoScanGaplessOnImport(autoScan: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_SCAN_GAPLESS_ON_IMPORT] = autoScan
        }
    }

    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.DYNAMIC_ISLAND_ENABLED] = enabled
        }
    }

    suspend fun updateThemePreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.PRESET] = preset
            when (preset) {
                "Sleek Interface" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#F27D26"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#050505"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.08f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 20f
                    prefs[PreferenceKeys.MODE] = "Dark"
                }
                "AMOLED Dark" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#00F5D4"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#000000"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.12f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 10f
                    prefs[PreferenceKeys.MODE] = "Dark"
                }
                "AMOLED Light" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#2563EB"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#FFFFFF"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.90f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 10f
                    prefs[PreferenceKeys.MODE] = "Light"
                }
                "Liquid Obsidian" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#00F5D4"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#07090E"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.25f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 16f
                    prefs[PreferenceKeys.MODE] = "Dark"
                }
                "Cyber Pink" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#FF007A"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#07030A"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.25f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 18f
                    prefs[PreferenceKeys.MODE] = "Dark"
                }
                "Emerald Wave" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#10B981"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#030A07"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.20f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 22f
                    prefs[PreferenceKeys.MODE] = "Dark"
                }
                "Sunset Gold" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#F59E0B"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#0A0503"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.22f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 18f
                    prefs[PreferenceKeys.MODE] = "Dark"
                }
                "Aether White" -> {
                    prefs[PreferenceKeys.ACCENT_COLOR] = "#6366F1"
                    prefs[PreferenceKeys.PRIMARY_COLOR] = "#F1F5F9"
                    prefs[PreferenceKeys.GLASS_OPACITY] = 0.80f
                    prefs[PreferenceKeys.BLUR_INTENSITY] = 15f
                    prefs[PreferenceKeys.MODE] = "Light"
                }
            }
        }
    }

    suspend fun setPrimaryColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.PRIMARY_COLOR] = hex
            prefs[PreferenceKeys.PRESET] = "Custom Aesthetics"
        }
    }

    suspend fun setCustomBgUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(PreferenceKeys.CUSTOM_BG_URI)
            } else {
                prefs[PreferenceKeys.CUSTOM_BG_URI] = uri
            }
        }
    }

    suspend fun setContrastSafeMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.CONTRAST_SAFE] = enabled
        }
    }

    suspend fun setAnimatedOrbs(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.ANIMATED_ORBS] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.MODE] = mode
        }
    }

    suspend fun setBlurIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.BLUR_INTENSITY] = intensity
        }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.ACCENT_COLOR] = hex
        }
    }

    suspend fun setVisualizerStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.VISUALIZER_STYLE] = style
        }
    }

    suspend fun setCrossfadeDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.CROSSFADE_DURATION] = seconds
        }
    }

    suspend fun setMiniPlayerShape(shape: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.MINI_PLAYER_SHAPE] = shape
        }
    }

    suspend fun setWidgetStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.WIDGET_STYLE] = style
        }
    }

    suspend fun setWidgetOpacity(opacity: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.WIDGET_OPACITY] = opacity
        }
    }

    suspend fun setWidgetShowSkip(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.WIDGET_SHOW_SKIP] = show
        }
    }

    suspend fun setWidgetShowAlbumArt(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.WIDGET_SHOW_ALBUM_ART] = show
        }
    }

    suspend fun setWidgetShowWaveform(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.WIDGET_SHOW_WAVEFORM] = show
        }
    }

    suspend fun setUserProfile(name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.USER_NAME] = name
            prefs[PreferenceKeys.USER_AVATAR] = avatar
        }
    }

    suspend fun setPureAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.PURE_AMOLED_BLACK] = enabled
            if (enabled) {
                prefs[PreferenceKeys.MODE] = "AMOLED"
            } else {
                prefs[PreferenceKeys.MODE] = "Dark"
            }
        }
    }

    suspend fun setAiConfig(provider: String, apiKey: String, model: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.AI_PROVIDER] = provider
            prefs[PreferenceKeys.AI_API_KEY] = apiKey
            prefs[PreferenceKeys.AI_MODEL] = model
        }
    }

    suspend fun setWidgetShowFavorite(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.WIDGET_SHOW_FAVORITE] = show
        }
    }

    suspend fun setCornerStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.CORNER_STYLE] = style
        }
    }

    suspend fun setStreamingQuality(quality: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.STREAMING_QUALITY] = quality
        }
    }
}
