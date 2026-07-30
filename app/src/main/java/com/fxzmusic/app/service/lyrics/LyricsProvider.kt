package com.fxzmusic.app.service.lyrics

import com.fxzmusic.app.data.LyricsLine

sealed class LyricsResult {
    data class Synced(val lines: List<LyricsLine>) : LyricsResult()
    data class Plain(val text: String) : LyricsResult()
    object Instrumental : LyricsResult()
    object NotFound : LyricsResult()
    data class Error(val message: String) : LyricsResult()
}

interface LyricsProvider {
    val name: String
    suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String? = null
    ): LyricsResult
}
