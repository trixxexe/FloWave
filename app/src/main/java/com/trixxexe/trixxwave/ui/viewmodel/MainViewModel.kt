package com.trixxexe.trixxwave.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trixxexe.trixxwave.TrixxWaveApp
import com.trixxexe.trixxwave.data.api.AiRepository
import com.trixxexe.trixxwave.data.api.LrclibService
import com.trixxexe.trixxwave.data.api.OpenAiService
import com.trixxexe.trixxwave.data.db.*
import com.trixxexe.trixxwave.data.preferences.AiConfig
import com.trixxexe.trixxwave.data.preferences.EncryptedKeyManager
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.media.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GaplessAnalysisState(
    val isAnalyzing: Boolean = false,
    val analyzedCount: Int = 0,
    val totalCount: Int = 0,
    val totalSilenceTrimmedMs: Long = 0L,
    val statusMessage: String = "Ready"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TrixxWaveApp
    private val db = app.database
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val profileDao = db.profileDao()
    private val historyDao = db.playHistoryDao()
    private val lyricsDao = db.lyricsDao()
    private val recentSearchDao = db.recentSearchDao()

    val themeConfig: StateFlow<ThemeConfig> = app.themePreferences.themeConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeConfig())

    val activeProfile: StateFlow<Profile?> = profileDao.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSongs: StateFlow<List<Song>> = songDao.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedSongs: StateFlow<List<Song>> = songDao.getLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = historyDao.getRecentlyPlayedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<RecentSearch>> = recentSearchDao.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback State
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<Song>>(emptyList())
    val playbackQueue: StateFlow<List<Song>> = _playbackQueue.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsCache?>(null)
    val currentLyrics: StateFlow<LyricsCache?> = _currentLyrics.asStateFlow()

    private val _aiTrackInsight = MutableStateFlow<String?>(null)
    val aiTrackInsight: StateFlow<String?> = _aiTrackInsight.asStateFlow()

    private val _gaplessState = MutableStateFlow(GaplessAnalysisState())
    val gaplessState: StateFlow<GaplessAnalysisState> = _gaplessState.asStateFlow()

    private val keyManager = EncryptedKeyManager(app)
    private val aiRepository = AiRepository(OpenAiService.create())
    private val lrclibService = LrclibService.create()

    init {
        // Auto-scan local audio on launch and run auto-tagger/waveform checks
        viewModelScope.launch(Dispatchers.IO) {
            MediaStoreScanner.scanDeviceAudio(app)
            autoTagAndProcessSongs()
        }

        // Observe allSongs to trigger auto-resume restoration on app launch
        viewModelScope.launch {
            allSongs.filter { it.isNotEmpty() }.first().let { songs ->
                attemptAutoResume(songs)
            }
        }
    }

    private suspend fun attemptAutoResume(songs: List<Song>) {
        if (_currentSong.value != null) return
        val config = themeConfig.value
        if (!config.autoResumeEnabled) return

        val (lastSongId, lastPos) = app.themePreferences.lastPlaybackStateFlow.first()
        if (lastSongId != null) {
            val restoredSong = songs.find { it.id == lastSongId }
            if (restoredSong != null) {
                _currentSong.value = restoredSong
                _currentPositionMs.value = lastPos
                _playbackQueue.value = songs
                _isPlaying.value = false
                notifyWidgetStateChanged()
                viewModelScope.launch(Dispatchers.IO) {
                    fetchLyricsForSong(restoredSong)
                    fetchAiInsightForSong(restoredSong)
                }
            }
        }
    }

    private suspend fun autoTagAndProcessSongs() {
        val songs = songDao.getAllSongs().firstOrNull() ?: return
        val aiConfig = keyManager.getAiConfig()

        for (song in songs) {
            var updated = song
            var needsDbUpdate = false

            // Waveform extraction check
            if (updated.waveformPoints.isNullOrBlank()) {
                val waveform = com.trixxexe.trixxwave.media.WaveformExtractor.extractWaveformPoints(updated.filePath)
                updated = updated.copy(waveformPoints = waveform)
                songDao.updateWaveform(updated.id, waveform)
                needsDbUpdate = true
            }

            // Mood & Genre Tagging (AI if key connected, else local rule-based fallback)
            if (updated.moodTags.isNullOrBlank()) {
                val tags = com.trixxexe.trixxwave.media.TaggingEngine.getMoodAndGenreTags(
                    aiRepository = aiRepository,
                    aiConfig = aiConfig,
                    title = updated.title,
                    artist = updated.artist,
                    album = updated.album
                )
                updated = updated.copy(moodTags = tags)
                songDao.updateMoodTags(updated.id, tags)
                needsDbUpdate = true
            }

            // Gapless silence analysis check
            if (!updated.isGaplessAnalyzed && themeConfig.value.autoScanGaplessOnImport) {
                val gaplessRes = com.trixxexe.trixxwave.media.GaplessAudioAnalyzer.analyzeTrackSilence(
                    updated,
                    themeConfig.value.gaplessSilenceThresholdDb
                )
                updated = updated.copy(
                    trimStartMs = gaplessRes.trimStartMs,
                    trimEndMs = gaplessRes.trimEndMs,
                    isGaplessAnalyzed = true
                )
                songDao.updateGaplessTrim(updated.id, gaplessRes.trimStartMs, gaplessRes.trimEndMs)
                needsDbUpdate = true
            }

            if (_currentSong.value?.id == updated.id && needsDbUpdate) {
                _currentSong.value = updated
            }
        }
    }

    fun runGaplessAnalysisScan(silenceThresholdDb: Float = -45f) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = songDao.getAllSongs().firstOrNull() ?: return@launch
            if (songs.isEmpty()) return@launch

            _gaplessState.value = GaplessAnalysisState(
                isAnalyzing = true,
                analyzedCount = 0,
                totalCount = songs.size,
                statusMessage = "Scanning audio tracks for silent padding..."
            )

            var accumulatedTrim = 0L
            songs.forEachIndexed { index, song ->
                val result = com.trixxexe.trixxwave.media.GaplessAudioAnalyzer.analyzeTrackSilence(
                    song,
                    silenceThresholdDb
                )
                songDao.updateGaplessTrim(song.id, result.trimStartMs, result.trimEndMs)
                accumulatedTrim += result.totalTrimmedMs

                _gaplessState.value = GaplessAnalysisState(
                    isAnalyzing = true,
                    analyzedCount = index + 1,
                    totalCount = songs.size,
                    totalSilenceTrimmedMs = accumulatedTrim,
                    statusMessage = "Analyzed ${song.title} (Trimmed ${result.totalTrimmedMs}ms)"
                )
            }

            val totalSec = accumulatedTrim / 1000f
            _gaplessState.value = GaplessAnalysisState(
                isAnalyzing = false,
                analyzedCount = songs.size,
                totalCount = songs.size,
                totalSilenceTrimmedMs = accumulatedTrim,
                statusMessage = "Gapless scan completed! Trimmed ${String.format("%.2f", totalSec)}s of silent padding."
            )
        }
    }

    fun resetGaplessTrims() {
        viewModelScope.launch(Dispatchers.IO) {
            songDao.resetAllGaplessTrim()
            _gaplessState.value = GaplessAnalysisState(statusMessage = "All gapless silent padding trims reset.")
        }
    }

    fun reTagSongWithAi(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val aiConfig = keyManager.getAiConfig()
            val newTags = com.trixxexe.trixxwave.media.TaggingEngine.getMoodAndGenreTags(
                aiRepository = aiRepository,
                aiConfig = aiConfig,
                title = song.title,
                artist = song.artist,
                album = song.album
            )
            songDao.updateMoodTags(song.id, newTags)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(moodTags = newTags)
            }
        }
    }


    fun playSong(song: Song, queue: List<Song> = allSongs.value) {
        _currentSong.value = song
        _playbackQueue.value = if (queue.isNotEmpty()) queue else listOf(song)
        _isPlaying.value = true
        _currentPositionMs.value = 0L
        savePlaybackStateToDataStore()
        notifyWidgetStateChanged()

        viewModelScope.launch(Dispatchers.IO) {
            songDao.incrementPlayCount(song.id)
            historyDao.insertEntry(PlayHistoryEntry(songId = song.id))
            fetchLyricsForSong(song)
            fetchAiInsightForSong(song)
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        savePlaybackStateToDataStore()
        notifyWidgetStateChanged()
    }

    fun skipNext() {
        val queue = _playbackQueue.value
        val current = _currentSong.value
        if (queue.isEmpty()) return
        val nextIndex = (queue.indexOf(current) + 1) % queue.size
        playSong(queue[nextIndex], queue)
    }

    fun skipPrevious() {
        val queue = _playbackQueue.value
        val current = _currentSong.value
        if (queue.isEmpty()) return
        val prevIndex = (queue.indexOf(current) - 1 + queue.size) % queue.size
        playSong(queue[prevIndex], queue)
    }

    fun toggleLikeSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val newLiked = !song.isLiked
            songDao.setLiked(song.id, newLiked)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isLiked = newLiked)
            }
            notifyWidgetStateChanged()
        }
    }

    private fun notifyWidgetStateChanged() {
        val song = _currentSong.value
        com.trixxexe.trixxwave.widget.WidgetStateStore.savePlaybackState(
            app,
            songTitle = song?.title ?: "FloWave Player",
            artistName = song?.artist ?: "Liquid Glass Aesthetics",
            isPlaying = _isPlaying.value,
            isLiked = song?.isLiked ?: false,
            albumArtUri = song?.albumArtUri
        )
        com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(app)
    }

    fun seekToPosition(positionMs: Long) {
        _currentPositionMs.value = positionMs
        savePlaybackStateToDataStore()
    }

    fun updatePositionFromService(positionMs: Long) {
        _currentPositionMs.value = positionMs
        savePlaybackStateToDataStore()
    }

    private fun savePlaybackStateToDataStore() {
        val song = _currentSong.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            app.themePreferences.saveLastPlaybackState(song.id, _currentPositionMs.value)
        }
    }

    private suspend fun fetchLyricsForSong(song: Song) {
        val cached = lyricsDao.getLyricsForSong(song.id)
        if (cached != null) {
            _currentLyrics.value = cached
            return
        }

        try {
            val response = lrclibService.getLyrics(
                trackName = song.title,
                artistName = song.artist,
                albumName = song.album,
                durationSeconds = (song.durationMs / 1000).toInt()
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val lyrics = LyricsCache(
                    songId = song.id,
                    plainLyrics = body.plainLyrics,
                    syncedLrc = body.syncedLyrics,
                    source = "LRCLIB"
                )
                lyricsDao.insertLyrics(lyrics)
                _currentLyrics.value = lyrics
            }
        } catch (e: Exception) {
            _currentLyrics.value = null
        }
    }

    fun saveCorrectedLyrics(songId: Long, plainLyrics: String?, syncedLrc: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedLyrics = LyricsCache(
                songId = songId,
                plainLyrics = plainLyrics?.ifBlank { null },
                syncedLrc = syncedLrc?.ifBlank { null },
                source = "Manual Editor",
                fetchedAt = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(updatedLyrics)
            if (_currentSong.value?.id == songId) {
                _currentLyrics.value = updatedLyrics
            }
        }
    }

    private suspend fun fetchAiInsightForSong(song: Song) {
        val aiConfig = keyManager.getAiConfig()
        if (aiConfig.apiKey.isNotBlank() && aiConfig.trackInsightsEnabled) {
            val insight = aiRepository.generateTrackInsights(aiConfig, song.title, song.artist, song.album)
            _aiTrackInsight.value = insight
        } else {
            _aiTrackInsight.value = "AI Track Insights available when API Key is configured in Settings."
        }
    }

    fun generateSmartMix(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val aiConfig = keyManager.getAiConfig()
            val availableTitles = allSongs.value.map { it.title }
            val selectedTitles = aiRepository.selectSmartMixSongs(aiConfig, prompt, availableTitles)
            val matchedSongs = allSongs.value.filter { it.title in selectedTitles }

            val newPlaylist = Playlist(
                name = "Smart Mix: $prompt",
                isAutoGenerated = true,
                description = "AI Curated for: $prompt"
            )
            val plId = playlistDao.insertPlaylist(newPlaylist)
            matchedSongs.forEachIndexed { index, song ->
                playlistDao.insertPlaylistSongCrossRef(
                    PlaylistSongCrossRef(playlistId = plId, songId = song.id, position = index)
                )
            }
        }
    }

    fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            recentSearchDao.insertSearch(RecentSearch(query = query.trim()))
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recentSearchDao.deleteSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            recentSearchDao.clearRecentSearches()
        }
    }
}
