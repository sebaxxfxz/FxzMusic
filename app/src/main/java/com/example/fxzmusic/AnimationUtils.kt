package com.example.fxzmusic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutQuart = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)
private const val FADE_LEAD_MS = 80

fun slideInFromBottomWithScale(
    durationMillis: Int = 400,
    delayMillis: Int = 0,
    useSpring: Boolean = false
): EnterTransition = if (useSpring) {
    slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + scaleIn(
        initialScale = 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(tween(durationMillis, delayMillis, easing = EaseOutExpo))
} else {
    slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = tween(durationMillis, delayMillis, easing = EaseOutExpo)
    ) + scaleIn(
        initialScale = 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(tween(durationMillis - FADE_LEAD_MS, delayMillis, easing = EaseOutExpo))
}

fun slideOutToBottom(durationMillis: Int = 380) = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(durationMillis, easing = EaseInOutQuart)
) + scaleOut(
    targetScale = 0.88f,
    animationSpec = tween(durationMillis, easing = EaseInOutQuart)
) + fadeOut(tween(durationMillis - 80))

fun popInAnimation(
    durationMillis: Int = 420,
    delayMillis: Int = 0
) = scaleIn(
    initialScale = 0.4f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
) + fadeIn(tween(durationMillis - 80, delayMillis))

@Composable
fun AnimatedCardItem(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInFromBottomWithScale(delayMillis = delayMs, useSpring = true),
        exit = slideOutToBottom()
    ) {
        Box(modifier = modifier) {
            content()
        }
    }
}

