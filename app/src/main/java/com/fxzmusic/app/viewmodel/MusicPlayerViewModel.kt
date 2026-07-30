package com.fxzmusic.app.viewmodel
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.data.AudioMetadata
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.service.*
import com.fxzmusic.app.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.WatchEndpoint
import com.fxzmusic.app.util.toSong
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
    private var playerListener: Player.Listener? = null
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

    var currentAudioMetadata by mutableStateOf<AudioMetadata?>(null)
    private var database: FxzDatabase? = null
        private set

    var showAudioDeviceSheet by mutableStateOf(false)

    private var positionSaveCounter = 0
    private var positionUpdateJob: Job? = null
    private val shuffleHistory = mutableListOf<Int>()

    private fun songToJson(song: Song): String {
        val obj = org.json.JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("duration", song.duration)
            put("filePath", song.filePath)
            put("coverUrl", song.coverUrl ?: "")
            put("isLiked", song.isLiked)
            put("isYouTube", song.isYouTube)
            put("youtubeVideoId", song.youtubeVideoId ?: if (song.isYouTube) song.id else "")
            put("youtubeThumbnailUrl", song.youtubeThumbnailUrl ?: "")
        }
        return obj.toString()
    }

    private fun songFromJson(jsonStr: String?): Song? {
        if (jsonStr.isNullOrEmpty()) return null
        return try {
            val obj = org.json.JSONObject(jsonStr)
            val id = obj.optString("id")
            if (id.isEmpty()) return null
            val isYT = obj.optBoolean("isYouTube", false)
            val ytId = obj.optString("youtubeVideoId").takeIf { it.isNotEmpty() }
                ?: if (isYT) id else null
            Song(
                id = id,
                title = obj.optString("title", "Desconocida"),
                artist = obj.optString("artist", ""),
                album = obj.optString("album", "Unknown Album"),
                duration = obj.optInt("duration", 180),
                filePath = obj.optString("filePath", ""),
                coverUrl = obj.optString("coverUrl").takeIf { it.isNotEmpty() },
                isLiked = obj.optBoolean("isLiked", false),
                isYouTube = isYT,
                youtubeVideoId = ytId,
                youtubeThumbnailUrl = obj.optString("youtubeThumbnailUrl").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun playlistToJson(songs: List<Song>): String {
        val arr = org.json.JSONArray()
        for (s in songs) {
            try {
                arr.put(org.json.JSONObject(songToJson(s)))
            } catch (_: Exception) {}
        }
        return arr.toString()
    }

    private fun playlistFromJson(jsonStr: String?): List<Song> {
        if (jsonStr.isNullOrEmpty()) return emptyList()
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val objStr = arr.getJSONObject(i).toString()
                songFromJson(objStr)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePlaybackStateToPrefs() {
        val song = currentSong ?: return
        val preferences = prefs ?: return
        val controller = mediaController
        val posMs = controller?.currentPosition?.takeIf { it >= 0 } ?: (currentPosition * 1000L)
        val index = playlist.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        try {
            preferences.edit().apply {
                putString("last_song_json", songToJson(song))
                putString("last_queue_json", playlistToJson(playlist))
                putLong("last_position_ms", posMs)
                putLong("last_position", posMs)
                putInt("last_index", index)
                putBoolean("is_shuffle", isShuffleEnabled)
                putInt("repeat_mode", when (repeatMode) {
                    RepeatMode.NONE -> 0
                    RepeatMode.ALL  -> 1
                    RepeatMode.ONE  -> 2
                })
                apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerVM", "Error saving playback state to prefs", e)
        }
    }

    enum class RepeatMode { NONE, ONE, ALL }

    fun initializePlayer(context: Context, allSongs: List<Song> = emptyList()) {
        prefs = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
        database = FxzDatabase.getInstance(context.applicationContext)

        if (mediaControllerFuture == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            mediaControllerFuture?.addListener({
                try {
                    mediaController = mediaControllerFuture?.get()
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayerVM", "Failed to build MediaController: ${e.message}", e)
                    mediaControllerFuture = null
                    return@addListener
                }

                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) startPositionUpdates() else stopPositionUpdates()
                        savePlaybackStateToPrefs()
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.let { item ->
                            val song = playlist.find { it.id == item.mediaId }
                            if (song != null) {
                                currentSong = song
                                duration = song.duration
                                currentPosition = 0
                                extractAudioMetadata(song)
                                checkAndReplenishQueue()
                            } else {
                                viewModelScope.launch {
                                    val foundSong = playlist.find { it.id == item.mediaId }
                                    if (foundSong != null) {
                                        currentSong = foundSong
                                        duration = foundSong.duration
                                        extractAudioMetadata(foundSong)
                                        checkAndReplenishQueue()
                                    } else {
                                        currentSong = Song(
                                            id     = item.mediaId ?: "",
                                            title  = item.mediaMetadata.title?.toString() ?: "Desconocida",
                                            artist = item.mediaMetadata.artist?.toString() ?: ""
                                        )
                                        duration = 0
                                        currentAudioMetadata = null
                                    }
                                    currentPosition = 0
                                }
                            }
                            savePlaybackStateToPrefs()
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            duration = ((mediaController?.duration ?: 0L) / 1000).toInt()
                            savePlaybackStateToPrefs()
                        } else if (playbackState == Player.STATE_ENDED) {
                            com.fxzmusic.app.util.EventBus.tryPublish(com.fxzmusic.app.util.UiEvent.TrackEnded)
                        }
                    }
                }
                playerListener = listener
                mediaController?.addListener(listener)
                restorePlaybackState(allSongs)
                applySpeedToController(currentPlaybackSpeed)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun restorePlaybackState(allSongs: List<Song> = emptyList()) {
        val controller = mediaController ?: return

        if (controller.mediaItemCount > 0) {
            val current = controller.currentMediaItem
            if (current != null) {
                val savedQueue = playlistFromJson(prefs?.getString("last_queue_json", null))
                val song = allSongs.find { it.id == current.mediaId }
                    ?: savedQueue.find { it.id == current.mediaId }
                    ?: playlist.find { it.id == current.mediaId }
                    ?: Song(
                        id = current.mediaId ?: "",
                        title = current.mediaMetadata.title?.toString() ?: "Desconocida",
                        artist = current.mediaMetadata.artist?.toString() ?: "",
                        coverUrl = current.mediaMetadata.artworkUri?.toString()
                    )
                currentSong = song
                if (playlist.isEmpty() && savedQueue.isNotEmpty()) {
                    playlist = savedQueue
                }
                duration = ((controller.duration.coerceAtLeast(0L)) / 1000).toInt()
                currentPosition = (controller.currentPosition.coerceAtLeast(0L) / 1000).toInt()
                isPlaying = controller.isPlaying
                if (isPlaying) startPositionUpdates()
            }
            return
        }

        val savedSongJson = prefs?.getString("last_song_json", null)
        var savedSong = songFromJson(savedSongJson)

        if (savedSong == null) {
            val lastIndex = prefs?.getInt("last_index", -1) ?: -1
            if (lastIndex >= 0 && lastIndex < allSongs.size) {
                savedSong = allSongs[lastIndex]
            } else {
                val lastTitle = prefs?.getString("last_title", "") ?: ""
                if (lastTitle.isNotEmpty()) {
                    val lastArtist = prefs?.getString("last_artist", "") ?: ""
                    val lastCover = prefs?.getString("last_cover_url", null)
                    savedSong = allSongs.find { it.title == lastTitle && it.artist == lastArtist }
                        ?: Song(id = "restored_legacy", title = lastTitle, artist = lastArtist, coverUrl = lastCover)
                }
            }
        }

        if (savedSong == null) return

        isRestoringState = true
        val savedQueueJson = prefs?.getString("last_queue_json", null)
        val savedQueue = playlistFromJson(savedQueueJson).ifEmpty {
            if (allSongs.isNotEmpty()) allSongs else listOf(savedSong)
        }
        val savedPositionMs = prefs?.getLong("last_position_ms", prefs?.getLong("last_position", 0L) ?: 0L) ?: 0L
        var savedIndex = savedQueue.indexOfFirst { it.id == savedSong?.id }
        if (savedIndex < 0) savedIndex = (prefs?.getInt("last_index", 0) ?: 0).coerceIn(0, (savedQueue.size - 1).coerceAtLeast(0))

        playlist = savedQueue
        currentSong = savedSong
        duration = savedSong.duration
        currentPosition = (savedPositionMs / 1000).toInt()

        if (!savedSong.isYouTube) {
            extractAudioMetadata(savedSong)
        }

        val mediaItems = savedQueue.map { buildMediaItem(it) }
        controller.setMediaItems(mediaItems, savedIndex, savedPositionMs)
        controller.prepare()
        controller.pause()
        isRestoringState = false
    }

    fun restoreWithSongs(allSongs: List<Song>) {
        if (currentSong == null) {
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
                    if (newPos != currentPosition) {
                        currentPosition = newPos
                        positionSaveCounter++
                        if (positionSaveCounter >= 25) { 
                            positionSaveCounter = 0
                            savePlaybackStateToPrefs()
                        }
                    }
                }
            }
        }
    }

    private fun stopPositionUpdates() {
        savePlaybackStateToPrefs()
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private var allowQueueReplenish = true

    fun playSong(
        song: Song,
        newPlaylist: List<Song> = emptyList(),
        context: Context?,
        allowQueueReplenish: Boolean = true
    ) {
        this.allowQueueReplenish = allowQueueReplenish
        playbackError = null
        android.util.Log.d("MusicPlayerVM", "playSong called: ${song.title}, isYouTube=${song.isYouTube}, videoId=${song.youtubeVideoId}")
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
            extractAudioMetadata(song)
            savePlaybackStateToPrefs()

            viewModelScope.launch {
                ensureControllerReady(context)
                playWithController(context)
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

    private suspend fun ensureControllerReady(context: Context?) {
        if (mediaController != null) return
        if (context == null) return
        val future = mediaControllerFuture
        if (future != null) {
            try {
                kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    while (mediaController == null) {
                        delay(50)
                    }
                }
            } catch (_: Exception) {}
        } else {
            initializePlayer(context)
            kotlinx.coroutines.withTimeoutOrNull(5000L) {
                while (mediaController == null) {
                    delay(50)
                }
            }
        }
    }

    private var isReplenishingQueue = false

    private fun checkAndReplenishQueue() {
        if (!allowQueueReplenish) return
        val controller = mediaController ?: return
        val currentIdx = controller.currentMediaItemIndex
        val count = controller.mediaItemCount
        
        if (count - currentIdx <= 2 && !isReplenishingQueue) {
            val song = currentSong ?: return
            if (song.isYouTube && !song.youtubeVideoId.isNullOrEmpty()) {
                isReplenishingQueue = true
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val result = YouTube.next(WatchEndpoint(videoId = song.youtubeVideoId)).getOrNull()
                        if (result != null && result.items.isNotEmpty()) {
                            val newSongs = result.items.map { it.toSong() }.filter { s ->
                                playlist.none { existing -> existing.id == s.id }
                            }
                            if (newSongs.isNotEmpty()) {
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    playlist = playlist + newSongs
                                    val newMediaItems = newSongs.map { buildMediaItem(it) }
                                    controller.addMediaItems(newMediaItems)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicPlayerVM", "Error replenishing queue", e)
                    } finally {
                        isReplenishingQueue = false
                    }
                }
            }
        }
    }

    private fun playWithController(context: Context?) {
        mediaController?.let { controller ->
            val mediaItems = playlist.map { s -> buildMediaItem(s) }
            val startIndex = playlist.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
            applySpeedToController(currentPlaybackSpeed)
        } ?: run {
            playbackError = "El reproductor no está listo. Intenta de nuevo."
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
        savePlaybackStateToPrefs()
    }

    fun playNext(songs: List<Song>) {
        val controller = mediaController ?: return
        val currentPlaylist = playlist.toMutableList()
        val newSongs = songs.filter { newSong -> currentPlaylist.none { it.id == newSong.id } }
        if (newSongs.isEmpty()) return
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(currentPlaylist.size)
        currentPlaylist.addAll(insertIndex, newSongs)
        playlist = currentPlaylist
        val mediaItems = newSongs.map { s -> buildMediaItem(s) }
        controller.addMediaItems(insertIndex, mediaItems)
        savePlaybackStateToPrefs()
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        if (index < 0 || index >= playlist.size) return
        val wasCurrent = playlist[index].id == currentSong?.id
        val newPlaylist = playlist.toMutableList().apply { removeAt(index) }
        playlist = newPlaylist
        controller.removeMediaItem(index)
        if (wasCurrent) {
            if (newPlaylist.isEmpty()) {
                stopSong()
            } else {
                val nextIdx = index.coerceAtMost(newPlaylist.lastIndex)
                currentSong = newPlaylist[nextIdx]
                duration = currentSong?.duration ?: 0
            }
        }
        savePlaybackStateToPrefs()
    }

    fun removeSongFromQueue(songId: String) {
        val index = playlist.indexOfFirst { it.id == songId }
        if (index >= 0) removeFromQueue(index)
    }

    fun clearQueueKeepCurrent() {
        val controller = mediaController ?: return
        val current = currentSong ?: return
        val singleList = listOf(current)
        playlist = singleList
        val mediaItems = singleList.map { buildMediaItem(it) }
        val currentIdx = controller.currentMediaItemIndex
        val currentPos = controller.currentPosition
        controller.setMediaItems(mediaItems, 0, currentPos)
        controller.prepare()
        savePlaybackStateToPrefs()
    }

    fun moveInQueue(from: Int, to: Int) {
        val controller = mediaController ?: return
        if (from < 0 || from >= playlist.size || to < 0 || to >= playlist.size || from == to) return
        val newPlaylist = playlist.toMutableList()
        val item = newPlaylist.removeAt(from)
        newPlaylist.add(to, item)
        playlist = newPlaylist
        controller.moveMediaItem(from, to)
    }

    fun clearError() { playbackError = null }

    fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed
        applySpeedToController(speed)
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
    fun seekTo(position: Int) {
        mediaController?.seekTo(position * 1000L)
        currentPosition = position
    }
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

    fun openAudioDevices() { showAudioDeviceSheet = true }
    fun closeAudioDevices() { showAudioDeviceSheet = false }

    private fun buildMediaItem(s: Song): MediaItem {
        val rawUriString = when {
            s.isYouTube -> s.youtubeVideoId?.takeIf { it.isNotEmpty() } ?: s.id
            s.filePath.isNotEmpty() -> s.filePath
            else -> s.id
        }

        val uri = when {
            rawUriString.startsWith("http://") || rawUriString.startsWith("https://") -> android.net.Uri.parse(rawUriString)
            rawUriString.startsWith("content://") || rawUriString.startsWith("file://") -> android.net.Uri.parse(rawUriString)
            rawUriString.startsWith("/") -> android.net.Uri.fromFile(File(rawUriString))
            s.isYouTube -> android.net.Uri.parse("https://music.youtube.com/watch?v=$rawUriString")
            else -> android.net.Uri.parse("file://$rawUriString")
        }

        val artworkUri = s.coverUrl?.takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) }
            ?: if (s.filePath.isNotEmpty()) android.net.Uri.fromFile(File(s.filePath)) else null

        val meta = MediaMetadata.Builder()
            .setTitle(s.title)
            .setArtist(s.artist)
            .setAlbumTitle(s.album)
        if (artworkUri != null) meta.setArtworkUri(artworkUri)

        val cacheKey = if (s.isYouTube) (s.youtubeVideoId?.takeIf { it.isNotEmpty() } ?: s.id) else null

        return MediaItem.Builder()
            .setMediaId(s.id)
            .setUri(uri)
            .setCustomCacheKey(cacheKey)
            .setMediaMetadata(meta.build())
            .build()
    }

    private val metadataCache = mutableMapOf<String, AudioMetadata>()

    private fun extractAudioMetadata(song: Song) {
        val cached = metadataCache[song.id]
        if (cached != null) {
            currentAudioMetadata = cached
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = if (!song.isYouTube && !song.filePath.isNullOrEmpty()) {
                MetadataUtils.extractAudioMetadata(song.filePath)
            } else {
                val videoId = song.youtubeVideoId?.takeIf { it.isNotEmpty() } ?: song.id
                val format = database?.songFormatDao()?.get(videoId)
                if (format != null) {
                    AudioMetadata(
                        bitrate = format.bitrate,
                        sampleRate = format.sampleRate,
                        channels = 2,
                        mimeType = format.mimeType,
                        codecString = format.codecs,
                        fileSize = format.contentLength
                    )
                } else {
                    AudioMetadata(
                        bitrate = 160_000,
                        sampleRate = 48_000,
                        channels = 2,
                        mimeType = "audio/opus",
                        codecString = "opus"
                    )
                }
            }
            withContext(Dispatchers.Main) {
                currentAudioMetadata = metadata
                if (metadata != null) metadataCache[song.id] = metadata
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        playerListener?.let { listener -> mediaController?.removeListener(listener) }
        playerListener = null
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
