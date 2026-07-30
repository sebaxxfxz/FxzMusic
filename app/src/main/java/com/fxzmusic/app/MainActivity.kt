package com.fxzmusic.app

import com.fxzmusic.app.data.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.viewmodel.*
import com.fxzmusic.app.util.*
import com.fxzmusic.app.ui.screens.*
import com.fxzmusic.app.ui.components.*
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fxzmusic.app.ui.theme.FxzMusicTheme
import com.fxzmusic.app.ui.theme.LocalFxzTheme
import com.fxzmusic.app.ui.theme.ACCENT_COLORS
import com.fxzmusic.app.navigation.LibraryNavHost
import com.fxzmusic.app.navigation.YouTubeNavHost
import com.fxzmusic.app.navigation.AlbumRoute
import com.fxzmusic.app.navigation.ArtistRoute
import com.fxzmusic.app.navigation.DownloadsRoute
import com.fxzmusic.app.navigation.FolderRoute
import com.fxzmusic.app.navigation.FavoritesRoute
import com.fxzmusic.app.navigation.PlaylistRoute
import com.fxzmusic.app.navigation.YouTubeAlbumRoute
import com.fxzmusic.app.navigation.YouTubeArtistRoute
import com.fxzmusic.app.navigation.YouTubeCategoryRoute
import com.fxzmusic.app.navigation.YouTubePlaylistRoute
import com.fxzmusic.app.navigation.YouTubeSearchRoute
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.PlayCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionsToRequest = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            ViewModelProvider(this)[LibraryViewModel::class.java].hasPermission = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        checkPermissionAndScan()
        setContent {
            val settingsViewModel: PlaybackSettingsViewModel = viewModel()
            val currentTheme = settingsViewModel.currentTheme()
            CompositionLocalProvider(LocalFxzTheme provides currentTheme) {
                FxzMusicTheme(theme = currentTheme) {
                    MainScreen(settingsViewModel = settingsViewModel)
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val display = display
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = maxMode.modeId
                    }
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = window.windowManager.defaultDisplay.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = maxMode.modeId
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not set high refresh rate: ${e.message}")
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopService(Intent(this, PlaybackService::class.java))
        }
        super.onDestroy()
    }

    private fun checkPermissionAndScan() {
        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missing.isEmpty()) {
            ViewModelProvider(this)[LibraryViewModel::class.java].hasPermission = true
        } else {
            requestPermissionLauncher.launch(missing)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    musicPlayerViewModel: MusicPlayerViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    equalizerViewModel: EqualizerViewModel = viewModel(),
    statsViewModel: StatsViewModel = viewModel(),
    settingsViewModel: PlaybackSettingsViewModel = viewModel(),
    fxViewModel: AudioFxViewModel = viewModel(),
    playerStateManager: com.fxzmusic.app.viewmodel.PlayerStateManager = viewModel(),
    youTubeViewModel: YouTubeMusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var activeUpdateInfo by remember { mutableStateOf<com.fxzmusic.app.util.UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        val result = com.fxzmusic.app.util.UpdateChecker.checkForUpdates(context)
        result.onSuccess { info ->
            if (info.isUpdateAvailable) {
                activeUpdateInfo = info
                com.fxzmusic.app.util.UpdateNotificationHelper.showUpdateNotification(context, info)
            }
        }
    }

    activeUpdateInfo?.let { info ->
        com.fxzmusic.app.ui.components.UpdateDialog(
            updateInfo = info,
            onDismiss = { activeUpdateInfo = null }
        )
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingSearchQuery by rememberSaveable { mutableStateOf("") }
    var activePlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var miniPlayerRevealToken by rememberSaveable { mutableIntStateOf(0) }
    val eqSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fxSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playbackDataStore = remember(context) { PlaylistPlaybackDataStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val miniPlayerShouldBeVisible by remember {
        androidx.compose.runtime.derivedStateOf {
            playerStateManager.showMiniPlayer && musicPlayerViewModel.currentSong != null && !playerStateManager.isFullPlayerVisible
        }
    }
    val latestMiniPlayerShouldBeVisible = rememberUpdatedState(miniPlayerShouldBeVisible)
    val shakeDetector = remember {
        ShakeDetector(onShake = {
            if (settingsViewModel.shakeToSkip && musicPlayerViewModel.isPlaying && musicPlayerViewModel.currentSong != null) {
                musicPlayerViewModel.playNext()
            }
        })
    }

    LaunchedEffect(Unit) {
        supervisorScope {
            launch(Dispatchers.IO) { equalizerViewModel.init(context) }
            launch(Dispatchers.IO) { statsViewModel.init(context) }
            launch(Dispatchers.IO) { libraryViewModel.initPrefs(context) }
            launch(Dispatchers.IO) { settingsViewModel.init(context) }
        }
        musicPlayerViewModel.initializePlayer(context)
    }

    LaunchedEffect(Unit) {
        com.fxzmusic.app.util.EventBus.events.collect { event ->
            when (event) {
                is com.fxzmusic.app.util.UiEvent.SongStatsChanged -> {
                    libraryViewModel.updateSongStats(event.songId, event.playCount, event.lastPlayed)
                }
                is com.fxzmusic.app.util.UiEvent.LikeChanged -> {
                    musicPlayerViewModel.updateCurrentSongLike(event.songId, event.isLiked)
                }
                is com.fxzmusic.app.util.UiEvent.PlaylistDeleted -> {
                    musicPlayerViewModel.stopSong()
                    playerStateManager.hideMiniPlayer() 
                    if (activePlaylistId == event.playlistId) activePlaylistId = null
                }
                is com.fxzmusic.app.util.UiEvent.BlacklistChanged -> {
                    libraryViewModel.updateBlacklist(event.blacklistedFolders)
                }
                is com.fxzmusic.app.util.UiEvent.SpeedRestored -> {}
                is com.fxzmusic.app.util.UiEvent.LibraryRefreshRequested -> {}
                is com.fxzmusic.app.util.UiEvent.TrackEnded -> {
                    if (settingsViewModel.sleepAfterTrack) {
                        musicPlayerViewModel.pauseIfPlaying()
                        settingsViewModel.sleepAfterTrack = false
                    }
                }
            }
        }
    }

    LaunchedEffect(libraryViewModel.hasPermission) {
        if (libraryViewModel.hasPermission && libraryViewModel.allSongs.isEmpty()) {
            libraryViewModel.scanLocalMusic(context)
        }
    }

    LaunchedEffect(libraryViewModel.allSongs) {
        if (libraryViewModel.allSongs.isNotEmpty()) {
            delay(500)
            musicPlayerViewModel.restoreWithSongs(libraryViewModel.allSongs)
        }
    }

    LaunchedEffect(musicPlayerViewModel.currentSong, settingsViewModel.dynamicColorBySong) {
        val song = musicPlayerViewModel.currentSong
        if (song != null) playerStateManager.showMiniPlayer() else playerStateManager.hideMiniPlayer()
        if (song != null) statsViewModel.onSongStarted(song)
        else statsViewModel.onSongEnded()

        if (song != null && settingsViewModel.dynamicColorBySong) {
            val url = song.coverUrl ?: song.youtubeThumbnailUrl ?: song.filePath
            withContext(Dispatchers.IO) {
                val colors = com.fxzmusic.app.util.ColorExtractor.extractDominantColors(context, url)
                val primaryColor = colors.firstOrNull()
                withContext(Dispatchers.Main) {
                    settingsViewModel.dynamicSongAccentColor = primaryColor
                }
            }
        } else {
            settingsViewModel.dynamicSongAccentColor = null
        }
    }

    LaunchedEffect(musicPlayerViewModel.playbackError) {
        val err = musicPlayerViewModel.playbackError
        if (err != null) {
            snackbarHostState.showSnackbar(message = err, duration = SnackbarDuration.Long)
            musicPlayerViewModel.clearError()
        }
    }

    LaunchedEffect(musicPlayerViewModel.isPlaying) {
        if (musicPlayerViewModel.isPlaying) playerStateManager.showMiniPlayer()
    }

    LaunchedEffect(musicPlayerViewModel.isPlaying, musicPlayerViewModel.currentSong) {
        if (!musicPlayerViewModel.isPlaying) return@LaunchedEffect
        repeat(20) {
            val sessionId = PlaybackService.currentAudioSessionId
            if (sessionId != 0) {
                equalizerViewModel.attachToAudioSession(sessionId)
                fxViewModel.attachToSession(context, sessionId)
                return@LaunchedEffect
            }
            delay(150)
        }
    }

    LaunchedEffect(Unit) {
        fxViewModel.init(context)
    }

    LaunchedEffect(settingsViewModel.playbackSpeed) {
        musicPlayerViewModel.setPlaybackSpeed(settingsViewModel.playbackSpeed)
    }

    DisposableEffect(lifecycleOwner, settingsViewModel.shakeToSkip) {
        if (!settingsViewModel.shakeToSkip) {
            shakeDetector.stop()
            return@DisposableEffect onDispose { shakeDetector.stop() }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> shakeDetector.start(context.applicationContext)
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> shakeDetector.stop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            shakeDetector.start(context.applicationContext)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            shakeDetector.stop()
        }
    }

    val currentTheme = settingsViewModel.currentTheme()

    fun rotateAccent() {
        val next = (settingsViewModel.accentColorIndex + 1) % ACCENT_COLORS.size
        settingsViewModel.setAccentColor(next)
    }

    LaunchedEffect(selectedTab) {
        if (miniPlayerShouldBeVisible) miniPlayerRevealToken++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && latestMiniPlayerShouldBeVisible.value) {
                miniPlayerRevealToken++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun playPlaylist(
        playlist: Playlist,
        startSongId: String? = null,
        startPositionMs: Long = 0L,
        shuffle: Boolean = false
    ) {
        if (playlist.songs.isEmpty()) return
        val queue = if (shuffle) playlist.songs.shuffled() else playlist.songs
        val startSong = startSongId?.let { wanted -> queue.find { it.id == wanted } } ?: queue.first()
        musicPlayerViewModel.playSong(startSong, queue, context, allowQueueReplenish = false)
        activePlaylistId = playlist.id
        playerStateManager.showMiniPlayer()
        if (startPositionMs > 0L) {
            scope.launch {
                delay(350)
                musicPlayerViewModel.seekTo((startPositionMs / 1000L).toInt())
            }
        }
    }

    val tabs = listOf(
        BottomNavItem("Inicio",     Icons.Filled.Home,        Icons.Outlined.Home),
        BottomNavItem("Buscar",     Icons.Filled.Search,      Icons.Outlined.Search),
        BottomNavItem("Biblioteca", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
        BottomNavItem("Perfil",     Icons.Filled.Person,      Icons.Outlined.Person),
        BottomNavItem("Ajustes",    Icons.Filled.Settings,    Icons.Outlined.Settings)
    )

    CompositionLocalProvider(LocalFxzTheme provides currentTheme) {
    SharedTransitionLayout {
        Box(modifier = Modifier.fillMaxSize().background(currentTheme.background)) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    BottomNavBar(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        accent = currentTheme.accent
                    )
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(100))
                    },
                    label = "tab_transition",
                    modifier = Modifier.padding(innerPadding)
                ) { targetTab ->
                    when (targetTab) {
                        0 -> YouTubeNavHost(
                            modifier = Modifier.fillMaxSize(),
                            homeContent = { navController ->
                                                                HomeScreen(
                                    libraryViewModel = libraryViewModel,
                                    youTubeViewModel = youTubeViewModel,
                                    onPlaySong = { song ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayYouTubeSong = { song, songs ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, songs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayPlaylist = { playlist -> playPlaylist(playlist) },
                                    onAddSongsToQueue = { songs -> musicPlayerViewModel.appendSongsToQueue(songs) },
                                    onAddYouTubeSongsToQueue = { songs -> musicPlayerViewModel.appendSongsToQueue(songs) },
                                    onShowFullPlayer = { playerStateManager.openFullPlayer() },
                                    onNavigateToSearch = { navController.navigate(com.fxzmusic.app.navigation.YouTubeSearchRoute) },
                                    onNavigateToSearchWithQuery = { query ->
                                        pendingSearchQuery = query
                                        selectedTab = 1
                                    },
                                    onNavigateToLibrary = { filter ->
                                        libraryViewModel.updateSelectedFilter(filter)
                                        selectedTab = 2
                                    },
                                    onOpenAlbum = { browseId ->
                                        youTubeViewModel.openAlbum(browseId)
                                        navController.navigate(com.fxzmusic.app.navigation.YouTubeAlbumRoute(browseId))
                                    },
                                    onOpenArtist = { browseId ->
                                        youTubeViewModel.openArtist(browseId)
                                        navController.navigate(com.fxzmusic.app.navigation.YouTubeArtistRoute(browseId))
                                    },
                                    onOpenPlaylist = { playlistId ->
                                        youTubeViewModel.openPlaylist(playlistId)
                                        navController.navigate(com.fxzmusic.app.navigation.YouTubePlaylistRoute(playlistId))
                                    },
                                    onOpenCategory = { category -> navController.navigate(com.fxzmusic.app.navigation.YouTubeCategoryRoute(category.name)) },
                                    onPlayNext = { song -> musicPlayerViewModel.playNext(listOf(song)) },
                                    onRetry = { }
                                )
                            },
                            searchContent = { navController ->
                                SearchScreen(
                                    libraryViewModel = libraryViewModel,
                                    youTubeViewModel = youTubeViewModel,
                                    onPlaySong = { song ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayYouTubeSong = { song, songs ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, songs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onOpenAlbum = { browseId ->
                                        youTubeViewModel.openAlbum(browseId)
                                        navController.navigate(YouTubeAlbumRoute(browseId))
                                    },
                                    onOpenArtist = { browseId ->
                                        youTubeViewModel.openArtist(browseId)
                                        navController.navigate(YouTubeArtistRoute(browseId))
                                    },
                                    onOpenPlaylist = { playlistId ->
                                        youTubeViewModel.openPlaylist(playlistId)
                                        navController.navigate(YouTubePlaylistRoute(playlistId))
                                    },
                                    onEditTags = { song -> playerStateManager.editTags(song) }
                                )
                            },
                            albumContent = { browseId, navController ->
                                YouTubeDetailScreen(
                                    viewModel = youTubeViewModel,
                                    targetId = browseId,
                                    targetType = "album",
                                    onBack = { navController.popBackStack() },
                                    onPlaySong = { song, songs ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, songs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayNext = { song -> musicPlayerViewModel.playNext(listOf(song)) },
                                    onAddSongsToQueue = { songs -> musicPlayerViewModel.appendSongsToQueue(songs) },
                                    onOpenAlbum = { id ->
                                        youTubeViewModel.openAlbum(id)
                                        navController.navigate(YouTubeAlbumRoute(id))
                                    },
                                    onOpenArtist = { id ->
                                        youTubeViewModel.openArtist(id)
                                        navController.navigate(YouTubeArtistRoute(id))
                                    },
                                    onOpenPlaylist = { id ->
                                        youTubeViewModel.openPlaylist(id)
                                        navController.navigate(YouTubePlaylistRoute(id))
                                    },
                                )
                            },
                            artistContent = { browseId, navController ->
                                YouTubeDetailScreen(
                                    viewModel = youTubeViewModel,
                                    targetId = browseId,
                                    targetType = "artist",
                                    onBack = { navController.popBackStack() },
                                    onPlaySong = { song, songs ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, songs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayNext = { song -> musicPlayerViewModel.playNext(listOf(song)) },
                                    onAddSongsToQueue = { songs -> musicPlayerViewModel.appendSongsToQueue(songs) },
                                    onOpenAlbum = { id ->
                                        youTubeViewModel.openAlbum(id)
                                        navController.navigate(YouTubeAlbumRoute(id))
                                    },
                                    onOpenArtist = { id ->
                                        youTubeViewModel.openArtist(id)
                                        navController.navigate(YouTubeArtistRoute(id))
                                    },
                                    onOpenPlaylist = { id ->
                                        youTubeViewModel.openPlaylist(id)
                                        navController.navigate(YouTubePlaylistRoute(id))
                                    },
                                )
                            },
                            playlistContent = { playlistId, navController ->
                                YouTubeDetailScreen(
                                    viewModel = youTubeViewModel,
                                    targetId = playlistId,
                                    targetType = "playlist",
                                    onBack = { navController.popBackStack() },
                                    onPlaySong = { song, songs ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, songs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayNext = { song -> musicPlayerViewModel.playNext(listOf(song)) },
                                    onAddSongsToQueue = { songs -> musicPlayerViewModel.appendSongsToQueue(songs) },
                                    onOpenAlbum = { id ->
                                        youTubeViewModel.openAlbum(id)
                                        navController.navigate(YouTubeAlbumRoute(id))
                                    },
                                    onOpenArtist = { id ->
                                        youTubeViewModel.openArtist(id)
                                        navController.navigate(YouTubeArtistRoute(id))
                                    },
                                    onOpenPlaylist = { id ->
                                        youTubeViewModel.openPlaylist(id)
                                        navController.navigate(YouTubePlaylistRoute(id))
                                    },
                                )
                            },
                            categoryContent = { category, navController ->
                                val cat = runCatching { com.fxzmusic.app.ui.screens.CategoryType.valueOf(category) }
                                    .getOrDefault(com.fxzmusic.app.ui.screens.CategoryType.TRENDS)
                                com.fxzmusic.app.ui.screens.CategoryScreen(
                                    category = cat,
                                    viewModel = youTubeViewModel,
                                    onBack = { navController.popBackStack() },
                                    onPlaySong = { song, songs ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, songs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onOpenAlbum = { browseId ->
                                        youTubeViewModel.openAlbum(browseId)
                                        navController.navigate(YouTubeAlbumRoute(browseId))
                                    },
                                    onOpenPlaylist = { playlistId ->
                                        youTubeViewModel.openPlaylist(playlistId)
                                        navController.navigate(YouTubePlaylistRoute(playlistId))
                                    }
                                )
                            },
                            syncContent = { navController ->
                                com.fxzmusic.app.ui.screens.SyncScreen(
                                    viewModel = youTubeViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        )
                        1 -> SearchScreen(
                            libraryViewModel = libraryViewModel,
                            youTubeViewModel = youTubeViewModel,
                            initialQuery = pendingSearchQuery,
                            onQueryConsumed = { pendingSearchQuery = "" },
                            onPlaySong = { song ->
                                activePlaylistId = null
                                musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                playerStateManager.showMiniPlayer()
                            },
                            onPlayYouTubeSong = { song, songs ->
                                activePlaylistId = null
                                musicPlayerViewModel.playSong(song, songs, context)
                                playerStateManager.showMiniPlayer()
                            },
                            onEditTags = { song -> playerStateManager.editTags(song) }
                        )
                        2 -> LibraryNavHost(
                            modifier = Modifier.fillMaxSize(),
                            libraryContent = { navController ->
                                LibraryScreen(
                                    libraryViewModel = libraryViewModel,
                                    onPlayPlaylist = { playlist -> playPlaylist(playlist) },
                                    onPlaySong = { song ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlaySongInPlaylist = { playlist, song -> playPlaylist(playlist, startSongId = song.id) },
                                    onShufflePlaylist = { playlist -> playPlaylist(playlist, shuffle = true) },
                                    onResumePlaylist = { playlist, songId, progressMs ->
                                        playPlaylist(
                                            playlist = playlist,
                                            startSongId = songId,
                                            startPositionMs = progressMs
                                        )
                                    },
                                    onCreatePlaylist = { libraryViewModel.showCreatePlaylist() },
                                    onEditTags = { song -> playerStateManager.editTags(song) },
                                    onNavigateToAlbum = { albumName -> navController.navigate(AlbumRoute(java.net.URLEncoder.encode(albumName, "UTF-8"))) },
                                    onNavigateToArtist = { artistName -> navController.navigate(ArtistRoute(artistName)) },
                                    onNavigateToFolder = { folderPath -> navController.navigate(FolderRoute(folderPath)) },
                                    onNavigateToPlaylist = { playlistId -> navController.navigate(PlaylistRoute(playlistId)) },
                                    onNavigateToFavorites = { navController.navigate(FavoritesRoute) },
                                    onNavigateToDownloads = { navController.navigate(DownloadsRoute) }
                                )
                            },
                            albumContent = { albumName, navController ->
                                val album = libraryViewModel.allAlbums.find { it.name == albumName }
                                if (album != null) {
                                    val playlist = Playlist(
                                        id        = album.name.hashCode().toLong(),
                                        name      = album.name,
                                        songCount = album.songs.size,
                                        coverColor = album.songs.firstOrNull()?.albumArt ?: listOf(Color.Gray),
                                        coverUrl  = album.coverUrl,
                                        songs     = album.songs
                                    )
                                    AlbumScreen(
                                        albumName  = album.name,
                                        songs      = album.songs,
                                        onBack     = { navController.popBackStack() },
                                        onPlaySong = { song -> playPlaylist(playlist, startSongId = song.id) },
                                        onPlayAll  = { songs ->
                                            if (songs.isNotEmpty()) {
                                                val playlist = Playlist(
                                                    id        = album.name.hashCode().toLong(),
                                                    name      = album.name,
                                                    songCount = songs.size,
                                                    coverColor = songs.first().albumArt,
                                                    coverUrl  = album.coverUrl,
                                                    songs     = songs
                                                )
                                                playPlaylist(playlist)
                                            }
                                        },
                                        onShuffleAll = { shuffled ->
                                            if (shuffled.isNotEmpty()) {
                                                val playlist = Playlist(
                                                    id        = album.name.hashCode().toLong(),
                                                    name      = album.name,
                                                    songCount = shuffled.size,
                                                    coverColor = shuffled.first().albumArt,
                                                    coverUrl  = album.coverUrl,
                                                    songs     = shuffled
                                                )
                                                playPlaylist(playlist)
                                            }
                                        }
                                    )
                                }
                            },
                            artistContent = { artistName, navController ->
                                val artist = libraryViewModel.allArtists.find { it.name == artistName }
                                if (artist != null) {
                                    val playlist = Playlist(
                                        id        = artist.name.hashCode().toLong(),
                                        name      = artist.name,
                                        songCount = artist.songs.size,
                                        coverColor = artist.songs.firstOrNull()?.albumArt ?: listOf(Color.Gray),
                                        coverUrl  = artist.coverUrl,
                                        songs     = artist.songs
                                    )
                                    ArtistScreen(
                                        artistName   = artist.name,
                                        songs        = artist.songs,
                                        onBack       = { navController.popBackStack() },
                                        onPlaySong   = { song, artistSongs ->
                                            playPlaylist(playlist.copy(songs = artistSongs), startSongId = song.id)
                                        },
                                        onPlayAll    = { songs ->
                                            if (songs.isNotEmpty()) {
                                                val playlist = Playlist(
                                                    id        = artist.name.hashCode().toLong(),
                                                    name      = artist.name,
                                                    songCount = songs.size,
                                                    coverColor = songs.first().albumArt,
                                                    coverUrl  = artist.coverUrl,
                                                    songs     = songs
                                                )
                                                playPlaylist(playlist)
                                            }
                                        },
                                        onShuffleAll = { shuffled ->
                                            if (shuffled.isNotEmpty()) {
                                                val playlist = Playlist(
                                                    id        = artist.name.hashCode().toLong(),
                                                    name      = artist.name,
                                                    songCount = shuffled.size,
                                                    coverColor = shuffled.first().albumArt,
                                                    coverUrl  = artist.coverUrl,
                                                    songs     = shuffled
                                                )
                                                playPlaylist(playlist)
                                            }
                                        }
                                    )
                                }
                            },
                            folderContent = { folderPath, navController ->
                                val folder = libraryViewModel.allFolders.find { it.name == folderPath }
                                if (folder != null) {
                                    FolderDetailScreen(
                                        folderName   = folder.name,
                                        songs        = folder.songs,
                                        onBack       = { navController.popBackStack() },
                                        onPlaySong   = { song ->
                                            activePlaylistId = null
                                            musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                            playerStateManager.showMiniPlayer()
                                        },
                                        onPlayAll    = { songs ->
                                            if (songs.isNotEmpty()) {
                                                val playlist = Playlist(
                                                    id        = folder.name.hashCode().toLong(),
                                                    name      = folder.name,
                                                    songCount = songs.size,
                                                    coverColor = songs.first().albumArt,
                                                    songs     = songs
                                                )
                                                playPlaylist(playlist)
                                            }
                                        },
                                        onShuffleAll = { shuffled ->
                                            if (shuffled.isNotEmpty()) {
                                                val playlist = Playlist(
                                                    id        = folder.name.hashCode().toLong(),
                                                    name      = folder.name,
                                                    songCount = shuffled.size,
                                                    coverColor = shuffled.first().albumArt,
                                                    songs     = shuffled
                                                )
                                                playPlaylist(playlist)
                                            }
                                        },
                                        onEditTags = { song -> playerStateManager.editTags(song) }
                                    )
                                }
                            },
                            playlistContent = { playlistId, navController ->
                                val playlist = libraryViewModel.userPlaylists.find { it.id == playlistId }
                                if (playlist != null) {
                                    PlaylistDetailScreen(
                                        playlist            = playlist,
                                        onBack              = { navController.popBackStack() },
                                        onPlaySong          = { song -> playPlaylist(playlist, startSongId = song.id) },
                                        onPlaySongInPlaylist = { pl, song -> playPlaylist(pl, startSongId = song.id) },
                                        onShufflePlaylist    = { pl -> playPlaylist(pl, shuffle = true) },
                                        onResumePlaylist     = { pl, songId, progressMs ->
                                            playPlaylist(
                                                playlist      = pl,
                                                startSongId   = songId,
                                                startPositionMs = progressMs
                                            )
                                        },
                                        onEditTags = { song -> playerStateManager.editTags(song) },
                                        onAddSongsClick = {
                                            libraryViewModel.openPlaylist(playlist)
                                            libraryViewModel.showAddSongsDialog = true
                                        },
                                        onReorderSongs = { reordered ->
                                            libraryViewModel.reorderPlaylistSongs(playlist.id, reordered)
                                        },
                                        onRemoveSong = { songId ->
                                            libraryViewModel.removeSongFromPlaylist(playlist.id, songId)
                                        },
                                        onUpdateCover = { uri ->
                                            libraryViewModel.updatePlaylistMeta(
                                                playlistId = playlist.id,
                                                name = playlist.name,
                                                description = playlist.description,
                                                colors = playlist.coverColor,
                                                coverUri = uri
                                            )
                                        }
                                    )
                                }
                            },
                            favoritesContent = { navController ->
                                val likedSongs = libraryViewModel.allSongs.filter { it.isLiked }
                                FavoritesScreen(
                                    songs = likedSongs,
                                    onBack = { navController.popBackStack() },
                                    onPlaySong = { song ->
                                        activePlaylistId = null
                                        musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                        playerStateManager.showMiniPlayer()
                                    },
                                    onPlayAll = {
                                        if (likedSongs.isNotEmpty()) {
                                            playPlaylist(
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
                            },
                            downloadsContent = { navController ->
                                DownloadsScreen(
                                    onBack = { navController.popBackStack() },
                                    onPlaySong = { videoId ->
                                        
                                        val ytSong = libraryViewModel.allSongs.find { it.youtubeVideoId == videoId }
                                        if (ytSong != null) {
                                            musicPlayerViewModel.playSong(ytSong, libraryViewModel.allSongs, context)
                                            playerStateManager.showMiniPlayer()
                                        }
                                    }
                                )
                            }
                        )
                        3 -> ProfileScreen(
                            statsViewModel = statsViewModel,
                            libraryViewModel = libraryViewModel,
                            onPlaySong = { song ->
                                activePlaylistId = null
                                musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                playerStateManager.showMiniPlayer()
                            },
                            onShowFullPlayer = { playerStateManager.openFullPlayer() },
                            onNavigateToLibrary = { filter ->
                                libraryViewModel.updateSelectedFilter(filter)
                                selectedTab = 2
                            },
                            onNavigateToSearchWithQuery = { query ->
                                pendingSearchQuery = query
                                selectedTab = 1
                            },
                            onNavigateToHistory = { showHistory = true },
                        )
                        4 -> PlaybackSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onPausePlayback = { musicPlayerViewModel.pauseIfPlaying() },
                            onOpenEqualizer = { playerStateManager.showEqualizer() },
                            onOpenAudioFx = { playerStateManager.showAudioFx() },
                            allSongs = libraryViewModel.allSongs
                        )
                    }
                }
            }

            if (libraryViewModel.showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { libraryViewModel.hideCreatePlaylist() },
                    onConfirm = { name, coverUri -> libraryViewModel.createPlaylist(name, coverUri = coverUri) }
                )
            }

            if (libraryViewModel.showAddSongsDialog && libraryViewModel.selectedPlaylist != null) {
                com.fxzmusic.app.ui.components.AddSongsDialog(
                    allSongs = libraryViewModel.allSongs,
                    existingSongs = libraryViewModel.selectedPlaylist!!.songs,
                    onDismiss = { libraryViewModel.showAddSongsDialog = false },
                    onAddSongs = { songs ->
                        libraryViewModel.addSongsToPlaylist(libraryViewModel.selectedPlaylist!!.id, songs)
                        libraryViewModel.showAddSongsDialog = false
                    }
                )
            }

            val songToEdit = playerStateManager.tagEditorSong
            if (songToEdit != null) {
                TagEditorScreen(
                    song = songToEdit,
                    onDismiss = { playerStateManager.hideTagEditor() },
                    onSaved = { playerStateManager.hideTagEditor() }
                )
            }

            if (showHistory) {
                com.fxzmusic.app.ui.screens.HistoryScreen(
                    onBack = { showHistory = false },
                    onPlaySong = { item ->
                        val song = libraryViewModel.allSongs.find { it.id == item.songId }
                            ?: com.fxzmusic.app.data.Song(
                                id = item.songId,
                                title = item.title,
                                artist = item.artist,
                                coverUrl = item.thumbnail,
                                isYouTube = true,
                            )
                        activePlaylistId = null
                        musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                        playerStateManager.showMiniPlayer()
                        showHistory = false
                    },
                )
            }


            AnimatedVisibility(
                visible = miniPlayerShouldBeVisible,
                enter = slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
            ) {
                val miniVisibilityScope = this
                val miniPlayerSong = musicPlayerViewModel.currentSong
                if (miniPlayerSong != null) {
                    val currentPlaylistName = activePlaylistId?.let { id ->
                        libraryViewModel.userPlaylists.find { it.id == id }?.name
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        MiniPlayerWithVisualizer(
                            state = MiniPlayerState(
                                currentSong = miniPlayerSong,
                                isPlaying = musicPlayerViewModel.isPlaying,
                                isLiked = miniPlayerSong.isLiked,
                                currentPosition = musicPlayerViewModel.currentPosition,
                                duration = musicPlayerViewModel.duration,
                                playlistName = currentPlaylistName
                            ),
                            callbacks = MiniPlayerCallbacks(
                                onPlayPause = { musicPlayerViewModel.togglePlayPause() },
                                onClick = { playerStateManager.openFullPlayer() },
                                onNext = { musicPlayerViewModel.playNext() },
                                onPrevious = { musicPlayerViewModel.playPrevious() },
                                onSeek = { musicPlayerViewModel.seekTo(it) },
                                onToggleLike = { libraryViewModel.toggleLike(miniPlayerSong.id) },
                                onSwipeToDismiss = { playerStateManager.hideMiniPlayer(); if (musicPlayerViewModel.isPlaying) musicPlayerViewModel.togglePlayPause() }
                            ),
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = miniVisibilityScope,
                            modifier = Modifier
                                .padding(bottom = 90.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            val fullPlayerSong = musicPlayerViewModel.currentSong
            AnimatedContent(
                targetState = playerStateManager.isFullPlayerVisible && fullPlayerSong != null,
                transitionSpec = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy)) togetherWith
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(tween(200))
                },
                label = "full_player_transition"
            ) { visible ->
                if (visible && fullPlayerSong != null) {
                    androidx.compose.material3.Surface(
                        color = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FullPlayerScreen(
                            currentSong      = fullPlayerSong,
                            isPlaying        = musicPlayerViewModel.isPlaying,
                            currentPosition  = musicPlayerViewModel.currentPosition,
                            duration         = musicPlayerViewModel.duration,
                            isShuffleEnabled = musicPlayerViewModel.isShuffleEnabled,
                            repeatMode       = musicPlayerViewModel.repeatMode,
                            audioMetadata    = musicPlayerViewModel.currentAudioMetadata,
                            queue            = musicPlayerViewModel.playlist,
                            onPlaySongFromQueue = { song -> musicPlayerViewModel.playSong(song, musicPlayerViewModel.playlist, context) },
                            onPlayPause      = { musicPlayerViewModel.togglePlayPause() },
                            onNext           = { musicPlayerViewModel.playNext() },
                            onPrevious       = { musicPlayerViewModel.playPrevious() },
                            onSeek           = { musicPlayerViewModel.seekTo(it) },
                            onToggleShuffle  = { musicPlayerViewModel.toggleShuffle() },
                            onToggleRepeat   = { musicPlayerViewModel.toggleRepeatMode() },
                            onClose          = { playerStateManager.closeFullPlayer() },
                            onThemeRotate    = { rotateAccent() },
                            onToggleLike     = { libraryViewModel.toggleLike(fullPlayerSong.id) },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            modifier         = Modifier.fillMaxSize(),
                            onShowAudioDeviceSheet = { musicPlayerViewModel.openAudioDevices() },
                            onOpenEqualizer = { playerStateManager.showEqualizer() },
                            onRemoveFromQueue = { song ->
                                musicPlayerViewModel.removeSongFromQueue(song.id)
                            },
                            onClearQueue = {
                                musicPlayerViewModel.clearQueueKeepCurrent()
                            },
                            sleepTimerRemainingMs = settingsViewModel.sleepTimerRemainingMs,
                            isSleepTimerActive = settingsViewModel.isSleepTimerActive,
                            sleepAfterTrack = settingsViewModel.sleepAfterTrack,
                            onStartSleepTimer = { mins -> settingsViewModel.startSleepTimer(mins) { musicPlayerViewModel.pauseIfPlaying() } },
                            onCancelSleepTimer = { settingsViewModel.cancelSleepTimer() },
                            onToggleSleepAfterTrack = {
                                settingsViewModel.toggleSleepAfterTrack()
                            },
                            onMoveInQueue = { from, to -> musicPlayerViewModel.moveInQueue(from, to) },
                            currentPlaybackSpeed = musicPlayerViewModel.currentPlaybackSpeed,
                            onPlaybackSpeedChange = { musicPlayerViewModel.setPlaybackSpeed(it) },
                            userPlaylists = libraryViewModel.userPlaylists,
                            onAddToPlaylist = { playlist ->
                                libraryViewModel.addSongsToPlaylist(playlist.id, listOf(fullPlayerSong))
                            },
                            onCreateNewPlaylist = { name ->
                                libraryViewModel.createPlaylist(name, listOf(fullPlayerSong))
                            },
                            onShareSong = {
                                com.fxzmusic.app.util.ShareUtils.shareSong(context, fullPlayerSong)
                            },
                            playerBackgroundStyle = settingsViewModel.playerBackgroundStyle
                        )
                    }

                    if (musicPlayerViewModel.showAudioDeviceSheet) {
                        FullPlayerDevicesSheet(
                            showAudioDeviceSheet = musicPlayerViewModel.showAudioDeviceSheet,
                            onDismiss = { musicPlayerViewModel.closeAudioDevices() }
                        )
                    }
                }
            }

            SleepTimerBadge(
                settingsViewModel = settingsViewModel,
                isFullPlayerVisible = playerStateManager.isFullPlayerVisible,
                accent = currentTheme.accent,
                background = currentTheme.surfaceVariant
            )

            if (playerStateManager.showEqualizerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { playerStateManager.hideEqualizer() },
                    sheetState = eqSheetState,
                    containerColor = currentTheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EqualizerScreen(equalizerViewModel = equalizerViewModel)
                }
            }

            if (playerStateManager.showAudioFxSheet) {
                ModalBottomSheet(
                    onDismissRequest = { playerStateManager.hideAudioFx() },
                    sheetState = fxSheetState,
                    containerColor = currentTheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AudioFxScreen(fxViewModel = fxViewModel)
                }
            }

        }
    }
    }

    LaunchedEffect(activePlaylistId, musicPlayerViewModel.currentSong?.id) {
        val playlistId = activePlaylistId ?: return@LaunchedEffect
        while (activePlaylistId == playlistId) {
            val currentSong = musicPlayerViewModel.currentSong ?: break
            playbackDataStore.saveState(
                playlistId = playlistId,
                songId = currentSong.id,
                progressMs = musicPlayerViewModel.currentPosition * 1000L
            )
            delay(2000)
        }
    }
}

@Composable
private fun SleepTimerBadge(
    settingsViewModel: PlaybackSettingsViewModel,
    isFullPlayerVisible: Boolean,
    accent: Color,
    background: Color
) {
    if (!settingsViewModel.isSleepTimerActive || isFullPlayerVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "sleep_badge")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            background.copy(alpha = 0.94f),
                            accent.copy(alpha = glowAlpha * 0.2f),
                            background.copy(alpha = 0.94f)
                        ),
                        start = Offset.Zero,
                        end = Offset(200f, 0f)
                    )
                )
                .border(0.5.dp, accent.copy(alpha = glowAlpha * 0.3f), RoundedCornerShape(16.dp))
                .graphicsLayer { scaleX = bounceScale; scaleY = bounceScale; alpha = glowAlpha + 0.5f }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {  }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = glowAlpha + 0.3f))
                        .graphicsLayer { scaleX = bounceScale; scaleY = bounceScale }
                )
                Text(
                    "\uD83D\uDD50 ${settingsViewModel.formatSleepRemaining()}",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    tabs: List<BottomNavItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    surface: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    isGlass: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = maxWidth / tabs.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedTab,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "nav_indicator_offset"
                )

                var isMoving by remember { mutableStateOf(false) }
                LaunchedEffect(selectedTab) {
                    isMoving = true
                    delay(250)
                    isMoving = false
                }

                val capsuleStretchScaleX by animateFloatAsState(
                    targetValue = if (isMoving) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "capsule_stretch"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp, horizontal = 5.dp)
                        .graphicsLayer {
                            scaleX = capsuleStretchScaleX
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = 0.32f),
                                    accent.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = 0.65f),
                                    accent.copy(alpha = 0.20f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, item ->
                        val isSelected = selectedTab == index
                        val interaction = remember { MutableInteractionSource() }
                        val isPressed by interaction.collectIsPressedAsState()

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            animationSpec = tween(220),
                            label = "nav_color_$index"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 0.84f
                                isSelected -> 1.15f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = if (isPressed) Spring.DampingRatioNoBouncy else Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "nav_scale_$index"
                        )
                        val iconRotation by animateFloatAsState(
                            targetValue = if (isSelected) -4f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "nav_rot_$index"
                        )
                        val labelAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.55f,
                            animationSpec = tween(200),
                            label = "nav_label_alpha_$index"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) { onTabSelected(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        rotationZ = iconRotation
                                    }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                color = iconColor.copy(alpha = labelAlpha),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

