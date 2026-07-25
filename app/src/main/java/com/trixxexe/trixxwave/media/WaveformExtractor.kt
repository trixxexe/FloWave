package com.trixxexe.trixxwave.media

import java.io.File
import kotlin.math.abs
import kotlin.random.Random

object WaveformExtractor {

    /**
     * Generates a normalized amplitude string array (e.g. "20,45,89,34,90...")
     * using audio file sampling or deterministic file hash seed for track waveform visuals.
     */
    fun extractWaveformPoints(filePath: String, numPoints: Int = 80): String {
        return try {
            val file = File(filePath)
            if (file.exists() && file.length() > 0) {
                // Try reading byte samples from file if available to extract raw amplitude shape
                val bytesToRead = (numPoints * 32).coerceAtMost(file.length().toInt())
                val buffer = ByteArray(bytesToRead)
                file.inputStream().use { it.read(buffer) }

                val chunkSize = (bytesToRead / numPoints).coerceAtLeast(1)
                val points = IntArray(numPoints)
                for (i in 0 until numPoints) {
                    var sum = 0L
                    val start = i * chunkSize
                    val end = ((i + 1) * chunkSize).coerceAtMost(bytesToRead)
                    for (j in start until end) {
                        sum += abs(buffer[j].toInt())
                    }
                    val avg = if (end > start) sum / (end - start) else 30
                    val normalized = ((avg.toDouble() / 128.0) * 80 + 15).toInt().coerceIn(15, 95)
                    points[i] = normalized
                }
                points.joinToString(",")
            } else {
                generateDeterministicWaveform(filePath, numPoints)
            }
        } catch (e: Exception) {
            generateDeterministicWaveform(filePath, numPoints)
        }
    }

    private fun generateDeterministicWaveform(seedKey: String, numPoints: Int): String {
        val seed = abs(seedKey.hashCode().toLong()) + 42L
        val rng = Random(seed)

        val points = IntArray(numPoints)
        var current = 45
        for (i in 0 until numPoints) {
            val delta = rng.nextInt(-18, 19)
            current = (current + delta).coerceIn(15, 95)
            points[i] = current
        }
        return points.joinToString(",")
    }
}

