package com.trixxexe.trixxwave.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class InnerTubeRepository(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var url = fetchStreamUrl(videoId, "ANDROID_MUSIC", "7.02.52")
            if (url == null) {
                url = fetchStreamUrl(videoId, "WEB_REMIX", "1.20240108.01.00")
            }
            url ?: throw IOException("Could not extract stream URL for $videoId")
        }
    }

    private fun fetchStreamUrl(videoId: String, clientName: String, clientVersion: String): String? {
        val payload = """
            {
                "context": {
                    "client": {
                        "clientName": "$clientName",
                        "clientVersion": "$clientVersion"
                    }
                },
                "videoId": "$videoId"
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val responseBody = response.body?.string() ?: return null
            val playerResponse = json.decodeFromString<PlayerResponse>(responseBody)
            
            val formats = playerResponse.streamingData?.adaptiveFormats ?: return null
            val audioFormats = formats.filter { 
                val mime = it.mimeType ?: ""
                (mime.startsWith("audio/webm") && mime.contains("opus")) ||
                (mime.startsWith("audio/mp4") && mime.contains("mp4a.40.2"))
            }
            
            return audioFormats.maxByOrNull { it.bitrate ?: 0 }?.url
        }
    }
}

@Serializable
data class PlayerResponse(
    val streamingData: StreamingData? = null
)

@Serializable
data class StreamingData(
    val adaptiveFormats: List<Format>? = null
)

@Serializable
data class Format(
    val itag: Int? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val bitrate: Int? = null
)
