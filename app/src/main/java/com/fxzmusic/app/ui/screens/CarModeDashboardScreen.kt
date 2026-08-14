package com.fxzmusic.app.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.os.BatteryManager
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.font.FontFamily
import com.fxzmusic.app.util.ColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fxzmusic.app.FxzApplication
import com.fxzmusic.app.R
import com.fxzmusic.app.data.AlbumGroup
import com.fxzmusic.app.data.Playlist
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.util.ConnectivityObserver
import com.fxzmusic.app.util.NetworkStatus
import com.fxzmusic.app.util.buildCoverRequest
import com.fxzmusic.app.viewmodel.DownloadViewModel
import com.fxzmusic.app.viewmodel.LibraryViewModel
import com.fxzmusic.app.viewmodel.MusicPlayerViewModel
import com.fxzmusic.app.viewmodel.PlaybackSettingsViewModel
import com.fxzmusic.app.viewmodel.YouTubeMusicViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.fxzmusic.app.service.toSong
import com.fxzmusic.app.viewmodel.ChartsUiState
import com.fxzmusic.app.viewmodel.ExploreUiState
import com.fxzmusic.app.viewmodel.SearchUiState
import com.fxzmusic.innertube.models.SongItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatCarTime(seconds: Int): String {
    val totalSec = seconds.coerceAtLeast(0)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

// -------------------------------------------------------------------------------------------------
// HUMAN-CRAFT DESIGN TOKENS & HIGH-END CAR COMPONENTS
// -------------------------------------------------------------------------------------------------

object CarCraftTheme {
    // Fondos satinados
    val SurfaceDeep = Color(0xFF07080B)
    val SurfaceCard = Color(0xFF0B0D12)
    val SurfaceElevated = Color(0xFF13161F)
    val SurfaceHighlight = Color(0xFF1B1F2B)

    // Micro-bordes CNC
    val BorderSubtle = Color(0x1AFFFFFF) // 10% blanco
    val BorderMedium = Color(0x26FFFFFF) // 15% blanco
    val BorderHigh = Color(0x33FFFFFF)   // 20% blanco

    // Textos
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF) // 70% blanco
    val TextTertiary = Color(0x80FFFFFF)  // 50% blanco
    val TextMuted = Color(0x4DFFFFFF)     // 30% blanco

    // Tipografía monoespaciada tabular (Zero visual jitter) y editorial
    val MonoTimerStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = Color.White
    )
    val EditorialHeaderStyle = TextStyle(
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        color = Color.White
    )
}

@Composable
fun CarCinematicAmbientBackground(
    currentSong: Song?,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(accent) }

    LaunchedEffect(currentSong?.id, currentSong?.coverUrl, currentSong?.filePath) {
        if (currentSong != null) {
            val target = currentSong.coverUrl?.takeIf { it.isNotEmpty() } ?: currentSong.filePath
            val extracted = withContext(Dispatchers.Default) {
                ColorExtractor.extractDominantColors(context, target)
            }
            if (extracted.isNotEmpty()) {
                dominantColor = extracted.first()
            } else {
                dominantColor = accent
            }
        } else {
            dominantColor = accent
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "car_ambient_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val radialBrush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.28f),
                        animatedColor.copy(alpha = 0.09f),
                        CarCraftTheme.SurfaceDeep
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.30f),
                    radius = (size.maxDimension * 0.75f).coerceAtLeast(1f)
                )
                val linearOverlay = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CarCraftTheme.SurfaceDeep.copy(alpha = 0.45f),
                        CarCraftTheme.SurfaceDeep
                    )
                )
                onDrawBehind {
                    drawRect(color = CarCraftTheme.SurfaceDeep)
                    drawRect(brush = radialBrush)
                    drawRect(brush = linearOverlay)
                }
            }
    ) {
        content()
    }
}

@Composable
fun CarTouchProgressBar(
    currentPosition: Int,
    duration: Int,
    accent: Color,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    val safeDuration = duration.coerceAtLeast(1)
    val currentSec = if (isDragging) (dragProgressFraction * safeDuration).toInt() else currentPosition.coerceIn(0, safeDuration)
    val displayFraction = if (isDragging) dragProgressFraction else (currentPosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 26.dp else 16.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "car_progress_thumb_size"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Barra táctil con hitbox ergonómica de 48dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width.toFloat()
                            if (width > 0) {
                                isDragging = true
                                dragProgressFraction = (offset.x / width).coerceIn(0f, 1f)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tryAwaitRelease()
                                isDragging = false
                                onSeek((dragProgressFraction * safeDuration).toInt())
                            }
                        }
                    )
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val width = size.width.toFloat()
                            if (width > 0) {
                                dragProgressFraction = (offset.x / width).coerceIn(0f, 1f)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgressFraction * safeDuration).toInt())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat()
                            if (width > 0) {
                                dragProgressFraction = (change.position.x / width).coerceIn(0f, 1f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth

            // Track Inactivo (Fondo satinado con microborde CNC)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, CarCraftTheme.BorderSubtle, RoundedCornerShape(6.dp))
            )

            // Track Activo (Color acento o gradiente vibrante)
            val activeWidth = totalWidth * displayFraction
            Box(
                modifier = Modifier
                    .width(activeWidth.coerceAtLeast(0.dp))
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.85f), accent)
                        )
                    )
            )

            // Thumb Elástico
            val thumbOffset = ((totalWidth - thumbSize) * displayFraction).coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .padding(start = thumbOffset)
                    .size(thumbSize)
                    .shadow(elevation = if (isDragging) 8.dp else 4.dp, shape = CircleShape, spotColor = accent)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, accent, CircleShape)
            )
        }

        // Tiempos Monoespaciados Tabulares - Zero Visual Jitter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(62.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = formatCarTime(currentSec),
                    style = CarCraftTheme.MonoTimerStyle,
                    color = if (isDragging) accent else Color.White
                )
            }

            Box(modifier = Modifier.width(62.dp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = formatCarTime(safeDuration),
                    style = CarCraftTheme.MonoTimerStyle,
                    color = CarCraftTheme.TextSecondary
                )
            }
        }
    }
}

@Composable
fun CarHardwareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp,
    iconSize: Dp,
    tint: Color,
    backgroundColor: Color = CarCraftTheme.SurfaceElevated,
    borderColor: Color = CarCraftTheme.BorderMedium,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "car_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CarHardwarePlayPauseButton(
    isPlaying: Boolean,
    accent: Color,
    size: Dp = 92.dp,
    iconSize: Dp = 48.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "car_play_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 8.dp else 20.dp,
                shape = CircleShape,
                spotColor = accent.copy(alpha = 0.7f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent,
                        accent.copy(alpha = 0.85f),
                        accent.copy(alpha = 0.70f)
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.40f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(140)))
                    .togetherWith(scaleOut(tween(90)) + fadeOut(tween(90)))
            },
            label = "car_play_pause_morph"
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) stringResource(R.string.car_pause) else stringResource(R.string.car_play),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun CarLiveWaveVisualizer(
    isPlaying: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "car_wave_transition")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "car_wave_bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "car_wave_bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "car_wave_bar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(540, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "car_wave_bar4"
    )

    val targetHeights = listOf(h1, h2, h3, h4)

    Row(
        modifier = modifier
            .height(26.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        targetHeights.forEachIndexed { index, rawHeight ->
            val animatedHeight by animateDpAsState(
                targetValue = if (isPlaying) rawHeight.dp else 4.dp,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "car_wave_bar_anim_$index"
            )

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
    }
}

@Composable
fun CarUpNextHorizontalStrip(
    musicPlayerViewModel: MusicPlayerViewModel,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPlaylist = musicPlayerViewModel.playlist
    val currentSong = musicPlayerViewModel.currentSong

    val upNextSongs = remember(currentPlaylist, currentSong?.id) {
        val idx = if (currentSong != null) currentPlaylist.indexOfFirst { it.id == currentSong.id } else -1
        if (idx != -1 && idx + 1 < currentPlaylist.size) {
            currentPlaylist.drop(idx + 1).take(5)
        } else {
            emptyList<Song>()
        }
    }

    if (upNextSongs.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CarCraftTheme.SurfaceCard)
                .border(1.dp, CarCraftTheme.BorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.car_up_next_title),
                        style = CarCraftTheme.EditorialHeaderStyle,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = stringResource(R.string.car_song_count, upNextSongs.size),
                    color = CarCraftTheme.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(upNextSongs, key = { it.id }) { song ->
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val itemScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "up_next_scale"
                    )

                    Row(
                        modifier = Modifier
                            .scale(itemScale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarCraftTheme.SurfaceElevated)
                            .border(1.dp, CarCraftTheme.BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                musicPlayerViewModel.playSong(song, currentPlaylist, context)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = buildCoverRequest(context, song, maxSize = 120),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Column(
                            modifier = Modifier.width(110.dp)
                        ) {
                            Text(
                                text = song.title,
                                color = CarCraftTheme.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = CarCraftTheme.TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarEditorialPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "editorial_playlist_scale"
    )

    Card(
        modifier = modifier
            .width(168.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CarCraftTheme.SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarCraftTheme.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            playlist.coverColor.ifEmpty { listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = playlist.name,
                style = CarCraftTheme.EditorialHeaderStyle,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.car_song_count, playlist.songs.size),
                color = CarCraftTheme.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CarEditorialAlbumCard(
    album: AlbumGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "editorial_album_scale"
    )

    Card(
        modifier = modifier
            .width(158.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CarCraftTheme.SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarCraftTheme.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val firstSong = album.songs.firstOrNull()
            if (firstSong != null) {
                AsyncImage(
                    model = buildCoverRequest(context, firstSong, maxSize = 320),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CarCraftTheme.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = CarCraftTheme.TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = album.name,
                style = CarCraftTheme.EditorialHeaderStyle,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.car_song_count, album.songs.size),
                color = CarCraftTheme.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

enum class CarNavSection(
    val titleRes: Int,
    val icon: ImageVector
) {
    NOW_PLAYING(R.string.car_nav_now_playing, Icons.Filled.PlayCircle),
    SEARCH(R.string.car_nav_search, Icons.Filled.Search),
    LIBRARY(R.string.car_nav_library, Icons.Filled.LibraryMusic),
    RADIOS(R.string.car_nav_radios, Icons.Filled.GraphicEq),
    DOWNLOADS(R.string.car_nav_downloads, Icons.Filled.CloudDownload),
    VOICE(R.string.car_nav_voice, Icons.Filled.Mic)
}

@Composable
fun CarClock(modifier: Modifier = Modifier) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var currentTimeText by remember { mutableStateOf(timeFormatter.format(Date())) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val now = System.currentTimeMillis()
            currentTimeText = timeFormatter.format(Date(now))
            val delayMs = (1000L - (now % 1000L)).coerceAtLeast(100L)
            delay(delayMs)
        }
    }

    Text(
        text = currentTimeText,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

@Composable
private fun CarBatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isCharging) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = stringResource(R.string.car_battery_charging),
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = if (batteryLevel > 20) Icons.Filled.BatteryFull else Icons.Filled.BatteryAlert,
                contentDescription = null,
                tint = if (batteryLevel > 20) Color.White.copy(alpha = 0.8f) else Color(0xFFFF5252),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "$batteryLevel%",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CarConnectivityBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
            .border(
                1.dp,
                if (isOnline) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
        )
        Text(
            text = if (isOnline) stringResource(R.string.car_online_badge) else stringResource(R.string.car_offline_badge),
            color = if (isOnline) Color(0xFF34D399) else Color(0xFFF87171),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CarDataSaverChip(
    isDataSaverEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val bg = if (isDataSaverEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
    val borderColor = if (isDataSaverEnabled) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF3B82F6).copy(alpha = 0.35f)
    val contentColor = if (isDataSaverEnabled) Color(0xFF34D399) else Color(0xFF60A5FA)
    val label = if (isDataSaverEnabled) stringResource(R.string.car_data_saver_badge) else stringResource(R.string.car_hifi_badge)
    val icon = if (isDataSaverEnabled) "🍃" else "🚀"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 12.sp
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun launchNavigationApp(context: Context) {
    try {
        val gmmIntentUri = android.net.Uri.parse("google.navigation:q=&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        val wazeIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("waze://")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (wazeIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(wazeIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        val geoIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (geoIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(geoIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        val webMapsIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://maps.google.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webMapsIntent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.car_nav_app_not_found),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun CarTopHud(
    isOnline: Boolean,
    batteryLevel: Int,
    isCharging: Boolean,
    isDataSaverEnabled: Boolean,
    onDataSaverToggle: () -> Unit,
    onGpsClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CarClock()
            CarBatteryIndicator(batteryLevel = batteryLevel, isCharging = isCharging)
            CarConnectivityBadge(isOnline = isOnline)
            CarDataSaverChip(
                isDataSaverEnabled = isDataSaverEnabled,
                onToggle = onDataSaverToggle
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CarIconButton(
                icon = Icons.Filled.DirectionsCar,
                contentDescription = stringResource(R.string.car_nav_gps),
                size = 46.dp,
                iconSize = 24.dp,
                backgroundColor = Color.White.copy(alpha = 0.10f),
                tint = Color(0xFF38BDF8),
                onClick = onGpsClick
            )
            CarIconButton(
                icon = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.car_mode_voice_search),
                size = 46.dp,
                iconSize = 24.dp,
                backgroundColor = Color.White.copy(alpha = 0.10f),
                tint = Color.White,
                onClick = onVoiceClick
            )
            CarExitButton(onClick = onExitClick)
        }
    }
}

@Composable
private fun CarEdgeAdjustmentHud(
    percent: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14161C).copy(alpha = 0.94f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val icon = when {
                percent == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                percent < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                else -> Icons.AutoMirrored.Filled.VolumeUp
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.car_hud_volume, percent),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun CarSmartRoadTripHeroCard(
    onPlayMix: () -> Unit,
    trackCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onPlayMix),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF416C), Color(0xFF8A2387))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✨ SMART MIX",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = stringResource(R.string.car_smart_mix_tracks, trackCount),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = stringResource(R.string.car_smart_mix_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = stringResource(R.string.car_smart_mix_subtitle),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(8.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.car_smart_mix_title),
                        tint = Color(0xFF8A2387),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CarNavRail(
    selectedSection: CarNavSection,
    onSectionSelected: (CarNavSection) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(92.dp)
            .fillMaxHeight()
            .background(Color(0xFF08090A))
            .border(1.dp, Color.White.copy(alpha = 0.06f))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CarNavSection.entries.forEach { section ->
            val isSelected = section == selectedSection
            val bg = if (isSelected) accent.copy(alpha = 0.20f) else Color.Transparent
            val contentColor = if (isSelected) accent else Color.White.copy(alpha = 0.6f)

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .clickable { onSectionSelected(section) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = stringResource(section.titleRes),
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(section.titleRes),
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CarTopTabBar(
    selectedSection: CarNavSection,
    onSectionSelected: (CarNavSection) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF08090A))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarNavSection.entries.forEach { section ->
            val isSelected = section == selectedSection
            val bg = if (isSelected) accent.copy(alpha = 0.20f) else Color.Transparent
            val contentColor = if (isSelected) accent else Color.White.copy(alpha = 0.6f)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSectionSelected(section) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = stringResource(section.titleRes),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(section.titleRes),
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarModeDashboardScreen(
    musicPlayerViewModel: MusicPlayerViewModel,
    libraryViewModel: LibraryViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    settingsViewModel: PlaybackSettingsViewModel,
    onClose: () -> Unit,
    downloadViewModel: DownloadViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    var selectedSection by remember { mutableStateOf(CarNavSection.NOW_PLAYING) }

    // Keep screen on
    DisposableEffect(settingsViewModel.carModeKeepScreenOn) {
        val activity = context.findActivity()
        val window = activity?.window
        if (settingsViewModel.carModeKeepScreenOn && window != null) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } catch (_: Exception) {}
        }
        onDispose {
            try {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } catch (_: Exception) {}
        }
    }

    var showVolumeHud by remember { mutableStateOf(false) }
    var hudAutoHideTrigger by remember { mutableLongStateOf(0L) }
    var volumeDragAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(hudAutoHideTrigger) {
        if (showVolumeHud) {
            delay(1500)
            showVolumeHud = false
        }
    }

    // Battery state
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL ||
                            plugged != 0
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            val sticky = context.registerReceiver(batteryReceiver, filter)
            sticky?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL ||
                        plugged != 0
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                }
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }
    }

    // System Volume
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7)
    }
    val maxVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }

    DisposableEffect(context) {
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val vol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: return
                currentVolume = vol
            }
        }
        try {
            context.registerReceiver(volumeReceiver, filter)
        } catch (_: Exception) {}

        onDispose {
            try {
                context.unregisterReceiver(volumeReceiver)
            } catch (_: Exception) {}
        }
    }

    fun setSystemVolume(vol: Int) {
        val clamped = vol.coerceIn(0, maxVolume)
        currentVolume = clamped
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        } catch (_: Exception) {}
    }

    // Connectivity state
    val app = context.applicationContext as? FxzApplication
    val connectivityObserver = remember { app?.connectivity ?: ConnectivityObserver(context) }
    val networkStatus by connectivityObserver.status.collectAsState()
    val isOnline = networkStatus == NetworkStatus.CONNECTED

    // Voice search handler
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.car_mode_voice_searching, spokenText),
                    Toast.LENGTH_SHORT
                ).show()

                val matchedSong = libraryViewModel.allSongs.find {
                    it.title.contains(spokenText, ignoreCase = true) || it.artist.contains(spokenText, ignoreCase = true)
                }
                if (matchedSong != null) {
                    musicPlayerViewModel.playSong(matchedSong, libraryViewModel.allSongs, context)
                    selectedSection = CarNavSection.NOW_PLAYING
                } else {
                    youTubeViewModel.runSearch(spokenText)
                    selectedSection = CarNavSection.NOW_PLAYING
                }
            } else {
                Toast.makeText(context, context.getString(R.string.car_mode_voice_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchVoiceSearch = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.car_mode_voice_prompt))
            }
            voiceSearchLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.car_mode_voice_not_supported), Toast.LENGTH_SHORT).show()
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Surface(
        color = CarCraftTheme.SurfaceDeep,
        modifier = modifier.fillMaxSize()
    ) {
        CarCinematicAmbientBackground(
            currentSong = musicPlayerViewModel.currentSong,
            accent = accent
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    CarNavRail(
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it },
                        accent = accent
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        CarTopHud(
                            isOnline = isOnline,
                            batteryLevel = batteryLevel,
                            isCharging = isCharging,
                            isDataSaverEnabled = settingsViewModel.isDataSaverEnabled,
                            onDataSaverToggle = {
                                val newState = settingsViewModel.toggleDataSaver()
                                Toast.makeText(
                                    context,
                                    if (newState) context.getString(R.string.car_data_saver_on) else context.getString(R.string.car_data_saver_off),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onGpsClick = { launchNavigationApp(context) },
                            onVoiceClick = launchVoiceSearch,
                            onExitClick = onClose
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            CarDashboardSectionContent(
                                selectedSection = selectedSection,
                                isLandscape = true,
                                musicPlayerViewModel = musicPlayerViewModel,
                                libraryViewModel = libraryViewModel,
                                youTubeViewModel = youTubeViewModel,
                                settingsViewModel = settingsViewModel,
                                currentVolume = currentVolume,
                                maxVolume = maxVolume,
                                onVolumeChange = { setSystemVolume(it) },
                                onVoiceSearch = launchVoiceSearch,
                                onNavigateToNowPlaying = { selectedSection = CarNavSection.NOW_PLAYING },
                                accent = accent,
                                downloadViewModel = downloadViewModel
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    CarTopHud(
                        isOnline = isOnline,
                        batteryLevel = batteryLevel,
                        isCharging = isCharging,
                        isDataSaverEnabled = settingsViewModel.isDataSaverEnabled,
                        onDataSaverToggle = {
                            val newState = settingsViewModel.toggleDataSaver()
                            Toast.makeText(
                                context,
                                if (newState) context.getString(R.string.car_data_saver_on) else context.getString(R.string.car_data_saver_off),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onGpsClick = { launchNavigationApp(context) },
                        onVoiceClick = launchVoiceSearch,
                        onExitClick = onClose
                    )

                    CarTopTabBar(
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it },
                        accent = accent
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CarDashboardSectionContent(
                            selectedSection = selectedSection,
                            isLandscape = false,
                            musicPlayerViewModel = musicPlayerViewModel,
                            libraryViewModel = libraryViewModel,
                            youTubeViewModel = youTubeViewModel,
                            settingsViewModel = settingsViewModel,
                            currentVolume = currentVolume,
                            maxVolume = maxVolume,
                            onVolumeChange = { setSystemVolume(it) },
                            onVoiceSearch = launchVoiceSearch,
                            onNavigateToNowPlaying = { selectedSection = CarNavSection.NOW_PLAYING },
                            accent = accent,
                            downloadViewModel = downloadViewModel
                        )
                    }
                }
            }

            // Right Edge Touch Zone (Volume)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(52.dp)
                    .pointerInput(maxVolume, currentVolume) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                volumeDragAccumulator = 0f
                                showVolumeHud = true
                                hudAutoHideTrigger = System.currentTimeMillis()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                volumeDragAccumulator += -dragAmount
                                val stepThreshold = 35f
                                if (kotlin.math.abs(volumeDragAccumulator) >= stepThreshold) {
                                    val steps = (volumeDragAccumulator / stepThreshold).toInt()
                                    val newVol = (currentVolume + steps).coerceIn(0, maxVolume)
                                    if (newVol != currentVolume) {
                                        setSystemVolume(newVol)
                                    }
                                    volumeDragAccumulator %= stepThreshold
                                }
                                showVolumeHud = true
                                hudAutoHideTrigger = System.currentTimeMillis()
                            }
                        )
                    }
            )

            // Floating Edge Adjustment HUD (Volume)
            AnimatedVisibility(
                visible = showVolumeHud,
                enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.9f),
                exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            ) {
                val percent = if (maxVolume > 0) (currentVolume * 100 / maxVolume).coerceIn(0, 100) else 0
                CarEdgeAdjustmentHud(
                    percent = percent,
                    accent = accent
                )
            }
        }
    }
}
}

@Composable
private fun CarDashboardSectionContent(
    selectedSection: CarNavSection,
    isLandscape: Boolean,
    musicPlayerViewModel: MusicPlayerViewModel,
    libraryViewModel: LibraryViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    settingsViewModel: PlaybackSettingsViewModel,
    currentVolume: Int,
    maxVolume: Int,
    onVolumeChange: (Int) -> Unit,
    onVoiceSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    accent: Color,
    downloadViewModel: DownloadViewModel = viewModel()
) {
    AnimatedContent(
        targetState = selectedSection,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
        },
        label = "car_dashboard_section_anim"
    ) { section ->
        when (section) {
            CarNavSection.NOW_PLAYING -> CarNowPlayingSection(
                musicPlayerViewModel = musicPlayerViewModel,
                libraryViewModel = libraryViewModel,
                youTubeViewModel = youTubeViewModel,
                settingsViewModel = settingsViewModel,
                isLandscape = isLandscape,
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                onVolumeChange = onVolumeChange,
                accent = accent,
                downloadViewModel = downloadViewModel
            )

            CarNavSection.SEARCH -> CarStreamingSearchSection(
                youTubeViewModel = youTubeViewModel,
                musicPlayerViewModel = musicPlayerViewModel,
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                onVoiceClick = onVoiceSearch,
                accent = accent,
                downloadViewModel = downloadViewModel
            )

            CarNavSection.LIBRARY -> CarLibrarySection(
                libraryViewModel = libraryViewModel,
                musicPlayerViewModel = musicPlayerViewModel,
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                accent = accent
            )

            CarNavSection.RADIOS -> CarRadiosSection(
                musicPlayerViewModel = musicPlayerViewModel,
                libraryViewModel = libraryViewModel,
                youTubeViewModel = youTubeViewModel,
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                accent = accent
            )

            CarNavSection.DOWNLOADS -> CarDownloadsSection(
                musicPlayerViewModel = musicPlayerViewModel,
                libraryViewModel = libraryViewModel,
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                accent = accent,
                downloadViewModel = downloadViewModel
            )

            CarNavSection.VOICE -> CarVoiceSection(
                onVoiceClick = onVoiceSearch,
                libraryViewModel = libraryViewModel,
                musicPlayerViewModel = musicPlayerViewModel,
                youTubeViewModel = youTubeViewModel,
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                accent = accent
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECCIÓN 1: AHORA SUENA (NOW PLAYING)
// -------------------------------------------------------------------------------------------------

@Composable
private fun CarNowPlayingSection(
    musicPlayerViewModel: MusicPlayerViewModel,
    libraryViewModel: LibraryViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    settingsViewModel: PlaybackSettingsViewModel,
    isLandscape: Boolean,
    currentVolume: Int,
    maxVolume: Int,
    onVolumeChange: (Int) -> Unit,
    accent: Color,
    downloadViewModel: DownloadViewModel = viewModel()
) {
    val currentSong = musicPlayerViewModel.currentSong
    val context = LocalContext.current

    if (currentSong == null) {
        val smartTracks = remember(libraryViewModel.allSongs, youTubeViewModel.quickPicks) {
            minOf(50, (libraryViewModel.allSongs.size + youTubeViewModel.quickPicks.value.size).coerceAtLeast(1))
        }
        CarNoSongEmptyState(
            onPlayAll = {
                if (libraryViewModel.allSongs.isNotEmpty()) {
                    musicPlayerViewModel.playSong(libraryViewModel.allSongs.first(), libraryViewModel.allSongs, context)
                }
            },
            onPlaySmartMix = {
                val mixSongs = youTubeViewModel.generateSmartRoadTripMix(
                    librarySongs = libraryViewModel.allSongs,
                    quickPicks = youTubeViewModel.quickPicks.value
                )
                if (mixSongs.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.car_smart_mix_playing), Toast.LENGTH_SHORT).show()
                    musicPlayerViewModel.playSong(mixSongs.first(), mixSongs, context)
                } else {
                    Toast.makeText(context, context.getString(R.string.car_no_songs_available), Toast.LENGTH_SHORT).show()
                }
            },
            smartTrackCount = smartTracks,
            accent = accent
        )
        return
    }

    val gestureModifier = if (settingsViewModel.carModeGesturesEnabled) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { musicPlayerViewModel.togglePlayPause() }
                )
            }
            .pointerInput(Unit) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        if (totalDragX > 100f) {
                            musicPlayerViewModel.playPrevious()
                        } else if (totalDragX < -100f) {
                            musicPlayerViewModel.playNext()
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = { totalDragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                    }
                )
            }
    } else {
        Modifier
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .then(gestureModifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna Izquierda: Cover Art con Glow + Info Canción + Visualizer + Like/Download
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(vertical = 4.dp)
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = accent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CarCraftTheme.SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarCraftTheme.BorderSubtle)
                ) {
                    AsyncImage(
                        model = buildCoverRequest(LocalContext.current, currentSong, maxSize = 1024),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentSong.title,
                            style = CarCraftTheme.EditorialHeaderStyle,
                            fontSize = 20.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Text(
                            text = currentSong.artist,
                            color = CarCraftTheme.TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    CarLiveWaveVisualizer(
                        isPlaying = musicPlayerViewModel.isPlaying,
                        accent = accent
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarHardwareIconButton(
                        icon = if (currentSong.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (currentSong.isLiked) stringResource(R.string.car_mode_unlike) else stringResource(R.string.car_mode_like),
                        size = 52.dp,
                        iconSize = 26.dp,
                        tint = if (currentSong.isLiked) accent else CarCraftTheme.TextSecondary,
                        backgroundColor = if (currentSong.isLiked) accent.copy(alpha = 0.18f) else CarCraftTheme.SurfaceElevated,
                        borderColor = if (currentSong.isLiked) accent.copy(alpha = 0.4f) else CarCraftTheme.BorderMedium,
                        onClick = { libraryViewModel.toggleLike(currentSong.id, currentSong) }
                    )

                    CarDownloadButton(
                        song = currentSong,
                        downloadViewModel = downloadViewModel,
                        size = 52.dp,
                        iconSize = 26.dp,
                        accent = accent
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Columna Derecha: Barra táctil + Controles Gigantes + Up Next + Slider de Volumen
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CarTouchProgressBar(
                    currentPosition = musicPlayerViewModel.currentPosition,
                    duration = musicPlayerViewModel.duration,
                    accent = accent,
                    onSeek = { musicPlayerViewModel.seekTo(it) }
                )

                // Controles de Transporte Hardware
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarHardwareIconButton(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.car_shuffle),
                        size = 52.dp,
                        iconSize = 26.dp,
                        tint = if (musicPlayerViewModel.isShuffleEnabled) accent else CarCraftTheme.TextTertiary,
                        backgroundColor = if (musicPlayerViewModel.isShuffleEnabled) accent.copy(alpha = 0.18f) else CarCraftTheme.SurfaceElevated,
                        borderColor = if (musicPlayerViewModel.isShuffleEnabled) accent.copy(alpha = 0.4f) else CarCraftTheme.BorderMedium,
                        onClick = { musicPlayerViewModel.toggleShuffle() }
                    )

                    CarHardwareIconButton(
                        icon = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.car_previous_song),
                        size = 64.dp,
                        iconSize = 36.dp,
                        tint = Color.White,
                        backgroundColor = CarCraftTheme.SurfaceElevated,
                        borderColor = CarCraftTheme.BorderMedium,
                        onClick = { musicPlayerViewModel.playPrevious() }
                    )

                    CarHardwarePlayPauseButton(
                        isPlaying = musicPlayerViewModel.isPlaying,
                        accent = accent,
                        size = 92.dp,
                        iconSize = 48.dp,
                        onClick = { musicPlayerViewModel.togglePlayPause() }
                    )

                    CarHardwareIconButton(
                        icon = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.car_next_song),
                        size = 64.dp,
                        iconSize = 36.dp,
                        tint = Color.White,
                        backgroundColor = CarCraftTheme.SurfaceElevated,
                        borderColor = CarCraftTheme.BorderMedium,
                        onClick = { musicPlayerViewModel.playNext() }
                    )

                    val repeatIcon = if (musicPlayerViewModel.repeatMode == MusicPlayerViewModel.RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                    val isRepeatActive = musicPlayerViewModel.repeatMode != MusicPlayerViewModel.RepeatMode.NONE
                    CarHardwareIconButton(
                        icon = repeatIcon,
                        contentDescription = stringResource(R.string.car_repeat),
                        size = 52.dp,
                        iconSize = 26.dp,
                        tint = if (isRepeatActive) accent else CarCraftTheme.TextTertiary,
                        backgroundColor = if (isRepeatActive) accent.copy(alpha = 0.18f) else CarCraftTheme.SurfaceElevated,
                        borderColor = if (isRepeatActive) accent.copy(alpha = 0.4f) else CarCraftTheme.BorderMedium,
                        onClick = { musicPlayerViewModel.toggleRepeatMode() }
                    )
                }

                // Up Next Strip en Landscape
                CarUpNextHorizontalStrip(
                    musicPlayerViewModel = musicPlayerViewModel,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )

                // Volumen del Sistema
                CarVolumeControl(
                    currentVolume = currentVolume,
                    maxVolume = maxVolume,
                    onVolumeChange = onVolumeChange,
                    accent = accent
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .then(gestureModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier
                    .size(240.dp)
                    .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = accent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CarCraftTheme.SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarCraftTheme.BorderSubtle)
            ) {
                AsyncImage(
                    model = buildCoverRequest(LocalContext.current, currentSong, maxSize = 1024),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong.title,
                        style = CarCraftTheme.EditorialHeaderStyle,
                        fontSize = 22.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong.artist,
                        color = CarCraftTheme.TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                CarLiveWaveVisualizer(
                    isPlaying = musicPlayerViewModel.isPlaying,
                    accent = accent
                )
            }

            CarTouchProgressBar(
                currentPosition = musicPlayerViewModel.currentPosition,
                duration = musicPlayerViewModel.duration,
                accent = accent,
                onSeek = { musicPlayerViewModel.seekTo(it) }
            )

            // Fila de Controles de Transporte Gigantes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarHardwareIconButton(
                    icon = Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.car_shuffle),
                    size = 50.dp,
                    iconSize = 24.dp,
                    tint = if (musicPlayerViewModel.isShuffleEnabled) accent else CarCraftTheme.TextTertiary,
                    backgroundColor = if (musicPlayerViewModel.isShuffleEnabled) accent.copy(alpha = 0.18f) else CarCraftTheme.SurfaceElevated,
                    borderColor = if (musicPlayerViewModel.isShuffleEnabled) accent.copy(alpha = 0.4f) else CarCraftTheme.BorderMedium,
                    onClick = { musicPlayerViewModel.toggleShuffle() }
                )

                CarHardwareIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.car_previous_song),
                    size = 64.dp,
                    iconSize = 36.dp,
                    tint = Color.White,
                    backgroundColor = CarCraftTheme.SurfaceElevated,
                    borderColor = CarCraftTheme.BorderMedium,
                    onClick = { musicPlayerViewModel.playPrevious() }
                )

                CarHardwarePlayPauseButton(
                    isPlaying = musicPlayerViewModel.isPlaying,
                    accent = accent,
                    size = 92.dp,
                    iconSize = 48.dp,
                    onClick = { musicPlayerViewModel.togglePlayPause() }
                )

                CarHardwareIconButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.car_next_song),
                    size = 64.dp,
                    iconSize = 36.dp,
                    tint = Color.White,
                    backgroundColor = CarCraftTheme.SurfaceElevated,
                    borderColor = CarCraftTheme.BorderMedium,
                    onClick = { musicPlayerViewModel.playNext() }
                )

                val repeatIcon = if (musicPlayerViewModel.repeatMode == MusicPlayerViewModel.RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                val isRepeatActive = musicPlayerViewModel.repeatMode != MusicPlayerViewModel.RepeatMode.NONE
                CarHardwareIconButton(
                    icon = repeatIcon,
                    contentDescription = stringResource(R.string.car_repeat),
                    size = 50.dp,
                    iconSize = 24.dp,
                    tint = if (isRepeatActive) accent else CarCraftTheme.TextTertiary,
                    backgroundColor = if (isRepeatActive) accent.copy(alpha = 0.18f) else CarCraftTheme.SurfaceElevated,
                    borderColor = if (isRepeatActive) accent.copy(alpha = 0.4f) else CarCraftTheme.BorderMedium,
                    onClick = { musicPlayerViewModel.toggleRepeatMode() }
                )
            }

            // Fila de Like + Descarga + Volumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CarHardwareIconButton(
                    icon = if (currentSong.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (currentSong.isLiked) stringResource(R.string.car_mode_unlike) else stringResource(R.string.car_mode_like),
                    size = 52.dp,
                    iconSize = 26.dp,
                    tint = if (currentSong.isLiked) accent else CarCraftTheme.TextSecondary,
                    backgroundColor = if (currentSong.isLiked) accent.copy(alpha = 0.18f) else CarCraftTheme.SurfaceElevated,
                    borderColor = if (currentSong.isLiked) accent.copy(alpha = 0.4f) else CarCraftTheme.BorderMedium,
                    onClick = { libraryViewModel.toggleLike(currentSong.id, currentSong) }
                )

                CarDownloadButton(
                    song = currentSong,
                    downloadViewModel = downloadViewModel,
                    size = 52.dp,
                    iconSize = 26.dp,
                    accent = accent
                )

                Box(modifier = Modifier.weight(1f)) {
                    CarVolumeControl(
                        currentVolume = currentVolume,
                        maxVolume = maxVolume,
                        onVolumeChange = onVolumeChange,
                        accent = accent
                    )
                }
            }

            // Up Next Strip en Portrait
            CarUpNextHorizontalStrip(
                musicPlayerViewModel = musicPlayerViewModel,
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CarVolumeControl(
    currentVolume: Int,
    maxVolume: Int,
    onVolumeChange: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val volIcon = when {
        currentVolume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
        currentVolume < maxVolume / 3 -> Icons.AutoMirrored.Filled.VolumeMute
        currentVolume < (maxVolume * 2) / 3 -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarCraftTheme.SurfaceCard)
            .border(1.dp, CarCraftTheme.BorderSubtle, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = volIcon,
            contentDescription = stringResource(R.string.car_volume_label),
            tint = accent,
            modifier = Modifier.size(22.dp)
        )

        Slider(
            value = currentVolume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..maxVolume.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$currentVolume",
            style = CarCraftTheme.MonoTimerStyle,
            fontSize = 13.sp,
            color = Color.White,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CarNoSongEmptyState(
    onPlayAll: () -> Unit,
    onPlaySmartMix: (() -> Unit)? = null,
    smartTrackCount: Int = 50,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .border(2.dp, accent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.car_no_song_playing),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.car_choose_music_prompt),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (onPlaySmartMix != null) {
            Spacer(modifier = Modifier.height(20.dp))
            CarSmartRoadTripHeroCard(
                onPlayMix = onPlaySmartMix,
                trackCount = smartTrackCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .clickable(onClick = onPlayAll)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.car_play_all_music),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECCIÓN 2: BIBLIOTECA & PLAYLISTS (LIBRARY)
// -------------------------------------------------------------------------------------------------

@Composable
private fun CarLibrarySection(
    libraryViewModel: LibraryViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    onNavigateToNowPlaying: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val likedSongs = remember(libraryViewModel.allSongs) {
        libraryViewModel.allSongs.filter { it.isLiked }
    }
    val allAlbums by libraryViewModel.allAlbums.collectAsState()
    val playlists = libraryViewModel.userPlaylists

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta Masiva: Tus Favoritos
        item {
            CarFeatureCard(
                title = stringResource(R.string.car_favorites_card),
                subtitle = stringResource(R.string.car_song_count, likedSongs.size),
                buttonLabel = stringResource(R.string.car_favorites_play_all),
                icon = Icons.Filled.Favorite,
                gradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                onClick = {
                    if (likedSongs.isNotEmpty()) {
                        musicPlayerViewModel.playSong(likedSongs.first(), likedSongs, context)
                        onNavigateToNowPlaying()
                    }
                }
            )
        }

        // Tarjeta Masiva: Modo Viaje en Carretera
        item {
            CarFeatureCard(
                title = stringResource(R.string.car_road_trip_mode),
                subtitle = stringResource(R.string.car_road_trip_desc),
                buttonLabel = stringResource(R.string.car_play),
                icon = Icons.Filled.DirectionsCar,
                gradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
                onClick = {
                    if (libraryViewModel.allSongs.isNotEmpty()) {
                        val shuffled = libraryViewModel.allSongs.shuffled()
                        musicPlayerViewModel.playSong(shuffled.first(), shuffled, context)
                        onNavigateToNowPlaying()
                    }
                }
            )
        }

        // Playlists Locales
        if (playlists.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.car_playlists_title),
                    style = CarCraftTheme.EditorialHeaderStyle,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(playlists, key = { it.id }) { pl ->
                        CarEditorialPlaylistCard(
                            playlist = pl,
                            onClick = {
                                if (pl.songs.isNotEmpty()) {
                                    musicPlayerViewModel.playSong(pl.songs.first(), pl.songs, context)
                                    onNavigateToNowPlaying()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Álbumes Locales
        if (allAlbums.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.car_albums_title),
                    style = CarCraftTheme.EditorialHeaderStyle,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(allAlbums.take(20), key = { it.name }) { album ->
                        CarEditorialAlbumCard(
                            album = album,
                            onClick = {
                                if (album.songs.isNotEmpty()) {
                                    musicPlayerViewModel.playSong(album.songs.first(), album.songs, context)
                                    onNavigateToNowPlaying()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarFeatureCard(
    title: String,
    subtitle: String,
    buttonLabel: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "feature_card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CarCraftTheme.SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarCraftTheme.BorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = CarCraftTheme.EditorialHeaderStyle,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = buttonLabel,
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CarPlaylistItemCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CarEditorialPlaylistCard(playlist = playlist, onClick = onClick, modifier = modifier)
}

@Composable
private fun CarAlbumItemCard(
    album: AlbumGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CarEditorialAlbumCard(album = album, onClick = onClick, modifier = modifier)
}

// -------------------------------------------------------------------------------------------------
// SECCIÓN 3: RADIOS & MIXES (RADIOS)
// -------------------------------------------------------------------------------------------------

private data class CarQuickMix(
    val titleRes: Int,
    val query: String,
    val gradient: List<Color>,
    val icon: ImageVector
)

@Composable
private fun CarRadiosSection(
    musicPlayerViewModel: MusicPlayerViewModel,
    libraryViewModel: LibraryViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    onNavigateToNowPlaying: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSong = musicPlayerViewModel.currentSong ?: libraryViewModel.allSongs.firstOrNull()

    val quickMixes = remember {
        listOf(
            CarQuickMix(R.string.car_quick_mix_energy, "Workout Energy Music Mix", listOf(Color(0xFFF97316), Color(0xFFEF4444)), Icons.Filled.Bolt),
            CarQuickMix(R.string.car_quick_mix_hits, "Top Hits Music", listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)), Icons.Filled.PlayCircle),
            CarQuickMix(R.string.car_quick_mix_chill, "Chill Relaxing Music", listOf(Color(0xFF10B981), Color(0xFF06B6D4)), Icons.Filled.MusicNote),
            CarQuickMix(R.string.car_quick_mix_rock, "Classic Rock Road Trip", listOf(Color(0xFFDC2626), Color(0xFF7C3AED)), Icons.Filled.Radio),
            CarQuickMix(R.string.car_quick_mix_latin, "Latin Hits Party", listOf(Color(0xFFF59E0B), Color(0xFFEC4899)), Icons.Filled.GraphicEq),
            CarQuickMix(R.string.car_quick_mix_discover, "Discover Weekly Mix", listOf(Color(0xFF6366F1), Color(0xFFA855F7)), Icons.Filled.DirectionsCar)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Smart Road Trip Mix Hero Card
        item {
            val smartTracks = remember(libraryViewModel.allSongs, youTubeViewModel.quickPicks) {
                minOf(50, (libraryViewModel.allSongs.size + youTubeViewModel.quickPicks.value.size).coerceAtLeast(1))
            }
            CarSmartRoadTripHeroCard(
                onPlayMix = {
                    val mixSongs = youTubeViewModel.generateSmartRoadTripMix(
                        librarySongs = libraryViewModel.allSongs,
                        quickPicks = youTubeViewModel.quickPicks.value
                    )
                    if (mixSongs.isNotEmpty()) {
                        Toast.makeText(context, context.getString(R.string.car_smart_mix_playing), Toast.LENGTH_SHORT).show()
                        musicPlayerViewModel.playSong(mixSongs.first(), mixSongs, context)
                        onNavigateToNowPlaying()
                    } else {
                        Toast.makeText(context, context.getString(R.string.car_no_songs_available), Toast.LENGTH_SHORT).show()
                    }
                },
                trackCount = smartTracks
            )
        }

        // Radio de la canción actual
        item {
            CarFeatureCard(
                title = stringResource(R.string.car_start_song_radio),
                subtitle = currentSong?.let { "${it.title} - ${it.artist}" } ?: stringResource(R.string.car_radio_desc),
                buttonLabel = stringResource(R.string.action_start_radio),
                icon = Icons.Filled.Radio,
                gradient = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
                onClick = {
                    if (currentSong != null) {
                        youTubeViewModel.startSongRadio(
                            song = currentSong,
                            playImmediately = true,
                            musicPlayerViewModel = musicPlayerViewModel,
                            context = context,
                            onStarted = { onNavigateToNowPlaying() }
                        )
                    }
                }
            )
        }

        item {
            Text(
                text = stringResource(R.string.car_quick_mixes_title),
                style = CarCraftTheme.EditorialHeaderStyle,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(quickMixes.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { mix ->
                    Box(modifier = Modifier.weight(1f)) {
                        CarQuickMixCard(
                            mix = mix,
                            onClick = {
                                youTubeViewModel.runSearch(mix.query)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CarQuickMixCard(
    mix: CarQuickMix,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "quick_mix_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CarCraftTheme.SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarCraftTheme.BorderSubtle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(mix.gradient))
                .padding(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = mix.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(mix.titleRes),
                        style = CarCraftTheme.EditorialHeaderStyle,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECCIÓN 4: DESCARGAS (DOWNLOADS)
// -------------------------------------------------------------------------------------------------

@Composable
private fun CarDownloadsSection(
    musicPlayerViewModel: MusicPlayerViewModel,
    libraryViewModel: LibraryViewModel,
    onNavigateToNowPlaying: () -> Unit,
    accent: Color,
    downloadViewModel: DownloadViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiItems by downloadViewModel.uiState.collectAsState()
    val completedItems = remember(uiItems) { uiItems.filter { it.isCompleted } }

    val downloadedSongs = remember(completedItems) {
        completedItems.map { item ->
            Song(
                id = item.entity.videoId,
                title = item.entity.title,
                artist = item.entity.artist,
                album = item.entity.album.ifBlank { "Unknown Album" },
                duration = item.entity.duration,
                filePath = "",
                coverUrl = item.entity.coverUrl,
                isYouTube = true,
                youtubeVideoId = item.entity.videoId,
                youtubeThumbnailUrl = item.entity.coverUrl
            )
        }
    }

    if (downloadedSongs.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.car_downloads_empty),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CarFeatureCard(
                title = stringResource(R.string.car_downloads_title),
                subtitle = stringResource(R.string.car_song_count, downloadedSongs.size),
                buttonLabel = stringResource(R.string.car_play_shuffle_downloads),
                icon = Icons.Filled.CloudDownload,
                gradient = listOf(Color(0xFF10B981), Color(0xFF047857)),
                onClick = {
                    val shuffled = downloadedSongs.shuffled()
                    musicPlayerViewModel.playSong(shuffled.first(), shuffled, context)
                    onNavigateToNowPlaying()
                }
            )
        }

        item {
            Text(
                text = stringResource(R.string.car_downloads_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(downloadedSongs, key = { it.id }) { song ->
            CarDownloadedSongRow(
                song = song,
                downloadViewModel = downloadViewModel,
                accent = accent,
                onClick = {
                    musicPlayerViewModel.playSong(song, downloadedSongs, context)
                    onNavigateToNowPlaying()
                }
            )
        }
    }
}

@Composable
private fun CarDownloadedSongRow(
    song: Song,
    onClick: () -> Unit,
    downloadViewModel: DownloadViewModel = viewModel(),
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16161E))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = buildCoverRequest(LocalContext.current, song, maxSize = 200),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatCarTime(song.duration),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        CarDownloadButton(
            song = song,
            downloadViewModel = downloadViewModel,
            size = 40.dp,
            iconSize = 20.dp,
            accent = accent
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECCIÓN 5: VOZ & FILTROS (VOICE & QUICK GENRES)
// -------------------------------------------------------------------------------------------------

private data class CarGenreChip(
    val titleRes: Int,
    val query: String,
    val color: Color
)

@Composable
private fun CarVoiceSection(
    onVoiceClick: () -> Unit,
    libraryViewModel: LibraryViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    onNavigateToNowPlaying: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val genreChips = remember {
        listOf(
            CarGenreChip(R.string.car_filter_rock, "Rock Hits", Color(0xFFEF4444)),
            CarGenreChip(R.string.car_filter_pop, "Pop Music Hits", Color(0xFFEC4899)),
            CarGenreChip(R.string.car_filter_latin, "Latino Hits", Color(0xFFF59E0B)),
            CarGenreChip(R.string.car_filter_electronic, "Electronic Dance Music", Color(0xFF06B6D4)),
            CarGenreChip(R.string.car_filter_chill, "Chill Relax", Color(0xFF10B981)),
            CarGenreChip(R.string.car_filter_top, "Top Trending Music", Color(0xFF8B5CF6))
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Botón Masivo de Micrófono (120dp) con animación pulsante
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.25f))
                )

                // Main Mic Button (110dp)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(24.dp, CircleShape, spotColor = accent)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accent, accent.copy(alpha = 0.85f))
                            )
                        )
                        .clickable(onClick = onVoiceClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.car_mode_voice_search),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.car_voice_hint),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Text(
                text = stringResource(R.string.car_nav_voice),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Chips / Tarjetas de Géneros
        items(genreChips.chunked(2)) { rowChips ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowChips.forEach { chip ->
                    Box(modifier = Modifier.weight(1f)) {
                        CarGenreCard(
                            chip = chip,
                            onClick = {
                                youTubeViewModel.runSearch(chip.query)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarGenreCard(
    chip: CarGenreChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = chip.color.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, chip.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(chip.color)
                )
                Text(
                    text = stringResource(chip.titleRes),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPONENTES COMUNES
// -------------------------------------------------------------------------------------------------

@Composable
private fun CarExitButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "car_exit_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.car_mode_exit),
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(R.string.car_mode_exit),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CarDownloadButton(
    song: Song,
    downloadViewModel: DownloadViewModel = viewModel(),
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 26.dp,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val isLocalSong = remember(song) { !song.isYouTube && song.filePath.isNotEmpty() }
    val targetVideoId = remember(song) {
        (song.youtubeVideoId ?: if (song.isYouTube) song.id else "").takeIf { it.isNotBlank() }
    }
    val safeCoverUrl = remember(song) { song.coverUrl ?: song.youtubeThumbnailUrl }

    CarDownloadButtonContent(
        targetVideoId = targetVideoId,
        isLocalSong = isLocalSong,
        title = song.title,
        artist = song.artist,
        album = song.album,
        coverUrl = safeCoverUrl,
        duration = song.duration,
        downloadViewModel = downloadViewModel,
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        accent = accent
    )
}

@Composable
fun CarDownloadButton(
    songItem: SongItem,
    downloadViewModel: DownloadViewModel = viewModel(),
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    CarDownloadButtonContent(
        targetVideoId = songItem.id.takeIf { it.isNotBlank() },
        isLocalSong = false,
        title = songItem.title,
        artist = songItem.artists.joinToString { it.name },
        album = songItem.album?.name ?: "",
        coverUrl = songItem.thumbnail,
        duration = songItem.duration ?: 0,
        downloadViewModel = downloadViewModel,
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        accent = accent
    )
}

@Composable
private fun CarDownloadButtonContent(
    targetVideoId: String?,
    isLocalSong: Boolean,
    title: String,
    artist: String,
    album: String,
    coverUrl: String?,
    duration: Int,
    downloadViewModel: DownloadViewModel,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 26.dp,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiItemsState = downloadViewModel.uiState.collectAsState()

    val dlItem by remember(targetVideoId) {
        derivedStateOf {
            targetVideoId?.let { id ->
                uiItemsState.value.find { it.entity.videoId == id }
            }
        }
    }

    val isDownloading = dlItem?.isDownloading == true || dlItem?.isQueued == true
    val isDownloaded = dlItem?.isCompleted == true || isLocalSong

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "car_dl_btn_scale"
    )

    val backgroundColor = when {
        isDownloaded -> Color(0xFF10B981).copy(alpha = 0.18f)
        isDownloading -> accent.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    val borderColor = when {
        isDownloaded -> Color(0xFF10B981).copy(alpha = 0.45f)
        isDownloading -> accent.copy(alpha = 0.40f)
        else -> Color.White.copy(alpha = 0.12f)
    }

    val iconTint = when {
        isDownloaded -> Color(0xFF10B981)
        isDownloading -> accent
        else -> Color.White.copy(alpha = 0.85f)
    }

    val contentDesc = when {
        isLocalSong -> stringResource(R.string.car_download_already_local)
        dlItem?.isCompleted == true -> stringResource(R.string.car_downloaded_song)
        isDownloading -> stringResource(R.string.car_downloading_song)
        else -> stringResource(R.string.car_download_song)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when {
                    isLocalSong -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.car_download_already_local),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dlItem?.isCompleted == true -> {
                        targetVideoId?.let { id ->
                            downloadViewModel.delete(id)
                            Toast.makeText(
                                context,
                                context.getString(R.string.car_download_deleted),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    isDownloading -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.car_downloading_song),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val videoIdToDownload = targetVideoId ?: ""
                        if (videoIdToDownload.isNotBlank()) {
                            downloadViewModel.download(
                                videoId = videoIdToDownload,
                                title = title,
                                artist = artist,
                                album = album,
                                coverUrl = coverUrl,
                                duration = duration
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.car_download_started),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isDownloading) {
            val progress = dlItem?.progress ?: 0f
            if (progress > 0f) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(size - 10.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.20f),
                    strokeWidth = 3.dp
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(size - 10.dp),
                    color = accent,
                    strokeWidth = 3.dp
                )
            }
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = contentDesc,
                tint = accent,
                modifier = Modifier.size(iconSize - 6.dp)
            )
        } else if (isDownloaded) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = contentDesc,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = contentDesc,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun CarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp,
    iconSize: Dp,
    tint: Color,
    backgroundColor: Color = CarCraftTheme.SurfaceElevated,
    onClick: () -> Unit
) {
    CarHardwareIconButton(
        icon = icon,
        contentDescription = contentDescription,
        size = size,
        iconSize = iconSize,
        tint = tint,
        backgroundColor = backgroundColor,
        onClick = onClick
    )
}

@Composable
private fun CarPlayPauseButton(
    isPlaying: Boolean,
    accent: Color,
    size: Dp = 92.dp,
    iconSize: Dp = 48.dp,
    onClick: () -> Unit
) {
    CarHardwarePlayPauseButton(
        isPlaying = isPlaying,
        accent = accent,
        size = size,
        iconSize = iconSize,
        onClick = onClick
    )
}

@Composable
private fun CarProgressBar(
    currentPosition: Int,
    duration: Int,
    accent: Color,
    onSeek: (Int) -> Unit
) {
    CarTouchProgressBar(
        currentPosition = currentPosition,
        duration = duration,
        accent = accent,
        onSeek = onSeek
    )
}

// -------------------------------------------------------------------------------------------------
// SECCIÓN 6: BÚSQUEDA & STREAMING (STREAMING SEARCH)
// -------------------------------------------------------------------------------------------------

@Composable
private fun CarStreamingSearchSection(
    youTubeViewModel: YouTubeMusicViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onVoiceClick: () -> Unit,
    accent: Color,
    downloadViewModel: DownloadViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val searchState = youTubeViewModel.search
    val quickPicks by youTubeViewModel.quickPicks.collectAsState()
    val chartsState = youTubeViewModel.chartsState

    LaunchedEffect(Unit) {
        if (chartsState is ChartsUiState.Idle) {
            youTubeViewModel.loadCharts()
        }
        if (youTubeViewModel.exploreState is ExploreUiState.Idle) {
            youTubeViewModel.loadExplore()
        }
    }

    val searchSongs: List<SongItem> = remember(searchState) {
        when (searchState) {
            is SearchUiState.Success -> {
                searchState.items.filterIsInstance<SongItem>()
            }
            else -> emptyList()
        }
    }

    val chartSongs: List<SongItem> = remember(chartsState) {
        if (chartsState is ChartsUiState.Success) {
            chartsState.page.sections
                .flatMap { it.items }
                .filterIsInstance<SongItem>()
        } else {
            emptyList()
        }
    }

    val genreRadios = remember {
        listOf(
            CarQuickMix(R.string.car_filter_top, "Top Trending Music Hits", listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), Icons.Filled.PlayCircle),
            CarQuickMix(R.string.car_filter_rock, "Classic Rock & Alternative", listOf(Color(0xFFDC2626), Color(0xFF7C3AED)), Icons.Filled.Radio),
            CarQuickMix(R.string.car_filter_pop, "Today's Top Pop Music", listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), Icons.Filled.MusicNote),
            CarQuickMix(R.string.car_filter_latin, "Latin Reggaeton y Pop Urbano", listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), Icons.Filled.GraphicEq),
            CarQuickMix(R.string.car_filter_electronic, "Electronic Dance EDM Party", listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)), Icons.Filled.Bolt),
            CarQuickMix(R.string.car_filter_chill, "Chill Relaxing Lo-Fi Beats", listOf(Color(0xFF10B981), Color(0xFF06B6D4)), Icons.Filled.DirectionsCar)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Barra vehicular de 64dp
        CarSearchBar(
            query = searchQuery,
            onQueryChange = { newQuery ->
                searchQuery = newQuery
                youTubeViewModel.updateQuery(newQuery)
            },
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
                if (searchQuery.isNotBlank()) {
                    youTubeViewModel.runSearch(searchQuery)
                }
            },
            onClear = {
                searchQuery = ""
                youTubeViewModel.updateQuery("")
            },
            onVoiceClick = onVoiceClick,
            accent = accent
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (searchQuery.isBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Radios de Género en 1 Toque
                item {
                    Text(
                        text = stringResource(R.string.car_streaming_genre_radios),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(genreRadios.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { mix ->
                            Box(modifier = Modifier.weight(1f)) {
                                CarQuickMixCard(
                                    mix = mix,
                                    onClick = {
                                        searchQuery = mix.query
                                        youTubeViewModel.runSearch(mix.query)
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Top Éxitos y Novedades de YouTube Music
                if (quickPicks.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.car_streaming_top_charts),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }

                    items(quickPicks.take(15), key = { it.id }) { songItem ->
                        CarStreamSongCard(
                            songItem = songItem,
                            accent = accent,
                            downloadViewModel = downloadViewModel,
                            onClick = {
                                val song = songItem.toSong()
                                val allSongs = quickPicks.map { it.toSong() }
                                musicPlayerViewModel.playSong(song, allSongs, context)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                } else if (chartSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.car_streaming_top_charts),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }

                    items(chartSongs.take(20), key = { it.id }) { songItem ->
                        CarStreamSongCard(
                            songItem = songItem,
                            accent = accent,
                            downloadViewModel = downloadViewModel,
                            onClick = {
                                val song = songItem.toSong()
                                val allSongs = chartSongs.map { it.toSong() }
                                musicPlayerViewModel.playSong(song, allSongs, context)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                }
            }
        } else {
            when (searchState) {
                is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = accent,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.car_streaming_searching),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                is SearchUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.car_streaming_error),
                                color = Color(0xFFF87171),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accent)
                                    .clickable { youTubeViewModel.runSearch(searchQuery) }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.car_streaming_retry),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is SearchUiState.Success -> {
                    if (searchSongs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.car_streaming_no_results),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(searchSongs, key = { it.id }) { songItem ->
                                CarStreamSongCard(
                                    songItem = songItem,
                                    accent = accent,
                                    downloadViewModel = downloadViewModel,
                                    onClick = {
                                        val song = songItem.toSong()
                                        val allSongs = searchSongs.map { it.toSong() }
                                        musicPlayerViewModel.playSong(song, allSongs, context)
                                        onNavigateToNowPlaying()
                                    }
                                )
                            }
                        }
                    }
                }

                is SearchUiState.Suggestions,
                SearchUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onVoiceClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF14161C))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch() }
            ),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.car_streaming_search_hint),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            },
            modifier = Modifier.weight(1f)
        )

        if (query.isNotEmpty()) {
            CarIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.car_search_clear),
                size = 42.dp,
                iconSize = 22.dp,
                tint = Color.White.copy(alpha = 0.8f),
                backgroundColor = Color.White.copy(alpha = 0.10f),
                onClick = onClear
            )
        }

        CarIconButton(
            icon = Icons.Filled.Mic,
            contentDescription = stringResource(R.string.car_mode_voice_search),
            size = 46.dp,
            iconSize = 24.dp,
            tint = MaterialTheme.colorScheme.onPrimary,
            backgroundColor = accent,
            onClick = onVoiceClick
        )
    }
}

@Composable
private fun CarStreamSongCard(
    songItem: SongItem,
    accent: Color,
    onClick: () -> Unit,
    downloadViewModel: DownloadViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14161C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = songItem.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = songItem.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = songItem.artists.joinToString { it.name },
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val durationSec = songItem.duration ?: 0
            if (durationSec > 0) {
                Text(
                    text = formatCarTime(durationSec),
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            CarDownloadButton(
                songItem = songItem,
                downloadViewModel = downloadViewModel,
                size = 44.dp,
                iconSize = 22.dp,
                accent = accent
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.20f))
                    .border(1.dp, accent.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.car_streaming_play_now),
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
