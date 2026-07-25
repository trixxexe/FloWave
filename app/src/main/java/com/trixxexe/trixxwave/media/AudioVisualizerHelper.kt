package com.trixxexe.trixxwave.media

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class AudioVisualizerHelper {

    private var visualizer: Visualizer? = null
    private val _fftBands = MutableStateFlow(FloatArray(20) { 0.1f })
    val fftBands: StateFlow<FloatArray> = _fftBands.asStateFlow()

    private val _waveformPoints = MutableStateFlow(FloatArray(32) { 0f })
    val waveformPoints: StateFlow<FloatArray> = _waveformPoints.asStateFlow()

    private var simulationJob: Job? = null

    fun attachToAudioSession(audioSessionId: Int, scope: CoroutineScope, isPlaying: Boolean) {
        release()
        if (!isPlaying || audioSessionId == 0) {
            startSimulation(scope, isPlaying = false)
            return
        }

        try {
            val vis = Visualizer(audioSessionId)
            vis.captureSize = Visualizer.getCaptureSizeRange()[1]
            vis.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null || waveform.isEmpty()) return
                        val targetPoints = 32
                        val points = FloatArray(targetPoints)
                        val step = (waveform.size / targetPoints).coerceAtLeast(1)

                        for (i in 0 until targetPoints) {
                            val rawIndex = (i * step).coerceAtMost(waveform.size - 1)
                            // Convert byte (-128 to 127) or unsigned to normalized Float -1.0..1.0
                            val sample = (waveform[rawIndex].toInt() and 0xFF) - 128
                            points[i] = (sample / 128f).coerceIn(-1f, 1f)
                        }
                        _waveformPoints.value = points
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null || fft.isEmpty()) return
                        val numBands = 20
                        val bandValues = FloatArray(numBands)
                        val chunkSize = (fft.size / 2) / numBands

                        for (i in 0 until numBands) {
                            var sum = 0f
                            val start = i * chunkSize
                            val end = (start + chunkSize).coerceAtMost(fft.size / 2)
                            for (j in start until end) {
                                val rIndex = j * 2
                                val iIndex = j * 2 + 1
                                if (iIndex < fft.size) {
                                    val re = fft[rIndex].toFloat()
                                    val im = fft[iIndex].toFloat()
                                    sum += hypot(re.toDouble(), im.toDouble()).toFloat()
                                }
                            }
                            val avg = if (end > start) sum / (end - start) else 0f
                            bandValues[i] = (avg / 128f).coerceIn(0.05f, 1f)
                        }
                        _fftBands.value = bandValues
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
            )
            vis.enabled = true
            visualizer = vis
        } catch (e: Exception) {
            Log.w("AudioVisualizerHelper", "Hardware Visualizer failed, fallback to smooth simulation: ${e.message}")
            startSimulation(scope, isPlaying = true)
        }
    }

    fun startSimulation(scope: CoroutineScope, isPlaying: Boolean) {
        simulationJob?.cancel()
        if (!isPlaying) {
            _fftBands.value = FloatArray(20) { 0.08f }
            _waveformPoints.value = FloatArray(32) { 0f }
            return
        }
        simulationJob = scope.launch(Dispatchers.Default) {
            val currentFft = FloatArray(20) { Random.nextFloat() * 0.4f + 0.1f }
            var phase = 0f
            while (isPlaying) {
                // Simulate FFT
                for (i in currentFft.indices) {
                    val target = Random.nextFloat() * 0.85f + 0.1f
                    currentFft[i] = currentFft[i] * 0.6f + target * 0.4f
                }
                _fftBands.value = currentFft.copyOf()

                // Simulate Waveform PCM sinusoid
                phase += 0.25f
                val currentWave = FloatArray(32) { idx ->
                    val angle = (idx * 0.3f) + phase
                    (kotlin.math.sin(angle.toDouble()) * 0.75 + (Random.nextFloat() - 0.5f) * 0.2f).toFloat().coerceIn(-1f, 1f)
                }
                _waveformPoints.value = currentWave

                delay(60)
            }
        }
    }

    fun release() {
        simulationJob?.cancel()
        simulationJob = null
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        visualizer = null
        _fftBands.value = FloatArray(20) { 0.05f }
        _waveformPoints.value = FloatArray(32) { 0f }
    }
}
