package com.trixxexe.trixxwave.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trixxexe.trixxwave.data.api.InnerTubeRepository
import com.trixxexe.trixxwave.data.api.LyricsRepository
import com.trixxexe.trixxwave.data.api.LyricsState
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.db.TrixxWaveDatabase
import com.trixxexe.trixxwave.download.DownloadManager
import com.trixxexe.trixxwave.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    
    val innerTubeRepo = InnerTubeRepository(client)
    val lyricsRepo = LyricsRepository(client)
    val playerManager = PlayerManager(application)
    val downloadManager = DownloadManager(application)
    
    private val db = TrixxWaveDatabase.getDatabase(application)
    val songDao = db.songDao()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val currentDuration = playerManager.currentDuration
    val downloadStates = downloadManager.downloadStates
    val lyricsState = lyricsRepo.lyricsState

    init {
        viewModelScope.launch {
            playerManager.initialize()
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

    fun playSong(song: Song) {
        _currentSong.value = song
        viewModelScope.launch {
            val url = song.originalUrl
            if (url != null && url.startsWith("http")) {
                try {
                    val streamUrl = innerTubeRepo.getStreamUrl(url).getOrNull()
                    if (streamUrl != null) {
                        playerManager.playTrack(
                            url = streamUrl,
                            videoId = url,
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.albumArtUri ?: ""
                        )
                        lyricsRepo.fetchLyrics(song.title, song.artist, song.durationMs, url)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (song.filePath != null) {
                playerManager.playTrack(
                    url = song.filePath,
                    videoId = song.filePath,
                    title = song.title,
                    artist = song.artist,
                    artworkUrl = song.albumArtUri ?: ""
                )
            }
        }
    }

    fun pause() = playerManager.pause()
    fun resume() = playerManager.resume()
    fun seekTo(pos: Long) = playerManager.seekTo(pos)

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            try {
                val url = song.originalUrl ?: return@launch
                val streamUrl = innerTubeRepo.getStreamUrl(url).getOrNull()
                if (streamUrl != null) {
                    downloadManager.startDownload(
                        videoId = url,
                        url = streamUrl,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        artworkUrl = song.albumArtUri,
                        isWebm = streamUrl.contains("webm")
                    )
                }
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
