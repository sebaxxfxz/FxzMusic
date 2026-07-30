package com.fxzmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.data.AudioMetadata
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.util.AudioFormatDetector
import com.fxzmusic.app.util.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: Song,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var metadata by remember { mutableStateOf<AudioMetadata?>(null) }
    var audioFormat by remember { mutableStateOf<com.fxzmusic.app.util.AudioFormatInfo?>(null) }

    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(song.filePath) {
        metadata = withContext(Dispatchers.IO) {
            song.filePath?.takeIf { it.isNotEmpty() }?.let { MetadataUtils.extractAudioMetadata(it) }
        }
        audioFormat = withContext(Dispatchers.IO) {
            song.filePath?.takeIf { it.isNotEmpty() }?.let { AudioFormatDetector.detect(it) }
        }
    }

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
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = accent)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} · ${song.album}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val isLossless = metadata?.formatDisplayName in listOf("FLAC", "WAV")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.HighQuality,
                            contentDescription = null,
                            tint = if (isLossless) accent else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isLossless) "Hi-Res Lossless Audio" else if (song.isYouTube) "Streaming HQ Audio" else "Audio Estándar HQ",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append(metadata?.formatDisplayName ?: if (song.isYouTube) "Opus" else "MP3")
                                    metadata?.bitrate?.let { append(" · ${it / 1000} kbps") }
                                    metadata?.sampleRate?.let { append(" · ${it / 1000.0} kHz") }
                                },
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "ESPECIFICACIÓN DE AUDIO",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpecRow(icon = Icons.Filled.GraphicEq, label = "Formato de Audio", value = metadata?.formatDisplayName ?: if (song.isYouTube) "Opus / WebM" else "MP3")
                    SpecRow(icon = Icons.Filled.GraphicEq, label = "Códec", value = metadata?.codecString ?: audioFormat?.codec ?: if (song.isYouTube) "opus" else "mp3")
                    SpecRow(icon = Icons.Filled.GraphicEq, label = "Bitrate", value = metadata?.bitrateKbps ?: AudioFormatDetector.formatBitrate(audioFormat?.bitrate ?: 0))
                    SpecRow(icon = Icons.Filled.GraphicEq, label = "Frecuencia de Muestreo", value = metadata?.sampleRateFormatted ?: AudioFormatDetector.formatSampleRate(audioFormat?.sampleRate ?: 0))
                    SpecRow(icon = Icons.Filled.GraphicEq, label = "Canales de Audio", value = metadata?.channelsText ?: "${audioFormat?.channels ?: 2}ch Stereo")
                }
            }

            Text(
                text = "ARCHIVO Y FUENTE",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpecRow(icon = Icons.Filled.Folder, label = "Fuente de Origen", value = if (song.isYouTube) "YouTube Music (Streaming)" else "Almacenamiento Local")
                    SpecRow(icon = Icons.Filled.Folder, label = "Duración Exacta", value = metadata?.durationFormatted ?: "${song.duration / 60}:${String.format(java.util.Locale.US, "%02d", song.duration % 60)}")
                    SpecRow(icon = Icons.Filled.Folder, label = "Tamaño de Archivo", value = metadata?.fileSizeFormatted ?: "N/A (Streamed)")
                    SpecRow(icon = Icons.Filled.Folder, label = "Ruta / Ubicación", value = song.filePath?.takeIf { it.isNotEmpty() } ?: "YouTube Stream Cache")
                }
            }
        }
    }
}

@Composable
private fun SpecRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
