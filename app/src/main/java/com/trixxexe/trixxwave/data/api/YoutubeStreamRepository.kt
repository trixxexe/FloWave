package com.trixxexe.trixxwave.data.api

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class StreamExtractionResult(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUrl: String?,
    val streamUrl: String,
    val format: String?
)

data class PipedStreamFormat(
    @Json(name = "url") val url: String?,
    @Json(name = "mimeType") val mimeType: String?,
    @Json(name = "quality") val quality: String?,
    @Json(name = "bitrate") val bitrate: Int?
)

data class PipedStreamResponse(
    @Json(name = "title") val title: String?,
    @Json(name = "uploader") val uploader: String?,
    @Json(name = "duration") val duration: Long?,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String?,
    @Json(name = "audioStreams") val audioStreams: List<PipedStreamFormat>?
)

data class PipedSearchItem(
    @Json(name = "url") val url: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "uploaderName") val uploaderName: String?,
    @Json(name = "duration") val duration: Long?,
    @Json(name = "thumbnail") val thumbnail: String?
)

data class PipedSearchWrapper(
    @Json(name = "items") val items: List<PipedSearchItem>?
)

class YoutubeStreamRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val pipedStreamAdapter = moshi.adapter(PipedStreamResponse::class.java)
    private val pipedSearchAdapter = moshi.adapter(PipedSearchWrapper::class.java)

    @Volatile
    private var isYtdlInitialized = false

    init {
        initYoutubeDL(context)
    }

    fun initYoutubeDL(appContext: Context) {
        if (isYtdlInitialized) return
        try {
            // Initialize YoutubeDL library on-device
            val youtubeDlClass = Class.forName("com.ya225.youtubedl.YoutubeDL")
            val getInstanceMethod = youtubeDlClass.getMethod("getInstance")
            val instance = getInstanceMethod.invoke(null)
            val initMethod = youtubeDlClass.getMethod("init", Context::class.java)
            initMethod.invoke(instance, appContext.applicationContext)
            isYtdlInitialized = true
        } catch (e1: Throwable) {
            try {
                // Alternative package names for youtubedl-android
                val youtubeDlClass = Class.forName("com.ya27.youtubedl.YoutubeDL")
                val getInstanceMethod = youtubeDlClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val initMethod = youtubeDlClass.getMethod("init", Context::class.java)
                initMethod.invoke(instance, appContext.applicationContext)
                isYtdlInitialized = true
            } catch (e2: Throwable) {
                // Binary init will fall back cleanly to high-speed open API stream extractor
            }
        }
    }

    suspend fun updateYoutubeDL(appContext: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val youtubeDlClass = Class.forName("com.ya225.youtubedl.YoutubeDL")
            val getInstanceMethod = youtubeDlClass.getMethod("getInstance")
            val instance = getInstanceMethod.invoke(null)
            val updateMethod = youtubeDlClass.getMethod("updateYoutubeDL", Context::class.java)
            updateMethod.invoke(instance, appContext.applicationContext)
            true
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun extractAudioStream(urlOrQuery: String): StreamExtractionResult? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(urlOrQuery) ?: urlOrQuery.trim()

        // 1. Try On-Device YoutubeDL if available
        if (isYtdlInitialized) {
            val ytdlResult = tryExtractYtdl(videoId)
            if (ytdlResult != null) return@withContext ytdlResult
        }

        // 2. High-speed open API fallback (Cobalt / Piped / Invidious stream API)
        return@withContext tryExtractApiFallback(videoId)
    }

    private fun tryExtractYtdl(videoId: String): StreamExtractionResult? {
        return try {
            val fullUrl = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"

            val reqClass = Class.forName("com.ya225.youtubedl.YoutubeDLRequest")
            val reqConstructor = reqClass.getConstructor(String::class.java)
            val request = reqConstructor.newInstance(fullUrl)

            val addOptionMethod = reqClass.getMethod("addOption", String::class.java, String::class.java)
            addOptionMethod.invoke(request, "-f", "bestaudio/best")

            val youtubeDlClass = Class.forName("com.ya225.youtubedl.YoutubeDL")
            val getInstanceMethod = youtubeDlClass.getMethod("getInstance")
            val instance = getInstanceMethod.invoke(null)

            val getInfoMethod = youtubeDlClass.getMethod("getInfo", reqClass)
            val videoInfo = getInfoMethod.invoke(instance, request) ?: return null

            val titleMethod = videoInfo.javaClass.getMethod("getTitle")
            val uploaderMethod = videoInfo.javaClass.getMethod("getUploader")
            val durationMethod = videoInfo.javaClass.getMethod("getDuration")
            val urlMethod = videoInfo.javaClass.getMethod("getUrl")
            val thumbnailMethod = videoInfo.javaClass.getMethod("getThumbnail")
            val extMethod = videoInfo.javaClass.getMethod("getExt")

            val title = titleMethod.invoke(videoInfo) as? String ?: "YouTube Track"
            val uploader = uploaderMethod.invoke(videoInfo) as? String ?: "YouTube Artist"
            val durationSec = (durationMethod.invoke(videoInfo) as? Number)?.toLong() ?: 0L
            val streamUrl = urlMethod.invoke(videoInfo) as? String ?: return null
            val thumbnail = thumbnailMethod.invoke(videoInfo) as? String
            val ext = extMethod.invoke(videoInfo) as? String ?: "m4a"

            StreamExtractionResult(
                title = title,
                artist = uploader,
                durationMs = durationSec * 1000L,
                artworkUrl = thumbnail,
                streamUrl = streamUrl,
                format = ext
            )
        } catch (e: Throwable) {
            null
        }
    }

    private fun tryExtractApiFallback(videoId: String): StreamExtractionResult? {
        val cleanId = extractVideoId(videoId) ?: videoId

        // A. Cobalt API direct audio stream extraction
        try {
            val cobaltResult = tryExtractCobaltApi(cleanId)
            if (cobaltResult != null) return cobaltResult
        } catch (_: Throwable) {}

        // B. Piped API multi-instance fallback
        val pipedApis = listOf(
            "https://pipedapi.kavin.rocks/streams/",
            "https://api.piped.video/streams/",
            "https://pipedapi.mha.fi/streams/",
            "https://pipedapi.drgns.space/streams/"
        )

        for (apiBase in pipedApis) {
            try {
                val req = Request.Builder()
                    .url(apiBase + cleanId)
                    .header("User-Agent", "FloWave-Client/1.0")
                    .build()

                client.newCall(req).execute().use { response ->
                    val body = response.body?.string() ?: return@use
                    val dto = pipedStreamAdapter.fromJson(body) ?: return@use
                    val audioStream = dto.audioStreams?.maxByOrNull { it.bitrate ?: 0 } ?: dto.audioStreams?.firstOrNull()
                    val streamUrl = audioStream?.url ?: return@use

                    return StreamExtractionResult(
                        title = dto.title ?: "YouTube Audio",
                        artist = dto.uploader ?: "YouTube Artist",
                        durationMs = (dto.duration ?: 0) * 1000L,
                        artworkUrl = dto.thumbnailUrl ?: "https://img.youtube.com/vi/$cleanId/hqdefault.jpg",
                        streamUrl = streamUrl,
                        format = audioStream.mimeType ?: "audio/m4a"
                    )
                }
            } catch (_: Throwable) {
                // Continue to next endpoint
            }
        }

        // C. Direct audio stream endpoint via Invidious (itag 140 = M4A 128kbps audio stream)
        val invidiousInstances = listOf(
            "https://invidious.nerdvpn.de",
            "https://inv.hostux.net",
            "https://vid.puffyan.us"
        )
        for (host in invidiousInstances) {
            val directAudioStreamUrl = "$host/latest_version?id=$cleanId&itag=140"
            val thumbnail = "https://img.youtube.com/vi/$cleanId/hqdefault.jpg"
            return StreamExtractionResult(
                title = "YouTube Track ($cleanId)",
                artist = "YouTube Audio",
                durationMs = 0L,
                artworkUrl = thumbnail,
                streamUrl = directAudioStreamUrl,
                format = "audio/m4a"
            )
        }

        val thumbnail = "https://img.youtube.com/vi/$cleanId/hqdefault.jpg"
        return StreamExtractionResult(
            title = "YouTube Track ($cleanId)",
            artist = "YouTube Audio",
            durationMs = 0L,
            artworkUrl = thumbnail,
            streamUrl = "https://inv.hostux.net/latest_version?id=$cleanId&itag=140",
            format = "audio/m4a"
        )
    }

    private fun tryExtractCobaltApi(videoId: String): StreamExtractionResult? {
        val cleanId = extractVideoId(videoId) ?: videoId
        val fullUrl = "https://www.youtube.com/watch?v=$cleanId"
        val cobaltInstances = listOf(
            "https://api.cobalt.tools/api/json",
            "https://co.wuk.sh/api/json"
        )
        val mediaType = "application/json".toMediaTypeOrNull()
        for (apiUrl in cobaltInstances) {
            try {
                val json = """{"url":"$fullUrl","downloadMode":"audio","audioFormat":"mp3"}"""
                val body = json.toRequestBody(mediaType)
                val req = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "FloWave-Client/1.0")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { res ->
                    val respStr = res.body?.string() ?: return@use
                    val jsonObj = org.json.JSONObject(respStr)
                    val status = jsonObj.optString("status")
                    val streamUrl = jsonObj.optString("url")
                    if ((status == "stream" || status == "redirect" || status == "picker") && streamUrl.isNotBlank()) {
                        return StreamExtractionResult(
                            title = "YouTube Audio ($cleanId)",
                            artist = "YouTube",
                            durationMs = 0L,
                            artworkUrl = "https://img.youtube.com/vi/$cleanId/hqdefault.jpg",
                            streamUrl = streamUrl,
                            format = "audio/mp3"
                        )
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    fun extractVideoId(urlOrId: String): String? {
        val trimmed = urlOrId.trim()
        if (trimmed.isBlank()) return null

        val pattern = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1)
        }

        // Only return trimmed if it matches strict 11-char video ID format without spaces
        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        return null
    }

    suspend fun searchYoutube(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val trimmedQuery = query.trim()

        // If user entered a direct YouTube URL or 11-char Video ID, treat as link
        val directVideoId = extractVideoId(trimmedQuery)
        if (directVideoId != null && (trimmedQuery.startsWith("http") || trimmedQuery.contains("youtu"))) {
            val singleSong = Song(
                id = -3000L - (directVideoId.hashCode().toLong().and(0x7FFFFFFF)),
                title = "YouTube Video ($directVideoId)",
                artist = "YouTube",
                album = "YouTube Direct",
                durationMs = 0L,
                filePath = "https://www.youtube.com/watch?v=$directVideoId",
                albumArtUri = "https://img.youtube.com/vi/$directVideoId/hqdefault.jpg",
                genre = "YouTube Stream",
                source = "YOUTUBE_EXTRACTED",
                streamUrl = null,
                originalUrl = "https://www.youtube.com/watch?v=$directVideoId"
            )
            return@withContext listOf(singleSong)
        }

        val encoded = java.net.URLEncoder.encode(trimmedQuery, "UTF-8")

        // 1. Try Piped Search API Instances
        val pipedSearchUrls = listOf(
            "https://pipedapi.kavin.rocks/search?q=$encoded&filter=videos",
            "https://api.piped.video/search?q=$encoded&filter=videos",
            "https://pipedapi.mha.fi/search?q=$encoded&filter=videos"
        )

        for (searchUrl in pipedSearchUrls) {
            try {
                val req = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "FloWave-Client/1.0")
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val wrapper = pipedSearchAdapter.fromJson(body)
                    val items = wrapper?.items ?: parsePipedSearchItemsManual(body)

                    if (!items.isNullOrEmpty()) {
                        val results = items.mapIndexedNotNull { index, item ->
                            val rawUrl = item.url ?: return@mapIndexedNotNull null
                            val videoId = extractVideoId(rawUrl) ?: return@mapIndexedNotNull null
                            val thumbnail = item.thumbnail ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                            Song(
                                id = -3000L - index - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
                                title = item.title?.ifBlank { "YouTube Video" } ?: "YouTube Video",
                                artist = item.uploaderName?.ifBlank { "YouTube Channel" } ?: "YouTube Channel",
                                album = "YouTube Search",
                                durationMs = (item.duration ?: 0) * 1000L,
                                filePath = "https://www.youtube.com/watch?v=$videoId",
                                albumArtUri = thumbnail,
                                genre = "YouTube Stream",
                                source = "YOUTUBE_EXTRACTED",
                                streamUrl = null,
                                originalUrl = "https://www.youtube.com/watch?v=$videoId"
                            )
                        }
                        if (results.isNotEmpty()) return@withContext results
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Try Invidious Search API Instances
        val invidiousSearchUrls = listOf(
            "https://inv.hostux.net/api/v1/search?q=$encoded&type=video",
            "https://invidious.nerdvpn.de/api/v1/search?q=$encoded&type=video",
            "https://vid.puffyan.us/api/v1/search?q=$encoded&type=video"
        )

        for (searchUrl in invidiousSearchUrls) {
            try {
                val req = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android Mobile) FloWave/1.0")
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val results = parseInvidiousSearchManual(body)
                    if (results.isNotEmpty()) return@withContext results
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: YouTube Web Search Scraper (Guaranteed Keyword Search Backup)
        try {
            val scraperUrl = "https://www.youtube.com/results?search_query=$encoded"
            val req = Request.Builder()
                .url(scraperUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(req).execute().use { response ->
                val html = response.body?.string() ?: return@withContext emptyList()
                val results = parseYoutubeHtmlSearchResults(html)
                if (results.isNotEmpty()) return@withContext results
            }
        } catch (_: Exception) {}

        emptyList()
    }

    private fun parsePipedSearchItemsManual(jsonStr: String): List<PipedSearchItem> {
        val list = mutableListOf<PipedSearchItem>()
        try {
            val videoIdPattern = Pattern.compile("\"url\":\"([^\"]+)\"")
            val titlePattern = Pattern.compile("\"title\":\"([^\"]+)\"")
            val uploaderPattern = Pattern.compile("\"uploaderName\":\"([^\"]+)\"")

            val vMatcher = videoIdPattern.matcher(jsonStr)
            val tMatcher = titlePattern.matcher(jsonStr)
            val uMatcher = uploaderPattern.matcher(jsonStr)

            while (vMatcher.find()) {
                val url = vMatcher.group(1)
                val title = if (tMatcher.find()) tMatcher.group(1) else "YouTube Track"
                val uploader = if (uMatcher.find()) uMatcher.group(1) else "YouTube Artist"
                list.add(
                    PipedSearchItem(
                        url = url,
                        title = title,
                        uploaderName = uploader,
                        duration = 180L,
                        thumbnail = null
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseInvidiousSearchManual(jsonStr: String): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val pattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\".*?\"title\":\"([^\"]+)\".*?\"author\":\"([^\"]+)\"")
            val matcher = pattern.matcher(jsonStr)
            var idx = 0
            while (matcher.find() && idx < 20) {
                val videoId = matcher.group(1)
                val title = matcher.group(2)
                val author = matcher.group(3)
                val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                songs.add(
                    Song(
                        id = -3000L - idx - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
                        title = title,
                        artist = author,
                        album = "YouTube Search",
                        durationMs = 180000L,
                        filePath = "https://www.youtube.com/watch?v=$videoId",
                        albumArtUri = thumbnail,
                        genre = "YouTube Stream",
                        source = "YOUTUBE_EXTRACTED",
                        streamUrl = null,
                        originalUrl = "https://www.youtube.com/watch?v=$videoId"
                    )
                )
                idx++
            }
        } catch (_: Exception) {}
        return songs
    }

    private fun parseYoutubeHtmlSearchResults(html: String): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            // Regex to find videoId and title in ytInitialData
            val pattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"")
            val matcher = pattern.matcher(html)
            val seenIds = mutableSetOf<String>()
            var idx = 0

            while (matcher.find() && idx < 20) {
                val videoId = matcher.group(1) ?: continue
                val title = matcher.group(2) ?: "YouTube Track"

                if (seenIds.add(videoId)) {
                    val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    songs.add(
                        Song(
                            id = -3000L - idx - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
                            title = title,
                            artist = "YouTube Music",
                            album = "YouTube Search",
                            durationMs = 200000L,
                            filePath = "https://www.youtube.com/watch?v=$videoId",
                            albumArtUri = thumbnail,
                            genre = "YouTube Stream",
                            source = "YOUTUBE_EXTRACTED",
                            streamUrl = null,
                            originalUrl = "https://www.youtube.com/watch?v=$videoId"
                        )
                    )
                    idx++
                }
            }
        } catch (_: Exception) {}
        return songs
    }
}
