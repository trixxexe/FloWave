package com.trixxexe.trixxwave.data.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
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

data class CachedStreamEntry(
    val result: StreamExtractionResult,
    val timestampMs: Long = System.currentTimeMillis(),
    val expireTimeSec: Long
) {
    fun isExpired(): Boolean {
        val currentSec = System.currentTimeMillis() / 1000L
        return currentSec >= (expireTimeSec - 300L) // Expire 5 mins before TTL
    }
}

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

class NewPipeOkHttpDownloader(private val client: OkHttpClient) : Downloader() {
    override fun execute(request: NewPipeRequest): NewPipeResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = Request.Builder().url(url)

        headers?.forEach { (headerName, headerValues) ->
            headerValues?.forEach { headerValue ->
                builder.addHeader(headerName, headerValue)
            }
        }

        if (httpMethod.equals("POST", ignoreCase = true)) {
            val body = dataToSend?.let { it.toRequestBody(null) } ?: ByteArray(0).toRequestBody(null)
            builder.post(body)
        } else {
            builder.get()
        }

        val response = client.newCall(builder.build()).execute()
        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers.values(name)
        }

        val bodyString = response.body?.string()
        return NewPipeResponse(
            response.code,
            response.message,
            responseHeaders,
            bodyString,
            response.request.url.toString()
        )
    }
}

class YoutubeStreamRepository(private val context: Context) {

    private val TAG = "YoutubeStreamRepo"

    private val streamCache = ConcurrentHashMap<String, CachedStreamEntry>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fastClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val pipedStreamAdapter = moshi.adapter(PipedStreamResponse::class.java)
    private val pipedSearchAdapter = moshi.adapter(PipedSearchWrapper::class.java)

    @Volatile
    private var isNewPipeInitialized = false

    init {
        initNewPipe()
    }

    private fun initNewPipe() {
        if (isNewPipeInitialized) return
        synchronized(this) {
            if (!isNewPipeInitialized) {
                try {
                    NewPipe.init(NewPipeOkHttpDownloader(fastClient), Localization.DEFAULT)
                    isNewPipeInitialized = true
                    Log.d(TAG, "NewPipeExtractor initialized successfully with OkHttp downloader.")
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to initialize NewPipeExtractor: ${e.message}", e)
                }
            }
        }
    }

    suspend fun updateYoutubeDL(appContext: Context): Boolean = true

    private fun parseUrlExpiration(streamUrl: String): Long {
        return try {
            val uri = android.net.Uri.parse(streamUrl)
            val expire = uri.getQueryParameter("expire")
            if (expire != null) {
                expire.toLong()
            } else {
                (System.currentTimeMillis() / 1000L) + 7200L // Default 2 hours
            }
        } catch (_: Exception) {
            (System.currentTimeMillis() / 1000L) + 7200L
        }
    }

    private fun cacheResult(id: String, result: StreamExtractionResult) {
        val expiry = parseUrlExpiration(result.streamUrl)
        streamCache[id] = CachedStreamEntry(
            result = result,
            expireTimeSec = expiry
        )
    }

    fun invalidateCacheForId(urlOrId: String) {
        val cleanId = extractVideoId(urlOrId) ?: urlOrId.trim()
        streamCache.remove(cleanId)
        Log.d(TAG, "Invalidated stream cache for '$cleanId'")
    }

    suspend fun extractAudioStream(
        urlOrQuery: String,
        songTitle: String? = null,
        songArtist: String? = null,
        forceRefresh: Boolean = false
    ): StreamExtractionResult? = withContext(Dispatchers.IO) {
        val cleanId = extractVideoId(urlOrQuery) ?: urlOrQuery.trim()

        if (!forceRefresh) {
            streamCache[cleanId]?.let { cached ->
                if (!cached.isExpired() && cached.result.streamUrl.isNotBlank()) {
                    Log.d(TAG, "[Cache Hit] Returning valid cached streamUrl for '$cleanId'")
                    return@withContext cached.result
                } else {
                    Log.d(TAG, "[Cache Expired] Cached entry for '$cleanId' is expired. Re-extracting...")
                    streamCache.remove(cleanId)
                }
            }
        } else {
            streamCache.remove(cleanId)
        }

        Log.d(TAG, "Starting audio stream extraction for videoId/query: '$cleanId', title: '$songTitle', artist: '$songArtist'")

        val cleanArtist = songArtist?.replace("YouTube Music", "", ignoreCase = true)?.replace("YouTube", "", ignoreCase = true)?.trim()
        val rawTitle = (songTitle ?: urlOrQuery)
            .replace("https://www.youtube.com/watch?v=", "")
            .replace("https://youtu.be/", "")
            .replace("Official Music Video", "", ignoreCase = true)
            .replace("Official Video", "", ignoreCase = true)
            .replace("Official Audio", "", ignoreCase = true)
            .replace("HD", "", ignoreCase = true)
            .replace("4K", "", ignoreCase = true)
            .replace("Lyric Video", "", ignoreCase = true)
            .replace("Lyrics", "", ignoreCase = true)
            .replace("-", " ")
            .replace("_", " ")
            .trim()

        val searchTerms = when {
            !rawTitle.isBlank() && !cleanArtist.isNullOrBlank() -> "$rawTitle $cleanArtist"
            !rawTitle.isBlank() -> rawTitle
            else -> cleanId
        }

        // 1. PRIMARY ENGINE: On-Device NewPipeExtractor
        if (cleanId.length == 11) {
            val newPipeResult = tryExtractNewPipe(cleanId)
            if (newPipeResult != null && newPipeResult.streamUrl.isNotBlank()) {
                Log.d(TAG, "[Primary Success - NewPipeExtractor] Extracted audio stream for videoId '$cleanId'")
                cacheResult(cleanId, newPipeResult)
                return@withContext newPipeResult
            } else {
                Log.w(TAG, "[Primary Engine Failed] NewPipeExtractor returned null for '$cleanId'. Running fallbacks...")
            }
        }

        // 2. FALLBACK 1: YouTube Music Innertube API
        if (cleanId.length == 11) {
            val youtubeiResult = tryExtractYoutubeiApi(cleanId)
            if (youtubeiResult != null && youtubeiResult.streamUrl.isNotBlank()) {
                Log.d(TAG, "[Fallback Success - Youtubei API] Extracted streamUrl for videoId '$cleanId'")
                cacheResult(cleanId, youtubeiResult)
                return@withContext youtubeiResult
            }
        }

        // 3. FALLBACK 2: Invidious API
        if (cleanId.length == 11) {
            val invidiousResult = tryExtractInvidiousApi(cleanId)
            if (invidiousResult != null && invidiousResult.streamUrl.isNotBlank()) {
                Log.d(TAG, "[Fallback Success - Invidious API] Extracted streamUrl for videoId '$cleanId'")
                cacheResult(cleanId, invidiousResult)
                return@withContext invidiousResult
            }
        }

        // 4. FALLBACK 3: Piped API
        if (cleanId.length == 11) {
            val pipedResult = tryExtractPipedApi(cleanId)
            if (pipedResult != null && pipedResult.streamUrl.isNotBlank()) {
                Log.d(TAG, "[Fallback Success - Piped API] Extracted streamUrl for videoId '$cleanId'")
                cacheResult(cleanId, pipedResult)
                return@withContext pipedResult
            }
        }

        // 5. FALLBACK 4: Cobalt API
        if (cleanId.length == 11) {
            val cobaltResult = tryExtractCobaltApi(cleanId)
            if (cobaltResult != null && cobaltResult.streamUrl.isNotBlank()) {
                Log.d(TAG, "[Fallback Success - Cobalt API] Extracted streamUrl for videoId '$cleanId'")
                cacheResult(cleanId, cobaltResult)
                return@withContext cobaltResult
            }
        }

        // 6. FALLBACK 5: iTunes Global Preview Engine
        val itunesResult = tryExtractItunesFallback(searchTerms)
        if (itunesResult != null && itunesResult.streamUrl.isNotBlank()) {
            Log.d(TAG, "[Fallback Success - iTunes Preview] Audio stream extracted for '$searchTerms'")
            cacheResult(cleanId, itunesResult)
            return@withContext itunesResult
        }

        // 7. FALLBACK 6: Audius Engine
        val audiusResult = tryExtractAudiusFallback(searchTerms)
        if (audiusResult != null && audiusResult.streamUrl.isNotBlank()) {
            Log.d(TAG, "[Fallback Success - Audius Engine] Stream extracted for '$searchTerms'")
            cacheResult(cleanId, audiusResult)
            return@withContext audiusResult
        }

        Log.e(TAG, "[Failure] All extraction paths exhausted for target '$cleanId'")
        null
    }

    private fun tryExtractNewPipe(videoId: String): StreamExtractionResult? {
        return try {
            initNewPipe()
            val videoUrl = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)

            val audioStreams: List<AudioStream> = streamInfo.audioStreams ?: emptyList()
            if (audioStreams.isEmpty()) return null

            val bestAudio = audioStreams
                .filter { !it.url.isNullOrBlank() }
                .maxByOrNull { it.averageBitrate.takeIf { b -> b > 0 } ?: it.bitrate }
                ?: audioStreams.firstOrNull { !it.url.isNullOrBlank() }

            val streamUrl = bestAudio?.url ?: return null

            val title = streamInfo.name ?: "YouTube Track"
            val uploader = streamInfo.uploaderName ?: "YouTube Artist"
            val durationMs = streamInfo.duration * 1000L
            val artworkUrl = streamInfo.thumbnails?.lastOrNull()?.url ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            val format = bestAudio.format?.mimeType ?: "audio/m4a"

            Log.d(TAG, "[NewPipeExtractor] Stream resolved: '$title' by '$uploader', format: $format, bitrate: ${bestAudio.averageBitrate}")

            StreamExtractionResult(
                title = title,
                artist = uploader,
                durationMs = durationMs,
                artworkUrl = artworkUrl,
                streamUrl = streamUrl,
                format = format
            )
        } catch (e: Throwable) {
            Log.w(TAG, "NewPipeExtractor extraction exception for '$videoId': ${e.message}", e)
            null
        }
    }

    private fun tryExtractYoutubeiApi(videoId: String): StreamExtractionResult? {
        val playerUrls = listOf(
            "https://music.youtube.com/youtubei/v1/player",
            "https://www.youtube.com/youtubei/v1/player"
        )
        val mediaType = "application/json".toMediaTypeOrNull()

        val clients = listOf(
            Triple("ANDROID_MUSIC", "6.42.52", "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 13; en_US)"),
            Triple("WEB_REMIX", "1.20240422.01.00", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"),
            Triple("IOS", "19.29.1", "com.google.ios.youtube/19.29.1 (iPhone14,3; U; CPU iOS 17_5_1 like Mac OS X; en_US)")
        )

        for (endpoint in playerUrls) {
            for ((clientName, clientVersion, userAgent) in clients) {
                try {
                    val payload = JSONObject().apply {
                        put("videoId", videoId)
                        put("context", JSONObject().apply {
                            put("client", JSONObject().apply {
                                put("clientName", clientName)
                                put("clientVersion", clientVersion)
                            })
                        })
                    }.toString()

                    val req = Request.Builder()
                        .url(endpoint)
                        .header("User-Agent", userAgent)
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

                        var bestStreamUrl: String? = null
                        var highestBitrate = 0
                        var bestMime = "audio/mp4"

                        if (adaptiveFormats != null) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val fmt = adaptiveFormats.getJSONObject(i)
                                val mimeType = fmt.optString("mimeType")
                                val url = fmt.optString("url")
                                val bitrate = fmt.optInt("bitrate", 0)

                                if (mimeType.contains("audio") && url.isNotBlank() && bitrate > highestBitrate) {
                                    highestBitrate = bitrate
                                    bestStreamUrl = url
                                    bestMime = mimeType
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
                    Log.w(TAG, "Youtubei endpoint $endpoint client $clientName failed: ${e.message}")
                }
            }
        }
        return null
    }

    private fun tryExtractInvidiousApi(videoId: String): StreamExtractionResult? {
        val cleanId = extractVideoId(videoId) ?: videoId
        val invidiousInstances = listOf(
            "https://invidious.flokinet.to",
            "https://invidious.nerdvpn.de",
            "https://inv.tux.pizza"
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

                            if (type.startsWith("audio/") && url.isNotBlank() && bitrate > highestBitrate) {
                                highestBitrate = bitrate
                                bestAudioUrl = url
                                bestFormat = type
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
            "https://api.piped.video/streams/"
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
                    .post(body)
                    .build()

                fastClient.newCall(req).execute().use { res ->
                    val respStr = res.body?.string() ?: return@use
                    val jsonObj = JSONObject(respStr)
                    val status = jsonObj.optString("status")
                    val streamUrl = jsonObj.optString("url")
                    if ((status == "stream" || status == "redirect" || status == "tunnel") && streamUrl.isNotBlank()) {
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

        // 1. Primary Search Engine: NewPipeExtractor
        val newPipeResults = trySearchNewPipe(trimmedQuery)
        if (newPipeResults.isNotEmpty()) {
            Log.d(TAG, "[Primary Search Success - NewPipeExtractor] Found ${newPipeResults.size} tracks for '$trimmedQuery'")
            return@withContext newPipeResults
        }

        // 2. Fallback Search Engine: Piped
        val encoded = java.net.URLEncoder.encode(trimmedQuery, "UTF-8")
        val pipedSearchUrls = listOf(
            "https://pipedapi.kavin.rocks/search?q=$encoded&filter=videos",
            "https://api.piped.video/search?q=$encoded&filter=videos"
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
                    val items = wrapper?.items ?: emptyList()

                    if (items.isNotEmpty()) {
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

        // 3. Fallback Search Engine: iTunes Search API
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

    private fun trySearchNewPipe(query: String): List<Song> {
        return try {
            initNewPipe()
            val searchExtractor = ServiceList.YouTube.getSearchExtractor(query, listOf("videos"), "")
            searchExtractor.fetchPage()
            val items = searchExtractor.initialPage?.items ?: emptyList()

            val songs = mutableListOf<Song>()
            for (item in items) {
                if (item is StreamInfoItem) {
                    val videoUrl = item.url ?: continue
                    val videoId = extractVideoId(videoUrl) ?: continue
                    val title = item.name ?: "YouTube Track"
                    val uploader = item.uploaderName ?: "YouTube Music"
                    val durationSec = item.duration
                    val durationMs = if (durationSec > 0) durationSec * 1000L else 200000L
                    val thumbnail = item.thumbnails?.lastOrNull()?.url ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    songs.add(
                        Song(
                            id = -3000000000L - (videoId.hashCode().toLong().and(0x7FFFFFFF)),
                            title = title,
                            artist = uploader,
                            album = "YouTube Search",
                            durationMs = durationMs,
                            filePath = "https://www.youtube.com/watch?v=$videoId",
                            albumArtUri = thumbnail,
                            genre = "YouTube Stream",
                            source = "YOUTUBE_EXTRACTED",
                            streamUrl = null,
                            originalUrl = "https://www.youtube.com/watch?v=$videoId"
                        )
                    )
                }
            }
            songs
        } catch (e: Throwable) {
            Log.w(TAG, "NewPipe search failed for '$query': ${e.message}")
            emptyList()
        }
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
