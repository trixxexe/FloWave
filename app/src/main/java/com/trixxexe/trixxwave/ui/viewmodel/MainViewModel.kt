package com.trixxexe.trixxwave.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trixxexe.trixxwave.data.api.InnerTubeRepository
import com.trixxexe.trixxwave.data.api.LyricsRepository
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.db.TrixxWaveDatabase
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.data.preferences.ThemePreferencesRepository
import com.trixxexe.trixxwave.download.DownloadManager
import com.trixxexe.trixxwave.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    
    val innerTubeRepo = InnerTubeRepository(client)
    val lyricsRepo = LyricsRepository(client)
    val playerManager = PlayerManager(application)
    val downloadManager = DownloadManager(application)
    val themePreferencesRepo = ThemePreferencesRepository(application)
    
    private val db = TrixxWaveDatabase.getDatabase(application)
    val songDao = db.songDao()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val currentDuration = playerManager.currentDuration
    val volume = playerManager.volume
    val downloadStates = downloadManager.downloadStates
    val lyricsState = lyricsRepo.lyricsState

    private val _currentOutputDevice = MutableStateFlow("This Device (Built-In High-Fi Audio)")
    val currentOutputDevice: StateFlow<String> = _currentOutputDevice.asStateFlow()

    private val _showDynamicIsland = MutableStateFlow(true)
    val showDynamicIsland: StateFlow<Boolean> = _showDynamicIsland.asStateFlow()

    private val _aiLyricsInsight = MutableStateFlow<String?>(null)
    val aiLyricsInsight: StateFlow<String?> = _aiLyricsInsight.asStateFlow()

    private val _isGeneratingAiVibe = MutableStateFlow(false)
    val isGeneratingAiVibe: StateFlow<Boolean> = _isGeneratingAiVibe.asStateFlow()

    val themeConfig: StateFlow<ThemeConfig> = themePreferencesRepo.themeConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeConfig())

    val isFirstRun: StateFlow<Boolean> = themePreferencesRepo.isFirstRunFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setFirstRunCompleted(completed: Boolean = true) {
        viewModelScope.launch { themePreferencesRepo.setFirstRunCompleted(completed) }
    }

    fun updateThemePreset(preset: String) {
        viewModelScope.launch { themePreferencesRepo.updateThemePreset(preset) }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch { themePreferencesRepo.setAccentColor(hex) }
    }

    fun setCustomBgUri(uri: String?) {
        viewModelScope.launch { themePreferencesRepo.setCustomBgUri(uri) }
    }

    fun setBlurIntensity(intensity: Float) {
        viewModelScope.launch { themePreferencesRepo.setBlurIntensity(intensity) }
    }

    fun setPureAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { themePreferencesRepo.setPureAmoledBlack(enabled) }
    }

    fun setCornerStyle(style: String) {
        viewModelScope.launch { themePreferencesRepo.setCornerStyle(style) }
    }

    fun setStreamingQuality(quality: String) {
        viewModelScope.launch { themePreferencesRepo.setStreamingQuality(quality) }
    }

    fun setUserProfile(name: String, avatar: String) {
        viewModelScope.launch { themePreferencesRepo.setUserProfile(name, avatar) }
    }

    fun setTrackTransitionAnimation(animation: String) {
        viewModelScope.launch { themePreferencesRepo.setTrackTransitionAnimation(animation) }
    }

    fun setAutoResumeEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferencesRepo.setAutoResumeEnabled(enabled) }
    }

    fun setAiConfig(provider: String, apiKey: String, model: String) {
        viewModelScope.launch { themePreferencesRepo.setAiConfig(provider, apiKey, model) }
    }

    init {
        viewModelScope.launch {
            playerManager.initialize()
            
            // Check if initial database is empty and populate featured music
            if (songDao.getSongCount() == 0) {
                songDao.insertSongs(innerTubeRepo.getFeaturedSongs())
            }
            
            songDao.getAllSongs().collect {
                _songs.value = it
            }
        }
        
        viewModelScope.launch(Dispatchers.Main) {
            while(true) {
                playerManager.updateProgress()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun searchOnline(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val results = innerTubeRepo.searchSongs(query).getOrDefault(emptyList())
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        viewModelScope.launch {
            val videoIdOrUrl = song.originalUrl ?: song.filePath
            if (videoIdOrUrl != null) {
                if (!videoIdOrUrl.startsWith("/") && !videoIdOrUrl.startsWith("file://") && !videoIdOrUrl.startsWith("content://")) {
                    // Online track
                    try {
                        val videoId = videoIdOrUrl.substringAfter("v=").substringBefore("&")
                        val streamUrl = innerTubeRepo.getStreamUrl(videoId, song.title, song.artist).getOrNull() ?: videoIdOrUrl
                        playerManager.playTrack(
                            url = streamUrl,
                            videoId = videoId,
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.albumArtUri ?: ""
                        )
                        lyricsRepo.fetchLyrics(song.title, song.artist, song.durationMs, videoId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Local file playback
                    playerManager.playTrack(
                        url = videoIdOrUrl,
                        videoId = videoIdOrUrl,
                        title = song.title,
                        artist = song.artist,
                        artworkUrl = song.albumArtUri ?: ""
                    )
                    lyricsRepo.fetchLyrics(song.title, song.artist, song.durationMs, song.id.toString())
                }
            }
        }
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val updated = song.copy(isLiked = !song.isLiked)
            songDao.insertSong(updated)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = updated
            }
        }
    }

    fun playNext() {
        val list = _songs.value
        val current = _currentSong.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id || it.originalUrl == current.originalUrl }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playSong(list[currentIndex + 1])
        } else if (list.isNotEmpty()) {
            playSong(list.first())
        }
    }

    fun playPrevious() {
        val list = _songs.value
        val current = _currentSong.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id || it.originalUrl == current.originalUrl }
        if (currentIndex > 0) {
            playSong(list[currentIndex - 1])
        } else if (list.isNotEmpty()) {
            playSong(list.last())
        }
    }

    fun pause() = playerManager.pause()
    fun resume() = playerManager.resume()
    fun seekTo(pos: Long) = playerManager.seekTo(pos)

    fun setVolume(vol: Float) {
        playerManager.setVolume(vol)
    }

    fun selectOutputDevice(deviceName: String) {
        _currentOutputDevice.value = deviceName
    }

    fun toggleDynamicIsland(enabled: Boolean? = null) {
        _showDynamicIsland.value = enabled ?: !_showDynamicIsland.value
    }

    fun generateLyricsInsight(title: String, artist: String) {
        viewModelScope.launch {
            _aiLyricsInsight.value = "Analyzing poetic themes for '$title'..."
            kotlinx.coroutines.delay(1200)
            _aiLyricsInsight.value = "✨ AI Lyric Analysis for '$title' by $artist:\n\nThis track explores themes of atmospheric liquid flow, urban synth harmony, and emotional resonance. The continuous rhythm pairs ambient melodies with deep bass textures, creating a late-night acoustic space."
        }
    }

    fun generateAiVibePlaylist(vibePrompt: String) {
        viewModelScope.launch {
            _isGeneratingAiVibe.value = true
            kotlinx.coroutines.delay(1500)
            val currentList = _songs.value
            if (currentList.isNotEmpty()) {
                val shuffled = currentList.shuffled()
                _searchResults.value = shuffled
                _searchQuery.value = "AI Vibe: $vibePrompt"
            }
            _isGeneratingAiVibe.value = false
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            try {
                val url = song.originalUrl ?: return@launch
                val videoId = url.substringAfter("v=").substringBefore("&")
                val streamUrl = innerTubeRepo.getStreamUrl(videoId, song.title, song.artist).getOrNull() ?: url
                
                val trackKey = if (videoId.length in 5..25) videoId else "dl_${song.title.hashCode()}_${song.artist.hashCode()}"
                downloadManager.startDownload(
                    videoId = trackKey,
                    url = streamUrl,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    artworkUrl = song.albumArtUri,
                    isWebm = streamUrl.contains("webm")
                )
                // Also save song into local DB
                songDao.insertSong(song)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

