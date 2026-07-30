package com.fxzmusic.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.data.EqBand
import com.fxzmusic.app.ui.components.GlassCard
import com.fxzmusic.app.ui.components.PressScale
import com.fxzmusic.app.ui.components.scaleOnPress
import com.fxzmusic.app.viewmodel.EqualizerViewModel
import kotlin.math.roundToInt

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    equalizerViewModel: EqualizerViewModel,
    onBack: () -> Unit = {}
) {
    val profile = equalizerViewModel.currentProfile
    val isEnabled = equalizerViewModel.isEnabled
    val accent = MaterialTheme.colorScheme.primary

    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 56.dp, bottom = 120.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                Text("Ecualizador", color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { equalizerViewModel.toggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1f else 0.4f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            InteractiveFrequencyGraph(
                bands = profile.bands,
                accent = accent,
                onBandChange = { index, gainDb ->
                    equalizerViewModel.updateBand(index, gainDb)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "PREAJUSTES FÁCILES",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    equalizerViewModel.allProfiles.forEach { p ->
                        val isSelected = profile.id == p.id
                        val interaction = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .scaleOnPress(interaction, PressScale.chip)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) accent else Color.White.copy(alpha = 0.08f))
                                .clickable(interactionSource = interaction, indication = null) {
                                    equalizerViewModel.selectProfile(p)
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                p.name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "CONTROLES DESLIZANTES",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        profile.bands.forEach { band ->
                            val friendlyName = when {
                                band.label.contains("60") -> "Graves (60Hz)"
                                band.label.contains("230") -> "Medios Bajos (230Hz)"
                                band.label.contains("910") -> "Medios (910Hz)"
                                band.label.contains("3.6") -> "Medios Altos (3.6kHz)"
                                band.label.contains("14") -> "Agudos (14kHz)"
                                else -> band.label
                            }

                            EasyBandSlider(
                                label = friendlyName,
                                gainDb = band.gainDb,
                                accent = accent,
                                onValueChange = { gainDb ->
                                    equalizerViewModel.updateBand(band.index, gainDb)
                                }
                            )
                        }
                    }
                }
            }

            if (profile.isCustom) {
                var showSaveDialog by remember { mutableStateOf(false) }
                var profileName by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showSaveDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar mi ajuste", color = accent, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .clickable { equalizerViewModel.deleteCustomProfile(profile) }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Restablecer / Eliminar este ajuste", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }

                if (showSaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Guardar Ecualizador", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = profileName,
                                onValueChange = { profileName = it },
                                placeholder = { Text("Ej. Mi ajuste favorito") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                equalizerViewModel.saveCustomProfile(profileName)
                                showSaveDialog = false
                                profileName = ""
                            }) {
                                Text("Guardar", color = accent, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSaveDialog = false }) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveFrequencyGraph(
    bands: List<EqBand>,
    accent: Color,
    onBandChange: (index: Int, gainDb: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var cardSize by remember { mutableStateOf(Size.Zero) }

    fun updateGainFromTouch(touchX: Float, touchY: Float) {
        if (bands.isEmpty() || cardSize.width <= 0f || cardSize.height <= 0f) return
        val width = cardSize.width
        val height = cardSize.height
        val midY = height / 2f

        val bandStep = width / (bands.size - 1).coerceAtLeast(1)
        val closestIndex = (touchX / bandStep).roundToInt().coerceIn(0, bands.size - 1)

        val rawGain = ((midY - touchY) / (midY * 0.85f)) * 12f
        val gainDb = (rawGain.coerceIn(-12f, 12f) * 2).roundToInt() / 2f

        onBandChange(closestIndex, gainDb)
    }

    GlassCard(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        cardSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
                    }
                    .pointerInput(bands) {
                        detectTapGestures { offset ->
                            updateGainFromTouch(offset.x, offset.y)
                        }
                    }
                    .pointerInput(bands) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            updateGainFromTouch(change.position.x, change.position.y)
                        }
                    }
            ) {
                if (bands.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val midY = height / 2f

                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.dp.toPx()
                )

                val points = bands.mapIndexed { index, band ->
                    val x = (index.toFloat() / (bands.size - 1).coerceAtLeast(1)) * width
                    val normalizedGain = (band.gainDb.coerceIn(-12f, 12f) / 12f)
                    val y = midY - (normalizedGain * (midY * 0.85f))
                    Offset(x, y)
                }

                val path = Path()
                val fillPath = Path()

                if (points.isNotEmpty()) {
                    path.moveTo(points.first().x, points.first().y)
                    fillPath.moveTo(points.first().x, height)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                        val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                        path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                        fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                    }

                    fillPath.lineTo(points.last().x, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.01f))
                        )
                    )

                    drawPath(
                        path = path,
                        color = accent,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    points.forEachIndexed { idx, pt ->
                        val band = bands[idx]
                        val isNonZero = band.gainDb != 0f

                        drawCircle(
                            color = if (isNonZero) accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f),
                            radius = 12.dp.toPx(),
                            center = pt
                        )

                        drawCircle(
                            color = if (isNonZero) accent else Color.White,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EasyBandSlider(
    label: String,
    gainDb: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = String.format("%+.1f dB", gainDb),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (gainDb != 0f) accent else Color.White.copy(alpha = 0.5f)
            )
        }

        Slider(
            value = gainDb,
            onValueChange = onValueChange,
            valueRange = -12f..12f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}
