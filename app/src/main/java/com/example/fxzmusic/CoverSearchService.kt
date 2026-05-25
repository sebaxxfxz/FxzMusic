package com.example.fxzmusic

import java.util.concurrent.ConcurrentHashMap

object CoverSearchService {
    private val coverCache = ConcurrentHashMap<String, String>()

    suspend fun searchCover(song: Song): String? {
        val cacheKey = "${song.artist}|${song.title}|${song.album}".lowercase()
        coverCache[cacheKey]?.let { return it }
        return try {
            val url = NetworkModule.searchCoverUrl(song.title, song.artist, song.album)
            if (url != null) coverCache[cacheKey] = url
            url
        } catch (_: Exception) { null }
    }
}
