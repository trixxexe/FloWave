package com.trixxexe.trixxwave.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.trixxexe.trixxwave.MainActivity
import com.trixxexe.trixxwave.TrixxWaveApp
import com.trixxexe.trixxwave.media.AudioEqualizerManager
import com.trixxexe.trixxwave.media.AudioVisualizerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var playerListener: Player.Listener? = null
    private var customNotificationProvider: CustomMediaNotificationProvider? = null

    val equalizerManager = AudioEqualizerManager()
    val visualizerHelper = AudioVisualizerHelper()

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var sleepTimerJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()

        val cacheFactory = com.trixxexe.trixxwave.media.MediaCacheManager.getCacheDataSourceFactory(this)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player = exoPlayer

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()

        val notificationProvider = CustomMediaNotificationProvider(
            this,
            AndroidColor.parseColor("#F27D26")
        )
        customNotificationProvider = notificationProvider
        setMediaNotificationProvider(notificationProvider)

        val app = application as? TrixxWaveApp
        if (app != null) {
            serviceScope.launch {
                app.themePreferences.themeConfigFlow.collect { config ->
                    val colorInt = try {
                        AndroidColor.parseColor(config.accentColorHex)
                    } catch (e: Exception) {
                        AndroidColor.parseColor("#F27D26")
                    }
                    notificationProvider.updateAccentColor(colorInt)
                    setMediaNotificationProvider(notificationProvider)
                }
            }
        }

        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                try {
                    if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        equalizerManager.initAudioEffects(audioSessionId)
                        visualizerHelper.attachToAudioSession(audioSessionId, serviceScope, exoPlayer.isPlaying)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                try {
                    val audioSessionId = exoPlayer.audioSessionId
                    visualizerHelper.attachToAudioSession(audioSessionId, serviceScope, isPlaying)
                    updateWidgetFromPlayer()
                    if (isPlaying) {
                        startWidgetProgressLoop()
                    } else {
                        widgetPingJob?.cancel()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateWidgetFromPlayer()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.d("PlaybackService", "ExoPlayer playbackStateChanged: $playbackState (READY=${Player.STATE_READY}, BUFFERING=${Player.STATE_BUFFERING}, ENDED=${Player.STATE_ENDED})")
                updateWidgetFromPlayer()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlaybackService", "ExoPlayer error [code=${error.errorCodeName}]: ${error.message}", error)
            }
        }
        playerListener = listener
        exoPlayer.addListener(listener)

        WidgetUpdateWorker.schedulePeriodicUpdates(this)
    }

    private var widgetPingJob: Job? = null

    private fun updateWidgetFromPlayer() {
        val p = player ?: return
        val item = p.currentMediaItem
        val metadata = item?.mediaMetadata
        val title = metadata?.title?.toString() ?: "FloWave Player"
        val artist = metadata?.artist?.toString() ?: "Liquid Glass Aesthetics"
        val artUri = metadata?.artworkUri?.toString()
        val isPlaying = p.isPlaying

        com.trixxexe.trixxwave.widget.WidgetStateStore.savePlaybackState(
            context = this,
            songTitle = title,
            artistName = artist,
            isPlaying = isPlaying,
            isLiked = com.trixxexe.trixxwave.widget.WidgetStateStore.isLiked(this),
            albumArtUri = artUri
        )
        com.trixxexe.trixxwave.widget.TrixxWaveWidgetProvider.updateAllWidgets(this)
    }

    private fun startWidgetProgressLoop() {
        widgetPingJob?.cancel()
        widgetPingJob = serviceScope.launch {
            while (player?.isPlaying == true) {
                updateWidgetFromPlayer()
                delay(10000L)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return binder
    }

    fun startSleepTimer(minutes: Int, onTimerFinished: () -> Unit) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) return

        sleepTimerJob = serviceScope.launch {
            val totalMs = minutes * 60 * 1000L
            val fadeMs = 10000L // 10 second gradual fade out
            val normalMs = (totalMs - fadeMs).coerceAtLeast(0L)

            delay(normalMs)

            val p = player ?: return@launch
            val startVol = p.volume
            val steps = 20
            val stepDelay = fadeMs / steps

            for (i in steps downTo 0) {
                p.volume = startVol * (i.toFloat() / steps)
                delay(stepDelay)
            }

            p.pause()
            p.volume = startVol
            onTimerFinished()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        player?.volume = 1.0f
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    fun play() {
        try {
            player?.play()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            player?.pause()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            player?.seekTo(positionMs)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun prepareTrackForResume(song: com.trixxexe.trixxwave.data.db.Song, queue: List<com.trixxexe.trixxwave.data.db.Song> = emptyList(), positionMs: Long = 0L, isGaplessEnabled: Boolean = true) {
        val p = player ?: return
        try {
            val targetId = song.id.toString()

            if (p.currentMediaItem?.mediaId == targetId) {
                p.pause()
                return
            }

            p.clearMediaItems()

            val playlist = if (queue.isNotEmpty()) queue else listOf(song)
            val mediaItems = playlist.map { s ->
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)
                    .setAlbumTitle(s.album)
                    .setArtworkUri(s.albumArtUri?.let { android.net.Uri.parse(it) })
                    .build()

                val rawUri = s.streamUrl?.takeIf { it.isNotBlank() } ?: s.filePath
                val androidUri = if (rawUri.startsWith("/")) {
                    android.net.Uri.fromFile(java.io.File(rawUri))
                } else {
                    android.net.Uri.parse(rawUri)
                }

                val builder = MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(androidUri)
                    .setMediaMetadata(metadata)

                if (isGaplessEnabled && (s.trimStartMs > 0 || s.trimEndMs > 0)) {
                    val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(s.trimStartMs)

                    if (s.trimEndMs > 0 && s.durationMs > s.trimEndMs) {
                        clippingBuilder.setEndPositionMs((s.durationMs - s.trimEndMs).coerceAtLeast(s.trimStartMs + 500L))
                    }
                    builder.setClippingConfiguration(clippingBuilder.build())
                }

                builder.build()
            }

            p.setMediaItems(mediaItems)
            val songIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            p.seekTo(songIndex, positionMs)
            p.prepare()
            p.pause()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun playTrackWithGapless(song: com.trixxexe.trixxwave.data.db.Song, queue: List<com.trixxexe.trixxwave.data.db.Song> = emptyList(), isGaplessEnabled: Boolean = true) {
        val p = player ?: return
        try {
            val targetId = song.id.toString()

            if (p.currentMediaItem?.mediaId == targetId) {
                if (!p.isPlaying) {
                    p.play()
                }
                return
            }

            p.clearMediaItems()

            val playlist = if (queue.isNotEmpty()) queue else listOf(song)
            val mediaItems = playlist.map { s ->
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)
                    .setAlbumTitle(s.album)
                    .setArtworkUri(s.albumArtUri?.let { android.net.Uri.parse(it) })
                    .build()

                val rawUri = s.streamUrl?.takeIf { it.isNotBlank() } ?: s.filePath
                val androidUri = if (rawUri.startsWith("/")) {
                    android.net.Uri.fromFile(java.io.File(rawUri))
                } else {
                    android.net.Uri.parse(rawUri)
                }

                val builder = MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(androidUri)
                    .setMediaMetadata(metadata)

                val trimStart = if (isGaplessEnabled) {
                    if (s.trimStartMs > 0) s.trimStartMs else 900L // Default 0.9s pre-music silence cut
                } else {
                    0L
                }

                if (isGaplessEnabled && (trimStart > 0 || s.trimEndMs > 0)) {
                    val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(trimStart)

                    if (s.trimEndMs > 0 && s.durationMs > s.trimEndMs) {
                        clippingBuilder.setEndPositionMs((s.durationMs - s.trimEndMs).coerceAtLeast(trimStart + 500L))
                    }
                    builder.setClippingConfiguration(clippingBuilder.build())
                }

                builder.build()
            }

            p.setMediaItems(mediaItems)
            val songIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            val seekStart = if (isGaplessEnabled) {
                if (song.trimStartMs > 0) song.trimStartMs else 900L
            } else {
                0L
            }
            p.seekTo(songIndex, seekStart)
            p.prepare()
            p.play()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            DynamicIslandOverlayService.stopService(this)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        try {
            DynamicIslandOverlayService.stopService(this)
        } catch (_: Throwable) {}
        widgetPingJob?.cancel()
        widgetPingJob = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        equalizerManager.release()
        visualizerHelper.release()
        playerListener?.let { player?.removeListener(it) }
        playerListener = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

private class CustomMediaNotificationProvider(
    context: android.content.Context,
    private var accentColorInt: Int
) : MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider.Builder(context).build()

    fun updateAccentColor(colorInt: Int) {
        this.accentColorInt = colorInt
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val mediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            callback
        )
        // Apply per-theme accent color to system media notification & media controls tint
        mediaNotification.notification.color = accentColorInt
        return mediaNotification
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle
    ): Boolean {
        return defaultProvider.handleCustomCommand(session, action, extras)
    }
}
