package com.fxzmusic.app.service.lyrics

import android.util.LruCache
import android.util.Log
import com.fxzmusic.app.data.LyricsLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class FetchResult(
    val result: LyricsResult,
    val provider: String
)

class LyricsManager(val providers: List<LyricsProvider>) {
    private val cache = LruCache<String, FetchResult>(3)
    private val scope = CoroutineScope(Dispatchers.IO)

    val providerNames: List<String> get() = providers.map { it.name }

    suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        positionMs: Long = 0,
        youtubeVideoId: String? = null,
        providers: List<LyricsProvider> = this.providers
    ): FetchResult {
        val cacheKey = buildCacheKey(title, artist, duration)
        cache.get(cacheKey)?.let { return it }

        val channel = Channel<Pair<String, LyricsResult>>(providers.size)
        val jobs = mutableListOf<Job>()

        providers.forEach { provider ->
            jobs.add(scope.launch {
                try {
                    val result = withTimeoutOrNull(15_000) {
                        provider.fetch(title, artist, album, duration, youtubeVideoId)
                    }
                    if (result != null) channel.send(provider.name to result)
                } catch (e: Exception) {
                    Log.w("LyricsManager", "${provider.name} failed: ${e.message}")
                }
            })
        }

        scope.launch { jobs.forEach { it.join() }; channel.close() }

        var firstPlain: Pair<String, LyricsResult.Plain>? = null
        var bestScored: Pair<String, LyricsResult.Synced>? = null
        var bestScore = -1
        val triedProviders = mutableListOf<String>()

        var syncedFound = false

        for ((provName, result) in channel) {
            triedProviders.add(provName)
            if (syncedFound && result !is LyricsResult.Synced) continue

            when (result) {
                is LyricsResult.Synced -> {
                    val score = calculateScore(result, duration, positionMs)
                    if (score > bestScore) {
                        bestScore = score
                        bestScored = provName to result
                    }
                    if (!syncedFound) {
                        syncedFound = true
                        jobs.forEach { it.cancel() }
                    }
                }
                is LyricsResult.Plain -> {
                    if (firstPlain == null) firstPlain = provName to result
                }
                is LyricsResult.Instrumental -> {
                    if (!syncedFound && firstPlain == null) {
                        val fr = FetchResult(result, provName)
                        cache.put(cacheKey, fr)
                        return fr
                    }
                }
                is LyricsResult.Error -> { }
                is LyricsResult.NotFound -> { }
            }
        }

        val chosen = bestScored ?: firstPlain
        val finalResult = chosen?.let { FetchResult(it.second, it.first) }
            ?: FetchResult(LyricsResult.NotFound, triedProviders.joinToString(", "))
        cache.put(cacheKey, finalResult)
        return finalResult
    }

    suspend fun fetchFromProvider(
        providerName: String,
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        positionMs: Long = 0,
        youtubeVideoId: String? = null
    ): FetchResult {
        val provider = providers.find { it.name == providerName } ?: return FetchResult(LyricsResult.NotFound, "Unknown")
        return try {
            val result = withTimeoutOrNull(15_000) {
                provider.fetch(title, artist, album, duration, youtubeVideoId)
            }
            result?.let { FetchResult(it, providerName) } ?: FetchResult(LyricsResult.NotFound, providerName)
        } catch (e: Exception) {
            Log.w("LyricsManager", "$providerName manual fetch failed: ${e.message}")
            FetchResult(LyricsResult.Error("${providerName} failed: ${e.message}"), providerName)
        }
    }

    private fun calculateScore(synced: LyricsResult.Synced, duration: Long?, positionMs: Long): Int {
        var score = 0

        duration?.let { dur ->
            val totalMs = synced.lines.lastOrNull()?.timeMs ?: 0L
            val diff = kotlin.math.abs(totalMs - dur)
            if (diff <= 5000) score += 100
        }

        if (synced.lines.any { it.words.isNotEmpty() }) score += 50

        score += kotlin.math.min(synced.lines.size, 50)

        if (positionMs > 0) {
            val hasLineNear = synced.lines.any { it.timeMs in (positionMs - 2000)..(positionMs + 2000) }
            if (hasLineNear) score += 20
        }

        return score
    }

    private fun buildCacheKey(title: String, artist: String?, duration: Long?): String {
        return "${title.trim().lowercase()}|${artist?.trim()?.lowercase() ?: ""}|${duration ?: 0}"
    }

    suspend fun fetchAll(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String? = null,
        providers: List<LyricsProvider> = this.providers
    ): List<Pair<String, LyricsResult>> {
        val results = mutableListOf<Pair<String, LyricsResult>>()
        providers.forEach { provider ->
            try {
                val result = withTimeoutOrNull(15_000) {
                    provider.fetch(title, artist, album, duration, youtubeVideoId)
                }
                if (result != null && result !is LyricsResult.Error && result !is LyricsResult.NotFound) {
                    results.add(provider.name to result)
                }
            } catch (_: Exception) {}
        }
        return results
    }

    fun invalidateCache(title: String, artist: String?, duration: Long?) {
        val key = buildCacheKey(title, artist, duration)
        cache.remove(key)
    }

    fun cancel() {
        scope.cancel()
    }
}
