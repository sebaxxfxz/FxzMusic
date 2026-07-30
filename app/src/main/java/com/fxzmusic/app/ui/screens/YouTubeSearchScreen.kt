package com.fxzmusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.components.EmptyState
import com.fxzmusic.app.ui.components.FilterChipsRow
import com.fxzmusic.app.ui.components.InteractiveSearchBar
import com.fxzmusic.app.ui.components.ShimmerList
import com.fxzmusic.app.ui.components.YouTubeAlbumCard
import com.fxzmusic.app.ui.components.YouTubeArtistCard
import com.fxzmusic.app.ui.components.YouTubePlaylistCard
import com.fxzmusic.app.ui.components.YouTubeSearchSuggestion
import com.fxzmusic.app.ui.components.YouTubeSectionHeaderMejorado
import com.fxzmusic.app.ui.components.YouTubeSongCard
import com.fxzmusic.app.util.toLocalSong
import androidx.activity.compose.BackHandler
import com.fxzmusic.app.util.toSong
import com.fxzmusic.app.viewmodel.YouTubeMusicViewModel
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.models.AlbumItem
import com.fxzmusic.innertube.models.ArtistItem
import com.fxzmusic.innertube.models.PlaylistItem
import com.fxzmusic.app.viewmodel.SearchUiState

@Composable
fun YouTubeSearchScreen(
    viewModel: YouTubeMusicViewModel,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAlbum: (browseId: String) -> Unit,
    onOpenArtist: (browseId: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddSongsToQueue: (List<Song>) -> Unit = {},
) {
    val state = viewModel.search
    var selectedFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    var contextMenuSong by remember { mutableStateOf<SongItem?>(null) }

    val currentQuery = when (state) {
        is SearchUiState.Suggestions -> state.query
        is SearchUiState.Success -> state.query
        else -> ""
    }

    BackHandler(enabled = currentQuery.isNotBlank()) {
        viewModel.cancelSearch()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        InteractiveSearchBar(
            value = currentQuery,
            onValueChange = { viewModel.updateQuery(it, selectedFilter) },
            onSearch = {
                if (currentQuery.isNotBlank()) viewModel.runSearch(currentQuery, selectedFilter)
            },
            onClear = { viewModel.cancelSearch() }
        )

        if (currentQuery.isNotBlank() || state is SearchUiState.Success || state is SearchUiState.Loading) {
            val filterOptions: List<Pair<YouTube.SearchFilter?, String>> = listOf(
                null to "Todo",
                YouTube.SearchFilter.FILTER_SONG to "Canciones",
                YouTube.SearchFilter.FILTER_VIDEO to "Videos",
                YouTube.SearchFilter.FILTER_ALBUM to "Álbumes",
                YouTube.SearchFilter.FILTER_ARTIST to "Artistas",
                YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to "Playlists",
                YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST to "Comunidad",
            )
            FilterChipsRow(
                filters = filterOptions.map { it.second },
                selectedFilter = filterOptions.firstOrNull { it.first == selectedFilter }?.second ?: "Todo",
                onFilterSelected = { label ->
                    val matched = filterOptions.firstOrNull { it.second == label } ?: return@FilterChipsRow
                    selectedFilter = matched.first
                    if (currentQuery.isNotBlank()) {
                        viewModel.runSearch(currentQuery, matched.first)
                    }
                }
            )
        }

        if (state is SearchUiState.Success) {
            YouTubeSearchFilterToggles(viewModel = viewModel)
        }

        when (state) {
            is SearchUiState.Idle -> {
                val recentSearches = remember { viewModel.getRecentSearches() }
                if (recentSearches.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Búsquedas recientes",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp
                                )
                                Text(
                                    "Limpiar",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        viewModel.clearSearchHistory()
                                    }
                                )
                            }
                        }
                        items(
                            items = recentSearches,
                            key = { it }
                        ) { query ->
                            YouTubeSearchSuggestion(
                                text = query,
                                isRecent = true,
                                onClick = {
                                    viewModel.runSearch(query, selectedFilter)
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Buscar en YouTube Music",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            is SearchUiState.Suggestions -> {
                if (state.items.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = state.items,
                            key = { it }
                        ) { suggestion ->
                            YouTubeSearchSuggestion(
                                text = suggestion,
                                isRecent = false,
                                onClick = {
                                    viewModel.runSearch(suggestion, selectedFilter)
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            is SearchUiState.Loading -> {
                ShimmerList(rows = 5)
            }
            is SearchUiState.Error -> {
                EmptyState(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Error en la búsqueda",
                    subtitle = state.message,
                    action = {
                        val query = when (val s = viewModel.search) {
                            is SearchUiState.Suggestions -> s.query
                            else -> ""
                        }
                        if (query.isNotBlank()) viewModel.runSearch(query, selectedFilter)
                    },
                    actionText = "Reintentar"
                )
            }
            is SearchUiState.Success -> {
                val summaries = state.summaries
                if (summaries != null) {
                    YouTubeSearchSummaryView(
                        state = state,
                        viewModel = viewModel,
                        onPlaySong = onPlaySong,
                        onOpenAlbum = onOpenAlbum,
                        onOpenArtist = onOpenArtist,
                        onOpenPlaylist = onOpenPlaylist,
                        onPlayNext = onPlayNext,
                        onAddSongsToQueue = onAddSongsToQueue,
                        onLongClickSong = { contextMenuSong = it },
                    )
                } else {
                    val results = state.items
                    if (results.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.MusicNote,
                            title = "Sin resultados",
                            subtitle = "No se encontraron resultados para \"${state.query}\"",
                            action = { viewModel.cancelSearch() },
                            actionText = "Limpiar"
                        )
                    } else {
                        val songs = results.filterIsInstance<SongItem>()
                        val albums = results.filterIsInstance<AlbumItem>()
                        val artists = results.filterIsInstance<ArtistItem>()
                        val playlists = results.filterIsInstance<PlaylistItem>()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            if (songs.isNotEmpty()) {
                                item {
                                    YouTubeSectionHeaderMejorado(
                                        title = "Canciones",
                                        subtitle = "${songs.size} resultados"
                                    )
                                }
                                itemsIndexed(
                                    items = songs,
                                    key = { _, it -> it.id }
                                ) { index, song ->
                                    if (index == songs.lastIndex && state.continuation != null) {
                                        LaunchedEffect(state.continuation) {
                                            viewModel.loadMoreSearch()
                                        }
                                    }
                                    YouTubeSongCard(
                                        song = song,
                                        onClick = {
                                            val allSongs = songs.map { it.toLocalSong() }
                                            onPlaySong(song.toLocalSong(), allSongs)
                                        },
                                        onLongClick = { contextMenuSong = song },
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp)
                                    )
                                }
                                if (state.isLoadingMore) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (albums.isNotEmpty()) {
                                item {
                                    YouTubeSectionHeaderMejorado(
                                        title = "Álbumes",
                                        subtitle = "${albums.size} resultados"
                                    )
                                }
                                item {
                                    androidx.compose.foundation.lazy.LazyRow(
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
                                    YouTubeSectionHeaderMejorado(
                                        title = "Artistas",
                                        subtitle = "${artists.size} resultados"
                                    )
                                }
                                item {
                                    androidx.compose.foundation.lazy.LazyRow(
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

                            if (playlists.isNotEmpty()) {
                                item {
                                    YouTubeSectionHeaderMejorado(
                                        title = "Playlists",
                                        subtitle = "${playlists.size} resultados"
                                    )
                                }
                                item {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(horizontal = 20.dp)
                                    ) {
                                        items(
                                            items = playlists,
                                            key = { it.id }
                                        ) { playlist ->
                                            YouTubePlaylistCard(
                                                playlist = playlist,
                                                onClick = { onOpenPlaylist(playlist.id) }
                                            )
                                        }
                                    }
                                }
                            }
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
}

@Composable
private fun YouTubeSearchSummaryView(
    state: SearchUiState.Success,
    viewModel: YouTubeMusicViewModel,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAlbum: (browseId: String) -> Unit,
    onOpenArtist: (browseId: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onLongClickSong: (SongItem) -> Unit,
) {
    val summaries = state.summaries.orEmpty()
    if (summaries.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.MusicNote,
            title = "Sin resultados",
            subtitle = "No se encontraron resultados para \"${state.query}\"",
            action = { viewModel.cancelSearch() },
            actionText = "Limpiar"
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        summaries.forEachIndexed { sectionIndex, summary ->
            val titleDisplay = when (summary.title) {
                "Top result", "Top result album" -> "Top resultado"
                "Songs" -> "Canciones"
                "Videos" -> "Videos"
                "Albums" -> "Álbumes"
                "Artists" -> "Artistas"
                "Playlists" -> "Playlists"
                else -> summary.title
            }
            item(key = "header-$sectionIndex-${summary.title}") {
                YouTubeSectionHeaderMejorado(
                    title = titleDisplay,
                    subtitle = "${summary.items.size} resultados"
                )
            }

            val songs = summary.items.filterIsInstance<SongItem>()
            val albums = summary.items.filterIsInstance<AlbumItem>()
            val artists = summary.items.filterIsInstance<ArtistItem>()
            val playlists = summary.items.filterIsInstance<PlaylistItem>()

            if (songs.isNotEmpty()) {
                itemsIndexed(
                    items = songs,
                    key = { _, it -> "song-${it.id}" }
                ) { index, song ->
                    YouTubeSongCard(
                        song = song,
                        onClick = {
                            val allSongs = songs.map { it.toLocalSong() }
                            onPlaySong(song.toLocalSong(), allSongs)
                        },
                        onLongClick = { onLongClickSong(song) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp)
                    )
                }
            }

            if (albums.isNotEmpty()) {
                item(key = "albums-row-$sectionIndex") {
                    androidx.compose.foundation.lazy.LazyRow(
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
                item(key = "artists-row-$sectionIndex") {
                    androidx.compose.foundation.lazy.LazyRow(
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

            if (playlists.isNotEmpty()) {
                item(key = "playlists-row-$sectionIndex") {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(
                            items = playlists,
                            key = { it.id }
                        ) { playlist ->
                            YouTubePlaylistCard(
                                playlist = playlist,
                                onClick = { onOpenPlaylist(playlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeSearchFilterToggles(viewModel: YouTubeMusicViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterToggleChip(
            label = "Ocultar explícitos",
            active = viewModel.hideExplicit,
            onClick = { viewModel.toggleHideExplicit() }
        )
        FilterToggleChip(
            label = "Ocultar videos",
            active = viewModel.hideVideoSongs,
            onClick = { viewModel.toggleHideVideoSongs() }
        )
        FilterToggleChip(
            label = "Ocultar shorts",
            active = viewModel.hideShorts,
            onClick = { viewModel.toggleHideShorts() }
        )
    }
}

@Composable
private fun FilterToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(9999.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(border = androidx.compose.foundation.BorderStroke(1.dp, borderColor), shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
