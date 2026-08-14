package com.fxzmusic.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fxzmusic.app.R
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.util.buildCoverRequest
import com.fxzmusic.app.viewmodel.MusicPlayerViewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatCarModeTime(seconds: Int): String {
    val totalSec = seconds.coerceAtLeast(0)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarModePlayerScreen(
    currentSong: Song,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: MusicPlayerViewModel.RepeatMode,
    keepScreenOn: Boolean,
    gesturesEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onClose: () -> Unit,
    onVoiceSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    // Mantener la pantalla encendida si está activado en ajustes
    DisposableEffect(keepScreenOn) {
        val activity = context.findActivity()
        val window = activity?.window
        if (keepScreenOn && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Launcher para reconocimiento por voz
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
                onVoiceSearch(spokenText)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.car_mode_voice_error),
                    Toast.LENGTH_SHORT
                ).show()
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
            Toast.makeText(
                context,
                context.getString(R.string.car_mode_voice_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Modifier para gestos de conducción táctiles
    val gestureModifier = if (gesturesEnabled) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onPlayPause() }
                )
            }
            .pointerInput(Unit) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        if (totalDragX > 120f) {
                            onPrevious()
                        } else if (totalDragX < -120f) {
                            onNext()
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

    Surface(
        color = Color(0xFF0D0D11),
        modifier = modifier
            .fillMaxSize()
            .then(gestureModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.12f),
                            Color(0xFF08080C),
                            Color(0xFF000000)
                        )
                    )
                )
        ) {
            if (isLandscape) {
                CarModeLandscapeLayout(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    accent = accent,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleLike = onToggleLike,
                    onClose = onClose,
                    onVoiceSearch = launchVoiceSearch
                )
            } else {
                CarModePortraitLayout(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    accent = accent,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleLike = onToggleLike,
                    onClose = onClose,
                    onVoiceSearch = launchVoiceSearch
                )
            }
        }
    }
}

@Composable
private fun CarModePortraitLayout(
    currentSong: Song,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: MusicPlayerViewModel.RepeatMode,
    accent: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onClose: () -> Unit,
    onVoiceSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Barra Superior: Salir + Título Modo Auto + Micrófono
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Salir grande y visible
            CarModeExitButton(onClick = onClose)

            // Indicador de modo auto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.car_mode_title),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón Búsqueda por Voz
            CarModeIconButton(
                icon = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.car_mode_voice_search),
                size = 54.dp,
                iconSize = 28.dp,
                backgroundColor = accent.copy(alpha = 0.2f),
                tint = accent,
                onClick = onVoiceSearch
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cover Art masivo con esquinas redondeadas
        Card(
            modifier = Modifier
                .size(260.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = accent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26))
        ) {
            AsyncImage(
                model = buildCoverRequest(LocalContext.current, currentSong, maxSize = 1024),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info de la Canción (Texto masivo y de alto contraste)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentSong.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentSong.artist,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de Progreso Gruesa para Auto
        CarModeProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            accent = accent,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fila de Controles de Transporte Gigantes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            CarModeIconButton(
                icon = Icons.Filled.Shuffle,
                contentDescription = null,
                size = 52.dp,
                iconSize = 26.dp,
                tint = if (isShuffleEnabled) accent else Color.White.copy(alpha = 0.5f),
                backgroundColor = if (isShuffleEnabled) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                onClick = onToggleShuffle
            )

            // Anterior (64dp)
            CarModeIconButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Anterior",
                size = 64.dp,
                iconSize = 36.dp,
                tint = Color.White,
                backgroundColor = Color.White.copy(alpha = 0.10f),
                onClick = onPrevious
            )

            // Play / Pause Gigante (86dp)
            CarModePlayPauseButton(
                isPlaying = isPlaying,
                accent = accent,
                size = 86.dp,
                iconSize = 46.dp,
                onClick = onPlayPause
            )

            // Siguiente (64dp)
            CarModeIconButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Siguiente",
                size = 64.dp,
                iconSize = 36.dp,
                tint = Color.White,
                backgroundColor = Color.White.copy(alpha = 0.10f),
                onClick = onNext
            )

            // Repeat
            val repeatIcon = if (repeatMode == MusicPlayerViewModel.RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
            val isRepeatActive = repeatMode != MusicPlayerViewModel.RepeatMode.NONE
            CarModeIconButton(
                icon = repeatIcon,
                contentDescription = null,
                size = 52.dp,
                iconSize = 26.dp,
                tint = if (isRepeatActive) accent else Color.White.copy(alpha = 0.5f),
                backgroundColor = if (isRepeatActive) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                onClick = onToggleRepeat
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Fila Inferior con Like
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarModeIconButton(
                icon = if (currentSong.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (currentSong.isLiked) stringResource(R.string.car_mode_unlike) else stringResource(R.string.car_mode_like),
                size = 56.dp,
                iconSize = 30.dp,
                tint = if (currentSong.isLiked) accent else Color.White.copy(alpha = 0.6f),
                backgroundColor = if (currentSong.isLiked) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                onClick = onToggleLike
            )
        }
    }
}

@Composable
private fun CarModeLandscapeLayout(
    currentSong: Song,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: MusicPlayerViewModel.RepeatMode,
    accent: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onClose: () -> Unit,
    onVoiceSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Izquierda: Cover Art + Título/Artista + Botones Like y Mic
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
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = accent.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26))
            ) {
                AsyncImage(
                    model = buildCoverRequest(LocalContext.current, currentSong, maxSize = 1024),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentSong.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = currentSong.artist,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarModeIconButton(
                    icon = if (currentSong.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (currentSong.isLiked) stringResource(R.string.car_mode_unlike) else stringResource(R.string.car_mode_like),
                    size = 50.dp,
                    iconSize = 26.dp,
                    tint = if (currentSong.isLiked) accent else Color.White.copy(alpha = 0.6f),
                    backgroundColor = if (currentSong.isLiked) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                    onClick = onToggleLike
                )

                CarModeIconButton(
                    icon = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.car_mode_voice_search),
                    size = 50.dp,
                    iconSize = 26.dp,
                    tint = accent,
                    backgroundColor = accent.copy(alpha = 0.2f),
                    onClick = onVoiceSearch
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Columna Derecha: Barra Superior Salir + Progreso + Controles Gigantes
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Salir + Indicador de Conducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.car_mode_title),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                CarModeExitButton(onClick = onClose)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de Progreso
            CarModeProgressBar(
                currentPosition = currentPosition,
                duration = duration,
                accent = accent,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Controles de Transporte en Landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                CarModeIconButton(
                    icon = Icons.Filled.Shuffle,
                    contentDescription = null,
                    size = 52.dp,
                    iconSize = 26.dp,
                    tint = if (isShuffleEnabled) accent else Color.White.copy(alpha = 0.5f),
                    backgroundColor = if (isShuffleEnabled) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                    onClick = onToggleShuffle
                )

                // Anterior (64dp)
                CarModeIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Anterior",
                    size = 64.dp,
                    iconSize = 36.dp,
                    tint = Color.White,
                    backgroundColor = Color.White.copy(alpha = 0.10f),
                    onClick = onPrevious
                )

                // Play / Pause Gigante (84dp)
                CarModePlayPauseButton(
                    isPlaying = isPlaying,
                    accent = accent,
                    size = 84.dp,
                    iconSize = 44.dp,
                    onClick = onPlayPause
                )

                // Siguiente (64dp)
                CarModeIconButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = "Siguiente",
                    size = 64.dp,
                    iconSize = 36.dp,
                    tint = Color.White,
                    backgroundColor = Color.White.copy(alpha = 0.10f),
                    onClick = onNext
                )

                // Repeat
                val repeatIcon = if (repeatMode == MusicPlayerViewModel.RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                val isRepeatActive = repeatMode != MusicPlayerViewModel.RepeatMode.NONE
                CarModeIconButton(
                    icon = repeatIcon,
                    contentDescription = null,
                    size = 52.dp,
                    iconSize = 26.dp,
                    tint = if (isRepeatActive) accent else Color.White.copy(alpha = 0.5f),
                    backgroundColor = if (isRepeatActive) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                    onClick = onToggleRepeat
                )
            }
        }
    }
}

@Composable
private fun CarModeExitButton(onClick: () -> Unit) {
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.car_mode_exit),
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.car_mode_exit),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CarModeIconButton(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp,
    iconSize: Dp,
    tint: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "car_icon_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
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
private fun CarModePlayPauseButton(
    isPlaying: Boolean,
    accent: Color,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "car_play_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .shadow(20.dp, CircleShape, spotColor = accent)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.85f))
                )
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.35f)) + fadeIn(tween(150)))
                    .togetherWith(scaleOut(tween(100)) + fadeOut(tween(100)))
            },
            label = "car_play_pause_anim"
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Reproducir",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarModeProgressBar(
    currentPosition: Int,
    duration: Int,
    accent: Color,
    onSeek: (Int) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val safeDuration = duration.coerceAtLeast(1)
    val currentSec = if (isDragging) dragPosition.toInt() else currentPosition
    val progressFraction = (currentSec.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Slider(
            value = progressFraction,
            onValueChange = { frac ->
                isDragging = true
                dragPosition = frac * safeDuration
            },
            onValueChangeFinished = {
                onSeek(dragPosition.toInt())
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatCarModeTime(currentSec),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatCarModeTime(duration),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
