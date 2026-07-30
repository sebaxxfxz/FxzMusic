package com.fxzmusic.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

private const val WAVY_SIDES = 9
private const val WAVY_MAX_INDENT = 0.08f
private const val WAVY_ROTATION_DEG_PER_SEC = 18f

fun rememberWavyShape(indent: Float, rotationDegrees: Float): Shape {
    val safeIndent = indent.coerceIn(0f, 0.2f)
    return GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)
        val steps = 180
        val rotationRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        moveTo(cx + r, cy)
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val angle = (2.0 * PI * t).toFloat() + rotationRad
            val radius = r * (1f - safeIndent + safeIndent * cos(WAVY_SIDES * angle))
            lineTo(
                cx + radius * cos(angle),
                cy + radius * kotlin.math.sin(angle)
            )
        }
        close()
    }
}

@Composable
fun WavyPlayButton(
    isPlaying: Boolean,
    accentColor: Color,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 72.dp,
    iconSizeDp: androidx.compose.ui.unit.Dp = 32.dp,
    iconTint: Color = Color.Black
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (360f / WAVY_ROTATION_DEG_PER_SEC).toInt() * 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_rotation_angle"
    )

    val targetIndent = if (isPlaying) WAVY_MAX_INDENT else 0f
    val indent by animateFloatAsState(
        targetValue = targetIndent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wavy_indent"
    )

    val displayRotation = if (isPlaying) rotation else 0f
    val shape = rememberWavyShape(indent = indent, rotationDegrees = displayRotation)

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(sizeDp)
            .scale(if (isPressed) 0.92f else 1f)
            .clip(shape)
            .background(accentColor)
            .clickable(interactionSource = interaction, indication = null) { onPlayPause() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { scaleIn() togetherWith scaleOut() },
            label = "wavy_play_pause"
        ) { playing ->
            val icon: ImageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow
            Icon(
                imageVector = icon,
                contentDescription = if (playing) "Pausar" else "Reproducir",
                tint = iconTint,
                modifier = Modifier.size(iconSizeDp)
            )
        }
    }
}
