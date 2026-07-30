package com.fxzmusic.app.ui.components

import com.fxzmusic.app.viewmodel.MusicPlayerViewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun FullPlayerControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: MusicPlayerViewModel.RepeatMode,
    showLyrics: Boolean,
    showQueue: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowLyricsToggle: () -> Unit,
    onShowQueueToggle: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onUserInteraction: () -> Unit,
    isSleepTimerActive: Boolean = false,
    sleepTimerRemainingMs: Long = 0L,
    onShowSleepTimerToggle: () -> Unit = {},
    currentPlaybackSpeed: Float = 1f,
    onShowSpeedToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        val shuffleSplit = remember { Animatable(0f) }
        val repeatRotation = remember { Animatable(0f) }
        val prevOffset = remember { Animatable(0f) }
        val nextOffset = remember { Animatable(0f) }
        val rippleScale = remember { Animatable(1f) }
        val rippleAlpha = remember { Animatable(0f) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shuffleInteraction = remember { MutableInteractionSource() }
            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
            val shuffleScale by animateFloatAsState(
                targetValue = if (isShufflePressed) 0.82f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "shuffle_scale"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(shuffleScale)
                    .clickable(interactionSource = shuffleInteraction, indication = null) {
                        onUserInteraction()
                        coroutineScope.launch {
                            shuffleSplit.snapTo(0f)
                            shuffleSplit.animateTo(1f, spring(dampingRatio = 0.40f, stiffness = Spring.StiffnessMedium))
                            shuffleSplit.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
                        }
                        onToggleShuffle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                )

                val splitVal = shuffleSplit.value
                val tintColor = if (isShuffleEnabled) accent else Color.White.copy(alpha = 0.5f)

                if (splitVal > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset(x = (8 * splitVal).dp, y = (-8 * splitVal).dp)
                            .graphicsLayer {
                                scaleX = 1f + (0.25f * splitVal)
                                scaleY = 1f + (0.25f * splitVal)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = (-8 * splitVal).dp, y = (8 * splitVal).dp)
                            .graphicsLayer {
                                scaleX = 1f + (0.25f * splitVal)
                                scaleY = 1f + (0.25f * splitVal)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = null,
                            tint = accent.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = tintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            val prevInteraction = remember { MutableInteractionSource() }
            val isPrevPressed by prevInteraction.collectIsPressedAsState()
            val prevScale by animateFloatAsState(
                targetValue = if (isPrevPressed) 0.82f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "prev_scale"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(prevScale)
                    .clickable(interactionSource = prevInteraction, indication = null) {
                        onUserInteraction()
                        coroutineScope.launch {
                            prevOffset.snapTo(0f)
                            prevOffset.animateTo(-22f, spring(dampingRatio = 0.30f, stiffness = Spring.StiffnessHigh))
                            prevOffset.animateTo(0f, spring(dampingRatio = 0.40f, stiffness = Spring.StiffnessMediumLow))
                        }
                        onPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                val offsetVal = prevOffset.value
                val absOffset = if (offsetVal < 0) -offsetVal else offsetVal
                val stretch = absOffset / 22f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1f + stretch * 0.40f
                            scaleY = 1f - stretch * 0.20f
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                )

                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { translationX = offsetVal }
                )
            }

            val playInteraction = remember { MutableInteractionSource() }
            val isPressed by playInteraction.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (isPressed) 0.90f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                label = "play_scale"
            )

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(playScale)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            onClick = {
                                onUserInteraction()
                                onPlayPause()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val morphProgress by animateFloatAsState(
                        targetValue = if (isPlaying) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                        label = "play_pause_morph"
                    )

                    Canvas(modifier = Modifier.size(32.dp)) {
                        val w = size.width
                        val h = size.height
                        val p = morphProgress.coerceIn(0f, 1f)

                        val barW = 5.5.dp.toPx()
                        val barH = 22.dp.toPx()
                        val corner = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx())
                        val topY = (h - barH) / 2f

                        val leftX = androidx.compose.ui.util.lerp(w * 0.28f, w * 0.24f, p)
                        val rightX = androidx.compose.ui.util.lerp(w * 0.46f, w * 0.62f, p)

                        val leftBarHeight = androidx.compose.ui.util.lerp(barH, barH, p)
                        val rightBarHeight = androidx.compose.ui.util.lerp(barH * 0.1f, barH, p)

                        drawRoundRect(
                            color = Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(leftX, topY),
                            size = androidx.compose.ui.geometry.Size(barW, leftBarHeight),
                            cornerRadius = corner
                        )

                        drawRoundRect(
                            color = Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(rightX, topY + (barH - rightBarHeight) / 2f),
                            size = androidx.compose.ui.geometry.Size(barW, rightBarHeight),
                            cornerRadius = corner
                        )

                        if (p < 0.95f) {
                            val triPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.32f, topY)
                                lineTo(w * 0.78f, h / 2f)
                                lineTo(w * 0.32f, topY + barH)
                                close()
                            }
                            drawPath(
                                path = triPath,
                                color = Color.Black,
                                alpha = (1f - p * 1.2f).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }

            val nextInteraction = remember { MutableInteractionSource() }
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val nextScale by animateFloatAsState(
                targetValue = if (isNextPressed) 0.82f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "next_scale"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(nextScale)
                    .clickable(interactionSource = nextInteraction, indication = null) {
                        onUserInteraction()
                        coroutineScope.launch {
                            nextOffset.snapTo(0f)
                            nextOffset.animateTo(22f, spring(dampingRatio = 0.30f, stiffness = Spring.StiffnessHigh))
                            nextOffset.animateTo(0f, spring(dampingRatio = 0.40f, stiffness = Spring.StiffnessMediumLow))
                        }
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                val offsetVal = nextOffset.value
                val absOffset = if (offsetVal < 0) -offsetVal else offsetVal
                val stretch = absOffset / 22f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1f + stretch * 0.40f
                            scaleY = 1f - stretch * 0.20f
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                )

                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Siguiente",
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { translationX = offsetVal }
                )
            }

            val repeatInteraction = remember { MutableInteractionSource() }
            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
            val repeatScale by animateFloatAsState(
                targetValue = if (isRepeatPressed) 0.82f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "repeat_scale"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(repeatScale)
                    .graphicsLayer { rotationZ = repeatRotation.value }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                    .clickable(interactionSource = repeatInteraction, indication = null) {
                        onUserInteraction()
                        coroutineScope.launch {
                            repeatRotation.animateTo(repeatRotation.value + 360f, tween(350, easing = FastOutSlowInEasing))
                        }
                        onToggleRepeat()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (repeatMode) {
                        MusicPlayerViewModel.RepeatMode.ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    },
                    contentDescription = "Repetición",
                    tint = when (repeatMode) {
                        MusicPlayerViewModel.RepeatMode.NONE -> Color.White.copy(alpha = 0.5f)
                        else -> accent
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Determine active selected tab index for sliding pill indicator
        val selectedTab = when {
            showLyrics -> 0
            showQueue -> 2
            currentPlaybackSpeed != 1f -> 1
            isSleepTimerActive -> 3
            else -> -1
        }

        // Bottom Nav Bar (Same glassmorphic design & sliding pill as Home BottomNavBar)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(30.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val speedText = if (currentPlaybackSpeed != 1f) {
                    val formatted = if (currentPlaybackSpeed % 1f == 0f) "${currentPlaybackSpeed.toInt()}" else "$currentPlaybackSpeed"
                    "${formatted}x"
                } else null

                val timerText = if (isSleepTimerActive) {
                    if (sleepTimerRemainingMs > 0) {
                        val mins = (sleepTimerRemainingMs / 60_000).toInt()
                        val secs = ((sleepTimerRemainingMs % 60_000) / 1000).toInt()
                        if (mins > 0) "${mins}m" else "${secs}s"
                    } else "ON"
                } else null

                val tabs = listOf(
                    Triple("LETRA", Icons.Filled.MicNone, showLyrics to null),
                    Triple("VEL.", Icons.Filled.Speed, (currentPlaybackSpeed != 1f) to speedText),
                    Triple("COLA", Icons.AutoMirrored.Filled.QueueMusic, showQueue to null),
                    Triple("TIMER", Icons.Filled.Timer, isSleepTimerActive to timerText)
                )
                val onActions = listOf(onShowLyricsToggle, onShowSpeedToggle, onShowQueueToggle, onShowSleepTimerToggle)

                val tabWidth = maxWidth / tabs.size
                val indicatorOffset by animateDpAsState(
                    targetValue = if (selectedTab >= 0) tabWidth * selectedTab else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "fullplayer_nav_indicator_offset"
                )

                var isMoving by remember { mutableStateOf(false) }
                LaunchedEffect(selectedTab) {
                    if (selectedTab >= 0) {
                        isMoving = true
                        delay(250)
                        isMoving = false
                    }
                }

                val capsuleStretchScaleX by animateFloatAsState(
                    targetValue = if (isMoving) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "fullplayer_capsule_stretch"
                )

                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (selectedTab >= 0) 1f else 0f,
                    animationSpec = tween(200),
                    label = "fullplayer_indicator_alpha"
                )

                // Sliding Capsule Indicator Pill
                if (indicatorAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .padding(vertical = 5.dp, horizontal = 5.dp)
                            .graphicsLayer {
                                scaleX = capsuleStretchScaleX
                                alpha = indicatorAlpha
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.32f),
                                        accent.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.65f),
                                        accent.copy(alpha = 0.20f)
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.zip(onActions).forEachIndexed { index, (tab, onAction) ->
                        val (label, icon, statePair) = tab
                        val (isItemActive, badgeText) = statePair
                        val isSelected = selectedTab == index

                        val interaction = remember { MutableInteractionSource() }
                        val isPressed by interaction.collectIsPressedAsState()

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected || isItemActive) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            animationSpec = tween(220),
                            label = "fullplayer_nav_color_$index"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 0.84f
                                isSelected -> 1.15f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = if (isPressed) Spring.DampingRatioNoBouncy else Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "fullplayer_nav_scale_$index"
                        )
                        val iconRotation by animateFloatAsState(
                            targetValue = if (isSelected) -4f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "fullplayer_nav_rot_$index"
                        )
                        val labelAlpha by animateFloatAsState(
                            targetValue = if (isSelected || isItemActive) 1f else 0.55f,
                            animationSpec = tween(200),
                            label = "fullplayer_nav_label_alpha_$index"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    onUserInteraction()
                                    onAction()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        rotationZ = iconRotation
                                    }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = iconColor.copy(alpha = labelAlpha),
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected || isItemActive) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                                if (badgeText != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(accent.copy(alpha = 0.25f))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = accent,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
