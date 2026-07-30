package com.fxzmusic.app.ui.screens

import com.fxzmusic.app.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var exitStarted by remember { mutableStateOf(false) }

    val logoScale = remember { Animatable(0.75f) }
    val logoAlpha = remember { Animatable(0f) }
    val ambientGlowScale = remember { Animatable(0.6f) }
    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val letterSpacingAnim = remember { Animatable(10f) }

    val screenAlpha by animateFloatAsState(
        targetValue = if (exitStarted) 0f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        finishedListener = { if (exitStarted) onFinished() },
        label = "splash_exit_alpha"
    )

    val screenScale by animateFloatAsState(
        targetValue = if (exitStarted) 1.06f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "splash_exit_scale"
    )

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                logoScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                logoAlpha.animateTo(1.0f, tween(300))
            }
            launch {
                ambientGlowScale.animateTo(1.15f, tween(800, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                textAlpha.animateTo(1.0f, tween(300))
            }
            launch {
                delay(200)
                letterSpacingAnim.animateTo(4f, tween(450, easing = FastOutSlowInEasing))
            }
            launch {
                delay(350)
                subtitleAlpha.animateTo(1.0f, tween(300))
            }
        }

        delay(1400)
        exitStarted = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .scale(screenScale)
            .background(Color(0xFF09090D)),
        contentAlignment = Alignment.Center
    ) {
        
        Box(
            modifier = Modifier
                .size(380.dp)
                .scale(ambientGlowScale.value)
                .alpha(logoAlpha.value * 0.7f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE5DFD3).copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .shadow(elevation = 20.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color(0xFF121218)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "FxzMusic Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "FXZ MUSIC",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = letterSpacingAnim.value.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "HIGH FIDELITY AUDIO",
                color = Color(0xFFE5DFD3).copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
