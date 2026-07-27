package com.trixxexe.trixxwave.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.trixxexe.trixxwave.data.db.Playlist
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.db.TrixxWaveDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreScanner {

    private const val TAG = "MediaStoreScanner"

    suspend fun scanDeviceAudio(context: Context): Int = withContext(Dispatchers.IO) {
        val database = TrixxWaveDatabase.getDatabase(context)
        val songDao = database.songDao()
        val playlistDao = database.playlistDao()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        var addedCount = 0
        var totalFound = 0

        val existingPaths = songDao.getAllSongPaths().toHashSet()

        try {
            Log.d(TAG, "Starting MediaStore query on collection: $collection")
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    totalFound++
                    val mediaId = cursor.getLong(idColumn)
                    val rawFilePath = cursor.getString(dataColumn) ?: ""
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId).toString()

                    // O(1) check if already in DB by content URI or path
                    if (existingPaths.contains(contentUri) || (rawFilePath.isNotBlank() && existingPaths.contains(rawFilePath))) {
                        continue
                    }

                    val title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() }
                        ?: if (rawFilePath.isNotBlank()) File(rawFilePath).nameWithoutExtension else "Local Track #$mediaId"
                    val artist = cursor.getString(artistColumn).let {
                        if (it == null || it == "<unknown>") "Unknown Artist" else it
                    }
                    val album = cursor.getString(albumColumn).let {
                        if (it == null || it == "<unknown>") "Unknown Album" else it
                    }
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val albumArtUri = try {
                        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                        ContentUris.withAppendedId(sArtworkUri, albumId).toString()
                    } catch (e: Exception) {
                        null
                    }

                    val waveform = WaveformExtractor.extractWaveformPoints(rawFilePath.ifBlank { contentUri })

                    val song = Song(
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        filePath = contentUri, // Primary playback URI is content:// for Scoped Storage compatibility
                        albumArtUri = albumArtUri,
                        waveformPoints = waveform,
                        dateAdded = System.currentTimeMillis()
                    )
                    songDao.insertSong(song)
                    addedCount++
                }
            }
            Log.d(TAG, "MediaStore scan complete: found $totalFound tracks, added $addedCount new tracks")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore: ${e.message}", e)
        }

        // Initialize default smart playlists if none exist
        ensureDefaultPlaylists(playlistDao)
        ensureDemoTracks(songDao)

        addedCount
    }

    private suspend fun ensureDemoTracks(songDao: com.trixxexe.trixxwave.data.db.SongDao) {
        if (songDao.getSongCount() > 0) return
        val demoSongs = listOf(
            Song(
                title = "Sofasound x Kaiyo - Come On",
                artist = "Phuture Collective",
                album = "Audius Wave Essentials",
                durationMs = 210000L,
                filePath = "https://api.audius.co/v1/tracks/Evw5wAJ/stream?app_name=FloWave",
                albumArtUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                genre = "Cyberpunk / Synthwave",
                source = "DEMO_STREAM",
                streamUrl = "https://api.audius.co/v1/tracks/Evw5wAJ/stream?app_name=FloWave",
                waveformPoints = "0.2,0.5,0.8,0.9,0.7,0.4,0.8,0.9,0.6,0.3,0.7,0.9,0.5"
            ),
            Song(
                title = "Blinding Lights (Acoustic Ambient Flow)",
                artist = "Neon Dreamers",
                album = "Liquid Glass Collection",
                durationMs = 201000L,
                filePath = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/17/b4/8f/17b48f9a-0b93-6bb8-fe1d-3a16623c2cfb/mzaf_9560252727299052414.plus.aac.p.m4a",
                albumArtUri = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
                genre = "Synthwave",
                source = "DEMO_STREAM",
                streamUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/17/b4/8f/17b48f9a-0b93-6bb8-fe1d-3a16623c2cfb/mzaf_9560252727299052414.plus.aac.p.m4a",
                waveformPoints = "0.3,0.6,0.9,0.7,0.4,0.8,0.9,0.5,0.8,0.4,0.7,0.9,0.6"
            ),
            Song(
                title = "Cyberpunk Synthwave Drive",
                artist = "Hack Black",
                album = "Future Retro Wave",
                durationMs = 195000L,
                filePath = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview126/v4/81/a6/20/81a62065-8ffd-8e43-653f-0aebcd7ede8a/mzaf_10500050305620563985.plus.aac.p.m4a",
                albumArtUri = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=600&auto=format&fit=crop&q=80",
                genre = "Electro Synth",
                source = "DEMO_STREAM",
                streamUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview126/v4/81/a6/20/81a62065-8ffd-8e43-653f-0aebcd7ede8a/mzaf_10500050305620563985.plus.aac.p.m4a",
                waveformPoints = "0.4,0.7,0.8,0.6,0.9,0.8,0.5,0.7,0.9,0.6,0.8,0.5,0.7"
            )
        )
        songDao.insertSongs(demoSongs)
    }

    private suspend fun ensureDefaultPlaylists(playlistDao: com.trixxexe.trixxwave.data.db.PlaylistDao) {
        val existing = playlistDao.getAllPlaylists().firstOrNull()
        if (!existing.isNullOrEmpty()) return
        val defaultLists = listOf(
            Playlist(name = "Liked Songs", isAutoGenerated = true, description = "Your favorite tracks"),
            Playlist(name = "Cyberpunk Glass Mix", isAutoGenerated = true, description = "AI Curated Synth & Electro Wave"),
            Playlist(name = "Chill Ambient Flow", isAutoGenerated = true, description = "Relaxing acoustic & lofi melodies"),
            Playlist(name = "Midnight Focus Drive", isAutoGenerated = true, description = "High energy deep bass tracks")
        )
        for (pl in defaultLists) {
            playlistDao.insertPlaylist(pl)
        }
    }

    suspend fun importSingleFileUri(context: Context, uri: Uri): Song? = withContext(Dispatchers.IO) {
        val database = TrixxWaveDatabase.getDatabase(context)
        val songDao = database.songDao()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Imported Track"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Imported Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 180000L

            val filePath = uri.toString()
            val existing = songDao.getSongByPath(filePath)
            if (existing != null) return@withContext existing

            val waveform = WaveformExtractor.extractWaveformPoints(filePath)

            val song = Song(
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                filePath = filePath,
                waveformPoints = waveform,
                dateAdded = System.currentTimeMillis()
            )
            val newId = songDao.insertSong(song)
            songDao.getSongById(newId)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing file URI $uri: ${e.message}", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
