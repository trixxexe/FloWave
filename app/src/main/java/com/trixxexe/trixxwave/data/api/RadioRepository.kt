package com.trixxexe.trixxwave.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.trixxexe.trixxwave.data.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class RadioStationDto(
    @Json(name = "stationuuid") val stationUuid: String,
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String,
    @Json(name = "url_resolved") val urlResolved: String? = null,
    @Json(name = "favicon") val favicon: String? = null,
    @Json(name = "tags") val tags: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "votes") val votes: Int? = 0,
    @Json(name = "codec") val codec: String? = null,
    @Json(name = "bitrate") val bitrate: Int? = 0
)

class RadioRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(Array<RadioStationDto>::class.java)

    suspend fun getTopStations(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://de1.api.radio-browser.info/json/stations/topvote/$limit"
        fetchStationsFromUrl(url)
    }

    suspend fun searchStations(query: String, limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getTopStations(limit)
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://de1.api.radio-browser.info/json/stations/search?name=$encoded&limit=$limit"
        fetchStationsFromUrl(url)
    }

    suspend fun getStationsByTag(tag: String, limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(tag, "UTF-8")
        val url = "https://de1.api.radio-browser.info/json/stations/search?tag=$encoded&limit=$limit"
        fetchStationsFromUrl(url)
    }

    private fun fetchStationsFromUrl(url: String): List<Song> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FloWave-Radio/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return emptyList()
                val dtos = adapter.fromJson(bodyStr) ?: return emptyList()
                dtos.mapIndexed { index, dto ->
                    val streamUrl = dto.urlResolved?.takeIf { it.isNotBlank() } ?: dto.url
                    Song(
                        id = -1000L - index - (dto.stationUuid.hashCode().toLong().and(0x7FFFFFFF)),
                        title = dto.name.ifBlank { "Radio Station" },
                        artist = if (!dto.country.isNullOrBlank()) dto.country else "Live Radio",
                        album = if (!dto.tags.isNullOrBlank()) dto.tags else "Radio Browser",
                        filePath = streamUrl,
                        albumArtUri = dto.favicon?.takeIf { it.startsWith("http") },
                        genre = dto.codec?.let { "$it ${dto.bitrate ?: 0}kbps" } ?: "Live Stream",
                        source = "RADIO",
                        streamUrl = streamUrl,
                        originalUrl = dto.url
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun String?.isNotNullOrBlank(): Boolean = this != null && this.isNotBlank()
    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
}
