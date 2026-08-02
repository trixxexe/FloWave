package com.trixxexe.trixxwave.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * InnerTube API Configuration & Dynamic Key/STS Resolver.
 *
 * NOTE FOR DEVELOPERS:
 * InnerTube client versions and signature timestamps (STS) require periodic updates as YouTube
 * updates its internal API endpoints. Seal (yt-dlp) handles this via self-updating engine binaries.
 * For FloWave's streaming client, we maintain primary fallback client identities here AND dynamically
 * extract the latest `signatureTimestamp` (STS) directly from YouTube's live player JS at runtime.
 */
object InnerTubeConfig {
    private const val TAG = "InnerTubeConfig"

    // --- CLIENT IDENTITIES (UPDATED REGULARLY) ---
    const val ANDROID_MUSIC_NAME = "ANDROID_MUSIC"
    var androidMusicVersion: String = "7.02.52"

    const val WEB_REMIX_NAME = "WEB_REMIX"
    var webRemixVersion: String = "1.20240108.01.00"

    const val WEB_NAME = "WEB"
    var webVersion: String = "2.20231011.00.00"

    const val IOS_NAME = "IOS"
    var iosVersion: String = "19.09.3"

    const val TV_HTML5_NAME = "TVHTML5_SIMPLY_EMBEDDED_PLAYER"
    var tvHtml5Version: String = "2.20240108.01.00"

    // --- DEFAULT FALLBACK SIGNATURE TIMESTAMP (STS) ---
    private val cachedSts = AtomicInteger(19700)
    private val cachedPlayerJsUrl = AtomicReference<String?>(null)

    /**
     * Get the current active signature timestamp (STS).
     */
    fun getSignatureTimestamp(): Int = cachedSts.get()

    /**
     * Dynamically fetch the latest `signatureTimestamp` from YouTube's live player JavaScript (`base.js`).
     * This prevents stream extraction failures caused by stale STS values.
     */
    suspend fun fetchDynamicSts(client: OkHttpClient): Int {
        return try {
            val iframeApiReq = Request.Builder()
                .url("https://www.youtube.com/iframe_api")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(iframeApiReq).execute().use { resp ->
                if (!resp.isSuccessful) return cachedSts.get()
                val body = resp.body?.string() ?: ""
                val playerJsMatch = Regex("""player_ias/msys/([a-zA-Z0-9_.-]+)/base\.js""").find(body)
                    ?: Regex("""/s/player/[a-zA-Z0-9_.-]+/player_ias\.vflset/[a-zA-Z_.-]+/base\.js""").find(body)

                val jsPath = playerJsMatch?.value ?: return cachedSts.get()
                val fullJsUrl = if (jsPath.startsWith("http")) jsPath else "https://www.youtube.com/$jsPath"

                val jsReq = Request.Builder()
                    .url(fullJsUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                client.newCall(jsReq).execute().use { jsResp ->
                    if (!jsResp.isSuccessful) return cachedSts.get()
                    val jsBody = jsResp.body?.string() ?: ""

                    // Extract signatureTimestamp / sts: 19800
                    val stsMatch = Regex("""signatureTimestamp\s*:\s*(\d+)""").find(jsBody)
                        ?: Regex("""sts\s*:\s*(\d+)""").find(jsBody)

                    val extractedSts = stsMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (extractedSts != null && extractedSts > 0) {
                        cachedSts.set(extractedSts)
                        cachedPlayerJsUrl.set(fullJsUrl)
                        Log.d(TAG, "Dynamically extracted live YouTube STS: $extractedSts from $fullJsUrl")
                        return extractedSts
                    }
                }
            }
            cachedSts.get()
        } catch (e: Exception) {
            Log.d(TAG, "STS dynamic extraction fallback to cached STS: ${cachedSts.get()} (${e.message})")
            cachedSts.get()
        }
    }

    /**
     * Generate JSON request payload for InnerTube player API with dynamic client identity & STS.
     */
    fun buildPlayerPayload(
        videoId: String,
        clientName: String = ANDROID_MUSIC_NAME,
        clientVersion: String = androidMusicVersion,
        sts: Int = getSignatureTimestamp()
    ): String {
        return """
            {
                "context": {
                    "client": {
                        "clientName": "$clientName",
                        "clientVersion": "$clientVersion",
                        "hl": "en",
                        "gl": "US"
                    },
                    "thirdParty": {
                        "embedUrl": "https://www.youtube.com"
                    }
                },
                "videoId": "$videoId",
                "playbackContext": {
                    "contentPlaybackContext": {
                        "signatureTimestamp": $sts
                    }
                }
            }
        """.trimIndent()
    }

    /**
     * Generate JSON request payload for InnerTube search API with dynamic client identity.
     */
    fun buildSearchPayload(
        query: String,
        clientName: String = WEB_REMIX_NAME,
        clientVersion: String = webRemixVersion
    ): String {
        return """
            {
                "context": {
                    "client": {
                        "clientName": "$clientName",
                        "clientVersion": "$clientVersion",
                        "hl": "en",
                        "gl": "US"
                    }
                },
                "query": "$query"
            }
        """.trimIndent()
    }
}
