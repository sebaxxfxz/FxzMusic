package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.Song
import com.fxzmusic.app.util.buildCoverRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import kotlin.math.abs
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Stable
data class MiniPlayerState(
    val currentSong: Song,
    val isPlaying: Boolean,
    val isLiked: Boolean,
    val currentPosition: Int,
    val duration: Int,
    val playlistName: String?
)

@Immutable
data class MiniPlayerCallbacks(
    val onPlayPause: () -> Unit,
    val onClick: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Int) -> Unit,
    val onToggleLike: () -> Unit,
    val onSwipeToDismiss: () -> Unit
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayerWithVisualizer(
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    var swipeOffsetY by remember { mutableFloatStateOf(0f) }

    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "swipe_offset_x"
    )

    val animatedSwipeOffsetY by animateFloatAsState(
        targetValue = swipeOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "swipe_offset_y"
    )

    val progressFraction = if (state.duration > 0) {
        (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(surfaceColor) 
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(26.dp)
            )
            .graphicsLayer {
                translationX = animatedSwipeOffset
                translationY = animatedSwipeOffsetY
                alpha = (1f - (animatedSwipeOffsetY / 160f)).coerceIn(0f, 1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (swipeOffsetY > 70f) {
                            callbacks.onSwipeToDismiss()
                        } else if (swipeOffsetX < -100f) {
                            callbacks.onNext()
                        } else if (swipeOffsetX > 100f) {
                            callbacks.onPrevious()
                        }
                        swipeOffsetX = 0f
                        swipeOffsetY = 0f
                    },
                    onDragCancel = {
                        swipeOffsetX = 0f
                        swipeOffsetY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (abs(dragAmount.y) > abs(dragAmount.x) && (dragAmount.y > 0 || swipeOffsetY > 0)) {
                        swipeOffsetY = (swipeOffsetY + dragAmount.y).coerceIn(0f, 140f)
                    } else {
                        swipeOffsetX = (swipeOffsetX + dragAmount.x).coerceIn(-180f, 180f)
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { callbacks.onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 3.dp.toPx()
                    val inset = strokeW / 2f
                    val diameter = size.minDimension - strokeW

                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeW)
                    )

                    if (progressFraction > 0f) {
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = strokeW,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                with(sharedTransitionScope) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.currentSong.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = state.currentSong.title,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "cover-art-${state.currentSong.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.currentSong.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(state.currentSong.artist)
                        if (state.playlistName != null) {
                            append(" • ${state.playlistName}")
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                val playInteraction = remember { MutableInteractionSource() }
                val isPlayPressed by playInteraction.collectIsPressedAsState()
                val playBtnScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) 0.88f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
                    label = "mini_play_scale"
                )

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .scale(playBtnScale)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            onClick = callbacks.onPlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = {
                            (scaleIn(tween(120)) + fadeIn(tween(120))).togetherWith(scaleOut(tween(120)) + fadeOut(tween(120)))
                        },
                        label = "mini_play_icon"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = callbacks.onNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Siguiente",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
