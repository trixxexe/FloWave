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
import java.net.URLEncoder
import java.io.IOException

class InnerTubeRepository(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getStreamUrl(videoIdOrQuery: String, title: String = "", artist: String = ""): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. If it is already a direct HTTP/HTTPS stream link, return it
            if (videoIdOrQuery.startsWith("http://") || videoIdOrQuery.startsWith("https://")) {
                println("[A3 Stream Resolver] Direct stream URL provided: $videoIdOrQuery")
                return@runCatching videoIdOrQuery
            }

            // 2. Try InnerTube Player API with dynamic STS and configurable client identities (Full tracks)
            try {
                InnerTubeConfig.fetchDynamicSts(client)
            } catch (e: Exception) {
                // Ignore dynamic STS fetch error, fallback to default
            }

            var url = fetchInnerTubeStream(videoIdOrQuery, InnerTubeConfig.ANDROID_MUSIC_NAME, InnerTubeConfig.androidMusicVersion)
            if (url == null) {
                url = fetchInnerTubeStream(videoIdOrQuery, InnerTubeConfig.WEB_REMIX_NAME, InnerTubeConfig.webRemixVersion)
            }
            if (url == null) {
                url = fetchInnerTubeStream(videoIdOrQuery, InnerTubeConfig.IOS_NAME, InnerTubeConfig.iosVersion)
            }
            if (url != null) {
                println("[A3 Stream Resolver] Resolved InnerTube direct full audio stream for ID $videoIdOrQuery: $url")
                return@runCatching url
            }

            // 3. Try Invidious / Piped Mirrors for YouTube VideoId (Full tracks)
            val invidiousStream = fetchInvidiousStream(videoIdOrQuery)
            if (invidiousStream != null) {
                println("[A3 Stream Resolver] Resolved Invidious webm/opus audio stream for ID $videoIdOrQuery: $invidiousStream")
                return@runCatching invidiousStream
            }

            // 4. Try JioSaavn direct fallback if title is present (Full tracks)
            if (title.isNotEmpty()) {
                val saavnStream = fetchSaavnStream(title, artist)
                if (saavnStream != null) {
                    println("[A3 Stream Resolver] Resolved JioSaavn audio stream for '$title': $saavnStream")
                    return@runCatching saavnStream
                }
            }

            throw IOException("Could not extract stream URL for $videoIdOrQuery")
        }
    }

    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.isBlank()) return@runCatching getFeaturedSongs()

            val combinedResults = mutableListOf<Song>()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            // Provider A: JioSaavn Search
            try {
                val saavnSongs = searchJioSaavn(encodedQuery, query)
                combinedResults.addAll(saavnSongs)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Provider B: iTunes Search
            try {
                val itunesSongs = searchiTunes(encodedQuery, query)
                combinedResults.addAll(itunesSongs)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Provider C: InnerTube Search
            try {
                val innerTubeSongs = searchInnerTube(query)
                combinedResults.addAll(innerTubeSongs)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Deduplicate by title & artist
            val distinctResults = combinedResults.distinctBy { 
                "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" 
            }

            if (distinctResults.isEmpty()) {
                getFeaturedSongs().filter { 
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
                }.ifEmpty { getFeaturedSongs() }
            } else {
                distinctResults
            }
        }
    }

    private fun searchJioSaavn(encodedQuery: String, rawQuery: String): List<Song> {
        val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&p=1&n=15&q=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            if (!body.contains("\"results\":")) return emptyList()

            val list = mutableListOf<Song>()
            val parsed = json.decodeFromString<SaavnSearchResponse>(body)
            parsed.results?.forEach { item ->
                val title = item.song?.replace("&quot;", "\"")?.replace("&amp;", "&") ?: "Track"
                val artist = item.primary_artists ?: item.singers ?: "Artist"
                val album = item.album ?: "JioSaavn Music"
                val artwork = item.image?.replace("150x150", "500x500")?.replace("50x50", "500x500")
                val previewUrl = item.media_preview_url
                val songId = item.id ?: "saavn_${System.currentTimeMillis()}"

                list.add(
                    Song(
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtUri = artwork,
                        originalUrl = previewUrl ?: songId,
                        durationMs = (item.duration?.toLongOrNull() ?: 180L) * 1000L,
                        source = "JioSaavn",
                        dateAdded = System.currentTimeMillis()
                    )
                )
            }
            return list
        }
    }

    private fun searchiTunes(encodedQuery: String, rawQuery: String): List<Song> {
        val url = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=15"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()

            val list = mutableListOf<Song>()
            val parsed = json.decodeFromString<ItunesSearchResponse>(body)
            parsed.results?.forEach { item ->
                val title = item.trackName ?: "Track"
                val artist = item.artistName ?: "Artist"
                val album = item.collectionName ?: "Apple Music"
                val artwork = item.artworkUrl100?.replace("100x100bb", "600x600bb")
                val streamUrl = item.previewUrl ?: ""

                if (streamUrl.isNotEmpty()) {
                    list.add(
                        Song(
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtUri = artwork,
                            originalUrl = streamUrl,
                            durationMs = item.trackTimeMillis ?: 180000L,
                            source = "iTunes",
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                }
            }
            return list
        }
    }

    private fun searchInnerTube(query: String): List<Song> {
        val payload = InnerTubeConfig.buildSearchPayload(query)

        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/search")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val results = mutableListOf<Song>()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
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
        return results
    }

    private fun fetchItunesStream(title: String, artist: String): String? {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val parsed = json.decodeFromString<ItunesSearchResponse>(body)
                parsed.results?.firstOrNull()?.previewUrl
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchSaavnStream(title: String, artist: String): String? {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&p=1&n=1&q=$query"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val parsed = json.decodeFromString<SaavnSearchResponse>(body)
                parsed.results?.firstOrNull()?.media_preview_url
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchInvidiousStream(videoId: String): String? {
        val mirrors = listOf(
            "https://y.com.sb",
            "https://invidious.nerdvpn.de",
            "https://inv.tux.pizza"
        )
        for (domain in mirrors) {
            try {
                val request = Request.Builder()
                    .url("$domain/api/v1/videos/$videoId")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val inv = json.decodeFromString<InvidiousResponse>(body)
                        val audioUrl = inv.adaptiveFormats?.firstOrNull { it.type?.contains("audio") == true }?.url
                        if (!audioUrl.isNullOrEmpty()) return audioUrl
                    }
                }
            } catch (e: Exception) {
                // Continue to next mirror
            }
        }
        return null
    }

    private fun fetchInnerTubeStream(videoId: String, clientName: String, clientVersion: String): String? {
        val payload = InnerTubeConfig.buildPlayerPayload(
            videoId = videoId,
            clientName = clientName,
            clientVersion = clientVersion
        )

        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
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

                audioFormats.maxByOrNull { it.bitrate ?: 0 }?.url
            }
        } catch (e: Exception) {
            null
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

@Serializable
data class ItunesSearchResponse(
    val resultCount: Int? = null,
    val results: List<ItunesTrack>? = null
)

@Serializable
data class ItunesTrack(
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackTimeMillis: Long? = null
)

@Serializable
data class SaavnSearchResponse(
    val results: List<SaavnTrack>? = null
)

@Serializable
data class SaavnTrack(
    val id: String? = null,
    val song: String? = null,
    val album: String? = null,
    val primary_artists: String? = null,
    val singers: String? = null,
    val image: String? = null,
    val media_preview_url: String? = null,
    val duration: String? = null
)

@Serializable
data class InvidiousResponse(
    val adaptiveFormats: List<InvidiousFormat>? = null
)

@Serializable
data class InvidiousFormat(
    val url: String? = null,
    val type: String? = null,
    val bitrate: String? = null
)
