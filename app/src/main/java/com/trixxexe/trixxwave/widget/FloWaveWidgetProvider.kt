package com.trixxexe.trixxwave.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trixxexe.trixxwave.MainActivity
import com.trixxexe.trixxwave.R
import com.trixxexe.trixxwave.player.PlaybackService

class FloWaveWidgetProvider : AppWidgetProvider() {

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
        val action = intent.action
        if (action == ACTION_PLAY_PAUSE || action == ACTION_NEXT || action == ACTION_PREV) {
            val serviceIntent = Intent(context, PlaybackService::class.java).apply {
                this.action = action
            }
            context.startForegroundService(serviceIntent)
        } else if (action == ACTION_UPDATE_WIDGET) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "FloWave Player"
            val artist = intent.getStringExtra(EXTRA_ARTIST) ?: "Playing Music"
            val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FloWaveWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_flowave).apply {
                    setTextViewText(R.id.widget_title, title)
                    setTextViewText(R.id.widget_artist, artist)
                    setImageViewResource(
                        R.id.widget_btn_play_pause,
                        if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    )
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.trixxexe.trixxwave.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.trixxexe.trixxwave.ACTION_NEXT"
        const val ACTION_PREV = "com.trixxexe.trixxwave.ACTION_PREV"
        const val ACTION_UPDATE_WIDGET = "com.trixxexe.trixxwave.ACTION_UPDATE_WIDGET"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_flowave)

            // Open app when clicking container
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

            // Play/Pause button
            val playPauseIntent = Intent(context, FloWaveWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            // Next button
            val nextIntent = Intent(context, FloWaveWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Prev button
            val prevIntent = Intent(context, FloWaveWidgetProvider::class.java).apply {
                action = ACTION_PREV
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context, 3, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun sendUpdateBroadcast(context: Context, title: String, artist: String, isPlaying: Boolean) {
            val intent = Intent(context, FloWaveWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.sendBroadcast(intent)
        }
    }
}
