package com.fxzmusic.app.service.lyrics

import com.fxzmusic.innertube.YouTube

class YouTubeSubtitleProvider : LyricsProvider {
    override val name: String = "YouTube Subtitle"

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        val videoId = youtubeVideoId?.takeIf { it.isNotBlank() } ?: return LyricsResult.NotFound
        return try {
            val rawLyrics = YouTube.transcript(videoId).getOrNull() ?: return LyricsResult.NotFound
            if (rawLyrics.isBlank()) return LyricsResult.NotFound
            
            val cleanLyrics = rawLyrics.replace("♪", "").trim()

            val lines = LrcParser.parseSyncedLyrics(cleanLyrics)
            if (lines.isNotEmpty()) {
                LyricsResult.Synced(lines)
            } else {
                LyricsResult.Plain(cleanLyrics)
            }
        } catch (e: Exception) {
            LyricsResult.Error(e.message ?: "Error getting transcript")
        }
    }
}
