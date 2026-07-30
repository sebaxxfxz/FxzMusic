package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.formatTime
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun FxzProgressBar(
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableIntStateOf(currentPosition) }
    var barSize by remember { mutableStateOf(IntSize.Zero) }

    val currentPositionState by rememberUpdatedState(currentPosition)
    val onSeekState by rememberUpdatedState(onSeek)
    val durationState by rememberUpdatedState(duration)

    val displayPosition = if (isDragging) dragPosition else currentPosition
    val rawProgress = if (duration > 0) (displayPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (isDragging) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progress_animate"
    )
    val progress = if (isDragging) rawProgress else animatedProgress

    val trackHeightDp by animateFloatAsState(
        targetValue = if (isDragging) 8f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "track_height"
    )

    val thumbRadiusDp by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "thumb_radius"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clipToBounds()
                .onSizeChanged { barSize = it }
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0 && barSize.width > 0) {
                            val seekPos = ((offset.x / barSize.width) * duration).toInt().coerceIn(0, duration)
                            onSeekState(seekPos)
                        }
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragPosition = currentPositionState
                        },
                        onDragEnd = {
                            isDragging = false
                            if (durationState > 0) {
                                onSeekState(dragPosition)
                            }
                        },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmountX ->
                        change.consume()
                        if (duration > 0 && barSize.width > 0) {
                            val delta = (dragAmountX / barSize.width * duration).toInt()
                            dragPosition = (dragPosition + delta).coerceIn(0, duration)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.Center)
            ) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f
                val trackH = with(density) { trackHeightDp.dp.toPx() }
                val thumbR = with(density) { thumbRadiusDp.dp.toPx() }

                val progressX = (w * progress).coerceIn(0f, w)

                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(0f, centerY),
                    end = Offset(w, centerY),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )

                if (progressX > 0f) {
                    drawLine(
                        color = accent,
                        start = Offset(0f, centerY),
                        end = Offset(progressX, centerY),
                        strokeWidth = trackH,
                        cap = StrokeCap.Round
                    )
                }

                if (thumbR > 0f) {
                    
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = thumbR + with(density) { 3.dp.toPx() },
                        center = Offset(progressX, centerY)
                    )
                    
                    drawCircle(
                        color = Color.White,
                        radius = thumbR,
                        center = Offset(progressX, centerY)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(displayPosition),
                color = if (isDragging) accent else Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                formatTime(duration),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun DrawScope.drawSquigglyPath(
    startX: Float,
    endX: Float,
    centerY: Float,
    amplitude: Float,
    waveLength: Float,
    phase: Float,
    color: Color,
    strokeWidth: Float
) {
    if (amplitude < 0.1f || waveLength < 1f) {
        drawLine(
            color = color,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        return
    }

    val path = Path()
    path.moveTo(startX, centerY)

    val steps = ((endX - startX) / 2f).toInt().coerceAtLeast(10)
    val dx = (endX - startX) / steps

    for (i in 0..steps) {
        val x = startX + i * dx
        val normalizedX = (x - startX) / waveLength
        val ampVariation = 1f + 0.15f * sin((normalizedX * 0.7f * PI).toFloat())
        val y = centerY + amplitude * ampVariation * sin((normalizedX * 2f * PI + phase).toFloat())

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            val prevX = startX + (i - 1) * dx
            val prevNormalizedX = (prevX - startX) / waveLength
            val prevAmpVariation = 1f + 0.15f * sin((prevNormalizedX * 0.7f * PI).toFloat())
            val prevY = centerY + amplitude * prevAmpVariation * sin((prevNormalizedX * 2f * PI + phase).toFloat())
            val midX = (prevX + x) / 2f
            path.cubicTo(midX, prevY, midX, y, x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun Float.fromPx(density: androidx.compose.ui.unit.Density): androidx.compose.ui.unit.Dp {
    return with(density) { this@fromPx.toDp() }
}
