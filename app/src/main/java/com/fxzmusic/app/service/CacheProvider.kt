package com.fxzmusic.app.service

import android.content.Context
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheProvider {

    private const val PLAYER_CACHE_MAX_BYTES = 500L * 1024 * 1024

    private var databaseProvider: ExoDatabaseProvider? = null
    private var playerCache: SimpleCache? = null
    private var downloadCache: SimpleCache? = null

    @Synchronized
    fun init(context: Context) {
        if (databaseProvider != null) return
        val provider = ExoDatabaseProvider(context)
        databaseProvider = provider

        val playerDir = File(context.cacheDir, "player_cache")
        if (!playerDir.exists()) playerDir.mkdirs()
        playerCache = SimpleCache(
            playerDir,
            LeastRecentlyUsedCacheEvictor(PLAYER_CACHE_MAX_BYTES),
            provider
        )

        val downloadDir = File(context.cacheDir, "download_cache")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        downloadCache = SimpleCache(
            downloadDir,
            NoOpCacheEvictor(),
            provider
        )
    }

    fun getPlayerCache(): SimpleCache? = playerCache
    fun getDownloadCache(): SimpleCache? = downloadCache
    fun getDatabaseProvider(): ExoDatabaseProvider? = databaseProvider
}
