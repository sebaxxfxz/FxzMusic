package com.example.fxzmusic

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

@OptIn(UnstableApi::class)
class MusicPlayerViewModel : ViewModel() {

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var prefs: SharedPreferences? = null

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set
    var isShuffleEnabled by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(RepeatMode.NONE)
        private set
    var playlist by mutableStateOf<List<Song>>(emptyList())
        private set
    var isRestoringState by mutableStateOf(false)
        private set
    var playbackError by mutableStateOf<String?>(null)
        private set

    var currentPlaybackSpeed by mutableFloatStateOf(1.0f)
        private set
    // Allow UI to toggle the audio device sheet state
    var showAudioDeviceSheet by mutableStateOf(false)

    private var positionUpdateJob: Job? = null
    private val shuffleHistory = mutableListOf<Int>()
    var allSongsProvider: (() -> List<Song>)? = null

    enum class RepeatMode { NONE, ONE, ALL }

    fun initializePlayer(context: Context, allSongs: List<Song> = emptyList()) {
        prefs = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)

        if (mediaControllerFuture == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            mediaControllerFuture?.addListener({
                mediaController = mediaControllerFuture?.get()

                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) startPositionUpdates() else stopPositionUpdates()
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.let { item ->
                            val allSongs = allSongsProvider?.invoke() ?: emptyList()
                            val song = playlist.find { it.id == item.mediaId }
                                ?: allSongs.find { it.id == item.mediaId }
                            if (song != null) {
                                currentSong = song
                                duration = song.duration
                                currentPosition = 0
                            } else {
                                currentSong = Song(
                                    id     = item.mediaId ?: "",
                                    title  = item.mediaMetadata.title?.toString() ?: "Desconocida",
                                    artist = item.mediaMetadata.artist?.toString() ?: ""
                                )
                                duration = 0
                                currentPosition = 0
                            }
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            duration = ((mediaController?.duration ?: 0L) / 1000).toInt()
                        }
                    }
                })
                restorePlaybackState(allSongs)
                applySpeedToController(currentPlaybackSpeed)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun restorePlaybackState(allSongs: List<Song>) {
        val controller = mediaController ?: return
        if (controller.mediaItemCount > 0) {
            val current = controller.currentMediaItem
            if (current != null) {
                val song = allSongs.find { it.id == current.mediaId }
                    ?: playlist.find { it.id == current.mediaId }
                if (song != null) {
                    currentSong = song
                    duration = ((controller.duration) / 1000).toInt()
                    currentPosition = (controller.currentPosition / 1000).toInt()
                    isPlaying = controller.isPlaying
                    if (isPlaying) startPositionUpdates()
                }
            }
            return
        }

        if (allSongs.isEmpty()) return
        isRestoringState = true

        val lastIndex    = prefs?.getInt("last_index", -1) ?: -1
        val lastPosition = prefs?.getLong("last_position", 0L) ?: 0L

        if (lastIndex < 0 || lastIndex >= allSongs.size) { isRestoringState = false; return }

        val song = allSongs[lastIndex]
        playlist = allSongs
        currentSong = song
        duration = song.duration
        currentPosition = (lastPosition / 1000).toInt()

        val mediaItems = allSongs.map { s -> buildMediaItem(s) }
        controller.setMediaItems(mediaItems, lastIndex, lastPosition)
        controller.prepare()
        isRestoringState = false
    }

    fun restoreWithSongs(allSongs: List<Song>) {
        if (playlist.isEmpty() && allSongs.isNotEmpty()) {
            restorePlaybackState(allSongs)
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                delay(200)
                mediaController?.let { player ->
                    val newPos = (player.currentPosition / 1000).toInt()
                    if (newPos != currentPosition) currentPosition = newPos
                }
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun playSong(song: Song, newPlaylist: List<Song> = emptyList(), context: Context?) {
        playbackError = null
        try {
            if (context != null && mediaController == null) initializePlayer(context)
            playlist = newPlaylist.ifEmpty { listOf(song) }
            val startIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            if (!shuffleHistory.contains(startIndex)) {
                shuffleHistory.add(startIndex)
                if (shuffleHistory.size > playlist.size / 2) shuffleHistory.removeAt(0)
            }
            currentSong = song
            duration = song.duration
            currentPosition = 0

            mediaController?.let { controller ->
                val mediaItems = playlist.map { s -> buildMediaItem(s) }
                controller.setMediaItems(mediaItems, startIndex, 0)
                controller.prepare()
                controller.play()
                applySpeedToController(currentPlaybackSpeed)
            } ?: run {
                playbackError = "El reproductor no está listo. Intenta de nuevo."
            }
        } catch (e: SecurityException) {
            playbackError = "Sin permiso para leer este archivo."
        } catch (e: FileNotFoundException) {
            playbackError = "Archivo no encontrado: ${song.filePath}"
        } catch (e: IllegalStateException) {
            playbackError = "El reproductor no pudo procesar \"${song.title}\". Intenta de nuevo."
        } catch (e: Exception) {
            playbackError = "No se pudo reproducir \"${song.title}\"."
        }
    }

    fun appendSongsToQueue(songs: List<Song>) {
        val controller = mediaController ?: return
        val currentPlaylist = playlist.toMutableList()
        val newSongs = songs.filter { newSong -> currentPlaylist.none { it.id == newSong.id } }
        if (newSongs.isEmpty()) return
        currentPlaylist.addAll(newSongs)
        playlist = currentPlaylist
        val mediaItems = newSongs.map { s -> buildMediaItem(s) }
        controller.addMediaItems(mediaItems)
    }

    fun clearError() { playbackError = null }

    fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed
        applySpeedToController(speed)
        prefs?.edit()?.putFloat("playback_speed", speed)?.apply()
    }

    private fun applySpeedToController(speed: Float) {
        mediaController?.playbackParameters = PlaybackParameters(speed)
    }

    fun updateCurrentSongLike(songId: String, isLiked: Boolean) {
        if (currentSong?.id == songId) {
            currentSong = currentSong?.copy(isLiked = isLiked)
        }
        playlist = playlist.map { if (it.id == songId) it.copy(isLiked = isLiked) else it }
    }

    fun togglePlayPause() { mediaController?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun pauseIfPlaying() { mediaController?.let { if (it.isPlaying) it.pause() } }
    fun seekTo(position: Int) { mediaController?.seekTo(position * 1000L); currentPosition = position }
    fun playNext() { mediaController?.seekToNextMediaItem() }
    fun playPrevious() { mediaController?.seekToPreviousMediaItem() }

    fun stopSong() {
        isPlaying = false; currentSong = null; currentPosition = 0
        mediaController?.stop(); mediaController?.clearMediaItems()
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        mediaController?.shuffleModeEnabled = isShuffleEnabled
        if (!isShuffleEnabled) shuffleHistory.clear()
    }

    fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL  -> RepeatMode.ONE
            RepeatMode.ONE  -> RepeatMode.NONE
        }
        mediaController?.repeatMode = when (repeatMode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE  -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL  -> Player.REPEAT_MODE_ALL
        }
    }

    // Helpers to show/hide the audio device selection sheet from UI
    fun openAudioDevices() { showAudioDeviceSheet = true }
    fun closeAudioDevices() { showAudioDeviceSheet = false }

    private fun buildMediaItem(s: Song): MediaItem {
        val artworkUri = s.coverUrl?.toUri()
            ?: if (s.filePath.isNotEmpty()) android.net.Uri.fromFile(File(s.filePath)) else null
        val meta = MediaMetadata.Builder()
            .setTitle(s.title)
            .setArtist(s.artist)
            .setAlbumTitle(s.album)
        if (artworkUri != null) meta.setArtworkUri(artworkUri)
        return MediaItem.Builder()
            .setMediaId(s.id)
            .setUri(s.filePath)
            .setMediaMetadata(meta.build())
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
