package com.example.fxzmusic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val theme = LocalFxzTheme.current
    val accent = theme.accent
    var loadingProgress by remember { mutableStateOf(0f) }
    var exitStarted by remember { mutableStateOf(false) }

    val screenAlpha by animateFloatAsState(
        targetValue = if (exitStarted) 0f else 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        finishedListener = { if (exitStarted) onFinished() },
        label = "ea"
    )

    val screenScale by animateFloatAsState(
        targetValue = if (exitStarted) 1.04f else 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "es"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Pulsing background ambient glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Scanner line offset
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_offset"
    )

    LaunchedEffect(Unit) {
        val duration = 2500L
        val interval = 50L
        val steps = (duration / interval).toInt()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            // Easing cubic
            val eased = 1f - (1f - t) * (1f - t) * (1f - t)
            val jitter = Random.nextFloat() * 0.015f
            loadingProgress = (eased + jitter).coerceIn(0f, 1f)
            delay(interval)
        }
        loadingProgress = 1f
        delay(500)
        exitStarted = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .scale(screenScale)
            .background(Color(0xFF0E0E0E)), // surface-container-lowest (#0E0E0E)
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric Ambient Glow (Glassmorphism underlying effect)
        Box(
            modifier = Modifier
                .size(400.dp)
                .alpha(glowPulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main content column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1.2f))

            // Logo Cluster
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(IntrinsicSize.Min)
            ) {
                // Wordmark: Large bold title uppercase with track tracking
                Text(
                    text = "FXZ MUSIC",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )

                // Scanning Neon Line Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1.2f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(9999.dp))
                ) {
                    // Base faint line
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // Glowing scanner element
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val scannerWidth = width * 0.35f
                        val startX = (scanOffset * width) - (scannerWidth / 2)

                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accent.copy(alpha = 0.3f),
                                    accent,
                                    accent.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(startX, 0f),
                            size = Size(scannerWidth, size.height)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Progress Area
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "System Initialization",
                        color = Cinematic_PlatinumText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    val percentage = (loadingProgress * 100).toInt()
                    Text(
                        text = String.format("%02d%%", percentage),
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Progress Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(9999.dp))
                ) {
                    // Progress Fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(loadingProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.8f),
                                        accent
                                    )
                                )
                            )
                    ) {
                        // Inner highlight for gloss
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}
