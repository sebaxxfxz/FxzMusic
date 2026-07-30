package com.fxzmusic.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.service.SyncUtils
import com.fxzmusic.app.service.YouTubeMusicRepository
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.AlbumItem
import com.fxzmusic.innertube.models.ArtistItem
import com.fxzmusic.innertube.models.BrowseEndpoint
import com.fxzmusic.innertube.models.PlaylistItem
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.models.WatchEndpoint
import com.fxzmusic.innertube.models.YTItem
import com.fxzmusic.innertube.models.filterExplicit
import com.fxzmusic.innertube.models.filterVideoSongs
import com.fxzmusic.innertube.models.filterYoutubeShorts
import com.fxzmusic.innertube.pages.AlbumPage
import com.fxzmusic.innertube.pages.ArtistPage
import com.fxzmusic.innertube.pages.ChartsPage
import com.fxzmusic.innertube.pages.ExplorePage
import com.fxzmusic.innertube.pages.HistoryPage
import com.fxzmusic.innertube.pages.HomePage
import com.fxzmusic.innertube.pages.MoodAndGenres
import com.fxzmusic.innertube.pages.PlaylistPage
import com.fxzmusic.innertube.pages.SearchResult
import com.fxzmusic.innertube.pages.SearchSummary
import com.fxzmusic.innertube.pages.SearchSummaryPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DailyDiscoverItem(
    val seedId: String,
    val seedTitle: String,
    val seedThumbnail: String?,
    val recommendation: SongItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class SimilarRecommendation(
    val seedTitle: String,
    val seedThumbnail: String?,
    val items: List<YTItem>
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

class YouTubeMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = YouTubeMusicRepository.get()
    private val db = FxzDatabase.getInstance(application)
    private val prefs = application.getSharedPreferences("yt_music_prefs", android.content.Context.MODE_PRIVATE)

    private var homeCache: Pair<HomePage, Long>? = null
    private val HOME_CACHE_TTL_MS = 15 * 60 * 1000L

    var home by mutableStateOf<HomeUiState>(HomeUiState.Idle)
        private set

    var search by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        private set

    var detail by mutableStateOf<DetailUiState>(DetailUiState.Idle)
        private set

    var chartsState  by mutableStateOf<ChartsUiState>(ChartsUiState.Idle)
        private set
    var exploreState by mutableStateOf<ExploreUiState>(ExploreUiState.Idle)
        private set
    var historyState by mutableStateOf<HistoryUiState>(HistoryUiState.Idle)
        private set

    var selectedChartType by mutableStateOf<com.fxzmusic.innertube.pages.ChartsPage.ChartType?>(null)
        private set
    private var chartsContinuation: String? = null
    private var isChartsLoadingMore = false

    var lastError by mutableStateOf<String?>(null)
        private set

    var randomizeHomeOrder by mutableStateOf(
        application.getSharedPreferences("yt_music_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("randomize_home_order", false)
    )
        private set

    private val _quickPicks = MutableStateFlow<List<SongItem>>(emptyList())
    val quickPicks: StateFlow<List<SongItem>> = _quickPicks.asStateFlow()

    private val _dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>>(emptyList())
    val dailyDiscover: StateFlow<List<DailyDiscoverItem>> = _dailyDiscover.asStateFlow()

    private val _similarRecommendations = MutableStateFlow<List<SimilarRecommendation>>(emptyList())
    val similarRecommendations: StateFlow<List<SimilarRecommendation>> = _similarRecommendations.asStateFlow()

    private val _communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>>(emptyList())
    val communityPlaylists: StateFlow<List<CommunityPlaylistItem>> = _communityPlaylists.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null
    private var liveSearchJob: Job? = null

    private var lastHomeParams: String? = null
    private var pendingContinuation: String? = null
    private var isHomeLoadingMore = false

    fun loadHome(params: String? = null) {
        val cached = homeCache
        if (cached != null) {
            home = HomeUiState.Success(cached.first)
            if (params == lastHomeParams && System.currentTimeMillis() - cached.second < HOME_CACHE_TTL_MS) {
                if (exploreState is ExploreUiState.Idle) loadExplore()
                return
            }
        } else {
            home = HomeUiState.Loading
        }

        lastHomeParams = params
        viewModelScope.launch(Dispatchers.IO) {
            val result = runSafely { repo.home(params = params) }
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { page ->
                        if (exploreState is ExploreUiState.Idle) loadExplore()
                        homeCache = page to System.currentTimeMillis()
                        pendingContinuation = page.continuation
                        home = HomeUiState.Success(page)
                    },
                    onFailure = { err ->
                        if (home !is HomeUiState.Success) {
                            home = HomeUiState.Error(err.message ?: "Error al cargar YouTube Music")
                        }
                    }
                )
            }
            loadRecommendationsAsync()
        }
    }

    private fun loadRecommendationsAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            coroutineScope {
                launch { getQuickPicks() }
                launch { getDailyDiscover() }
                launch { getSimilarRecommendations() }
                launch { getCommunityPlaylists() }
            }
        }
    }

    private suspend fun getQuickPicks() {
        val recentHistory = runCatching { db.playbackHistoryDao().getRecent(20) }.getOrDefault(emptyList())
        val recentYtIds = recentHistory.filter { it.songId.length == 11 && !it.songId.contains("/") }
        if (recentYtIds.isEmpty()) return
        val seed = recentYtIds.first()
        val endpoint = runCatching {
            YouTube.next(WatchEndpoint(videoId = seed.songId)).getOrNull()?.relatedEndpoint
        }.getOrNull() ?: return
        val page = runCatching { YouTube.related(endpoint).getOrNull() }.getOrNull() ?: return
        val picks = page.songs.take(20)
        if (picks.isNotEmpty()) _quickPicks.value = picks
    }

    private suspend fun getDailyDiscover() {
        val likedIds = runCatching { db.ytLikedSongDao().getAll().map { it.videoId } }.getOrDefault(emptyList())
        val seeds = likedIds.shuffled().take(5)
        if (seeds.isEmpty()) return
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())
        coroutineScope {
            seeds.map { videoId ->
                launch(Dispatchers.IO) {
                    val endpoint = runCatching {
                        YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()?.relatedEndpoint
                    }.getOrNull() ?: return@launch
                    runCatching {
                        YouTube.related(endpoint).onSuccess { related ->
                            val rec = related.songs.shuffled().firstOrNull { it.id != videoId } ?: return@onSuccess
                            val likedSong = db.ytLikedSongDao().getAll().firstOrNull { it.videoId == videoId }
                            items.add(
                                DailyDiscoverItem(
                                    seedId = videoId,
                                    seedTitle = likedSong?.title ?: videoId,
                                    seedThumbnail = likedSong?.thumbnail,
                                    recommendation = rec,
                                    relatedEndpoint = endpoint
                                )
                            )
                        }
                    }
                }
            }.forEach { it.join() }
        }
        val result = items.distinctBy { it.recommendation.id }.shuffled()
        if (result.isNotEmpty()) _dailyDiscover.value = result
    }

    private suspend fun getSimilarRecommendations() {
        val stats = runCatching { db.songStatsDao().getAll().sortedByDescending { it.playCount }.take(10) }.getOrDefault(emptyList())
        val ytSeeds = stats.filter { it.songId.length == 11 && !it.songId.contains("/") }.take(5)
        val artistSeeds = runCatching { db.ytArtistDao().getAll().take(4) }.getOrDefault(emptyList())
        if (ytSeeds.isEmpty() && artistSeeds.isEmpty()) return
        coroutineScope {
            val songDeferreds = ytSeeds.map { stat ->
                async(Dispatchers.IO) {
                    val endpoint = runCatching {
                        YouTube.next(WatchEndpoint(videoId = stat.songId)).getOrNull()?.relatedEndpoint
                    }.getOrNull() ?: return@async null
                    val page = runCatching { YouTube.related(endpoint).getOrNull() }.getOrNull() ?: return@async null
                    val related: List<YTItem> = (page.songs.shuffled().take(8) + page.albums.shuffled().take(3) + page.artists.shuffled().take(2)).distinctBy { it.id }
                    if (related.isEmpty()) return@async null
                    SimilarRecommendation(seedTitle = stat.title, seedThumbnail = stat.coverUrl, items = related)
                }
            }
            val artistDeferreds = artistSeeds.map { artist ->
                async(Dispatchers.IO) {
                    val page = runCatching { YouTube.artist(artist.channelId).getOrNull() }.getOrNull() ?: return@async null
                    val related: List<YTItem> = page.sections.takeLast(3).flatMap { it.items }.distinctBy { it.id }.shuffled().take(12)
                    if (related.isEmpty()) return@async null
                    SimilarRecommendation(seedTitle = artist.name, seedThumbnail = artist.thumbnail, items = related)
                }
            }
            val results = (songDeferreds + artistDeferreds).awaitAll().filterNotNull().shuffled()
            if (results.isNotEmpty()) _similarRecommendations.value = results
        }
    }

    private suspend fun getCommunityPlaylists() {
        val artistSeeds = runCatching { db.ytArtistDao().getAll().shuffled().take(3) }.getOrDefault(emptyList())
        val songSeeds = runCatching { db.songStatsDao().getAll().sortedByDescending { it.playCount }.take(5) }.getOrDefault(emptyList())
        val ytSongSeeds = songSeeds.filter { it.songId.length == 11 && !it.songId.contains("/") }.take(3)
        val candidates = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())
        coroutineScope {
            artistSeeds.map { artist ->
                launch(Dispatchers.IO) {
                    runCatching {
                        YouTube.artist(artist.channelId).onSuccess { page ->
                            page.sections.forEach { section ->
                                section.items.filterIsInstance<PlaylistItem>().forEach { pl ->
                                    if (pl.author?.name != "YouTube Music" && pl.author?.name != "YouTube" && !pl.id.startsWith("RD") && !pl.id.startsWith("OLAK"))
                                        candidates.add(pl)
                                }
                            }
                        }
                    }
                }
            }
            ytSongSeeds.map { stat ->
                launch(Dispatchers.IO) {
                    runCatching {
                        val endpoint = YouTube.next(WatchEndpoint(videoId = stat.songId)).getOrNull()?.relatedEndpoint ?: return@launch
                        YouTube.related(endpoint).onSuccess { page ->
                            page.playlists.forEach { pl ->
                                if (pl.author?.name != "YouTube Music" && pl.author?.name != "YouTube" && !pl.id.startsWith("RD"))
                                    candidates.add(pl)
                            }
                        }
                    }
                }
            }
        }
        val unique = candidates.distinctBy { it.id }.shuffled().take(5)
        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())
        coroutineScope {
            unique.map { pl ->
                launch(Dispatchers.IO) {
                    runCatching {
                        YouTube.playlist(pl.id).onSuccess { page ->
                            val songs = page.songs.take(10)
                            if (songs.isNotEmpty()) playlists.add(CommunityPlaylistItem(pl, songs))
                        }
                    }
                }
            }.forEach { it.join() }
        }
        if (playlists.isNotEmpty()) _communityPlaylists.value = playlists.shuffled()
    }

    fun loadMoreHome() {
        if (isHomeLoadingMore) return
        val continuation = pendingContinuation ?: return
        val current = home as? HomeUiState.Success ?: return
        isHomeLoadingMore = true
        viewModelScope.launch {
            runSafely { repo.home(continuation = continuation) }
                .fold(
                    onSuccess = { newPage ->
                        pendingContinuation = newPage.continuation
                        val merged = current.page.copy(
                            sections = current.page.sections + newPage.sections
                        )
                        home = HomeUiState.Success(merged)
                        homeCache = merged to System.currentTimeMillis()
                    },
                    onFailure = {  },
                )
            isHomeLoadingMore = false
        }
    }

    fun isHomeLoadingMore(): Boolean = isHomeLoadingMore

    fun refreshHome() {
        homeCache = null
        pendingContinuation = null
        exploreState = ExploreUiState.Idle
        _quickPicks.value = emptyList()
        _dailyDiscover.value = emptyList()
        _similarRecommendations.value = emptyList()
        _communityPlaylists.value = emptyList()
        loadHome(lastHomeParams)
    }

    fun toggleRandomizeHomeOrder() {
        randomizeHomeOrder = !randomizeHomeOrder
        prefs.edit().putBoolean("randomize_home_order", randomizeHomeOrder).apply()
        homeCache = null
        loadHome(lastHomeParams)
    }

    fun loadCharts() {
        if (chartsState is ChartsUiState.Loading) return
        chartsState = ChartsUiState.Loading
        chartsContinuation = null
        isChartsLoadingMore = false
        viewModelScope.launch {
            chartsState = runSafely { repo.charts() }.fold(
                onSuccess = { page ->
                    chartsContinuation = page.continuation
                    ChartsUiState.Success(page)
                },
                onFailure = { ChartsUiState.Error(it.message ?: "Sin conexión. Intenta más tarde") }
            )
        }
    }

    fun loadMoreCharts() {
        val cont = chartsContinuation ?: return
        if (isChartsLoadingMore) return
        isChartsLoadingMore = true
        viewModelScope.launch {
            runSafely {
                com.fxzmusic.innertube.YouTube.getChartsPage(cont)
            }.fold(
                onSuccess = { newPage ->
                    chartsContinuation = newPage.continuation
                    val current = (chartsState as? ChartsUiState.Success)?.page
                    if (current != null) {
                        val merged = current.copy(
                            sections = current.sections + newPage.sections,
                            continuation = newPage.continuation,
                        )
                        chartsState = ChartsUiState.Success(merged)
                    }
                    isChartsLoadingMore = false
                },
                onFailure = { isChartsLoadingMore = false }
            )
        }
    }

    fun setChartType(type: com.fxzmusic.innertube.pages.ChartsPage.ChartType?) {
        selectedChartType = type
    }

    fun loadExplore() {
        if (exploreState is ExploreUiState.Loading) return
        exploreState = ExploreUiState.Loading
        viewModelScope.launch {
            exploreState = runSafely { repo.explore() }.fold(
                onSuccess = { page ->
                    ExploreUiState.Success(
                        albums = page.newReleaseAlbums,
                        moods = page.moodAndGenres
                    )
                },
                onFailure = { ExploreUiState.Error(it.message ?: "No se pudo cargar Explorar") }
            )
        }
    }

    fun loadHistory() {
        if (historyState is HistoryUiState.Loading) return
        historyState = HistoryUiState.Loading
        viewModelScope.launch {
            historyState = runSafely { repo.history() }.fold(
                onSuccess = { page ->
                    val sections = page.sections ?: emptyList()
                    HistoryUiState.Success(sections)
                },
                onFailure = { HistoryUiState.Error(it.message ?: "No se pudo cargar el historial") }
            )
        }
    }

    fun updateQuery(raw: String, filter: YouTube.SearchFilter? = null) {
        search = (search as? SearchUiState.Suggestions)
            ?.copy(query = raw)
            ?: SearchUiState.Suggestions(query = raw, items = emptyList())
        scheduleAutocompleteAndLiveSearch(raw, filter)
    }

    private fun scheduleAutocompleteAndLiveSearch(query: String, filter: YouTube.SearchFilter?) {
        suggestionsJob?.cancel()
        liveSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            search = SearchUiState.Idle
            return
        }

        suggestionsJob = viewModelScope.launch {
            delay(180)
            val items = runSafely { repo.searchSuggestions(trimmed) }.getOrDefault(emptyList())
            if (search is SearchUiState.Suggestions) {
                search = (search as SearchUiState.Suggestions).copy(items = items)
            }
        }

        if (trimmed.length >= 2) {
            liveSearchJob = viewModelScope.launch {
                delay(380)
                runSearchInternal(trimmed, filter, saveHistory = false)
            }
        }
    }

    fun runSearch(
        query: String,
        filter: YouTube.SearchFilter? = null,
    ) {
        runSearchInternal(query, filter, saveHistory = true)
    }

    private fun runSearchInternal(
        query: String,
        filter: YouTube.SearchFilter?,
        saveHistory: Boolean
    ) {
        searchJob?.cancel()
        liveSearchJob?.cancel()
        if (query.isBlank()) { search = SearchUiState.Idle; return }
        if (saveHistory) saveSearchQuery(query)
        if (search !is SearchUiState.Success || (search as SearchUiState.Success).query != query || (search as SearchUiState.Success).filter != filter) {
            search = SearchUiState.Loading
        }
        searchJob = viewModelScope.launch {
            if (filter == null) {
                search = runSafely { repo.searchSummary(query) }
                    .fold(
                        onSuccess = { page ->
                            val filtered = applySummaryFilters(page)
                            val flat = filtered.summaries.flatMap { it.items }
                            SearchUiState.Success(
                                query = query,
                                filter = null,
                                items = flat,
                                summaries = filtered.summaries,
                                continuation = null,
                            )
                        },
                        onFailure = { SearchUiState.Error(it.message ?: "Sin resultados") },
                    )
            } else {
                search = runSafely { repo.search(query, filter) }
                    .fold(
                        onSuccess = { result ->
                            val items = applyListFilters(repo.flattenSearch(result))
                            SearchUiState.Success(
                                query = query,
                                filter = filter,
                                items = items,
                                summaries = null,
                                continuation = result.continuation,
                            )
                        },
                        onFailure = { SearchUiState.Error(it.message ?: "Sin resultados") },
                    )
            }
        }
    }

    fun loadMoreSearch() {
        val current = search as? SearchUiState.Success ?: return
        if (current.isLoadingMore) return
        val filter = current.filter
        if (filter == null) return 
        val continuation = current.continuation ?: return
        search = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runSafely { repo.searchContinuation(continuation) }
                .fold(
                    onSuccess = { page: SearchResult ->
                        val more = applyListFilters(repo.flattenSearch(page))
                        if (more.isEmpty()) {
                            search = current.copy(isLoadingMore = false, continuation = null)
                            return@fold
                        }
                        val mergedItems = (current.items + more).distinctBy { it.id }
                        search = current.copy(
                            items = mergedItems,
                            continuation = (page as SearchResult).continuation,
                            isLoadingMore = false,
                        )
                    },
                    onFailure = { search = current.copy(isLoadingMore = false) },
                )
        }
    }

    private fun applyListFilters(items: List<YTItem>): List<YTItem> {
        var out = items
        out = out.filterExplicit(hideExplicit)
        out = out.filterVideoSongs(hideVideoSongs)
        out = out.filterYoutubeShorts(hideShorts)
        return out
    }

    private fun applySummaryFilters(page: SearchSummaryPage): SearchSummaryPage {
        var out = page
        out = out.filterExplicit(hideExplicit)
        out = out.filterVideoSongs(hideVideoSongs)
        out = out.filterYoutubeShorts(hideShorts)
        return out
    }

    var hideExplicit by mutableStateOf(prefs.getBoolean("hide_explicit_search", false))
        private set
    var hideVideoSongs by mutableStateOf(prefs.getBoolean("hide_video_search", false))
        private set
    var hideShorts by mutableStateOf(prefs.getBoolean("hide_shorts_search", false))
        private set

    fun toggleHideExplicit() {
        hideExplicit = !hideExplicit
        prefs.edit().putBoolean("hide_explicit_search", hideExplicit).apply()
        refreshCurrentSearchFilters()
    }

    fun toggleHideVideoSongs() {
        hideVideoSongs = !hideVideoSongs
        prefs.edit().putBoolean("hide_video_search", hideVideoSongs).apply()
        refreshCurrentSearchFilters()
    }

    fun toggleHideShorts() {
        hideShorts = !hideShorts
        prefs.edit().putBoolean("hide_shorts_search", hideShorts).apply()
        refreshCurrentSearchFilters()
    }

    private fun refreshCurrentSearchFilters() {
        val current = search as? SearchUiState.Success ?: return
        search = current.copy(items = applyListFilters(current.items))
    }

    private fun saveSearchQuery(query: String) {
        val current = prefs.getStringSet("search_history", emptySet()).orEmpty().toMutableSet()
        current.remove(query)
        current.add(query)
        if (current.size > 50) {
            val sorted = current.toList().sortedBy { prefs.getLong("query_ts_$it", 0L) }
            sorted.take(current.size - 50).forEach { current.remove(it); prefs.edit().remove("query_ts_$it").apply() }
        }
        prefs.edit()
            .putStringSet("search_history", current)
            .putLong("query_ts_$query", System.currentTimeMillis())
            .apply()
    }

    fun getRecentSearches(): List<String> {
        val queries = prefs.getStringSet("search_history", emptySet()).orEmpty()
        return queries.sortedByDescending { prefs.getLong("query_ts_$it", 0L) }
    }

    fun clearSearchHistory() {
        prefs.edit()
            .remove("search_history")
            .apply()
        val keys = prefs.all.keys.filter { it.startsWith("query_ts_") }
        keys.forEach { prefs.edit().remove(it).apply() }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        search = SearchUiState.Idle
    }

    fun openAlbum(browseId: String) {
        detail = DetailUiState.Loading
        viewModelScope.launch {
            detail = runSafely { repo.album(browseId) }.fold(
                onSuccess = { DetailUiState.Album(it) },
                onFailure = { DetailUiState.Error(it.message ?: "No se pudo abrir el album") },
            )
        }
    }

    fun openPlaylist(playlistId: String) {
        detail = DetailUiState.Loading
        viewModelScope.launch {
            detail = runSafely { repo.playlist(playlistId) }.fold(
                onSuccess = { DetailUiState.Playlist(it) },
                onFailure = { DetailUiState.Error(it.message ?: "No se pudo abrir la playlist") },
            )
        }
    }

    fun openArtist(browseId: String) {
        detail = DetailUiState.Loading
        viewModelScope.launch {
            detail = runSafely { repo.artist(browseId) }.fold(
                onSuccess = { DetailUiState.Artist(it) },
                onFailure = { DetailUiState.Error(it.message ?: "No se pudo abrir el artista") },
            )
        }
    }

    fun clearDetail() { detail = DetailUiState.Idle }

    suspend fun resolveStreamUrl(videoId: String): String? {
        val ctx: Context = getApplication()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: run { lastError = "Sin ConnectivityManager"; return null }
        val result = runSafely {
            repo.resolveStreamUrl(
                videoId = videoId,
                context = ctx,
                connectivityManager = cm,
            )
        }
        if (result.isFailure) lastError = result.exceptionOrNull()?.message ?: "Error de stream"
        return result.getOrNull()
    }

    fun getSyncState(): SyncUtils = SyncUtils.get(getApplication())

    private var syncJob: Job? = null

    fun runSync() {
        if (syncJob?.isActive == true) return
        val ctx: Context = getApplication()
        syncJob = viewModelScope.launch {
            SyncUtils.get(ctx).fullSync()
        }
    }

    fun syncLikedSongs() {
        if (syncJob?.isActive == true) return
        val ctx: Context = getApplication()
        syncJob = viewModelScope.launch {
            SyncUtils.get(ctx).syncLikedSongs()
        }
    }

    fun syncAlbums() {
        if (syncJob?.isActive == true) return
        val ctx: Context = getApplication()
        syncJob = viewModelScope.launch {
            SyncUtils.get(ctx).syncAlbums()
        }
    }

    fun syncArtists() {
        if (syncJob?.isActive == true) return
        val ctx: Context = getApplication()
        syncJob = viewModelScope.launch {
            SyncUtils.get(ctx).syncArtists()
        }
    }

    fun syncPlaylists() {
        if (syncJob?.isActive == true) return
        val ctx: Context = getApplication()
        syncJob = viewModelScope.launch {
            SyncUtils.get(ctx).syncPlaylists()
        }
    }

    private suspend inline fun <T> runSafely(crossinline block: suspend () -> Result<T>): Result<T> =
        try { block() } catch (t: Throwable) { Result.failure(t) }

    fun reset() {
        home = HomeUiState.Idle
        search = SearchUiState.Idle
        detail = DetailUiState.Idle
        chartsState = ChartsUiState.Idle
        exploreState = ExploreUiState.Idle
        historyState = HistoryUiState.Idle
        lastError = null
        searchJob?.cancel()
        suggestionsJob?.cancel()
    }
}

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Success(val page: HomePage) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Suggestions(val query: String, val items: List<String>) : SearchUiState
    data class Success(
        val query: String,
        val filter: YouTube.SearchFilter?,
        val items: List<YTItem>,
        val summaries: List<SearchSummary>? = null,
        val continuation: String? = null,
        val isLoadingMore: Boolean = false,
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface DetailUiState {
    data object Idle : DetailUiState
    data object Loading : DetailUiState
    data class Album(val page: AlbumPage) : DetailUiState
    data class Artist(val page: ArtistPage) : DetailUiState
    data class Playlist(val page: PlaylistPage) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

sealed interface ChartsUiState {
    data object Idle : ChartsUiState
    data object Loading : ChartsUiState
    data class Success(val page: ChartsPage) : ChartsUiState
    data class Error(val message: String) : ChartsUiState
}

sealed interface ExploreUiState {
    data object Idle : ExploreUiState
    data object Loading : ExploreUiState
    data class Success(
        val albums: List<com.fxzmusic.innertube.models.AlbumItem>,
        val moods: List<MoodAndGenres.Item>
    ) : ExploreUiState
    data class Error(val message: String) : ExploreUiState
}

sealed interface HistoryUiState {
    data object Idle : HistoryUiState
    data object Loading : HistoryUiState
    data class Success(val sections: List<com.fxzmusic.innertube.pages.HistoryPage.HistorySection>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}
