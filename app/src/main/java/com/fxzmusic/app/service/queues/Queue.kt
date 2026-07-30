package com.fxzmusic.app.service.queues

import androidx.media3.common.MediaItem

interface Queue {
    
    val title: String?

    suspend fun getInitialStatus(): InitialStatus

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    val preloadItem: MediaItem?
        get() = null

    data class InitialStatus(
        val items: List<MediaItem> = emptyList(),
        val startIndex: Int = 0,
        val startPositionMs: Long = 0L,
        val positionSourceIsQueue: Boolean = false
    )
}
