package com.trixxexe.trixxwave.media

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.random.Random

object WaveformExtractor {

    /**
     * Generates a normalized amplitude string array (e.g. "20,45,89,34,90...")
     * using audio file sampling or deterministic file hash seed for track waveform visuals.
     * Parses ID3v2 header to skip embedded tags/artwork before sampling audio PCM bytes.
     */
    fun extractWaveformPoints(filePath: String, numPoints: Int = 80): String {
        return try {
            val file = File(filePath)
            if (file.exists() && file.length() > 1024) {
                val fileLength = file.length()
                
                // Parse ID3v2 header to skip tags and embedded artwork
                val id3Offset = parseId3v2HeaderSize(file)
                val audioStartOffset = id3Offset.toLong().coerceAtMost(fileLength - 512)
                val audioPayloadLength = fileLength - audioStartOffset

                if (audioPayloadLength <= 0) {
                    return generateDeterministicWaveform(filePath, numPoints)
                }

                val points = IntArray(numPoints)
                val sampleBlockSize = 64
                val stepSize = (audioPayloadLength / numPoints).coerceAtLeast(1L)
                val buffer = ByteArray(sampleBlockSize)

                RandomAccessFile(file, "r").use { raf ->
                    for (i in 0 until numPoints) {
                        val pos = audioStartOffset + (i * stepSize).coerceAtMost(audioPayloadLength - sampleBlockSize)
                        raf.seek(pos)
                        val bytesRead = raf.read(buffer, 0, sampleBlockSize)
                        
                        var sum = 0L
                        val count = if (bytesRead > 0) bytesRead else 1
                        for (j in 0 until count) {
                            sum += abs(buffer[j].toInt())
                        }
                        val avg = sum / count
                        val normalized = ((avg.toDouble() / 128.0) * 80 + 15).toInt().coerceIn(15, 95)
                        points[i] = normalized
                    }
                }
                points.joinToString(",")
            } else {
                generateDeterministicWaveform(filePath, numPoints)
            }
        } catch (e: Exception) {
            generateDeterministicWaveform(filePath, numPoints)
        }
    }

    private fun parseId3v2HeaderSize(file: File): Int {
        return try {
            if (file.length() < 10) return 0
            val header = ByteArray(10)
            file.inputStream().use { it.read(header, 0, 10) }
            if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                val size = ((header[6].toInt() and 0x7F) shl 21) or
                           ((header[7].toInt() and 0x7F) shl 14) or
                           ((header[8].toInt() and 0x7F) shl 7) or
                           (header[9].toInt() and 0x7F)
                size + 10
            } else 0
        } catch (_: Exception) {
            0
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
