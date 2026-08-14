package com.fxzmusic.app.ui.screens

import com.fxzmusic.app.*
import com.fxzmusic.app.R
import com.fxzmusic.app.data.*
import com.fxzmusic.app.viewmodel.*
import com.fxzmusic.app.ui.components.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.service.toSong
import androidx.compose.material3.ExperimentalMaterial3Api
import com.fxzmusic.app.util.*
import androidx.compose.runtime.collectAsState

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import com.fxzmusic.innertube.models.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

enum class HomeFilterChip { ALL, LOCAL, YOUTUBE, MIXES }

@Composable
fun AnimatedSection(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    onPlaySong: (Song) -> Unit,
    onPlayYouTubeSong: (Song, List<Song>) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onShowFullPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSearchWithQuery: ((String) -> Unit)? = null,
    onNavigateToLibrary: (String) -> Unit = {},
    onAddSongsToQueue: (List<Song>) -> Unit = {},
    onAddYouTubeSongsToQueue: (List<Song>) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onOpenCategory: (CategoryType) -> Unit = {},
    onPlayNext: (Song) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(HomeFilterChip.ALL) }

    val ytState = youTubeViewModel.home
    val exploreState = youTubeViewModel.exploreState
    val ytPage = (ytState as? HomeUiState.Success)?.page
    LaunchedEffect(Unit) {
        if (ytState is HomeUiState.Idle || ytState is HomeUiState.Error) youTubeViewModel.loadHome()
    }

    val quickPicks by youTubeViewModel.quickPicks.collectAsState()
    val dailyDiscover by youTubeViewModel.dailyDiscover.collectAsState()
    val similarRecommendations by youTubeViewModel.similarRecommendations.collectAsState()
    val communityPlaylists by youTubeViewModel.communityPlaylists.collectAsState()

    val allSongs = libraryViewModel.allSongs
    val recentSongs = remember(allSongs) { allSongs.filter { it.lastPlayed > 0L }.sortedByDescending { it.lastPlayed }.take(10) }
    val topSongs = remember(allSongs) { allSongs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(15) }
    val dailyMixes by produceState(
        initialValue = emptyList<Mix>(),
        allSongs, topSongs, ytPage
    ) {
        value = withContext(Dispatchers.Default) { generateDailyMixes(allSongs, topSongs, ytPage) }
    }
    val topArtists = remember(allSongs) { allSongs.filter { it.playCount > 0 }.groupBy { it.artist }.map { (artist, songs) -> Triple(artist, songs.sumOf { it.playCount }, songs.firstOrNull { it.coverUrl != null }) }.sortedByDescending { it.second }.take(10) }

    val chips = ytPage?.chips
    var selectedYtChipIndex by remember { mutableIntStateOf(0) }
    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val isRandomize = youTubeViewModel.randomizeHomeOrder
    var contextMenuSong by remember { mutableStateOf<SongItem?>(null) }

    val mergedSongs = remember(quickPicks, dailyDiscover, similarRecommendations) {
        (quickPicks +
            dailyDiscover.map { it.recommendation } +
            similarRecommendations.flatMap { it.items }.filterIsInstance<SongItem>()
        ).distinctBy { it.id }.take(30)
    }
    val mergedSongsConverted = remember(mergedSongs) { mergedSongs.map { it.toSong() } }
    val mergedPlaylists = remember(communityPlaylists, similarRecommendations) {
        (communityPlaylists.map { it.playlist } +
            similarRecommendations.flatMap { it.items }.filterIsInstance<PlaylistItem>()
        ).distinctBy { it.id }.take(15)
    }
    val shuffledSections = remember(ytPage, randomSeed, isRandomize) {
        if (ytPage != null) {
            if (isRandomize && ytPage.sections.size > 2) {
                val quickPicksIdx = ytPage.sections.indexOfFirst { YouTubeHomeSections.isQuickPicks(it.title) }
                val parts = ytPage.sections.withIndex().partition { it.index == quickPicksIdx }
                val qp = parts.first.map { it.value }
                val rest = parts.second.map { it.value }
                qp + rest.shuffled(kotlin.random.Random(randomSeed))
            } else {
                ytPage.sections
            }
        } else emptyList()
    }
    val sections = remember(ytPage) {
        ytPage?.sections?.foldIndexed(mutableListOf<Pair<Int, SectionKind>>()) { idx, acc, section ->
            val songs = section.items.filterIsInstance<SongItem>()
            val albums = section.items.filterIsInstance<AlbumItem>()
            val artists = section.items.filterIsInstance<ArtistItem>()
            val playlists = section.items.filterIsInstance<PlaylistItem>()
            val kind = when {
                songs.isNotEmpty() && YouTubeHomeSections.isQuickPicks(section.title) -> SectionKind.QuickPicks
                songs.isNotEmpty() && YouTubeHomeSections.isListenAgain(section.title) -> SectionKind.Songs
                songs.isNotEmpty() && YouTubeHomeSections.isForgottenFavorites(section.title) -> SectionKind.Songs
                playlists.isNotEmpty() && YouTubeHomeSections.isRecommendedPlaylists(section.title) -> SectionKind.Playlists
                playlists.isNotEmpty() && YouTubeHomeSections.isCommunityPlaylists(section.title) -> SectionKind.CommunityPlaylists
                artists.isNotEmpty() && YouTubeHomeSections.isRecommendedArtists(section.title) -> SectionKind.RecommendedArtists
                albums.isNotEmpty() && YouTubeHomeSections.isNewReleases(section.title) -> SectionKind.Albums
                songs.isNotEmpty() -> SectionKind.Songs
                albums.isNotEmpty() -> SectionKind.Albums
                artists.isNotEmpty() -> SectionKind.Artists
                playlists.isNotEmpty() -> SectionKind.Playlists
                else -> SectionKind.Mixed
            }
            acc.add(idx to kind)
            acc
        }?.toList() ?: emptyList()
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore, selectedFilter) {
        if (shouldLoadMore && selectedFilter in listOf(HomeFilterChip.ALL, HomeFilterChip.YOUTUBE)) {
            youTubeViewModel.loadMoreHome()
        }
    }

    @Composable
    fun FilterChipPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }

    val networkStatus by youTubeViewModel.networkStatus.collectAsState()

    PullToRefreshBox(
        isRefreshing = libraryViewModel.isScanning || ytState is HomeUiState.Loading,
        onRefresh = { 
            youTubeViewModel.refreshHome()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            
            item {
                HomeHeader()
            }

            if (libraryViewModel.isOfflineMode) {
                item {
                    TravelModeBanner()
                }
            } else if (networkStatus == com.fxzmusic.app.util.NetworkStatus.DISCONNECTED) {
                item {
                    OfflineBanner()
                }
            }
            
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FilterChipPill("Todo", selectedFilter == HomeFilterChip.ALL) { selectedFilter = HomeFilterChip.ALL } }
                    item { FilterChipPill("Local", selectedFilter == HomeFilterChip.LOCAL) { selectedFilter = HomeFilterChip.LOCAL } }
                    item { FilterChipPill("YouTube", selectedFilter == HomeFilterChip.YOUTUBE) { selectedFilter = HomeFilterChip.YOUTUBE } }
                    item { FilterChipPill("Mixes", selectedFilter == HomeFilterChip.MIXES) { selectedFilter = HomeFilterChip.MIXES } }
                }
            }

            if (selectedFilter in listOf(HomeFilterChip.ALL, HomeFilterChip.YOUTUBE)) {
                if (similarRecommendations.isNotEmpty()) {
                    similarRecommendations.forEachIndexed { simIndex, simRec ->
                        if (simRec.items.isNotEmpty()) {
                            val simSongs = simRec.items.filterIsInstance<SongItem>()
                            val convertedSimSongs = simSongs.map { it.toSong() }
                            item(key = "because_listened_hdr_${simRec.seedTitle}_$simIndex") {
                                YouTubeSectionHeaderMejorado(
                                    title = stringResource(R.string.because_you_listened_to, simRec.seedTitle)
                                )
                            }
                            item(key = "because_listened_row_${simRec.seedTitle}_$simIndex") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(simRec.items, key = { "sim_${simIndex}_${it.id}" }) { item ->
                                        when (item) {
                                            is SongItem -> {
                                                YouTubeSongCard(
                                                    song = item,
                                                    onClick = { onPlayYouTubeSong(item.toSong(), convertedSimSongs) },
                                                    onLongClick = { contextMenuSong = item }
                                                )
                                            }
                                            is AlbumItem -> {
                                                YouTubeAlbumCard(album = item, onClick = { onOpenAlbum(item.browseId) })
                                            }
                                            is ArtistItem -> {
                                                YouTubeArtistCard(artist = item, onClick = { onOpenArtist(item.id) })
                                            }
                                            is PlaylistItem -> {
                                                YouTubePlaylistCard(playlist = item, onClick = { onOpenPlaylist(item.id) })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (mergedSongs.isNotEmpty()) {
                    item {
                        YouTubeSectionHeaderMejorado(title = stringResource(R.string.because_you_listened))
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(mergedSongs, key = { "rec_${it.id}" }) { song ->
                                YouTubeSongCard(
                                    song = song,
                                    onClick = { onPlayYouTubeSong(song.toSong(), mergedSongsConverted) },
                                    onLongClick = { contextMenuSong = song }
                                )
                            }
                        }
                    }
                }

                if (mergedPlaylists.isNotEmpty()) {
                    item {
                        YouTubeSectionHeaderMejorado(title = stringResource(R.string.android_auto_recommendations))
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(mergedPlaylists, key = { "rpl_${it.id}" }) { playlist ->
                                YouTubePlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                            }
                        }
                    }
                }

                if (ytState is HomeUiState.Loading || ytState is HomeUiState.Idle) {
                    item { AppShimmerSkeleton() }
                }
                if (ytState is HomeUiState.Error) {
                    item {
                        val offlineMessage = if (ytState.message == "Sin conexión a Internet")
                            stringResource(R.string.home_error_offline) else null
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                offlineMessage ?: stringResource(R.string.home_error_generic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { youTubeViewModel.loadHome() }) {
                                Text(stringResource(R.string.home_retry))
                            }
                        }
                    }
                }

                itemsIndexed(shuffledSections, key = { index, section -> "yt_section_${section.title}_$index" }, contentType = { _, _ -> "yt_section" }) { sectionIndex, section ->
                    Column(modifier = Modifier.padding(top = if (sectionIndex == 0) 16.dp else 24.dp)) {
                        SectionContent(
                            section = section,
                            kind = sections.getOrNull(sectionIndex)?.second ?: SectionKind.Mixed,
                            onPlaySong = onPlayYouTubeSong,
                            onOpenAlbum = onOpenAlbum,
                            onOpenArtist = onOpenArtist,
                            onOpenPlaylist = onOpenPlaylist,
                            onSongLongClick = { contextMenuSong = it },
                        )
                    }
                }
            }

            if (selectedFilter in listOf(HomeFilterChip.ALL, HomeFilterChip.LOCAL)) {
                if (recentSongs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Reproducido recientemente (Local)", onAction = { onNavigateToLibrary("Todo") })
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            itemsIndexed(recentSongs, key = { _, song -> "rec_${song.id}" }, contentType = { _, _ -> "local_row" }) { _, song ->
                                Box(Modifier.animateItem()) {
                                    SongCard(song = song, onClick = { onPlaySong(song); onShowFullPlayer() })
                                }
                            }
                        }
                    }
                }
            }
            if (selectedFilter in listOf(HomeFilterChip.ALL, HomeFilterChip.LOCAL, HomeFilterChip.MIXES)) {
                if (dailyMixes.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.Bottom) {
                            Text("TUS MIXES DIARIOS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        }
                    }
                    items(dailyMixes, key = { "mix_${it.name}" }, contentType = { "mix" }) { mix ->
                        DailyMixRow(
                            title = mix.name,
                            description = mix.description,
                            songList = mix.songs,
                            coverColors = mix.coverColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onPlayMix = {
                                val mixPlaylist = Playlist(-(100L + dailyMixes.indexOf(mix)), mix.name, mix.songs.size, mix.coverColor, "", mix.description, mix.songs)
                                onPlayPlaylist(mixPlaylist)
                                onShowFullPlayer()
                            },
                            onAddSongsToQueue = onAddSongsToQueue
                        )
                    }
                }
            }

            if (selectedFilter in listOf(HomeFilterChip.ALL, HomeFilterChip.LOCAL)) {
                if (topArtists.isNotEmpty()) {
                    item {
                        AnimatedSection(200) { SectionHeader(title = "Artistas locales", onAction = { onNavigateToLibrary("Artistas") }) }
                    }
                    item {
                        AnimatedSection(250) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                itemsIndexed(topArtists, key = { _, t -> "art_${t.first}" }, contentType = { _, _ -> "local_row" }) { _, (artist, _, cover) ->
                                    Box(Modifier.animateItem()) {
                                        ArtistCard(name = artist, coverUrl = cover?.coverUrl, albumArt = cover?.albumArt ?: listOf(Color.Gray, Color.Gray), onClick = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedFilter in listOf(HomeFilterChip.ALL, HomeFilterChip.YOUTUBE)) {
                val explore = exploreState as? ExploreUiState.Success
                if (explore != null && explore.moods.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 20.dp)) {
                            YouTubeSectionHeaderMejorado(title = "Estados de ánimo y géneros (YouTube)")
                            YouTubeMoodGenresGrid(
                                items = explore.moods,
                                onMoodClick = { onOpenCategory(CategoryType.GENRES) },
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
        val downloads by downloadUtil.downloads.collectAsState()
        val videoId = currentSong.id
        val isDownloaded = downloads[videoId]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
        YouTubeSongContextMenu(
            title = currentSong.title,
            artistName = currentSong.artists.joinToString(", ") { it.name },
            albumBrowseId = currentSong.album?.id,
            onPlayNext = { onPlayNext(currentSong.toSong()) },
            onAddToQueue = { onAddYouTubeSongsToQueue(listOf(currentSong.toSong())) },
            onViewArtist = { currentSong.artists.firstOrNull()?.id?.let { onOpenArtist(it) } },
            onViewAlbum = { currentSong.album?.id?.let { onOpenAlbum(it) } },
            onDownload = if (!isDownloaded) {{ downloadUtil.enqueue(videoId, currentSong.title, currentSong.artists.joinToString(", "){it.name}, currentSong.album?.name?:"", currentSong.thumbnail) }} else null,
            onDeleteDownload = if (isDownloaded) {{ downloadUtil.delete(videoId) }} else null,
            onDismiss = { contextMenuSong = null }
        )
    }
}

enum class SectionKind { QuickPicks, Songs, Albums, Artists, Playlists, CommunityPlaylists, RecommendedArtists, Mixed }

data class Mix(
    val name: String,
    val description: String,
    val coverColor: List<Color>,
    val songs: List<Song>
)

fun generateDailyMixes(allSongs: List<Song>, topSongs: List<Song>, ytPage: com.fxzmusic.innertube.pages.HomePage? = null): List<Mix> {
    val mixes = mutableListOf<Mix>()
    val ytSongItems: List<SongItem> = ytPage?.sections?.flatMap { sec -> sec.items.filterIsInstance<SongItem>() }.orEmpty()
    val ytSongs: List<Song> = ytSongItems.map { item -> item.toSong() }
    val combinedSongs: List<Song> = (allSongs + ytSongs).distinctBy { it.id }

    if (combinedSongs.isEmpty()) return emptyList()

    val favoriteSongs = if (topSongs.isNotEmpty()) topSongs else combinedSongs.filter { it.isLiked || it.playCount > 0 }.ifEmpty { combinedSongs.take(20) }
    if (favoriteSongs.isNotEmpty()) {
        mixes.add(
            Mix(
                name = "Mix Mis Favoritos",
                description = "Tus canciones favoritas y sugerencias",
                coverColor = listOf(Color(0xFFE91E63), Color(0xFF9C27B0)),
                songs = favoriteSongs.take(20)
            )
        )
    }

    val topArtists = combinedSongs
        .groupBy { it.artist }
        .filterKeys { it.isNotBlank() && it != "Unknown Artist" && it != "<unknown>" && it != "Artista desconocido" }
        .mapValues { entry -> entry.value.sortedByDescending { it.playCount } }
        .entries
        .sortedByDescending { entry -> entry.value.size }

    val gradients = listOf(
        listOf(Color(0xFF673AB7), Color(0xFF512DA8)),
        listOf(Color(0xFF009688), Color(0xFF00796B)),
        listOf(Color(0xFFFF5722), Color(0xFFE64A19)),
        listOf(Color(0xFF3F51B5), Color(0xFF303F9F)),
        listOf(Color(0xFF4CAF50), Color(0xFF388E3C)),
        listOf(Color(0xFFFF9800), Color(0xFFF57C00))
    )

    var colorIndex = 0
    topArtists.take(4).forEach { (artistName, songsByArtist) ->
        val otherSongs = combinedSongs.filter { it.artist != artistName }.shuffled()
        val mixSongs = (songsByArtist + otherSongs).take(20)
        mixes.add(
            Mix(
                name = "Mix de $artistName",
                description = "Canciones de $artistName y similares",
                coverColor = gradients[colorIndex % gradients.size],
                songs = mixSongs
            )
        )
        colorIndex++
    }

    val recentOrShuffled = combinedSongs.filter { it.lastPlayed > 0L }.sortedByDescending { it.lastPlayed }.ifEmpty { combinedSongs.shuffled() }
    if (recentOrShuffled.isNotEmpty()) {
        mixes.add(
            Mix(
                name = "Mix Descubrimientos",
                description = "Basado en tu actividad y recomendaciones",
                coverColor = listOf(Color(0xFF2196F3), Color(0xFF00BCD4)),
                songs = recentOrShuffled.take(20)
            )
        )
    }

    return mixes
}

object YouTubeHomeSections {
    fun isQuickPicks(title: String?) = title?.contains("selecciones", ignoreCase = true) == true || title?.contains("picks", ignoreCase = true) == true
    fun isListenAgain(title: String?) = title?.contains("volver", ignoreCase = true) == true || title?.contains("again", ignoreCase = true) == true
    fun isForgottenFavorites(title: String?) = title?.contains("olvidados", ignoreCase = true) == true || title?.contains("forgotten", ignoreCase = true) == true
    fun isRecommendedPlaylists(title: String?) = title?.contains("playlists", ignoreCase = true) == true
    fun isCommunityPlaylists(title: String?) = title?.contains("comunidad", ignoreCase = true) == true
    fun isRecommendedArtists(title: String?) = title?.contains("artistas", ignoreCase = true) == true
    fun isNewReleases(title: String?) = title?.contains("nuevos", ignoreCase = true) == true || title?.contains("releases", ignoreCase = true) == true
}

@Composable
fun SectionContent(
    section: com.fxzmusic.innertube.pages.HomePage.Section,
    kind: SectionKind,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSongLongClick: (SongItem) -> Unit
) {
    Column {
        YouTubeSectionHeaderMejorado(title = section.title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(section.items, key = { "${it.javaClass.simpleName}_${it.id}" }) { item ->
                when (item) {
                    is SongItem -> {
                        YouTubeSongCard(
                            song = item,
                            onClick = { onPlaySong(item.toSong(), section.items.filterIsInstance<SongItem>().map { it.toSong() }) },
                            onLongClick = { onSongLongClick(item) }
                        )
                    }
                    is AlbumItem -> {
                        YouTubeAlbumCard(
                            album = item,
                            onClick = { onOpenAlbum(item.browseId) }
                        )
                    }
                    is ArtistItem -> {
                        YouTubeArtistCard(
                            artist = item,
                            onClick = { onOpenArtist(item.id) }
                        )
                    }
                    is PlaylistItem -> {
                        YouTubePlaylistCard(
                            playlist = item,
                            onClick = { onOpenPlaylist(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSearchBar(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onSearchWithQuery: ((String) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onSearchClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Buscar canciones, artistas...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 16.sp)
    }
}


