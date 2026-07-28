package com.trixxexe.trixxwave.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.trixxexe.trixxwave.MainActivity
import com.trixxexe.trixxwave.widget.FloWaveWidgetProvider

@RequiresApi(Build.VERSION_CODES.N)
class FloWaveTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        if (tile.state == Tile.STATE_ACTIVE) {
            // Send play/pause intent
            val intent = Intent(this, FloWaveWidgetProvider::class.java).apply {
                action = FloWaveWidgetProvider.ACTION_PLAY_PAUSE
            }
            sendBroadcast(intent)
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        } else {
            // Launch main app
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "FloWave"
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
