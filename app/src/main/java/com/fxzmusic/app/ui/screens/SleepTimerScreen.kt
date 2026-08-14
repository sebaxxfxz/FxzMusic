package com.fxzmusic.app.ui.screens

import com.fxzmusic.app.ui.components.GlassCard
import com.fxzmusic.app.ui.components.scaleOnPress
import com.fxzmusic.app.ui.components.PressScale
import com.fxzmusic.app.viewmodel.PlaybackSettingsViewModel
import com.fxzmusic.app.viewmodel.SLEEP_TIMER_OPTIONS
import com.fxzmusic.app.viewmodel.SLEEP_TIMER_LABELS

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun AnimatedCountdownCircle(
    remainingMs: Long,
    totalMs: Long,
    accent: Color
) {
    val fraction = if (totalMs > 0L) (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "countdown_arc"
    )

    val glowTransition = rememberInfiniteTransition(label = "countdown_glow")
    val glowRadius by glowTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_radius"
    )

    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val r = (size.minDimension / 2f) - 4.dp.toPx()
            val sw = 4.dp.toPx()
            val sweep = 360f * animatedFraction

            drawCircle(accent.copy(alpha = 0.08f), center = c, radius = r + 8.dp.toPx() * glowRadius, style = Stroke(width = sw * 1.5f, cap = StrokeCap.Round))
            drawCircle(accent.copy(alpha = 0.12f), center = c, radius = r, style = Stroke(width = sw, cap = StrokeCap.Round))
            drawArc(accent, -90f, sweep, useCenter = false, style = Stroke(width = sw, cap = StrokeCap.Round))
            if (animatedFraction > 0.05f) {
                val endAngle = -90f + sweep
                val endRad = endAngle * (Math.PI / 180.0)
                val dotX = c.x + r * kotlin.math.cos(endRad).toFloat()
                val dotY = c.y + r * kotlin.math.sin(endRad).toFloat()
                drawCircle(accent, radius = sw * 1.5f, center = Offset(dotX, dotY))
                drawCircle(accent.copy(alpha = 0.3f), radius = sw * 3f, center = Offset(dotX, dotY))
            }
        }
    }
}

@Composable
fun SleepTimerScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onBack: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var fadeSliderValue by remember(settingsViewModel.fadeDurationSeconds) {
        mutableFloatStateOf(settingsViewModel.fadeDurationSeconds.toFloat())
    }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    val hasCustom = settingsViewModel.customSleepTimerMinutes > 0

    val pulseTransition = rememberInfiniteTransition(label = "sleep_ambient")
    val ambientGlow by pulseTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ambient_glow"
    )

    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsSubScreenHeader(title = "Sleep Timer", icon = Icons.Filled.Bedtime, accent = accent, onBack = onBack)
            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = settingsViewModel.isSleepTimerActive,
                enter = fadeIn(tween(400)) + expandVertically(expandFrom = Alignment.Top, animationSpec = tween(350)),
                exit  = fadeOut(tween(250)) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(250))
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accent.copy(alpha = ambientGlow * 0.12f), accent.copy(alpha = 0.02f)),
                            start = Offset.Zero, end = Offset(600f, 600f)
                        )
                    )
                    .border(0.5.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnimatedCountdownCircle(
                            remainingMs = settingsViewModel.sleepTimerRemainingMs,
                            totalMs = settingsViewModel.sleepTimerMinutes * 60_000L,
                            accent = accent
                        )
                        Column {
                            Text("Pausando reproducción", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            AnimatedContent(
                                targetState = settingsViewModel.formatSleepRemaining(),
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                label = "remaining"
                            ) { time ->
                                Text(time, color = accent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    IconButton(onClick = { settingsViewModel.cancelSleepTimer() }) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Close, null, tint = accent, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "PREAJUSTES RÁPIDOS",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )

                val rows = SLEEP_TIMER_OPTIONS.zip(SLEEP_TIMER_LABELS).chunked(3)
                rows.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { (mins, label) ->
                            val isSelected = settingsViewModel.sleepTimerMinutes == mins &&
                                    (mins == 0 || settingsViewModel.isSleepTimerActive)
                            val chipInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scaleOnPress(chipInteraction, PressScale.chip)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) accent.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable(interactionSource = chipInteraction, indication = null) {
                                        if (mins == 0) settingsViewModel.cancelSleepTimer()
                                        else settingsViewModel.startSleepTimer(mins) {}
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (mins == 0) Icons.Filled.Close else Icons.Filled.Bedtime,
                                        null,
                                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        label,
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "OPCIONES AVANZADAS DE APAGADO",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MusicNote, null, tint = accent, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Detener al finalizar canción", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Espera a que termine el tema actual", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            Switch(
                                checked = settingsViewModel.sleepAfterTrack,
                                onCheckedChange = { settingsViewModel.toggleSleepAfterTrack() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = accent
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.GraphicEq, null, tint = accent, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("Duración de atenuación", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Fade-out progresivo al finalizar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                                Text("${fadeSliderValue.toInt()}s", color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Slider(
                                value = fadeSliderValue,
                                onValueChange = { fadeSliderValue = it },
                                onValueChangeFinished = { settingsViewModel.updateFadeDuration(fadeSliderValue.toInt()) },
                                valueRange = 0f..60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                val isCustomSelected = settingsViewModel.isSleepTimerActive &&
                        settingsViewModel.customSleepTimerMinutes > 0 &&
                        settingsViewModel.sleepTimerMinutes == settingsViewModel.customSleepTimerMinutes
                val customInteraction = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scaleOnPress(customInteraction, PressScale.list)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCustomSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
                        .border(0.5.dp, if (isCustomSelected) accent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .clickable(interactionSource = customInteraction, indication = null) { showCustomDialog = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(
                            if (isCustomSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.EditNote, null, tint = if (isCustomSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                    Column {
                        Text(
                            if (hasCustom) "${settingsViewModel.customSleepTimerMinutes} min" else "Personalizado",
                            color = if (isCustomSelected) accent else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCustomSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        if (isCustomSelected) Text("Activo", color = accent, fontSize = 11.sp)
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.AutoMirrored.Filled.VolumeDown, null, tint = accent, modifier = Modifier.size(18.dp)) }
                        Text("Fade al pausar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        if (fadeSliderValue.toInt() == 0) "Sin fade"
                        else "${fadeSliderValue.toInt()}s",
                        color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                    )
                }
                Slider(
                    value = fadeSliderValue,
                    onValueChange = { fadeSliderValue = it },
                    onValueChangeFinished = { settingsViewModel.updateFadeDuration(fadeSliderValue.toInt()) },
                    valueRange = 0f..60f, steps = 11,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sin fade", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("60s", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.SkipNext, null, tint = accent, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Finalizar tema", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Apaga al terminar la canción actual", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settingsViewModel.sleepAfterTrack,
                        onCheckedChange = { settingsViewModel.sleepAfterTrack = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
    }

    if (showCustomDialog) {
        var inputText by remember { mutableStateOf(if (hasCustom) settingsViewModel.customSleepTimerMinutes.toString() else "") }
        val isValid = inputText.toIntOrNull()?.let { it in 1..360 } == true
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.EditNote, null, tint = accent, modifier = Modifier.size(22.dp))
                    Text("Tiempo personalizado", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputText, onValueChange = {
                            val digits = it.filter { c -> c.isDigit() }.take(3)
                            inputText = if ((digits.toIntOrNull() ?: 0) > 360) "360" else digits
                        },
                        label = { Text("Minutos (1-360)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    if (inputText.isNotEmpty() && !isValid) {
                        Text("Valor entre 1 y 360", color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SLEEP_TIMER_OPTIONS.filter { it in 5..30 }.forEach { quick ->
                            val quickInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.copy(alpha = 0.1f))
                                    .clickable(interactionSource = quickInteraction, indication = null) {
                                        inputText = quick.toString()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${quick}m", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.setCustomSleepTimer(inputText.toIntOrNull() ?: return@Button) {}
                        showCustomDialog = false
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Iniciar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}
}
