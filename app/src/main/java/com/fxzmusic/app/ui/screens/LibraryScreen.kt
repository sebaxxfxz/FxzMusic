package com.fxzmusic.app.ui.screens

import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.viewmodel.*
import com.fxzmusic.app.ui.components.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.util.*

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    onPlayPlaylist: (Playlist) -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlaySongInPlaylist: (Playlist, Song) -> Unit,
    onShufflePlaylist: (Playlist) -> Unit,
    onResumePlaylist: (Playlist, String, Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onEditTags: (Song) -> Unit = {},
    onNavigateToAlbum: (albumName: String) -> Unit = {},
    onNavigateToArtist: (artistName: String) -> Unit = {},
    onNavigateToFolder: (folderPath: String) -> Unit = {},
    onNavigateToPlaylist: (playlistId: Long) -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {}
) {
    val allSongs = libraryViewModel.allSongs
    val allAlbums = libraryViewModel.allAlbums
    val allArtists = libraryViewModel.allArtists
    val allFolders = libraryViewModel.allFolders
    val selectedFilter = libraryViewModel.selectedFilter
    val userPlaylists = libraryViewModel.userPlaylists

    val searchQuery = libraryViewModel.searchQuery
    var isSearching by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }

    if (isSearching || searchQuery.isNotEmpty()) {
        BackHandler {
            libraryViewModel.clearSearch()
            isSearching = false
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            libraryViewModel.refreshLibrary()
            delay(1000)
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Biblioteca",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${allSongs.size} canciones · ${allAlbums.size} álbumes · ${allArtists.size} artistas",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isSearching = !isSearching },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Buscar",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { onCreatePlaylist() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Nueva playlist",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (libraryViewModel.isScanning) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Escaneando música local (${libraryViewModel.scanProgress}/${libraryViewModel.scanTotal})...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSearching || searchQuery.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            InteractiveSearchBar(
                                value = searchQuery,
                                onValueChange = { libraryViewModel.updateSearchQuery(it) },
                                onClear = { libraryViewModel.clearSearch() },
                                onClose = {
                                    libraryViewModel.clearSearch()
                                    isSearching = false
                                }
                            )
                        }
                    }
                }
            }

            if (searchQuery.isEmpty()) {
                item {
                    FilterChipsRow(
                        filters = libraryViewModel.filters,
                        selectedFilter = selectedFilter,
                        onFilterSelected = { libraryViewModel.updateSelectedFilter(it) }
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                val matchingSongs = allSongs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true) ||
                            it.album.contains(searchQuery, ignoreCase = true)
                }
                val matchingPlaylists = userPlaylists.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }
                val matchingAlbums = allAlbums.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }
                val matchingArtists = allArtists.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                if (matchingSongs.isEmpty() && matchingPlaylists.isEmpty() && matchingAlbums.isEmpty() && matchingArtists.isEmpty()) {
                    item {
                        EmptySearchResultsState(query = searchQuery)
                    }
                } else {
                    if (matchingPlaylists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Playlists coincidentes", count = matchingPlaylists.size)
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(matchingPlaylists, key = { it.id }) { playlist ->
                                    PlaylistCard(playlist = playlist, onClick = { onNavigateToPlaylist(playlist.id) })
                                }
                            }
                        }
                    }

                    if (matchingAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Álbumes coincidentes", count = matchingAlbums.size)
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(matchingAlbums, key = { it.name }) { album ->
                                    AlbumCard(album = album, onClick = { onNavigateToAlbum(album.name) })
                                }
                            }
                        }
                    }

                    if (matchingArtists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Artistas coincidentes", count = matchingArtists.size)
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(matchingArtists, key = { it.name }) { artist ->
                                    ArtistCard(artist = artist, onClick = { onNavigateToArtist(artist.name) })
                                }
                            }
                        }
                    }

                    if (matchingSongs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Canciones", count = matchingSongs.size)
                        }
                        items(matchingSongs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                onClick = { onPlaySong(song) },
                                onEditTags = { onEditTags(song) }
                            )
                        }
                    }
                }
            } else {
                
                if (selectedFilter == "Todo") {
                    item {
                        QuickAccessGrid(
                            likedCount = allSongs.count { it.isLiked },
                            downloadsCount = allSongs.count { it.isYouTube },
                            onNavigateToFavorites = onNavigateToFavorites,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onOpenMostPlayed = {
                                userPlaylists.find { it.smartType == SmartPlaylistType.MOST_PLAYED }?.let {
                                    onNavigateToPlaylist(it.id)
                                }
                            },
                            onOpenRecentAdded = {
                                userPlaylists.find { it.smartType == SmartPlaylistType.RECENT_ADDED }?.let {
                                    onNavigateToPlaylist(it.id)
                                }
                            }
                        )
                    }
                }

                if (selectedFilter == "Todo") {
                    
                    val filteredPlaylists = userPlaylists
                    if (filteredPlaylists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Playlists",
                                count = filteredPlaylists.size,
                                onAction = { libraryViewModel.updateSelectedFilter("Playlists") }
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(filteredPlaylists.take(8), key = { it.id }) { playlist ->
                                    PlaylistCard(
                                        playlist = playlist,
                                        onClick = { onNavigateToPlaylist(playlist.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (allAlbums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionHeader(
                                title = "Álbumes",
                                count = allAlbums.size,
                                onAction = { libraryViewModel.updateSelectedFilter("Álbumes") }
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(allAlbums.take(8), key = { it.name }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onNavigateToAlbum(album.name) }
                                    )
                                }
                            }
                        }
                    }

                    if (allArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionHeader(
                                title = "Artistas",
                                count = allArtists.size,
                                onAction = { libraryViewModel.updateSelectedFilter("Artistas") }
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(allArtists.take(8), key = { it.name }) { artist ->
                                    ArtistCard(
                                        artist = artist,
                                        onClick = { onNavigateToArtist(artist.name) }
                                    )
                                }
                            }
                        }
                    }

                    if (allFolders.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionHeader(
                                title = "Carpetas",
                                count = allFolders.size,
                                onAction = { libraryViewModel.updateSelectedFilter("Carpetas") }
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(allFolders.take(8), key = { it.name }) { folder ->
                                    FolderCard(
                                        folder = folder,
                                        onClick = { onNavigateToFolder(folder.name) }
                                    )
                                }
                            }
                        }
                    }

                    if (allSongs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(
                                title = "Todas las canciones",
                                count = allSongs.size,
                                onAction = { libraryViewModel.updateSelectedFilter("Todo") }
                            )
                        }
                        items(allSongs.take(10), key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                onClick = { onPlaySong(song) },
                                onEditTags = { onEditTags(song) }
                            )
                        }
                    }
                }

                if (selectedFilter == "Playlists") {
                    val filtered = userPlaylists
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Todas las playlists (${filtered.size})",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = onCreatePlaylist,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nueva", fontSize = 13.sp)
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item { EmptySectionState(title = "No hay playlists creadas aún") }
                    } else {
                        
                        items(filtered.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (playlist in pair) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PlaylistCard(
                                            playlist = playlist,
                                            onClick = { onNavigateToPlaylist(playlist.id) },
                                            modifier = Modifier.fillMaxWidth().height(190.dp)
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (selectedFilter == "Álbumes") {
                    item {
                        Text(
                            "Todos los álbumes (${allAlbums.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (allAlbums.isEmpty()) {
                        item { EmptySectionState(title = "No se encontraron álbumes") }
                    } else {
                        items(allAlbums.chunked(2), key = { chunk -> chunk.first().name }) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (album in pair) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AlbumCard(
                                            album = album,
                                            onClick = { onNavigateToAlbum(album.name) },
                                            modifier = Modifier.fillMaxWidth().height(190.dp)
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (selectedFilter == "Artistas") {
                    item {
                        Text(
                            "Todos los artistas (${allArtists.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (allArtists.isEmpty()) {
                        item { EmptySectionState(title = "No se encontraron artistas") }
                    } else {
                        items(allArtists, key = { it.name }) { artist ->
                            ArtistRow(artist = artist, onClick = { onNavigateToArtist(artist.name) })
                        }
                    }
                }

                if (selectedFilter == "Carpetas") {
                    item {
                        Text(
                            "Todas las carpetas (${allFolders.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (allFolders.isEmpty()) {
                        item { EmptySectionState(title = "No se encontraron carpetas con música") }
                    } else {
                        items(allFolders, key = { it.name }) { folder ->
                            FolderRow(folder = folder, onClick = { onNavigateToFolder(folder.name) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessGrid(
    likedCount: Int,
    downloadsCount: Int,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onOpenRecentAdded: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessTile(
                title = "Favoritos",
                subtitle = "$likedCount guardadas",
                icon = Icons.Filled.Favorite,
                gradient = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFavorites
            )
            QuickAccessTile(
                title = "Descargas",
                subtitle = "$downloadsCount offline",
                icon = Icons.Filled.Download,
                gradient = listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDownloads
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessTile(
                title = "Top Escuchadas",
                subtitle = "Smart Playlist",
                icon = Icons.Filled.Star,
                gradient = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
                modifier = Modifier.weight(1f),
                onClick = onOpenMostPlayed
            )
            QuickAccessTile(
                title = "Recientes",
                subtitle = "Agregados hoy",
                icon = Icons.Filled.Schedule,
                gradient = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                modifier = Modifier.weight(1f),
                onClick = onOpenRecentAdded
            )
        }
    }
}

@Composable
fun QuickAccessTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "tile_scale"
    )

    GlassCard(
        modifier = modifier
            .scale(scale)
            .height(84.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "playlist_scale"
    )

    val coverImage = playlist.coverUrl ?: playlist.songs.firstOrNull()?.coverUrl

    GlassCard(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .width(160.dp)
            .height(185.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(playlist.coverColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    playlist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${playlist.songCount} canciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: AlbumGroup,
    onClick: () -> Unit,
    onPlay: (List<Song>) -> Unit = {},
    onShuffle: (List<Song>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "album_scale"
    )

    GlassCard(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .width(160.dp)
            .height(185.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(album.coverColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Album,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    album.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${album.songs.size} canciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ArtistCard(
    artist: ArtistGroup,
    onClick: () -> Unit,
    onPlay: (List<Song>) -> Unit = {},
    onShuffle: (List<Song>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "artist_scale"
    )

    GlassCard(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .width(140.dp)
            .height(175.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (artist.coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artist.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(artist.coverColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            artist.name.take(1).uppercase(),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${artist.songs.size} canciones",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: FolderGroup,
    onClick: () -> Unit,
    onPlay: (List<Song>) -> Unit = {},
    onShuffle: (List<Song>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "folder_scale"
    )

    GlassCard(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .width(160.dp)
            .height(145.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                folder.name.substringAfterLast("/"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                "${folder.songs.size} canciones",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumRow(album: AlbumGroup, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(album.coverColor)),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUrl != null) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.Album, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    album.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text("${album.songs.size} canciones", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: ArtistGroup, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(artist.coverColor)),
                contentAlignment = Alignment.Center
            ) {
                if (artist.coverUrl != null) {
                    AsyncImage(
                        model = artist.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        artist.name.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    artist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text("${artist.songs.size} canciones", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FolderRow(folder: FolderGroup, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    folder.name.substringAfterLast("/"),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text("${folder.songs.size} canciones · ${folder.name}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun EmptySearchResultsState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Text(
                "Sin resultados para \"$query\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Intenta buscar con otro término o revisa tus filtros",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptySectionState(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FolderDetailScreen(
    folderName: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onEditTags: (Song) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    folderName.substringAfterLast("/"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${songs.size} canciones",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onPlayAll(songs) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = { onShuffleAll(songs.shuffled()) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aleatorio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                SongListItem(
                    song = song,
                    onClick = { onPlaySong(song) },
                    onEditTags = { onEditTags(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlaySongInPlaylist: (Playlist, Song) -> Unit,
    onShufflePlaylist: (Playlist) -> Unit,
    onResumePlaylist: (Playlist, String, Long) -> Unit,
    onEditTags: (Song) -> Unit = {},
    onAddSongsClick: () -> Unit = {},
    onReorderSongs: ((List<Song>) -> Unit)? = null,
    onRemoveSong: ((String) -> Unit)? = null,
    onUpdateCover: ((Uri) -> Unit)? = null
) {
    var isOrganizeMode by remember { mutableStateOf(false) }
    var currentSongs by remember(playlist.songs) { mutableStateOf(playlist.songs) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && onUpdateCover != null) {
            onUpdateCover(uri)
        }
    }

    val filteredSongs = remember(currentSongs, searchQuery) {
        if (searchQuery.isBlank()) currentSongs
        else currentSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalDurationSeconds = remember(currentSongs) {
        currentSongs.sumOf { it.duration.toLong() }
    }
    val formattedDuration = remember(totalDurationSeconds) {
        if (totalDurationSeconds <= 0) null
        else {
            val mins = totalDurationSeconds / 60
            val hours = mins / 60
            val remMins = mins % 60
            if (hours > 0) "${hours} h ${remMins} min" else "${mins} min"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            
            if (!playlist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    playlist.coverColor.firstOrNull() ?: accent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color.White)
                    }

                    if (onUpdateCover != null) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "Cambiar Portada", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = onAddSongsClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir Canciones", tint = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.2f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!playlist.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = playlist.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "PLAYLIST LOCAL",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp
                        )
                        formattedDuration?.let { dur ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(dur, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = playlist.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${currentSongs.size} canciones guardadas",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (currentSongs.isNotEmpty()) onPlaySongInPlaylist(playlist, currentSongs.first()) },
                enabled = currentSongs.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = { onShufflePlaylist(playlist) },
                enabled = currentSongs.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aleatorio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            FilledTonalIconButton(
                onClick = { isOrganizeMode = !isOrganizeMode },
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isOrganizeMode) accent else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isOrganizeMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isOrganizeMode) Icons.Filled.Check else Icons.AutoMirrored.Filled.Sort,
                    contentDescription = if (isOrganizeMode) "Listo" else "Organizar playlist",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar en esta playlist...", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = accent) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (isOrganizeMode) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DragHandle, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Mantén presionado y arrastra ☰ para ordenar en tiempo real",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (currentSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Esta playlist está vacía",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onAddSongsClick) {
                        Text("Añadir canciones")
                    }
                }
            }
        } else {
            
            val targetIndex = if (draggedIndex != -1) {
                val shift = (draggedOffset / itemHeightPx).let {
                    if (it >= 0) Math.floor(it.toDouble()).toInt()
                    else Math.ceil(it.toDouble()).toInt()
                }
                (draggedIndex + shift).coerceIn(0, filteredSongs.lastIndex)
            } else -1

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                    val isDragged = index == draggedIndex

                    val itemTranslationY = when {
                        isDragged -> draggedOffset
                        draggedIndex != -1 && index in (draggedIndex + 1..targetIndex) -> -itemHeightPx
                        draggedIndex != -1 && index in (targetIndex until draggedIndex) -> itemHeightPx
                        else -> 0f
                    }

                    SongListItem(
                        song = song,
                        onClick = {
                            if (!isOrganizeMode) onPlaySong(song)
                        },
                        onEditTags = { onEditTags(song) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragged) 10f else 0f)
                            .graphicsLayer {
                                translationY = itemTranslationY
                                shadowElevation = if (isDragged) 16f else 0f
                            }
                            .border(
                                width = if (isDragged) 1.5.dp else 0.dp,
                                color = if (isDragged) accent else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        trailingContent = if (isOrganizeMode) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (onRemoveSong != null) {
                                        IconButton(
                                            onClick = {
                                                val mutable = currentSongs.filter { it.id != song.id }
                                                currentSongs = mutable
                                                onRemoveSong(song.id)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Quitar",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .pointerInput(index, filteredSongs.size) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedIndex = index
                                                        draggedOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        draggedOffset += dragAmount.y
                                                    },
                                                    onDragEnd = {
                                                        if (draggedIndex != -1 && draggedIndex != targetIndex && targetIndex != -1) {
                                                            val mutable = currentSongs.toMutableList()
                                                            if (draggedIndex in mutable.indices && targetIndex in mutable.indices) {
                                                                val item = mutable.removeAt(draggedIndex)
                                                                mutable.add(targetIndex, item)
                                                                currentSongs = mutable
                                                                onReorderSongs?.invoke(mutable)
                                                            }
                                                        }
                                                        draggedIndex = -1
                                                        draggedOffset = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedIndex = -1
                                                        draggedOffset = 0f
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.DragHandle,
                                            contentDescription = "Arrastrar para reordenar",
                                            tint = if (isDragged) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    songs: List<Song>,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Favoritos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${songs.size} canciones guardadas",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Sin favoritos aún",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Marca canciones con el corazón\npara verlas aquí",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reproducir todo", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onPlaySong(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "qa_scale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}
