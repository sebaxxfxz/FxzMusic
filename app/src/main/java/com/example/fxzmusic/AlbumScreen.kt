package com.example.fxzmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.blur
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
fun AlbumScreen(
    albumName: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit
) {
    val theme       = LocalFxzTheme.current
    val coverSong   = songs.firstOrNull { it.coverUrl != null } ?: songs.firstOrNull()
    val totalTime   = formatTime(songs.sumOf { it.duration })

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        coverSong?.let { song ->
            if (song.coverUrl != null) {
                AsyncImage(
                    model            = ImageRequest.Builder(LocalContext.current).data(song.coverUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale     = ContentScale.Crop,
                    modifier         = Modifier.fillMaxWidth().height(340.dp).blur(60.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(Brush.linearGradient(song.albumArt))
                        .blur(60.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.3f), theme.background)
                        )
                    )
            )
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .padding(top = 52.dp, start = 16.dp, end = 16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            item {
                Column(
                    modifier            = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(coverSong?.albumArt ?: listOf(Color(0xFF4158D0), Color(0xFFC850C0))))
                            .align(Alignment.CenterHorizontally)
                    ) {
                        if (coverSong?.coverUrl != null) {
                            AsyncImage(
                                model            = ImageRequest.Builder(LocalContext.current).data(coverSong.coverUrl).crossfade(true).build(),
                                contentDescription = null,
                                contentScale     = ContentScale.Crop,
                                modifier         = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(albumName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${songs.size} canciones · $totalTime",
                        color = Color.Gray, fontSize = 13.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { if (songs.isNotEmpty()) onPlayAll(songs) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = theme.accent)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reproducir", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { if (songs.isNotEmpty()) onShuffleAll(songs.shuffled()) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Icon(Icons.Filled.Shuffle, null, tint = theme.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aleatorio", color = theme.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            itemsIndexed(songs) { index, song ->
                AlbumSongRow(
                    song     = song,
                    index    = index + 1,
                    accent   = theme.accent,
                    onClick  = { onPlaySong(song) }
                )
            }
        }
    }
}

@Composable
private fun AlbumSongRow(song: Song, index: Int, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed   by interaction.collectIsPressedAsState()
    val scale       by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "row_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = index.toString(),
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatTime(song.duration), color = Color.Gray, fontSize = 12.sp)
    }
}
