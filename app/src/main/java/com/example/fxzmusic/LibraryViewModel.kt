package com.example.fxzmusic

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class LibraryViewModel : ViewModel() {

    enum class PlaylistSongSort {
        MANUAL, TITLE, ARTIST, DATE_ADDED
    }

    var searchQuery by mutableStateOf("")
        private set
    var selectedFilter by mutableStateOf("Todo")
        private set
    val filters = listOf("Todo", "Playlists", "Álbumes", "Artistas", "Carpetas")
    var isSearchActive by mutableStateOf(false)
    var showCreatePlaylistDialog by mutableStateOf(false)
    var hasPermission by mutableStateOf(false)
    var userPlaylists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var allSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var selectedPlaylist by mutableStateOf<Playlist?>(null)
        private set
    var showAddSongsDialog by mutableStateOf(false)

    var isScanning by mutableStateOf(false)
        private set
    var scanProgress by mutableIntStateOf(0)
        private set
    var scanTotal by mutableIntStateOf(0)
        private set

    private val _playlistSongQuery = MutableStateFlow("")
    val playlistSongQuery: StateFlow<String> = _playlistSongQuery

    private val _playlistSongSort = MutableStateFlow(PlaylistSongSort.MANUAL)
    val playlistSongSort: StateFlow<PlaylistSongSort> = _playlistSongSort

    private var coverCachePrefs: SharedPreferences? = null
    private var database: FxzDatabase? = null
    private var playlistSongRefsById: Map<Long, List<String>> = emptyMap()
    private var contentObserver: ContentObserver? = null
    private var appContext: Context? = null
    private var scanJob: Job? = null

    private val itunesSemaphore = Semaphore(4)
    private var smartPlaylistDebounceJob: Job? = null

    var onLikeChanged: ((songId: String, isLiked: Boolean) -> Unit)? = null
    var onPlaylistDeleted: ((playlistId: Long) -> Unit)? = null

    fun initPrefs(context: Context) {
        appContext      = context.applicationContext
        coverCachePrefs = context.applicationContext.getSharedPreferences("cover_cache", Context.MODE_PRIVATE)
        database        = FxzDatabase.getInstance(context.applicationContext)
        viewModelScope.launch {
            LegacyMigrationManager.migrateIfNeeded(context.applicationContext)
            loadPlaylists()
        }
        registerContentObserver(context.applicationContext)
    }

    private fun registerContentObserver(context: Context) {
        val handler = Handler(Looper.getMainLooper())
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                val ctx = appContext ?: return
                if (hasPermission) viewModelScope.launch { scanLocalMusic(ctx) }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!
        )
    }

    fun unregisterContentObserver(context: Context) {
        contentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        contentObserver = null
    }

    fun updateSearchQuery(query: String) { searchQuery = query; isSearchActive = query.isNotEmpty() }
    fun updateSelectedFilter(filter: String) { selectedFilter = filter }
    fun clearSearch() { searchQuery = ""; isSearchActive = false }
    fun showCreatePlaylist() { showCreatePlaylistDialog = true }
    fun hideCreatePlaylist() { showCreatePlaylistDialog = false }

    private fun resetPlaylistDetailState() {
        _playlistSongQuery.value = ""
        _playlistSongSort.value = PlaylistSongSort.MANUAL
    }

    fun openPlaylist(playlist: Playlist) {
        resetPlaylistDetailState()
        selectedPlaylist = playlist
    }

    fun closePlaylist() {
        selectedPlaylist = null
        resetPlaylistDetailState()
    }

    fun updatePlaylistSongQuery(query: String) {
        _playlistSongQuery.value = query
    }

    fun updatePlaylistSongSort(sort: PlaylistSongSort) {
        _playlistSongSort.value = sort
    }

    fun createPlaylist(name: String, songs: List<Song> = emptyList()) {
        val newPlaylist = Playlist(
            id         = System.currentTimeMillis(),
            name       = name,
            songCount  = songs.size,
            coverColor = generateRandomGradient(),
            songs      = songs
        )
        userPlaylists = userPlaylists + newPlaylist
        playlistSongRefsById = playlistSongRefsById + (newPlaylist.id to songs.map { it.id })
        showCreatePlaylistDialog = false
        selectedFilter = "Playlists"
        isSearchActive = false
        searchQuery = ""
        savePlaylists()
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        userPlaylists = userPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                val existing = playlist.songs.map { it.id }.toSet()
                val newSongs = songs.filter { it.id !in existing }
                val merged   = playlist.songs + newSongs
                playlistSongRefsById = playlistSongRefsById + (playlistId to merged.map { it.id })
                playlist.copy(songs = merged, songCount = merged.size)
            } else playlist
        }
        selectedPlaylist = userPlaylists.find { it.id == playlistId }
        savePlaylists()
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        userPlaylists = userPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                val newSongs = playlist.songs.filter { it.id != songId }
                playlistSongRefsById = playlistSongRefsById + (playlistId to newSongs.map { it.id })
                playlist.copy(songs = newSongs, songCount = newSongs.size)
            } else playlist
        }
        selectedPlaylist = userPlaylists.find { it.id == playlistId }
        savePlaylists()
    }

    fun reorderPlaylistSongs(playlistId: Long, reordered: List<Song>) {
        userPlaylists = userPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                playlistSongRefsById = playlistSongRefsById + (playlistId to reordered.map { it.id })
                playlist.copy(songs = reordered, songCount = reordered.size)
            }
            else playlist
        }
        selectedPlaylist = userPlaylists.find { it.id == playlistId }
        savePlaylists()
    }

    fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        userPlaylists = userPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                val updatedSongs = playlist.songs.toMutableList()
                if (fromIndex >= 0 && fromIndex < updatedSongs.size && toIndex >= 0 && toIndex < updatedSongs.size) {
                    val song = updatedSongs.removeAt(fromIndex)
                    updatedSongs.add(toIndex, song)
                }
                playlist.copy(songs = updatedSongs, songCount = updatedSongs.size)
            } else {
                playlist
            }
        }
        selectedPlaylist = userPlaylists.find { it.id == playlistId }
        playlistSongRefsById = playlistSongRefsById + (playlistId to (selectedPlaylist?.songs?.map { it.id } ?: emptyList()))
        
        val playlist = userPlaylists.find { it.id == playlistId } ?: return
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val entities = playlist.songs.mapIndexed { index, song ->
                PlaylistSongEntity(playlistId = playlist.id, songId = song.id, position = index)
            }
            dao.replacePlaylistSongs(playlist.id, entities)
        }
    }

    fun updatePlaylistMeta(playlistId: Long, name: String, description: String = "", colors: List<Color>, coverUri: Uri?) {
        userPlaylists = userPlaylists.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(
                name       = name,
                description = description,
                coverColor = colors,
                coverUrl   = coverUri?.toString() ?: playlist.coverUrl
            ) else playlist
        }
        selectedPlaylist = userPlaylists.find { it.id == playlistId }
        
        if (coverUri != null) {
            appContext?.let { context ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        coverUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
            }
        }
        savePlaylists()
    }

    fun deletePlaylist(playlistId: Long) {
        userPlaylists = userPlaylists.filter { it.id != playlistId }
        playlistSongRefsById = playlistSongRefsById - playlistId
        if (selectedPlaylist?.id == playlistId) selectedPlaylist = null
        onPlaylistDeleted?.invoke(playlistId)
        savePlaylists()
    }

    fun updateTaggedSong(updated: Song) {
        allSongs = allSongs.map { if (it.id == updated.id) updated else it }
    }

    fun toggleLike(songId: String) {
        allSongs = allSongs.map { song ->
            if (song.id == songId) song.copy(isLiked = !song.isLiked) else song
        }
        val newIsLiked = allSongs.find { it.id == songId }?.isLiked ?: false
        val song = allSongs.find { it.id == songId }
        if (song != null) {
            viewModelScope.launch(Dispatchers.IO) {
                database?.songMetaDao()?.upsert(
                    SongMetaEntity(
                        songId = song.id,
                        playCount = song.playCount,
                        lastPlayed = song.lastPlayed,
                        isLiked = newIsLiked
                    )
                )
            }
        }
        onLikeChanged?.invoke(songId, newIsLiked)
        userPlaylists = userPlaylists.map { playlist ->
            val updated = playlist.songs.map { s ->
                if (s.id == songId) s.copy(isLiked = newIsLiked) else s
            }
            playlist.copy(songs = updated)
        }
    }

    private fun loadLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = database?.songMetaDao()?.getAll().orEmpty()
            if (meta.isEmpty()) return@launch
            val metaBySong = meta.associateBy { it.songId }
            withContext(Dispatchers.Main) {
                allSongs = allSongs.map { song ->
                    val m = metaBySong[song.id] ?: return@map song
                    song.copy(
                        isLiked = m.isLiked,
                        playCount = maxOf(song.playCount, m.playCount),
                        lastPlayed = maxOf(song.lastPlayed, m.lastPlayed)
                    )
                }
                generateSmartPlaylists()
            }
        }
    }

    fun updateSongStats(songId: String, newPlayCount: Int, newLastPlayed: Long) {
        allSongs = allSongs.map { song ->
            if (song.id == songId) song.copy(playCount = newPlayCount, lastPlayed = newLastPlayed)
            else song
        }
        val song = allSongs.find { it.id == songId }
        if (song != null) {
            viewModelScope.launch(Dispatchers.IO) {
                database?.songMetaDao()?.upsert(
                    SongMetaEntity(
                        songId = song.id,
                        playCount = newPlayCount,
                        lastPlayed = newLastPlayed,
                        isLiked = song.isLiked
                    )
                )
            }
        }
        generateSmartPlaylists()
    }

    private fun savePlaylists() {
        val dao = database?.playlistDao() ?: return
        val nonSmart = userPlaylists.filter { !it.isSmart }
        val refsSnapshot = nonSmart.associate { playlist ->
            val ids = playlist.songs.map { it.id }.ifEmpty { playlistSongRefsById[playlist.id].orEmpty() }
            playlist.id to ids
        }
        playlistSongRefsById = refsSnapshot

        viewModelScope.launch(Dispatchers.IO) {
            val entities = nonSmart.map {
                PlaylistEntity(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    songCount = refsSnapshot[it.id]?.size ?: it.songCount,
                    coverColors = it.coverColor.encodeColorList(),
                    coverUrl = it.coverUrl,
                    isSmart = false,
                    smartType = null
                )
            }
            dao.upsertPlaylists(entities)
            if (nonSmart.isEmpty()) dao.deleteAllNonSmart() else dao.deleteNonSmartNotIn(nonSmart.map { it.id })

            nonSmart.forEach { playlist ->
                val refs = refsSnapshot[playlist.id].orEmpty()
                dao.replacePlaylistSongs(
                    playlist.id,
                    refs.mapIndexed { index, songId ->
                        PlaylistSongEntity(playlistId = playlist.id, songId = songId, position = index)
                    }
                )
            }
        }
    }

    private suspend fun loadPlaylists() {
        val dao = database?.playlistDao() ?: return
        val entities = withContext(Dispatchers.IO) { dao.getAll().filter { !it.isSmart } }
        val refsByPlaylist = withContext(Dispatchers.IO) {
            entities.associate { entity ->
                val refs = dao.getSongsForPlaylist(entity.id).map { it.songId }
                entity.id to refs
            }
        }
        playlistSongRefsById = refsByPlaylist
        val songIndex = allSongs.associateBy { it.id }
        val loaded = entities.map { entity ->
            val refs = refsByPlaylist[entity.id].orEmpty()
            val songs = refs.mapNotNull { songId -> songIndex[songId] }
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                songCount = refs.size,
                coverColor = decodeColorList(entity.coverColors).ifEmpty { generateRandomGradient() },
                coverUrl = entity.coverUrl,
                songs = songs,
                isSmart = false,
                smartType = null
            )
        }
        userPlaylists = loaded
        selectedPlaylist?.let { current ->
            selectedPlaylist = loaded.find { it.id == current.id }
        }
    }

    private fun refreshPlaylistSongs() {
        val songIndex = allSongs.associateBy { it.id }
        val updated = userPlaylists.map { playlist ->
            if (playlist.isSmart) return@map playlist
            val refs = playlistSongRefsById[playlist.id].orEmpty()
            val refreshed = if (refs.isNotEmpty()) {
                refs.mapNotNull { songId -> songIndex[songId] }
            } else {
                playlist.songs.mapNotNull { song -> songIndex[song.id] ?: song }
            }
            playlist.copy(songs = refreshed, songCount = maxOf(playlist.songCount, refs.size))
        }
        if (updated != userPlaylists) {
            userPlaylists = updated
            selectedPlaylist?.let { current ->
                selectedPlaylist = updated.find { it.id == current.id }
            }
        }
    }

    private fun generateRandomGradient(): List<Color> {
        val colors = listOf(
            Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70),
            Color(0xFF0093E9), Color(0xFF80D0C7), Color(0xFF8EC5FC),
            Color(0xFFFBAB7E), Color(0xFFF7CE68), Color(0xFFFA8BFF)
        )
        return colors.shuffled().take(3)
    }

    private fun getCachedCover(songId: String): String? =
        coverCachePrefs?.getString("cover_$songId", null)

    private fun cacheCover(songId: String, url: String) {
        coverCachePrefs?.edit()?.putString("cover_$songId", url)?.apply()
    }

    fun scanLocalMusic(context: Context) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            isScanning   = true
            scanProgress = 0

            val songDataMap = mutableMapOf<String, SongRawData>()

            val hasAudio =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)

            if (hasAudio) {
                withContext(Dispatchers.IO) {
                    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                    val projection = arrayOf(
                        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.ALBUM_ID
                    )

                    context.contentResolver.query(
                        collection, projection,
                        "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                        "${MediaStore.Audio.Media.TITLE} ASC"
                    )?.use { cursor ->
                        val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                        val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dataCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                        val dateCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                        while (cursor.moveToNext()) {
                            val durationMs = cursor.getInt(durCol)
                            if (durationMs < 30000) continue
                            val id      = cursor.getString(idCol)
                            val albumId = cursor.getLong(albumIdCol)
                            val embeddedUri = Uri.withAppendedPath(
                                Uri.parse("content://media/external/audio/albumart"), albumId.toString()
                            ).toString()
                            songDataMap[id] = SongRawData(
                                id               = id,
                                title            = cursor.getString(titleCol) ?: "Unknown",
                                artist           = cursor.getString(artistCol) ?: "Unknown Artist",
                                album            = cursor.getString(albumCol)  ?: "Unknown Album",
                                path             = cursor.getString(dataCol),
                                durationMs       = durationMs,
                                dateAdded        = cursor.getLong(dateCol),
                                embeddedCoverUri = embeddedUri
                            )
                        }
                    }
                }
            }

            scanTotal = songDataMap.size

            val cachedIds       = mutableSetOf<String>()
            val needsNetworkIds = mutableSetOf<String>()

            songDataMap.values.forEach { data ->
                val cached = getCachedCover(data.id)
                if (cached != null) {
                    songDataMap[data.id] = data.copy(coverUrl = cached)
                    cachedIds.add(data.id)
                } else {
                    needsNetworkIds.add(data.id)
                }
            }

            val cachedSongs = songDataMap
                .filterKeys { it in cachedIds }
                .values.map { buildSong(it) }
                .sortedBy { it.title }

            if (cachedSongs.isNotEmpty()) {
                allSongs = cachedSongs
                loadLikedSongs()
                generateSmartPlaylists()
            }

            scanProgress = cachedIds.size

            val wifiOnlyCovers = context.applicationContext
                .getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
                .getBoolean("wifi_only_covers", true)
            val allowNetworkCoverFetch = !wifiOnlyCovers || context.isWifiConnected()

            if (!allowNetworkCoverFetch) {
                allSongs = songDataMap.values.map { buildSong(it) }.sortedBy { it.title }
                loadLikedSongs()
                loadPlaylists()
                refreshPlaylistSongs()
                generateSmartPlaylists()
                isScanning = false
                return@launch
            }

            val needsSearch = songDataMap.filterKeys { it in needsNetworkIds }.values.filter { data ->
                data.title != "Unknown" &&
                !data.title.contains("AUD-") &&
                !data.title.contains("VID-") &&
                data.title.length > 2
            }
            val albumGroups = needsSearch.groupBy { it.album.ifBlank { "__single__${it.id}" } }

            val networkJobs = albumGroups.entries.map { (_, songsInAlbum) ->
                async(Dispatchers.IO) {
                    itunesSemaphore.withPermit {
                        try {
                            val rep  = songsInAlbum.first()
                            val best = NetworkModule.searchBestTrack(rep.title, rep.artist, rep.album)
                            if (best != null) {
                                val coverUrl = best.artworkUrl100?.replace("100x100bb", "600x600bb")
                                songsInAlbum.forEach { data ->
                                    val newArtist = if (data.artist.contains("unknown", true) || data.artist.isBlank())
                                        best.artistName ?: data.artist else data.artist
                                    val newAlbum = if (data.album.contains("unknown", true) || data.album.isBlank())
                                        best.collectionName ?: data.album else data.album
                                    songDataMap[data.id] = data.copy(coverUrl = coverUrl, artist = newArtist, album = newAlbum)
                                    if (coverUrl != null) cacheCover(data.id, coverUrl)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        scanProgress += songsInAlbum.size
                    }
                }
            }

            val searchedIds = needsSearch.map { it.id }.toSet()
            val skippedJobs = songDataMap.filterKeys { it in needsNetworkIds && it !in searchedIds }.values.map { data ->
                async(Dispatchers.IO) { withContext(kotlinx.coroutines.Dispatchers.Main) { scanProgress++ } }
            }
            (networkJobs + skippedJobs).awaitAll()

            allSongs = songDataMap.values.map { buildSong(it) }.sortedBy { it.title }
            loadLikedSongs()
            loadPlaylists()
            refreshPlaylistSongs()
            generateSmartPlaylists()
            isScanning = false
        }
    }

    private fun generateSmartPlaylists() {
        smartPlaylistDebounceJob?.cancel()
        smartPlaylistDebounceJob = viewModelScope.launch {
            delay(500)
            val smart = listOf(
                createMostPlayedPlaylist(),
                createForgottenGemsPlaylist(),
                createNeverPlayedPlaylist(),
                createRecentAddedPlaylist()
            )
            userPlaylists = userPlaylists.filter { !it.isSmart } + smart
        }
    }

    private fun createMostPlayedPlaylist(): Playlist {
        val songs = allSongs.sortedByDescending { it.playCount }.take(25)
        return Playlist(-1L, "Top Locales", songs.size, listOf(Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70)), songs = songs, isSmart = true, smartType = SmartPlaylistType.MOST_PLAYED)
    }

    private fun createForgottenGemsPlaylist(): Playlist {
        val monthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val songs = allSongs.filter { it.lastPlayed < monthAgo && it.playCount > 0 }.shuffled().take(18)
        return Playlist(-2L, "Joyas Olvidadas", songs.size, listOf(Color(0xFF0093E9), Color(0xFF80D0C7)), songs = songs, isSmart = true, smartType = SmartPlaylistType.FORGOTTEN_GEMS)
    }

    private fun createNeverPlayedPlaylist(): Playlist {
        val songs = allSongs.filter { it.playCount == 0 }.take(20)
        return Playlist(-3L, "Sin Escuchar", songs.size, listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)), songs = songs, isSmart = true, smartType = SmartPlaylistType.NEVER_PLAYED)
    }

    private fun createRecentAddedPlaylist(): Playlist {
        val weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val songs = allSongs.filter { it.dateAdded > weekAgo }.take(20)
        return Playlist(-4L, "Agregados Recientemente", songs.size, listOf(Color(0xFF11998E), Color(0xFF38EF7D)), songs = songs, isSmart = true, smartType = SmartPlaylistType.RECENT_ADDED)
    }

    private fun buildSong(d: SongRawData): Song {
        val coverToUse = d.coverUrl ?: d.embeddedCoverUri
        return Song(
            id        = d.id,
            title     = d.title,
            artist    = d.artist,
            album     = d.album,
            filePath  = d.path,
            duration  = d.durationMs / 1000,
            dateAdded = d.dateAdded,
            coverUrl  = coverToUse,
            albumArt  = generateAlbumArt(d.id)
        )
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        smartPlaylistDebounceJob?.cancel()
        appContext?.let { unregisterContentObserver(it) }
    }

    private fun generateAlbumArt(id: String): List<Color> {
        val hue1 = (id.hashCode().and(0xFF).toFloat() / 255f) * 360f
        val hue2 = (hue1 + 40f) % 360f
        val hue3 = (hue1 + 80f) % 360f
        fun hslToColor(h: Float): Color {
            val s = 0.6f
            val l = 0.45f
            val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
            val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
            val m = l - c / 2f
            val (r, g, b) = when {
                h < 60f  -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else     -> Triple(c, 0f, x)
            }
            return Color(r + m, g + m, b + m)
        }
        return listOf(hslToColor(hue1), hslToColor(hue2), hslToColor(hue3))
    }
}

