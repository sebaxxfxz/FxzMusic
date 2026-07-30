package com.fxzmusic.app.service.queues

import androidx.media3.common.MediaItem

object EmptyQueue : Queue {
    override val title: String? = null
    override suspend fun getInitialStatus(): Queue.InitialStatus = Queue.InitialStatus()
    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}
