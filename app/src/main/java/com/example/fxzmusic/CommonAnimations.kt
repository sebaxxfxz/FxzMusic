package com.example.fxzmusic

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

fun bounceInAnimation(delayMillis: Int = 0): EnterTransition = scaleIn(
    initialScale = 0.3f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(tween(260, delayMillis))

fun bounceOutAnimation(): ExitTransition = scaleOut(
    targetScale = 0.4f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
) + fadeOut(tween(180))

fun popInEnhanced(delayMillis: Int = 0): EnterTransition = scaleIn(
    initialScale = 0.2f,
    animationSpec = spring(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessMedium
    )
) + fadeIn(tween(300, delayMillis))

fun morphEnterTransition(delayMillis: Int = 0): EnterTransition = scaleIn(
    initialScale = 0.88f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(tween(320, delayMillis, EaseOutBack))

fun morphExitTransition(): ExitTransition = scaleOut(
    targetScale = 0.88f,
    animationSpec = tween(220, easing = CubicBezierEasing(0.76f, 0f, 0.24f, 1f))
) + fadeOut(tween(160))

fun staggeredSlideIn(index: Int, baseDelayMs: Int = 0, stepMs: Int = 50): EnterTransition =
    slideInFromBottomWithScale(
        delayMillis = baseDelayMs + index * stepMs,
        useSpring = true
    )
