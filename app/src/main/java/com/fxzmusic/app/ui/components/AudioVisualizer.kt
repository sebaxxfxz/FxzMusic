package com.fxzmusic.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioVisualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val animatedFractions = (0 until barCount).map { i ->
        key(i) {
            val duration = 280 + i * 90
            infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = if (isPlaying) 1f else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing, delayMillis = i * 55),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
        }
    }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        if (totalWidth <= 0f || totalHeight <= 0f || barCount <= 0) return@Canvas

        val spacingPx = 3.dp.toPx()
        val totalSpacing = spacingPx * (barCount - 1)
        val availableWidth = (totalWidth - totalSpacing).coerceAtLeast(0f)
        val barWidth = (availableWidth / barCount).coerceAtLeast(0f)
        if (barWidth <= 0f) return@Canvas

        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        for (i in 0 until barCount) {
            val fraction = animatedFractions[i].value.coerceIn(0.15f, 1f)
            val barHeight = (totalHeight * fraction).coerceIn(0f, totalHeight)
            val left = i * (barWidth + spacingPx)
            val top = totalHeight - barHeight

            val color = if (i % 2 == 0) accent else accent.copy(alpha = 0.65f)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
