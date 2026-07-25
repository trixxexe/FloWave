package com.trixxexe.trixxwave.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent

class TrixxWaveTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "FloWave"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        if (tile.state == Tile.STATE_ACTIVE) {
            tile.state = Tile.STATE_INACTIVE
        } else {
            tile.state = Tile.STATE_ACTIVE
        }
        tile.updateTile()

        // Send playback toggle intent
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = "TOGGLE_PLAYBACK"
        }
        startService(intent)
    }
}
