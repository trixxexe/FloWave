package com.trixxexe.trixxwave.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.trixxexe.trixxwave.widget.FloWaveWidgetProvider

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(CacheManager.getCacheDataSourceFactory(this))
            )
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateWidgetState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidgetState()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                FloWaveWidgetProvider.ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    updateWidgetState()
                }
                FloWaveWidgetProvider.ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    }
                    updateWidgetState()
                }
                FloWaveWidgetProvider.ACTION_PREV -> {
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    }
                    updateWidgetState()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWidgetState() {
        val currentMedia = player.currentMediaItem
        val title = currentMedia?.mediaMetadata?.title?.toString() ?: "FloWave Player"
        val artist = currentMedia?.mediaMetadata?.artist?.toString() ?: "Select a song to play"
        val isPlaying = player.isPlaying
        FloWaveWidgetProvider.sendUpdateBroadcast(this, title, artist, isPlaying)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.stop()
        player.clearMediaItems()
    }
}

