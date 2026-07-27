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

data class AudiusUserDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "handle") val handle: String? = null
)

data class AudiusArtworkDto(
    @Json(name = "150x150") val small: String? = null,
    @Json(name = "480x480") val medium: String? = null,
    @Json(name = "1000x1000") val large: String? = null
)

data class AudiusTrackDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "user") val user: AudiusUserDto? = null,
    @Json(name = "artwork") val artwork: AudiusArtworkDto? = null,
    @Json(name = "duration") val duration: Int? = 0,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "mood") val mood: String? = null
)

data class AudiusResponseWrapper(
    @Json(name = "data") val data: List<AudiusTrackDto>? = null
)

class AudiusRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(AudiusResponseWrapper::class.java)

    suspend fun getTrendingTracks(): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://api.audius.co/v1/tracks/trending?app_name=FloWave"
        fetchAudiusFromUrl(url)
    }

    suspend fun searchTracks(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getTrendingTracks()
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.audius.co/v1/tracks/search?query=$encoded&app_name=FloWave"
        fetchAudiusFromUrl(url)
    }

    private fun fetchAudiusFromUrl(url: String): List<Song> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FloWave-Audius/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return emptyList()
                val wrapper = adapter.fromJson(bodyStr) ?: return emptyList()
                val dtos = wrapper.data ?: return emptyList()

                dtos.map { dto ->
                    val streamUrl = "https://api.audius.co/v1/tracks/${dto.id}/stream?app_name=FloWave"
                    val artUri = dto.artwork?.medium ?: dto.artwork?.large ?: dto.artwork?.small
                    Song(
                        id = -2000000000L - (dto.id.hashCode().toLong().and(0x7FFFFFFF)),
                        title = dto.title.ifBlank { "Audius Track" },
                        artist = dto.user?.name?.ifBlank { "Audius Artist" } ?: "Audius Artist",
                        album = dto.genre ?: "Audius Music",
                        durationMs = (dto.duration ?: 0) * 1000L,
                        filePath = streamUrl,
                        albumArtUri = artUri,
                        genre = dto.genre,
                        moodTags = dto.mood,
                        source = "AUDIUS",
                        streamUrl = streamUrl,
                        originalUrl = "https://audius.co/tracks/${dto.id}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
