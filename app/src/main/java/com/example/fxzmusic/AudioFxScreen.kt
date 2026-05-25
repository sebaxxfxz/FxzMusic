package com.example.fxzmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioFxScreen(fxViewModel: AudioFxViewModel) {
    val context = LocalContext.current
    val accent  = LocalFxzTheme.current.accent

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("SONIDO", color = Cinematic_PlatinumText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Efectos", color = Cinematic_OnSurface, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
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
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Cinematic_GlassBackground)
                    .border(1.dp, Cinematic_GlassBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Concentric expanding circles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = 40.dp.toPx()
                    val maxRadius = size.width.coerceAtLeast(size.height) / 1.6f

                    drawCircle(
                        color = accent.copy(alpha = (1f - pingProgress1) * 0.25f),
                        radius = baseRadius + (maxRadius - baseRadius) * pingProgress1,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = accent.copy(alpha = (1f - pingProgress2) * 0.15f),
                        radius = baseRadius + (maxRadius - baseRadius) * pingProgress2,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.25f), CircleShape),
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
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.SurroundSound, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Sonido Envolvente", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Virtualizer 3D", color = Cinematic_PlatinumText, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = fxViewModel.virtualizerEnabled,
                            onCheckedChange = { fxViewModel.toggleVirtualizer(context) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent, uncheckedThumbColor = Cinematic_MutedText, uncheckedTrackColor = LocalFxzTheme.current.surfaceVariant)
                        )
                    }
                    if (fxViewModel.virtualizerEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Intensidad", color = Cinematic_PlatinumText, fontSize = 12.sp)
                            Text("${(fxViewModel.virtualizerStrength / 10)}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fxViewModel.virtualizerStrength.toFloat(),
                            onValueChange = { fxViewModel.updateVirtualizerStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = LocalFxzTheme.current.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Potenciador de Graves", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Bass Boost", color = Cinematic_PlatinumText, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = fxViewModel.bassBoostEnabled,
                            onCheckedChange = { fxViewModel.toggleBassBoost(context) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent, uncheckedThumbColor = Cinematic_MutedText, uncheckedTrackColor = LocalFxzTheme.current.surfaceVariant)
                        )
                    }
                    if (fxViewModel.bassBoostEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Intensidad", color = Cinematic_PlatinumText, fontSize = 12.sp)
                            Text("${(fxViewModel.bassBoostStrength / 10)}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fxViewModel.bassBoostStrength.toFloat(),
                            onValueChange = { fxViewModel.updateBassBoostStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = LocalFxzTheme.current.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Consejo", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Usa 'Concierto' con audífonos para la mejor experiencia de sonido envolvente.", color = Cinematic_PlatinumText, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ambiente", color = Cinematic_OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fxViewModel.presets.forEach { preset ->
                            val isSelected = preset == fxViewModel.selectedPreset
                            val chipInteraction = remember { MutableInteractionSource() }
                            val isChipPressed by chipInteraction.collectIsPressedAsState()
                            val chipScale by animateFloatAsState(targetValue = if (isChipPressed) 0.94f else 1f, label = "chip_$preset")
                            
                            val labelUpper = when (preset) {
                                "Concierto" -> "CONCIERTO"
                                "Estadio"   -> "ESTADIO"
                                "Club"      -> "CLUB"
                                "Sala"      -> "SALA"
                                else        -> preset.uppercase()
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(chipScale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) accent else Cinematic_GlassBackground)
                                    .border(1.dp, if (isSelected) accent else Cinematic_GlassBorder, RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = chipInteraction,
                                        indication = null
                                    ) { fxViewModel.selectPreset(context, preset) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = labelUpper,
                                    color = if (isSelected) Color.Black else Cinematic_OnSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
