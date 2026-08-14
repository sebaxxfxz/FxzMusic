package com.fxzmusic.app.service

import android.content.Context
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheProvider {

    private var databaseProvider: ExoDatabaseProvider? = null
    private var playerCache: SimpleCache? = null
    private var downloadCache: SimpleCache? = null

    fun getMaxCacheSizeBytes(context: Context): Long {
        val prefs = context.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        val mb = prefs.getLong("max_cache_size_mb", 512L)
        return if (mb <= 0L) -1L else mb * 1024L * 1024L
    }

    @Synchronized
    fun init(context: Context) {
        if (databaseProvider != null) return
        val provider = ExoDatabaseProvider(context)
        databaseProvider = provider

        val maxBytes = getMaxCacheSizeBytes(context)
        val evictor = if (maxBytes > 0L) LeastRecentlyUsedCacheEvictor(maxBytes) else NoOpCacheEvictor()

        val playerDir = File(context.cacheDir, "player_cache")
        if (!playerDir.exists()) playerDir.mkdirs()
        playerCache = SimpleCache(
            playerDir,
            evictor,
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

    @Synchronized
    fun updateMaxCacheSize(context: Context, newMaxMb: Long) {
        val prefs = context.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        prefs.edit().putLong("max_cache_size_mb", newMaxMb).apply()

        val cache = playerCache ?: return
        if (newMaxMb <= 0L) return

        val maxBytes = newMaxMb * 1024L * 1024L
        val activeMediaId = PlaybackService.exoPlayerInstance?.currentMediaItem?.mediaId

        try {
            if (cache.cacheSpace > maxBytes) {
                val keys = cache.keys.toList()
                for (key in keys) {
                    if (cache.cacheSpace <= maxBytes) break
                    if (activeMediaId != null && (key == activeMediaId || key.contains(activeMediaId))) {
                        continue
                    }
                    try {
                        cache.removeResource(key)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CacheProvider", "Failed to trim cache to $newMaxMb MB", e)
        }
    }

    fun getPlayerCache(): SimpleCache? = playerCache
    fun getDownloadCache(): SimpleCache? = downloadCache
    fun getDatabaseProvider(): ExoDatabaseProvider? = databaseProvider
}
