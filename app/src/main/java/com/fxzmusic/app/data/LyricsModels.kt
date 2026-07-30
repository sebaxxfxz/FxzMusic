package com.fxzmusic.app.data

import androidx.compose.runtime.Immutable

@Immutable
data class LyricsWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

@Immutable
data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricsWord> = emptyList(),
    val agent: String? = null,
    val isBackground: Boolean = false
)

@Immutable
data class LyricsResponse(
    val id: Int = 0,
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Double = 0.0,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

sealed class LyricsState {
    data object Idle : LyricsState()
    data object Loading : LyricsState()
    @Immutable data class Synced(val lines: List<LyricsLine>, val provider: String? = null) : LyricsState()
    @Immutable data class Plain(val text: String, val provider: String? = null) : LyricsState()
    @Immutable data class NotFound(val providers: List<String> = emptyList()) : LyricsState()
    data object Instrumental : LyricsState()
}

enum class LyricsStyle { DEFAULT, FADE, GLOW, KARAOKE }
