package com.fxzmusic.app.service.lyrics

import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.WatchEndpoint

class YouTubeLyricsProvider : LyricsProvider {
    override val name: String = "YouTube Music"

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        val videoId = youtubeVideoId?.takeIf { it.isNotBlank() } ?: return LyricsResult.NotFound
        return try {
            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
            val endpoint = nextResult?.lyricsEndpoint ?: return LyricsResult.NotFound
            val rawLyrics = YouTube.lyrics(endpoint).getOrNull() ?: return LyricsResult.NotFound
            if (rawLyrics.isBlank()) return LyricsResult.NotFound

            val lines = LrcParser.parseSyncedLyrics(rawLyrics)
            if (lines.isNotEmpty()) {
                LyricsResult.Synced(lines)
            } else {
                LyricsResult.Plain(rawLyrics)
            }
        } catch (e: Exception) {
            LyricsResult.Error(e.message ?: "Error al obtener letras de YouTube Music")
        }
    }
}
