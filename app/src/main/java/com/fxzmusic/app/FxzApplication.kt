package com.fxzmusic.app

import com.fxzmusic.app.data.*
import com.fxzmusic.app.service.CacheProvider
import com.fxzmusic.app.service.DownloadUtil
import com.fxzmusic.ytpipeline.cipher.CipherDeobfuscator
import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import android.content.Context
import java.util.concurrent.TimeUnit

class FxzApplication : Application() {

    lateinit var database: FxzDatabase
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        database = FxzDatabase.getInstance(this)

        CacheProvider.init(this)
        CipherDeobfuscator.initialize(this)
        val playerCache = CacheProvider.getPlayerCache()
        val downloadCache = CacheProvider.getDownloadCache()
        val databaseProvider = CacheProvider.getDatabaseProvider()
        if (playerCache != null && downloadCache != null && databaseProvider != null) {
            DownloadUtil.init(
                context = this,
                database = database,
                playerCache = playerCache,
                downloadCache = downloadCache,
                databaseProvider = databaseProvider
            )
        } else {
            android.util.Log.e("FxzApp", "CacheProvider failed to initialise – downloads disabled")
        }

        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(300L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(150)
            .respectCacheHeaders(false)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            .build()

        Coil.setImageLoader(imageLoader)

        appScope.launch {
            try {
                val prefs = getSharedPreferences("youtube_prefs", Context.MODE_PRIVATE)
                val cached = prefs.getString("visitor_data", null)
                if (cached != null && cached != "null") {
                    com.fxzmusic.innertube.YouTube.visitorData = cached
                } else {
                    val newVisitorData = com.fxzmusic.innertube.YouTube.visitorData().getOrNull()
                    if (newVisitorData != null) {
                        com.fxzmusic.innertube.YouTube.visitorData = newVisitorData
                        prefs.edit().putString("visitor_data", newVisitorData).apply()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FxzApp", "Failed to init YouTube visitorData: ${e.message}", e)
            }
        }
    }
}
