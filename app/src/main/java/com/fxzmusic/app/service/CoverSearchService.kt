package com.fxzmusic.app.service
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.util.*

import android.util.Log
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

object CoverSearchService {
    private val coverCache = ConcurrentHashMap<String, String>()
    private const val MAX_CACHE_SIZE = 200
    private const val MAX_RETRIES = 2

    suspend fun searchCover(song: Song): String? {
        val cacheKey = "${song.artist}|${song.title}|${song.album}".lowercase()
        coverCache[cacheKey]?.let { return it }
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val url = NetworkModule.searchCoverUrl(song.title, song.artist, song.album)
                if (url != null) {
                    if (coverCache.size >= MAX_CACHE_SIZE) {
                        val oldest = coverCache.keys.firstOrNull()
                        if (oldest != null) coverCache.remove(oldest)
                    }
                    coverCache[cacheKey] = url
                }
                return url
            } catch (e: Exception) {
                Log.w("CoverSearchService", "Attempt ${attempt + 1} failed for ${song.title}", e)
                if (attempt < MAX_RETRIES) delay((attempt + 1) * 1000L)
            }
        }
        return null
    }
}
