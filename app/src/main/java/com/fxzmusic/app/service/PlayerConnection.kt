package com.fxzmusic.app.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.service.queues.Queue
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlayerConnection private constructor(
    private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val isPlaying: StateFlow<Boolean> get() = _isPlaying
    val mediaItemCount: StateFlow<Int> get() = _mediaItemCount
    val currentIndex: StateFlow<Int> get() = _currentIndex
    val positionMs: StateFlow<Long> get() = _positionMs
    val durationMs: StateFlow<Long> get() = _durationMs
    val playbackParameters: StateFlow<PlaybackParameters> get() = _playbackParameters
    val repeatMode: StateFlow<Int> get() = _repeatMode
    val shuffleMode: StateFlow<Boolean> get() = _shuffleMode
    val playerError: StateFlow<Throwable?> get() = _playerError

    private val _isPlaying = MutableStateFlow(false)
    private val _mediaItemCount = MutableStateFlow(0)
    private val _currentIndex = MutableStateFlow(0)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    private val _playbackParameters = MutableStateFlow(PlaybackParameters(1f))
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val _shuffleMode = MutableStateFlow(false)
    private val _playerError = MutableStateFlow<Throwable?>(null)

    private var positionJob: kotlinx.coroutines.Job? = null

    fun connect(onReady: (MediaController?) -> Unit = {}) {
        if (controller != null) {
            onReady(controller)
            return
        }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val ctrl = future.get()
                controller = ctrl
                attachListeners(ctrl)
                seedState(ctrl)
                onReady(ctrl)
            } catch (e: Exception) {
                android.util.Log.e("PlayerConnection", "connect failed: ${e.message}", e)
                onReady(null)
            }
        }, ContextCompat_mainExecutor(context))
    }

    fun disconnect() {
        positionJob?.cancel()
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        scope.cancel()
    }

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0, playWhenReady: Boolean = true) {
        val ctrl = controller ?: return
        ctrl.setMediaItems(items, startIndex, 0L)
        ctrl.prepare()
        ctrl.playWhenReady = playWhenReady
    }

    suspend fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        val ctrl = controller ?: return
        val status = queue.getInitialStatus()
        if (status.items.isEmpty()) return
        withContext(Dispatchers.Main) {
            ctrl.setMediaItems(status.items, status.startIndex, status.startPositionMs)
            ctrl.prepare()
            ctrl.playWhenReady = playWhenReady
        }
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun playPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun seekToNext() { controller?.seekToNextMediaItem() }
    fun seekToPrevious() { controller?.seekToPreviousMediaItem() }
    fun seekToIndex(index: Int) { controller?.seekTo(index, 0L) }
    fun setRepeatMode(mode: Int) { controller?.repeatMode = mode }
    fun setShuffleMode(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun setPlaybackParameters(params: PlaybackParameters) { controller?.playbackParameters = params }
    fun addItems(items: List<MediaItem>) { controller?.addMediaItems(items) }
    fun addItem(item: MediaItem) { controller?.addMediaItem(item) }
    fun moveItem(from: Int, to: Int) { controller?.moveMediaItem(from, to) }
    fun removeItem(index: Int) { controller?.removeMediaItem(index) }
    fun clearQueue() { controller?.clearMediaItems() }

    fun stop() { controller?.stop() }

    fun currentMediaItem(): MediaItem? = controller?.currentMediaItem
    fun controller(): MediaController? = controller

    private fun attachListeners(ctrl: MediaController) {
        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { _isPlaying.value = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = ctrl.currentMediaItemIndex
            }
            override fun onPlaybackStateChanged(state: Int) {
                _durationMs.value = if (state == Player.STATE_READY) ctrl.duration.coerceAtLeast(0L) else 0L
            }
            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                _playbackParameters.value = params
            }
            override fun onRepeatModeChanged(mode: Int) { _repeatMode.value = mode }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { _shuffleMode.value = enabled }
            override fun onPlayerErrorChanged(error: androidx.media3.common.PlaybackException?) {
                _playerError.value = error
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                _mediaItemCount.value = ctrl.mediaItemCount
            }
        })
        startPollingPosition(ctrl)
        scope.launch {
            
            _mediaItemCount.value = ctrl.mediaItemCount
            _currentIndex.value = ctrl.currentMediaItemIndex
            _repeatMode.value = ctrl.repeatMode
            _shuffleMode.value = ctrl.shuffleModeEnabled
        }
    }

    private fun seedState(ctrl: MediaController) {
        _isPlaying.value = ctrl.isPlaying
        _mediaItemCount.value = ctrl.mediaItemCount
        _currentIndex.value = ctrl.currentMediaItemIndex
        _durationMs.value = ctrl.duration.coerceAtLeast(0L)
        _playbackParameters.value = ctrl.playbackParameters
        _repeatMode.value = ctrl.repeatMode
        _shuffleMode.value = ctrl.shuffleModeEnabled
    }

    private fun startPollingPosition(ctrl: MediaController) {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                delay(200)
                val isNewPlaying = ctrl.isPlaying
                val pos = ctrl.currentPosition.coerceAtLeast(0L)
                if (pos != _positionMs.value) _positionMs.value = pos
                val dur = ctrl.duration.coerceAtLeast(0L)
                if (dur != _durationMs.value) _durationMs.value = dur
                if (isNewPlaying != _isPlaying.value) _isPlaying.value = isNewPlaying
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: PlayerConnection? = null

        fun getOrCreate(context: Context): PlayerConnection {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerConnection(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun connect(context: Context, onReady: (MediaController?) -> Unit = {}) {
            getOrCreate(context).connect(onReady)
        }

        fun disconnect(context: Context) {
            getOrCreate(context).disconnect()
            synchronized(this) { INSTANCE = null }
        }
    }
}

private fun ContextCompat_mainExecutor(context: Context): java.util.concurrent.Executor {
    return androidx.core.content.ContextCompat.getMainExecutor(context)
}
