package com.trixxexe.trixxwave.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import java.io.File

@OptIn(UnstableApi::class)
object CacheManager {
    private var cache: SimpleCache? = null
    
    fun getCache(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: createCache(context).also { cache = it }
        }
    }
    
    private fun createCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(2L * 1024 * 1024 * 1024) // 2 GB
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

class PlayerManager(private val context: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    suspend fun initialize() {
        if (controller != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controller = controllerFuture?.await()
        
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = controller?.duration ?: 0L
                if (dur > 0) _currentDuration.value = dur
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                val dur = controller?.duration ?: 0L
                if (dur > 0) _currentDuration.value = dur
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val dur = controller?.duration ?: 0L
                if (dur > 0) _currentDuration.value = dur
            }
        })
    }
    
    fun updateProgress() {
        val c = controller ?: return
        _currentPosition.value = c.currentPosition
        val dur = c.duration
        if (dur > 0 && _currentDuration.value != dur) {
            _currentDuration.value = dur
        }
    }

    fun playTrack(url: String, videoId: String, title: String, artist: String, artworkUrl: String) {
        val c = controller ?: return
        _isPlaying.value = true
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(android.net.Uri.parse(artworkUrl))
                    .build()
            )
            .build()
            
        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()
    }

    fun pause() {
        _isPlaying.value = false
        controller?.pause()
    }

    fun resume() {
        _isPlaying.value = true
        controller?.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        controller?.volume = clamped
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }
}
