package com.example.fxzmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Mic
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
    val context = LocalContext.current
    val theme = LocalFxzTheme.current
    val isGlass = theme.mode == ThemeMode.GLASSMORPHISM
    var searchText    by rememberSaveable { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                searchText = spokenText
            }
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Ambient Background Texture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x0EFFFFFF), // Subtle top-right ambient glow (~5.5% white)
                            Color.Transparent
                        ),
                        center = Offset(x = 1000f, y = -100f),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 52.dp)) {
                Column {
                    Text("BÚSQUEDA", color = Cinematic_PlatinumText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Buscar", color = Cinematic_OnSurface, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recessed Input Field
                OutlinedTextField(
                    value         = searchText,
                    onValueChange = { searchText = it },
                    placeholder   = { Text("¿Qué quieres escuchar?", color = Cinematic_PlatinumText, fontSize = 15.sp) },
                    leadingIcon   = { 
                        Icon(
                            Icons.Filled.Search, 
                            null, 
                            tint = if (searchText.isNotEmpty()) theme.accent else Cinematic_PlatinumText
                        ) 
                    },
                    trailingIcon  = {
                        if (searchText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { searchText = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", color = Color.White, fontSize = 11.sp)
                            }
                        } else {
                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora para buscar...")
                                }
                                try {
                                    voiceLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Reconocimiento de voz no disponible", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Mic, contentDescription = null, tint = Cinematic_PlatinumText)
                            }
                        }
                    },
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape         = RoundedCornerShape(16.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor  = Color(0x991C1B1B), // surface-container-low/60
                        focusedContainerColor    = Color(0xCC201F1F),   // surface-container/80
                        unfocusedBorderColor     = Color.White.copy(alpha = 0.08f),
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
                        
                        val chipBg = if (isSelected) theme.accent else Color(0x1AFFFFFF)
                        val chipBorder = if (isSelected) theme.accent else Color.White.copy(alpha = 0.08f)
                        val chipTextColor = if (isSelected) Color.Black else Cinematic_OnSurface

                        Box(
                            modifier = Modifier
                                .scale(if (isPressed) 0.93f else 1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                .clickable(interactionSource = interaction, indication = null) { activeCategory = cat }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    cat.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color      = chipTextColor,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                Spacer(modifier = Modifier.height(16.dp))
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
                color     = Cinematic_PlatinumText,
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
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape  = RoundedCornerShape(16.dp)
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
                    Text(name.take(1).uppercase(), color = Cinematic_OnSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$songCount canciones", color = Cinematic_PlatinumText, fontSize = 12.sp)
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
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape  = RoundedCornerShape(16.dp)
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
                Text(name, color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$artist · $songCount canciones", color = Cinematic_PlatinumText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape  = RoundedCornerShape(16.dp)
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
                Text(song.title, color = Cinematic_OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} · ${song.album}", color = Cinematic_PlatinumText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatTime(song.duration), color = Cinematic_PlatinumText, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, null, tint = Cinematic_PlatinumText, modifier = Modifier.size(18.dp))
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
                                Text("Editar etiquetas", color = Cinematic_OnSurface)
                            }
                        },
                        onClick = { onEditTags(); showMenu = false }
                    )
                }
            }
        }
    }
}