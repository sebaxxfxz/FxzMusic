package com.fxzmusic.app.service.queues

import android.util.Log
import androidx.media3.common.MediaItem
import com.fxzmusic.app.service.YouTubeMusicRepository
import com.fxzmusic.app.service.toMediaItem
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeAlbumRadio(
    override val title: String? = null,
    private val playlistId: String,
) : Queue {

    private var albumSongCount = 0
    private var continuation: String? = null
    private var firstTimeLoaded = false

    override suspend fun getInitialStatus(): Queue.InitialStatus = withContext(Dispatchers.IO) {
        val albumSongs = YouTubeMusicRepository.get().albumSongs(playlistId).getOrThrow()
        albumSongCount = albumSongs.size
        Queue.InitialStatus(
            items = albumSongs.map { it.toMediaItem() },
            startIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(Dispatchers.IO) {
        val endpoint = WatchEndpoint(playlistId = playlistId)
        val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
        continuation = nextResult.continuation
        if (!firstTimeLoaded) {
            firstTimeLoaded = true
            nextResult.items.subList(albumSongCount, nextResult.items.size).map { it.toMediaItem() }
        } else {
            nextResult.items.map { it.toMediaItem() }
        }
    }
}
