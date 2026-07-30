package com.fxzmusic.app.service.queues

import android.util.Log
import androidx.media3.common.MediaItem
import com.fxzmusic.app.service.toMediaItem
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMixQueue(
    override val title: String? = "Tu mezcla",
    items: List<MediaItem>,
    startIndex: Int = 0,
) : Queue {

    private val localItems = items
    private var localIndex = startIndex
    private var continuation: String? = null
    private var localPlayedOut = false

    override suspend fun getInitialStatus(): Queue.InitialStatus = withContext(Dispatchers.IO) {
        Queue.InitialStatus(
            items = localItems,
            startIndex = localIndex,
        )
    }

    override fun hasNextPage(): Boolean = !localPlayedOut || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(Dispatchers.IO) {
        if (!localPlayedOut) {
            localPlayedOut = true
            val lastVideoId = localItems.lastOrNull()?.mediaId
                ?: return@withContext emptyList()
            return@withContext fetchRelatedContent(lastVideoId)
        }
        val lastVideoId = localItems.lastOrNull()?.mediaId
            ?: return@withContext emptyList()
        fetchRelatedContent(lastVideoId)
    }

    private suspend fun fetchRelatedContent(seedVideoId: String): List<MediaItem> {
        return try {
            val endpoint = WatchEndpoint(videoId = seedVideoId)
            val nextResult = YouTube.next(endpoint, continuation).getOrNull()
            continuation = nextResult?.continuation
            val relatedItems = nextResult?.items
                ?.map { it.toMediaItem() }
                ?.filter { it.mediaId != seedVideoId }
                .orEmpty()
            if (relatedItems.isEmpty()) {
                continuation = null
            }
            relatedItems
        } catch (e: Exception) {
            Log.w("LocalMixQueue", "Failed to fetch related content", e)
            continuation = null
            emptyList()
        }
    }
}
