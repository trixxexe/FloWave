package com.trixxexe.trixxwave.data.api

import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class InnerTubeRepository(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var url = fetchStreamUrl(videoId, "ANDROID_MUSIC", "7.02.52")
            if (url == null) {
                url = fetchStreamUrl(videoId, "WEB_REMIX", "1.20240108.01.00")
            }
            url ?: throw IOException("Could not extract stream URL for $videoId")
        }
    }

    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.isBlank()) return@runCatching getFeaturedSongs()
            
            val payload = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20240108.01.00"
                        }
                    },
                    "query": "$query"
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val results = mutableListOf<Song>()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    // Quick parse for videoIds in search response or fallback to featured list
                    val videoIdMatches = Regex(""""videoId":"([^"]+)"""").findAll(responseBody)
                    val titleMatches = Regex(""""text":"([^"]+)"""").findAll(responseBody)
                    
                    val foundIds = videoIdMatches.map { it.groupValues[1] }.distinct().take(10).toList()
                    val foundTitles = titleMatches.map { it.groupValues[1] }.filter { it.length > 2 && !it.startsWith("http") }.toList()

                    foundIds.forEachIndexed { index, vid ->
                        val title = foundTitles.getOrNull(index * 2) ?: "Track $query #${index + 1}"
                        val artist = foundTitles.getOrNull(index * 2 + 1) ?: "Artist $query"
                        results.add(
                            Song(
                                title = title,
                                artist = artist,
                                album = "YouTube Music",
                                albumArtUri = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
                                originalUrl = vid,
                                source = "InnerTube",
                                dateAdded = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            if (results.isEmpty()) {
                getFeaturedSongs().filter { 
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
                }.ifEmpty { getFeaturedSongs() }
            } else {
                results
            }
        }
    }

    fun getFeaturedSongs(): List<Song> {
        return listOf(
            Song(
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                albumArtUri = "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
                originalUrl = "4NRXx6U8ABQ",
                durationMs = 200000L,
                source = "InnerTube"
            ),
            Song(
                title = "Starboy",
                artist = "The Weeknd ft. Daft Punk",
                album = "Starboy",
                albumArtUri = "https://img.youtube.com/vi/34Na4j8AVgA/hqdefault.jpg",
                originalUrl = "34Na4j8AVgA",
                durationMs = 230000L,
                source = "InnerTube"
            ),
            Song(
                title = "Shape of You",
                artist = "Ed Sheeran",
                album = "÷ (Divide)",
                albumArtUri = "https://img.youtube.com/vi/JGwWNGJdvx8/hqdefault.jpg",
                originalUrl = "JGwWNGJdvx8",
                durationMs = 233000L,
                source = "InnerTube"
            ),
            Song(
                title = "Levitating",
                artist = "Dua Lipa",
                album = "Future Nostalgia",
                albumArtUri = "https://img.youtube.com/vi/TUVcZfQe-Kw/hqdefault.jpg",
                originalUrl = "TUVcZfQe-Kw",
                durationMs = 203000L,
                source = "InnerTube"
            ),
            Song(
                title = "As It Was",
                artist = "Harry Styles",
                album = "Harry's House",
                albumArtUri = "https://img.youtube.com/vi/H5v3kku4y6Q/hqdefault.jpg",
                originalUrl = "H5v3kku4y6Q",
                durationMs = 167000L,
                source = "InnerTube"
            )
        )
    }

    private fun fetchStreamUrl(videoId: String, clientName: String, clientVersion: String): String? {
        val payload = """
            {
                "context": {
                    "client": {
                        "clientName": "$clientName",
                        "clientVersion": "$clientVersion"
                    }
                },
                "videoId": "$videoId"
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val responseBody = response.body?.string() ?: return null
            val playerResponse = json.decodeFromString<PlayerResponse>(responseBody)
            
            val formats = playerResponse.streamingData?.adaptiveFormats ?: return null
            val audioFormats = formats.filter { 
                val mime = it.mimeType ?: ""
                (mime.startsWith("audio/webm") && mime.contains("opus")) ||
                (mime.startsWith("audio/mp4") && mime.contains("mp4a.40.2"))
            }
            
            return audioFormats.maxByOrNull { it.bitrate ?: 0 }?.url
        }
    }
}

@Serializable
data class PlayerResponse(
    val streamingData: StreamingData? = null
)

@Serializable
data class StreamingData(
    val adaptiveFormats: List<Format>? = null
)

@Serializable
data class Format(
    val itag: Int? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val bitrate: Int? = null
)

