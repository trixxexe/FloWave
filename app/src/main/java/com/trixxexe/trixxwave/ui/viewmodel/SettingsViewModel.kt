package com.trixxexe.trixxwave.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trixxexe.trixxwave.TrixxWaveApp
import com.trixxexe.trixxwave.data.api.AiRepository
import com.trixxexe.trixxwave.data.api.OpenAiService
import com.trixxexe.trixxwave.data.db.Profile
import com.trixxexe.trixxwave.data.preferences.AiConfig
import com.trixxexe.trixxwave.data.preferences.EncryptedKeyManager
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.media.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TrixxWaveApp
    private val keyManager = EncryptedKeyManager(app)
    private val themeRepo = app.themePreferences
    private val profileDao = app.database.profileDao()
    private val aiRepository = AiRepository(OpenAiService.create())

    private val _aiConfig = MutableStateFlow(keyManager.getAiConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus.asStateFlow()

    private val _fetchedModels = MutableStateFlow<List<String>>(emptyList())
    val fetchedModels: StateFlow<List<String>> = _fetchedModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    private val _fetchModelsStatus = MutableStateFlow<String?>(null)
    val fetchModelsStatus: StateFlow<String?> = _fetchModelsStatus.asStateFlow()

    val profiles: StateFlow<List<Profile>> = profileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isFirstRun: StateFlow<Boolean> = themeRepo.isFirstRunFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveAiConfig(config: AiConfig) {
        keyManager.saveAiConfig(config)
        _aiConfig.value = config
    }

    fun fetchAvailableModels(provider: String, apiKey: String, customEndpoint: String) {
        val tempConfig = _aiConfig.value.copy(
            provider = provider,
            apiKey = apiKey,
            customEndpoint = customEndpoint
        )
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingModels.value = true
            _fetchModelsStatus.value = "Connecting to $provider models endpoint..."
            val result = aiRepository.fetchAvailableModels(tempConfig)
            result.onSuccess { models ->
                _fetchedModels.value = models
                _isFetchingModels.value = false
                _fetchModelsStatus.value = "Fetched ${models.size} models successfully!"
            }.onFailure { err ->
                _isFetchingModels.value = false
                _fetchModelsStatus.value = err.localizedMessage ?: "Failed to fetch models"
            }
        }
    }

    fun setFirstRunCompleted(completed: Boolean) {
        viewModelScope.launch {
            themeRepo.setFirstRunCompleted(completed)
        }
    }

    fun completeOnboarding(name: String, avatarUri: String?, themePreset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            themeRepo.updateThemePreset(themePreset)
            val existing = profileDao.getActiveProfileSync()
            if (existing != null) {
                val updated = existing.copy(name = name, avatarUri = avatarUri, themePreset = themePreset)
                profileDao.insertProfile(updated)
            } else {
                val profile = Profile(name = name, avatarUri = avatarUri, themePreset = themePreset, isActive = true)
                val id = profileDao.insertProfile(profile)
                profileDao.setActiveProfile(id)
            }
            themeRepo.setFirstRunCompleted(true)
        }
    }

    fun testAiConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            _testStatus.value = "Testing connection..."
            val result = aiRepository.testConnection(_aiConfig.value)
            result.onSuccess { msg ->
                _testStatus.value = "Success: $msg"
            }.onFailure { err ->
                _testStatus.value = "Error: ${err.localizedMessage}"
            }
        }
    }

    fun updateThemePreset(preset: String) {
        viewModelScope.launch {
            themeRepo.updateThemePreset(preset)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            themeRepo.setThemeMode(mode)
        }
    }

    fun setBlurIntensity(intensity: Float) {
        viewModelScope.launch {
            themeRepo.setBlurIntensity(intensity)
        }
    }

    fun setPrimaryColor(hex: String) {
        viewModelScope.launch {
            themeRepo.setPrimaryColor(hex)
        }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch {
            themeRepo.setAccentColor(hex)
        }
    }

    fun setCustomBgUri(uri: String?) {
        viewModelScope.launch {
            themeRepo.setCustomBgUri(uri)
        }
    }

    fun setContrastSafeMode(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setContrastSafeMode(enabled)
        }
    }

    fun setVisualizerStyle(style: String) {
        viewModelScope.launch {
            themeRepo.setVisualizerStyle(style)
        }
    }

    fun setMiniPlayerShape(shape: String) {
        viewModelScope.launch {
            themeRepo.setMiniPlayerShape(shape)
        }
    }

    fun setWidgetStyle(style: String) {
        viewModelScope.launch {
            themeRepo.setWidgetStyle(style)
            com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
        }
    }

    fun setWidgetOpacity(opacity: Float) {
        viewModelScope.launch {
            themeRepo.setWidgetOpacity(opacity)
            com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
        }
    }

    fun setWidgetShowSkip(show: Boolean) {
        viewModelScope.launch {
            themeRepo.setWidgetShowSkip(show)
            com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
        }
    }

    fun setWidgetShowAlbumArt(show: Boolean) {
        viewModelScope.launch {
            themeRepo.setWidgetShowAlbumArt(show)
            com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
        }
    }

    fun setWidgetShowWaveform(show: Boolean) {
        viewModelScope.launch {
            themeRepo.setWidgetShowWaveform(show)
            com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
        }
    }

    fun setWidgetShowFavorite(show: Boolean) {
        viewModelScope.launch {
            themeRepo.setWidgetShowFavorite(show)
            com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
        }
    }

    fun setDynamicIslandEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setDynamicIslandEnabled(enabled)
        }
    }

    fun setStaticBlurMode(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setStaticBlurMode(enabled)
        }
    }

    fun setAutoResumeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setAutoResumeEnabled(enabled)
        }
    }

    fun setTrackTransitionAnimation(animation: String) {
        viewModelScope.launch {
            themeRepo.setTrackTransitionAnimation(animation)
        }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setGaplessEnabled(enabled)
        }
    }

    fun setGaplessSilenceThresholdDb(thresholdDb: Float) {
        viewModelScope.launch {
            themeRepo.setGaplessSilenceThresholdDb(thresholdDb)
        }
    }

    fun setAutoScanGaplessOnImport(autoScan: Boolean) {
        viewModelScope.launch {
            themeRepo.setAutoScanGaplessOnImport(autoScan)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            MediaStoreScanner.scanDeviceAudio(app)
        }
    }

    fun createProfile(name: String, avatarUri: String? = null, pin: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = Profile(name = name, avatarUri = avatarUri, pinHash = pin, isActive = false)
            profileDao.insertProfile(profile)
        }
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDao.setActiveProfile(profileId)
        }
    }
}
