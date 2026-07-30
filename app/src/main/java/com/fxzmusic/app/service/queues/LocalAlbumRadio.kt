package com.fxzmusic.app.service.queues

import androidx.media3.common.MediaItem

class LocalAlbumRadio(
    override val title: String? = null,
    private val seedSongId: String,
    private val items: List<MediaItem>
) : Queue {

    override suspend fun getInitialStatus(): Queue.InitialStatus {
        val idx = items.indexOfFirst { it.mediaId == seedSongId }.coerceAtLeast(0)
        return Queue.InitialStatus(items = items, startIndex = idx)
    }

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}
