package com.fxzmusic.app.service.queues

import android.util.Log
import androidx.media3.common.MediaItem
import com.fxzmusic.app.service.YouTubeMusicRepository
import com.fxzmusic.app.service.toMediaItem
import com.fxzmusic.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubePlaylistQueue(
    private val playlistId: String,
    override val title: String? = null,
    private val startIndex: Int = 0,
) : Queue {

    private var continuation: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    override suspend fun getInitialStatus(): Queue.InitialStatus = withContext(Dispatchers.IO) {
        val page = YouTubeMusicRepository.get().playlist(playlistId).getOrThrow()
        continuation = page.songsContinuation
        Queue.InitialStatus(
            items = page.songs.map { it.toMediaItem() },
            startIndex = startIndex,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(Dispatchers.IO) {
        val currentContinuation = continuation ?: return@withContext emptyList()
        var lastException: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                val continuationPage = YouTube.playlistContinuation(currentContinuation).getOrThrow()
                continuation = continuationPage.continuation
                retryCount = 0
                return@withContext continuationPage.songs.map { it.toMediaItem() }
            } catch (e: Exception) {
                lastException = e
                retryCount++
                if (retryCount >= maxRetries) {
                    continuation = null
                }
                Log.w("YouTubePlaylistQueue", "nextPage attempt $attempt failed", e)
            }
        }
        throw lastException ?: Exception("Failed to load next playlist page")
    }
}
