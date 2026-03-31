package com.example.fxzmusic

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.core.content.edit

@Composable
fun ProfileScreen(
    statsViewModel: StatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val stats   = statsViewModel.stats
    val accent  = LocalFxzTheme.current.accent

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun enterAnim(delayMs: Int) =
        fadeIn(tween(400, delayMs)) + slideInVertically(tween(400, delayMs)) { it / 3 }

    val prefs        = remember { context.applicationContext.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    var profileName  by remember { mutableStateOf(prefs.getString("name", "Usuario") ?: "Usuario") }
    var profilePhoto by remember { mutableStateOf(prefs.getString("photo_uri", null)) }
    var editingName  by remember { mutableStateOf(false) }
    var nameField    by remember(profileName) { mutableStateOf(profileName) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            profilePhoto = uri.toString()
            prefs.edit { putString("photo_uri", uri.toString()) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LocalFxzTheme.current.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(visible = visible, enter = enterAnim(0)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Perfil", color = accent, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))))
                                .border(3.dp, accent, CircleShape)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhoto != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(profilePhoto).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    profileName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Edit, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (editingName) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nameField,
                                onValueChange = { nameField = it },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = Color(0xFF2A2A2A),
                                    focusedContainerColor = Color(0xFF1E1E1E),
                                    unfocusedContainerColor = Color(0xFF1E1E1E),
                                    cursorColor = accent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val trimmed = nameField.trim()
                                if (trimmed.isNotBlank()) {
                                    profileName = trimmed
                                    prefs.edit { putString("name", trimmed) }
                                }
                                editingName = false
                            }) {
                                Icon(Icons.Filled.Check, null, tint = accent)
                            }
                            IconButton(onClick = { editingName = false }) {
                                Icon(Icons.Filled.Close, null, tint = Color.Gray)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(profileName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            IconButton(
                                onClick = { editingName = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Text("Tus estadísticas locales", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        if (stats != null) {
            item {
                AnimatedVisibility(visible = visible, enter = enterAnim(100)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(icon = Icons.Filled.Headphones, value = statsViewModel.formatDuration(stats.totalListenedMs), label = "Total escuchado", modifier = Modifier.weight(1f))
                        StatCard(icon = Icons.Filled.MusicNote, value = "${stats.totalSongs}", label = "Canciones", modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(icon = Icons.Filled.Timer, value = statsViewModel.formatDuration(stats.todayListenedMs), label = "Hoy", modifier = Modifier.weight(1f))
                    StatCard(
                        icon        = Icons.Filled.LocalFireDepartment,
                        value       = if (stats.currentStreak > 0) "${stats.currentStreak} días" else "—",
                        label       = "Racha actual",
                        modifier    = Modifier.weight(1f),
                        accentColor = if (stats.currentStreak > 0) Color(0xFFFF6B35) else Color.Gray
                    )
                }
            }
            if (stats.topSongs.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = enterAnim(200)) {
                        Text("Top canciones", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                val topFive = stats.topSongs.take(5)
                items(topFive.size, key = { topFive[it].songId }) { index ->
                    AnimatedVisibility(visible = visible, enter = enterAnim(250 + index * 60)) {
                        TopSongItem(rank = index + 1, song = topFive[index], statsViewModel = statsViewModel)
                    }
                }
            }
            if (stats.topArtists.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = enterAnim(550)) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Top artistas", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(stats.topArtists, key = { it.artistName }) { artist ->
                                    ArtistStatCard(artist = artist, statsViewModel = statsViewModel)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                    Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎵", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Empieza a escuchar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tus estadísticas aparecerán aquí después de escuchar algunas canciones", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier, accentColor: Color = LocalFxzTheme.current.accent) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "stat_scale"
    )
    Card(
        modifier = modifier.scale(scale).clickable(interactionSource = interaction, indication = null) {},
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun TopSongItem(rank: Int, song: SongStats, statsViewModel: StatsViewModel) {
    val accent = LocalFxzTheme.current.accent
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "topsong_scale"
    )
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFB0BEC5)
        3 -> Color(0xFFCD7F32)
        else -> Color.Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interaction, indication = null) {},
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (rank == 1) Color(0xFF1A1A12) else Color(0xFF1E1E1E)
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                color      = rankColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 16.sp,
                modifier   = Modifier.width(36.dp)
            )
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(song.albumArt))) {
                if (song.coverUrl != null) {
                    AsyncImage(
                        model              = ImageRequest.Builder(LocalContext.current).data(song.coverUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title,  color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.Gray,  fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${song.playCount}x", color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(statsViewModel.formatDuration(song.totalListenedMs), color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ArtistStatCard(artist: ArtistStats, statsViewModel: StatsViewModel) {
    val accent = LocalFxzTheme.current.accent
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "artist_scale"
    )
    Card(
        modifier = Modifier.width(140.dp).scale(scale).clickable(interactionSource = interaction, indication = null) {},
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(artist.artistName.take(1).uppercase(), color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(artist.artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${artist.totalPlayCount} reproducciones", color = Color.Gray, fontSize = 11.sp)
            Text(statsViewModel.formatDuration(artist.totalListenedMs), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}