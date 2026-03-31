package com.example.fxzmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.sin
import kotlin.math.PI

private sealed class SettingsScreen {
    object Main   : SettingsScreen()
    object Speed  : SettingsScreen()
    object Sleep  : SettingsScreen()
    object Theme  : SettingsScreen()
    object Sound  : SettingsScreen()
}

@Composable
private fun AnimatedCountdownCircle(
    remainingMs: Long,
    totalMs: Long,
    accent: Color
) {
    val fraction = if (totalMs > 0L) (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(1000),
        label = "countdown_arc"
    )
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            drawArc(
                color = accent.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Icon(Icons.Filled.Timer, null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun PlaybackSettingsScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onPausePlayback: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenAudioFx: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<SettingsScreen>(SettingsScreen.Main) }

    AnimatedContent(
        targetState   = currentScreen,
        transitionSpec = {
            if (targetState == SettingsScreen.Main) {
                (slideInVertically(
                    initialOffsetY = { -it / 6 },
                    animationSpec  = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(350))) togetherWith
                        (slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(200)))
            } else {
                (slideInVertically(
                    initialOffsetY = { it },
                    animationSpec  = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness    = Spring.StiffnessLow
                    )
                ) + fadeIn(tween(400))) togetherWith
                        (slideOutVertically(
                            targetOffsetY = { -it / 6 },
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(200)))
            }
        },
        label = "settings_nav"
    ) { screen ->
        when (screen) {
            SettingsScreen.Main  -> SettingsMainScreen(
                settingsViewModel = settingsViewModel,
                onOpenSpeed       = { currentScreen = SettingsScreen.Speed },
                onOpenSleep       = { currentScreen = SettingsScreen.Sleep },
                onOpenTheme       = { currentScreen = SettingsScreen.Theme },
                onOpenSound       = { currentScreen = SettingsScreen.Sound }
            )
            SettingsScreen.Speed -> SpeedScreen(
                settingsViewModel = settingsViewModel,
                onBack            = { currentScreen = SettingsScreen.Main }
            )
            SettingsScreen.Sleep -> SleepTimerScreen(
                settingsViewModel = settingsViewModel,
                onBack            = { currentScreen = SettingsScreen.Main }
            )
            SettingsScreen.Theme -> ThemeScreen(
                settingsViewModel = settingsViewModel,
                onBack            = { currentScreen = SettingsScreen.Main }
            )
            SettingsScreen.Sound -> SoundScreen(
                settingsViewModel = settingsViewModel,
                onBack          = { currentScreen = SettingsScreen.Main },
                onOpenEqualizer = onOpenEqualizer,
                onOpenAudioFx   = onOpenAudioFx
            )
        }
    }
}

@Composable
private fun SettingsMainScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onOpenSpeed: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenSound: () -> Unit
) {
    val theme  = LocalFxzTheme.current
    val accent = theme.accent

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Ajustes", color = accent, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("Personaliza tu experiencia", color = Color.Gray, fontSize = 13.sp)
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            SettingsRow(
                icon     = Icons.Filled.Speed,
                title    = "Velocidad de reproducción",
                subtitle = PLAYBACK_SPEED_LABELS.getOrElse(
                    PLAYBACK_SPEEDS.indexOfFirst { it == settingsViewModel.playbackSpeed }.coerceAtLeast(0)
                ) { "1x" },
                accent   = accent,
                onClick  = onOpenSpeed
            )
        }

        item {
            SettingsRow(
                icon       = Icons.Filled.Bedtime,
                title      = "Sleep Timer",
                subtitle   = if (settingsViewModel.isSleepTimerActive) "⏱ ${settingsViewModel.formatSleepRemaining()}" else "Desactivado",
                accent     = accent,
                onClick    = onOpenSleep,
                badge      = if (settingsViewModel.isSleepTimerActive) settingsViewModel.formatSleepRemaining() else null,
                badgeColor = accent
            )
        }

        item {
            SettingsRow(
                icon     = Icons.Filled.Equalizer,
                title    = "Configuración general de sonido",
                subtitle = "Ecualizador y efectos de audio",
                accent   = accent,
                onClick  = onOpenSound
            )
        }

        item {
            SettingsRow(
                icon     = Icons.Filled.Palette,
                title    = "Tema y colores",
                subtitle = "Acento: ${ACCENT_NAMES.getOrElse(settingsViewModel.accentColorIndex) { "Verde" }} · ${THEME_NAMES[ThemeMode.entries.indexOf(settingsViewModel.themeMode)]}",
                accent   = accent,
                onClick  = onOpenTheme
            )
        }

        item {
            ShakeToggleRow(
                enabled  = settingsViewModel.shakeToSkip,
                accent   = accent,
                onToggle = { settingsViewModel.toggleShakeToSkip() }
            )
        }

        item {
            AboutAppCard(accent = accent)
        }
    }
}

@Composable
private fun SoundScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAudioFx: () -> Unit
) {
    val theme = LocalFxzTheme.current
    val accent = theme.accent

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SettingsSubScreenHeader(title = "Sonido", icon = Icons.Filled.Equalizer, accent = accent, onBack = onBack) }

        item {
            SettingsRow(
                icon = Icons.Filled.GraphicEq,
                title = "Ecualizador de 10 bandas",
                subtitle = "Ajusta frecuencias y perfil de audio",
                accent = accent,
                onClick = onOpenEqualizer
            )
        }

        item {
            SettingsRow(
                icon = Icons.Filled.SurroundSound,
                title = "Efectos de sonido",
                subtitle = "Bass boost, virtualizer y presets",
                accent = accent,
                onClick = onOpenAudioFx
            )
        }

        item {
            ToggleSettingRow(
                icon = Icons.Filled.Tune,
                title = "Normalizacion de loudness",
                subtitle = if (settingsViewModel.loudnessNormalization) "Activa (volumen mas uniforme)" else "Desactivada",
                checked = settingsViewModel.loudnessNormalization,
                accent = accent,
                onCheckedChange = { settingsViewModel.updateLoudnessNormalization(it) }
            )
        }

        item {
            ToggleSettingRow(
                icon = Icons.Filled.Wifi,
                title = "Portadas solo por Wi-Fi",
                subtitle = if (settingsViewModel.wifiOnlyCovers) "Ahorro de datos moviles" else "Descargas permitidas en red movil",
                checked = settingsViewModel.wifiOnlyCovers,
                accent = accent,
                onCheckedChange = { settingsViewModel.updateWifiOnlyCovers(it) }
            )
        }
    }
}

@Composable
fun SpeedScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onBack: () -> Unit
) {
    val theme  = LocalFxzTheme.current
    val accent = theme.accent
    val label  = PLAYBACK_SPEED_LABELS.getOrElse(
        PLAYBACK_SPEEDS.indexOfFirst { it == settingsViewModel.playbackSpeed }.coerceAtLeast(0)
    ) { "1x" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SettingsSubScreenHeader(title = "Velocidad", icon = Icons.Filled.Speed, accent = accent, onBack = onBack) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = accent, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                    Text("velocidad actual", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    PLAYBACK_SPEEDS.forEachIndexed { i, speed ->
                        val isSelected  = settingsViewModel.playbackSpeed == speed
                        val interaction = remember { MutableInteractionSource() }
                        val isPressed   by interaction.collectIsPressedAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(if (isPressed) 0.97f else 1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable(interactionSource = interaction, indication = null) {
                                    settingsViewModel.updateSpeed(speed)
                                }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) accent.copy(alpha = 0.2f) else Color(0xFF1E1E1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Speed,
                                        null,
                                        tint = if (isSelected) accent else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    PLAYBACK_SPEED_LABELS[i],
                                    color = if (isSelected) accent else Color.White,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (i < PLAYBACK_SPEEDS.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = Color(0xFF1E1E1E),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onBack: () -> Unit
) {
    val theme  = LocalFxzTheme.current
    val accent = theme.accent
    var fadeSliderValue by remember(settingsViewModel.fadeDurationSeconds) {
        mutableFloatStateOf(settingsViewModel.fadeDurationSeconds.toFloat())
    }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "sleep_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )

    if (showCustomTimerDialog) {
        var inputText by remember {
            mutableStateOf(
                if (settingsViewModel.customSleepTimerMinutes > 0)
                    settingsViewModel.customSleepTimerMinutes.toString()
                else ""
            )
        }
        val isValid = inputText.toIntOrNull()?.let { it in 1..360 } == true
        AlertDialog(
            onDismissRequest = { showCustomTimerDialog = false },
            containerColor   = Color(0xFF1A1A1A),
            title = { Text("Tiempo personalizado", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { new ->
                            val digits = new.filter { c -> c.isDigit() }.take(3)
                            val asInt  = digits.toIntOrNull() ?: 0
                            inputText  = if (asInt > 360) "360" else digits
                        },
                        label         = { Text("Minutos (1-360)", color = Color.Gray) },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedTextColor    = Color.White,
                            unfocusedTextColor  = Color.White,
                            focusedBorderColor  = accent,
                            unfocusedBorderColor = Color(0xFF2A2A2A)
                        )
                    )
                    if (inputText.isNotEmpty() && !isValid) {
                        Text("Ingresa un valor entre 1 y 360", color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = inputText.toIntOrNull() ?: return@Button
                        settingsViewModel.setCustomSleepTimer(mins) {}
                        showCustomTimerDialog = false
                    },
                    enabled = isValid,
                    colors  = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Iniciar", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimerDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SettingsSubScreenHeader(title = "Sleep Timer", icon = Icons.Filled.Bedtime, accent = accent, onBack = onBack) }

        item {
            AnimatedVisibility(
                visible = settingsViewModel.isSleepTimerActive,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.1f))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedCountdownCircle(
                            remainingMs = settingsViewModel.sleepTimerRemainingMs,
                            totalMs = settingsViewModel.sleepTimerMinutes * 60_000L,
                            accent = accent
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Pausando en", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                settingsViewModel.formatSleepRemaining(),
                                color = accent,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    IconButton(onClick = { settingsViewModel.cancelSleepTimer() }) {
                        Icon(Icons.Filled.Close, null, tint = Color.Gray)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SLEEP_TIMER_OPTIONS.zip(SLEEP_TIMER_LABELS).forEachIndexed { i, (mins, label) ->
                        val isSelected  = settingsViewModel.sleepTimerMinutes == mins &&
                                (mins == 0 || settingsViewModel.isSleepTimerActive)
                        val interaction = remember { MutableInteractionSource() }
                        val isPressed   by interaction.collectIsPressedAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(if (isPressed) 0.97f else 1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable(interactionSource = interaction, indication = null) {
                                    if (mins == 0) settingsViewModel.cancelSleepTimer()
                                    else settingsViewModel.startSleepTimer(mins) {}
                                }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) accent.copy(alpha = 0.2f) else Color(0xFF1E1E1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (mins == 0) Icons.Filled.Close else Icons.Filled.Bedtime,
                                        null,
                                        tint = if (isSelected) accent else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    label,
                                    color = if (isSelected) accent else Color.White,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (i < SLEEP_TIMER_OPTIONS.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = Color(0xFF1E1E1E),
                                thickness = 0.5.dp
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 20.dp),
                        color     = Color(0xFF1E1E1E),
                        thickness = 0.5.dp
                    )
                    val isCustomSelected = settingsViewModel.isSleepTimerActive &&
                            settingsViewModel.customSleepTimerMinutes > 0 &&
                            settingsViewModel.sleepTimerMinutes == settingsViewModel.customSleepTimerMinutes
                    val customInteraction = remember { MutableInteractionSource() }
                    val isCustomPressed   by customInteraction.collectIsPressedAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(if (isCustomPressed) 0.97f else 1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCustomSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable(interactionSource = customInteraction, indication = null) {
                                showCustomTimerDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCustomSelected) accent.copy(alpha = 0.2f) else Color(0xFF1E1E1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.EditNote,
                                    null,
                                    tint = if (isCustomSelected) accent else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                if (settingsViewModel.customSleepTimerMinutes > 0)
                                    "${settingsViewModel.customSleepTimerMinutes}m (personalizado)"
                                else
                                    "Personalizado...",
                                color = if (isCustomSelected) accent else Color.White,
                                fontWeight = if (isCustomSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                        if (isCustomSelected) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Box {
                    if (fadeSliderValue > 0f) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            for (i in 0..2) {
                                val path = androidx.compose.ui.graphics.Path()
                                path.moveTo(0f, h / 2f)
                                val freq = 1.0f + (i * 0.5f)
                                val amp = h * 0.18f
                                val phase = wavePhase * (if (i % 2 == 0) 1f else -1f) + (i * PI.toFloat() / 3f)
                                var x = 0f
                                while (x <= w) {
                                    val y = h / 2f + sin((x / w * freq * 2f * PI.toFloat()) + phase) * amp
                                    path.lineTo(x, y)
                                    x += 6f
                                }
                                drawPath(
                                    path = path,
                                    color = accent.copy(alpha = 0.12f - (i * 0.03f)),
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.VolumeUp, null, tint = accent, modifier = Modifier.size(18.dp))
                                }
                                Text("Fade al pausar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(
                                if (fadeSliderValue.toInt() == 0) "Sin fade"
                                else "${fadeSliderValue.toInt()}s",
                                color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                            )
                        }
                        Slider(
                            value         = fadeSliderValue,
                            onValueChange = { fadeSliderValue = it },
                            onValueChangeFinished = {
                                settingsViewModel.updateFadeDuration(fadeSliderValue.toInt())
                            },
                            valueRange    = 0f..60f,
                            steps         = 11,
                            colors        = SliderDefaults.colors(
                                thumbColor         = accent,
                                activeTrackColor   = accent,
                                inactiveTrackColor = Color(0xFF2A2A2A)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sin fade", color = Color.Gray, fontSize = 11.sp)
                            Text("60s", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onBack: () -> Unit
) {
    val theme  = LocalFxzTheme.current
    val accent = theme.accent

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(theme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SettingsSubScreenHeader(title = "Tema y colores", icon = Icons.Filled.Palette, accent = accent, onBack = onBack) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.3f), accent.copy(alpha = 0.05f)))),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Vista previa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "${ACCENT_NAMES.getOrElse(settingsViewModel.accentColorIndex) { "Verde" }} · ${THEME_NAMES[ThemeMode.entries.indexOf(settingsViewModel.themeMode)]}",
                            color = accent, fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Modo visual", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        val isSelected  = settingsViewModel.themeMode == mode
                        val interaction = remember { MutableInteractionSource() }
                        val isPressed   by interaction.collectIsPressedAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(if (isPressed) 0.97f else 1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable(interactionSource = interaction, indication = null) {
                                    settingsViewModel.updateThemeMode(mode)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) accent.copy(alpha = 0.2f) else Color(0xFF1E1E1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (mode) {
                                        ThemeMode.AMOLED        -> Icons.Filled.DarkMode
                                        ThemeMode.SOFT_DARK     -> Icons.Filled.Brightness4
                                        ThemeMode.GLASSMORPHISM -> Icons.Filled.BlurOn
                                    }
                                    Icon(icon, null, tint = if (isSelected) accent else Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        THEME_NAMES[i],
                                        color      = if (isSelected) accent else Color.White,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize   = 14.sp
                                    )
                                    Text(
                                        THEME_DESCRIPTIONS[i],
                                        color    = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier.size(26.dp).clip(CircleShape).background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        if (i < ThemeMode.entries.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = Color(0xFF1E1E1E), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Color de acento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(ACCENT_COLORS) { i, color ->
                            val isSelected = settingsViewModel.accentColorIndex == i
                            val scale by animateFloatAsState(
                                targetValue   = if (isSelected) 1.2f else 1f,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                                label         = "accent_scale_$i"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                        .clickable { settingsViewModel.setAccentColor(i) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                                Text(
                                    ACCENT_NAMES[i],
                                    color      = if (isSelected) color else Color.Gray,
                                    fontSize   = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSubScreenHeader(
    title: String,
    icon: ImageVector,
    accent: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    badge: String? = null,
    badgeColor: Color = Color(0xFF00E676)
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed   by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.97f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = if (badge != null) badgeColor else Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
    }
}

@Composable
fun ShakeToggleRow(enabled: Boolean, accent: Color, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Vibration, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Shake para saltar", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(if (enabled) "Activo" else "Agita el teléfono para siguiente canción", color = if (enabled) accent else Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.Black,
                checkedTrackColor  = accent,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

@Composable
fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = if (checked) accent else Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.Black,
                checkedTrackColor  = accent,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

@Composable
fun AboutAppCard(accent: Color) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent, accent.copy(alpha = 0.5f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        "FxzMusic",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Beta 1.0.0",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "FxzMusic es un reproductor de música local fluido, moderno y altamente personalizable, potenciado por Jetpack Compose y Media3. Este representa mi primer proyecto \"grande\" en el desarrollo móvil.",
                color = Color.Gray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "Desarrollador",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        "SebaxxFxz",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.1f))
                    .clickable {
                        uriHandler.openUri("https://github.com/sebaxxfxz")
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "GitHub",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ver código y otros proyectos",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hecho con ", color = Color(0xFF555555), fontSize = 12.sp)
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(" en Android", color = Color(0xFF555555), fontSize = 12.sp)
            }
        }
    }
}
