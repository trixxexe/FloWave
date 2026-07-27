package com.trixxexe.trixxwave.ui.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trixxexe.trixxwave.TrixxWaveApp
import com.trixxexe.trixxwave.data.api.AiRepository
import com.trixxexe.trixxwave.data.api.AudiusRepository
import com.trixxexe.trixxwave.data.api.LrclibRepository
import com.trixxexe.trixxwave.data.api.LrclibService
import com.trixxexe.trixxwave.data.api.OpenAiService
import com.trixxexe.trixxwave.data.api.RadioRepository
import com.trixxexe.trixxwave.data.api.YoutubeStreamRepository
import com.trixxexe.trixxwave.data.db.*
import com.trixxexe.trixxwave.data.preferences.AiConfig
import com.trixxexe.trixxwave.data.preferences.EncryptedKeyManager
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.media.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Hybrid Online Mode States
    private val _isOnlineMode = MutableStateFlow(false)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    private val _activeOnlineTab = MutableStateFlow("YOUTUBE") // "YOUTUBE", "AUDIUS", "RADIO"
    val activeOnlineTab: StateFlow<String> = _activeOnlineTab.asStateFlow()

    private val _youtubeSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val youtubeSearchResults: StateFlow<List<Song>> = _youtubeSearchResults.asStateFlow()

    private val _audiusTrending = MutableStateFlow<List<Song>>(emptyList())
    val audiusTrending: StateFlow<List<Song>> = _audiusTrending.asStateFlow()

    private val _radioStations = MutableStateFlow<List<Song>>(emptyList())
    val radioStations: StateFlow<List<Song>> = _radioStations.asStateFlow()

    private val _isExtractingStream = MutableStateFlow(false)
    val isExtractingStream: StateFlow<Boolean> = _isExtractingStream.asStateFlow()

    private val _isOnlineSearchLoading = MutableStateFlow(false)
    val isOnlineSearchLoading: StateFlow<Boolean> = _isOnlineSearchLoading.asStateFlow()

    private val _onlineStreamError = MutableStateFlow<String?>(null)
    val onlineStreamError: StateFlow<String?> = _onlineStreamError.asStateFlow()

    private val keyManager = EncryptedKeyManager(app)
    private val aiRepository = AiRepository(OpenAiService.create())
    private val lrclibService = LrclibService.create()
    val lrclibRepo by lazy { LrclibRepository(lrclibService, lyricsDao) }

    val youtubeRepo = YoutubeStreamRepository(app)
    val radioRepo = RadioRepository()
    val audiusRepo = AudiusRepository()

    private var contentObserver: ContentObserver? = null

    init {
        registerStorageObserver()
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

    fun scanCustomFolder(folderPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val added = MediaStoreScanner.scanCustomDirectory(app, folderPath)
            if (added > 0) {
                autoTagAndProcessSongs()
            }
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    app,
                    if (added > 0) "Indexed $added new audio files from folder!" else "No new audio files found in selected directory.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun registerStorageObserver() {
        if (contentObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                viewModelScope.launch(Dispatchers.IO) {
                    val added = MediaStoreScanner.scanDeviceAudio(app)
                    if (added > 0) {
                        autoTagAndProcessSongs()
                    }
                }
            }
        }
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            app.contentResolver.registerContentObserver(uri, true, observer)
            contentObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        contentObserver?.let {
            try { app.contentResolver.unregisterContentObserver(it) } catch (e: Exception) {}
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
        val localDownloadedCopy = allSongs.value.firstOrNull { 
            (it.source == "DOWNLOADED" || (it.filePath.startsWith("/") && !it.filePath.startsWith("http"))) && 
            it.title.equals(song.title, ignoreCase = true) && 
            it.artist.equals(song.artist, ignoreCase = true) 
        }
        val targetSong = localDownloadedCopy ?: song

        if ((targetSong.source == "YOUTUBE_EXTRACTED" || targetSong.filePath.contains("youtube.com") || targetSong.filePath.contains("youtu.be")) && targetSong.streamUrl.isNullOrBlank()) {
            android.util.Log.d("MainViewModel", "playSong intercepted YouTube track with blank streamUrl. Triggering extraction for: '${targetSong.title}'")
            extractAndPlayYoutubeUrl(targetSong.originalUrl ?: targetSong.filePath, targetSong.title, targetSong.artist)
            return
        }

        android.util.Log.d("MainViewModel", "playSong setting currentSong: '${targetSong.title}', streamUrl: ${targetSong.streamUrl}, filePath: ${targetSong.filePath}")
        _currentSong.value = targetSong
        _playbackQueue.value = if (queue.isNotEmpty()) queue else listOf(targetSong)
        _isPlaying.value = true
        _currentPositionMs.value = 0L
        savePlaybackStateToDataStore()
        notifyWidgetStateChanged()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = songDao.getSongById(song.id)
                val targetSongId = if (existing == null) {
                    songDao.insertSong(song)
                } else {
                    song.id
                }
                songDao.incrementPlayCount(targetSongId)
                historyDao.insertEntry(PlayHistoryEntry(songId = targetSongId))
                fetchLyricsForSong(song)
                fetchAiInsightForSong(song)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
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
            try {
                val newLiked = !song.isLiked
                val existing = songDao.getSongById(song.id)
                if (existing == null) {
                    songDao.insertSong(song.copy(isLiked = newLiked))
                } else {
                    songDao.setLiked(song.id, newLiked)
                }
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = _currentSong.value?.copy(isLiked = newLiked)
                }
                notifyWidgetStateChanged()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
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
        val lyrics = lrclibRepo.getOrFetchLyrics(song)
        _currentLyrics.value = lyrics
        if (lyrics?.aiInsight != null) {
            _aiTrackInsight.value = lyrics.aiInsight
        }
    }

    fun saveCorrectedLyrics(songId: Long, plainLyrics: String?, syncedLrc: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = lyricsDao.getLyricsForSong(songId)
            val updatedLyrics = LyricsCache(
                songId = songId,
                plainLyrics = plainLyrics?.ifBlank { null },
                syncedLrc = syncedLrc?.ifBlank { null },
                aiInsight = existing?.aiInsight,
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
        val cached = lyricsDao.getLyricsForSong(song.id)
        if (!cached?.aiInsight.isNullOrBlank()) {
            _aiTrackInsight.value = cached!!.aiInsight
            return
        }

        val aiConfig = keyManager.getAiConfig()
        if (aiConfig.apiKey.isNotBlank() && aiConfig.trackInsightsEnabled) {
            try {
                val insight = aiRepository.generateTrackInsights(aiConfig, song.title, song.artist, song.album)
                _aiTrackInsight.value = insight
                val existing = cached ?: LyricsCache(songId = song.id)
                lyricsDao.insertLyrics(existing.copy(aiInsight = insight))
            } catch (e: Exception) {
                if (!cached?.aiInsight.isNullOrBlank()) {
                    _aiTrackInsight.value = cached!!.aiInsight
                } else {
                    _aiTrackInsight.value = "Unable to fetch online insights: ${e.localizedMessage}. Check network connection or API Key."
                }
            }
        } else {
            if (!cached?.aiInsight.isNullOrBlank()) {
                _aiTrackInsight.value = cached!!.aiInsight
            } else {
                _aiTrackInsight.value = "AI Track Insights available when API Key is configured in Settings."
            }
        }
    }

    fun generateSmartMix(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val songsList = allSongs.value
            val aiConfig = keyManager.getAiConfig()
            val availableTitles = songsList.map { it.title }
            val selectedTitles = aiRepository.selectSmartMixSongs(aiConfig, prompt, availableTitles)
            var matchedSongs = songsList.filter { it.title in selectedTitles }

            if (matchedSongs.isEmpty() && songsList.isNotEmpty()) {
                // Smart fallback: search by prompt keywords or pick top songs
                val promptWords = prompt.lowercase().split(" ").filter { it.length > 2 }
                val keywordMatches = songsList.filter { song ->
                    promptWords.any { word ->
                        song.title.lowercase().contains(word) ||
                        song.artist.lowercase().contains(word) ||
                        song.genre?.lowercase()?.contains(word) == true ||
                        song.moodTags?.lowercase()?.contains(word) == true
                    }
                }
                matchedSongs = if (keywordMatches.isNotEmpty()) keywordMatches else songsList.shuffled().take(8)
            }

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

    fun createCustomPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val pl = Playlist(
                name = name.trim(),
                description = description.ifBlank { "Custom Playlist" },
                isAutoGenerated = false
            )
            playlistDao.insertPlaylist(pl)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId)

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val target = songs.getOrNull(startIndex) ?: songs.first()
        _playbackQueue.value = songs
        playSong(target)
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
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

    // --- Hybrid Online Mode Functions ---
    fun toggleOnlineMode(online: Boolean) {
        _isOnlineMode.value = online
        if (online) {
            when (_activeOnlineTab.value) {
                "AUDIUS" -> if (_audiusTrending.value.isEmpty()) fetchAudiusTrending()
                "RADIO" -> if (_radioStations.value.isEmpty()) fetchRadioStations()
            }
        }
    }

    fun setOnlineTab(tab: String) {
        _activeOnlineTab.value = tab
        when (tab) {
            "AUDIUS" -> if (_audiusTrending.value.isEmpty()) fetchAudiusTrending()
            "RADIO" -> if (_radioStations.value.isEmpty()) fetchRadioStations()
        }
    }

    fun searchYoutube(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isOnlineSearchLoading.value = true
            _onlineStreamError.value = null
            val results = youtubeRepo.searchYoutube(query)
            _youtubeSearchResults.value = results
            _isOnlineSearchLoading.value = false
        }
    }

    fun extractAndPlayYoutubeUrl(urlOrQuery: String, songTitle: String? = null, songArtist: String? = null) {
        if (urlOrQuery.isBlank()) return
        viewModelScope.launch {
            _isExtractingStream.value = true
            _onlineStreamError.value = null

            val videoId = youtubeRepo.extractVideoId(urlOrQuery)
            if (videoId == null && !urlOrQuery.startsWith("http")) {
                searchYoutube(urlOrQuery)
                _isExtractingStream.value = false
                return@launch
            }

            val result = youtubeRepo.extractAudioStream(urlOrQuery, songTitle, songArtist)
            _isExtractingStream.value = false

            if (result != null) {
                val song = Song(
                    id = if (videoId != null) -3000000000L - (videoId.hashCode().toLong().and(0x7FFFFFFF)) else System.currentTimeMillis(),
                    title = result.title.ifBlank { songTitle ?: "YouTube Track" },
                    artist = result.artist.ifBlank { songArtist ?: "YouTube Artist" },
                    album = "YouTube Direct",
                    durationMs = result.durationMs,
                    filePath = result.streamUrl,
                    albumArtUri = result.artworkUrl,
                    genre = "YouTube Stream",
                    source = "YOUTUBE_EXTRACTED",
                    streamUrl = result.streamUrl,
                    originalUrl = urlOrQuery
                )
                playSong(song, listOf(song))
            } else {
                _onlineStreamError.value = "Unable to stream online track. Please check network or try searching keyword."
            }
        }
    }

    fun fetchAudiusTrending() {
        viewModelScope.launch {
            _isOnlineSearchLoading.value = true
            val tracks = audiusRepo.getTrendingTracks()
            _audiusTrending.value = tracks
            _isOnlineSearchLoading.value = false
        }
    }

    fun searchAudius(query: String) {
        viewModelScope.launch {
            _isOnlineSearchLoading.value = true
            val tracks = audiusRepo.searchTracks(query)
            _audiusTrending.value = tracks
            _isOnlineSearchLoading.value = false
        }
    }

    fun fetchRadioStations(query: String = "") {
        viewModelScope.launch {
            _isOnlineSearchLoading.value = true
            val stations = radioRepo.searchStations(query)
            _radioStations.value = stations
            _isOnlineSearchLoading.value = false
        }
    }

    fun playOnlineTrack(song: Song) {
        if (song.source == "YOUTUBE_EXTRACTED" && song.streamUrl.isNullOrBlank()) {
            extractAndPlayYoutubeUrl(song.originalUrl ?: song.filePath, song.title, song.artist)
        } else {
            playSong(song, listOf(song))
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = songDao.getSongById(song.id)
                val targetId = if (existing == null) {
                    songDao.insertSong(song)
                } else {
                    song.id
                }
                val currentSongs = playlistDao.getSongsForPlaylist(playlistId).firstOrNull() ?: emptyList()
                val newPos = currentSongs.size
                playlistDao.insertPlaylistSongCrossRef(
                    PlaylistSongCrossRef(playlistId = playlistId, songId = targetId, position = newPos)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _downloadStatusMap = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val downloadStatusMap: StateFlow<Map<Long, Float>> = _downloadStatusMap.asStateFlow()

    fun downloadOnlineSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(app, "Downloading '${song.title}'...", android.widget.Toast.LENGTH_SHORT).show()
            }
            try {
                _downloadStatusMap.value = _downloadStatusMap.value + (song.id to 0.05f)
                
                // Extract a verified direct audio stream URL
                val extracted = youtubeRepo.extractAudioStream(
                    urlOrQuery = song.originalUrl ?: song.filePath,
                    songTitle = song.title,
                    songArtist = song.artist
                )
                val targetStreamUrl = extracted?.streamUrl ?: song.streamUrl ?: song.filePath

                if (targetStreamUrl.isBlank() || targetStreamUrl.startsWith("/")) {
                    _downloadStatusMap.value = _downloadStatusMap.value - song.id
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(app, "Unable to resolve stream URL for '${song.title}'", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val downloadDir = java.io.File(app.filesDir, "downloads").apply { mkdirs() }
                val cleanFileName = "${System.currentTimeMillis()}_${song.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.mp3"
                val destFile = java.io.File(downloadDir, cleanFileName)

                val req = okhttp3.Request.Builder()
                    .url(targetStreamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful || response.body == null) {
                        _downloadStatusMap.value = _downloadStatusMap.value - song.id
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(app, "Download failed (HTTP ${response.code})", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    val body = response.body!!
                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L

                    body.byteStream().use { input ->
                        destFile.outputStream().use { output ->
                            val buffer = ByteArray(16384)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                val progress = if (totalBytes > 0) {
                                    (downloadedBytes.toFloat() / totalBytes).coerceIn(0.05f, 0.98f)
                                } else {
                                    (((downloadedBytes / 102400L) % 10) + 1) / 10f
                                }
                                _downloadStatusMap.value = _downloadStatusMap.value + (song.id to progress)
                            }
                        }
                    }
                }

                // Verify downloaded file content (must be audio bytes, not HTML error pages)
                if (!destFile.exists() || destFile.length() < 5000L) {
                    destFile.delete()
                    _downloadStatusMap.value = _downloadStatusMap.value - song.id
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(app, "Downloaded file is invalid or corrupted. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val headerBytes = destFile.inputStream().use { input ->
                    val buf = ByteArray(200)
                    val n = input.read(buf)
                    if (n > 0) String(buf, 0, n, Charsets.UTF_8) else ""
                }
                if (headerBytes.contains("<!DOCTYPE", ignoreCase = true) || headerBytes.contains("<html", ignoreCase = true) || headerBytes.contains("403 Forbidden", ignoreCase = true)) {
                    destFile.delete()
                    _downloadStatusMap.value = _downloadStatusMap.value - song.id
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(app, "Download blocked by server. Trying alternate link...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val targetDbId = if (song.id < 0) 0L else song.id
                val updatedSong = song.copy(
                    id = targetDbId,
                    filePath = destFile.absolutePath,
                    source = "DOWNLOADED",
                    streamUrl = destFile.absolutePath,
                    albumArtUri = extracted?.artworkUrl ?: song.albumArtUri
                )
                
                val insertedId = songDao.insertSong(updatedSong)
                val finalSong = updatedSong.copy(id = if (insertedId > 0) insertedId else updatedSong.id)

                val current = _currentSong.value
                if (current != null && (current.id == song.id || (current.title.equals(song.title, ignoreCase = true) && current.artist.equals(song.artist, ignoreCase = true)))) {
                    _currentSong.value = finalSong
                }

                _downloadStatusMap.value = _downloadStatusMap.value + (song.id to 1.0f)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "Downloaded '${song.title}' to Offline Library!", android.widget.Toast.LENGTH_LONG).show()
                }
                kotlinx.coroutines.delay(1200L)
                _downloadStatusMap.value = _downloadStatusMap.value - song.id

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadStatusMap.value = _downloadStatusMap.value - song.id
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "Download failed: ${e.localizedMessage ?: "Network error"}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportSongToPublicStorage(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val srcFile = java.io.File(song.filePath)
                if (!srcFile.exists()) return@launch

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "${song.title} - ${song.artist}.mp3")
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MUSIC + "/FloWave")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val uri = app.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        app.contentResolver.openOutputStream(uri)?.use { out ->
                            srcFile.inputStream().use { input -> input.copyTo(out) }
                        }
                        values.clear()
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        app.contentResolver.update(uri, values, null, null)
                    }
                } else {
                    val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                    val floWaveFolder = java.io.File(musicDir, "FloWave").apply { mkdirs() }
                    val dest = java.io.File(floWaveFolder, "${song.title} - ${song.artist}.mp3")
                    srcFile.copyTo(dest, overwrite = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
