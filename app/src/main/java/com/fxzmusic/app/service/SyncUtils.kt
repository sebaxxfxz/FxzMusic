package com.fxzmusic.app.service

import android.content.Context
import android.util.Log
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.data.YtAlbumEntity
import com.fxzmusic.app.data.YtArtistEntity
import com.fxzmusic.app.data.YtLikedSongEntity
import com.fxzmusic.app.data.YtPlaylistEntity
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.AlbumItem
import com.fxzmusic.innertube.models.ArtistItem
import com.fxzmusic.innertube.models.PlaylistItem
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SyncUtils private constructor(context: Context) {

    private val db = FxzDatabase.getInstance(context)

    data class SyncState(
        val likedSongs: SyncStatus = SyncStatus.Idle,
        val albums: SyncStatus = SyncStatus.Idle,
        val artists: SyncStatus = SyncStatus.Idle,
        val playlists: SyncStatus = SyncStatus.Idle,
        val currentOperation: String = "",
    ) {
        val isRunning: Boolean
            get() = likedSongs == SyncStatus.Running || albums == SyncStatus.Running ||
                    artists == SyncStatus.Running || playlists == SyncStatus.Running
    }

    sealed class SyncStatus {
        data object Idle : SyncStatus()
        data object Running : SyncStatus()
        data class Completed(val count: Int = 0) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private var syncMutex: kotlinx.coroutines.sync.Mutex? = null

    suspend fun fullSync() {
        if (syncMutex?.isLocked == true) return
        syncMutex = kotlinx.coroutines.sync.Mutex()
        syncMutex!!.lock()
        try {
            syncLikedSongs()
            syncAlbums()
            syncArtists()
            syncPlaylists()
        } finally {
            syncMutex!!.unlock()
            syncMutex = null
        }
    }

    suspend fun syncLikedSongs() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(likedSongs = SyncStatus.Running, currentOperation = "Sincronizando canciones likeadas")
        try {
            val page = YouTube.playlist("LM").completed().getOrThrow()
            val songs = page.songs
            val entities = songs.map { song -> song.toLikedSongEntity() }
            db.ytLikedSongDao().deleteAll()
            db.ytLikedSongDao().upsertAll(entities)
            _state.value = _state.value.copy(likedSongs = SyncStatus.Completed(entities.size))
            Log.d("SyncUtils", "Synced ${entities.size} liked songs")
        } catch (e: Exception) {
            Log.e("SyncUtils", "Failed to sync liked songs", e)
            _state.value = _state.value.copy(likedSongs = SyncStatus.Error(e.message ?: "Error desconocido"))
        }
    }

    suspend fun syncAlbums() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(albums = SyncStatus.Running, currentOperation = "Sincronizando álbumes")
        try {
            val page = YouTube.library("FEmusic_liked_albums").completed().getOrThrow()
            val albums = page.items.filterIsInstance<AlbumItem>()
            val entities = albums.map { album ->
                YtAlbumEntity(
                    browseId = album.browseId,
                    playlistId = album.playlistId,
                    title = album.title,
                    artist = album.artists?.joinToString(", ") { it.name },
                    year = album.year,
                    thumbnail = album.thumbnail,
                )
            }
            db.ytAlbumDao().deleteAll()
            db.ytAlbumDao().upsertAll(entities)
            _state.value = _state.value.copy(albums = SyncStatus.Completed(entities.size))
            Log.d("SyncUtils", "Synced ${entities.size} albums")
        } catch (e: Exception) {
            Log.e("SyncUtils", "Failed to sync albums", e)
            _state.value = _state.value.copy(albums = SyncStatus.Error(e.message ?: "Error desconocido"))
        }
    }

    suspend fun syncArtists() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(artists = SyncStatus.Running, currentOperation = "Sincronizando artistas")
        try {
            val page = YouTube.library("FEmusic_library_corpus_artists").completed().getOrThrow()
            val artists = page.items.filterIsInstance<ArtistItem>()
            val entities = artists.map { artist ->
                YtArtistEntity(
                    channelId = artist.id,
                    name = artist.title,
                    thumbnail = artist.thumbnail,
                )
            }
            db.ytArtistDao().deleteAll()
            db.ytArtistDao().upsertAll(entities)
            _state.value = _state.value.copy(artists = SyncStatus.Completed(entities.size))
            Log.d("SyncUtils", "Synced ${entities.size} artists")
        } catch (e: Exception) {
            Log.e("SyncUtils", "Failed to sync artists", e)
            _state.value = _state.value.copy(artists = SyncStatus.Error(e.message ?: "Error desconocido"))
        }
    }

    suspend fun syncPlaylists() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(playlists = SyncStatus.Running, currentOperation = "Sincronizando playlists")
        try {
            val page = YouTube.library("FEmusic_liked_playlists").completed().getOrThrow()
            val playlists = page.items.filterIsInstance<PlaylistItem>()
            val entities = playlists.map { pl ->
                YtPlaylistEntity(
                    playlistId = pl.id,
                    title = pl.title,
                    author = pl.author?.name,
                    songCount = pl.songCountText?.replace(Regex("[^0-9]"), "")?.toIntOrNull(),
                    thumbnail = pl.thumbnail,
                )
            }
            db.ytPlaylistDao().deleteAll()
            db.ytPlaylistDao().upsertAll(entities)
            _state.value = _state.value.copy(playlists = SyncStatus.Completed(entities.size))
            Log.d("SyncUtils", "Synced ${entities.size} playlists")
        } catch (e: Exception) {
            Log.e("SyncUtils", "Failed to sync playlists", e)
            _state.value = _state.value.copy(playlists = SyncStatus.Error(e.message ?: "Error desconocido"))
        }
    }

    private fun SongItem.toLikedSongEntity() = YtLikedSongEntity(
        videoId = id,
        title = title,
        artist = artists.joinToString(", ") { it.name },
        artistId = artists.firstOrNull()?.id,
        album = album?.name,
        albumId = album?.id,
        duration = duration,
        thumbnail = thumbnail,
        explicit = explicit,
    )

    companion object {
        @Volatile private var instance: SyncUtils? = null

        fun get(context: Context): SyncUtils =
            instance ?: synchronized(this) {
                instance ?: SyncUtils(context.applicationContext).also { instance = it }
            }
    }
}
