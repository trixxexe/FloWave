package com.trixxexe.trixxwave.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object MediaCacheManager {

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    fun getCache(context: Context): SimpleCache? {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.applicationContext.cacheDir, "media_stream_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024L) // 500 MB max disk cache
                val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                try {
                    val cache = SimpleCache(cacheDir, evictor, databaseProvider)
                    simpleCache = cache
                    cache
                } catch (e: Exception) {
                    try {
                        cacheDir.deleteRecursively()
                        cacheDir.mkdirs()
                        val cache = SimpleCache(cacheDir, evictor, databaseProvider)
                        simpleCache = cache
                        cache
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    fun getCacheDataSourceFactory(context: Context): androidx.media3.datasource.DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://www.youtube.com/",
                "Origin" to "https://www.youtube.com"
            ))

        val cache = getCache(context)
        val httpSourceFactory = if (cache != null) {
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
            httpDataSourceFactory
        }

        // DefaultDataSource delegates HTTP/HTTPS to httpSourceFactory (with caching),
        // while routing local content://, file://, assets directly via native ContentDataSource/FileDataSource
        return DefaultDataSource.Factory(context.applicationContext, httpSourceFactory)
    }
}
