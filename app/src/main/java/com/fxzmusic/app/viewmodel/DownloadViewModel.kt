package com.fxzmusic.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.fxzmusic.app.data.DownloadEntity
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.service.DownloadUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val util get() = DownloadUtil.get()

    val uiState: StateFlow<List<DownloadUiItem>> = combine(
        util.downloadsDbFlow,
        util.downloads
    ) { dbItems, activeDownloads ->
        dbItems.map { entity ->
            val download = activeDownloads[entity.videoId]
            DownloadUiItem(
                entity = entity,
                download = download,
                progress = when {
                    download == null -> if (activeDownloads.containsKey(entity.videoId)) 1f else 1f
                    download.state == Download.STATE_COMPLETED -> 1f
                    download.contentLength > 0 ->
                        (download.bytesDownloaded.toFloat() / download.contentLength).coerceIn(0f, 1f)
                    else -> 0f
                },
                state = download?.state ?: Download.STATE_COMPLETED
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalStorageFlow: StateFlow<Long> = combine(
        util.downloadsDbFlow
    ) { (items) -> items.sumOf { it.fileSize } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun download(
        videoId: String,
        title: String = "",
        artist: String = "",
        album: String = "",
        coverUrl: String? = null,
        duration: Int = 0
    ) {
        util.enqueue(
            videoId = videoId,
            title = title,
            artist = artist,
            album = album,
            coverUrl = coverUrl,
            duration = duration
        )
    }

    fun cancel(videoId: String) = util.cancel(videoId)

    fun resume(videoId: String) = util.resume(videoId)

    fun delete(videoId: String) = util.delete(videoId)

    fun isDownloaded(videoId: String): Boolean {
        return util.downloads.value.containsKey(videoId)
    }

    fun getDownloadState(videoId: String): Int =
        util.downloads.value[videoId]?.state ?: -1

    fun getProgress(videoId: String): Float {
        val dl = util.downloads.value[videoId] ?: return 0f
        if (dl.state == Download.STATE_COMPLETED) return 1f
        if (dl.contentLength <= 0) return 0f
        return (dl.bytesDownloaded.toFloat() / dl.contentLength).coerceIn(0f, 1f)
    }

    fun clearBrokenCache() = util.clearBrokenCache()
}

data class DownloadUiItem(
    val entity: DownloadEntity,
    val download: Download?,
    
    val progress: Float,
    
    val state: Int
) {
    val isCompleted get() = state == Download.STATE_COMPLETED
    val isDownloading get() = state == Download.STATE_DOWNLOADING
    val isQueued get() = state == Download.STATE_QUEUED
    val isFailed get() = state == Download.STATE_FAILED
}
