package com.trixxexe.trixxwave.data.api

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.trixxexe.trixxwave.data.db.Song
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

sealed class DownloadResult {
    data class Success(val song: Song, val filePath: String, val durationMs: Long) : DownloadResult()
    data class Failed(val errorMessage: String) : DownloadResult()
}

class YoutubeDownloadManager(
    private val context: Context,
    private val youtubeStreamRepository: YoutubeStreamRepository
) {
    private val tag = "YoutubeDownloadManager"
    @Volatile private var isYtdlInitialized = false

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun ensureYtdlInit() {
        if (!isYtdlInitialized) {
            synchronized(this) {
                if (!isYtdlInitialized) {
                    try {
                        try {
                            FFmpeg.getInstance().init(context.applicationContext)
                            Log.i(tag, "FFmpeg initialized successfully.")
                        } catch (e: Throwable) {
                            Log.w(tag, "FFmpeg init notice: ${e.message}")
                        }
                        YoutubeDL.getInstance().init(context.applicationContext)
                        isYtdlInitialized = true
                        Log.i(tag, "YoutubeDL initialized successfully for offline downloads.")
                    } catch (e: Throwable) {
                        Log.e(tag, "Failed to initialize YoutubeDL: ${e.message}", e)
                    }
                }
            }
        }
    }

    /**
     * Downloads an audio track to offline storage using yt-dlp (Seal engine) with multi-source fallback.
     */
    suspend fun downloadAudioTrack(
        song: Song,
        onProgress: (Float, String) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val videoUrl = song.originalUrl ?: song.filePath
        val extractedId = youtubeStreamRepository.extractVideoId(videoUrl)
        val videoId = extractedId ?: "track_${System.currentTimeMillis()}"
        val downloadDir = File(context.filesDir, "downloads").apply { mkdirs() }

        val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40).ifBlank { "track" }
        val finalFile = File(downloadDir, "${videoId}_${safeTitle}.m4a")

        Log.i(tag, "Starting download for '${song.title}' ($videoId)")
        onProgress(0.05f, "Initializing download engine...")

        var downloadedFile: File? = null

        // -------------------------------------------------------------
        // STRATEGY 1: yt-dlp (youtubedl-android / Seal engine)
        // -------------------------------------------------------------
        try {
            ensureYtdlInit()
            if (isYtdlInitialized) {
                Log.i(tag, "Attempting yt-dlp download for videoId/url: $videoUrl")
                onProgress(0.10f, "Connecting to yt-dlp engine...")

                val outputTemplate = "${downloadDir.absolutePath}/${videoId}_yt.%(ext)s"
                val targetUrl = if (videoUrl.startsWith("http")) videoUrl else "https://www.youtube.com/watch?v=$videoId"

                val request = YoutubeDLRequest(targetUrl).apply {
                    addOption("-f", "bestaudio/ba/b")
                    addOption("-x")
                    addOption("--audio-format", "m4a")
                    addOption("--audio-quality", "0")
                    addOption("-o", outputTemplate)
                    addOption("--no-playlist")
                    addOption("--no-mtime")
                    addOption("--concurrent-fragments", "4")
                    addOption("--extractor-args", "youtube:player_client=android,web")
                }

                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    val p = (progress / 100f).coerceIn(0.10f, 0.90f)
                    onProgress(p, "Downloading via yt-dlp (${progress.toInt()}%)")
                }

                // Search for output files matching videoId_yt.*
                val matchingFiles = downloadDir.listFiles { _, name ->
                    name.startsWith("${videoId}_yt.") && !name.endsWith(".part") && !name.endsWith(".ytdl")
                }
                val candidate = matchingFiles?.maxByOrNull { it.length() }
                if (candidate != null && candidate.exists() && candidate.length() > 50 * 1024L) {
                    downloadedFile = candidate
                    Log.i(tag, "yt-dlp download success! File: ${candidate.name}, size: ${candidate.length()}")
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "yt-dlp primary download failed: ${e.message}. Trying query search / stream fallback...", e)
        }

        // -------------------------------------------------------------
        // STRATEGY 2: yt-dlp via ytsearch1 query fallback
        // -------------------------------------------------------------
        if (downloadedFile == null && isYtdlInitialized) {
            try {
                Log.i(tag, "Attempting yt-dlp ytsearch fallback for '${song.title} ${song.artist}'")
                onProgress(0.20f, "Searching & fetching via yt-dlp...")

                val queryUrl = "ytsearch1:${song.title} ${song.artist}".trim()
                val outputTemplate = "${downloadDir.absolutePath}/${videoId}_search.%(ext)s"

                val request = YoutubeDLRequest(queryUrl).apply {
                    addOption("-f", "bestaudio/ba/b")
                    addOption("-x")
                    addOption("--audio-format", "m4a")
                    addOption("--audio-quality", "0")
                    addOption("-o", outputTemplate)
                    addOption("--no-playlist")
                    addOption("--no-mtime")
                    addOption("--extractor-args", "youtube:player_client=android,web")
                }

                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    val p = (progress / 100f).coerceIn(0.20f, 0.90f)
                    onProgress(p, "Downloading audio track (${progress.toInt()}%)")
                }

                val matchingFiles = downloadDir.listFiles { _, name ->
                    name.startsWith("${videoId}_search.") && !name.endsWith(".part") && !name.endsWith(".ytdl")
                }
                val candidate = matchingFiles?.maxByOrNull { it.length() }
                if (candidate != null && candidate.exists() && candidate.length() > 50 * 1024L) {
                    downloadedFile = candidate
                    Log.i(tag, "yt-dlp query download success! File: ${candidate.name}")
                }
            } catch (e: Throwable) {
                Log.w(tag, "yt-dlp search fallback failed: ${e.message}", e)
            }
        }

        // -------------------------------------------------------------
        // STRATEGY 3: Direct Stream Resolution (NewPipe + Innertube + Piped) + OkHttp
        // -------------------------------------------------------------
        if (downloadedFile == null) {
            Log.i(tag, "Attempting Direct Audio Stream resolution fallback...")
            onProgress(0.30f, "Resolving audio stream link...")

            val extracted = youtubeStreamRepository.extractAudioStream(
                urlOrQuery = videoUrl,
                songTitle = song.title,
                songArtist = song.artist,
                forceRefresh = true
            )
            var directStreamUrl = extracted?.streamUrl ?: song.streamUrl

            if (directStreamUrl.isNullOrBlank() || directStreamUrl.startsWith("/")) {
                // Try Cobalt / Piped direct API fetch
                directStreamUrl = fetchCobaltOrPipedAudioStream(videoId)
            }

            if (!directStreamUrl.isNullOrBlank() && !directStreamUrl.startsWith("/")) {
                val tempFile = File(downloadDir, "temp_${videoId}_direct.m4a")
                try {
                    val req = Request.Builder()
                        .url(directStreamUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "*/*")
                        .header("Connection", "keep-alive")
                        .build()

                    okHttpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful && response.body != null) {
                            val body = response.body!!
                            val totalBytes = body.contentLength()
                            var downloadedBytes = 0L

                            body.byteStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    val buffer = ByteArray(16384)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                        downloadedBytes += read
                                        val p = if (totalBytes > 0) {
                                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0.30f, 0.95f)
                                        } else {
                                            (((downloadedBytes / 102400L) % 10) + 1) / 10f
                                        }
                                        onProgress(p, "Saving audio file...")
                                    }
                                }
                            }

                            if (tempFile.exists() && tempFile.length() > 50 * 1024L) {
                                downloadedFile = tempFile
                                Log.i(tag, "Direct stream download success! File size: ${tempFile.length()}")
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(tag, "Direct stream download exception: ${e.message}", e)
                    if (tempFile.exists()) tempFile.delete()
                }
            }
        }

        val rawDownloadedFile = downloadedFile
        if (rawDownloadedFile == null || !rawDownloadedFile.exists()) {
            return@withContext DownloadResult.Failed("Download failed: Could not fetch audio stream for '${song.title}'.")
        }

        // -------------------------------------------------------------
        // VALIDATION & RENAMING
        // -------------------------------------------------------------
        onProgress(0.96f, "Validating audio integrity...")
        val validatedDurationMs = validateAudioFile(rawDownloadedFile)

        if (validatedDurationMs == null || validatedDurationMs <= 0L) {
            Log.e(tag, "File validation failed for ${rawDownloadedFile.name}. Size: ${rawDownloadedFile.length()}")
            if (rawDownloadedFile.exists()) rawDownloadedFile.delete()
            return@withContext DownloadResult.Failed("Validation failed: Downloaded file was incomplete or corrupt.")
        }

        // Rename candidate file to final destination file
        if (finalFile.exists()) finalFile.delete()
        val renameSuccess = rawDownloadedFile.renameTo(finalFile)
        val targetFile = if (renameSuccess) finalFile else rawDownloadedFile

        Log.i(tag, "Download successful! Final path: ${targetFile.absolutePath}, Duration: ${validatedDurationMs}ms, Size: ${targetFile.length()} bytes")
        onProgress(1.0f, "Completed!")

        val downloadedSong = song.copy(
            filePath = targetFile.absolutePath,
            source = "DOWNLOADED",
            streamUrl = targetFile.absolutePath,
            durationMs = validatedDurationMs
        )

        DownloadResult.Success(
            song = downloadedSong,
            filePath = targetFile.absolutePath,
            durationMs = validatedDurationMs
        )
    }

    private fun fetchCobaltOrPipedAudioStream(videoId: String): String? {
        // Try Cobalt API
        try {
            val json = JSONObject().apply {
                put("url", "https://www.youtube.com/watch?v=$videoId")
                put("downloadMode", "audio")
                put("audioFormat", "m4a")
            }
            val reqBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .post(reqBody)
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val obj = JSONObject(bodyStr)
                    val url = obj.optString("url", "")
                    if (url.startsWith("http")) return url
                }
            }
        } catch (_: Throwable) {}

        // Try Piped API
        try {
            val pipedUrls = listOf("https://pipedapi.kavin.rocks/streams/$videoId", "https://api.piped.video/streams/$videoId")
            for (pUrl in pipedUrls) {
                val req = Request.Builder()
                    .url(pUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string() ?: ""
                        val obj = JSONObject(bodyStr)
                        val audioStreams = obj.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            val streamUrl = audioStreams.getJSONObject(0).optString("url", "")
                            if (streamUrl.startsWith("http")) return streamUrl
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun validateAudioFile(file: File): Long? {
        if (!file.exists() || file.length() < 50 * 1024L) {
            return null
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs > 0L) durationMs else null
        } catch (e: Throwable) {
            Log.w(tag, "MediaMetadataRetriever validation exception: ${e.message}")
            null
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }
}
