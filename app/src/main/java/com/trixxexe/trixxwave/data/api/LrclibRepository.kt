package com.trixxexe.trixxwave.data.api

import com.trixxexe.trixxwave.data.db.LyricsCache
import com.trixxexe.trixxwave.data.db.LyricsDao
import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for fetching synchronized and plain lyrics from LRCLIB API as a fallback
 * when local or primary stream lyrics are absent, and caching them in Room DB.
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

        // 2. Query LRCLIB API endpoint (GET /api/get)
        val durationSec = (song.durationMs / 1000).toInt().takeIf { it > 0 }
        val albumParam = song.album.takeIf { it.isNotBlank() && it != "Unknown Album" }

        try {
            val response = lrclibService.getLyrics(
                trackName = song.title,
                artistName = song.artist,
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

            // Fallback: Search endpoint (GET /api/search)
            val searchRes = lrclibService.searchLyrics("${song.title} ${song.artist}")
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
}
