package com.trixxexe.trixxwave.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.trixxexe.trixxwave.MainActivity
import com.trixxexe.trixxwave.R
import com.trixxexe.trixxwave.TrixxWaveApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Storage for current widget state across process lifecycles.
 */
object WidgetStateStore {
    private const val PREFS_NAME = "trixxwave_widget_state"

    fun savePlaybackState(
        context: Context,
        songTitle: String,
        artistName: String,
        isPlaying: Boolean,
        isLiked: Boolean,
        albumArtUri: String? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("title", songTitle)
            .putString("artist", artistName)
            .putBoolean("isPlaying", isPlaying)
            .putBoolean("isLiked", isLiked)
            .putString("albumArtUri", albumArtUri)
            .apply()
    }

    fun getTitle(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("title", "FloWave Player") ?: "FloWave Player"

    fun getArtist(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("artist", "Liquid Glass Aesthetics") ?: "Liquid Glass Aesthetics"

    fun isPlaying(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("isPlaying", false)

    fun isLiked(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("isLiked", false)

    fun getAlbumArtUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("albumArtUri", null)
}

class TrixxWaveWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                val currentIsPlaying = WidgetStateStore.isPlaying(context)
                WidgetStateStore.savePlaybackState(
                    context,
                    WidgetStateStore.getTitle(context),
                    WidgetStateStore.getArtist(context),
                    !currentIsPlaying,
                    WidgetStateStore.isLiked(context),
                    WidgetStateStore.getAlbumArtUri(context)
                )
                // Broadcast to App
                val appBroadcast = Intent(ACTION_APP_PLAY_PAUSE).setPackage(context.packageName)
                context.sendBroadcast(appBroadcast)
                updateAllWidgets(context)
            }
            ACTION_WIDGET_NEXT -> {
                val appBroadcast = Intent(ACTION_APP_NEXT).setPackage(context.packageName)
                context.sendBroadcast(appBroadcast)
                updateAllWidgets(context)
            }
            ACTION_WIDGET_PREV -> {
                val appBroadcast = Intent(ACTION_APP_PREV).setPackage(context.packageName)
                context.sendBroadcast(appBroadcast)
                updateAllWidgets(context)
            }
            ACTION_WIDGET_LIKE -> {
                val currentLiked = WidgetStateStore.isLiked(context)
                WidgetStateStore.savePlaybackState(
                    context,
                    WidgetStateStore.getTitle(context),
                    WidgetStateStore.getArtist(context),
                    WidgetStateStore.isPlaying(context),
                    !currentLiked,
                    WidgetStateStore.getAlbumArtUri(context)
                )
                val appBroadcast = Intent(ACTION_APP_LIKE).setPackage(context.packageName)
                context.sendBroadcast(appBroadcast)
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.trixxexe.trixxwave.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.trixxexe.trixxwave.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.trixxexe.trixxwave.WIDGET_PREV"
        const val ACTION_WIDGET_LIKE = "com.trixxexe.trixxwave.WIDGET_LIKE"

        const val ACTION_APP_PLAY_PAUSE = "com.trixxexe.trixxwave.APP_PLAY_PAUSE"
        const val ACTION_APP_NEXT = "com.trixxexe.trixxwave.APP_NEXT"
        const val ACTION_APP_PREV = "com.trixxexe.trixxwave.APP_PREV"
        const val ACTION_APP_LIKE = "com.trixxexe.trixxwave.APP_LIKE"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TrixxWaveWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val app = context.applicationContext as? TrixxWaveApp
            val themeConfig = try {
                runBlocking { app?.themePreferences?.themeConfigFlow?.first() }
            } catch (e: Exception) {
                null
            } ?: com.trixxexe.trixxwave.data.preferences.ThemeConfig()

            // Choose Layout based on widgetStyle
            val layoutRes = when (themeConfig.widgetStyle) {
                "Compact Bar" -> R.layout.widget_compact
                "Full Glass Suite" -> R.layout.widget_full_suite
                "Audio Waveform & Stats" -> R.layout.widget_waveform
                "Quick Playlist Launcher" -> R.layout.widget_quick_playlists
                else -> R.layout.widget_standard // "Standard Card"
            }

            val views = RemoteViews(context.packageName, layoutRes)

            val title = WidgetStateStore.getTitle(context)
            val artist = WidgetStateStore.getArtist(context)
            val isPlaying = WidgetStateStore.isPlaying(context)
            val isLiked = WidgetStateStore.isLiked(context)

            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)

            // Play/Pause button icon
            views.setImageViewResource(
                R.id.widget_btn_play,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            // Like heart icon if present
            try {
                views.setImageViewResource(
                    R.id.widget_btn_like,
                    if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                )
            } catch (e: Exception) { /* Optional view */ }

            // Dynamic Theme Color Calculation
            val primaryHex = themeConfig.primaryColorHex
            val baseColor = try {
                android.graphics.Color.parseColor(primaryHex)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#050505")
            }

            val opacity = themeConfig.widgetOpacity.coerceIn(0.05f, 1f)
            val alpha = (opacity * 255).toInt()
            val bgArgb = android.graphics.Color.argb(
                alpha,
                android.graphics.Color.red(baseColor),
                android.graphics.Color.green(baseColor),
                android.graphics.Color.blue(baseColor)
            )

            views.setInt(R.id.widget_container, "setBackgroundColor", bgArgb)

            // Accent Tinting
            val accentHex = themeConfig.accentColorHex
            val accentColorInt = try {
                android.graphics.Color.parseColor(accentHex)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#F27D26")
            }

            val isLightMode = themeConfig.mode == "Light"
            val titleColor = if (isLightMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            val subtextColor = if (isLightMode) android.graphics.Color.DKGRAY else android.graphics.Color.parseColor("#B0BEC5")

            views.setTextColor(R.id.widget_title, titleColor)
            views.setTextColor(R.id.widget_artist, subtextColor)

            // Optional Badge & Stats colors
            try { views.setTextColor(R.id.widget_badge, accentColorInt) } catch (e: Exception) {}
            try { views.setTextColor(R.id.widget_stats, accentColorInt) } catch (e: Exception) {}
            try { views.setInt(R.id.widget_progress_bar, "setBackgroundColor", accentColorInt) } catch (e: Exception) {}

            // Visibility Customizations
            val skipVis = if (themeConfig.widgetShowSkip) View.VISIBLE else View.GONE
            val artVis = if (themeConfig.widgetShowAlbumArt) View.VISIBLE else View.GONE
            val favVis = if (themeConfig.widgetShowFavorite) View.VISIBLE else View.GONE

            try { views.setViewVisibility(R.id.widget_btn_prev, skipVis) } catch (e: Exception) {}
            try { views.setViewVisibility(R.id.widget_btn_next, skipVis) } catch (e: Exception) {}
            try { views.setViewVisibility(R.id.widget_album_art, artVis) } catch (e: Exception) {}
            try { views.setViewVisibility(R.id.widget_btn_like, favVis) } catch (e: Exception) {}

            // Attach Click PendingIntents
            val appIntent = Intent(context, MainActivity::class.java)
            val pendingAppIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingAppIntent)

            // Play/Pause Intent
            val playIntent = Intent(context, TrixxWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PLAY_PAUSE
            }
            val pendingPlay = PendingIntent.getBroadcast(
                context, 101, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play, pendingPlay)

            // Prev Intent
            val prevIntent = Intent(context, TrixxWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PREV
            }
            val pendingPrev = PendingIntent.getBroadcast(
                context, 102, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try { views.setOnClickPendingIntent(R.id.widget_btn_prev, pendingPrev) } catch (e: Exception) {}

            // Next Intent
            val nextIntent = Intent(context, TrixxWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            val pendingNext = PendingIntent.getBroadcast(
                context, 103, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try { views.setOnClickPendingIntent(R.id.widget_btn_next, pendingNext) } catch (e: Exception) {}

            // Like Intent
            val likeIntent = Intent(context, TrixxWaveWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_LIKE
            }
            val pendingLike = PendingIntent.getBroadcast(
                context, 104, likeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try { views.setOnClickPendingIntent(R.id.widget_btn_like, pendingLike) } catch (e: Exception) {}

            // Quick Playlists launch intents
            try { views.setOnClickPendingIntent(R.id.widget_btn_liked, pendingAppIntent) } catch (e: Exception) {}
            try { views.setOnClickPendingIntent(R.id.widget_btn_recent, pendingAppIntent) } catch (e: Exception) {}
            try { views.setOnClickPendingIntent(R.id.widget_btn_smart, pendingAppIntent) } catch (e: Exception) {}

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
