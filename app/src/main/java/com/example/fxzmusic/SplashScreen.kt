package com.example.fxzmusic

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NeonWhite = Color(0xFFFFFFFF)
private val NeonGlow  = Color(0xFFD0DCFF)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val fullText = "FXZ Music"

    val letterAlphas  = remember { List(fullText.length) { Animatable(0f) } }
    val letterOffsets = remember { List(fullText.length) { Animatable(14f) } }

    var activeCount   by remember { mutableIntStateOf(0) }
    var showCursor    by remember { mutableStateOf(true) }
    var showUnderline by remember { mutableStateOf(false) }
    var exitStarted   by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "s")

    val cursorAlpha by infinite.animateFloat(
        initialValue  = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "c"
    )

    val glowPulse by infinite.animateFloat(
        initialValue  = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "g"
    )

    val scanFrac by infinite.animateFloat(
        initialValue  = -0.02f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "sc"
    )

    val underlineAlpha by animateFloatAsState(
        targetValue   = if (showUnderline) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "u"
    )

    val screenAlpha by animateFloatAsState(
        targetValue      = if (exitStarted) 0f else 1f,
        animationSpec    = tween(700, easing = FastOutSlowInEasing),
        finishedListener = { if (exitStarted) onFinished() },
        label            = "ea"
    )

    val screenScale by animateFloatAsState(
        targetValue   = if (exitStarted) 1.04f else 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "es"
    )

    LaunchedEffect(Unit) {
        delay(300)

        fullText.forEachIndexed { idx, ch ->
            activeCount = idx + 1

            if (ch != ' ') {
                launch {
                    letterAlphas[idx].animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                }
                launch {
                    letterOffsets[idx].animateTo(0f, tween(420, easing = FastOutSlowInEasing))
                }
            } else {
                letterAlphas[idx].snapTo(1f)
                letterOffsets[idx].snapTo(0f)
            }

            delay(115)
        }

        showCursor = false
        delay(120)
        showUnderline = true
        delay(1800)
        exitStarted = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .scale(screenScale)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawScanLine(scanFrac)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                fullText.forEachIndexed { idx, ch ->
                    if (idx >= activeCount) return@forEachIndexed

                    if (ch == ' ') {
                        Spacer(modifier = Modifier.width(20.dp))
                        return@forEachIndexed
                    }

                    val alpha  = letterAlphas[idx].value
                    val offset = letterOffsets[idx].value
                    val pulse  = glowPulse * alpha

                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text     = ch.toString(),
                            modifier = Modifier
                                .alpha((pulse * 0.15f).coerceAtMost(1f))
                                .offset(y = offset.dp),
                            style    = letterStyle(NeonGlow, 65.sp)
                        )
                        Text(
                            text     = ch.toString(),
                            modifier = Modifier
                                .alpha(alpha)
                                .offset(y = offset.dp),
                            style    = letterStyle(NeonWhite, 60.sp).copy(
                                shadow = Shadow(
                                    color      = NeonGlow.copy(alpha = 0.5f * pulse),
                                    offset     = Offset.Zero,
                                    blurRadius = 20f
                                )
                            )
                        )
                    }
                }

                if (showCursor) {
                    Box(
                        modifier = Modifier
                            .alpha(cursorAlpha)
                            .padding(start = 3.dp, bottom = 9.dp)
                            .width(2.dp)
                            .height(44.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(NeonWhite, NeonWhite.copy(alpha = 0.1f))
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .alpha(underlineAlpha * (0.4f + glowPulse * 0.6f))
                    .height(3.dp)
                    .width(290.dp)
            ) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            NeonWhite.copy(alpha = 0.25f),
                            NeonWhite.copy(alpha = 0.85f),
                            NeonWhite.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            NeonGlow.copy(alpha = 0.06f),
                            NeonGlow.copy(alpha = 0.18f),
                            NeonGlow.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    start       = Offset(0f, 6f),
                    end         = Offset(size.width, 6f),
                    strokeWidth = 2f
                )
            }
        }
    }
}

private fun letterStyle(color: Color, size: androidx.compose.ui.unit.TextUnit) = TextStyle(
    color         = color,
    fontSize      = size,
    fontWeight    = FontWeight.Black,
    fontFamily    = FontFamily.Monospace,
    letterSpacing = 5.sp
)

private fun DrawScope.drawScanLine(frac: Float) {
    val y = size.height * frac
    drawLine(
        color       = Color.White.copy(alpha = 0.025f),
        start       = Offset(0f, y),
        end         = Offset(size.width, y),
        strokeWidth = 2f
    )
    for (i in 1..3) {
        drawLine(
            color       = Color.White.copy(alpha = 0.01f / i),
            start       = Offset(0f, y - i * 5f),
            end         = Offset(size.width, y - i * 5f),
            strokeWidth = 1.5f
        )
    }
}
