package com.fxzmusic.app.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class UiEvent {
    data class LikeChanged(val songId: String, val isLiked: Boolean) : UiEvent()
    data class PlaylistDeleted(val playlistId: Long) : UiEvent()
    data class SpeedRestored(val speed: Float) : UiEvent()
    data class BlacklistChanged(val blacklistedFolders: Set<String>) : UiEvent()
    data class SongStatsChanged(val songId: String, val playCount: Int, val lastPlayed: Long) : UiEvent()
    object LibraryRefreshRequested : UiEvent()
    object TrackEnded : UiEvent()
}

object EventBus {
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun publish(event: UiEvent) {
        _events.emit(event)
    }
    
    fun tryPublish(event: UiEvent) {
        _events.tryEmit(event)
    }
}
