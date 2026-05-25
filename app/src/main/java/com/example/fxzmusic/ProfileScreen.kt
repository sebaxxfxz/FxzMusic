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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Stars
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
    statsViewModel: StatsViewModel = viewModel(),
    libraryViewModel: LibraryViewModel,
    onPlaySong: (Song) -> Unit,
    onShowFullPlayer: () -> Unit,
    onNavigateToLibrary: (String) -> Unit = {},
    onNavigateToSearchWithQuery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val stats   = statsViewModel.stats
    val accent  = LocalFxzTheme.current.accent
    val theme = LocalFxzTheme.current

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
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))))
                                .border(3.dp, accent.copy(alpha = 0.8f), CircleShape)
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
                                    color = Cinematic_OnSurface,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Edit, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (editingName) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            OutlinedTextField(
                                value = nameField,
                                onValueChange = { nameField = it },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Cinematic_OnSurface,
                                    unfocusedTextColor = Cinematic_OnSurface,
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = LocalFxzTheme.current.surfaceVariant,
                                    focusedContainerColor = LocalFxzTheme.current.surface,
                                    unfocusedContainerColor = LocalFxzTheme.current.surface,
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
                                Icon(Icons.Filled.Close, null, tint = Cinematic_MutedText)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(profileName, color = Cinematic_OnSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            IconButton(
                                onClick = { editingName = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Edit, null, tint = Cinematic_PlatinumText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    val totalMinutes = (stats?.totalListenedMs ?: 0L) / 60_000
                    val level = (totalMinutes / 300).coerceAtLeast(1L).toInt()
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            .background(accent.copy(alpha = 0.05f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Stars, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Text("Audiophile Level $level", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
            }
        }

        if (stats != null) {
            item {
                AnimatedVisibility(visible = visible, enter = enterAnim(100)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            StatCard(icon = Icons.Filled.Schedule, value = statsViewModel.formatDuration(stats.totalListenedMs), label = "MINUTOS SÓNICOS", modifier = Modifier.weight(1f))
                            StatCard(
                                icon = Icons.Filled.LibraryMusic,
                                value = "${stats.totalSongs}",
                                label = "CANCIONES ÚNICAS",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToLibrary("Todo") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            StatCard(icon = Icons.Filled.BarChart, value = statsViewModel.formatDuration(stats.todayListenedMs), label = "TIEMPO HOY", modifier = Modifier.weight(1f))
                            StatCard(
                                icon        = Icons.Filled.LocalFireDepartment,
                                value       = if (stats.currentStreak > 0) "${stats.currentStreak} días" else "—",
                                label       = "RACHA SÓNICA",
                                modifier    = Modifier.weight(1f),
                                accentColor = if (stats.currentStreak > 0) Color(0xFFFF6D00) else Cinematic_MutedText
                            )
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = visible, enter = enterAnim(150)) {
                    ListeningActivityCard(accent = accent)
                }
            }
            if (stats.topSongs.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = enterAnim(100)) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Top canciones", color = Cinematic_OnSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                val topFive = stats.topSongs.take(5)
                items(topFive.size, key = { topFive[it].songId }) { index ->
                    AnimatedVisibility(visible = visible, enter = enterAnim(150 + index * 60)) {
                        TopSongItem(
                            rank = index + 1,
                            song = topFive[index],
                            statsViewModel = statsViewModel,
                            onClick = {
                                val targetSong = libraryViewModel.allSongs.find { it.id == topFive[index].songId }
                                if (targetSong != null) {
                                    onPlaySong(targetSong)
                                    onShowFullPlayer()
                                }
                            }
                        )
                    }
                }
            }
            if (stats.topArtists.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = enterAnim(400)) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Top artistas", color = Cinematic_OnSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(stats.topArtists, key = { it.artistName }) { artist ->
                                    ArtistStatCard(
                                        artist = artist,
                                        statsViewModel = statsViewModel,
                                        onClick = {
                                            onNavigateToSearchWithQuery(artist.artistName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎵", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Empieza a escuchar", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tus estadísticas aparecerán aquí después de escuchar algunas canciones", color = Cinematic_PlatinumText, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalFxzTheme.current.accent,
    onClick: () -> Unit = {}
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "stat_scale"
    )
    GlassCard(
        modifier = modifier.scale(scale).clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape    = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(value, color = Cinematic_OnSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Cinematic_MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun ListeningActivityCard(accent: Color) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Actividad de Escucha", color = Cinematic_OnSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("ÚLTIMOS 7 DÍAS", color = Cinematic_MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val days = listOf("L", "M", "M", "J", "V", "S", "D")
                val heights = listOf(0.40f, 0.65f, 0.45f, 0.80f, 0.55f, 1.00f, 0.70f)
                val currentDayIndex = when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> 0
                    java.util.Calendar.TUESDAY -> 1
                    java.util.Calendar.WEDNESDAY -> 2
                    java.util.Calendar.THURSDAY -> 3
                    java.util.Calendar.FRIDAY -> 4
                    java.util.Calendar.SATURDAY -> 5
                    java.util.Calendar.SUNDAY -> 6
                    else -> 0
                }
                days.forEachIndexed { index, day ->
                    val heightFraction = heights[index]
                    val isCurrentDay = index == currentDayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(16.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isCurrentDay) accent else Cinematic_SurfaceVariant)
                        )
                        Text(
                            day,
                            color = if (isCurrentDay) Cinematic_OnSurface else Cinematic_MutedText,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopSongItem(rank: Int, song: SongStats, statsViewModel: StatsViewModel, onClick: () -> Unit) {
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
    
    val isGlass = LocalFxzTheme.current.mode == ThemeMode.GLASSMORPHISM
    val isRank1 = rank == 1
    
    val borderStroke = when {
        isRank1 -> BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f))
        isGlass -> BorderStroke(1.dp, Cinematic_GlassBorder)
        else -> BorderStroke(1.dp, LocalFxzTheme.current.surfaceVariant)
    }
    
    val containerBg = when {
        isRank1 -> Brush.linearGradient(listOf(Color(0xFFD4AF37).copy(alpha = 0.08f), Color(0xFFD4AF37).copy(alpha = 0.01f)))
        isGlass -> Brush.linearGradient(listOf(Cinematic_GlassBackground, Cinematic_GlassBackground))
        else -> Brush.linearGradient(listOf(LocalFxzTheme.current.surface, LocalFxzTheme.current.surface))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBg)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#$rank",
                    color = rankColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(36.dp)
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
                    Text(song.title,  color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = if (isRank1) Color(0xFFFFD700).copy(alpha = 0.7f) else Cinematic_PlatinumText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${song.playCount}x", color = if (isRank1) Color(0xFFFFD700) else accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(statsViewModel.formatDuration(song.totalListenedMs), color = if (isRank1) Color(0xFFFFD700).copy(alpha = 0.6f) else Cinematic_PlatinumText, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ArtistStatCard(artist: ArtistStats, statsViewModel: StatsViewModel, onClick: () -> Unit) {
    val accent = LocalFxzTheme.current.accent
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "artist_scale"
    )
    GlassCard(
        modifier = Modifier.width(140.dp).scale(scale).clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(artist.artistName.take(1).uppercase(), color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(artist.artistName, color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${artist.totalPlayCount} reproducciones", color = Cinematic_PlatinumText, fontSize = 11.sp)
            Text(statsViewModel.formatDuration(artist.totalListenedMs), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}