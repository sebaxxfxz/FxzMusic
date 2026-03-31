package com.example.fxzmusic

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
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
import com.example.fxzmusic.ui.theme.FxzMusicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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
        checkPermissionAndScan()
        setContent { FxzMusicTheme { MainScreen() } }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    musicPlayerViewModel: MusicPlayerViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    equalizerViewModel: EqualizerViewModel = viewModel(),
    statsViewModel: StatsViewModel = viewModel(),
    settingsViewModel: PlaybackSettingsViewModel = viewModel(),
    fxViewModel: AudioFxViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var pendingSearchQuery by rememberSaveable { mutableStateOf("") }
    var showMiniPlayer by rememberSaveable { mutableStateOf(false) }
    var isFullPlayerVisible by rememberSaveable { mutableStateOf(false) }
    var showEqualizerSheet by rememberSaveable { mutableStateOf(false) }
    var showAudioFxSheet by rememberSaveable { mutableStateOf(false) }
    var tagEditorSong by remember { mutableStateOf<Song?>(null) }
    var activePlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var miniPlayerRevealToken by rememberSaveable { mutableIntStateOf(0) }
    val eqSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fxSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playbackDataStore = remember(context) { PlaylistPlaybackDataStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val miniPlayerShouldBeVisible by remember {
        androidx.compose.runtime.derivedStateOf {
            showMiniPlayer && musicPlayerViewModel.currentSong != null && !isFullPlayerVisible
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
        coroutineScope {
            launch(Dispatchers.IO) { equalizerViewModel.init(context) }
            launch(Dispatchers.IO) { statsViewModel.init(context) }
            launch(Dispatchers.IO) { libraryViewModel.initPrefs(context) }
            launch(Dispatchers.IO) { settingsViewModel.init(context) }
        }
        musicPlayerViewModel.initializePlayer(context)
    }

    DisposableEffect(Unit) {
        statsViewModel.onSongStatsChanged = { songId, playCount, lastPlayed ->
            libraryViewModel.updateSongStats(songId, playCount, lastPlayed)
        }
        libraryViewModel.onLikeChanged = { songId, isLiked ->
            musicPlayerViewModel.updateCurrentSongLike(songId, isLiked)
        }
        libraryViewModel.onPlaylistDeleted = { playlistId ->
            musicPlayerViewModel.stopSong()
            isFullPlayerVisible = false
            showMiniPlayer = false
            if (activePlaylistId == playlistId) activePlaylistId = null
        }
        onDispose {
            statsViewModel.onSongStatsChanged = null
            libraryViewModel.onLikeChanged = null
            libraryViewModel.onPlaylistDeleted = null
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

    LaunchedEffect(musicPlayerViewModel.currentSong) {
        val song = musicPlayerViewModel.currentSong
        showMiniPlayer = song != null
        if (song != null) statsViewModel.onSongStarted(song)
        else statsViewModel.onSongEnded()
    }

    LaunchedEffect(musicPlayerViewModel.isPlaying) {
        if (musicPlayerViewModel.isPlaying) showMiniPlayer = true
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
        musicPlayerViewModel.playSong(startSong, queue, context)
        activePlaylistId = playlist.id
        showMiniPlayer = true
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
        Box(modifier = Modifier.fillMaxSize().background(currentTheme.background)) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    BottomNavBar(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        accent = currentTheme.accent,
                        surface = currentTheme.surface,
                        isGlass = currentTheme.mode == ThemeMode.GLASSMORPHISM
                    )
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(
                            initialOffsetX = { if (forward) it / 5 else -it / 5 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { if (forward) -it / 5 else it / 5 },
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(200)))
                    },
                    label = "tab_transition",
                    modifier = Modifier.padding(innerPadding)
                ) { targetTab ->
                    when (targetTab) {
                        0 -> HomeScreen(
                            libraryViewModel = libraryViewModel,
                            onPlaySong = { song ->
                                activePlaylistId = null
                                musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                showMiniPlayer = true
                            },
                            onPlayPlaylist = { playlist -> playPlaylist(playlist) },
                            onShowFullPlayer = { isFullPlayerVisible = true },
                            onNavigateToSearch = { selectedTab = 1 },
                            onNavigateToSearchWithQuery = { query ->
                                pendingSearchQuery = query
                                selectedTab = 1
                            }
                        )
                        1 -> SearchScreen(
                            libraryViewModel = libraryViewModel,
                            initialQuery = pendingSearchQuery,
                            onQueryConsumed = { pendingSearchQuery = "" },
                            onPlaySong = { song ->
                                activePlaylistId = null
                                musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                showMiniPlayer = true
                            },
                            onEditTags = { song -> tagEditorSong = song }
                        )
                        2 -> LibraryScreen(
                            libraryViewModel = libraryViewModel,
                            onPlayPlaylist = { playlist -> playPlaylist(playlist) },
                            onPlaySong = { song ->
                                activePlaylistId = null
                                musicPlayerViewModel.playSong(song, libraryViewModel.allSongs, context)
                                showMiniPlayer = true
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
                            onEditTags = { song -> tagEditorSong = song }
                        )
                        3 -> ProfileScreen(statsViewModel = statsViewModel)
                        4 -> PlaybackSettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onPausePlayback = { musicPlayerViewModel.pauseIfPlaying() },
                            onOpenEqualizer = { showEqualizerSheet = true },
                            onOpenAudioFx = { showAudioFxSheet = true }
                        )
                    }
                }
            }

            if (libraryViewModel.showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { libraryViewModel.hideCreatePlaylist() },
                    onConfirm = { name -> libraryViewModel.createPlaylist(name) }
                )
            }

            val songToEdit = tagEditorSong
            if (songToEdit != null) {
                TagEditorScreen(
                    song = songToEdit,
                    onDismiss = { tagEditorSong = null },
                    onSaved = { tagEditorSong = null }
                )
            }

            val miniPlayerSong = musicPlayerViewModel.currentSong
            val miniPlayerVisibleState = remember(miniPlayerRevealToken) {
                MutableTransitionState(false).apply { targetState = miniPlayerShouldBeVisible }
            }
            LaunchedEffect(miniPlayerShouldBeVisible) {
                miniPlayerVisibleState.targetState = miniPlayerShouldBeVisible
            }
            AnimatedVisibility(
                visibleState = miniPlayerVisibleState,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 320)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)),
                exit  = slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220)) +
                        scaleOut(targetScale = 0.99f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp).padding(horizontal = 16.dp)
            ) {
                if (miniPlayerSong != null) {
                    MiniPlayerWithVisualizer(
                        currentSong      = miniPlayerSong,
                        isPlaying        = musicPlayerViewModel.isPlaying,
                        currentPosition  = musicPlayerViewModel.currentPosition,
                        duration         = musicPlayerViewModel.duration,
                        onPlayPause      = { musicPlayerViewModel.togglePlayPause() },
                        onNext           = { musicPlayerViewModel.playNext() },
                        onClick          = { isFullPlayerVisible = true },
                        onSwipeToDismiss = { showMiniPlayer = false; if (musicPlayerViewModel.isPlaying) musicPlayerViewModel.togglePlayPause() },
                        modifier         = Modifier.graphicsLayer { alpha = 1f }
                    )
                }
            }

            val fullPlayerSong = musicPlayerViewModel.currentSong
            AnimatedVisibility(
                visible = isFullPlayerVisible && fullPlayerSong != null,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)) + fadeIn(tween(260)),
                exit  = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
            ) {
                if (fullPlayerSong != null) FullPlayerScreen(
                    currentSong      = fullPlayerSong,
                    isPlaying        = musicPlayerViewModel.isPlaying,
                    currentPosition  = musicPlayerViewModel.currentPosition,
                    duration         = musicPlayerViewModel.duration,
                    isShuffleEnabled = musicPlayerViewModel.isShuffleEnabled,
                    repeatMode       = musicPlayerViewModel.repeatMode,
                    playbackSpeed    = musicPlayerViewModel.currentPlaybackSpeed,
                    queue            = musicPlayerViewModel.playlist,
                    onPlaySongFromQueue = { song -> musicPlayerViewModel.playSong(song, musicPlayerViewModel.playlist, context) },
                    onPlayPause      = { musicPlayerViewModel.togglePlayPause() },
                    onNext           = { musicPlayerViewModel.playNext() },
                    onPrevious       = { musicPlayerViewModel.playPrevious() },
                    onSeek           = { musicPlayerViewModel.seekTo(it) },
                    onToggleShuffle  = { musicPlayerViewModel.toggleShuffle() },
                    onToggleRepeat   = { musicPlayerViewModel.toggleRepeatMode() },
                    onClose          = { isFullPlayerVisible = false },
                    onSpeedChange    = { speed -> settingsViewModel.updateSpeed(speed); musicPlayerViewModel.setPlaybackSpeed(speed) },
                    onThemeRotate    = { rotateAccent() },
                    onToggleLike     = { libraryViewModel.toggleLike(fullPlayerSong.id) },
                    modifier         = Modifier.fillMaxSize()
                )
            }

            SleepTimerBadge(
                settingsViewModel = settingsViewModel,
                isFullPlayerVisible = isFullPlayerVisible,
                accent = currentTheme.accent,
                background = currentTheme.surfaceVariant
            )

            if (showEqualizerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showEqualizerSheet = false },
                    sheetState = eqSheetState,
                    containerColor = currentTheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EqualizerScreen(equalizerViewModel = equalizerViewModel)
                }
            }

            if (showAudioFxSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAudioFxSheet = false },
                    sheetState = fxSheetState,
                    containerColor = currentTheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AudioFxScreen(fxViewModel = fxViewModel)
                }
            }

            if (showSplash) {
                SplashScreen(onFinished = { showSplash = false })
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(background.copy(alpha = 0.92f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("⏱ ${settingsViewModel.formatSleepRemaining()}", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomNavBar(
    tabs: List<BottomNavItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    accent: Color = Color(0xFF00E676),
    surface: Color = Color(0xEE0A0A0A),
    isGlass: Boolean = false
) {
    NavigationBar(
        containerColor = if (isGlass) surface.copy(alpha = 0.7f) else surface.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        modifier = Modifier.graphicsLayer { shadowElevation = if (isGlass) 6f else 14f }
    ) {
        tabs.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            val iconColor by animateColorAsState(targetValue = if (isSelected) accent else Color.Gray, animationSpec = tween(300), label = "nav_color_$index")
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.18f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "nav_scale_$index"
            )
            NavigationBarItem(
                icon = { Icon(imageVector = if (isSelected) item.selectedIcon else item.icon, contentDescription = item.label, tint = iconColor, modifier = Modifier.scale(iconScale)) },
                label = { Text(text = item.label, color = iconColor, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                selected = isSelected,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedIconColor = accent, unselectedIconColor = Color.Gray)
            )
        }
    }
}
