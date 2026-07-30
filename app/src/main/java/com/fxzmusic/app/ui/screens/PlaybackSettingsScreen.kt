package com.fxzmusic.app.ui.screens
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.viewmodel.*
import com.fxzmusic.app.ui.components.*
import com.fxzmusic.app.ui.theme.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.util.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fxzmusic.innertube.models.IpVersion
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class SettingsScreen {
    object Main    : SettingsScreen()
    object Theme   : SettingsScreen()
    object Sound   : SettingsScreen()
    object Folders : SettingsScreen()
}

@Composable
fun PlaybackSettingsScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onPausePlayback: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenAudioFx: () -> Unit = {},
    allSongs: List<Song> = emptyList()
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
                onOpenTheme       = { currentScreen = SettingsScreen.Theme },
                onOpenSound       = { currentScreen = SettingsScreen.Sound },
                onOpenFolders     = { currentScreen = SettingsScreen.Folders },
                onPausePlayback   = onPausePlayback
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
            SettingsScreen.Folders -> FolderBlacklistScreen(
                settingsViewModel = settingsViewModel,
                allSongs          = allSongs,
                onBack            = { currentScreen = SettingsScreen.Main }
            )
        }
    }
}

@Composable
private fun SettingsMainScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    onOpenTheme: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenFolders: () -> Unit,
    onPausePlayback: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioOutputManager(context) }
    LaunchedEffect(Unit) { manager.refresh() }
    val deviceLabel = manager.getCurrentLabel()
    var showAudioDeviceSheet by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()

    val accent = MaterialTheme.colorScheme.primary
    val speedLabel = PLAYBACK_SPEED_LABELS.getOrElse(
        PLAYBACK_SPEEDS.indexOfFirst { it == settingsViewModel.playbackSpeed }.coerceAtLeast(0)
    ) { "1x" }

    if (showAudioDeviceSheet) {
        AudioDeviceSheet(
            manager = manager,
            accent  = accent,
            onDismiss = { showAudioDeviceSheet = false }
        )
    }

    var showIpVersionDialog by remember { mutableStateOf(false) }

    if (showIpVersionDialog) {
        Dialog(
            onDismissRequest = { showIpVersionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Versión IP",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    IpVersion.entries.forEach { ipVer ->
                        val isSelected = settingsViewModel.ipVersion == ipVer
                        val label = when (ipVer) {
                            IpVersion.AUTO -> "Automático"
                            IpVersion.IPV4 -> "IPv4 (Recomendado)"
                            IpVersion.IPV6 -> "IPv6"
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    settingsViewModel.updateIpVersion(ipVer)
                                    showIpVersionDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showIpVersionDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cerrar", color = accent)
                    }
                }
            }
        }
    }

    updateInfoState?.let { info ->
        UpdateDialog(
            updateInfo = info,
            onDismiss = { updateInfoState = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Configuración", color = MaterialTheme.colorScheme.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "AUDIO Y REPRODUCCIÓN",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                SettingsGroupCard {
                    SettingsItemRow(
                        icon = Icons.Filled.GraphicEq,
                        title = "Audio y Ecualizador",
                        subtitle = "Ajustes de sonido y volumen",
                        accent = accent,
                        onClick = onOpenSound
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                    SettingsItemRow(
                        icon = Icons.Filled.Vibration,
                        title = "Agitar para saltar",
                        subtitle = if (settingsViewModel.shakeToSkip) "Activado" else "Desactivado",
                        accent = accent,
                        trailingContent = {
                            Switch(
                                checked = settingsViewModel.shakeToSkip,
                                onCheckedChange = { settingsViewModel.toggleShakeToSkip() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor  = accent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "APARIENCIA",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                SettingsGroupCard {
                    SettingsItemRow(
                        icon = Icons.Filled.Palette,
                        title = "Tema y colores",
                        subtitle = "${THEME_NAMES[ThemeMode.values().indexOf(settingsViewModel.themeMode)]}",
                        accent = accent,
                        onClick = onOpenTheme
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "ALMACENAMIENTO Y BIBLIOTECA",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                SettingsGroupCard {
                    StorageUsageRow(settingsViewModel = settingsViewModel, accent = accent)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                    SettingsItemRow(
                        icon = Icons.Filled.FolderOff,
                        title = "Carpetas ocultas",
                        subtitle = if (settingsViewModel.blacklistedFolders.isEmpty()) "Ninguna carpeta oculta"
                                   else "${settingsViewModel.blacklistedFolders.size} carpeta(s) oculta(s)",
                        accent = accent,
                        onClick = onOpenFolders
                    )
                }
            }
        }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "CONECTIVIDAD Y DISPOSITIVOS",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                SettingsGroupCard {
                    SettingsItemRow(
                        icon = Icons.Filled.SpeakerGroup,
                        title = "Salida de audio activa",
                        subtitle = deviceLabel,
                        accent = accent,
                        onClick = {
                            manager.refresh()
                            showAudioDeviceSheet = true
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                    SettingsItemRow(
                        icon = Icons.Filled.Headphones,
                        title = "Pausar al desconectar audífonos",
                        subtitle = "Pausa la música automáticamente al quitar auriculares o Bluetooth",
                        accent = accent,
                        trailingContent = {
                            Switch(
                                checked = settingsViewModel.pauseOnHeadphonesDisconnect,
                                onCheckedChange = { settingsViewModel.togglePauseOnHeadphonesDisconnect() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor  = accent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                    SettingsItemRow(
                        icon = Icons.Filled.BluetoothConnected,
                        title = "Reanudar al conectar audífonos",
                        subtitle = "Continúa la reproducción al reconectar dispositivos",
                        accent = accent,
                        trailingContent = {
                            Switch(
                                checked = settingsViewModel.resumeOnHeadphonesConnect,
                                onCheckedChange = { settingsViewModel.toggleResumeOnHeadphonesConnect() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor  = accent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                }
            }
        }

        item {
            AboutAppCard(
                accent = accent,
                isCheckingUpdate = isCheckingUpdate,
                onCheckUpdate = {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        android.widget.Toast.makeText(context, "Buscando actualizaciones...", android.widget.Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val result = UpdateChecker.checkForUpdates(context)
                            isCheckingUpdate = false
                            result.fold(
                                onSuccess = { info ->
                                    if (info.isUpdateAvailable) {
                                        updateInfoState = info
                                    } else {
                                        android.widget.Toast.makeText(context, "FxzMusic está actualizado (v${info.latestVersion})", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onFailure = {
                                    android.widget.Toast.makeText(context, "Error al buscar actualización.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: Color,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StorageUsageRow(settingsViewModel: PlaybackSettingsViewModel, accent: Color) {
    val used = settingsViewModel.usedStorageSpaceGb
    val total = settingsViewModel.totalStorageSpaceGb
    val fraction = if (total > 0f) (used / total).coerceIn(0f, 1f) else 0f
    val textLabel = String.format(java.util.Locale.US, "%.1f GB / %.1f GB", used, total)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Uso de Almacenamiento", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Espacio utilizado en el dispositivo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Text(textLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(accent)
            )
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
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SettingsSubScreenHeader(title = "Audio y Ecualizador", icon = Icons.Filled.GraphicEq, accent = accent, onBack = onBack) }

        item {
            SettingsGroupCard {
                SettingsItemRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Ecualizador de 10 bandas",
                    subtitle = "Ajusta perfiles de audio a tu gusto",
                    accent = accent,
                    onClick = onOpenEqualizer
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                SettingsItemRow(
                    icon = Icons.Filled.SurroundSound,
                    title = "Efectos de sonido",
                    subtitle = "Bass boost, virtualizador espacial",
                    accent = accent,
                    onClick = onOpenAudioFx
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                SettingsItemRow(
                    icon = Icons.Filled.Tune,
                    title = "Normalización de volumen",
                    subtitle = if (settingsViewModel.loudnessNormalization) "Volumen uniforme en todas las pistas" else "Desactivada",
                    accent = accent,
                    trailingContent = {
                        Switch(
                            checked = settingsViewModel.loudnessNormalization,
                            onCheckedChange = { settingsViewModel.updateLoudnessNormalization(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor  = accent,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
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
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
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
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
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
    val rowBg = MaterialTheme.colorScheme.surface
    val rowBorder = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scaleOnPress(interaction, PressScale.list)
            .clip(RoundedCornerShape(20.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(20.dp))
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
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = if (badge != null) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ShakeToggleRow(enabled: Boolean, accent: Color, onToggle: () -> Unit) {
    val rowBg = MaterialTheme.colorScheme.surface
    val rowBorder = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(20.dp))
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
            Text("Shake para saltar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(if (enabled) "Activo" else "Agita el teléfono para siguiente canción", color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor  = accent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
    val rowBg = MaterialTheme.colorScheme.surface
    val rowBorder = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(20.dp))
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
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor  = accent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun AboutAppCard(
    accent: Color,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    val context = LocalContext.current
    val currentVersion = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "3.0.0"
        } catch (_: Exception) {
            "3.0.0"
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent, accent.copy(alpha = 0.55f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "FxzMusic",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.2f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "v$currentVersion",
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        "Reproductor Local & YouTube Music",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                "FxzMusic es un reproductor de música ultra fluido, moderno y personalizable. Combina la reproducción de tu biblioteca local con la transmisión de millones de canciones de YouTube Music, audio de alta fidelidad, letras dinámicas y animaciones de resorte.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Compose", "Media3", "YouTube", "AMOLED").forEach { tech ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tech,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "SebaxxFxz",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verificado",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { onCheckUpdate() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent)
                    } else {
                        Icon(
                            Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Buscar Actualizaciones",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Comprobar si hay una nueva versión en GitHub",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable {
                        uriHandler.openUri("https://github.com/sebaxxfxz/FxzMusic")
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Repositorio en GitHub",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ver código fuente y contribuir al proyecto",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hecho con ", color = Color(0xFF666666), fontSize = 12.sp)
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(" en Android", color = Color(0xFF666666), fontSize = 12.sp)
            }
        }
    }
}
