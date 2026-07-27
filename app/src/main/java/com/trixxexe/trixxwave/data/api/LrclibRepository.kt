package com.trixxexe.trixxwave.data.api

import com.trixxexe.trixxwave.data.db.LyricsCache
import com.trixxexe.trixxwave.data.db.LyricsDao
import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for fetching synchronized and plain lyrics from LRCLIB API
 * with robust title/artist sanitization for YouTube/Online tracks.
 */
class LrclibRepository(
    private val lrclibService: LrclibService,
    private val lyricsDao: LyricsDao
) {
    suspend fun getOrFetchLyrics(song: Song): LyricsCache? = withContext(Dispatchers.IO) {
        // 1. Check local Room DB cache first
        val cached = lyricsDao.getLyricsForSong(song.id)
        if (cached != null && (!cached.syncedLrc.isNullOrBlank() || !cached.plainLyrics.isNullOrBlank())) {
            return@withContext cached
        }

        val cleanTitle = sanitizeTitle(song.title)
        val cleanArtist = sanitizeArtist(song.artist)

        val durationSec = (song.durationMs / 1000).toInt().takeIf { it > 0 }
        val albumParam = song.album.takeIf { it.isNotBlank() && it != "Unknown Album" && !it.contains("YouTube") }

        try {
            // 2. Query LRCLIB API endpoint (GET /api/get) with clean title
            val response = lrclibService.getLyrics(
                trackName = cleanTitle,
                artistName = cleanArtist,
                albumName = albumParam,
                durationSeconds = durationSec
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (!body.syncedLyrics.isNullOrBlank() || !body.plainLyrics.isNullOrBlank()) {
                    val lyrics = LyricsCache(
                        songId = song.id,
                        plainLyrics = body.plainLyrics,
                        syncedLrc = body.syncedLyrics,
                        source = "LRCLIB"
                    )
                    lyricsDao.insertLyrics(lyrics)
                    return@withContext lyrics
                }
            }

            // 3. Fallback: Search endpoint (GET /api/search)
            val searchQuery = "$cleanTitle $cleanArtist".trim()
            val searchRes = lrclibService.searchLyrics(searchQuery)
            if (searchRes.isSuccessful && searchRes.body() != null) {
                val results = searchRes.body()!!
                val bestMatch = results.firstOrNull {
                    !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank()
                }
                if (bestMatch != null) {
                    val lyrics = LyricsCache(
                        songId = song.id,
                        plainLyrics = bestMatch.plainLyrics,
                        syncedLrc = bestMatch.syncedLyrics,
                        source = "LRCLIB_SEARCH"
                    )
                    lyricsDao.insertLyrics(lyrics)
                    return@withContext lyrics
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext cached
    }

    private fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\(official\\s*(music)?\\s*(video|audio|lyric|lyrics)?\\)"), "")
            .replace(Regex("(?i)\\[official\\s*(music)?\\s*(video|audio|lyric|lyrics)?\\]"), "")
            .replace(Regex("(?i)\\b(official\\s*video|official\\s*audio|lyric\\s*video|4k|hd|remastered)\\b"), "")
            .replace(Regex("(?i)ft\\.|feat\\..*"), "")
            .trim()
    }

    private fun sanitizeArtist(artist: String): String {
        return artist
            .replace(Regex("(?i)\\b(VEVO|Topic|Official|YouTube Music)\\b"), "")
            .replace("- Topic", "")
            .trim()
    }
}

