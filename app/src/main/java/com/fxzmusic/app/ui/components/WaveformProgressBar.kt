package com.fxzmusic.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.data.formatTime
import java.util.Random
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private const val BAR_COUNT = 54

@Composable
fun WaveformProgressBar(
    songId: String,
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.White.copy(alpha = 0.22f)
) {
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
        label = "waveform_progress_anim"
    )
    val progress = if (isDragging) rawProgress else animatedProgress

    val barFractions = remember(songId) {
        val seed = songId.hashCode().toLong()
        val random = Random(seed)
        FloatArray(BAR_COUNT) { index ->
            val t = index.toFloat() / (BAR_COUNT - 1).toFloat()
            val envelope = (sin(t * PI).toFloat()).pow(0.45f).coerceIn(0.22f, 1f)
            val jitter = 0.35f + random.nextFloat() * 0.65f
            (envelope * jitter).coerceIn(0.18f, 1.0f)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
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
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .align(Alignment.Center)
            ) {
                val totalWidth = size.width
                val totalHeight = size.height
                if (totalWidth <= 0f || totalHeight <= 0f) return@Canvas

                val slotWidth = totalWidth / BAR_COUNT
                val barWidth = (slotWidth * 0.68f).coerceIn(2f, 8.dp.toPx())
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                val minBarHeight = 4.dp.toPx()
                val maxBarHeight = totalHeight * 0.88f

                for (i in 0 until BAR_COUNT) {
                    val fraction = barFractions[i]
                    val barHeight = (maxBarHeight * fraction).coerceAtLeast(minBarHeight)
                    val centerX = (i + 0.5f) * slotWidth
                    val left = centerX - barWidth / 2f
                    val top = (totalHeight - barHeight) / 2f

                    val barFractionX = centerX / totalWidth
                    val isPlayed = barFractionX <= progress

                    drawRoundRect(
                        color = if (isPlayed) accentColor else inactiveColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(displayPosition),
                color = if (isDragging) accentColor else Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = formatTime(duration),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
