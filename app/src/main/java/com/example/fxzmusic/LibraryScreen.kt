package com.example.fxzmusic

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
val GRADIENT_PRESETS = listOf(
    listOf(Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70)),
    listOf(Color(0xFF0093E9), Color(0xFF80D0C7)),
    listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),
    listOf(Color(0xFFF7CE68), Color(0xFFFBAB7E)),
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
    listOf(Color(0xFF43C6AC), Color(0xFF191654)),
)

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    onPlayPlaylist: (Playlist) -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlaySongInPlaylist: (Playlist, Song) -> Unit,
    onShufflePlaylist: (Playlist) -> Unit,
    onResumePlaylist: (Playlist, String, Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onEditTags: (Song) -> Unit = {}
) {
    val selected = libraryViewModel.selectedPlaylist
    var selectedAlbum  by remember { mutableStateOf<AlbumGroup?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistGroup?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderGroup?>(null) }
    var showFavorites by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedAlbum != null || selectedArtist != null ||
            selectedFolder != null || selected != null || showFavorites) {
        when {
            selectedAlbum  != null -> selectedAlbum  = null
            selectedArtist != null -> selectedArtist = null
            selectedFolder != null -> selectedFolder = null
            selected       != null -> libraryViewModel.closePlaylist()
            showFavorites         -> showFavorites = false
        }
    }

    if (showFavorites) {
        val likedSongs = libraryViewModel.allSongs.filter { it.isLiked }
        FavoritesScreen(
            songs = likedSongs,
            onBack = { showFavorites = false },
            onPlaySong = onPlaySong,
            onPlayAll = {
                if (likedSongs.isNotEmpty()) {
                    onPlayPlaylist(
                        Playlist(
                            id = -10L,
                            name = "Favoritos",
                            songCount = likedSongs.size,
                            coverColor = listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
                            songs = likedSongs
                        )
                    )
                }
            }
        )
        return
    }

    if (selectedAlbum != null) {
        val album = selectedAlbum!!
        AlbumScreen(
            albumName    = album.name,
            songs        = album.songs,
            onBack       = { selectedAlbum = null },
            onPlaySong   = { song -> onPlaySong(song) },
            onPlayAll    = { songs ->
                if (songs.isNotEmpty()) {
                    val playlist = Playlist(
                        id         = album.name.hashCode().toLong(),
                        name       = album.name,
                        songCount  = songs.size,
                        coverColor = songs.first().albumArt,
                        coverUrl   = album.coverUrl,
                        songs      = songs
                    )
                    onPlayPlaylist(playlist)
                }
            },
            onShuffleAll = { shuffled ->
                if (shuffled.isNotEmpty()) {
                    val playlist = Playlist(
                        id         = album.name.hashCode().toLong(),
                        name       = album.name,
                        songCount  = shuffled.size,
                        coverColor = shuffled.first().albumArt,
                        coverUrl   = album.coverUrl,
                        songs      = shuffled
                    )
                    onPlayPlaylist(playlist)
                }
            }
        )
        return
    }

    if (selectedArtist != null) {
        val artist = selectedArtist!!
        ArtistScreen(
            artistName   = artist.name,
            songs        = artist.songs,
            onBack       = { selectedArtist = null },
            onPlaySong   = { song, _ -> onPlaySong(song) },
            onPlayAll    = { songs ->
                if (songs.isNotEmpty()) {
                    val playlist = Playlist(
                        id         = artist.name.hashCode().toLong(),
                        name       = artist.name,
                        songCount  = songs.size,
                        coverColor = songs.first().albumArt,
                        coverUrl   = artist.coverUrl,
                        songs      = songs
                    )
                    onPlayPlaylist(playlist)
                }
            },
            onShuffleAll = { shuffled ->
                if (shuffled.isNotEmpty()) {
                    val playlist = Playlist(
                        id         = artist.name.hashCode().toLong(),
                        name       = artist.name,
                        songCount  = shuffled.size,
                        coverColor = shuffled.first().albumArt,
                        coverUrl   = artist.coverUrl,
                        songs      = shuffled
                    )
                    onPlayPlaylist(playlist)
                }
            }
        )
        return
    }

    if (selectedFolder != null) {
        FolderDetailScreen(
            folder     = selectedFolder!!,
            onBack     = { selectedFolder = null },
            onPlaySong = onPlaySong,
            onPlayAll  = { folder ->
                if (folder.songs.isNotEmpty()) {
                    val playlist = Playlist(
                        id         = folder.name.hashCode().toLong(),
                        name       = folder.name,
                        songCount  = folder.songs.size,
                        coverColor = folder.songs.first().albumArt,
                        songs      = folder.songs
                    )
                    onPlayPlaylist(playlist)
                }
            }
        )
        return
    }

    if (selected != null) {
        PlaylistDetailScreen(
            playlist         = selected,
            allSongs         = libraryViewModel.allSongs,
            libraryViewModel = libraryViewModel,
            onBack           = { libraryViewModel.closePlaylist() },
            onPlaySong       = { song, queue ->
                val scopedPlaylist = selected.copy(songs = queue, songCount = queue.size)
                onPlaySongInPlaylist(scopedPlaylist, song)
            },
            onPlayAll        = { queue -> onPlayPlaylist(selected.copy(songs = queue, songCount = queue.size)) },
            onShuffleAll     = { queue -> onShufflePlaylist(selected.copy(songs = queue, songCount = queue.size)) },
            onResumePlayback = { queue, songId, progressMs ->
                val scopedPlaylist = selected.copy(songs = queue, songCount = queue.size)
                onResumePlaylist(scopedPlaylist, songId, progressMs)
            },
            onAddSongs       = { songs -> libraryViewModel.addSongsToPlaylist(selected.id, songs) },
            onRemoveSong     = { songId -> libraryViewModel.removeSongFromPlaylist(selected.id, songId) },
            onDeletePlaylist = { libraryViewModel.deletePlaylist(selected.id) },
            onEditTags       = onEditTags,
            onReorderSongs   = { reordered -> libraryViewModel.reorderPlaylistSongs(selected.id, reordered) },
            onUpdatePlaylist = { name, description, colors, coverUri ->
                libraryViewModel.updatePlaylistMeta(selected.id, name, description, colors, coverUri)
            }
        )
        return
    }

    val allSongs = libraryViewModel.allSongs

    val albums = remember(allSongs) {
        allSongs.groupBy { it.album }
            .map { (albumName, songs) ->
                AlbumGroup(
                    name     = albumName,
                    artist   = songs.map { it.artist }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "",
                    songs    = songs.sortedBy { it.title },
                    coverUrl = songs.firstOrNull { it.coverUrl != null }?.coverUrl
                )
            }.sortedBy { it.name }
    }

    val artists = remember(allSongs) {
        allSongs
            .groupBy { song ->
                song.artist
                    .split(",", "/", "&", " x ", " X ")
                    .first()
                    .replace(Regex("(?i)\\s*(feat\\.?|ft\\.?|featuring).*"), "")
                    .trim()
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            }
            .map { (artistName, songs) ->
                ArtistGroup(
                    name     = artistName,
                    songs    = songs.sortedBy { it.title },
                    coverUrl = songs.firstOrNull { it.coverUrl != null }?.coverUrl
                )
            }
            .sortedBy { it.name }
    }

    val folders = remember(allSongs) {
        allSongs.groupBy { song ->
            val parts = song.filePath.split("/")
            if (parts.size >= 2) parts[parts.size - 2] else "Raíz"
        }.map { (folderName, songs) ->
            FolderGroup(
                name  = folderName,
                path  = songs.first().filePath.substringBeforeLast("/"),
                songs = songs.sortedBy { it.title }
            )
        }.sortedBy { it.name }
    }

    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        modifier              = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
        contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedLibraryHeader(
                onSearchClick         = { libraryViewModel.updateSearchQuery(" ") },
                onCreatePlaylistClick = onCreatePlaylist
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                visible = libraryViewModel.isSearchActive,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    InteractiveSearchBar(
                        value         = libraryViewModel.searchQuery,
                        onValueChange = { libraryViewModel.updateSearchQuery(it) },
                        onClear       = { libraryViewModel.clearSearch() },
                        onClose       = { libraryViewModel.clearSearch() }
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
            FilterChipsRow(
                filters          = libraryViewModel.filters,
                selectedFilter   = libraryViewModel.selectedFilter,
                onFilterSelected = { libraryViewModel.updateSelectedFilter(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (libraryViewModel.selectedFilter) {
            "Todo" -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val likedSongs = allSongs.filter { it.isLiked }
                    PremiumLikedSongsBanner(songCount = likedSongs.size, onClick = { showFavorites = true })
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val displayPlaylists = libraryViewModel.userPlaylists.distinctBy { it.id }.filter { !it.isSmart }
                if (displayPlaylists.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Playlists", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    items(displayPlaylists.size.coerceAtMost(4)) { index ->
                        GridPlaylistItem(
                            playlist = displayPlaylists[index],
                            onClick  = { libraryViewModel.openPlaylist(displayPlaylists[index]) }
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Todas las canciones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                items(allSongs.size, span = { GridItemSpan(maxLineSpan) }) { index ->
                    LibrarySongListItem(song = allSongs[index], onClick = { onPlaySong(allSongs[index]) })
                }
            }
            "Álbumes" -> {
                items(albums.size) { index ->
                    AlbumGridItem(album = albums[index], onClick = { selectedAlbum = albums[index] })
                }
            }
            "Artistas" -> {
                items(artists.size) { index ->
                    ArtistGridItem(artist = artists[index], onClick = { selectedArtist = artists[index] })
                }
            }
            "Carpetas" -> {
                items(folders.size, span = { GridItemSpan(maxLineSpan) }) { index ->
                    FolderListItem(folder = folders[index], onClick = { selectedFolder = folders[index] })
                }
            }
            "Playlists" -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val likedSongs = allSongs.filter { it.isLiked }
                    PremiumLikedSongsBanner(songCount = likedSongs.size, onClick = { showFavorites = true })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val displayPlaylists = libraryViewModel.userPlaylists.distinctBy { it.id }.filter { !it.isSmart }
                items(displayPlaylists.size) { index ->
                    GridPlaylistItem(
                        playlist = displayPlaylists[index],
                        onClick  = { libraryViewModel.openPlaylist(displayPlaylists[index]) }
                    )
                }
            }
            else -> Unit
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    allSongs: List<Song>,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onResumePlayback: (List<Song>, String, Long) -> Unit,
    onAddSongs: (List<Song>) -> Unit,
    onRemoveSong: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onEditTags: (Song) -> Unit = {},
    onReorderSongs: (List<Song>) -> Unit = {},
    onUpdatePlaylist: (String, String, List<Color>, Uri?) -> Unit = { _, _, _, _ -> }
) {
    val theme  = LocalFxzTheme.current
    val accent = theme.accent

    var showMenu              by remember(playlist.id) { mutableStateOf(false) }
    var showEditDialog        by remember(playlist.id) { mutableStateOf(false) }
    var showMultiSelectDialog by remember(playlist.id) { mutableStateOf(false) }
    var pendingCoverUri       by remember(playlist.id) { mutableStateOf<Uri?>(null) }

    val selectedIds  = remember(playlist.id) { mutableStateListOf<String>() }
    val isSelectMode = selectedIds.isNotEmpty()
    var showBulkMenu by remember(playlist.id) { mutableStateOf(false) }

    val songs     = remember(playlist.id) { mutableStateListOf(*playlist.songs.toTypedArray()) }
    val listState = rememberLazyListState()

    val songQuery    by libraryViewModel.playlistSongQuery.collectAsState()
    val playlistSort by libraryViewModel.playlistSongSort.collectAsState()

    val visibleSongs by remember {
        derivedStateOf {
            val filtered = if (songQuery.isBlank()) songs.toList()
            else songs.filter {
                it.title.contains(songQuery, true) ||
                    it.artist.contains(songQuery, true) ||
                    it.album.contains(songQuery, true)
            }
            when (playlistSort) {
                LibraryViewModel.PlaylistSongSort.MANUAL     -> filtered
                LibraryViewModel.PlaylistSongSort.TITLE      -> filtered.sortedBy { it.title.lowercase() }
                LibraryViewModel.PlaylistSongSort.ARTIST     -> filtered.sortedBy { it.artist.lowercase() }
                LibraryViewModel.PlaylistSongSort.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
            }
        }
    }
    val canManualReorder = playlistSort == LibraryViewModel.PlaylistSongSort.MANUAL && songQuery.isBlank()

    val context           = LocalContext.current
    val view              = LocalView.current
    val playbackDataStore = remember(context) { PlaylistPlaybackDataStore(context.applicationContext) }
    var lastSongId        by remember(playlist.id) { mutableStateOf<String?>(null) }
    var lastProgressMs    by remember(playlist.id) { mutableLongStateOf(0L) }

    LaunchedEffect(playlist.id, playlist.songs) {
        if (songs != playlist.songs) { songs.clear(); songs.addAll(playlist.songs) }
        selectedIds.retainAll(playlist.songs.map { it.id }.toSet())
    }
    LaunchedEffect(playlist.id, songs.size) {
        val (songId, progressMs) = playbackDataStore.loadState(playlist.id)
        lastSongId     = songId?.takeIf { id -> songs.any { it.id == id } }
        lastProgressMs = progressMs
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            pendingCoverUri = uri
            onUpdatePlaylist(playlist.name, playlist.description, playlist.coverColor, uri)
        }
    }

    val collapseRangePx = with(androidx.compose.ui.platform.LocalDensity.current) { 430.dp.toPx() * 0.65f }

    val scrollProgress by remember {
        derivedStateOf {
            val offset = if (listState.firstVisibleItemIndex > 0) collapseRangePx
            else listState.firstVisibleItemScrollOffset.toFloat()
            (offset / collapseRangePx).coerceIn(0f, 1f)
        }
    }

    val gradientColors = playlist.coverColor.takeIf { it.size >= 2 }
        ?: listOf(Color(0xFF6A11CB), Color(0xFF2575FC))

    val coverScale   by animateFloatAsState(1f - scrollProgress * 0.35f, tween(0), label = "coverScale")
    val miniBarAlpha by animateFloatAsState(scrollProgress,               tween(0), label = "miniAlpha")
    val topBarBgAlpha by animateFloatAsState(scrollProgress,              tween(0), label = "topBarBg")

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {

        if (isSelectMode) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 80.dp)) {
                if (visibleSongs.isNotEmpty()) {
                    LazyColumn(
                        state               = listState,
                        contentPadding      = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(visibleSongs, key = { _, s -> s.id }) { _, song ->
                            PlaylistSongItemContent(
                                song, selectedIds, songs, accent, isSelectMode, canManualReorder,
                                onPlaySong, visibleSongs, onRemoveSong, onEditTags, onReorderSongs
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state          = listState,
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item(key = "hero") {
                    PlaylistHeroHeader(
                        playlist          = playlist,
                        songCount         = songs.size,
                        accent            = accent,
                        coverScale        = coverScale,
                        onPlayAll         = { onPlayAll(visibleSongs) },
                        onShuffleAll      = { onShuffleAll(visibleSongs) },
                        onResumePlayback  = {
                            val songId = lastSongId ?: return@PlaylistHeroHeader
                            onResumePlayback(visibleSongs, songId, lastProgressMs)
                        },
                        canResumePlayback = lastSongId != null && lastProgressMs > 0L,
                        onAddSongs        = { showMultiSelectDialog = true }
                    )
                }

                item(key = "search") {
                    OutlinedTextField(
                        value          = songQuery,
                        onValueChange  = { libraryViewModel.updatePlaylistSongQuery(it) },
                        placeholder    = { Text("Buscar en playlist...", color = Color(0xFF555555)) },
                        leadingIcon    = { Icon(Icons.Filled.Search, null, tint = Color(0xFF555555)) },
                        modifier       = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape          = RoundedCornerShape(14.dp),
                        colors         = OutlinedTextFieldDefaults.colors(
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            focusedContainerColor   = Color.White.copy(alpha = 0.07f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                            focusedBorderColor      = accent.copy(alpha = 0.50f),
                            unfocusedBorderColor    = Color.White.copy(alpha = 0.10f)
                        )
                    )
                }

                if (visibleSongs.isNotEmpty()) {
                    itemsIndexed(visibleSongs, key = { _, s -> s.id }) { _, song ->
                        PlaylistSongItemContent(
                            song, selectedIds, songs, accent, isSelectMode, canManualReorder,
                            onPlaySong, visibleSongs, onRemoveSong, onEditTags, onReorderSongs
                        )
                    }
                } else {
                    item(key = "empty") {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint     = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.size(88.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (songQuery.isBlank()) "Playlist vacía" else "Sin resultados",
                                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (songQuery.isBlank()) "Agrega canciones para empezar" else "Prueba con otra búsqueda",
                                    color = Color(0xFF666666), fontSize = 14.sp
                                )
                                Spacer(Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accent)
                                        .clickable {
                                            if (songQuery.isBlank()) showMultiSelectDialog = true
                                            else libraryViewModel.updatePlaylistSongQuery("")
                                        }
                                        .padding(horizontal = 22.dp, vertical = 13.dp)
                                ) {
                                    Text(
                                        if (songQuery.isBlank()) "Explorar música" else "Limpiar búsqueda",
                                        color = Color.Black, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .background(
                    when (theme.mode) {
                        ThemeMode.GLASSMORPHISM -> Brush.verticalGradient(
                            listOf(gradientColors.first().copy(alpha = topBarBgAlpha * 0.85f), Color.Transparent)
                        )
                        ThemeMode.SOFT_DARK -> Brush.verticalGradient(
                            listOf(theme.background.copy(alpha = topBarBgAlpha), Color.Transparent)
                        )
                        else -> Brush.verticalGradient(
                            listOf(gradientColors.first().copy(alpha = topBarBgAlpha * 0.95f), Color.Transparent)
                        )
                    }
                )
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(top = 40.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.30f))
                        .border(1.dp, Color.White.copy(alpha = if (scrollProgress < 0.5f) 0.15f else 0.0f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    playlist.name,
                    color      = Color.White.copy(alpha = miniBarAlpha),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer(alpha = miniBarAlpha)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(enabled = scrollProgress > 0.5f) { onPlayAll(visibleSongs) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.width(6.dp))

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.30f))
                            .border(1.dp, Color.White.copy(alpha = if (scrollProgress < 0.5f) 0.15f else 0.0f), CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded         = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier         = Modifier.background(Color(0xFF1A1A1A))
                    ) {
                        DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Edit, null, tint = accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text("Editar playlist", color = Color.White) } }, onClick = { showEditDialog = true; showMenu = false })
                        DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Image, null, tint = accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text("Cambiar portada", color = Color.White) } }, onClick = { coverPickerLauncher.launch(arrayOf("image/*")); showMenu = false })
                        DropdownMenuItem(text = { Text("Agregar canciones",  color = Color.White) },         onClick = { showMultiSelectDialog = true; showMenu = false })
                        DropdownMenuItem(text = { Text("Orden manual",       color = Color.White) },         onClick = { libraryViewModel.updatePlaylistSongSort(LibraryViewModel.PlaylistSongSort.MANUAL);     showMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por título", color = Color.White) },         onClick = { libraryViewModel.updatePlaylistSongSort(LibraryViewModel.PlaylistSongSort.TITLE);      showMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por artista",color = Color.White) },         onClick = { libraryViewModel.updatePlaylistSongSort(LibraryViewModel.PlaylistSongSort.ARTIST);     showMenu = false })
                        DropdownMenuItem(text = { Text("Ordenar por fecha",  color = Color.White) },         onClick = { libraryViewModel.updatePlaylistSongSort(LibraryViewModel.PlaylistSongSort.DATE_ADDED); showMenu = false })
                        DropdownMenuItem(text = { Text("Compartir playlist", color = Color.White) }, onClick = {
                            val bitmap = view.drawToBitmap(Bitmap.Config.ARGB_8888)
                            val file   = File(context.cacheDir, "playlist_${playlist.id}.png")
                            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 95, out) }
                            val uri2   = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri2)
                                putExtra(Intent.EXTRA_TEXT, "${playlist.name}\n${playlist.description}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir playlist"))
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Eliminar playlist", color = Color(0xFFFF5252)) }, onClick = { onDeletePlaylist(); showMenu = false })
                    }
                }
            }
        }

        if (isSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surfaceVariant)
                    .padding(top = 44.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedIds.clear() }) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
                Text(
                    "${selectedIds.size} seleccionadas",
                    color      = Color.White,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showBulkMenu = true }) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded         = showBulkMenu,
                        onDismissRequest = { showBulkMenu = false },
                        modifier         = Modifier.background(Color(0xFF1A1A1A))
                    ) {
                        DropdownMenuItem(
                            text    = { Text("Quitar seleccionadas", color = Color(0xFFFF5252)) },
                            onClick = {
                                val ids = selectedIds.toSet()
                                songs.removeAll { it.id in ids }
                                ids.forEach { onRemoveSong(it) }
                                selectedIds.clear()
                                showBulkMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text    = { Text("Seleccionar todo", color = accent) },
                            onClick = { selectedIds.clear(); selectedIds.addAll(songs.map { it.id }); showBulkMenu = false }
                        )
                    }
                }
            }
        }
    }

    if (showMultiSelectDialog) {
        MultiSelectDialog(
            allSongs        = allSongs,
            existingSongIds = songs.map { it.id }.toSet(),
            onDismiss       = { showMultiSelectDialog = false },
            onAdd           = {
                val existing  = songs.map { s -> s.id }.toSet()
                val additions = it.filter { s -> s.id !in existing }
                if (additions.isNotEmpty()) songs.addAll(additions)
                onAddSongs(it)
                showMultiSelectDialog = false
            }
        )
    }
    if (showEditDialog) {
        PlaylistEditDialog(
            currentName        = playlist.name,
            currentDescription = playlist.description,
            currentColors      = playlist.coverColor,
            onDismiss          = { showEditDialog = false },
            onConfirm          = { name, description, colors ->
                onUpdatePlaylist(name, description, colors, pendingCoverUri)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSongItemContent(
    song: Song,
    selectedIds: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    songs: androidx.compose.runtime.snapshots.SnapshotStateList<Song>,
    accent: Color,
    isSelectMode: Boolean,
    canManualReorder: Boolean,
    onPlaySong: (Song, List<Song>) -> Unit,
    visibleSongs: List<Song>,
    onRemoveSong: (String) -> Unit,
    onEditTags: (Song) -> Unit,
    onReorderSongs: (List<Song>) -> Unit
) {
    val isSelected        = song.id in selectedIds
    val currentIndexInAll = songs.indexOfFirst { it.id == song.id }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f },
        confirmValueChange  = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                songs.removeAll { s -> s.id == song.id }
                selectedIds.remove(song.id)
                onRemoveSong(song.id)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state                  = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent      = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0x00FF3B30), Color(0xAAFF3B30), Color(0xDDFF2020))
                        )
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.padding(end = 22.dp).size(22.dp)
                )
            }
        }
    ) {
        PlaylistSongItem(
            song           = song,
            isSelected     = isSelected,
            isDragging     = false,
            isSelectMode   = isSelectMode,
            accent         = accent,
            onPlay         = {
                if (isSelectMode) {
                    if (isSelected) selectedIds.remove(song.id) else selectedIds.add(song.id)
                } else {
                    onPlaySong(song, visibleSongs)
                }
            },
            onLongPress    = { if (!isSelectMode) selectedIds.add(song.id) },
            onToggleSelect = { if (isSelected) selectedIds.remove(song.id) else selectedIds.add(song.id) },
            onRemove       = {
                songs.removeAll { it.id == song.id }
                selectedIds.remove(song.id)
                onRemoveSong(song.id)
            },
            onEditTags     = { onEditTags(song) },
            onMoveUp       = {
                if (canManualReorder && currentIndexInAll > 0) {
                    val item = songs.removeAt(currentIndexInAll)
                    songs.add(currentIndexInAll - 1, item)
                    onReorderSongs(songs.toList())
                }
            },
            onMoveDown     = {
                if (canManualReorder && currentIndexInAll < songs.size - 1 && currentIndexInAll >= 0) {
                    val item = songs.removeAt(currentIndexInAll)
                    songs.add(currentIndexInAll + 1, item)
                    onReorderSongs(songs.toList())
                }
            },
            reorderEnabled = canManualReorder
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistSongItem(
    song: Song,
    isSelected: Boolean,
    isDragging: Boolean,
    isSelectMode: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelect: () -> Unit,
    onRemove: () -> Unit,
    onEditTags: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    reorderEnabled: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    val context  = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 14.dp else 0.dp, RoundedCornerShape(12.dp), clip = false)
            .scale(if (isDragging) 1.02f else 1f)
            .graphicsLayer(alpha = if (isDragging) 0.80f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDragging) Color(0xFF1A1A1A) else Color(0xFF121212))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) accent.copy(alpha = 0.45f) else Color.Transparent
            )
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onLongPress
            )
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.Black, uncheckedColor = Color.Gray),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(song.albumArt))
            ) {
                if (song.coverUrl != null) {
                    AsyncImage(
                        model = buildCoverRequest(LocalContext.current, song.coverUrl),
                        contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} • ${formatTime(song.duration)}", color = Color(0xFF9B9B9B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!isSelectMode) {
                Box {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.PlayArrow, null, tint = accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Reproducir ahora", color = Color.White) } },
                            onClick = { onPlay(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Edit, null, tint = accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Editar etiquetas", color = Color.White) } },
                            onClick = { onEditTags(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Share, null, tint = accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Compartir", color = Color.White) } },
                            onClick = {
                                val query = Uri.encode("${song.title} ${song.artist}")
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${song.title} · ${song.artist}\nhttps://www.youtube.com/results?search_query=$query")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir canción"))
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Quitar de playlist", color = Color(0xFFFF5252)) } },
                            onClick = { onRemove(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Add, null, tint = accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Copiar ruta del archivo", color = Color.White) } },
                            onClick = {
                                if (song.filePath.isNotBlank()) {
                                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("ruta_cancion", song.filePath))
                                }
                                showMenu = false
                            }
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = reorderEnabled,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Subir cancion",
                            tint = if (reorderEnabled) Color(0xFFBDBDBD) else Color(0xFF444444)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = reorderEnabled,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Bajar cancion",
                            tint = if (reorderEnabled) Color(0xFFBDBDBD) else Color(0xFF444444)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun PlaylistEditDialog(
    currentName: String,
    currentDescription: String,
    currentColors: List<Color>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<Color>) -> Unit
) {
    val accent = LocalFxzTheme.current.accent
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf(currentDescription) }
    var selectedGradientIndex by remember {
        mutableIntStateOf(
            GRADIENT_PRESETS.indexOfFirst { it.firstOrNull() == currentColors.firstOrNull() }
                .coerceAtLeast(0)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF2A2A2A)
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripcion", color = Color.Gray) },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color(0xFF2A2A2A)
                    )
                )
                Text("Color de portada", color = Color.Gray, fontSize = 12.sp)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(GRADIENT_PRESETS) { i, colors ->
                        val isSelected = selectedGradientIndex == i
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(colors))
                                .then(if (isSelected) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { selectedGradientIndex = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description, GRADIENT_PRESETS[selectedGradientIndex]) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = Color.DarkGray)
            ) { Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } },
        containerColor = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun MultiSelectDialog(
    allSongs: List<Song>,
    existingSongIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (List<Song>) -> Unit
) {
    val selected = remember { mutableStateListOf<String>() }
    var searchText by remember { mutableStateOf("") }
    val accent = LocalFxzTheme.current.accent
    val filtered = allSongs.filter {
        it.id !in existingSongIds &&
            (searchText.isBlank() || it.title.contains(searchText, true) || it.artist.contains(searchText, true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar canciones", color = Color.White, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.10f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                        focusedBorderColor = accent.copy(alpha = 0.55f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { song ->
                        val isChecked = song.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    if (isChecked) selected.remove(song.id) else selected.add(song.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(song.albumArt))
                            ) {
                                if (song.coverUrl != null) {
                                    AsyncImage(
                                        model = buildCoverRequest(LocalContext.current, song.coverUrl),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selected.add(song.id) else selected.remove(song.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.Black)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(allSongs.filter { it.id in selected }) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Agregar (${selected.size})", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    )
}
@Composable
fun AddSongsDialog(
    allSongs: List<Song>,
    existingSongIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (List<Song>) -> Unit
) {
    val selected    = remember { mutableStateListOf<String>() }
    var searchText  by remember { mutableStateOf("") }
    val accent      = LocalFxzTheme.current.accent
    val filtered    = allSongs.filter {
        it.id !in existingSongIds && (searchText.isEmpty() || it.title.contains(searchText, true) || it.artist.contains(searchText, true))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text("Agregar canciones", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (selected.isNotEmpty()) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(accent).clickable { onAdd(allSongs.filter { it.id in selected }) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text("Agregar (${selected.size})", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            OutlinedTextField(
                value = searchText, onValueChange = { searchText = it },
                placeholder = { Text("Buscar...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { song ->
                    val isSelected = song.id in selected
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { if (isSelected) selected.remove(song.id) else selected.add(song.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF0D2818) else Color(0xFF1E1E1E))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(song.albumArt))) {
                                if (song.coverUrl != null) {
                                    AsyncImage(model = buildCoverRequest(LocalContext.current, song.coverUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { if (it) selected.add(song.id) else selected.remove(song.id) },
                                colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.Black, uncheckedColor = Color.Gray)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AnimatedLibraryHeader(onSearchClick: () -> Unit, onCreatePlaylistClick: () -> Unit) {
    val accent = LocalFxzTheme.current.accent
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Colección Local", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BouncyIconButton(icon = Icons.Filled.Add, tint = accent, onClick = onCreatePlaylistClick)
            BouncyIconButton(icon = Icons.Filled.Search, tint = Color.White, onClick = onSearchClick)
        }
    }
}

@Composable
fun PremiumLikedSongsBanner(songCount: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "banner_scale")
    val infiniteTransition = rememberInfiniteTransition(label = "banner_anim")
    val gradientShift by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse), label = "gradient")
    Card(
        modifier = Modifier.fillMaxWidth().height(110.dp).scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(if (isPressed) 2.dp else 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(brush = Brush.linearGradient(colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC), Color(0xFF6A11CB)), start = Offset(gradientShift, 0f), end = Offset(gradientShift + 500f, 1000f))) }.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tus Favoritos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("$songCount pistas guardadas", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    songs: List<Song>,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayAll: () -> Unit
) {
    val accent = LocalFxzTheme.current.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFxzTheme.current.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tus favoritos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("${songs.size} canciones", color = Color.Gray, fontSize = 12.sp)
            }
            if (songs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent)
                        .clickable { onPlayAll() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Reproducir", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no tienes canciones favoritas", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    LibrarySongListItem(song = song, onClick = { onPlaySong(song) })
                }
            }
        }
    }
}

@Composable
fun LibrarySongListItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(song.albumArt))
            ) {
                if (song.coverUrl != null) {
                    AsyncImage(
                        model = buildCoverRequest(LocalContext.current, song.coverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} • ${formatTime(song.duration)}", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.PlayArrow, null, tint = LocalFxzTheme.current.accent)
        }
    }
}

@Composable
fun GridPlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "grid_scale")
    Column(modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(if (isPressed) 2.dp else 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = playlist.coverColor)).blur(if (playlist.coverUrl != null) 0.dp else 20.dp)) {
                if (playlist.coverUrl != null) {
                    AsyncImage(model = buildCoverRequest(LocalContext.current, playlist.coverUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.LibraryMusic, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp).align(Alignment.Center))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(playlist.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
        Text("${playlist.songCount} canciones", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

data class AlbumGroup(
    val name: String,
    val artist: String,
    val songs: List<Song>,
    val coverUrl: String? = null
)

@Composable
fun AlbumGridItem(album: AlbumGroup, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "album_scale"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().aspectRatio(1f),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(if (isPressed) 2.dp else 8.dp),
            colors    = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (album.coverUrl != null) {
                    AsyncImage(
                        model            = buildCoverRequest(LocalContext.current, album.coverUrl),
                        contentDescription = null,
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(album.songs.first().albumArt)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            null,
                            tint     = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        "${album.songs.size}",
                        color      = Color.White.copy(alpha = 0.8f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            album.name,
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            album.artist,
            color    = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
    val coverUrl: String? = null
)

data class FolderGroup(
    val name: String,
    val path: String,
    val songs: List<Song>
)

@Composable
fun ArtistGridItem(artist: ArtistGroup, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "artist_scale"
    )
    Column(
        modifier              = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Box(
            modifier            = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape),
            contentAlignment    = Alignment.Center
        ) {
            if (artist.coverUrl != null) {
                AsyncImage(
                    model              = buildCoverRequest(LocalContext.current, artist.coverUrl),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize().background(Brush.linearGradient(artist.songs.first().albumArt)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        artist.name.take(1).uppercase(),
                        color      = Color.White,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            artist.name,
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            "${artist.songs.size} canciones",
            color    = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun FolderListItem(folder: FolderGroup, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Card(
        modifier = Modifier.fillMaxWidth().scale(if (isPressed) 0.97f else 1f).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(LocalFxzTheme.current.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LibraryMusic, null, tint = LocalFxzTheme.current.accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(folder.path, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${folder.songs.size}", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FolderDetailScreen(
    folder: FolderGroup,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayAll: (FolderGroup) -> Unit
) {
    val accent = LocalFxzTheme.current.accent
    Column(modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${folder.songs.size} canciones", color = Color.Gray, fontSize = 13.sp)
            }
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(accent).clickable { onPlayAll(folder) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(folder.songs) { index, song ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                Card(modifier = Modifier.fillMaxWidth().scale(if (isPressed) 0.97f else 1f).clickable(interactionSource = interactionSource, indication = null) { onPlaySong(song) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(song.albumArt))) {
                            if (song.coverUrl != null) { AsyncImage(model = buildCoverRequest(LocalContext.current, song.coverUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatTime(song.duration), color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistHeroHeader(
    playlist: Playlist,
    songCount: Int,
    accent: Color,
    coverScale: Float = 1f,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onResumePlayback: () -> Unit,
    canResumePlayback: Boolean,
    onAddSongs: () -> Unit
) {
    val gradientColors = playlist.coverColor.takeIf { it.size >= 2 }
        ?: listOf(Color(0xFF6A11CB), Color(0xFF2575FC))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.92f),
                            gradientColors.last().copy(alpha = 0.60f),
                            Color(0xFF090909)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)),
                        radius = 1100f
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
                .size(150.dp)
                .scale(coverScale)
                .shadow(
                    elevation    = 40.dp,
                    shape        = RoundedCornerShape(26.dp),
                    ambientColor = gradientColors.last().copy(alpha = 0.7f),
                    spotColor    = gradientColors.first().copy(alpha = 0.7f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(gradientColors))
        ) {
            if (playlist.coverUrl != null) {
                AsyncImage(
                    model              = buildCoverRequest(LocalContext.current, playlist.coverUrl),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    null,
                    tint     = Color.White.copy(alpha = 0.30f),
                    modifier = Modifier.size(60.dp).align(Alignment.Center)
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        Offset(0f, 0f), Offset(180f, 180f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    playlist.name,
                    color         = Color.White,
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    maxLines      = 1,
                    overflow      = TextOverflow.Ellipsis
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        playlist.description,
                        color    = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
                    Text("$songCount canciones", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                    Text("·", color = Color.White.copy(alpha = 0.28f), fontSize = 12.sp)
                    Text("Orden personalizada", color = accent.copy(alpha = 0.95f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent)
                        .clickable { onPlayAll() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Text("Reproducir", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .clickable { onShuffleAll() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Shuffle, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .clickable { onAddSongs() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                if (canResumePlayback) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable { onResumePlayback() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = accent, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}
