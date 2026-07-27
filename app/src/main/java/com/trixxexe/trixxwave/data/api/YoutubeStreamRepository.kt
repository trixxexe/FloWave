package com.trixxexe.trixxwave.data.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trixxexe.trixxwave.data.db.Song
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import org.json.JSONObject

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

    private val TAG = "YoutubeStreamRepo"

    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, StreamExtractionResult>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fastClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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
            YoutubeDL.getInstance().init(appContext.applicationContext)
            isYtdlInitialized = true
            Log.d(TAG, "On-device YoutubeDL initialized successfully.")
        } catch (e: Throwable) {
            Log.w(TAG, "On-device YoutubeDL binary init bypassed; using fast direct stream extraction APIs. Reason: ${e.message}")
        }
    }

    suspend fun updateYoutubeDL(appContext: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().updateYoutubeDL(appContext.applicationContext)
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun isStreamUrlPlayable(streamUrl: String): Boolean {
        if (streamUrl.isBlank()) return false
        return try {
            val req = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Range", "bytes=0-1024")
                .build()

            fastClient.newCall(req).execute().use { response ->
                val code = response.code
                val contentType = response.header("Content-Type") ?: ""
                val isPlayable = (code in 200..399) && !contentType.contains("html", ignoreCase = true)
                Log.d(TAG, "isStreamUrlPlayable for ${streamUrl.take(60)}... code=$code, type=$contentType -> $isPlayable")
                isPlayable
            }
        } catch (e: Throwable) {
            Log.w(TAG, "isStreamUrlPlayable check failed for ${streamUrl.take(60)}...: ${e.message}")
            false
        }
    }

    suspend fun extractAudioStream(urlOrQuery: String, songTitle: String? = null, songArtist: String? = null): StreamExtractionResult? = withContext(Dispatchers.IO) {
        val cleanId = extractVideoId(urlOrQuery) ?: urlOrQuery.trim()
        
        // Instant Cache Hit (< 1ms)
        streamCache[cleanId]?.let { cached ->
            if (isStreamUrlPlayable(cached.streamUrl)) {
                Log.d(TAG, "[Cache Hit] Returning verified streamUrl for '$cleanId'")
                return@withContext cached
            } else {
                streamCache.remove(cleanId)
            }
        }

        Log.d(TAG, "Starting audio stream extraction for videoId/query: '$cleanId', title: '$songTitle', artist: '$songArtist'")

        // 1. Try Cobalt API (Fast direct MP3/M4A stream, high compatibility)
        val cobaltResult = tryExtractCobaltApi(cleanId)
        if (cobaltResult != null && cobaltResult.streamUrl.isNotBlank() && isStreamUrlPlayable(cobaltResult.streamUrl)) {
            Log.d(TAG, "[Success] Cobalt API extracted verified streamUrl: ${cobaltResult.streamUrl.take(80)}...")
            streamCache[cleanId] = cobaltResult
            return@withContext cobaltResult
        }

        // 2. Try Invidious API (Proxied audio stream, bypasses 403)
        val invidiousResult = tryExtractInvidiousApi(cleanId)
        if (invidiousResult != null && invidiousResult.streamUrl.isNotBlank() && isStreamUrlPlayable(invidiousResult.streamUrl)) {
            Log.d(TAG, "[Success] Invidious API extracted verified streamUrl: ${invidiousResult.streamUrl.take(80)}...")
            streamCache[cleanId] = invidiousResult
            return@withContext invidiousResult
        }

        // 3. Try Piped API
        val pipedResult = tryExtractPipedApi(cleanId)
        if (pipedResult != null && pipedResult.streamUrl.isNotBlank() && isStreamUrlPlayable(pipedResult.streamUrl)) {
            Log.d(TAG, "[Success] Piped API extracted verified streamUrl: ${pipedResult.streamUrl.take(80)}...")
            streamCache[cleanId] = pipedResult
            return@withContext pipedResult
        }

        // 4. Try Direct Youtubei Player API (Verified playable)
        if (cleanId.length == 11) {
            val youtubeiResult = tryExtractYoutubeiApi(cleanId)
            if (youtubeiResult != null && youtubeiResult.streamUrl.isNotBlank() && isStreamUrlPlayable(youtubeiResult.streamUrl)) {
                Log.d(TAG, "[Success] Youtubei API extracted verified streamUrl: ${youtubeiResult.streamUrl.take(80)}...")
                streamCache[cleanId] = youtubeiResult
                return@withContext youtubeiResult
            }
        }

        // 5. Try Native YoutubeDL / yt-dlp extractor
        if (cleanId.length == 11) {
            val ytdlResult = tryExtractYtdl(cleanId)
            if (ytdlResult != null && ytdlResult.streamUrl.isNotBlank() && isStreamUrlPlayable(ytdlResult.streamUrl)) {
                Log.d(TAG, "[Success] YoutubeDL extracted verified streamUrl: ${ytdlResult.streamUrl.take(80)}...")
                streamCache[cleanId] = ytdlResult
                return@withContext ytdlResult
            }
        }

        // 6. Build clean search query for Full Track Fallback
        val searchTerms = when {
            !songTitle.isNullOrBlank() && !songArtist.isNullOrBlank() -> "$songTitle $songArtist"
            !songTitle.isNullOrBlank() -> songTitle
            else -> urlOrQuery
                .replace("https://www.youtube.com/watch?v=", "")
                .replace("https://youtu.be/", "")
                .replace("-", " ")
                .replace("_", " ")
        }

        // 7. Try Audius Search API Fallback (Full length tracks, NOT 30s clips)
        val audiusResult = tryExtractAudiusFallback(searchTerms)
        if (audiusResult != null && audiusResult.streamUrl.isNotBlank() && isStreamUrlPlayable(audiusResult.streamUrl)) {
            Log.d(TAG, "[Success] Audius fallback extracted verified streamUrl: ${audiusResult.streamUrl.take(80)}...")
            return@withContext audiusResult
        }

        Log.e(TAG, "[Failure] All stream extraction endpoints failed for target '$cleanId'")
        null
    }

    private fun tryExtractYoutubeiApi(videoId: String): StreamExtractionResult? {
        val playerUrls = listOf(
            "https://www.youtube.com/youtubei/v1/player",
            "https://music.youtube.com/youtubei/v1/player"
        )
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonPayloads = listOf(
            """{"videoId":"$videoId","context":{"client":{"clientName":"ANDROID","clientVersion":"19.08.35","androidSdkVersion":34}}}""",
            """{"videoId":"$videoId","context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20231214.01.00"}}}""",
            """{"videoId":"$videoId","context":{"client":{"clientName":"IOS","clientVersion":"19.08.35","osName":"iOS"}}}"""
        )

        for (endpoint in playerUrls) {
            for (payload in jsonPayloads) {
                try {
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .header("Content-Type", "application/json")
                        .post(payload.toRequestBody(mediaType))
                        .build()

                    fastClient.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body?.string() ?: return@use
                        val jsonObj = JSONObject(body)

                        val videoDetails = jsonObj.optJSONObject("videoDetails")
                        val title = videoDetails?.optString("title") ?: "YouTube Audio Track"
                        val author = videoDetails?.optString("author") ?: "YouTube Artist"
                        val lengthSeconds = videoDetails?.optLong("lengthSeconds", 0L) ?: 0L

                        val streamingData = jsonObj.optJSONObject("streamingData") ?: return@use
                        val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                        val formats = streamingData.optJSONArray("formats")

                        var bestStreamUrl: String? = null
                        var highestBitrate = 0
                        var bestMime = "audio/mp4"

                        if (adaptiveFormats != null) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val fmt = adaptiveFormats.getJSONObject(i)
                                val mimeType = fmt.optString("mimeType")
                                val url = fmt.optString("url")
                                val bitrate = fmt.optInt("bitrate", 0)

                                if (mimeType.contains("audio") && url.isNotBlank()) {
                                    if (bitrate > highestBitrate) {
                                        highestBitrate = bitrate
                                        bestStreamUrl = url
                                        bestMime = mimeType
                                    }
                                }
                            }
                        }

                        if (bestStreamUrl.isNullOrBlank() && formats != null) {
                            for (i in 0 until formats.length()) {
                                val fmt = formats.getJSONObject(i)
                                val url = fmt.optString("url")
                                if (url.isNotBlank()) {
                                    bestStreamUrl = url
                                    break
                                }
                            }
                        }

                        if (!bestStreamUrl.isNullOrBlank()) {
                            return StreamExtractionResult(
                                title = title,
                                artist = author,
                                durationMs = lengthSeconds * 1000L,
                                artworkUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                                streamUrl = bestStreamUrl,
                                format = bestMime
                            )
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Youtubei endpoint $endpoint failed: ${e.message}")
                }
            }
        }
        return null
    }

    private fun tryExtractYtdl(videoId: String): StreamExtractionResult? {
        return try {
            val fullUrl = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"
            val request = YoutubeDLRequest(fullUrl).apply {
                addOption("-f", "bestaudio/best")
            }
            val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(request) ?: return null

            val title = videoInfo.title ?: "YouTube Track"
            val uploader = videoInfo.uploader ?: "YouTube Artist"
            val durationSec = videoInfo.duration.toLong()
            val streamUrl = videoInfo.url ?: return null
            val thumbnail = videoInfo.thumbnail
            val ext = videoInfo.ext ?: "m4a"

            StreamExtractionResult(
                title = title,
                artist = uploader,
                durationMs = durationSec * 1000L,
                artworkUrl = thumbnail,
                streamUrl = streamUrl,
                format = ext
            )
        } catch (e: Throwable) {
            Log.w(TAG, "YTDL extraction attempted and bypassed: ${e.message}")
            null
        }
    }

    private fun tryExtractInvidiousApi(videoId: String): StreamExtractionResult? {
        val cleanId = extractVideoId(videoId) ?: videoId
        val invidiousInstances = listOf(
            "https://inv.hostux.net",
            "https://invidious.nerdvpn.de",
            "https://vid.puffyan.us",
            "https://invidious.drgns.space",
            "https://invidious.projectsegfau.lt"
        )

        for (host in invidiousInstances) {
            try {
                val req = Request.Builder()
                    .url("$host/api/v1/videos/$cleanId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                fastClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val jsonObj = JSONObject(body)

                    val title = jsonObj.optString("title", "YouTube Track")
                    val author = jsonObj.optString("author", "YouTube Artist")
                    val lengthSeconds = jsonObj.optLong("lengthSeconds", 0L)

                    val adaptiveFormats = jsonObj.optJSONArray("adaptiveFormats")
                    if (adaptiveFormats != null) {
                        var bestAudioUrl: String? = null
                        var highestBitrate = 0
                        var bestFormat = "audio/m4a"

                        for (i in 0 until adaptiveFormats.length()) {
                            val fmt = adaptiveFormats.getJSONObject(i)
                            val type = fmt.optString("type")
                            val url = fmt.optString("url")
                            val bitrate = fmt.optInt("bitrate", 0)

                            if (type.startsWith("audio/") && url.isNotBlank()) {
                                if (bitrate > highestBitrate) {
                                    highestBitrate = bitrate
                                    bestAudioUrl = url
                                    bestFormat = type
                                }
                            }
                        }

                        if (!bestAudioUrl.isNullOrBlank()) {
                            return StreamExtractionResult(
                                title = title,
                                artist = author,
                                durationMs = lengthSeconds * 1000L,
                                artworkUrl = "https://img.youtube.com/vi/$cleanId/hqdefault.jpg",
                                streamUrl = bestAudioUrl,
                                format = bestFormat
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Invidious endpoint $host failed: ${e.message}")
            }
        }
        return null
    }

    private fun tryExtractPipedApi(videoId: String): StreamExtractionResult? {
        val cleanId = extractVideoId(videoId) ?: videoId
        val pipedApis = listOf(
            "https://pipedapi.kavin.rocks/streams/",
            "https://api.piped.video/streams/",
            "https://pipedapi.mha.fi/streams/",
            "https://pipedapi.drgns.space/streams/",
            "https://pipedapi.col2.metube.fr/streams/",
            "https://pipedapi.astral.sh/streams/"
        )

        for (apiBase in pipedApis) {
            try {
                val req = Request.Builder()
                    .url(apiBase + cleanId)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                fastClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
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
            } catch (e: Throwable) {
                Log.w(TAG, "Piped endpoint $apiBase failed: ${e.message}")
            }
        }
        return null
    }

    private fun tryExtractCobaltApi(videoId: String): StreamExtractionResult? {
        val cleanId = extractVideoId(videoId) ?: videoId
        val fullUrl = "https://www.youtube.com/watch?v=$cleanId"
        val cobaltInstances = listOf(
            "https://api.cobalt.tools",
            "https://co.wuk.sh"
        )
        val mediaType = "application/json".toMediaTypeOrNull()
        for (baseUrl in cobaltInstances) {
            try {
                val json = """{"url":"$fullUrl","downloadMode":"audio","audioFormat":"mp3"}"""
                val body = json.toRequestBody(mediaType)
                val req = Request.Builder()
                    .url(baseUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(body)
                    .build()

                fastClient.newCall(req).execute().use { res ->
                    val respStr = res.body?.string() ?: return@use
                    val jsonObj = JSONObject(respStr)
                    val status = jsonObj.optString("status")
                    val streamUrl = jsonObj.optString("url")
                    if ((status == "stream" || status == "redirect" || status == "picker" || status == "tunnel") && streamUrl.isNotBlank()) {
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
            } catch (e: Throwable) {
                Log.w(TAG, "Cobalt endpoint $baseUrl failed: ${e.message}")
            }
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

        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        return null
    }

    suspend fun searchYoutube(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val trimmedQuery = query.trim()

        val directVideoId = extractVideoId(trimmedQuery)
        if (directVideoId != null && (trimmedQuery.startsWith("http") || trimmedQuery.contains("youtu"))) {
            val singleSong = Song(
                id = -3000000000L - (directVideoId.hashCode().toLong().and(0x7FFFFFFF)),
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
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val wrapper = pipedSearchAdapter.fromJson(body)
                    val items = wrapper?.items ?: parsePipedSearchItemsManual(body)

                    if (!items.isNullOrEmpty()) {
                        val results = items.mapIndexedNotNull { _, item ->
                            val rawUrl = item.url ?: return@mapIndexedNotNull null
                            val videoId = extractVideoId(rawUrl) ?: return@mapIndexedNotNull null
                            val thumbnail = item.thumbnail ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                            Song(
                                id = -3000000000L - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
                                title = item.title?.ifBlank { "YouTube Track" } ?: "YouTube Track",
                                artist = item.uploaderName?.ifBlank { "YouTube Music" } ?: "YouTube Music",
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
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val results = parseInvidiousSearchManual(body)
                    if (results.isNotEmpty()) return@withContext results
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: YouTube Web Search Scraper
        try {
            val scraperUrl = "https://www.youtube.com/results?search_query=$encoded"
            val req = Request.Builder()
                .url(scraperUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(req).execute().use { response ->
                val html = response.body?.string()
                if (!html.isNullOrBlank()) {
                    val results = parseYoutubeHtmlSearchResults(html)
                    if (results.isNotEmpty()) return@withContext results
                }
            }
        } catch (_: Exception) {}

        // 4. Fallback: iTunes Search API for guaranteed music results
        try {
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=15")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val js = JSONObject(jsonStr)
            val resultsArr = js.optJSONArray("results")
            if (resultsArr != null && resultsArr.length() > 0) {
                val itunesSongs = mutableListOf<Song>()
                for (i in 0 until resultsArr.length()) {
                    val r = resultsArr.getJSONObject(i)
                    val streamUrl = r.optString("previewUrl")
                    if (streamUrl.isNotBlank()) {
                        val trackName = r.optString("trackName", trimmedQuery)
                        val artistName = r.optString("artistName", "iTunes Stream")
                        val art = r.optString("artworkUrl100").replace("100x100bb", "600x600bb")
                        val id = -4000000000L - (r.optLong("trackId", i.toLong()).and(0x7FFFFFFF))

                        itunesSongs.add(
                            Song(
                                id = id,
                                title = trackName,
                                artist = artistName,
                                album = r.optString("collectionName", "iTunes Track"),
                                durationMs = r.optLong("trackTimeMillis", 180000L),
                                filePath = streamUrl,
                                albumArtUri = art.ifBlank { null },
                                genre = r.optString("primaryGenreName", "Online Stream"),
                                source = "YOUTUBE_EXTRACTED",
                                streamUrl = streamUrl,
                                originalUrl = streamUrl
                            )
                        )
                    }
                }
                if (itunesSongs.isNotEmpty()) return@withContext itunesSongs
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
                        id = -3000000000L - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
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
                            id = -3000000000L - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
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

    private fun tryExtractItunesFallback(query: String): StreamExtractionResult? {
        return try {
            val cleanQuery = query
                .replace("https://www.youtube.com/watch?v=", "")
                .replace("https://youtu.be/", "")
                .replace("Official Music Video", "", ignoreCase = true)
                .replace("Official Audio", "", ignoreCase = true)
                .replace("HD", "", ignoreCase = true)
                .replace("4K", "", ignoreCase = true)
                .trim()
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val js = JSONObject(jsonStr)
            val results = js.optJSONArray("results") ?: return null
            if (results.length() > 0) {
                val r = results.getJSONObject(0)
                val streamUrl = r.optString("previewUrl")
                if (streamUrl.isNotBlank()) {
                    val art = r.optString("artworkUrl100").replace("100x100bb", "600x600bb")
                    return StreamExtractionResult(
                        title = r.optString("trackName", cleanQuery),
                        artist = r.optString("artistName", "iTunes Stream"),
                        streamUrl = streamUrl,
                        artworkUrl = art.ifBlank { "https://img.youtube.com/vi/$query/hqdefault.jpg" },
                        durationMs = r.optLong("trackTimeMillis", 180000L),
                        format = "audio/m4a"
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "iTunes fallback error: ${e.message}")
            null
        }
    }

    private fun tryExtractAudiusFallback(query: String): StreamExtractionResult? {
        return try {
            val cleanQuery = query.replace("https://www.youtube.com/watch?v=", "").replace("https://youtu.be/", "").trim()
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val url = URL("https://api.audius.co/v1/tracks/search?query=$encoded&app_name=FloWave")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val js = JSONObject(jsonStr)
            val data = js.optJSONArray("data") ?: return null
            if (data.length() > 0) {
                val r = data.getJSONObject(0)
                val trackId = r.optString("id")
                if (trackId.isNotBlank()) {
                    val streamUrl = "https://api.audius.co/v1/tracks/$trackId/stream?app_name=FloWave"
                    val user = r.optJSONObject("user")
                    val art = r.optJSONObject("artwork")?.optString("1000x1000") ?: ""
                    return StreamExtractionResult(
                        title = r.optString("title", cleanQuery),
                        artist = user?.optString("name") ?: "Audius Stream",
                        streamUrl = streamUrl,
                        artworkUrl = art.ifBlank { "https://img.youtube.com/vi/$query/hqdefault.jpg" },
                        durationMs = r.optLong("duration", 180L) * 1000L,
                        format = "audio/mp3"
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Audius fallback error: ${e.message}")
            null
        }
    }
}
