package com.fxzmusic.app.service

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.fxzmusic.app.R
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.data.SongMetaEntity
import com.fxzmusic.app.data.YtLikedSongEntity
import com.fxzmusic.app.util.EventBus
import com.fxzmusic.app.util.UiEvent
import com.fxzmusic.app.util.VoiceCommandCleaner
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.models.WatchEndpoint
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FxzMediaLibrarySessionCallback(
    private val context: Context,
    private val database: FxzDatabase,
    private val serviceScope: CoroutineScope
) : MediaLibraryService.MediaLibrarySession.Callback {

    companion object {
        const val ROOT_ID = "ROOT"
        const val FAVORITES_PATH = "FAVORITES"
        const val DOWNLOADS_PATH = "DOWNLOADS"
        const val PLAYLISTS_PATH = "PLAYLISTS"
        const val ARTISTS_PATH = "ARTISTS"
        const val ALBUMS_PATH = "ALBUMS"
        const val RECOMMENDATIONS_PATH = "RECOMMENDATIONS"

        const val CUSTOM_ACTION_TOGGLE_LIKE = "com.fxzmusic.app.ACTION_TOGGLE_LIKE"
        const val CUSTOM_ACTION_START_RADIO = "com.fxzmusic.app.ACTION_START_RADIO"
        const val CUSTOM_ACTION_SHUFFLE = "com.fxzmusic.app.ACTION_SHUFFLE"
    }

    private fun <T> coroutineFuture(
        scope: CoroutineScope,
        block: suspend () -> T
    ): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        val job = scope.launch {
            try {
                future.set(block())
            } catch (e: Throwable) {
                future.setException(e)
            }
        }
        future.addListener({
            if (future.isCancelled) {
                job.cancel()
            }
        }, Runnable::run)
        return future
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .add(SessionCommand(CUSTOM_ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .add(SessionCommand(CUSTOM_ACTION_START_RADIO, Bundle.EMPTY))
            .add(SessionCommand(CUSTOM_ACTION_SHUFFLE, Bundle.EMPTY))
            .build()

        val initialButtons = ImmutableList.of(
            CommandButton.Builder()
                .setDisplayName(context.getString(R.string.android_auto_action_like))
                .setIconResId(R.drawable.ic_heart)
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                .build(),
            CommandButton.Builder()
                .setDisplayName(context.getString(R.string.android_auto_action_radio))
                .setIconResId(R.drawable.ic_radio)
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_START_RADIO, Bundle.EMPTY))
                .build(),
            CommandButton.Builder()
                .setDisplayName(context.getString(R.string.android_auto_action_shuffle))
                .setIconResId(R.drawable.ic_shuffle)
                .setSessionCommand(SessionCommand(CUSTOM_ACTION_SHUFFLE, Bundle.EMPTY))
                .build()
        )

        serviceScope.launch {
            updateCustomLayout(session)
        }

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands)
            .setCustomLayout(initialButtons)
            .build()
    }

    suspend fun updateCustomLayout(session: MediaSession) {
        val mediaId = withContext(Dispatchers.Main) { session.player.currentMediaItem?.mediaId }
        val isLiked = if (!mediaId.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                checkIfLiked(mediaId)
            }
        } else {
            false
        }

        val likeIcon = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
        val likeTitle = context.getString(if (isLiked) R.string.android_auto_action_unlike else R.string.android_auto_action_like)

        val likeButton = CommandButton.Builder()
            .setDisplayName(likeTitle)
            .setIconResId(likeIcon)
            .setSessionCommand(SessionCommand(CUSTOM_ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .build()

        val radioButton = CommandButton.Builder()
            .setDisplayName(context.getString(R.string.android_auto_action_radio))
            .setIconResId(R.drawable.ic_radio)
            .setSessionCommand(SessionCommand(CUSTOM_ACTION_START_RADIO, Bundle.EMPTY))
            .build()

        val shuffleButton = CommandButton.Builder()
            .setDisplayName(context.getString(R.string.android_auto_action_shuffle))
            .setIconResId(R.drawable.ic_shuffle)
            .setSessionCommand(SessionCommand(CUSTOM_ACTION_SHUFFLE, Bundle.EMPTY))
            .build()

        withContext(Dispatchers.Main) {
            session.setCustomLayout(ImmutableList.of(likeButton, radioButton, shuffleButton))
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return coroutineFuture(serviceScope) {
            when (customCommand.customAction) {
                CUSTOM_ACTION_TOGGLE_LIKE -> {
                    val currentItem = withContext(Dispatchers.Main) { session.player.currentMediaItem }
                    val mediaId = currentItem?.mediaId
                    if (!mediaId.isNullOrEmpty()) {
                        val newIsLiked = withContext(Dispatchers.IO) {
                            toggleLikeInDatabase(mediaId, currentItem)
                        }
                        EventBus.tryPublish(UiEvent.LikeChanged(mediaId, newIsLiked))
                        updateCustomLayout(session)
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                CUSTOM_ACTION_START_RADIO -> {
                    val currentItem = withContext(Dispatchers.Main) { session.player.currentMediaItem }
                    val mediaId = currentItem?.mediaId
                    if (!mediaId.isNullOrEmpty()) {
                        val cleanId = mediaId.removePrefix("yt:")
                        val radioTracks = withContext(Dispatchers.IO) {
                            fetchRadioTracks(cleanId)
                        }
                        if (radioTracks.isNotEmpty()) {
                            val mediaItems = radioTracks.map { track ->
                                buildPlayableMediaItem(
                                    mediaId = track.id,
                                    title = track.title,
                                    artist = track.artist,
                                    album = track.album,
                                    artworkUri = track.coverUrl?.toUri()
                                )
                            }
                            withContext(Dispatchers.Main) {
                                session.player.addMediaItems(mediaItems)
                            }
                        }
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                CUSTOM_ACTION_SHUFFLE -> {
                    withContext(Dispatchers.Main) {
                        val newShuffle = !session.player.shuffleModeEnabled
                        session.player.shuffleModeEnabled = newShuffle
                        val prefs = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_shuffle", newShuffle).apply()
                    }
                    updateCustomLayout(session)
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                else -> SessionResult(SessionResult.RESULT_SUCCESS)
            }
        }
    }

    private suspend fun checkIfLiked(mediaId: String): Boolean {
        val cleanId = mediaId.removePrefix("yt:")
        val meta = database.songMetaDao().getAll().find { it.songId == cleanId || it.songId == mediaId }
        if (meta != null) return meta.isLiked
        val unified = database.libraryDao().getUnifiedSong(cleanId) ?: database.libraryDao().getUnifiedSong(mediaId)
        if (unified != null) return unified.song.liked
        return database.ytLikedSongDao().getAll().any { it.videoId == cleanId || it.videoId == mediaId }
    }

    private suspend fun toggleLikeInDatabase(mediaId: String, currentItem: MediaItem): Boolean {
        val cleanId = mediaId.removePrefix("yt:")
        val currentlyLiked = checkIfLiked(mediaId)
        val newLiked = !currentlyLiked
        val now = System.currentTimeMillis()

        val existingMeta = database.songMetaDao().getAll().find { it.songId == cleanId || it.songId == mediaId }
        database.songMetaDao().upsert(
            SongMetaEntity(
                songId = cleanId,
                playCount = existingMeta?.playCount ?: 0,
                lastPlayed = existingMeta?.lastPlayed ?: now,
                isLiked = newLiked
            )
        )

        if (newLiked) {
            val title = currentItem.mediaMetadata.title?.toString() ?: "Unknown"
            val artist = currentItem.mediaMetadata.artist?.toString() ?: "Unknown"
            val album = currentItem.mediaMetadata.albumTitle?.toString()
            val thumb = currentItem.mediaMetadata.artworkUri?.toString()
            database.ytLikedSongDao().upsertAll(
                listOf(
                    YtLikedSongEntity(
                        videoId = cleanId,
                        title = title,
                        artist = artist,
                        artistId = null,
                        album = album,
                        albumId = null,
                        duration = null,
                        thumbnail = thumb,
                        syncedAt = now
                    )
                )
            )
        } else {
            database.ytLikedSongDao().deleteById(cleanId)
            database.ytLikedSongDao().deleteById(mediaId)
        }
        return newLiked
    }

    private suspend fun fetchRadioTracks(videoId: String): List<Song> {
        return try {
            val radioNext = YouTube.next(WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")).getOrNull()
            val items = radioNext?.items?.filterIsInstance<SongItem>()
                ?: YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()?.items?.filterIsInstance<SongItem>()
                ?: emptyList()
            items.map { item ->
                Song(
                    id = item.id,
                    title = item.title,
                    artist = item.artists.joinToString(", ") { it.name },
                    album = item.album?.name ?: "Radio",
                    duration = item.duration ?: 0,
                    coverUrl = item.thumbnail,
                    isYouTube = true,
                    youtubeVideoId = item.id,
                    youtubeThumbnailUrl = item.thumbnail
                )
            }.filter { it.id != videoId }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onSetMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return coroutineFuture(serviceScope) {
            val resolvedItems = mediaItems.map { rawItem ->
                if (rawItem.localConfiguration != null) {
                    rawItem
                } else {
                    resolveMediaItem(rawItem.mediaId) ?: rawItem
                }
            }
            val validItems = resolvedItems.filter { it.localConfiguration != null }
            if (validItems.isNotEmpty()) {
                val validIndex = startIndex.coerceIn(0, validItems.lastIndex)
                val validPosition = startPositionMs.coerceAtLeast(0L)
                withContext(Dispatchers.Main) {
                    session.player.setMediaItems(validItems, validIndex, validPosition)
                }
                MediaSession.MediaItemsWithStartPosition(validItems, validIndex, validPosition)
            } else {
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, C.TIME_UNSET)
            }
        }
    }

    override fun onAddMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        return coroutineFuture(serviceScope) {
            val resolvedItems = mediaItems.map { rawItem ->
                if (rawItem.localConfiguration != null) {
                    rawItem
                } else {
                    resolveMediaItem(rawItem.mediaId) ?: rawItem
                }
            }.filter { it.localConfiguration != null }.toMutableList()
            if (resolvedItems.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    session.player.addMediaItems(resolvedItems)
                }
            }
            resolvedItems
        }
    }

    override fun onPlaybackResumption(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return coroutineFuture(serviceScope) {
            val (items, index, position) = withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
                val savedQueueJson = prefs.getString("last_queue_json", null)
                val savedSongs = deserializeQueue(savedQueueJson)
                val savedIndex = prefs.getInt("last_index", 0)
                val savedPos = prefs.getLong("last_position_ms", prefs.getLong("last_position", 0L)).coerceAtLeast(0L)

                if (savedSongs.isNotEmpty()) {
                    val mediaItems = savedSongs.map { s ->
                        buildPlayableMediaItem(
                            mediaId = s.id,
                            title = s.title,
                            artist = s.artist,
                            album = s.album,
                            artworkUri = s.coverUrl?.toUri(),
                            filePath = s.filePath
                        )
                    }
                    val safeIndex = savedIndex.coerceIn(0, mediaItems.lastIndex)
                    Triple(mediaItems, safeIndex, savedPos)
                } else {
                    val history = database.playbackHistoryDao().getRecent(20)
                    if (history.isNotEmpty()) {
                        val mediaItems = history.map { h ->
                            buildPlayableMediaItem(
                                mediaId = h.songId,
                                title = h.title.ifBlank { "Canción" },
                                artist = h.artist.ifBlank { "Artista" },
                                artworkUri = h.thumbnail.takeIf { it.isNotBlank() }?.toUri()
                            )
                        }
                        Triple(mediaItems, 0, 0L)
                    } else {
                        Triple(emptyList(), 0, C.TIME_UNSET)
                    }
                }
            }

            if (items.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    session.player.setMediaItems(items, index, position)
                    session.player.prepare()
                }
                MediaSession.MediaItemsWithStartPosition(items, index, position)
            } else {
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, C.TIME_UNSET)
            }
        }
    }

    private fun deserializeQueue(jsonStr: String?): List<Song> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isNotEmpty()) {
                    val isYT = obj.optBoolean("isYouTube", false)
                    val ytId = obj.optString("youtubeVideoId").takeIf { it.isNotEmpty() } ?: if (isYT) id else null
                    list.add(
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
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val item = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(context.getString(R.string.app_name))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(item, params))
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return coroutineFuture(serviceScope) {
            if (mediaId == ROOT_ID) {
                val item = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(context.getString(R.string.app_name))
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build()
                LibraryResult.ofItem(item, null)
            } else {
                when {
                    mediaId == FAVORITES_PATH -> LibraryResult.ofItem(browsable(FAVORITES_PATH, context.getString(R.string.android_auto_favorites), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS), null)
                    mediaId == DOWNLOADS_PATH -> LibraryResult.ofItem(browsable(DOWNLOADS_PATH, context.getString(R.string.android_auto_downloads), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED), null)
                    mediaId == PLAYLISTS_PATH -> LibraryResult.ofItem(browsable(PLAYLISTS_PATH, context.getString(R.string.android_auto_playlists), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS), null)
                    mediaId == ARTISTS_PATH -> LibraryResult.ofItem(browsable(ARTISTS_PATH, context.getString(R.string.android_auto_artists), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS), null)
                    mediaId == ALBUMS_PATH -> LibraryResult.ofItem(browsable(ALBUMS_PATH, context.getString(R.string.android_auto_albums), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS), null)
                    mediaId == RECOMMENDATIONS_PATH -> LibraryResult.ofItem(browsable(RECOMMENDATIONS_PATH, context.getString(R.string.android_auto_recommendations), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED), null)
                    mediaId.startsWith("PLAYLIST_") -> {
                        val playlistId = mediaId.removePrefix("PLAYLIST_")
                        val title = withContext(Dispatchers.IO) {
                            val local = playlistId.toLongOrNull()?.let { id -> database.playlistDao().getAll().find { it.id == id } }
                            local?.name ?: database.ytPlaylistDao().getAll().find { it.playlistId == playlistId }?.title ?: "Playlist"
                        }
                        LibraryResult.ofItem(browsable(mediaId, title, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS), null)
                    }
                    mediaId.startsWith("ARTIST_") -> {
                        val artistId = mediaId.removePrefix("ARTIST_")
                        val name = withContext(Dispatchers.IO) {
                            val local = database.libraryDao().getAllUnifiedSongs().flatMap { it.artists }.find { it.id == artistId || it.name == artistId }
                            local?.name ?: database.ytArtistDao().getAll().find { it.channelId == artistId }?.name ?: "Artista"
                        }
                        LibraryResult.ofItem(browsable(mediaId, name, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS), null)
                    }
                    mediaId.startsWith("ALBUM_") -> {
                        val albumId = mediaId.removePrefix("ALBUM_")
                        val title = withContext(Dispatchers.IO) {
                            val local = database.libraryDao().getAllUnifiedSongs().mapNotNull { it.album }.find { it.id == albumId }
                            local?.title ?: database.ytAlbumDao().getAll().find { it.browseId == albumId }?.title ?: "Álbum"
                        }
                        LibraryResult.ofItem(browsable(mediaId, title, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS), null)
                    }
                    else -> {
                        val resolved = resolveMediaItem(mediaId)
                        if (resolved != null) {
                            LibraryResult.ofItem(resolved, null)
                        } else {
                            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                        }
                    }
                }
            }
        }
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return coroutineFuture(serviceScope) {
            val allItems: List<MediaItem> = withContext(Dispatchers.IO) {
                when {
                    parentId == ROOT_ID -> {
                        listOf(
                            browsable(FAVORITES_PATH, context.getString(R.string.android_auto_favorites), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                            browsable(DOWNLOADS_PATH, context.getString(R.string.android_auto_downloads), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                            browsable(PLAYLISTS_PATH, context.getString(R.string.android_auto_playlists), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                            browsable(ARTISTS_PATH, context.getString(R.string.android_auto_artists), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                            browsable(ALBUMS_PATH, context.getString(R.string.android_auto_albums), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                            browsable(RECOMMENDATIONS_PATH, context.getString(R.string.android_auto_recommendations), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        )
                    }
                    parentId == FAVORITES_PATH -> {
                        val likedYt = database.ytLikedSongDao().getAll().map { entity ->
                            buildPlayableMediaItem(
                                mediaId = entity.videoId,
                                title = entity.title,
                                artist = entity.artist,
                                album = entity.album,
                                artworkUri = entity.thumbnail?.toUri()
                            )
                        }
                        val likedUnified = database.libraryDao().getAllUnifiedSongs().filter { it.song.liked }.map { u ->
                            val s = u.toSongDataClass()
                            buildPlayableMediaItem(
                                mediaId = s.id,
                                title = s.title,
                                artist = s.artist,
                                album = s.album,
                                artworkUri = s.coverUrl?.toUri(),
                                filePath = s.filePath
                            )
                        }
                        (likedYt + likedUnified).distinctBy { it.mediaId }
                    }
                    parentId == DOWNLOADS_PATH -> {
                        database.downloadDao().getAll().map { entity ->
                            buildPlayableMediaItem(
                                mediaId = entity.videoId,
                                title = entity.title,
                                artist = entity.artist,
                                album = entity.album.ifBlank { "YouTube Downloads" },
                                artworkUri = entity.coverUrl?.toUri()
                            )
                        }
                    }
                    parentId == PLAYLISTS_PATH -> {
                        val localPlaylists = database.playlistDao().getAll().map { p ->
                            browsable("PLAYLIST_${p.id}", p.name, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, p.coverUrl?.toUri())
                        }
                        val ytPlaylists = database.ytPlaylistDao().getAll().map { p ->
                            browsable("PLAYLIST_YT_${p.playlistId}", p.title, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, p.thumbnail?.toUri())
                        }
                        (localPlaylists + ytPlaylists).distinctBy { it.mediaId }
                    }
                    parentId.startsWith("PLAYLIST_YT_") -> {
                        val ytPlaylistId = parentId.removePrefix("PLAYLIST_YT_")
                        val playlistResult = YouTube.playlist(ytPlaylistId).getOrNull()
                        val songItems = playlistResult?.songs.orEmpty()
                        songItems.map { item ->
                            buildPlayableMediaItem(
                                mediaId = item.id,
                                title = item.title,
                                artist = item.artists.joinToString(", ") { it.name },
                                album = item.album?.name ?: playlistResult?.playlist?.title,
                                artworkUri = item.thumbnail.toUri()
                            )
                        }
                    }
                    parentId.startsWith("PLAYLIST_") -> {
                        val playlistId = parentId.removePrefix("PLAYLIST_").toLongOrNull()
                        if (playlistId != null) {
                            val playlistSongs = database.playlistDao().getSongsForPlaylist(playlistId)
                            val resolvedSongs = mutableListOf<MediaItem>()
                            for (ps in playlistSongs) {
                                val item = resolveMediaItem(ps.songId)
                                if (item != null) resolvedSongs.add(item)
                            }
                            resolvedSongs
                        } else {
                            emptyList()
                        }
                    }
                    parentId == ARTISTS_PATH -> {
                        val localArtists = database.libraryDao().getAllUnifiedSongs().flatMap { it.artists }.distinctBy { it.id }.map { a ->
                            browsable("ARTIST_${a.id}", a.name, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS, a.imageUrl?.toUri())
                        }
                        val ytArtists = database.ytArtistDao().getAll().map { a ->
                            browsable("ARTIST_YT_${a.channelId}", a.name, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS, a.thumbnail?.toUri())
                        }
                        (localArtists + ytArtists).distinctBy { it.mediaId }
                    }
                    parentId.startsWith("ARTIST_YT_") -> {
                        val channelId = parentId.removePrefix("ARTIST_YT_")
                        val artistResult = YouTube.artist(channelId).getOrNull()
                        val songs = artistResult?.sections?.flatMap { it.items.filterIsInstance<SongItem>() }.orEmpty()
                        songs.map { item ->
                            buildPlayableMediaItem(
                                mediaId = item.id,
                                title = item.title,
                                artist = item.artists.joinToString(", ") { it.name },
                                album = item.album?.name,
                                artworkUri = item.thumbnail.toUri()
                            )
                        }
                    }
                    parentId.startsWith("ARTIST_") -> {
                        val artistId = parentId.removePrefix("ARTIST_")
                        database.libraryDao().getAllUnifiedSongs()
                            .filter { it.artists.any { a -> a.id == artistId || a.name.equals(artistId, ignoreCase = true) } }
                            .map { u ->
                                val s = u.toSongDataClass()
                                buildPlayableMediaItem(
                                    mediaId = s.id,
                                    title = s.title,
                                    artist = s.artist,
                                    album = s.album,
                                    artworkUri = s.coverUrl?.toUri(),
                                    filePath = s.filePath
                                )
                            }
                    }
                    parentId == ALBUMS_PATH -> {
                        val ytAlbums = database.ytAlbumDao().getAll().map { album ->
                            browsable("ALBUM_${album.browseId}", album.title, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, album.thumbnail?.toUri())
                        }
                        val localAlbums = database.libraryDao().getAllUnifiedSongs().mapNotNull { it.album }.distinctBy { it.id }.map { album ->
                            browsable("ALBUM_${album.id}", album.title, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, album.coverUrl?.toUri())
                        }
                        (ytAlbums + localAlbums).distinctBy { it.mediaId }
                    }
                    parentId.startsWith("ALBUM_") -> {
                        val albumId = parentId.removePrefix("ALBUM_")
                        val localMatches = database.libraryDao().getAllUnifiedSongs()
                            .filter { it.album?.id == albumId || it.song.albumId == albumId }
                            .map { u ->
                                val s = u.toSongDataClass()
                                buildPlayableMediaItem(
                                    mediaId = s.id,
                                    title = s.title,
                                    artist = s.artist,
                                    album = s.album,
                                    artworkUri = s.coverUrl?.toUri(),
                                    filePath = s.filePath
                                )
                            }
                        if (localMatches.isNotEmpty()) {
                            localMatches
                        } else {
                            val ytAlbum = YouTube.album(albumId, withSongs = true).getOrNull()
                            ytAlbum?.songs?.map { item ->
                                buildPlayableMediaItem(
                                    mediaId = item.id,
                                    title = item.title,
                                    artist = item.artists.joinToString(", ") { it.name },
                                    album = ytAlbum.album.title,
                                    artworkUri = item.thumbnail.toUri()
                                )
                            }.orEmpty()
                        }
                    }
                    parentId == RECOMMENDATIONS_PATH -> {
                        val recentHistory = database.playbackHistoryDao().getRecent(50).map { h ->
                            buildPlayableMediaItem(
                                mediaId = h.songId,
                                title = h.title.ifBlank { "Canción" },
                                artist = h.artist.ifBlank { "Artista" },
                                artworkUri = h.thumbnail.takeIf { it.isNotBlank() }?.toUri()
                            )
                        }
                        if (recentHistory.isNotEmpty()) {
                            recentHistory.distinctBy { it.mediaId }
                        } else {
                            database.libraryDao().getAllUnifiedSongs().take(50).map { u ->
                                val s = u.toSongDataClass()
                                buildPlayableMediaItem(
                                    mediaId = s.id,
                                    title = s.title,
                                    artist = s.artist,
                                    album = s.album,
                                    artworkUri = s.coverUrl?.toUri(),
                                    filePath = s.filePath
                                )
                            }
                        }
                    }
                    else -> emptyList()
                }
            }

            val pagedItems = if (pageSize > 0 && page >= 0) {
                val fromIndex = (page * pageSize).coerceAtMost(allItems.size)
                val toIndex = (fromIndex + pageSize).coerceAtMost(allItems.size)
                if (fromIndex < allItems.size) allItems.subList(fromIndex, toIndex) else emptyList()
            } else {
                allItems
            }

            LibraryResult.ofItemList(ImmutableList.copyOf(pagedItems), params)
        }
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return coroutineFuture(serviceScope) {
            val q = VoiceCommandCleaner.clean(query).trim().lowercase()
            if (q.isBlank()) {
                LibraryResult.ofItemList(ImmutableList.of(), params)
            } else {
                val results = withContext(Dispatchers.IO) {
                    val combined = mutableListOf<MediaItem>()

                    val localMatches = database.libraryDao().getAllUnifiedSongs()
                        .filter {
                            it.song.title.lowercase().contains(q) ||
                            it.artists.any { a -> a.name.lowercase().contains(q) } ||
                            (it.album?.title?.lowercase()?.contains(q) == true)
                        }
                        .map { u ->
                            val s = u.toSongDataClass()
                            buildPlayableMediaItem(
                                mediaId = s.id,
                                title = s.title,
                                artist = s.artist,
                                album = s.album,
                                artworkUri = s.coverUrl?.toUri(),
                                filePath = s.filePath
                            )
                        }
                    combined.addAll(localMatches)

                    val downloadMatches = database.downloadDao().getAll()
                        .filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
                        .map { entity ->
                            buildPlayableMediaItem(
                                mediaId = entity.videoId,
                                title = entity.title,
                                artist = entity.artist,
                                album = entity.album.ifBlank { "YouTube Downloads" },
                                artworkUri = entity.coverUrl?.toUri()
                            )
                        }
                    combined.addAll(downloadMatches)

                    val likedMatches = database.ytLikedSongDao().getAll()
                        .filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
                        .map { entity ->
                            buildPlayableMediaItem(
                                mediaId = entity.videoId,
                                title = entity.title,
                                artist = entity.artist,
                                album = entity.album,
                                artworkUri = entity.thumbnail?.toUri()
                            )
                        }
                    combined.addAll(likedMatches)

                    val historyMatches = database.playbackHistoryDao().getRecent(100)
                        .filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
                        .map { h ->
                            buildPlayableMediaItem(
                                mediaId = h.songId,
                                title = h.title.ifBlank { "Canción" },
                                artist = h.artist.ifBlank { "Artista" },
                                artworkUri = h.thumbnail.takeIf { it.isNotBlank() }?.toUri()
                            )
                        }
                    combined.addAll(historyMatches)

                    val distinctLocal = combined.distinctBy { it.mediaId }.toMutableList()

                    if (distinctLocal.size < 5) {
                        try {
                            val ytResult = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            val onlineItems = ytResult?.items?.filterIsInstance<SongItem>()?.map { songItem ->
                                buildPlayableMediaItem(
                                    mediaId = songItem.id,
                                    title = songItem.title,
                                    artist = songItem.artists.joinToString(", ") { it.name },
                                    album = songItem.album?.name,
                                    artworkUri = songItem.thumbnail.toUri()
                                )
                            }.orEmpty()
                            distinctLocal.addAll(onlineItems)
                        } catch (_: Exception) {}
                    }

                    distinctLocal.distinctBy { it.mediaId }
                }

                val paged = if (pageSize > 0 && page >= 0) {
                    val fromIndex = (page * pageSize).coerceAtMost(results.size)
                    val toIndex = (fromIndex + pageSize).coerceAtMost(results.size)
                    if (fromIndex < results.size) results.subList(fromIndex, toIndex) else emptyList()
                } else {
                    results
                }

                LibraryResult.ofItemList(ImmutableList.copyOf(paged), params)
            }
        }
    }

    private suspend fun resolveMediaItem(mediaId: String): MediaItem? {
        if (mediaId.isBlank()) return null
        val cleanId = mediaId.removePrefix("yt:")

        val downloads = database.downloadDao().getAll()
        val download = downloads.find { it.videoId == cleanId || it.videoId == mediaId }
        if (download != null) {
            return buildPlayableMediaItem(
                mediaId = download.videoId,
                title = download.title,
                artist = download.artist,
                album = download.album.ifBlank { "YouTube Downloads" },
                artworkUri = download.coverUrl?.toUri()
            )
        }

        val unifiedSong = database.libraryDao().getUnifiedSong(cleanId) ?: database.libraryDao().getUnifiedSong(mediaId)
        if (unifiedSong != null) {
            val song = unifiedSong.toSongDataClass()
            return buildPlayableMediaItem(
                mediaId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                artworkUri = song.coverUrl?.toUri(),
                filePath = song.filePath
            )
        }

        val ytLiked = database.ytLikedSongDao().getAll().find { it.videoId == cleanId || it.videoId == mediaId }
        if (ytLiked != null) {
            return buildPlayableMediaItem(
                mediaId = ytLiked.videoId,
                title = ytLiked.title,
                artist = ytLiked.artist,
                album = ytLiked.album ?: context.getString(R.string.android_auto_favorites),
                artworkUri = ytLiked.thumbnail?.toUri()
            )
        }

        val history = database.playbackHistoryDao().getBySong(cleanId).firstOrNull()
            ?: database.playbackHistoryDao().getBySong(mediaId).firstOrNull()
        if (history != null) {
            return buildPlayableMediaItem(
                mediaId = history.songId,
                title = history.title.ifBlank { "Canción" },
                artist = history.artist.ifBlank { "Artista" },
                artworkUri = history.thumbnail.takeIf { it.isNotBlank() }?.toUri()
            )
        }

        if (cleanId.length == 11 && cleanId.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            return buildPlayableMediaItem(
                mediaId = cleanId,
                title = "YouTube Track",
                artist = "YouTube",
                artworkUri = "https://i.ytimg.com/vi/$cleanId/hqdefault.jpg".toUri()
            )
        }

        return null
    }

    private fun buildPlayableMediaItem(
        mediaId: String,
        title: String,
        artist: String,
        album: String? = null,
        artworkUri: Uri? = null,
        filePath: String = ""
    ): MediaItem {
        val isLocal = filePath.isNotEmpty()
        val cleanId = mediaId.removePrefix("yt:")
        val uri = if (isLocal) filePath else "yt://$cleanId"
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        if (artworkUri != null) {
            meta.setArtworkUri(artworkUri)
        }
        val builder = MediaItem.Builder()
            .setMediaId(cleanId)
            .setUri(uri)
            .setMediaMetadata(meta.build())
        if (!isLocal) {
            builder.setCustomCacheKey(cleanId)
        }
        return builder.build()
    }

    private fun browsable(id: String, title: String, mediaType: Int, artworkUri: Uri? = null): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .apply {
                    if (artworkUri != null) setArtworkUri(artworkUri)
                }
                .build()
        )
        .build()
}
