package com.fxzmusic.app.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.theme.LocalFxzTheme
import com.fxzmusic.app.ui.theme.ThemeMode
import com.fxzmusic.app.util.ColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun defaultAmbientPalette(): List<Color> = listOf(
    Color(0xFF4158D0),
    Color(0xFFC850C0),
    Color(0xFFFFCC70)
)

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

    // Anti-flicker: Maintain persistent palette across song changes without re-keying remember
    var currentPalette by remember {
        mutableStateOf(currentSong.albumArt.ifEmpty { defaultAmbientPalette() })
    }

    LaunchedEffect(currentSong.id, currentSong.coverUrl, currentSong.filePath) {
        val target = currentSong.coverUrl?.takeIf { it.isNotEmpty() } ?: currentSong.filePath
        val extracted = ColorExtractor.extractDominantColors(context, target)
        if (extracted.isNotEmpty()) {
            currentPalette = extracted
        }
    }

    // Smooth chromatic fade between songs (1000ms FastOutSlowInEasing)
    val animSpec = tween<Color>(1000, easing = FastOutSlowInEasing)

    val primaryColor by animateColorAsState(
        targetValue = currentPalette.getOrElse(0) { Color(0xFF4158D0) },
        animationSpec = animSpec,
        label = "ambient_primary"
    )
    val secondaryColor by animateColorAsState(
        targetValue = currentPalette.getOrElse(1) { Color(0xFFC850C0) },
        animationSpec = animSpec,
        label = "ambient_secondary"
    )
    val tertiaryColor by animateColorAsState(
        targetValue = currentPalette.getOrElse(2) { Color(0xFFFFCC70) },
        animationSpec = animSpec,
        label = "ambient_tertiary"
    )

    var blurredBitmap by remember {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(currentSong.id, currentSong.coverUrl, currentSong.filePath) {
        val target = currentSong.coverUrl?.takeIf { it.isNotEmpty() } ?: currentSong.filePath
        if (!target.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(target)
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
                        val canvas = Canvas(bitmap)
                        val paint = Paint().apply {
                            isAntiAlias = true
                            maskFilter = BlurMaskFilter(
                                40f, BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        blurredBitmap = bitmap
                    } else if (!target.startsWith("http") && File(target).exists()) {
                        try {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(target)
                            val art = retriever.embeddedPicture
                            retriever.release()
                            if (art != null) {
                                val src = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size)
                                if (src != null) {
                                    val scaled = android.graphics.Bitmap.createScaledBitmap(src, 128, 128, true)
                                    val bitmap = scaled.copy(
                                        scaled.config ?: android.graphics.Bitmap.Config.ARGB_8888, true
                                    ) ?: scaled
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(
                                            40f, BlurMaskFilter.Blur.NORMAL
                                        )
                                    }
                                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                                    blurredBitmap = bitmap
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (style) {
            PlayerBackgroundStyle.GRADIENT -> {
                DynamicBackground(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    pulseScale = pulseScale,
                    resetKey = resetKey,
                    modifier = Modifier.fillMaxSize()
                )
            }
            PlayerBackgroundStyle.DEFAULT -> {
                // Ambient Glow (Apple Music style) with Zero-Allocation drawWithCache
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val w = size.width
                            val h = size.height

                            val baseColor = if (isLightTheme) Color(0xFFF3F4F6) else Color(0xFF0A0A0E)
                            val scrimColor = if (isLightTheme) Color(0xFFF3F4F6) else Color(0xFF0A0A0E)

                            // Contrast protection scrims (WCAG AAA)
                            val topScrimBrush = Brush.verticalGradient(
                                colors = listOf(
                                    scrimColor.copy(alpha = if (isLightTheme) 0.85f else 0.75f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h * 0.25f
                            )

                            val bottomScrimBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimColor.copy(alpha = if (isLightTheme) 0.85f else 0.80f),
                                    scrimColor
                                ),
                                startY = h * 0.52f,
                                endY = h
                            )

                            // Primary Ambient Glow Orb (centered behind album cover)
                            val orbRadius1 = (w * 0.85f) * pulseScale
                            val orbCenter1 = Offset(w * 0.50f, h * 0.36f)
                            val primaryAlpha = if (isLightTheme) 0.18f else 0.50f
                            val orbBrush1 = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = primaryAlpha),
                                    primaryColor.copy(alpha = primaryAlpha * 0.45f),
                                    Color.Transparent
                                ),
                                center = orbCenter1,
                                radius = orbRadius1
                            )

                            // Secondary Ambient Mesh Orb (lower right)
                            val orbRadius2 = (w * 0.70f) * pulseScale
                            val orbCenter2 = Offset(w * 0.80f, h * 0.62f)
                            val secondaryAlpha = if (isLightTheme) 0.14f else 0.38f
                            val orbBrush2 = Brush.radialGradient(
                                colors = listOf(
                                    secondaryColor.copy(alpha = secondaryAlpha),
                                    secondaryColor.copy(alpha = secondaryAlpha * 0.40f),
                                    Color.Transparent
                                ),
                                center = orbCenter2,
                                radius = orbRadius2
                            )

                            // Tertiary Ambient Mesh Orb (lower left)
                            val orbRadius3 = (w * 0.65f) * pulseScale
                            val orbCenter3 = Offset(w * 0.20f, h * 0.68f)
                            val tertiaryAlpha = if (isLightTheme) 0.12f else 0.30f
                            val orbBrush3 = Brush.radialGradient(
                                colors = listOf(
                                    tertiaryColor.copy(alpha = tertiaryAlpha),
                                    tertiaryColor.copy(alpha = tertiaryAlpha * 0.30f),
                                    Color.Transparent
                                ),
                                center = orbCenter3,
                                radius = orbRadius3
                            )

                            onDrawBehind {
                                // 1. Base solid canvas
                                drawRect(baseColor)

                                // 2. Ambient glow radial orbs
                                drawCircle(
                                    brush = orbBrush1,
                                    radius = orbRadius1,
                                    center = orbCenter1
                                )
                                drawCircle(
                                    brush = orbBrush2,
                                    radius = orbRadius2,
                                    center = orbCenter2
                                )
                                drawCircle(
                                    brush = orbBrush3,
                                    radius = orbRadius3,
                                    center = orbCenter3
                                )

                                // 3. Top and bottom scrims for WCAG AAA text/controls contrast
                                drawRect(brush = topScrimBrush)
                                drawRect(brush = bottomScrimBrush)
                            }
                        }
                )
            }
            PlayerBackgroundStyle.BLUR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val h = size.height
                            val baseColor = if (isLightTheme) Color(0xFFF3F4F6) else Color(0xFF0A0A0E)
                            val scrimColor = if (isLightTheme) Color(0xFFF3F4F6) else Color(0xFF0A0A0E)

                            val topScrimBrush = Brush.verticalGradient(
                                colors = listOf(
                                    scrimColor.copy(alpha = if (isLightTheme) 0.85f else 0.75f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h * 0.25f
                            )

                            val bottomScrimBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimColor.copy(alpha = if (isLightTheme) 0.75f else 0.70f),
                                    scrimColor.copy(alpha = if (isLightTheme) 0.95f else 0.90f)
                                ),
                                startY = h * 0.45f,
                                endY = h
                            )

                            val surfaceOverlayColor = if (isLightTheme) {
                                Color.White.copy(alpha = 0.20f)
                            } else {
                                Color.Black.copy(alpha = 0.35f)
                            }

                            onDrawBehind {
                                drawRect(baseColor)
                                val bitmap = blurredBitmap
                                if (bitmap != null && !bitmap.isRecycled) {
                                    drawIntoCanvas { canvas ->
                                        val paint = Paint().apply {
                                            alpha = if (isLightTheme) 150 else 180
                                        }
                                        canvas.nativeCanvas.drawBitmap(
                                            bitmap,
                                            null,
                                            Rect(0, 0, size.width.toInt(), size.height.toInt()),
                                            paint
                                        )
                                    }
                                }
                                drawRect(surfaceOverlayColor)
                                drawRect(brush = topScrimBrush)
                                drawRect(brush = bottomScrimBrush)
                            }
                        }
                )
            }
        }
    }
}
