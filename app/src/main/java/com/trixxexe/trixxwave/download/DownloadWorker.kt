package com.trixxexe.trixxwave.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val client = OkHttpClient()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("url") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Unknown Title"
        val artist = inputData.getString("artist") ?: "Unknown Artist"
        val album = inputData.getString("album") ?: "Unknown Album"
        val artworkUrl = inputData.getString("artworkUrl")
        val isWebm = inputData.getBoolean("isWebm", false)
        val videoId = inputData.getString("videoId") ?: "unknown"

        val extension = if (isWebm) "webm" else "m4a"
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val filename = "${safeTitle}_${safeArtist}.$extension"

        createNotificationChannel()
        setForeground(createForegroundInfo(title, 0, videoId.hashCode()))

        try {
            // Download audio file to cache first
            val tempFile = File(context.cacheDir, filename)
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")
                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()
                var bytesCopied = 0L

                body.source().use { source ->
                    tempFile.sink().buffer().use { sink ->
                        var bytes = source.read(sink.buffer, 8192)
                        var lastReportTime = System.currentTimeMillis()
                        while (bytes != -1L) {
                            bytesCopied += bytes
                            sink.emit()
                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 500) {
                                val progress = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else 0
                                setForeground(createForegroundInfo(title, progress, videoId.hashCode()))
                                setProgress(workDataOf("progress" to progress, "videoId" to videoId))
                                lastReportTime = now
                            }
                            bytes = source.read(sink.buffer, 8192)
                        }
                    }
                }
            }

            // Tagging
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.setField(FieldKey.TITLE, title)
                tag.setField(FieldKey.ARTIST, artist)
                tag.setField(FieldKey.ALBUM, album)

                if (artworkUrl != null) {
                    val artRequest = Request.Builder().url(artworkUrl).build()
                    client.newCall(artRequest).execute().use { artResponse ->
                        val artBytes = artResponse.body?.bytes()
                        if (artBytes != null) {
                            val artwork = ArtworkFactory.getNew()
                            artwork.binaryData = artBytes
                            artwork.mimeType = "image/jpeg"
                            tag.setField(artwork)
                        }
                    }
                }
                audioFile.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Move to MediaStore
            val mimeType = if (isWebm) "audio/webm" else "audio/mp4"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.ALBUM, album)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/FloWave")
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            // Update local Room database so the downloaded song appears in Downloaded tab
            try {
                val db = com.trixxexe.trixxwave.data.db.TrixxWaveDatabase.getDatabase(context)
                val existing = db.songDao().getSongByTitleAndArtist(title, artist)
                if (existing != null) {
                    db.songDao().insertSong(existing.copy(filePath = uri.toString()))
                } else {
                    db.songDao().insertSong(
                        com.trixxexe.trixxwave.data.db.Song(
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtUri = artworkUrl,
                            filePath = uri.toString(),
                            source = "Downloaded",
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            tempFile.delete()
            setProgress(workDataOf("progress" to 100, "videoId" to videoId))
            Result.success(workDataOf("uri" to uri.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    private fun createForegroundInfo(title: String, progress: Int, notificationId: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Downloading $title")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
