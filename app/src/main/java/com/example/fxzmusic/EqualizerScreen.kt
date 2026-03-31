package com.example.fxzmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EqualizerScreen(
    equalizerViewModel: EqualizerViewModel
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Ecualizador", color = LocalFxzTheme.current.accent, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Text("10 bandas paramétricas", color = Color.Gray, fontSize = 14.sp)
                }
                Switch(
                    checked = equalizerViewModel.isEnabled,
                    onCheckedChange = { equalizerViewModel.toggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = LocalFxzTheme.current.accent,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF2A2A2A)
                    )
                )
            }
        }

        item {
            Text("Perfiles", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(equalizerViewModel.allProfiles) { profile ->
                    ProfileChip(
                        profile   = profile,
                        isSelected = equalizerViewModel.currentProfile.id == profile.id,
                        onSelect  = { equalizerViewModel.selectProfile(profile) },
                        onDelete  = if (profile.isCustom) {{ equalizerViewModel.deleteCustomProfile(profile) }} else null
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        equalizerViewModel.currentProfile.name,
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    equalizerViewModel.currentProfile.bands.forEach { band ->
                        EqBandRow(
                            band     = band,
                            enabled  = equalizerViewModel.isEnabled,
                            onChange = { gain -> equalizerViewModel.updateBand(band.index, gain) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { equalizerViewModel.selectProfile(PRESET_PROFILES.first()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalFxzTheme.current.surfaceVariant)
                ) {
                    Text("Restablecer", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalFxzTheme.current.accent)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar perfil", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            EqVisualizerCard(bands = equalizerViewModel.currentProfile.bands)
        }
    }

    if (showSaveDialog) {
        SaveProfileDialog(
            onDismiss = { showSaveDialog = false },
            onSave    = { name ->
                equalizerViewModel.saveCustomProfile(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
fun ProfileChip(
    profile: EqProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .scale(if (isPressed) 0.93f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) LocalFxzTheme.current.accent else LocalFxzTheme.current.surface)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            profile.name,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = if (isSelected) Color.Black.copy(alpha = 0.6f) else Color.Gray,
                modifier = Modifier.size(14.dp).clickable { onDelete() }
            )
        }
    }
}

@Composable
fun EqBandRow(band: EqBand, enabled: Boolean, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            band.label,
            color = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(12.dp))
        Slider(
            value = band.gainDb,
            onValueChange = { if (enabled) onChange(it) },
            valueRange = -12f..12f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor        = if (enabled) LocalFxzTheme.current.accent else Color.Gray,
                activeTrackColor  = if (enabled) LocalFxzTheme.current.accent else Color.Gray,
                inactiveTrackColor = Color(0xFF2A2A2A)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "${if (band.gainDb >= 0) "+" else ""}${band.gainDb.toInt()}dB",
            color = when {
                !enabled          -> Color.Gray.copy(alpha = 0.4f)
                band.gainDb > 0f  -> LocalFxzTheme.current.accent
                band.gainDb < 0f  -> Color(0xFFFF5252)
                else              -> Color.Gray
            },
            fontSize = 11.sp,
            modifier = Modifier.width(44.dp)
        )
    }
}

@Composable
fun EqVisualizerCard(bands: List<EqBand>) {
    val barMaxHeight = 80.dp
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LocalFxzTheme.current.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bands.forEach { band ->
                val normalizedHeight by animateFloatAsState(
                    targetValue = ((band.gainDb + 12f) / 24f).coerceIn(0.04f, 1f),
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label = "bar_${band.index}"
                )
                val barColor = when {
                    band.gainDb > 6f  -> LocalFxzTheme.current.accent
                    band.gainDb > 0f  -> LocalFxzTheme.current.accent.copy(alpha = 0.7f)
                    band.gainDb < -6f -> Color(0xFFFF5252)
                    band.gainDb < 0f  -> Color(0xFFFF5252).copy(alpha = 0.7f)
                    else              -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(barMaxHeight * normalizedHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
fun SaveProfileDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar perfil", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del perfil", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = LocalFxzTheme.current.accent, unfocusedBorderColor = Color.DarkGray
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LocalFxzTheme.current.accent, disabledContainerColor = Color.DarkGray)
            ) { Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } },
        containerColor = LocalFxzTheme.current.surface,
        shape = RoundedCornerShape(28.dp)
    )
}