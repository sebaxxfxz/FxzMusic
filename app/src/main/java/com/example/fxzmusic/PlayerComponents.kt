package com.example.fxzmusic

import android.content.Intent
import android.media.AudioDeviceInfo
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.abs

suspend fun getCoverUrl(song: Song): String? = song.coverUrl ?: CoverSearchService.searchCover(song)

@OptIn(ExperimentalMaterial3Api::class)
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
    playbackSpeed: Float = 1.0f,
    queue: List<Song> = emptyList(),
    onPlaySongFromQueue: (Song) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onToggleLike: () -> Unit = {},
    onAlbumClick: () -> Unit = {},
    onThemeRotate: () -> Unit = {},
    lyricsViewModel: LyricsViewModel = viewModel()
) {
    var showUi by remember { mutableStateOf(true) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableIntStateOf(0) }

    val currentPositionRef = rememberUpdatedState(currentPosition)

    LaunchedEffect(currentSong.id) {
        lyricsViewModel.loadLyrics(currentSong)
        swipeDirection = 0
        showUi = true
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) lyricsViewModel.startSync { currentPositionRef.value.toLong() * 1000L }
        else lyricsViewModel.stopSync()
    }

    LaunchedEffect(isPlaying, showUi) {
        if (isPlaying && showUi) { delay(15000); showUi = false }
    }

    fun onUserInteraction() { showUi = true }

    val infiniteTransition = rememberInfiniteTransition(label = "full_player_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_pulse"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "cover"
    )

    val playPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "play_pulse_scale"
    )

    val playPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "play_pulse_alpha"
    )

    val dynamicElevation by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cover_elevation_pulse"
    )

    val coverElevation by animateFloatAsState(
        targetValue = if (isPlaying) dynamicElevation else 12f,
        animationSpec = tween(500),
        label = "cover_elevation"
    )

    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "particles"
    )

    val particles = remember {
        List(8) {
            floatArrayOf(
                Math.random().toFloat(),
                (Math.random() * 0.5 + 0.2).toFloat(),
                (Math.random() * 2f + 1f).toFloat(),
                (Math.random() * 0.025f + 0.005f).toFloat()
            )
        }
    }

    val isGlass      = LocalFxzTheme.current.mode == ThemeMode.GLASSMORPHISM
    val accent       = LocalFxzTheme.current.accent


    BackHandler(enabled = true) { onClose() }

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onUserInteraction() },
                    onLongPress = {
                        onThemeRotate()
                        onUserInteraction()
                    }
                )
            }
    ) {
        key(currentSong.id) {
            if (currentSong.coverUrl != null) {
                AsyncImage(
                    model = buildCoverRequest(LocalContext.current, currentSong.coverUrl),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().scale(pulseScale).blur(70.dp).graphicsLayer { alpha = if (isGlass) 0.35f else 0.5f }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().scale(pulseScale).background(Brush.linearGradient(colors = currentSong.albumArt)).blur(70.dp).graphicsLayer { alpha = if (isGlass) 0.35f else 0.5f })
            }
        }

        if (isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    val x = p[0] * size.width
                    val speed = p[1]
                    val radius = p[2]
                    val alpha = p[3]
                    val y = size.height + radius - ((particleTime * speed * 60f) % (size.height + radius * 2f))
                    drawCircle(
                        color = accent.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }
        }

        if (isGlass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = showUi, enter = fadeIn(tween(300)), exit = fadeOut(tween(800))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BouncyIconButton(icon = Icons.Filled.KeyboardArrowDown, tint = Color.White, onClick = { onUserInteraction(); onClose() })
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("REPRODUCIENDO", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        if (currentSong.album.isNotBlank() && currentSong.album != "Unknown Album") {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onUserInteraction(); onAlbumClick() }
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Album, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                                Text(currentSong.album, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BouncyIconButton(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            tint = if (showQueue) LocalFxzTheme.current.accent else Color.White,
                            onClick = { onUserInteraction(); showQueue = !showQueue; showLyrics = false }
                        )
                        BouncyIconButton(
                            icon = if (showLyrics) Icons.Filled.MusicNote else Icons.Filled.MicNone,
                            tint = if (showLyrics) LocalFxzTheme.current.accent else Color.White,
                            onClick = { onUserInteraction(); showLyrics = !showLyrics; showQueue = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showQueue) {
                QueuePanel(
                    queue = queue,
                    currentSong = currentSong,
                    onPlaySong = { onUserInteraction(); onPlaySongFromQueue(it) },
                    modifier = Modifier.weight(3f).fillMaxWidth()
                )
            } else if (showLyrics) {
                LyricsPanel(lyricsViewModel = lyricsViewModel, modifier = Modifier.weight(3f).fillMaxWidth())
            } else {
                Box(
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onUserInteraction() }
                        )
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(320.dp).scale(coverScale).blur(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape))

                    AnimatedContent(
                        targetState  = currentSong.id,
                        transitionSpec = {
                            val dir = swipeDirection
                            swipeDirection = 0
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> if (dir >= 0) fullWidth else -fullWidth },
                                animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(tween(200))) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> if (dir >= 0) -fullWidth else fullWidth },
                                        animationSpec  = tween(250, easing = FastOutSlowInEasing)
                                    ) + fadeOut(tween(150)))
                        },
                        label = "cover_slide"
                    ) { songId ->
                        key(songId) {}
                        val pinchScale = remember { mutableFloatStateOf(1f) }
                        val transformState = rememberTransformableState { zoomChange, _, _ ->
                            pinchScale.floatValue = (pinchScale.floatValue * zoomChange).coerceIn(0.5f, 1.5f)
                            if (pinchScale.floatValue < 0.65f) onClose()
                            onUserInteraction()
                        }
                        var likeHeartVisible by remember { mutableStateOf(false) }

                        Box {
                            var totalDragX by remember(songId) { mutableFloatStateOf(0f) }
                            var totalDragY by remember(songId) { mutableFloatStateOf(0f) }

                            Card(
                                modifier = Modifier
                                    .size(340.dp)
                                    .scale(coverScale * pinchScale.floatValue)
                                    .transformable(transformState)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                totalDragX = 0f
                                                totalDragY = 0f
                                                onUserInteraction()
                                            },
                                            onDragEnd = {
                                                val absX = abs(totalDragX)
                                                val absY = abs(totalDragY)

                                                when {
                                                    absX > absY && totalDragX > 120f -> { swipeDirection = -1; onPrevious() }
                                                    absX > absY && totalDragX < -120f -> { swipeDirection = 1; onNext() }
                                                    absY > absX && totalDragY < -120f -> {
                                                        showLyrics = true
                                                        showQueue = false
                                                        onUserInteraction()
                                                    }
                                                    absY > absX && totalDragY > 170f -> onClose()
                                                }
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            onUserInteraction()
                                            totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { onUserInteraction() },
                                            onDoubleTap = {
                                                swipeDirection = 1
                                                onNext()
                                                onUserInteraction()
                                            }
                                        )
                                    },
                                shape     = RoundedCornerShape(32.dp),
                                elevation = CardDefaults.cardElevation(coverElevation.dp)
                            ) {
                                if (currentSong.coverUrl != null) {
                                    AsyncImage(
                                        model = buildCoverRequest(LocalContext.current, currentSong.coverUrl),
                                        contentDescription = null,
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = currentSong.albumArt)))
                                }
                            }

                            if (likeHeartVisible) {
                                LaunchedEffect(Unit) {
                                    delay(700)
                                    likeHeartVisible = false
                                }
                                Box(
                                    modifier = Modifier
                                        .size(340.dp)
                                        .scale(coverScale * pinchScale.floatValue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint     = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(visible = showUi, enter = fadeIn(tween(300)), exit = fadeOut(tween(800))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    key(currentSong.id) {
                        Text(currentSong.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentSong.artist, color = Color.White.copy(alpha = 0.8f), fontSize = 17.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            SpeedChip(speed = playbackSpeed, onClick = { onUserInteraction(); showSpeedMenu = true })
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                modifier = Modifier.background(Color(0xFF1E1E1E))
                            ) {
                                PLAYBACK_SPEEDS.forEachIndexed { i, speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                PLAYBACK_SPEED_LABELS[i],
                                                color = if (speed == playbackSpeed) LocalFxzTheme.current.accent else Color.White,
                                                fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = { onUserInteraction(); onSpeedChange(speed); showSpeedMenu = false }
                                    )
                                }
                            }
                        }
                        AudioOutputBar(onClick = { onUserInteraction() })
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FxzProgressBar(
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = { onUserInteraction(); onSeek(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BouncyIconButton(
                            icon    = Icons.Filled.Shuffle,
                            tint    = if (isShuffleEnabled) LocalFxzTheme.current.accent else Color.White.copy(alpha = 0.4f),
                            onClick = { onUserInteraction(); onToggleShuffle() }
                        )
                        BouncyIconButton(icon = Icons.Filled.SkipPrevious, tint = Color.White, onClick = { onUserInteraction(); onPrevious() })
                        val playInteraction = remember { MutableInteractionSource() }
                        val isPlayPressed by playInteraction.collectIsPressedAsState()

                        Box(contentAlignment = Alignment.Center) {
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .scale(playPulseScale)
                                        .clip(CircleShape)
                                        .background(LocalFxzTheme.current.accent.copy(alpha = playPulseAlpha))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .scale(if (isPlayPressed) 0.85f else 1f)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable(interactionSource = playInteraction, indication = null) { onUserInteraction(); onPlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(targetState = isPlaying, transitionSpec = { scaleIn() togetherWith scaleOut() }, label = "play_pause_big") { playing ->
                                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                        BouncyIconButton(icon = Icons.Filled.SkipNext, tint = Color.White, onClick = { onUserInteraction(); onNext() })
                        BouncyIconButton(
                            icon = when (repeatMode) {
                                MusicPlayerViewModel.RepeatMode.ONE  -> Icons.Filled.RepeatOne
                                else                                 -> Icons.Filled.Repeat
                            },
                            tint = when (repeatMode) {
                                MusicPlayerViewModel.RepeatMode.NONE -> Color.White.copy(alpha = 0.4f)
                                else                                 -> LocalFxzTheme.current.accent
                            },
                            onClick = { onUserInteraction(); onToggleRepeat() }
                        )
                    }

                    val likeInteraction = remember { MutableInteractionSource() }
                    val isLikePressed   by likeInteraction.collectIsPressedAsState()
                    val likeScale       by animateFloatAsState(
                        targetValue   = if (isLikePressed) 0.75f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label         = "like_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(likeScale)
                            .clip(CircleShape)
                            .background(if (currentSong.isLiked) LocalFxzTheme.current.accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                            .clickable(interactionSource = likeInteraction, indication = null) { onUserInteraction(); onToggleLike() },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState   = currentSong.isLiked,
                            transitionSpec = { scaleIn() togetherWith scaleOut() },
                            label         = "like_icon"
                        ) { liked ->
                            Icon(
                                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint     = if (liked) LocalFxzTheme.current.accent else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun AudioVisualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    accent: Color = LocalFxzTheme.current.accent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val barHeights = (0 until barCount).map { i ->
        key(i) {
            val duration = 280 + i * 90
            infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue  = if (isPlaying) 1f else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing, delayMillis = i * 55),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barHeights.forEachIndexed { i, heightFraction ->
            val h by heightFraction
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .height((h * 100).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(if (i % 2 == 0) accent else accent.copy(alpha = 0.65f))
            )
        }
    }
}

@Composable
fun SpeedChip(speed: Float, onClick: () -> Unit) {
    val label      = PLAYBACK_SPEED_LABELS.getOrElse(PLAYBACK_SPEEDS.indexOfFirst { it == speed }.coerceAtLeast(0)) { "1x" }
    val isModified = speed != 1.0f
    val accent     = LocalFxzTheme.current.accent
    val chipColor  = if (isModified) accent else Color.White.copy(alpha = 0.7f)
    Row(
        modifier = Modifier
            .border(1.dp, if (isModified) accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isModified) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("⚡", fontSize = 10.sp)
        Text(label, color = chipColor, fontSize = 11.sp, fontWeight = if (isModified) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun LyricsPanel(lyricsViewModel: LyricsViewModel, modifier: Modifier = Modifier) {
    val listState    = rememberLazyListState()
    val currentIndex = lyricsViewModel.currentLineIndex
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.animateScrollToItem(index = (currentIndex - 2).coerceAtLeast(0))
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val state = lyricsViewModel.lyricsState) {
            is LyricsState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎵", fontSize = 32.sp); Spacer(modifier = Modifier.height(12.dp))
                Text("Buscando letra...", color = Color.Gray, fontSize = 14.sp)
            }
            is LyricsState.Synced -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(state.lines) { index, line ->
                    val isActive = index == currentIndex
                    val isPast   = index < currentIndex
                    val lineScale by animateFloatAsState(targetValue = if (isActive) 1.06f else 1f, animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "line_scale")
                    Text(text = line.text.ifEmpty { "♪" }, color = when { isActive -> Color.White; isPast -> Color.White.copy(alpha = 0.3f); else -> Color.White.copy(alpha = 0.55f) }, fontSize = if (isActive) 26.sp else 20.sp, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal, textAlign = TextAlign.Center, lineHeight = 32.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).scale(lineScale))
                }
            }
            is LyricsState.Plain -> LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                item { Text(state.text, color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 30.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
            }
            is LyricsState.Instrumental -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎸", fontSize = 40.sp); Spacer(modifier = Modifier.height(12.dp))
                Text("Pista instrumental", color = Color.Gray, fontSize = 16.sp)
            }
            is LyricsState.NotFound -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎤", fontSize = 40.sp); Spacer(modifier = Modifier.height(12.dp))
                Text("Letra no encontrada", color = Color.Gray, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("LRCLIB no tiene esta canción aún", color = Color.Gray.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            else -> {}
        }
    }
}

@Composable
fun QueuePanel(
    queue: List<Song>,
    currentSong: Song,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalFxzTheme.current.accent
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        item {
            Text(
                "Cola de reproducción",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        itemsIndexed(queue) { index, song ->
            val isCurrent = song.id == currentSong.id
            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (isPressed) 0.97f else 1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isCurrent) accent.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .clickable(interactionSource = interaction, indication = null) { onPlaySong(song) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState  = index + 1,
                    transitionSpec = {
                        (slideInVertically(animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) { height -> -height } +
                                fadeIn()) togetherWith (slideOutVertically { height -> height } + fadeOut())
                    },
                    label = "queue_num_$index"
                ) { num ->
                    Text(
                        "$num",
                        color = if (isCurrent) accent else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                    if (isCurrent) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AudioVisualizerBars(isPlaying = true, barCount = 3, accent = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = if (isCurrent) accent else Color.White,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(song.artist, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatTime(song.duration), color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun FxzProgressBar(
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalFxzTheme.current.accent
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableIntStateOf(currentPosition) }
    var barWidth by remember { mutableIntStateOf(0) }

    val currentPositionState by rememberUpdatedState(currentPosition)
    val onSeekState by rememberUpdatedState(onSeek)

    val displayPosition = if (isDragging) dragPosition else currentPosition
    val progress = if (duration > 0) (displayPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "track_height"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 18.dp else 14.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumb_size"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { barWidth = it.width }
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0 && barWidth > 0) {
                            val seekPos = ((offset.x / barWidth) * duration).toInt().coerceIn(0, duration)
                            onSeekState(seekPos)
                        }
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true; dragPosition = currentPositionState },
                        onDragEnd = { isDragging = false; onSeekState(dragPosition) },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmountX ->
                        change.consume()
                        if (duration > 0 && barWidth > 0) {
                            val delta = (dragAmountX / barWidth * duration).toInt()
                            dragPosition = (dragPosition + delta).coerceIn(0, duration)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.7f), accent)
                        )
                    )
                    .align(Alignment.CenterStart)
            )
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(trackHeight)
                        .align(Alignment.CenterStart)
                        .blur(8.dp)
                        .background(accent.copy(alpha = 0.4f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, accent, CircleShape)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(displayPosition),
                color = if (isDragging) accent else Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                formatTime(duration),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SongPreviewOverlay(song: Song, modifier: Modifier = Modifier, progress: Float = 0f, onDismiss: () -> Unit) {
    val accent = LocalFxzTheme.current.accent
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AudioVisualizerBars(isPlaying = progress > 0f, barCount = 3, accent = accent, modifier = Modifier.width(24.dp).height(16.dp))
                            Text("Vista previa · 5s", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), accent)))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioOutputBar(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val manager = remember { AudioOutputManager(context) }
    val accent = LocalFxzTheme.current.accent

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

    val chipColor = if (isNonDefault) accent else Color.White.copy(alpha = 0.6f)
    val borderMod = if (isBt)
        Modifier.border(1.dp, accent.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
    else
        Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))

    Row(
        modifier = modifier
            .then(borderMod)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isNonDefault) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.07f))
            .clickable {
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
            contentDescription = null,
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

@Composable
fun MiniPlayerWithVisualizer(
    currentSong: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    onSwipeToDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentPosition: Int = 0,
    duration: Int = 0,
    onNext: () -> Unit = {},
    onSwipeDown: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingHorizontal by remember { mutableStateOf(false) }
    var isDraggingDown by remember { mutableStateOf(false) }
    var dragStarted by remember { mutableStateOf(false) }

    val dismissThreshold = 250f
    val alphaValue = when {
        isDraggingHorizontal -> (1f - (abs(offsetX) / (dismissThreshold * 1.8f))).coerceIn(0.2f, 1f)
        isDraggingDown -> (1f - (offsetY / 300f)).coerceIn(0.2f, 1f)
        else -> 1f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        currentSong.albumArt.firstOrNull()?.copy(alpha = glowAlpha * 0.4f)
                            ?: LocalFxzTheme.current.accent.copy(alpha = glowAlpha * 0.4f)
                    )
                    .blur(16.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .graphicsLayer {
                    translationX = if (isDraggingHorizontal) offsetX else 0f
                    translationY = if (isDraggingDown) offsetY else 0f
                    alpha = alphaValue
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            offsetX = 0f; offsetY = 0f
                            dragStarted = true
                            isDraggingHorizontal = false; isDraggingDown = false
                        },
                        onDragEnd = {
                            when {
                                isDraggingHorizontal && abs(offsetX) > dismissThreshold -> onSwipeToDismiss()
                                isDraggingDown && offsetY > 120f -> { onSwipeDown() }
                            }
                            offsetX = 0f; offsetY = 0f
                            isDraggingHorizontal = false; isDraggingDown = false
                            dragStarted = false
                        },
                        onDragCancel = {
                            offsetX = 0f; offsetY = 0f
                            isDraggingHorizontal = false; isDraggingDown = false
                            dragStarted = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val absX = abs(dragAmount.x)
                        val absY = abs(dragAmount.y)
                        if (!isDraggingHorizontal && !isDraggingDown && dragStarted) {
                            when {
                                absX > absY && absX > 5f -> isDraggingHorizontal = true
                                absY > absX && dragAmount.y > 5f -> isDraggingDown = true
                            }
                        }
                        if (isDraggingHorizontal) offsetX += dragAmount.x
                        if (isDraggingDown) offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f)
                    }
                }
                .scale(if (isPressed) 0.96f else 1f)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (LocalFxzTheme.current.mode == ThemeMode.GLASSMORPHISM)
                    Color(0x99121212) else Color(0xD8121212)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isPlaying) {
                    val accent = LocalFxzTheme.current.accent
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        for (i in 0..2) {
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, h / 2f)
                            val freq = 1.2f + (i * 0.4f)
                            val amp = h * 0.2f
                            val phase = wavePhase * (if (i % 2 == 0) 1f else -1f) + (i * Math.PI.toFloat() / 3f)
                            var x = 0f
                            while (x <= w) {
                                val y = h / 2f + kotlin.math.sin((x / w * freq * 2f * Math.PI.toFloat()) + phase) * amp
                                path.lineTo(x, y)
                                x += 5f
                            }
                            drawPath(
                                path = path,
                                color = accent.copy(alpha = 0.15f - (i * 0.04f)),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors = currentSong.albumArt)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentSong.coverUrl != null) {
                            AsyncImage(
                                model = buildCoverRequest(LocalContext.current, currentSong.coverUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF121212)))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentSong.title, color = Color.White,
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Text(currentSong.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }


                    val btnInteraction = remember { MutableInteractionSource() }
                    val isBtnPressed by btnInteraction.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .scale(if (isBtnPressed) 0.85f else 1f)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(interactionSource = btnInteraction, indication = null, onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { scaleIn() togetherWith scaleOut() },
                            label = "play_pause"
                        ) { playing ->
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                val progress = if (duration > 0) {
                    (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter),
                    color = LocalFxzTheme.current.accent,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }
        }
    }
}