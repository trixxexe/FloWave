package com.trixxexe.trixxwave.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class LrclibResponse(
    val id: Long?,
    val name: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

interface LrclibService {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSeconds: Int? = null
    ): Response<LrclibResponse>

    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LrclibResponse>>

    companion object {
        private const val BASE_URL = "https://lrclib.net/api/"

        fun create(): LrclibService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(LrclibService::class.java)
        }
    }
}
