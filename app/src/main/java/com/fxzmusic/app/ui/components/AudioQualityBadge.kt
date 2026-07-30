package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.AudioMetadata
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioQualityBadge(
    metadata: AudioMetadata?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = metadata != null,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 },
        exit = fadeOut(tween(200))
    ) {
        metadata?.let { meta ->
            val isLossless = meta.formatDisplayName in listOf("FLAC", "WAV")
            val isHighQuality = meta.formatDisplayName == "Opus" ||
                    (meta.bitrate != null && meta.bitrate >= 256_000)

            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            isLossless -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            isHighQuality -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)
                        }
                    )
                    .border(
                        0.5.dp,
                        when {
                            isLossless -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            isHighQuality -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        },
                        RoundedCornerShape(16.dp)
                    )
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(onClick = onClick)
                        } else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLossless) {
                    Icon(
                        Icons.Filled.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = buildString {
                        append(meta.formatDisplayName)
                        meta.bitrate?.let { append(" \u00b7 ${it / 1000}kbps") }
                        meta.sampleRate?.let { append(" \u00b7 ${it / 1000.0}kHz") }
                    },
                    color = when {
                        isLossless -> MaterialTheme.colorScheme.primary
                        isHighQuality -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isLossless) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                if (isLossless) {
                    Text(
                        "Lossless",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
