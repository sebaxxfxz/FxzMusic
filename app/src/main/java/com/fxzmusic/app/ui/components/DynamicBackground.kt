package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.theme.LocalFxzTheme
import com.fxzmusic.app.ui.theme.ThemeMode
import com.fxzmusic.app.util.ColorExtractor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicBackground(
    currentSong: Song,
    isPlaying: Boolean,
    pulseScale: Float,
    resetKey: Any?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGlass = LocalFxzTheme.current.mode == ThemeMode.AMOLED

    var dominantColors by remember(currentSong.id) {
        mutableStateOf(currentSong.albumArt)
    }

    LaunchedEffect(currentSong.id, currentSong.coverUrl, currentSong.filePath) {
        val target = currentSong.coverUrl?.takeIf { it.isNotEmpty() } ?: currentSong.filePath
        dominantColors = ColorExtractor.extractDominantColors(context, target)
    }

    val color1 by animateColorAsState(
        targetValue = dominantColors.getOrElse(0) { Color(0xFF4158D0) },
        animationSpec = tween(1400, easing = LinearEasing),
        label = "bg_color1"
    )
    val color2 by animateColorAsState(
        targetValue = dominantColors.getOrElse(1) { Color(0xFFC850C0) },
        animationSpec = tween(1400, easing = LinearEasing),
        label = "bg_color2"
    )
    val color3 by animateColorAsState(
        targetValue = dominantColors.getOrElse(2) { Color(0xFFFFCC70) },
        animationSpec = tween(1400, easing = LinearEasing),
        label = "bg_color3"
    )

    val safeColor1 = remember(color1, isGlass) { sanitizeOrbColor(color1, isGlass) }
    val safeColor2 = remember(color2, isGlass) { sanitizeOrbColor(color2, isGlass) }
    val safeColor3 = remember(color3, isGlass) { sanitizeOrbColor(color3, isGlass) }
    val alphaFactor = if (isGlass) 0.35f else 1.0f

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    val orbPhase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb_phase1"
    )
    val orbPhase2 by infiniteTransition.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb_phase2"
    )
    val orbPhase3 by infiniteTransition.animateFloat(
        initialValue = 240f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(50000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb_phase3"
    )

    val playbackAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.45f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playback_alpha"
    )

    val noisePattern = remember {
        val size = 128
        val pixels = IntArray(size * size)
        val rng = java.util.Random(42)
        for (i in pixels.indices) {
            val v = rng.nextInt(18) - 9
            val c = (128 + v).coerceIn(0, 255)
            pixels[i] = (25 shl 24) or (c shl 16) or (c shl 8) or c
        }
        android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888)
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
            val w = size.width
            val h = size.height

            val rad1 = w * 0.65f
            val orbX1 = w * 0.5f + cos(Math.toRadians(orbPhase1.toDouble())).toFloat() * w * 0.18f
            val orbY1 = h * 0.35f + sin(Math.toRadians(orbPhase1.toDouble())).toFloat() * h * 0.12f
            drawCircle(
                brush = ShaderBrush(RadialGradientShader(
                    center = Offset(orbX1, orbY1),
                    radius = rad1,
                    colors = listOf(safeColor1.copy(alpha = 0.40f * playbackAlpha * alphaFactor), safeColor1.copy(alpha = 0f))
                )),
                radius = rad1,
                center = Offset(orbX1, orbY1)
            )

            val rad2 = w * 0.55f
            val orbX2 = w * 0.65f + cos(Math.toRadians(orbPhase2.toDouble())).toFloat() * w * 0.15f
            val orbY2 = h * 0.65f + sin(Math.toRadians(orbPhase2.toDouble())).toFloat() * h * 0.15f
            drawCircle(
                brush = ShaderBrush(RadialGradientShader(
                    center = Offset(orbX2, orbY2),
                    radius = rad2,
                    colors = listOf(safeColor2.copy(alpha = 0.30f * playbackAlpha * alphaFactor), safeColor2.copy(alpha = 0f))
                )),
                radius = rad2,
                center = Offset(orbX2, orbY2)
            )

            val rad3 = w * 0.5f
            val orbX3 = w * 0.35f + cos(Math.toRadians(orbPhase3.toDouble())).toFloat() * w * 0.2f
            val orbY3 = h * 0.8f + sin(Math.toRadians(orbPhase3.toDouble())).toFloat() * h * 0.1f
            drawCircle(
                brush = ShaderBrush(RadialGradientShader(
                    center = Offset(orbX3, orbY3),
                    radius = rad3,
                    colors = listOf(safeColor3.copy(alpha = 0.22f * playbackAlpha * alphaFactor), safeColor3.copy(alpha = 0f))
                )),
                radius = rad3,
                center = Offset(orbX3, orbY3)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val maxRadius = kotlin.math.sqrt(cx * cx + cy * cy)

            drawCircle(
                brush = ShaderBrush(RadialGradientShader(
                    center = Offset(cx, cy),
                    radius = maxRadius,
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.4f),
                        Color.Black.copy(alpha = 0.8f)
                    ),
                    colorStops = listOf(0f, 0.6f, 1f)
                )),
                radius = maxRadius,
                center = Offset(cx, cy)
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.18f
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val bitmapShader = android.graphics.BitmapShader(
                    noisePattern,
                    android.graphics.Shader.TileMode.REPEAT,
                    android.graphics.Shader.TileMode.REPEAT
                )
                val paint = android.graphics.Paint().apply {
                    shader = bitmapShader
                    alpha = 18
                }
                canvas.nativeCanvas.drawBitmap(
                    noisePattern,
                    0f,
                    0f,
                    paint
                )
            }
        }

        if (isGlass) {
            var glassOverlayPressed by remember { mutableStateOf(false) }
            LaunchedEffect(resetKey) {
                glassOverlayPressed = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x06FFFFFF),
                                Color(0x12FFFFFF).copy(alpha = if (glassOverlayPressed) 0.12f else 0.04f),
                                Color(0x08FFFFFF)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                glassOverlayPressed = true
                                tryAwaitRelease()
                                glassOverlayPressed = false
                            }
                        )
                    }
            )
        }
    }
}

private fun sanitizeOrbColor(color: Color, isAmoled: Boolean): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val maxBrightness = if (isAmoled) 0.30f else 0.65f
    if (hsv[2] > maxBrightness) {
        hsv[2] = maxBrightness
    }
    if (hsv[1] < 0.25f) {
        hsv[2] = if (isAmoled) 0.18f else 0.35f
    }
    return Color(android.graphics.Color.HSVToColor(hsv))
}
