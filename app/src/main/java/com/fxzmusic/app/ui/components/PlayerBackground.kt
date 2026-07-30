package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.theme.LocalFxzTheme
import com.fxzmusic.app.ui.theme.ThemeMode
import com.fxzmusic.app.util.ColorExtractor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerBackground(
    currentSong: Song,
    isPlaying: Boolean,
    pulseScale: Float,
    resetKey: Any?,
    modifier: Modifier = Modifier,
    style: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT
) {
    val context = LocalContext.current
    val isLightTheme = LocalFxzTheme.current.mode == ThemeMode.LIGHT

    var paletteColors by remember(currentSong.id) {
        mutableStateOf<List<Color>?>(null)
    }

    LaunchedEffect(currentSong.id, currentSong.coverUrl, currentSong.filePath) {
        val target = currentSong.coverUrl?.takeIf { it.isNotEmpty() } ?: currentSong.filePath
        paletteColors = ColorExtractor.extractDominantColors(context, target)
    }

    val vibrantColor by animateColorAsState(
        targetValue = paletteColors?.firstOrNull() ?: Color(0xFF4158D0),
        animationSpec = tween(800, easing = LinearEasing),
        label = "bg_vibrant"
    )
    val mutedColor by animateColorAsState(
        targetValue = paletteColors?.getOrElse(1) { Color(0xFFC850C0).copy(alpha = 0.8f) }
            ?: Color(0xFFC850C0).copy(alpha = 0.8f),
        animationSpec = tween(800, easing = LinearEasing),
        label = "bg_muted"
    )
    val darkColor by animateColorAsState(
        targetValue = paletteColors?.lastOrNull() ?: Color(0xFFFFCC70).copy(alpha = 0.4f),
        animationSpec = tween(800, easing = LinearEasing),
        label = "bg_dark"
    )

    var blurredBitmap by remember(currentSong.id) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(currentSong.id, currentSong.coverUrl) {
        if (!currentSong.coverUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(currentSong.coverUrl)
                        .size(128, 128)
                        .scale(Scale.FILL)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val drawable = result.drawable
                        val src = drawable.toBitmap(128, 128)
                        val bitmap = src.copy(
                            src.config ?: android.graphics.Bitmap.Config.ARGB_8888, false
                        ) ?: src
                        val canvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            maskFilter = android.graphics.BlurMaskFilter(
                                40f, android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        blurredBitmap = bitmap
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val gradientColors = if (isLightTheme) {
        listOf(
            vibrantColor.copy(alpha = 0.85f),
            mutedColor.copy(alpha = 0.65f),
            darkColor.copy(alpha = 0.55f)
        )
    } else {
        listOf(
            vibrantColor.copy(alpha = 0.9f),
            mutedColor.copy(alpha = 0.75f),
            darkColor.copy(alpha = 0.65f)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        DynamicBackground(
            currentSong = currentSong,
            isPlaying = isPlaying,
            pulseScale = pulseScale,
            resetKey = resetKey,
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    when (style) {
                        PlayerBackgroundStyle.DEFAULT -> {
                            if (blurredBitmap != null) {
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply { alpha = 40 }
                                    canvas.nativeCanvas.drawBitmap(
                                        blurredBitmap!!,
                                        null,
                                        android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt()),
                                        paint
                                    )
                                }
                            }
                            drawRect(Color(0xFF0A0A0C))
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = gradientColors,
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                        PlayerBackgroundStyle.BLUR -> {
                            if (blurredBitmap != null) {
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply { alpha = 180 }
                                    canvas.nativeCanvas.drawBitmap(
                                        blurredBitmap!!,
                                        null,
                                        android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt()),
                                        paint
                                    )
                                }
                            }
                            val surfaceColor = Color.Black.copy(alpha = if (isLightTheme) 0.15f else 0.35f)
                            drawRect(surfaceColor)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = if (isLightTheme) 0.4f else 0.6f)
                                    ),
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                        PlayerBackgroundStyle.GRADIENT -> {
                            val surfaceColor = Color.Black.copy(alpha = if (isLightTheme) 0.1f else 0.3f)
                            drawRect(surfaceColor)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = gradientColors.map { color ->
                                        color.copy(alpha = (color.alpha * 1.2f).coerceAtMost(1f))
                                    },
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                    }
                }
        )
    }
}
