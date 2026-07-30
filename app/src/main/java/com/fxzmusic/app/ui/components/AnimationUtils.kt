package com.fxzmusic.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn

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
