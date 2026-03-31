package com.example.fxzmusic

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlin.math.abs
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    onPlaySong: (Song) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onShowFullPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSearchWithQuery: ((String) -> Unit)? = null,
    onRetry: () -> Unit = {}
) {
    val allSongs = libraryViewModel.allSongs

    val recentSongs = remember(allSongs) {
        allSongs.filter { it.lastPlayed > 0L }.sortedByDescending { it.lastPlayed }.take(10)
    }
    val topSongs = remember(allSongs) {
        allSongs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(15)
    }
    val albumCovers = remember(allSongs) {
        allSongs.groupBy { it.album }
            .map { (name, songs) -> name to songs.firstOrNull { it.coverUrl != null } }
            .sortedBy { it.first }
            .take(20)
    }

    val isFirstScan = libraryViewModel.isScanning && allSongs.isEmpty()
    val isEmpty = !libraryViewModel.isScanning && allSongs.isEmpty()

    if (isFirstScan) {
        Column(
            modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HomeHeader()
            Spacer(modifier = Modifier.weight(1f))
            ScanProgressCard(
                progress = libraryViewModel.scanProgress,
                total = libraryViewModel.scanTotal,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        return
    }

    if (isEmpty) {
        Column(
            modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HomeHeader()
            Spacer(modifier = Modifier.weight(1f))
            AnimatedEmptyState(onRetry)
            Spacer(modifier = Modifier.weight(1f))
        }
        return
    }

    var sectionVisible by remember { mutableStateOf(false) }
    LaunchedEffect(allSongs.isNotEmpty()) { if (allSongs.isNotEmpty()) { delay(50); sectionVisible = true } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            AnimatedVisibility(visible = sectionVisible, enter = fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) { it / 5 }) {
                HomeHeader()
            }
        }

        item {
            AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 80, useSpring = true)) {
                AnimatedSearchBar(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onSearchClick = onNavigateToSearch,
                    onSearchWithQuery = onNavigateToSearchWithQuery
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = libraryViewModel.isScanning,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(400))
            ) {
                ScanProgressCard(
                    progress = libraryViewModel.scanProgress,
                    total = libraryViewModel.scanTotal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        if (allSongs.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 160, useSpring = true)) {
                    SectionHeader(title = "Todas las canciones", onShowAllClick = {})
                }
            }
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 180, useSpring = true)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(allSongs.take(20)) { idx, song ->
                            AnimatedCardItem(
                                delayMs = 200 + (idx * 30),
                                modifier = Modifier.width(130.dp)
                            ) {
                                SongCard(song = song, onClick = { onPlaySong(song); onShowFullPlayer() })
                            }
                        }
                    }
                }
            }
        }

        if (topSongs.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 240, useSpring = true)) {
                    SectionHeader(title = "Mas escuchadas", onShowAllClick = {})
                }
            }
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 260, useSpring = true)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(topSongs) { idx, song ->
                            AnimatedCardItem(
                                delayMs = 280 + (idx * 25),
                                modifier = Modifier.width(130.dp)
                            ) {
                                SongCard(song = song, onClick = { onPlaySong(song); onShowFullPlayer() })
                            }
                        }
                    }
                }
            }
        }

        if (albumCovers.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 320, useSpring = true)) {
                    SectionHeader(title = "Albums", onShowAllClick = {})
                }
            }
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 340, useSpring = true)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(albumCovers) { idx, (name, song) ->
                            AnimatedCardItem(
                                delayMs = 360 + (idx * 25),
                                modifier = Modifier.width(120.dp)
                            ) {
                                AlbumCoverCard(
                                    name = name,
                                    coverUrl = song?.coverUrl,
                                    albumArt = song?.albumArt ?: listOf(Color(0xFF4158D0), Color(0xFFC850C0)),
                                    onClick = { song?.let { onPlaySong(it); onShowFullPlayer() } }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (recentSongs.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 400, useSpring = true)) {
                    SectionHeader(title = "Reproducido Recientemente", onShowAllClick = {})
                }
            }
            item {
                AnimatedVisibility(visible = sectionVisible, enter = slideInFromBottomWithScale(delayMillis = 440, useSpring = true)) {
                    val pagerState = rememberPagerState(pageCount = { recentSongs.size })
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 64.dp),
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    ) { page ->
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val absOffset = abs(pageOffset)
                        AnimatedRecentCard(
                            song = recentSongs[page],
                            pageOffset = pageOffset,
                            absOffset = absOffset,
                            onPlaySong = { onPlaySong(recentSongs[page]); onShowFullPlayer() }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AnimatedRecentCard(
    song: Song,
    pageOffset: Float,
    absOffset: Float,
    onPlaySong: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                scaleX = 1f - (absOffset * 0.15f).coerceIn(0f, 1f)
                scaleY = 1f - (absOffset * 0.15f).coerceIn(0f, 1f)
                alpha = 1f - (absOffset * 0.3f).coerceIn(0f, 1f)
                rotationY = (pageOffset * 25f).coerceIn(-45f, 45f)
                cameraDistance = 8 * density
            }
            .clickable { onPlaySong() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(if (pageOffset == 0f) 12.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = song.albumArt))
        ) {
            if (song.coverUrl != null) {
                AsyncImage(
                    model = buildCoverRequest(LocalContext.current, song.coverUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedEmptyState(onRetry: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    AnimatedVisibility(
        visible = true,
        enter = popInAnimation(delayMillis = 200)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(LocalFxzTheme.current.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = LocalFxzTheme.current.accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "No se encontraron canciones",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Asegurate de tener permiso de almacenamiento\ny archivos de musica en el dispositivo",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalFxzTheme.current.accent)
                    .clickable { onRetry() }
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text("Reintentar escaneo", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ScanProgressCard(progress: Int, total: Int, modifier: Modifier = Modifier) {
    val accent = LocalFxzTheme.current.accent
    val fraction = if (total > 0) progress.toFloat() / total.toFloat() else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = pulse))
                )
                Text(
                    if (total > 0) "Escaneando musica... $progress / $total"
                    else "Escaneando musica...",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = accent,
                trackColor = Color(0xFF2A2A2A),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "shimmer_x"
    )
    val accent = LocalFxzTheme.current.accent
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF181818),
                        Color(0xFF222222),
                        accent.copy(alpha = 0.06f),
                        Color(0xFF2C2C2C),
                        Color(0xFF222222),
                        Color(0xFF181818)
                    ),
                    start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(shimmerX + 400f, 220f)
                )
            )
    )
}

@Composable
fun ShimmerSongCard() {
    Column(modifier = Modifier.width(130.dp)) {
        ShimmerBox(modifier = Modifier.size(130.dp), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp), shape = RoundedCornerShape(6.dp))
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(10.dp), shape = RoundedCornerShape(5.dp))
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    var imageLoaded by remember { mutableStateOf(false) }
    val accent = LocalFxzTheme.current.accent

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "song_scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0f,
        animationSpec = tween(120),
        label = "song_glow"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 8.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "song_elevation"
    )

    Column(
        modifier = Modifier
            .width(130.dp)
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Card(
            modifier = Modifier.size(130.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(elevation)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageLoaded) {
                    ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(song.albumArt))
                        .graphicsLayer { alpha = if (imageLoaded || song.coverUrl == null) 1f else 0f }
                )
                if (song.coverUrl != null) {
                    AsyncImage(
                        model = buildCoverRequest(LocalContext.current, song.coverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (imageLoaded) 1f else 0f },
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Success) {
                                imageLoaded = true
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) { imageLoaded = true }
                }
                if (glowAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accent.copy(alpha = glowAlpha * 0.3f))
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(song.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AlbumCoverCard(
    name: String,
    coverUrl: String?,
    albumArt: List<Color>,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    var imageLoaded by remember { mutableStateOf(false) }
    val accent = LocalFxzTheme.current.accent

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "album_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 6.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "album_elevation"
    )

    Column(
        modifier = Modifier
            .width(120.dp)
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(elevation)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageLoaded) {
                    ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(14.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(albumArt))
                        .graphicsLayer { alpha = if (imageLoaded || coverUrl == null) 1f else 0f }
                )
                if (coverUrl != null) {
                    AsyncImage(
                        model = buildCoverRequest(LocalContext.current, coverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (imageLoaded) 1f else 0f },
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Success) {
                                imageLoaded = true
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) { imageLoaded = true }
                }
                if (isPressed) {
                    Box(modifier = Modifier.fillMaxSize().background(accent.copy(alpha = 0.15f)))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun HomeHeader() {
    val accent = LocalFxzTheme.current.accent
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot_scale"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fxz Music", color = accent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text("v1.0", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    val theme = LocalFxzTheme.current
    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    fun submitSearch() {
        keyboard?.hide()
        if (text.isNotBlank() && onSearchWithQuery != null) {
            onSearchWithQuery(text)
        } else {
            onSearchClick()
        }
        text = ""
        isFocused = false
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceVariant.copy(alpha = if (isGlass) 0.72f else 1f)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = if (isFocused) theme.accent else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Buscar en el dispositivo...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = theme.accent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() })
            )
            AnimatedVisibility(visible = text.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(theme.surface)
                        .clickable { text = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}