package com.example.fxzmusic

data class LyricsLine(
    val timeMs: Long,
    val text: String
)

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
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Synced(val lines: List<LyricsLine>) : LyricsState()
    data class Plain(val text: String) : LyricsState()
    object NotFound : LyricsState()
    object Instrumental : LyricsState()
}

fun parseSyncedLyrics(raw: String): List<LyricsLine> {
    val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
    return raw.lines()
        .mapNotNull { line ->
            val match = regex.find(line.trim()) ?: return@mapNotNull null
            val (min, sec, ms, text) = match.destructured
            val timeMs = min.toLong() * 60_000 + sec.toLong() * 1_000 + ms.padEnd(3, '0').toLong()
            LyricsLine(timeMs = timeMs, text = text.trim())
        }
        .sortedBy { it.timeMs }
}
