package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.Song
import com.fxzmusic.app.util.buildCoverRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullPlayerCoverArt(
    currentSong: Song,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onToggleLike: () -> Unit,
    onShowLyrics: () -> Unit,
    onUserInteraction: () -> Unit,
    onTap: () -> Unit = onUserInteraction,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    albumColors: List<Color> = currentSong.albumArt,
    modifier: Modifier = Modifier
) {
    var swipeDirection by remember { mutableIntStateOf(0) }
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val coverSizeDp = ((screenHeightDp * 0.38f).coerceIn(220f, 360f)).dp

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "cover_scale"
    )

    val coverEntranceScale = remember { Animatable(0.85f) }
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentSong.id) {
        dragOffsetX.snapTo(0f)
        dragOffsetY.snapTo(0f)
        coverEntranceScale.snapTo(0.85f)
        coverEntranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier.scale(coverEntranceScale.value).pointerInput(Unit) {
            detectTapGestures(
                onTap = { onTap() }
            )
        },
        contentAlignment = Alignment.Center
    ) {
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
                pinchScale.floatValue = (pinchScale.floatValue * zoomChange).coerceIn(0.8f, 1.3f)
                onUserInteraction()
            }
            var likeHeartVisible by remember { mutableStateOf(false) }

            Box {
                var totalDragX by remember(songId) { mutableFloatStateOf(0f) }
                var totalDragY by remember(songId) { mutableFloatStateOf(0f) }
                var showSeekOverlay by remember(songId) { mutableStateOf(false) }
                var seekText by remember(songId) { mutableStateOf("") }
                var seekIconIsForward by remember(songId) { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .size(coverSizeDp)
                        .scale(coverScale * pinchScale.floatValue)
                        .graphicsLayer {
                            translationX = dragOffsetX.value
                            translationY = dragOffsetY.value
                            rotationZ = (dragOffsetX.value / 40f).coerceIn(-10f, 10f)
                        }
                        .transformable(transformState)
                        .pointerInput(songId) {
                            detectDragGestures(
                                onDragStart = {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                    onUserInteraction()
                                },
                                onDragEnd = {
                                    val absX = abs(totalDragX)
                                    val absY = abs(totalDragY)

                                    coroutineScope.launch {
                                        when {
                                            absX > absY && totalDragX > 120f -> {
                                                swipeDirection = -1
                                                dragOffsetX.snapTo(0f)
                                                dragOffsetY.snapTo(0f)
                                                onPrevious()
                                            }
                                            absX > absY && totalDragX < -120f -> {
                                                swipeDirection = 1
                                                dragOffsetX.snapTo(0f)
                                                dragOffsetY.snapTo(0f)
                                                onNext()
                                            }
                                            absY > absX && totalDragY < -120f -> {
                                                dragOffsetX.snapTo(0f)
                                                dragOffsetY.snapTo(0f)
                                                onShowLyrics()
                                                onUserInteraction()
                                            }
                                            absY > absX && totalDragY > 170f -> {
                                                dragOffsetX.snapTo(0f)
                                                dragOffsetY.snapTo(0f)
                                                onClose()
                                            }
                                            else -> {
                                                launch {
                                                    dragOffsetX.animateTo(
                                                        0f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }
                                                launch {
                                                    dragOffsetY.animateTo(
                                                        0f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        launch {
                                            dragOffsetX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                        launch {
                                            dragOffsetY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                onUserInteraction()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                coroutineScope.launch {
                                    dragOffsetX.snapTo(totalDragX)
                                    dragOffsetY.snapTo(totalDragY)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onTap() },
                                onDoubleTap = { tapOffset ->
                                    val tapFraction = tapOffset.x / size.width
                                    val seekDelta =
                                        if (tapFraction < 0.45f) -5
                                        else if (tapFraction > 0.55f) 5
                                        else return@detectTapGestures

                                    val newPos = (currentPosition + seekDelta).coerceIn(0, duration)
                                    onSeek(newPos)
                                    onUserInteraction()
                                    showSeekOverlay = true
                                    seekText = "${if (seekDelta > 0) "+" else ""}$seekDelta s"
                                    seekIconIsForward = seekDelta > 0
                                }
                            )
                        },
                    shape     = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp, pressedElevation = 12.dp)
                ) {
                    with(sharedTransitionScope) {
                        if (currentSong.coverUrl != null) {
                            AsyncImage(
                                model = buildCoverRequest(LocalContext.current, currentSong.coverUrl, maxSize = 1024),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "cover-art-${currentSong.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(colors = currentSong.albumArt))
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "cover-art-${currentSong.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            )
                        }
                    }
                }

                if (likeHeartVisible) {
                    LaunchedEffect(Unit) {
                        delay(700)
                        likeHeartVisible = false
                    }
                    Box(
                        modifier = Modifier
                            .size(coverSizeDp)
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

                if (showSeekOverlay) {
                    LaunchedEffect(Unit) {
                        delay(1500)
                        showSeekOverlay = false
                    }
                    Box(
                        modifier = Modifier
                            .size(coverSizeDp)
                            .scale(coverScale * pinchScale.floatValue),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = if (seekIconIsForward) 80.dp else (-80).dp)
                                .size(48.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (seekIconIsForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    seekText,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
