package com.fxzmusic.app.service.queues

import androidx.media3.common.MediaItem

class ListQueue(
    override val title: String? = null,
    private val items: List<MediaItem>,
    private val startIndex: Int = 0,
    private val startPositionMs: Long = 0L,
    private val positionSourceIsQueue: Boolean = true
) : Queue {

    init {
        require(items.isNotEmpty()) { "items must not be empty" }
        require(startIndex in items.indices) {
            "startIndex=$startIndex out of bounds (size=${items.size})"
        }
    }

    override suspend fun getInitialStatus(): Queue.InitialStatus = Queue.InitialStatus(
        items = items,
        startIndex = startIndex,
        startPositionMs = startPositionMs,
        positionSourceIsQueue = positionSourceIsQueue
    )

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}
