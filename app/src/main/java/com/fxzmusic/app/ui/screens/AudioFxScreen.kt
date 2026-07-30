package com.fxzmusic.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.ui.components.GlassCard
import com.fxzmusic.app.viewmodel.AudioFxViewModel

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun AudioFxScreen(
    fxViewModel: AudioFxViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    BackHandler(enabled = true) {
        onBack()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("AUDIO DSP", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Efectos de Sonido", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        item {
            val infiniteTransition = rememberInfiniteTransition(label = "audio_fx_ping")
            val pingProgress1 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ping1"
            )
            val pingProgress2 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ping2"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = 36.dp.toPx()
                    val maxRadius = size.width.coerceAtLeast(size.height) / 1.6f

                    drawCircle(
                        color = accent.copy(alpha = (1f - pingProgress1) * 0.25f),
                        radius = baseRadius + (maxRadius - baseRadius) * pingProgress1,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = accent.copy(alpha = (1f - pingProgress2) * 0.15f),
                        radius = baseRadius + (maxRadius - baseRadius) * pingProgress2,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Surround 3D", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Switch(
                                checked = fxViewModel.virtualizerEnabled,
                                onCheckedChange = { fxViewModel.toggleVirtualizer(context) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        AudioKnobDial(
                            percentage = if (fxViewModel.virtualizerEnabled) (fxViewModel.virtualizerStrength / 10f) else 0f,
                            accent = accent,
                            enabled = fxViewModel.virtualizerEnabled
                        )

                        if (fxViewModel.virtualizerEnabled) {
                            Slider(
                                value = fxViewModel.virtualizerStrength.toFloat(),
                                onValueChange = { fxViewModel.updateVirtualizerStrength(it.toInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bass Boost", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Switch(
                                checked = fxViewModel.bassBoostEnabled,
                                onCheckedChange = { fxViewModel.toggleBassBoost(context) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        AudioKnobDial(
                            percentage = if (fxViewModel.bassBoostEnabled) (fxViewModel.bassBoostStrength / 10f) else 0f,
                            accent = accent,
                            enabled = fxViewModel.bassBoostEnabled
                        )

                        if (fxViewModel.bassBoostEnabled) {
                            Slider(
                                value = fxViewModel.bassBoostStrength.toFloat(),
                                onValueChange = { fxViewModel.updateBassBoostStrength(it.toInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("AMBIENTE ACÚSTICO", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)

                    val envList = listOf(
                        Triple("Concierto", "🎤", "En vivo y espacial"),
                        Triple("Estadio", "🏟️", "Reverberación amplia"),
                        Triple("Club", "🪩", "Resonancia y graves"),
                        Triple("Sala", "🛋️", "Cálido e íntimo")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        envList.chunked(2).forEach { rowEnvs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowEnvs.forEach { (preset, icon, desc) ->
                                    val isSelected = preset == fxViewModel.selectedPreset
                                    val chipInteraction = remember { MutableInteractionSource() }
                                    val isChipPressed by chipInteraction.collectIsPressedAsState()
                                    val chipScale by animateFloatAsState(
                                        targetValue = if (isChipPressed) 0.94f else 1f,
                                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                                        label = "env_$preset"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .scale(chipScale)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSelected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) accent else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable(
                                                interactionSource = chipInteraction,
                                                indication = null
                                            ) { fxViewModel.selectPreset(context, preset) }
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(icon, fontSize = 22.sp)
                                            Column {
                                                Text(
                                                    text = preset,
                                                    color = if (isSelected) accent else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = desc,
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 11.sp
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
    }
}

@Composable
private fun AudioKnobDial(
    percentage: Float,
    accent: Color,
    enabled: Boolean
) {
    Box(
        modifier = Modifier.size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (enabled && percentage > 0f) {
                val sweep = (percentage / 100f) * 240f
                drawArc(
                    color = accent,
                    startAngle = 150f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%",
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
