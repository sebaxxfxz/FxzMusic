package com.fxzmusic.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.components.AutoResizeText
import com.fxzmusic.app.ui.components.EmptyState
import com.fxzmusic.app.ui.components.ShimmerList
import com.fxzmusic.app.ui.components.SongListItem
import com.fxzmusic.app.ui.components.YouTubeAlbumCard
import com.fxzmusic.app.ui.components.YouTubePlaylistCard
import com.fxzmusic.app.ui.components.YouTubeSectionHeaderMejorado
import com.fxzmusic.app.util.toSong
import com.fxzmusic.app.viewmodel.ChartsUiState
import com.fxzmusic.app.viewmodel.ExploreUiState
import com.fxzmusic.app.viewmodel.HistoryUiState
import com.fxzmusic.app.viewmodel.YouTubeMusicViewModel
import com.fxzmusic.innertube.models.AlbumItem
import com.fxzmusic.innertube.models.PlaylistItem
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.pages.ChartsPage
import com.fxzmusic.innertube.pages.HistoryPage
import com.fxzmusic.innertube.pages.MoodAndGenres

enum class CategoryType(val title: String, val subtitle: String) {
    TRENDS("Tendencias", "Lo más escuchado ahora"),
    PODCASTS("Podcasts", "Episodios y canales"),
    HISTORY("Historial", "Tu actividad de reproducción"),
    GENRES("Géneros y estados de ánimo", "Explora por categoría")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: CategoryType,
    viewModel: YouTubeMusicViewModel,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAlbum: (browseId: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(category) {
        when (category) {
            CategoryType.TRENDS -> viewModel.loadCharts()
            CategoryType.HISTORY -> viewModel.loadHistory()
            CategoryType.GENRES -> viewModel.loadExplore()
            CategoryType.PODCASTS -> viewModel.loadCharts()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 3 && category == CategoryType.TRENDS
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMoreCharts()
    }

    val accent = MaterialTheme.colorScheme.primary
    val chartType = viewModel.selectedChartType
    val chartSections = (viewModel.chartsState as? ChartsUiState.Success)?.page?.sections
    val filteredSections = if (chartType != null && chartSections != null) {
        chartSections.filter { it.chartType == chartType }
    } else {
        chartSections
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { CategoryHeader(title = category.title, subtitle = category.subtitle, accent = accent, onBack = onBack) }

        if (category == CategoryType.TRENDS && chartSections != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = chartType == null,
                        onClick = { viewModel.setChartType(null) },
                        label = { Text("Todo") },
                    )
                    ChartsPage.ChartType.entries.forEach { type ->
                        FilterChip(
                            selected = chartType == type,
                            onClick = { viewModel.setChartType(type) },
                            label = {
                                Text(
                                    when (type) {
                                        ChartsPage.ChartType.TOP -> "Top"
                                        ChartsPage.ChartType.TRENDING -> "Tendencia"
                                        ChartsPage.ChartType.GENRE -> "Género"
                                        ChartsPage.ChartType.NEW_RELEASES -> "Nuevos"
                                    }
                                )
                            },
                        )
                    }
                }
            }
        }

        when (category) {
            CategoryType.TRENDS, CategoryType.PODCASTS -> {
                when (val s = viewModel.chartsState) {
                    is ChartsUiState.Idle, is ChartsUiState.Loading -> {
                        item { ShimmerList(rows = 6) }
                    }
                    is ChartsUiState.Error -> {
                        item {
                            EmptyState(
                                icon = Icons.Filled.ErrorOutline,
                                title = "Error",
                                subtitle = s.message,
                                action = { viewModel.loadCharts() },
                                actionText = "Reintentar"
                            )
                        }
                    }
                    is ChartsUiState.Success -> {
                        chartsItems(filteredSections ?: emptyList(), onPlaySong, onOpenAlbum, onOpenPlaylist)
                        if (s.page.continuation != null) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            CategoryType.HISTORY -> {
                when (val s = viewModel.historyState) {
                    is HistoryUiState.Idle, is HistoryUiState.Loading -> {
                        item { ShimmerList(rows = 6) }
                    }
                    is HistoryUiState.Error -> {
                        item {
                            EmptyState(
                                icon = Icons.Filled.ErrorOutline,
                                title = "Sin historial",
                                subtitle = s.message,
                                action = onBack,
                                actionText = "Volver"
                            )
                        }
                    }
                    is HistoryUiState.Success -> {
                        if (s.sections.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Filled.History,
                                    title = "Aún no hay historial",
                                    subtitle = "Las canciones que escuches aparecerán aquí",
                                    action = onBack,
                                    actionText = "Explorar música"
                                )
                            }
                        } else {
                            historyItems(s.sections, onPlaySong)
                        }
                    }
                }
            }
            CategoryType.GENRES -> {
                when (val s = viewModel.exploreState) {
                    is ExploreUiState.Idle, is ExploreUiState.Loading -> {
                        item { ShimmerList(rows = 6) }
                    }
                    is ExploreUiState.Error -> {
                        item {
                            EmptyState(
                                icon = Icons.Filled.ErrorOutline,
                                title = "Error",
                                subtitle = s.message,
                                action = { viewModel.loadExplore() },
                                actionText = "Reintentar"
                            )
                        }
                    }
                    is ExploreUiState.Success -> {
                        exploreItems(s.albums, s.moods, accent, onOpenAlbum)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String, subtitle: String, accent: Color, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 20.dp)
    ) {
        Column {
            FilledIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.height(16.dp))
            AutoResizeText(
                text = title,
                targetFontSize = 32.sp,
                minFontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun LazyListScope.chartsItems(
    sections: List<ChartsPage.ChartSection>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit
) {
    sections.forEach { section ->
        item { YouTubeSectionHeaderMejorado(title = section.title, subtitle = "${section.items.size} elementos") }
        val songs = section.items.filterIsInstance<SongItem>()
        val albums = section.items.filterIsInstance<AlbumItem>()
        val playlists = section.items.filterIsInstance<PlaylistItem>()

        if (songs.isNotEmpty()) {
            itemsIndexed(songs, key = { _, it -> it.id }) { _, song ->
                SongListItem(
                    song = song.toSong(),
                    onClick = {
                        val all = songs.map { it.toSong() }
                        onPlaySong(song.toSong(), all)
                    }
                )
            }
        }
        if (albums.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(albums, key = { it.browseId }) { album ->
                        YouTubeAlbumCard(album = album, onClick = { onOpenAlbum(album.browseId) })
                    }
                }
            }
        }
        if (playlists.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        YouTubePlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }
    }
}

private fun LazyListScope.historyItems(
    sections: List<HistoryPage.HistorySection>,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    sections.forEach { section ->
        item { YouTubeSectionHeaderMejorado(title = section.title, subtitle = "${section.songs.size} canciones") }
        itemsIndexed(section.songs, key = { _, it -> it.id }) { _, song ->
            SongListItem(
                song = song.toSong(),
                onClick = {
                    val all = section.songs.map { it.toSong() }
                    onPlaySong(song.toSong(), all)
                }
            )
        }
    }
}

private fun LazyListScope.exploreItems(
    albums: List<AlbumItem>,
    moods: List<MoodAndGenres.Item>,
    accent: Color,
    onOpenAlbum: (String) -> Unit
) {
    if (albums.isNotEmpty()) {
        item { YouTubeSectionHeaderMejorado(title = "Nuevos lanzamientos", subtitle = "${albums.size} álbumes") }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(albums, key = { it.browseId }) { album ->
                    YouTubeAlbumCard(album = album, onClick = { onOpenAlbum(album.browseId) })
                }
            }
        }
    }
    if (moods.isNotEmpty()) {
        item { YouTubeSectionHeaderMejorado(title = "Géneros y estados de ánimo", subtitle = "${moods.size} categorías") }
        itemsIndexed(moods) { _, mood ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Text(mood.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
