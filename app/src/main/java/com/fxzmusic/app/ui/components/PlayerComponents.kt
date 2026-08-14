@file:Suppress("DEPRECATION")
package com.fxzmusic.app.ui.components
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.viewmodel.MusicPlayerViewModel
import com.fxzmusic.app.viewmodel.LyricsViewModel
import com.fxzmusic.app.service.AudioOutputManager
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import com.fxzmusic.app.util.buildCoverRequest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fxzmusic.app.service.DownloadUtil
import com.fxzmusic.app.service.YouTubeMusicRepository
import com.fxzmusic.app.viewmodel.YouTubeMusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node {
        return object : Modifier.Node(), DrawModifierNode {
            override fun ContentDrawScope.draw() {
                drawContent()
            }
        }
    }
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

@Composable
fun FullPlayerDevicesSheet(
    showAudioDeviceSheet: Boolean,
    onDismiss: () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    if (!showAudioDeviceSheet) return
    val context = LocalContext.current
    val manager = remember { AudioOutputManager(context) }

    val currentDevice = remember(manager.devices) {
        manager.devices.find { it.isCurrent }
    }

    AudioDeviceSheet(
        manager = manager,
        accent = accent,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FullPlayerScreen(
    currentSong: Song,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: MusicPlayerViewModel.RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    audioMetadata: AudioMetadata? = null,
    queue: List<Song> = emptyList(),
    onPlaySongFromQueue: (Song) -> Unit = {},
    onToggleLike: () -> Unit = {},
    onAlbumClick: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onThemeRotate: () -> Unit = {},
    onOpenCarMode: () -> Unit = {},
    onShowAudioDeviceSheet: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onRemoveFromQueue: (Song) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onMoveInQueue: (from: Int, to: Int) -> Unit = { _, _ -> },
    sleepTimerRemainingMs: Long = 0L,
    isSleepTimerActive: Boolean = false,
    sleepAfterTrack: Boolean = false,
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onToggleSleepAfterTrack: () -> Unit = {},
    currentPlaybackSpeed: Float = 1f,
    onPlaybackSpeedChange: (Float) -> Unit = {},
    connectedDeviceName: String? = null,
    lyricsViewModel: LyricsViewModel = viewModel(),
    youTubeMusicViewModel: YouTubeMusicViewModel = viewModel(),
    musicPlayerViewModel: MusicPlayerViewModel = viewModel(),
    userPlaylists: List<Playlist> = emptyList(),
    onAddToPlaylist: (Playlist) -> Unit = {},
    onCreateNewPlaylist: (String) -> Unit = {},
    onShareSong: () -> Unit = {},
    playerBackgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    getCurrentPositionMs: () -> Long = { currentPosition.toLong() * 1000L }
) {
    var showUi by remember { mutableStateOf(true) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showSongInfo by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showPlayerMenu by remember { mutableStateOf(false) }
    var showSimilarSongsSheet by remember { mutableStateOf(false) }
    var glassResetTrigger by remember { mutableIntStateOf(0) }
    val currentPositionMsProvider = rememberUpdatedState(getCurrentPositionMs)

    LaunchedEffect(currentSong.id) {
        lyricsViewModel.loadLyricsWithOffset(currentSong)
        showUi = true
    }

    LaunchedEffect(isPlaying, showLyrics) {
        if (isPlaying && showLyrics) {
            lyricsViewModel.startSync({ isPlaying }) { currentPositionMsProvider.value() }
        } else {
            lyricsViewModel.stopSync()
        }
    }

    LaunchedEffect(isPlaying, showUi) {
        if (isPlaying && showUi) { delay(5000); showUi = false }
    }

    val onUserInteraction: () -> Unit = {
        showUi = true
        glassResetTrigger += 1
    }

    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "full_player_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_pulse"
    )

    BackHandler(enabled = true) { onClose() }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioOutputManager = remember { AudioOutputManager(context) }
    var showAudioDeviceSheet by remember { mutableStateOf(false) }
    var lastVolumeDragMs by remember { mutableLongStateOf(0L) }
    var accumulatedVolumeDrag by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    CompositionLocalProvider(LocalIndication provides NoIndication) {
        Box(
            modifier = modifier
                .background(Color.Black)
                .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showUi = !showUi }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedVolumeDrag = 0f },
                    onDragEnd = { accumulatedVolumeDrag = 0f },
                    onDragCancel = { accumulatedVolumeDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedVolumeDrag += dragAmount
                        val now = System.currentTimeMillis()
                        if (kotlin.math.abs(accumulatedVolumeDrag) > 35f && now - lastVolumeDragMs > 100L) {
                            lastVolumeDragMs = now
                            val direction = if (accumulatedVolumeDrag < 0f) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
                            accumulatedVolumeDrag = 0f
                        }
                    }
                )
            }
    ) {
        PlayerBackground(
            currentSong = currentSong,
            isPlaying = isPlaying,
            pulseScale = pulseScale,
            resetKey = glassResetTrigger,
            style = playerBackgroundStyle
        )

        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val panelState = when {
                    showQueue -> 1
                    showLyrics -> 2
                    else -> 0
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = panelState,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)) togetherWith
                                slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(150))
                            } else {
                                slideInHorizontally(tween(300)) { -it } + fadeIn(tween(200)) togetherWith
                                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150))
                            }
                        },
                        label = "panel_transition_landscape"
                    ) { state ->
                        when (state) {
                            1 -> QueuePanel(
                                queue = queue,
                                currentSong = currentSong,
                                onPlaySong = { onUserInteraction(); onPlaySongFromQueue(it) },
                                onRemoveFromQueue = { onUserInteraction(); onRemoveFromQueue(it) },
                                onClearQueue = { onUserInteraction(); onClearQueue() },
                                onMoveInQueue = { from, to -> onUserInteraction(); onMoveInQueue(from, to) },
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> LyricsPanel(lyricsViewModel = lyricsViewModel, onSeek = { onUserInteraction(); onSeek(it) }, modifier = Modifier.fillMaxSize())
                            else -> FullPlayerCoverArt(
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                onSeek = { onUserInteraction(); onSeek(it) },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onClose = onClose,
                                onToggleLike = onToggleLike,
                                onShowLyrics = { showLyrics = true; showQueue = false },
                                onUserInteraction = onUserInteraction,
                                onTap = { showUi = !showUi },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                albumColors = currentSong.albumArt
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BouncyIconButton(icon = Icons.Filled.KeyboardArrowDown, tint = MaterialTheme.colorScheme.onSurface, onClick = { onUserInteraction(); onClose() })
                        if (currentSong.album.isNotBlank() && currentSong.album != "Unknown Album") {
                            Text(currentSong.album, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BouncyIconButton(
                                icon = Icons.Filled.AutoAwesome,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onUserInteraction(); showSimilarSongsSheet = true }
                            )
                            BouncyIconButton(
                                icon = Icons.Filled.MoreVert,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onClick = { onUserInteraction(); showPlayerMenu = true }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentSong.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(currentSong.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        val likeInteraction = remember { MutableInteractionSource() }
                        val isLikePressed by likeInteraction.collectIsPressedAsState()
                        val likeScale by animateFloatAsState(
                            targetValue = if (isLikePressed) 0.80f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                            label = "landscape_like_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .scale(likeScale)
                                .clip(CircleShape)
                                .clickable(interactionSource = likeInteraction, indication = null) {
                                    onUserInteraction()
                                    onToggleLike()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = currentSong.isLiked,
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(150)))
                                        .togetherWith(scaleOut(tween(100)) + fadeOut(tween(100)))
                                },
                                label = "landscape_like_icon"
                            ) { liked ->
                                Icon(
                                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (liked) "Quitar de favoritos" else "Marcar como favorito",
                                    tint = if (liked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    WaveformProgressBar(
                        songId = currentSong.id,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = { onUserInteraction(); onSeek(it) }
                    )

                    FullPlayerControls(
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        showLyrics = showLyrics,
                        showQueue = showQueue,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onShowLyricsToggle = { showLyrics = !showLyrics; showQueue = false },
                        onShowQueueToggle = { showQueue = !showQueue; showLyrics = false },
                        onOpenEqualizer = onOpenEqualizer,
                        onUserInteraction = onUserInteraction,
                        isSleepTimerActive = isSleepTimerActive,
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        onShowSleepTimerToggle = { showSleepTimerDialog = true },
                        currentPlaybackSpeed = currentPlaybackSpeed,
                        onShowSpeedToggle = { showSpeedDialog = true }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = showUi, enter = slideInVertically(tween(300)) { -it / 4 } + fadeIn(tween(250)), exit = slideOutVertically(tween(500)) { -it / 5 } + fadeOut(tween(400))) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BouncyIconButton(icon = Icons.Filled.KeyboardArrowDown, tint = MaterialTheme.colorScheme.onSurface, onClick = { onUserInteraction(); onClose() })
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (currentSong.album.isNotBlank() && currentSong.album != "Unknown Album") {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onUserInteraction(); onAlbumClick() }
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(currentSong.album, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSleepTimerActive && sleepTimerRemainingMs > 0) {
                                val mins = (sleepTimerRemainingMs / 60_000).toInt()
                                val secs = ((sleepTimerRemainingMs % 60_000) / 1000).toInt()
                                val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            timeStr,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            BouncyIconButton(
                                icon = Icons.Filled.AutoAwesome,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onUserInteraction(); showSimilarSongsSheet = true }
                            )
                            BouncyIconButton(
                                icon = Icons.Filled.MoreVert,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onClick = { onUserInteraction(); showPlayerMenu = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val panelState = when {
                    showQueue -> 1
                    showLyrics -> 2
                    else -> 0
                }

                AnimatedContent(
                    targetState = panelState,
                    modifier = Modifier.weight(4f),
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)) togetherWith
                            slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(150))
                        } else {
                            slideInHorizontally(tween(300)) { -it } + fadeIn(tween(200)) togetherWith
                            slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150))
                        }
                    },
                    label = "panel_transition"
                ) { state ->
                    when (state) {
                        1 -> QueuePanel(
                            queue = queue,
                            currentSong = currentSong,
                            onPlaySong = { onUserInteraction(); onPlaySongFromQueue(it) },
                            onRemoveFromQueue = { onUserInteraction(); onRemoveFromQueue(it) },
                            onClearQueue = { onUserInteraction(); onClearQueue() },
                            onMoveInQueue = { from, to -> onUserInteraction(); onMoveInQueue(from, to) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        2 -> LyricsPanel(lyricsViewModel = lyricsViewModel, onSeek = { onUserInteraction(); onSeek(it) }, modifier = Modifier.fillMaxWidth())
                        else -> FullPlayerCoverArt(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onSeek = { onUserInteraction(); onSeek(it) },
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onClose = onClose,
                            onToggleLike = onToggleLike,
                            onShowLyrics = { showLyrics = true; showQueue = false },
                            onUserInteraction = onUserInteraction,
                            onTap = { showUi = !showUi },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            albumColors = currentSong.albumArt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(visible = showUi, enter = slideInVertically(tween(350)) { it / 4 } + fadeIn(tween(300)), exit = slideOutVertically(tween(450)) { it / 5 } + fadeOut(tween(400))) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val titleOffset = remember { Animatable(30f) }
                        val titleAlpha = remember { Animatable(0f) }
                        val artistOffsetX = remember { Animatable(-12f) }
                        val artistAlpha = remember { Animatable(0f) }

                        LaunchedEffect(currentSong.id) {
                            titleOffset.snapTo(30f); titleAlpha.snapTo(0f)
                            artistOffsetX.snapTo(-12f); artistAlpha.snapTo(0f)
                            kotlinx.coroutines.coroutineScope {
                                launch {
                                    titleOffset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow))
                                    titleAlpha.animateTo(1f, tween(250))
                                }
                                launch {
                                    kotlinx.coroutines.delay(120)
                                    artistOffsetX.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
                                    artistAlpha.animateTo(1f, tween(300))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val context = LocalContext.current
                                val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
                                Text(
                                    currentSong.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .pointerInput(currentSong.title) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    val clip = ClipData.newPlainText("Título", currentSong.title)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Título copiado", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                        .basicMarquee(iterations = Int.MAX_VALUE)
                                        .graphicsLayer {
                                            translationY = titleOffset.value
                                            alpha = titleAlpha.value
                                        }
                                )
                                Text(
                                    currentSong.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .pointerInput(currentSong.artist) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    val clip = ClipData.newPlainText("Artista", currentSong.artist)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Artista copiado", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                        .graphicsLayer {
                                            translationX = artistOffsetX.value
                                            alpha = artistAlpha.value
                                        }
                                )
                                if (audioMetadata != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AudioQualityBadge(
                                            metadata = audioMetadata,
                                            onClick = { showSongInfo = true }
                                        )
                                        val currentOutputDevice = remember(audioOutputManager.devices) {
                                            audioOutputManager.devices.find { it.isCurrent }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    onUserInteraction()
                                                    audioOutputManager.refresh()
                                                    showAudioDeviceSheet = true
                                                }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (currentOutputDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) Icons.Filled.Bluetooth
                                                              else if (currentOutputDevice?.isUsbc == true) Icons.Filled.Usb
                                                              else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = currentOutputDevice?.name ?: "Altavoz",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            val likeInteraction = remember { MutableInteractionSource() }
                            val isLikePressed   by likeInteraction.collectIsPressedAsState()
                            val likeScale       by animateFloatAsState(
                                targetValue   = if (isLikePressed) 0.82f else 1f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                                label         = "like_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(likeScale)
                                    .clip(CircleShape)
                                    .clickable(interactionSource = likeInteraction, indication = null) {
                                        onUserInteraction()
                                        onToggleLike()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState   = currentSong.isLiked,
                                    transitionSpec = { scaleIn(spring(dampingRatio = 0.4f)) togetherWith scaleOut(tween(120)) },
                                    label         = "like_icon"
                                ) { liked ->
                                    Icon(
                                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (liked) "Quitar de favoritos" else "Marcar como favorito",
                                        tint     = if (liked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        WaveformProgressBar(
                            songId = currentSong.id,
                            currentPosition = currentPosition,
                            duration = duration,
                            onSeek = { onUserInteraction(); onSeek(it) }
                        )

                        FullPlayerControls(
                            isPlaying = isPlaying,
                            isShuffleEnabled = isShuffleEnabled,
                            repeatMode = repeatMode,
                            showLyrics = showLyrics,
                            showQueue = showQueue,
                            onPlayPause = onPlayPause,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onShowLyricsToggle = { showLyrics = !showLyrics; showQueue = false },
                            onShowQueueToggle = { showQueue = !showQueue; showLyrics = false },
                            onOpenEqualizer = onOpenEqualizer,
                            onUserInteraction = onUserInteraction,
                            isSleepTimerActive = isSleepTimerActive,
                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                            onShowSleepTimerToggle = { showSleepTimerDialog = true },
                            currentPlaybackSpeed = currentPlaybackSpeed,
                            onShowSpeedToggle = { showSpeedDialog = true }
                        )
                    }
                }
            }
        }
    }
}

    PlayerMenuSheet(
        isVisible = showPlayerMenu,
        onDismiss = { showPlayerMenu = false },
        onAddToPlaylist = { showPlayerMenu = false; showPlaylistPicker = true },
        onShare = { onShareSong() },
        onDownload = {
            DownloadUtil.get().enqueue(
                videoId = currentSong.id,
                title = currentSong.title,
                artist = currentSong.artist,
                album = currentSong.album,
                coverUrl = currentSong.coverUrl ?: currentSong.youtubeThumbnailUrl,
                duration = currentSong.duration
            )
            android.widget.Toast.makeText(context, "Descarga iniciada", android.widget.Toast.LENGTH_SHORT).show()
        },
        onSongInfo = { showSongInfo = true },
        onComments = { showComments = true },
        onThemeChange = { onThemeRotate() },
        onOpenCarMode = { onOpenCarMode() },
        onSleepTimer = { showSleepTimerDialog = true },
        onSimilarSongs = { showSimilarSongsSheet = true },
        onStartRadio = {
            youTubeMusicViewModel.startSongRadio(
                song = currentSong,
                playImmediately = true,
                musicPlayerViewModel = musicPlayerViewModel,
                context = context,
                onStarted = {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.song_radio_started),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        },
        lyricsOffsetMs = lyricsViewModel.lyricsOffsetMs,
        hasSyncedLyrics = lyricsViewModel.lyricsState is LyricsState.Synced,
        onLyricsOffsetPlus = { lyricsViewModel.adjustOffset(500) },
        onLyricsOffsetMinus = { lyricsViewModel.adjustOffset(-500) },
        onLyricsOffsetReset = { lyricsViewModel.resetOffset() }
    )

    SimilarSongsSheet(
        isVisible = showSimilarSongsSheet,
        song = currentSong,
        youTubeViewModel = youTubeMusicViewModel,
        musicPlayerViewModel = musicPlayerViewModel,
        onDismiss = { showSimilarSongsSheet = false },
        onOpenArtist = onOpenArtist,
        onOpenAlbum = onOpenAlbum
    )

    if (showAudioDeviceSheet) {
        AudioDeviceSheet(
            manager = audioOutputManager,
            accent = MaterialTheme.colorScheme.primary,
            onDismiss = { showAudioDeviceSheet = false }
        )
    }

    if (showPlaylistPicker) {
        val context = LocalContext.current
        PlaylistPickerSheet(
            playlists = userPlaylists,
            currentSong = currentSong,
            onAddToPlaylist = { playlist ->
                val isInPlaylist = playlist.songs.any { it.id == currentSong.id }
                if (isInPlaylist) {
                    android.widget.Toast.makeText(context, "Ya está en \"${playlist.name}\"", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    onAddToPlaylist(playlist)
                    android.widget.Toast.makeText(context, "Agregado a \"${playlist.name}\"", android.widget.Toast.LENGTH_SHORT).show()
                }
                showPlaylistPicker = false
            },
            onCreateNew = { name ->
                onCreateNewPlaylist(name)
                android.widget.Toast.makeText(context, "Playlist \"$name\" creada", android.widget.Toast.LENGTH_SHORT).show()
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    if (showSongInfo) {
        SongInfoSheet(
            song = currentSong,
            audioMetadata = audioMetadata,
            onDismiss = { showSongInfo = false }
        )
    }

    if (showComments) {
        CommentSheet(
            videoId = currentSong.id,
            onDismiss = { showComments = false },
        )
    }

    SleepTimerDialog(
        isVisible = showSleepTimerDialog,
        isSleepTimerActive = isSleepTimerActive,
        sleepTimerRemainingMs = sleepTimerRemainingMs,
        sleepAfterTrack = sleepAfterTrack,
        onSetTimer = { onStartSleepTimer(it) },
        onCancelTimer = { onCancelSleepTimer() },
        onToggleSleepAfterTrack = { onToggleSleepAfterTrack() },
        onDismiss = { showSleepTimerDialog = false }
    )

    SpeedDialog(
        isVisible = showSpeedDialog,
        currentPlaybackSpeed = currentPlaybackSpeed,
        onSetSpeed = { onPlaybackSpeedChange(it) },
        onDismiss = { showSpeedDialog = false }
    )
}
@Composable
fun AudioOutputBar(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val manager = remember { AudioOutputManager(context) }
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) { manager.refresh() }

    val current = manager.devices.firstOrNull { it.isCurrent }
    val isBt = current?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
    val isUsbc = current?.isUsbc == true
    val isWired = current?.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            current?.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
    val isNonDefault = isBt || isUsbc || isWired
    val label = manager.getCurrentLabel()

    val infiniteTransition = rememberInfiniteTransition(label = "bt_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val chipColor = if (isNonDefault) accent else MaterialTheme.colorScheme.onSurfaceVariant
    val borderMod = if (isBt)
        Modifier.border(1.dp, accent.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
    else
        Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(20.dp))

    AnimatedVisibility(
        visible = isNonDefault,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 },
        exit = fadeOut(tween(200))
    ) {
        Row(
            modifier = modifier
                .then(borderMod)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isNonDefault) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.07f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
                manager.refresh()
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_VOLUME)
                } else {
                    Intent(Settings.ACTION_SOUND_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = when {
                isBt -> Icons.Filled.Bluetooth
                isUsbc -> Icons.Filled.Usb
                else -> Icons.AutoMirrored.Filled.VolumeUp
            },
            contentDescription = "Salida de audio: $label",
            tint = chipColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = chipColor,
            fontSize = 11.sp,
            fontWeight = if (isNonDefault) FontWeight.Bold else FontWeight.Normal
        )
    }
    }
}

@Composable
fun SongPreviewOverlay(
    song: Song,
    progress: Float,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.material3.Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(16.dp),
                modifier = Modifier.size(240.dp)
            ) {
                Box {
                    coil.compose.AsyncImage(
                        model = buildCoverRequest(androidx.compose.ui.platform.LocalContext.current, song, maxSize = 1024),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = song.title,
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.artist,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
