package com.trixxexe.trixxwave.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val currentLevelMb: Short
)

class AudioEqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    var isEnabled: Boolean = false
        private set

    fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }
            isEnabled = true
        } catch (e: Exception) {
            Log.e("AudioEqualizerManager", "Error initializing audio effects: ${e.localizedMessage}")
        }
    }

    fun getBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
        val minMb = range[0]
        val maxMb = range[1]

        val list = mutableListOf<EqualizerBand>()
        for (i in 0 until numBands) {
            val bandIndex = i.toShort()
            val freq = eq.getCenterFreq(bandIndex) / 1000
            val current = eq.getBandLevel(bandIndex)
            list.add(EqualizerBand(i, freq, minMb, maxMb, current))
        }
        return list
    }

    fun setBandLevel(bandIndex: Int, levelMb: Short) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMb)
        } catch (e: Exception) {
            Log.e("AudioEqualizerManager", "Failed setBandLevel: ${e.message}")
        }
    }

    fun setBassBoost(strength: Short) { // 0 to 1000
        try {
            bassBoost?.setStrength(strength)
        } catch (e: Exception) {
            Log.e("AudioEqualizerManager", "Failed setBassBoost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Short) { // 0 to 1000
        try {
            virtualizer?.setStrength(strength)
        } catch (e: Exception) {
            Log.e("AudioEqualizerManager", "Failed setVirtualizer: ${e.message}")
        }
    }

    fun applyPreset(presetName: String) {
        val bands = getBands()
        if (bands.isEmpty()) return
        when (presetName) {
            "Bass Boost" -> {
                setBassBoost(800)
                setBandLevel(0, 1000)
                setBandLevel(1, 600)
            }
            "Flat" -> {
                setBassBoost(0)
                bands.indices.forEach { setBandLevel(it, 0) }
            }
            "Rock" -> {
                setBassBoost(500)
                bands.indices.forEach { idx ->
                    val level = when (idx) {
                        0 -> 800; 1 -> 400; 2 -> -200; 3 -> 400; 4 -> 800
                        else -> 300
                    }
                    setBandLevel(idx, level.toShort())
                }
            }
            "Pop" -> {
                setBassBoost(300)
                bands.indices.forEach { idx ->
                    val level = when (idx) {
                        0 -> -200; 1 -> 200; 2 -> 700; 3 -> 300; 4 -> -100
                        else -> 200
                    }
                    setBandLevel(idx, level.toShort())
                }
            }
            "Electronic" -> {
                setBassBoost(900)
                bands.indices.forEach { idx ->
                    val level = when (idx) {
                        0 -> 1000; 1 -> 700; 2 -> 0; 3 -> 500; 4 -> 900
                        else -> 400
                    }
                    setBandLevel(idx, level.toShort())
                }
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        isEnabled = false
    }
}
