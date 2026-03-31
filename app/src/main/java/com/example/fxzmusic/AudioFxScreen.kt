package com.example.fxzmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
            Text("Sonido", color = accent, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Efectos de audio en tiempo real", color = Color.Gray, fontSize = 14.sp)
        }

        item {
            Text("Presets de ambiente", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fxViewModel.presets) { preset ->
                    val isSelected = preset == fxViewModel.selectedPreset
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.93f else 1f, animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "preset_scale")

                    val emoji = when (preset) {
                        "Concierto" -> "🎸"
                        "Estadio"   -> "🏟️"
                        "Club"      -> "🎛️"
                        "Sala"      -> "🎹"
                        else        -> "🎵"
                    }

                    Column(
                        modifier = Modifier.scale(scale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) accent else LocalFxzTheme.current.surface)
                            .border(if (isSelected) 0.dp else 1.dp, LocalFxzTheme.current.surfaceVariant, RoundedCornerShape(20.dp))
                            .clickable(interactionSource = interaction, indication = null) { fxViewModel.selectPreset(context, preset) }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(emoji, fontSize = 22.sp)
                        Text(preset, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surface)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.SurroundSound, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Sonido Envolvente", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Virtualizer 3D", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = fxViewModel.virtualizerEnabled,
                            onCheckedChange = { fxViewModel.toggleVirtualizer(context) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF2A2A2A))
                        )
                    }
                    if (fxViewModel.virtualizerEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Intensidad", color = Color.Gray, fontSize = 12.sp)
                            Text("${(fxViewModel.virtualizerStrength / 10)}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fxViewModel.virtualizerStrength.toFloat(),
                            onValueChange = { fxViewModel.updateVirtualizerStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF2A2A2A)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surface)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Potenciador de Graves", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Bass Boost", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = fxViewModel.bassBoostEnabled,
                            onCheckedChange = { fxViewModel.toggleBassBoost(context) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF2A2A2A))
                        )
                    }
                    if (fxViewModel.bassBoostEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Intensidad", color = Color.Gray, fontSize = 12.sp)
                            Text("${(fxViewModel.bassBoostStrength / 10)}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fxViewModel.bassBoostStrength.toFloat(),
                            onValueChange = { fxViewModel.updateBassBoostStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF2A2A2A)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Consejo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Usa 'Concierto' con audífonos para la mejor experiencia de sonido envolvente.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
