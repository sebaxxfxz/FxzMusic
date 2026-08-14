package com.fxzmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenuSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onSongInfo: () -> Unit,
    onComments: () -> Unit,
    onThemeChange: () -> Unit,
    onOpenCarMode: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    onSimilarSongs: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    lyricsOffsetMs: Long = 0L,
    hasSyncedLyrics: Boolean = false,
    onLyricsOffsetPlus: () -> Unit = {},
    onLyricsOffsetMinus: () -> Unit = {},
    onLyricsOffsetReset: () -> Unit = {}
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Opciones",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PlayerMenuItem(
                icon = Icons.Filled.AutoAwesome,
                label = stringResource(R.string.action_similar_songs),
                accentColor = true,
                onClick = { onDismiss(); onSimilarSongs() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.Radio,
                label = stringResource(R.string.action_start_radio),
                accentColor = true,
                onClick = { onDismiss(); onStartRadio() }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            PlayerMenuItem(
                icon = Icons.Filled.Add,
                label = "Agregar a Playlist",
                onClick = { onDismiss(); onAddToPlaylist() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.Share,
                label = "Compartir",
                onClick = { onDismiss(); onShare() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.Download,
                label = "Descargar",
                onClick = { onDismiss(); onDownload() }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            PlayerMenuItem(
                icon = Icons.Filled.Info,
                label = "Info de Canción",
                onClick = { onDismiss(); onSongInfo() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.ChatBubbleOutline,
                label = "Comentarios",
                onClick = { onDismiss(); onComments() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.Palette,
                label = "Cambiar tema de acento",
                onClick = { onDismiss(); onThemeChange() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.DirectionsCar,
                label = stringResource(R.string.action_car_mode),
                onClick = { onDismiss(); onOpenCarMode() }
            )
            PlayerMenuItem(
                icon = Icons.Filled.Timer,
                label = "Temporizador de apagado",
                onClick = { onDismiss(); onSleepTimer() }
            )

            if (hasSyncedLyrics) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                val offset = lyricsOffsetMs
                val sign = if (offset >= 0) "+" else ""
                val offsetLabel = "${sign}${(offset / 1000.0).toString().take(4)}s"

                PlayerMenuItem(
                    icon = Icons.Filled.MusicNote,
                    label = "Letra: $offsetLabel (+)",
                    onClick = { onDismiss(); onLyricsOffsetPlus() }
                )
                PlayerMenuItem(
                    icon = Icons.Filled.MusicNote,
                    label = "Letra: $offsetLabel (-)",
                    onClick = { onDismiss(); onLyricsOffsetMinus() }
                )
                if (offset != 0L) {
                    PlayerMenuItem(
                        icon = Icons.Filled.Refresh,
                        label = "Reset offset",
                        accentColor = true,
                        onClick = { onDismiss(); onLyricsOffsetReset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerMenuItem(
    icon: ImageVector,
    label: String,
    accentColor: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val textColor = if (accentColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val iconTint = if (accentColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
