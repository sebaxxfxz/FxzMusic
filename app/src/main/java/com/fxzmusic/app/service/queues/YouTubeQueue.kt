package com.fxzmusic.app.service.queues

import androidx.media3.common.MediaItem
import com.fxzmusic.app.service.toMediaItem
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaItem? = null
) : Queue {

    private var continuation: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    override val title: String? = null

    override suspend fun getInitialStatus(): Queue.InitialStatus = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (attempt in 0..maxRetries) {
            try {
                val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                endpoint = nextResult.endpoint
                continuation = nextResult.continuation
                retryCount = 0
                val out = nextResult.items.map { it.toMediaItem() }
                val idx = (nextResult.currentIndex ?: 0).coerceIn(0, (out.size - 1).coerceAtLeast(0))
                return@withContext Queue.InitialStatus(
                    items = out,
                    startIndex = idx
                )
            } catch (e: Exception) {
                lastError = e
                if (attempt == 0 && endpoint.videoId != null && endpoint.playlistId == null) {
                    
                    endpoint = WatchEndpoint(
                        videoId = endpoint.videoId,
                        playlistId = "RDAMVM${endpoint.videoId}"
                    )
                }
            }
        }
        throw lastError ?: IllegalStateException("Failed to load YouTube queue")
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (attempt in 0..maxRetries) {
            try {
                val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                endpoint = nextResult.endpoint
                continuation = nextResult.continuation
                retryCount = 0
                return@withContext nextResult.items.map { it.toMediaItem() }
            } catch (e: Exception) {
                lastError = e
                retryCount++
                if (retryCount >= maxRetries) {
                    continuation = null
                }
            }
        }
        throw lastError ?: IllegalStateException("Failed to load next page")
    }

    companion object {
        
        fun radio(videoId: String, preloadItem: MediaItem? = null): YouTubeQueue =
            YouTubeQueue(WatchEndpoint(videoId = videoId), preloadItem)

        fun playlist(playlistId: String): YouTubeQueue =
            YouTubeQueue(WatchEndpoint(playlistId = playlistId))
    }
}
