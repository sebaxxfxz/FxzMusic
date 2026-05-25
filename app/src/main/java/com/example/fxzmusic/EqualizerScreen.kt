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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
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
                    Text("SONIDO", color = Cinematic_PlatinumText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Ecualizador", color = Cinematic_OnSurface, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                Switch(
                    checked = equalizerViewModel.isEnabled,
                    onCheckedChange = { equalizerViewModel.toggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = LocalFxzTheme.current.accent,
                        uncheckedThumbColor = Cinematic_MutedText,
                        uncheckedTrackColor = LocalFxzTheme.current.surfaceVariant
                    )
                )
            }
        }

        item {
            Text("Perfiles", color = Cinematic_OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        equalizerViewModel.currentProfile.name,
                        color = Cinematic_OnSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
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
                    Text("Restablecer", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold)
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

    val isGlass = LocalFxzTheme.current.mode == ThemeMode.GLASSMORPHISM
    val chipBg = if (isSelected) LocalFxzTheme.current.accent else if (isGlass) Cinematic_GlassBackground else LocalFxzTheme.current.surface
    val chipBorderColor = if (isSelected) LocalFxzTheme.current.accent else if (isGlass) Cinematic_GlassBorder else Color.Transparent

    Row(
        modifier = Modifier
            .scale(if (isPressed) 0.93f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(chipBg)
            .border(1.dp, chipBorderColor, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            profile.name,
            color = if (isSelected) Color.Black else Cinematic_OnSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = if (isSelected) Color.Black.copy(alpha = 0.6f) else Cinematic_MutedText,
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
            color = if (enabled) Cinematic_PlatinumText else Cinematic_MutedText.copy(alpha = 0.4f),
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
                thumbColor        = if (enabled) LocalFxzTheme.current.accent else Cinematic_MutedText,
                activeTrackColor  = if (enabled) LocalFxzTheme.current.accent else Cinematic_MutedText,
                inactiveTrackColor = LocalFxzTheme.current.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "${if (band.gainDb >= 0) "+" else ""}${band.gainDb.toInt()}dB",
            color = when {
                !enabled          -> Cinematic_MutedText.copy(alpha = 0.4f)
                band.gainDb > 0f  -> LocalFxzTheme.current.accent
                band.gainDb < 0f  -> Color(0xFFFF5252)
                else              -> Cinematic_PlatinumText
            },
            fontSize = 11.sp,
            modifier = Modifier.width(44.dp)
        )
    }
}

@Composable
fun EqVisualizerCard(bands: List<EqBand>) {
    val accent = LocalFxzTheme.current.accent
    val animatedGains = bands.map { band ->
        animateFloatAsState(
            targetValue = band.gainDb,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
            label = "band_gain_${band.index}"
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Curva Lineal de Fase",
                color = Cinematic_MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val points = animatedGains.mapIndexed { index, animatedGain ->
                        val x = if (bands.size > 1) (index.toFloat() / (bands.size - 1)) * width else 0f
                        val gain = animatedGain.value
                        val y = height / 2f - (gain / 12f) * (height / 2f - 10f)
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    // 1. Center Reference Line (Dashed)
                    val dashedStroke = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    drawLine(
                        color = Cinematic_MutedText.copy(alpha = 0.25f),
                        start = androidx.compose.ui.geometry.Offset(0f, height / 2f),
                        end = androidx.compose.ui.geometry.Offset(width, height / 2f),
                        strokeWidth = 1f,
                        pathEffect = dashedStroke.pathEffect
                    )

                    // 2. Smooth Spline Path
                    val path = Path()
                    if (points.isNotEmpty()) {
                        path.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val mX = (p1.x + p2.x) / 2f
                            val mY = (p1.y + p2.y) / 2f
                            path.quadraticTo(p1.x, p1.y, mX, mY)
                        }
                        path.lineTo(points.last().x, points.last().y)
                    }

                    // 3. Draw Gradient Fill Under the Curve
                    if (points.isNotEmpty()) {
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(accent.copy(alpha = 0.15f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )
                    }

                    // 4. Draw Glowing Spline Path
                    // Outer glow
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.05f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 16f,
                            cap = StrokeCap.Round
                        )
                    )
                    // Inner glow
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 8f,
                            cap = StrokeCap.Round
                        )
                    )
                    // Core line
                    drawPath(
                        path = path,
                        color = accent,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SaveProfileDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar perfil", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del perfil", color = Cinematic_MutedText) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Cinematic_OnSurface, unfocusedTextColor = Cinematic_OnSurface,
                    focusedBorderColor = LocalFxzTheme.current.accent,
                    unfocusedBorderColor = if (LocalFxzTheme.current.mode == ThemeMode.GLASSMORPHISM) Cinematic_GlassBorder else LocalFxzTheme.current.surfaceVariant
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LocalFxzTheme.current.accent, disabledContainerColor = LocalFxzTheme.current.surfaceVariant)
            ) { Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Cinematic_MutedText) } },
        containerColor = if (LocalFxzTheme.current.mode == ThemeMode.GLASSMORPHISM) Cinematic_SurfaceContainerHigh else LocalFxzTheme.current.surface,
        shape = RoundedCornerShape(20.dp)
    )
}