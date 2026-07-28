package com.trixxexe.trixxwave.data.api

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
            .connectTimeout(20, TimeUnit.SECONDS)
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
     * Downloads an audio track to offline storage using yt-dlp with NewPipeExtractor fallback.
     * Uses temporary file names during download, and validates with MediaMetadataRetriever before renaming.
     */
    suspend fun downloadAudioTrack(
        song: Song,
        onProgress: (Float, String) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val videoUrl = song.originalUrl ?: song.filePath
        val videoId = youtubeStreamRepository.extractVideoId(videoUrl) ?: "track_${System.currentTimeMillis()}"
        val downloadDir = File(context.filesDir, "downloads").apply { mkdirs() }

        val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "track" }
        val tempFile = File(downloadDir, "temp_${videoId}_${System.currentTimeMillis()}.m4a")
        val finalFile = File(downloadDir, "${videoId}_${safeTitle}.m4a")

        Log.i(tag, "Starting download for '${song.title}' ($videoId)")
        onProgress(0.05f, "Initializing download engine...")

        var downloadSuccess = false

        // Primary Attempt: yt-dlp (youtubedl-android)
        try {
            ensureYtdlInit()
            if (isYtdlInitialized) {
                Log.i(tag, "Attempting download via yt-dlp (youtubedl-android)...")
                val request = YoutubeDLRequest(videoUrl).apply {
                    addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                    addOption("-o", tempFile.absolutePath)
                    addOption("--no-mtime")
                    addOption("--no-playlist")
                }
                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    val p = (progress / 100f).coerceIn(0.05f, 0.95f)
                    onProgress(p, "Downloading via yt-dlp (${progress.toInt()}%)")
                }
                downloadSuccess = tempFile.exists() && tempFile.length() > 50 * 1024L
            }
        } catch (e: Throwable) {
            Log.w(tag, "yt-dlp download failed: ${e.message}. Falling back to NewPipeExtractor direct stream.", e)
            downloadSuccess = false
        }

        // Secondary Fallback Attempt: NewPipeExtractor direct stream + OkHttp
        if (!downloadSuccess) {
            Log.i(tag, "Attempting fallback download via NewPipeExtractor direct audio stream...")
            onProgress(0.15f, "Resolving direct audio stream...")

            val extracted = youtubeStreamRepository.extractAudioStream(
                urlOrQuery = videoUrl,
                songTitle = song.title,
                songArtist = song.artist
            )
            val directStreamUrl = extracted?.streamUrl ?: song.streamUrl

            if (!directStreamUrl.isNullOrBlank() && !directStreamUrl.startsWith("/")) {
                try {
                    val req = Request.Builder()
                        .url(directStreamUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
                                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0.15f, 0.95f)
                                        } else {
                                            (((downloadedBytes / 102400L) % 10) + 1) / 10f
                                        }
                                        onProgress(p, "Downloading audio bytes...")
                                    }
                                }
                            }
                            downloadSuccess = tempFile.exists() && tempFile.length() > 50 * 1024L
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(tag, "NewPipeExtractor fallback download failed: ${e.message}", e)
                }
            }
        }

        if (!downloadSuccess || !tempFile.exists()) {
            if (tempFile.exists()) tempFile.delete()
            return@withContext DownloadResult.Failed("Download failed: Audio stream could not be fetched.")
        }

        // Strict Validation Step
        onProgress(0.96f, "Validating audio integrity...")
        val validatedDurationMs = validateAudioFile(tempFile)

        if (validatedDurationMs == null || validatedDurationMs <= 0L) {
            Log.e(tag, "File validation failed! File size=${tempFile.length()}, duration=$validatedDurationMs. Deleting corrupted temp file.")
            if (tempFile.exists()) tempFile.delete()
            return@withContext DownloadResult.Failed("Validation failed: Audio file was corrupt or incomplete.")
        }

        // Move/Rename temp file to final destination file
        if (finalFile.exists()) finalFile.delete()
        val renameSuccess = tempFile.renameTo(finalFile)
        val targetFile = if (renameSuccess) finalFile else tempFile

        Log.i(tag, "Download and validation successful! Final file: ${targetFile.absolutePath}, Duration: ${validatedDurationMs}ms, Size: ${targetFile.length()} bytes")
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

    private fun validateAudioFile(file: File): Long? {
        if (!file.exists() || file.length() < 50 * 1024L) { // Min 50KB
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
