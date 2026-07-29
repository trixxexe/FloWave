package com.trixxexe.trixxwave.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class LrcLine(val timestampMs: Long, val text: String)

sealed class LyricsState {
    data object Loading : LyricsState()
    data class Success(val lines: List<LrcLine>, val isSynchronized: Boolean, val plainLyrics: String? = null) : LyricsState()
    data class Error(val message: String) : LyricsState()
}

class LyricsRepository(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    suspend fun fetchLyrics(title: String, artist: String, durationMs: Long, videoId: String) {
        _lyricsState.value = LyricsState.Loading
        withContext(Dispatchers.IO) {
            try {
                val lrclibUrl = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("track_name", title)
                    ?.addQueryParameter("artist_name", artist)
                    ?.addQueryParameter("duration", (durationMs / 1000).toString())
                    ?.build()
                    
                if (lrclibUrl != null) {
                    val request = Request.Builder().url(lrclibUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val lrcResponse = json.decodeFromString<LrcLibResponse>(body)
                                if (!lrcResponse.syncedLyrics.isNullOrEmpty()) {
                                    val lines = parseLrc(lrcResponse.syncedLyrics)
                                    _lyricsState.value = LyricsState.Success(lines, true, lrcResponse.plainLyrics)
                                    return@withContext
                                } else if (!lrcResponse.plainLyrics.isNullOrEmpty()) {
                                    _lyricsState.value = LyricsState.Success(emptyList(), false, lrcResponse.plainLyrics)
                                    return@withContext
                                }
                            }
                        }
                    }
                }
                
                val fallbackLyrics = fetchInnerTubeLyrics(videoId)
                if (fallbackLyrics != null) {
                    _lyricsState.value = LyricsState.Success(emptyList(), false, fallbackLyrics)
                } else {
                    _lyricsState.value = LyricsState.Error("Lyrics not found")
                }
            } catch (e: Exception) {
                _lyricsState.value = LyricsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun parseLrc(lrc: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        lrc.lines().forEach { line ->
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLong()
                val seconds = matchResult.groupValues[2].toLong()
                var msStr = matchResult.groupValues[3]
                if (msStr.length == 2) msStr += "0"
                val milliseconds = msStr.toLong()
                val text = matchResult.groupValues[4].trim()
                
                val totalMs = minutes * 60000 + seconds * 1000 + milliseconds
                lines.add(LrcLine(totalMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
    
    private fun fetchInnerTubeLyrics(videoId: String): String? {
        val payload = """
            {
                "context": {
                    "client": {
                        "clientName": "${InnerTubeConfig.WEB_NAME}",
                        "clientVersion": "${InnerTubeConfig.webVersion}"
                    }
                },
                "videoId": "$videoId"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/next")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            
            return if (body.contains("\"lyrics\":")) {
                val start = body.indexOf("\"lyrics\":") + 9
                val end = body.indexOf("\"", start + 1)
                if (start > 8 && end > start) {
                    body.substring(start, end).replace("\\n", "\n")
                } else null
            } else {
                null
            }
        }
    }
}

@Serializable
data class LrcLibResponse(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null
)
