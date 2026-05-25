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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ArtistScreen(
    artistName: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit
) {
    val theme   = LocalFxzTheme.current
    val albums  = songs.groupBy { it.album }.entries.sortedBy { it.key }
    val totalMs = songs.sumOf { it.duration }

    val headerColors: List<Color> = songs.firstOrNull()?.albumArt
        ?.takeIf { it.size >= 2 }
        ?: listOf(Color(0xFF4158D0), Color(0xFFC850C0))

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.linearGradient(headerColors)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, theme.background)
                                )
                            )
                    )
                    Box(modifier = Modifier.padding(top = 52.dp, start = 16.dp)) {
                        BouncyIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, onClick = onBack)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Cinematic_GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarSong = songs.firstOrNull { it.coverUrl != null }
                            if (avatarSong?.coverUrl != null) {
                                AsyncImage(
                                    model              = ImageRequest.Builder(LocalContext.current).data(avatarSong.coverUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    artistName.take(1).uppercase(),
                                    color      = Cinematic_OnSurface,
                                    fontSize   = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(artistName, color = Cinematic_OnSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${songs.size} canciones · ${albums.size} álbumes · ${formatTime(totalMs)}",
                            color = Cinematic_PlatinumText, fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { if (songs.isNotEmpty()) onPlayAll(songs) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reproducir", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
                    val shuffleBg = if (isGlass) Cinematic_GlassBackground else Color(0xFF1E1E1E)
                    val shuffleBorderColor = if (isGlass) Cinematic_GlassBorder else Color.Transparent

                    Button(
                        onClick  = { if (songs.isNotEmpty()) onShuffleAll(songs.shuffled()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(if (isGlass) Modifier.border(1.dp, shuffleBorderColor, RoundedCornerShape(14.dp)) else Modifier),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = shuffleBg)
                    ) {
                        Icon(Icons.Filled.Shuffle, null, tint = theme.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Aleatorio", color = theme.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            albums.forEach { (album, albumSongs) ->
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Album, null, tint = theme.accent, modifier = Modifier.size(18.dp))
                        Text(album, color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                itemsIndexed(albumSongs) { index, song ->
                    ArtistSongRow(
                        song    = song,
                        index   = index + 1,
                        accent  = theme.accent,
                        onClick = { onPlaySong(song, albumSongs) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ArtistSongRow(song: Song, index: Int, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed   by interaction.collectIsPressedAsState()
    val scale       by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "row_scale"
    )

    val songColors: List<Color> = song.albumArt
        .takeIf { it.size >= 2 }
        ?: listOf(Color(0xFF1E1E1E), Color(0xFF2E2E2E))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = index.toString(),
            color    = Cinematic_MutedText,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp)
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(songColors))
                .border(1.dp, Cinematic_GlassBorder, RoundedCornerShape(8.dp))
        ) {
            if (song.coverUrl != null) {
                AsyncImage(
                    model              = ImageRequest.Builder(LocalContext.current).data(song.coverUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = Cinematic_OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.album, color = Cinematic_PlatinumText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatTime(song.duration), color = Cinematic_MutedText, fontSize = 12.sp)
    }
}
