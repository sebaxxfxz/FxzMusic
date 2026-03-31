package com.example.fxzmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

private enum class SearchCategory { CANCIONES, ARTISTAS, ALBUMES }

@Composable
fun SearchScreen(
    libraryViewModel: LibraryViewModel,
    onPlaySong: (Song) -> Unit,
    onEditTags: (Song) -> Unit = {},
    initialQuery: String = "",
    onQueryConsumed: () -> Unit = {}
) {
    val theme = LocalFxzTheme.current
    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
    var searchText    by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            searchText = initialQuery
            onQueryConsumed()
        }
    }
    var activeCategory by rememberSaveable { mutableStateOf(SearchCategory.CANCIONES) }
    var previewSong   by remember { mutableStateOf<Song?>(null) }
    var previewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var previewProgress by remember { mutableFloatStateOf(0f) }
    val previewScope  = rememberCoroutineScope()
    var previewJob    by remember { mutableStateOf<Job?>(null) }

    fun startPreview(song: Song) {
        previewJob?.cancel()
        previewPlayer?.release()
        previewPlayer = null
        previewProgress = 0f
        previewSong = song
        previewJob = previewScope.launch {
            val mp = android.media.MediaPlayer()
            try {
                mp.setDataSource(song.filePath)
                mp.prepare()
                val midpoint = (mp.duration / 2).coerceAtLeast(0)
                mp.seekTo(midpoint)
                mp.start()
                previewPlayer = mp
                val startMs = System.currentTimeMillis()
                val previewDurationMs = 5_000L
                while (System.currentTimeMillis() - startMs < previewDurationMs && mp.isPlaying) {
                    previewProgress = ((System.currentTimeMillis() - startMs).toFloat() / previewDurationMs).coerceIn(0f, 1f)
                    delay(100)
                }
            } catch (_: Exception) {
            } finally {
                mp.runCatching { stop() }
                mp.release()
                previewPlayer = null
                previewSong   = null
                previewProgress = 0f
            }
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewPlayer?.stop()
        previewPlayer?.release()
        previewPlayer = null
        previewSong   = null
        previewProgress = 0f
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val allSongs = libraryViewModel.allSongs

    var debouncedSearch by remember { mutableStateOf(searchText) }
    LaunchedEffect(searchText) {
        if (searchText.isBlank()) {
            debouncedSearch = ""
        } else {
            delay(200)
            debouncedSearch = searchText
        }
    }

    val filteredSongs by remember(debouncedSearch, allSongs) {
        derivedStateOf {
            if (debouncedSearch.isBlank()) allSongs
            else allSongs.filter {
                it.title.contains(debouncedSearch, ignoreCase = true) ||
                        it.artist.contains(debouncedSearch, ignoreCase = true) ||
                        it.album.contains(debouncedSearch, ignoreCase = true)
            }
        }
    }

    val artistResults by remember(filteredSongs) {
        derivedStateOf {
            filteredSongs
                .groupBy { it.artist.split(",", "/", "&", "feat.", "ft.").first().trim() }
                .entries
                .sortedByDescending { it.value.size }
                .take(20)
        }
    }

    val albumResults by remember(filteredSongs) {
        derivedStateOf {
            filteredSongs
                .groupBy { it.album }
                .entries
                .sortedByDescending { it.value.size }
                .take(20)
        }
    }

    val categories = SearchCategory.entries
    val counts = mapOf(
        SearchCategory.CANCIONES to filteredSongs.size,
        SearchCategory.ARTISTAS  to artistResults.size,
        SearchCategory.ALBUMES   to albumResults.size
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalFxzTheme.current.background)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 52.dp)) {
            Text(
                "Buscar",
                color      = LocalFxzTheme.current.accent,
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value         = searchText,
                onValueChange = { searchText = it },
                placeholder   = { Text("Canciones, artistas, álbumes...", color = Color.Gray) },
                leadingIcon   = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                trailingIcon  = {
                    AnimatedVisibility(visible = searchText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(theme.surfaceVariant.copy(alpha = if (isGlass) 0.75f else 1f))
                                .clickable { searchText = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                },
                modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                shape         = RoundedCornerShape(20.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor  = theme.surfaceVariant.copy(alpha = if (isGlass) 0.8f else 1f),
                    focusedContainerColor    = theme.surfaceVariant.copy(alpha = if (isGlass) 0.9f else 1f),
                    unfocusedBorderColor     = Color.Transparent,
                    focusedBorderColor       = theme.accent,
                    cursorColor              = theme.accent,
                    focusedTextColor         = Color.White,
                    unfocusedTextColor       = Color.White
                ),
                singleLine    = true
            )
            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = cat == activeCategory
                    val count      = counts[cat] ?: 0
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    Card(
                        modifier = Modifier
                            .scale(if (isPressed) 0.93f else 1f)
                            .clickable(interactionSource = interaction, indication = null) { activeCategory = cat },
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) theme.accent else theme.surface.copy(alpha = if (isGlass) 0.78f else 1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                cat.name.lowercase().replaceFirstChar { it.uppercase() },
                                color      = if (isSelected) Color.Black else Color.White,
                                fontSize   = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (searchText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.Black.copy(alpha = 0.15f)
                                            else Color.White.copy(alpha = 0.1f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "$count",
                                        color    = if (isSelected) Color.Black else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        AnimatedContent(
            targetState   = activeCategory,
            transitionSpec = {
                (slideInVertically { it / 8 } + fadeIn()) togetherWith (slideOutVertically { -it / 8 } + fadeOut())
            },
            label = "search_category"
        ) { category ->
            when (category) {
                SearchCategory.CANCIONES -> {
                    if (filteredSongs.isEmpty()) {
                        SearchEmptyState(searchText)
                    } else {
                        LazyColumn(
                            modifier        = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding  = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp)
                        ) {
                            if (searchText.isBlank()) {
                                item {
                                    Text(
                                        "Todas las canciones · ${filteredSongs.size}",
                                        color    = Color.Gray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            items(filteredSongs, key = { it.id }) { song ->
                                SearchResultItem(
                                    song        = song,
                                    onClick     = { onPlaySong(song) },
                                    onEditTags  = { onEditTags(song) },
                                    onLongPress = { startPreview(song) }
                                )
                            }
                        }
                    }
                }
                SearchCategory.ARTISTAS -> {
                    if (artistResults.isEmpty()) {
                        SearchEmptyState(searchText)
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp)
                        ) {
                            items(artistResults.size) { index ->
                                val entry = artistResults[index]
                                ArtistSearchItem(
                                    name      = entry.key,
                                    songCount = entry.value.size,
                                    coverUrl  = entry.value.firstOrNull { it.coverUrl != null }?.coverUrl,
                                    albumArt  = entry.value.first().albumArt,
                                    onClick   = { onPlaySong(entry.value.first()) }
                                )
                            }
                        }
                    }
                }
                SearchCategory.ALBUMES -> {
                    if (albumResults.isEmpty()) {
                        SearchEmptyState(searchText)
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp)
                        ) {
                            items(albumResults.size) { index ->
                                val entry = albumResults[index]
                                AlbumSearchItem(
                                    name      = entry.key,
                                    artist    = entry.value.map { it.artist }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "",
                                    songCount = entry.value.size,
                                    coverUrl  = entry.value.firstOrNull { it.coverUrl != null }?.coverUrl,
                                    albumArt  = entry.value.first().albumArt,
                                    onClick   = { onPlaySong(entry.value.first()) }
                                )
                            }
                        }
                    }
                }
            }
        }
        val songForPreview = previewSong
        if (songForPreview != null) {
            Box(modifier = Modifier.fillMaxSize().zIndex(5f)) {
                SongPreviewOverlay(
                    song      = songForPreview,
                    progress  = previewProgress,
                    onDismiss = { stopPreview() }
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (query.isBlank()) "Escribe para buscar" else "Sin resultados para \"$query\"",
                color     = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ArtistSearchItem(
    name: String,
    songCount: Int,
    coverUrl: String?,
    albumArt: List<Color>,
    onClick: () -> Unit
) {
    val theme = LocalFxzTheme.current
    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface.copy(alpha = if (isGlass) 0.78f else 1f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(Brush.linearGradient(albumArt)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(coverUrl).crossfade(true).build(),
                        contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(name.take(1).uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$songCount canciones", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AlbumSearchItem(
    name: String,
    artist: String,
    songCount: Int,
    coverUrl: String?,
    albumArt: List<Color>,
    onClick: () -> Unit
) {
    val theme = LocalFxzTheme.current
    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface.copy(alpha = if (isGlass) 0.78f else 1f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(albumArt))
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(coverUrl).crossfade(true).build(),
                        contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$artist · $songCount canciones", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(song: Song, onClick: () -> Unit, onEditTags: () -> Unit = {}, onLongPress: () -> Unit = {}) {
    val theme = LocalFxzTheme.current
    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface.copy(alpha = if (isGlass) 0.78f else 1f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors = song.albumArt))
            ) {
                if (song.coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(song.coverUrl).crossfade(true).build(),
                        contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} · ${song.album}", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatTime(song.duration), color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded          = showMenu,
                    onDismissRequest  = { showMenu = false },
                    modifier          = Modifier.background(theme.surface.copy(alpha = if (isGlass) 0.9f else 1f))
                ) {
                    DropdownMenuItem(
                        text    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Edit, null, tint = LocalFxzTheme.current.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar etiquetas", color = Color.White)
                            }
                        },
                        onClick = { onEditTags(); showMenu = false }
                    )
                }
            }
        }
    }
}