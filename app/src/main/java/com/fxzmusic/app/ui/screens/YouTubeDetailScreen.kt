package com.fxzmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.widget.Toast
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fxzmusic.app.data.Playlist
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.components.AutoResizeText
import com.fxzmusic.app.ui.components.EmptyState
import com.fxzmusic.app.ui.components.ExpandableText
import com.fxzmusic.app.ui.components.PlaylistPickerSheet
import com.fxzmusic.app.ui.components.ShimmerList
import com.fxzmusic.app.ui.components.SongListItem
import com.fxzmusic.app.ui.components.YouTubeAlbumCard
import com.fxzmusic.app.ui.components.YouTubeArtistCard
import com.fxzmusic.app.ui.components.YouTubeSectionHeaderMejorado
import com.fxzmusic.app.util.toSong
import com.fxzmusic.app.util.toLocalSong
import com.fxzmusic.app.util.toLocalSongs
import com.fxzmusic.app.viewmodel.DetailUiState
import com.fxzmusic.app.viewmodel.YouTubeMusicViewModel
import com.fxzmusic.innertube.models.AlbumItem
import com.fxzmusic.innertube.models.ArtistItem
import com.fxzmusic.innertube.models.SongItem

@Composable
fun YouTubeDetailScreen(
    viewModel: YouTubeMusicViewModel,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddSongsToQueue: (List<Song>) -> Unit = {},
    onOpenAlbum: (browseId: String) -> Unit = {},
    onOpenArtist: (browseId: String) -> Unit = {},
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    
    targetId: String? = null,
    
    targetType: String? = null,
) {
    val state = viewModel.detail

    androidx.compose.runtime.LaunchedEffect(targetId, targetType) {
        if (targetId != null && state is DetailUiState.Idle) {
            when (targetType) {
                "album"    -> viewModel.openAlbum(targetId)
                "artist"   -> viewModel.openArtist(targetId)
                "playlist" -> viewModel.openPlaylist(targetId)
            }
        }
    }

    when (state) {
        is DetailUiState.Idle,
        is DetailUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 48.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = { viewModel.clearDetail(); onBack() },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                ShimmerList(rows = 4)
            }
        }
        is DetailUiState.Error -> {
            EmptyState(
                icon = Icons.Filled.ErrorOutline,
                title = "Error",
                subtitle = state.message,
                action = onBack,
                actionText = "Volver"
            )
        }
        is DetailUiState.Album -> {
            AlbumDetail(
                albumPage = state.page,
                onBack = { viewModel.clearDetail(); onBack() },
                onPlaySong = onPlaySong,
                onPlayNext = onPlayNext,
                onAddSongsToQueue = onAddSongsToQueue,
                onOpenAlbum = { onOpenAlbum(it) },
                onOpenArtist = { onOpenArtist(it) },
            )
        }
        is DetailUiState.Artist -> {
            ArtistDetail(
                artistPage = state.page,
                onBack = { viewModel.clearDetail(); onBack() },
                onPlaySong = onPlaySong,
                onPlayNext = onPlayNext,
                onAddSongsToQueue = onAddSongsToQueue,
                onOpenAlbum = { onOpenAlbum(it) },
                onOpenArtist = { onOpenArtist(it) },
            )
        }
        is DetailUiState.Playlist -> {
            PlaylistDetail(
                playlistPage = state.page,
                onBack = { viewModel.clearDetail(); onBack() },
                onPlaySong = onPlaySong,
                onPlayNext = onPlayNext,
                onAddSongsToQueue = onAddSongsToQueue,
                onOpenArtist = { onOpenArtist(it) },
                onOpenAlbum = { onOpenAlbum(it) },
            )
        }
    }
}

@Composable
private fun AlbumDetail(
    albumPage: com.fxzmusic.innertube.pages.AlbumPage,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onOpenAlbum: (browseId: String) -> Unit = {},
    onOpenArtist: (browseId: String) -> Unit = {},
) {
    val album = albumPage.album
    val songs = albumPage.songs
    val accentColor = MaterialTheme.colorScheme.primary
    var contextMenuSong by remember { mutableStateOf<SongItem?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(mutableSetOf<String>()) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var showBatchDownloadDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedSongs.clear()
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val parallaxOffset by remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isNotEmpty() && layoutInfo.visibleItemsInfo.first().index == 0) {
                layoutInfo.visibleItemsInfo.first().offset.toFloat() * 0.5f
            } else 0f
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        if (isSelectionMode) {
            item {
                SelectionModeToolbar(
                    selectedCount = selectedSongs.size,
                    totalCount = songs.size,
                    onClose = { isSelectionMode = false; selectedSongs.clear() },
                    onSelectAll = {
                        if (selectedSongs.size == songs.size) {
                            selectedSongs.clear()
                        } else {
                            selectedSongs.addAll(songs.map { it.id })
                        }
                    },
                    onAddToPlaylist = { showPlaylistPicker = true },
                    onAddToQueue = {
                        val toAdd = songs.filter { it.id in selectedSongs }.map { it.toLocalSong() }
                        onAddSongsToQueue(toAdd)
                        isSelectionMode = false
                        selectedSongs.clear()
                    },
                    onShare = {
                        isSelectionMode = false
                        selectedSongs.clear()
                    },
                    onDownloadSelection = {
                        if (selectedSongs.isNotEmpty()) showBatchDownloadDialog = true
                    }
                )
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                AsyncImage(
                    model = album.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = -parallaxOffset }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
                FilledIconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 16.dp, top = 48.dp).align(Alignment.TopStart),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Text(
                        "ÁLBUM",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AutoResizeText(
                        text = album.title,
                        targetFontSize = 26.sp,
                        minFontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2
                    )
                    album.artists?.firstOrNull()?.let { artist ->
                        Text(
                            artist.name,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { artist.id?.let { onOpenArtist(it) } }
                        )
                    }
                    album.year?.let { year ->
                        val totalDuration = songs.sumOf { it.duration?.coerceAtLeast(0) ?: 0 }
                        val minutes = totalDuration / 60000
                        Text(
                            text = "$year • ${songs.size} canciones${if (minutes > 0) " · ${minutes} min" else ""}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val allSongs = songs.toLocalSongs()
                            onPlaySong(allSongs.first(), allSongs)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reproducir", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.toLocalSongs().shuffled()
                            onPlaySong(shuffled.first(), shuffled)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aleatorio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                FilledTonalIconButton(
                    onClick = { if (songs.isNotEmpty()) showBatchDownloadDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Descargar Álbum")
                }
            }
        }

        val description = albumPage.description ?: if (songs.isNotEmpty()) {
            val artistName = album.artists?.firstOrNull()?.name ?: ""
            val year = album.year?.toString() ?: ""
            "${album.title} es un álbum de $artistName${if (year.isNotEmpty()) ", lanzado en $year" else ""}. Esta colección incluye ${songs.size} canciones."
        } else null
        if (description != null) {
            item {
                ExpandableText(
                    text = description,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    collapsedMaxLines = 3,
                )
            }
        }

        if (songs.isNotEmpty()) {
            item {
                YouTubeSectionHeaderMejorado(
                    title = "Canciones",
                    subtitle = "${songs.size} canciones"
                )
            }
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { _, song ->
                SongListItem(
                    song = song.toLocalSong(),
                    onClick = {
                        val allSongs = songs.toLocalSongs()
                        onPlaySong(song.toLocalSong(), allSongs)
                    },
                    onLongClick = { contextMenuSong = song },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (albumPage.otherVersions.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    YouTubeSectionHeaderMejorado(title = "Otras versiones")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(
                            items = albumPage.otherVersions,
                            key = { it.browseId }
                        ) { ver ->
                            YouTubeAlbumCard(album = ver, onClick = { onOpenAlbum(ver.browseId) })
                        }
                    }
                }
            }
        }

        if (albumPage.releasesForYou.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    YouTubeSectionHeaderMejorado(title = "Lanzamientos para ti")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(
                            items = albumPage.releasesForYou,
                            key = { it.browseId }
                        ) { rel ->
                            YouTubeAlbumCard(album = rel, onClick = { onOpenAlbum(rel.browseId) })
                        }
                    }
                }
            }
        }
    }

    val currentSong = contextMenuSong
    if (currentSong != null) {
        val downloadUtil = remember { com.fxzmusic.app.service.DownloadUtil.get() }
        val dlDownloads by downloadUtil.downloads.collectAsState()
        val dlVideoId = currentSong.id
        val dlIsDownloaded = dlDownloads[dlVideoId]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
        com.fxzmusic.app.ui.components.YouTubeSongContextMenu(
            title = currentSong.title,
            artistName = currentSong.artists.joinToString(", ") { it.name },
            albumBrowseId = currentSong.album?.id,
            onPlayNext = { onPlayNext(currentSong.toLocalSong()) },
            onAddToQueue = { onAddSongsToQueue(listOf(currentSong.toLocalSong())) },
            onViewArtist = { currentSong.artists.firstOrNull()?.id?.let { onOpenArtist(it) } },
            onViewAlbum = { currentSong.album?.id?.let { onOpenAlbum(it) } },
            onDownload = if (!dlIsDownloaded) {{
                downloadUtil.enqueue(
                    videoId = dlVideoId,
                    title = currentSong.title,
                    artist = currentSong.artists.joinToString(", ") { it.name },
                    album = currentSong.album?.name ?: "",
                    coverUrl = currentSong.thumbnail
                )
            }} else null,
            onDeleteDownload = if (dlIsDownloaded) {{ downloadUtil.delete(dlVideoId) }} else null,
            isDownloaded = dlIsDownloaded,
            onDismiss = { contextMenuSong = null },
        )
    }

    if (showPlaylistPicker) {
        PlaylistPickerSheet(
            playlists = playlists,
            currentSong = songs.filter { it.id in selectedSongs }.map { it.toLocalSong() }.firstOrNull(),
            onAddToPlaylist = { playlist ->
                val toAdd = songs.filter { it.id in selectedSongs }.map { it.toLocalSong() }
                
                showPlaylistPicker = false
                isSelectionMode = false
                selectedSongs.clear()
            },
            onCreateNew = { name ->
                
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    if (showBatchDownloadDialog) {
        com.fxzmusic.app.ui.components.BatchDownloadDialog(
            songs = songs.toLocalSongs(),
            title = album.title,
            coverUrl = album.thumbnail,
            onDismiss = { showBatchDownloadDialog = false },
            onConfirmDownload = { quality ->
                com.fxzmusic.app.service.DownloadUtil.get().enqueueBatch(songs.toLocalSongs(), quality)
            }
        )
    }
}

@Composable
private fun ArtistDetail(
    artistPage: com.fxzmusic.innertube.pages.ArtistPage,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onOpenAlbum: (browseId: String) -> Unit = {},
    onOpenArtist: (browseId: String) -> Unit = {},
) {
    val artist = artistPage.artist
    val accentColor = MaterialTheme.colorScheme.primary
    var contextMenuSong by remember { mutableStateOf<SongItem?>(null) }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val parallaxOffset by remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isNotEmpty() && layoutInfo.visibleItemsInfo.first().index == 0) {
                layoutInfo.visibleItemsInfo.first().offset.toFloat() * 0.5f
            } else 0f
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(
                    model = artist.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = -parallaxOffset }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
                FilledIconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 16.dp, top = 48.dp).align(Alignment.TopStart),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Text(
                        "ARTISTA",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AutoResizeText(
                        text = artist.title,
                        targetFontSize = 28.sp,
                        minFontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        artistPage.subscriberCountText?.let { count ->
                            Text(
                                count,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        artistPage.monthlyListenerCount?.let { listeners ->
                            Text(
                                "$listeners oyentes",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        artistPage.sections.forEach { section ->
            item {
                YouTubeSectionHeaderMejorado(
                    title = section.title,
                    subtitle = "${section.items.size} elementos"
                )
            }

            val songs = section.items.filterIsInstance<SongItem>()
            val albums = section.items.filterIsInstance<AlbumItem>()
            val artists = section.items.filterIsInstance<ArtistItem>()

if (songs.isNotEmpty()) {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { _, song ->
                SongListItem(
                    song = song.toLocalSong(),
                    onClick = {
                        val allSongs = songs.toLocalSongs()
                        onPlaySong(song.toLocalSong(), allSongs)
                    },
                    onLongClick = { contextMenuSong = song },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (albums.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(
                            items = albums,
                            key = { it.browseId }
                        ) { album ->
                            YouTubeAlbumCard(
                                album = album,
                                onClick = { onOpenAlbum(album.browseId) }
                            )
                        }
                    }
                }
            }

            if (artists.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(
                            items = artists,
                            key = { it.id }
                        ) { artist ->
                            YouTubeArtistCard(
                                artist = artist,
                                onClick = { onOpenArtist(artist.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    val currentSong = contextMenuSong
    if (currentSong != null) {
        val downloadUtil = remember { com.fxzmusic.app.service.DownloadUtil.get() }
        val dlDownloads by downloadUtil.downloads.collectAsState()
        val dlVideoId = currentSong.id
        val dlIsDownloaded = dlDownloads[dlVideoId]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
        com.fxzmusic.app.ui.components.YouTubeSongContextMenu(
            title = currentSong.title,
            artistName = currentSong.artists.joinToString(", ") { it.name },
            albumBrowseId = currentSong.album?.id,
            onPlayNext = { onPlayNext(currentSong.toLocalSong()) },
            onAddToQueue = { onAddSongsToQueue(listOf(currentSong.toLocalSong())) },
            onViewArtist = { currentSong.artists.firstOrNull()?.id?.let { onOpenArtist(it) } },
            onViewAlbum = { currentSong.album?.id?.let { onOpenAlbum(it) } },
            onDownload = if (!dlIsDownloaded) {
                {
                    downloadUtil.enqueue(
                        videoId = dlVideoId,
                        title = currentSong.title,
                        artist = currentSong.artists.joinToString(", ") { it.name },
                        album = currentSong.album?.name ?: "",
                        coverUrl = currentSong.thumbnail
                    )
                }
            } else null,
            onDeleteDownload = if (dlIsDownloaded) { { downloadUtil.delete(dlVideoId) } } else null,
            isDownloaded = dlIsDownloaded,
            onDismiss = { contextMenuSong = null }
        )
    }
}

private enum class PlaylistSortOption(val label: String) {
    Original("Original"),
    TitleAZ("A-Z"),
    Artist("Artista"),
    Duration("Duración")
}

@Composable
private fun PlaylistDetail(
    playlistPage: com.fxzmusic.innertube.pages.PlaylistPage,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onOpenArtist: (browseId: String) -> Unit = {},
    onOpenAlbum: (browseId: String) -> Unit = {},
) {
    val playlist = playlistPage.playlist
    val songs = playlistPage.songs
    val accentColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    var contextMenuSong by remember { mutableStateOf<SongItem?>(null) }
    var showBatchDownloadDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(PlaylistSortOption.Original) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(mutableSetOf<String>()) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    BackHandler(enabled = isSelectionMode || isSearchActive) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedSongs.clear()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    val totalDurationSeconds = remember(songs) {
        songs.sumOf { (it.duration ?: 0).toLong() }
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

    val filteredAndSortedSongs = remember(songs, searchQuery, selectedSort) {
        var list = songs.toLocalSongs()
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
        when (selectedSort) {
            PlaylistSortOption.Original -> list
            PlaylistSortOption.TitleAZ -> list.sortedBy { it.title.lowercase() }
            PlaylistSortOption.Artist -> list.sortedBy { it.artist.lowercase() }
            PlaylistSortOption.Duration -> list.sortedByDescending { it.duration }
        }
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val parallaxOffset by remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isNotEmpty() && layoutInfo.visibleItemsInfo.first().index == 0) {
                layoutInfo.visibleItemsInfo.first().offset.toFloat() * 0.5f
            } else 0f
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        if (isSelectionMode) {
            item {
                SelectionModeToolbar(
                    selectedCount = selectedSongs.size,
                    totalCount = songs.size,
                    onClose = { isSelectionMode = false; selectedSongs.clear() },
                    onSelectAll = {
                        if (selectedSongs.size == songs.size) {
                            selectedSongs.clear()
                        } else {
                            selectedSongs.addAll(songs.map { it.id })
                        }
                    },
                    onAddToPlaylist = { showPlaylistPicker = true },
                    onAddToQueue = {
                        val toAdd = songs.filter { it.id in selectedSongs }.map { it.toLocalSong() }
                        onAddSongsToQueue(toAdd)
                        isSelectionMode = false
                        selectedSongs.clear()
                    },
                    onShare = {
                        isSelectionMode = false
                        selectedSongs.clear()
                    },
                    onDownloadSelection = {
                        if (selectedSongs.isNotEmpty()) showBatchDownloadDialog = true
                    }
                )
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                AsyncImage(
                    model = playlist.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = -parallaxOffset }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledIconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isSearchActive) accentColor else Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar en playlist")
                        }

                        FilledIconButton(
                            onClick = {
                                val shareUrl = "https://music.youtube.com/playlist?list=${playlist.id}"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareUrl)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartir Playlist"))
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartir")
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "PLAYLIST",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp
                        )
                        formattedDuration?.let { dur ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(dur, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AutoResizeText(
                        text = playlist.title,
                        targetFontSize = 26.sp,
                        minFontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        playlist.author?.let { author ->
                            Text(
                                author.name,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        playlist.songCountText?.let { count ->
                            Text(
                                count,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElevatedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val allSongs = songs.toLocalSongs()
                            onPlaySong(allSongs.first(), allSongs)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reproducir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.toLocalSongs().shuffled()
                            onPlaySong(shuffled.first(), shuffled)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aleatorio", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalIconButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val allSongs = songs.toLocalSongs()
                            onPlaySong(allSongs.first(), allSongs)
                            Toast.makeText(context, "Iniciando Radio de la Playlist...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Radio, contentDescription = "Radio de Playlist")
                }

                FilledTonalIconButton(
                    onClick = { if (songs.isNotEmpty()) showBatchDownloadDialog = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Descargar Playlist")
                }
            }
        }

        if (isSearchActive) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar en esta playlist...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = accentColor) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        if (songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (searchQuery.isBlank()) "${filteredAndSortedSongs.size} canciones" else "${filteredAndSortedSongs.size} de ${songs.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(PlaylistSortOption.entries.toTypedArray()) { sort ->
                            FilterChip(
                                selected = selectedSort == sort,
                                onClick = { selectedSort = sort },
                                label = { Text(sort.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = filteredAndSortedSongs,
                key = { _, song -> song.id }
            ) { _, song ->
                val isSelected = song.id in selectedSongs
                SongListItem(
                    song = song,
                    onClick = {
                        if (isSelectionMode) {
                            if (isSelected) selectedSongs.remove(song.id) else selectedSongs.add(song.id)
                        } else {
                            onPlaySong(song, filteredAndSortedSongs)
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            isSelectionMode = true
                            selectedSongs.add(song.id)
                        } else {
                            contextMenuSong = songs.firstOrNull { it.id == song.id }
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (contextMenuSong != null) {
        val currentSong = contextMenuSong!!
        val dlVideoId = currentSong.id
        val downloadUtil = remember { com.fxzmusic.app.service.DownloadUtil.get() }
        val dlDownloads by downloadUtil.downloads.collectAsState()
        val dlIsDownloaded = dlDownloads[dlVideoId]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED

        com.fxzmusic.app.ui.components.YouTubeSongContextMenu(
            title = currentSong.title,
            artistName = currentSong.artists.joinToString(", ") { it.name },
            albumBrowseId = currentSong.album?.id,
            onPlayNext = { onPlayNext(currentSong.toLocalSong()) },
            onAddToQueue = { onAddSongsToQueue(listOf(currentSong.toLocalSong())) },
            onViewArtist = { currentSong.artists.firstOrNull()?.id?.let { onOpenArtist(it) } },
            onViewAlbum = { currentSong.album?.id?.let { onOpenAlbum(it) } },
            onDownload = if (!dlIsDownloaded) {
                {
                    downloadUtil.enqueue(
                        videoId = dlVideoId,
                        title = currentSong.title,
                        artist = currentSong.artists.joinToString(", ") { it.name },
                        album = currentSong.album?.name ?: "",
                        coverUrl = currentSong.thumbnail
                    )
                }
            } else null,
            onDeleteDownload = if (dlIsDownloaded) { { downloadUtil.delete(dlVideoId) } } else null,
            isDownloaded = dlIsDownloaded,
            onDismiss = { contextMenuSong = null }
        )
    }

    if (showPlaylistPicker) {
        PlaylistPickerSheet(
            playlists = playlists,
            currentSong = songs.filter { it.id in selectedSongs }.map { it.toLocalSong() }.firstOrNull(),
            onAddToPlaylist = { playlist ->
                showPlaylistPicker = false
                isSelectionMode = false
                selectedSongs.clear()
            },
            onCreateNew = { name ->
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    if (showBatchDownloadDialog) {
        com.fxzmusic.app.ui.components.BatchDownloadDialog(
            songs = songs.toLocalSongs(),
            title = playlist.title,
            coverUrl = playlist.thumbnail,
            onDismiss = { showBatchDownloadDialog = false },
            onConfirmDownload = { quality ->
                com.fxzmusic.app.service.DownloadUtil.get().enqueueBatch(songs.toLocalSongs(), quality)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeToolbar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onShare: () -> Unit,
    onDownloadSelection: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = "$selectedCount de $totalCount seleccionados",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar selección")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = if (selectedCount == totalCount) Icons.Filled.Check else Icons.Filled.SelectAll,
                    contentDescription = if (selectedCount == totalCount) "Desmarcar todo" else "Seleccionar todo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (onDownloadSelection != null) {
                IconButton(onClick = onDownloadSelection) {
                    Icon(Icons.Filled.Download, contentDescription = "Descargar seleccionados", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar a playlist", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onAddToQueue) {
                Icon(Icons.Filled.QueueMusic, contentDescription = "Agregar a la cola", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}
