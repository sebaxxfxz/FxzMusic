package com.fxzmusic.app.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.data.formatTime
import com.fxzmusic.app.ui.components.FilterChipsRow
import com.fxzmusic.app.ui.components.GlassCard
import com.fxzmusic.app.ui.components.OfflineBanner
import com.fxzmusic.app.ui.components.PressScale
import com.fxzmusic.app.ui.components.SongPreviewOverlay
import com.fxzmusic.app.ui.components.StaggeredItem
import com.fxzmusic.app.ui.components.YouTubeAlbumCard
import com.fxzmusic.app.ui.components.YouTubeArtistCard
import com.fxzmusic.app.ui.components.YouTubePlaylistCard
import com.fxzmusic.app.ui.components.YouTubeSongCard
import com.fxzmusic.app.ui.components.scaleOnPress
import com.fxzmusic.app.util.NetworkStatus
import com.fxzmusic.app.util.toSong
import com.fxzmusic.app.viewmodel.LibraryViewModel
import com.fxzmusic.app.viewmodel.SearchUiState
import com.fxzmusic.app.viewmodel.YouTubeMusicViewModel
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.AlbumItem
import com.fxzmusic.innertube.models.ArtistItem
import com.fxzmusic.innertube.models.PlaylistItem
import com.fxzmusic.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SourceFilter { TODO, LOCAL, YOUTUBE }
private enum class LocalCategory { CANCIONES, ARTISTAS, ALBUMES }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    libraryViewModel: LibraryViewModel,
    youTubeViewModel: YouTubeMusicViewModel,
    onPlaySong: (Song) -> Unit,
    onPlayYouTubeSong: (Song, List<Song>) -> Unit = { _, _ -> },
    onOpenAlbum: (browseId: String) -> Unit = {},
    onOpenArtist: (browseId: String) -> Unit = {},
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    onEditTags: (Song) -> Unit = {},
    initialQuery: String = "",
    onQueryConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    var searchText by rememberSaveable { mutableStateOf("") }
    val recentSearches = rememberSaveable { mutableStateListOf<String>() }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            recentSearches.remove(trimmed)
            recentSearches.add(0, trimmed)
            if (recentSearches.size > 5) {
                recentSearches.removeAt(recentSearches.lastIndex)
            }
        }
    }

    var activeSource by rememberSaveable { mutableStateOf(SourceFilter.TODO) }
    var activeLocalCategory by rememberSaveable { mutableStateOf(LocalCategory.CANCIONES) }
    var selectedYtFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }

    val ytState = youTubeViewModel.search

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                searchText = spokenText
                addRecentSearch(spokenText)
                youTubeViewModel.runSearch(spokenText, selectedYtFilter)
            }
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            searchText = initialQuery
            addRecentSearch(initialQuery)
            youTubeViewModel.runSearch(initialQuery, selectedYtFilter)
            onQueryConsumed()
        }
    }

    var previewSong by remember { mutableStateOf<Song?>(null) }
    var previewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var previewProgress by remember { mutableFloatStateOf(0f) }
    val previewScope = rememberCoroutineScope()
    var previewJob by remember { mutableStateOf<Job?>(null) }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        val mp = previewPlayer
        previewPlayer = null
        previewSong = null
        previewProgress = 0f
        if (mp != null) {
            runCatching { mp.stop() }
            mp.release()
        }
    }

    fun startPreview(song: Song) {
        stopPreview()
        previewProgress = 0f
        previewSong = song
        previewJob = previewScope.launch(Dispatchers.IO) {
            val mp = android.media.MediaPlayer()
            try {
                mp.setDataSource(song.filePath)
                mp.prepare()
                val midpoint = (mp.duration / 2).coerceAtLeast(0)
                mp.seekTo(midpoint)
                mp.start()
                withContext(Dispatchers.Main) {
                    previewPlayer = mp
                }
                val startMs = System.currentTimeMillis()
                val previewDurationMs = 5_000L
                while (System.currentTimeMillis() - startMs < previewDurationMs && mp.isPlaying) {
                    val progress = ((System.currentTimeMillis() - startMs).toFloat() / previewDurationMs).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        previewProgress = progress
                    }
                    delay(100)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { mp.stop() }
                mp.release()
                withContext(Dispatchers.Main) {
                    if (previewPlayer == mp) {
                        previewPlayer = null
                        previewSong = null
                        previewProgress = 0f
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (searchText.isBlank() && initialQuery.isBlank()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val networkStatus by youTubeViewModel.networkStatus.collectAsState()
    val allSongs = libraryViewModel.allSongs

    var debouncedSearch by remember { mutableStateOf(searchText) }
    LaunchedEffect(searchText, networkStatus) {
        if (searchText.isBlank()) {
            debouncedSearch = ""
            youTubeViewModel.cancelSearch()
        } else {
            delay(200)
            debouncedSearch = searchText
            if (networkStatus != NetworkStatus.DISCONNECTED) {
                youTubeViewModel.runSearch(searchText, selectedYtFilter)
            } else {
                youTubeViewModel.cancelSearch()
            }
        }
    }

    val filteredSongs by remember {
        derivedStateOf {
            if (debouncedSearch.isBlank()) allSongs
            else allSongs.filter {
                it.title.contains(debouncedSearch, ignoreCase = true) ||
                        it.artist.contains(debouncedSearch, ignoreCase = true) ||
                        it.album.contains(debouncedSearch, ignoreCase = true)
            }
        }
    }

    val artistResults by remember {
        derivedStateOf {
            filteredSongs
                .groupBy { it.artist.split(",", "/", "&", "feat.", "ft.").first().trim() }
                .entries
                .sortedByDescending { it.value.size }
                .take(20)
        }
    }

    val albumResults by remember {
        derivedStateOf {
            filteredSongs
                .groupBy { it.album }
                .entries
                .sortedByDescending { it.value.size }
                .take(20)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(x = 800f, y = -100f),
                        radius = 1200f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp)) {
                Column {
                    Text("Buscar", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    placeholder = { Text("Buscar en Local y YouTube...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            null,
                            tint = if (searchText.isNotEmpty()) accent else Color.White.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        searchText = ""
                                        youTubeViewModel.cancelSearch()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = Color.White, modifier = Modifier.size(14.dp))
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
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "Reconocimiento de voz no disponible", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voz", tint = accent)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        if (searchText.isNotBlank()) {
                            addRecentSearch(searchText)
                            youTubeViewModel.runSearch(searchText, selectedYtFilter)
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF16161E),
                        focusedContainerColor = Color(0xFF1E1E28),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = accent,
                        cursorColor = accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SourceFilter.entries.forEach { source ->
                        val isSelected = source == activeSource
                        val interaction = remember { MutableInteractionSource() }
                        val bg = if (isSelected) accent else Color.White.copy(alpha = 0.08f)
                        val textColor = if (isSelected) Color.Black else Color.White

                        Box(
                            modifier = Modifier
                                .scaleOnPress(interaction, PressScale.chip)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bg)
                                .clickable(interactionSource = interaction, indication = null) { activeSource = source }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                when (source) {
                                    SourceFilter.TODO -> "Todo"
                                    SourceFilter.LOCAL -> "Biblioteca Local"
                                    SourceFilter.YOUTUBE -> "YouTube Music"
                                },
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (networkStatus == NetworkStatus.DISCONNECTED) {
                    OfflineBanner()
                }
            }

            if (searchText.isBlank()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "BÚSQUEDAS RECIENTES",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "Borrar",
                                        color = accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { recentSearches.clear() }
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentSearches.forEach { query ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .clickable {
                                                    searchText = query
                                                    addRecentSearch(query)
                                                    youTubeViewModel.runSearch(query, selectedYtFilter)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.History,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(query, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Borrar",
                                                tint = Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { recentSearches.remove(query) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Biblioteca Local · ${allSongs.size} canciones",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    itemsIndexed(allSongs, key = { _, song -> song.id }) { index, song ->
                        StaggeredItem(index = index) {
                            SearchResultItem(
                                song = song,
                                onClick = { onPlaySong(song) },
                                onEditTags = { onEditTags(song) },
                                onLongPress = { startPreview(song) }
                            )
                        }
                    }
                }
            } else {
                
                when (activeSource) {
                    SourceFilter.TODO -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            
                            if (filteredSongs.isNotEmpty()) {
                                item {
                                    Text(
                                        "BIBLIOTECA LOCAL (${filteredSongs.size})",
                                        color = accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                val songsToShow = if (networkStatus == NetworkStatus.DISCONNECTED) filteredSongs else filteredSongs.take(5)
                                itemsIndexed(songsToShow, key = { _, song -> "local_${song.id}" }) { index, song ->
                                    StaggeredItem(index = index) {
                                        SearchResultItem(
                                            song = song,
                                            onClick = { onPlaySong(song) },
                                            onEditTags = { onEditTags(song) },
                                            onLongPress = { startPreview(song) }
                                        )
                                    }
                                }
                            }

                            if (networkStatus != NetworkStatus.DISCONNECTED) {
                                item {
                                    Text(
                                        "YOUTUBE MUSIC",
                                        color = accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                when (val state = ytState) {
                                    is SearchUiState.Loading -> {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = accent)
                                            }
                                        }
                                    }
                                    is SearchUiState.Success -> {
                                        val ytSongs = state.items.filterIsInstance<SongItem>()
                                        if (ytSongs.isEmpty()) {
                                            item { Text("Sin resultados en YouTube", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) }
                                        } else {
                                            items(ytSongs.take(10), key = { "yt_${it.id}" }) { songItem ->
                                                YouTubeSongCard(
                                                    song = songItem,
                                                    onClick = {
                                                        val localSong = songItem.toSong()
                                                        val localList = ytSongs.map { it.toSong() }
                                                        onPlayYouTubeSong(localSong, localList)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    is SearchUiState.Error -> {
                                        item { Text("Error al buscar en YouTube: ${state.message}", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) }
                                    }
                                    else -> {}
                                }
                            } else if (filteredSongs.isEmpty()) {
                                item {
                                    SearchEmptyState(androidx.compose.ui.res.stringResource(com.fxzmusic.app.R.string.offline_search_empty))
                                }
                            }
                        }
                    }

                    SourceFilter.LOCAL -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LocalCategory.entries.forEach { cat ->
                                    val isSelected = cat == activeLocalCategory
                                    val count = when (cat) {
                                        LocalCategory.CANCIONES -> filteredSongs.size
                                        LocalCategory.ARTISTAS -> artistResults.size
                                        LocalCategory.ALBUMES -> albumResults.size
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) accent else Color.White.copy(alpha = 0.08f))
                                            .clickable { activeLocalCategory = cat }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "${cat.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            when (activeLocalCategory) {
                                LocalCategory.CANCIONES -> {
                                    if (filteredSongs.isEmpty()) SearchEmptyState(searchText)
                                    else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp)
                                        ) {
                                            itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                                                StaggeredItem(index = index) {
                                                    SearchResultItem(
                                                        song = song,
                                                        onClick = { onPlaySong(song) },
                                                        onEditTags = { onEditTags(song) },
                                                        onLongPress = { startPreview(song) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                LocalCategory.ARTISTAS -> {
                                    if (artistResults.isEmpty()) SearchEmptyState(searchText)
                                    else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp)
                                        ) {
                                            itemsIndexed(artistResults, key = { _, entry -> entry.key }) { index, entry ->
                                                StaggeredItem(index = index) {
                                                    ArtistSearchItem(
                                                        name = entry.key,
                                                        songCount = entry.value.size,
                                                        coverUrl = entry.value.firstOrNull { it.coverUrl != null }?.coverUrl,
                                                        albumArt = entry.value.first().albumArt,
                                                        onClick = { onPlaySong(entry.value.first()) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                LocalCategory.ALBUMES -> {
                                    if (albumResults.isEmpty()) SearchEmptyState(searchText)
                                    else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp)
                                        ) {
                                            itemsIndexed(albumResults, key = { _, entry -> entry.key }) { index, entry ->
                                                StaggeredItem(index = index) {
                                                    AlbumSearchItem(
                                                        name = entry.key,
                                                        artist = entry.value.map { it.artist }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "",
                                                        songCount = entry.value.size,
                                                        coverUrl = entry.value.firstOrNull { it.coverUrl != null }?.coverUrl,
                                                        albumArt = entry.value.first().albumArt,
                                                        onClick = { onPlaySong(entry.value.first()) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SourceFilter.YOUTUBE -> {
                        val filterOptions: List<Pair<YouTube.SearchFilter?, String>> = listOf(
                            null to "Todo",
                            YouTube.SearchFilter.FILTER_SONG to "Canciones",
                            YouTube.SearchFilter.FILTER_VIDEO to "Videos",
                            YouTube.SearchFilter.FILTER_ALBUM to "Álbumes",
                            YouTube.SearchFilter.FILTER_ARTIST to "Artistas",
                            YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to "Playlists",
                        )

                        Column(modifier = Modifier.fillMaxSize()) {
                            FilterChipsRow(
                                filters = filterOptions.map { it.second },
                                selectedFilter = filterOptions.first { it.first == selectedYtFilter }.second,
                                onFilterSelected = { label ->
                                    val matched = filterOptions.firstOrNull { it.second == label } ?: return@FilterChipsRow
                                    selectedYtFilter = matched.first
                                    youTubeViewModel.runSearch(searchText, matched.first)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (networkStatus == NetworkStatus.DISCONNECTED) {
                                if (filteredSongs.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        itemsIndexed(filteredSongs, key = { _, song -> "yt_offline_${song.id}" }) { index, song ->
                                            StaggeredItem(index = index) {
                                                SearchResultItem(
                                                    song = song,
                                                    onClick = { onPlaySong(song) },
                                                    onEditTags = { onEditTags(song) },
                                                    onLongPress = { startPreview(song) }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    SearchEmptyState(androidx.compose.ui.res.stringResource(com.fxzmusic.app.R.string.offline_search_empty))
                                }
                            } else {
                                when (val state = ytState) {
                                    is SearchUiState.Loading -> {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = accent)
                                        }
                                    }
                                    is SearchUiState.Success -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(state.items, key = { it.id }) { item ->
                                                when (item) {
                                                    is SongItem -> YouTubeSongCard(
                                                        song = item,
                                                        onClick = {
                                                            val songList = state.items.filterIsInstance<SongItem>().map { it.toSong() }
                                                            onPlayYouTubeSong(item.toSong(), songList)
                                                        }
                                                    )
                                                    is ArtistItem -> YouTubeArtistCard(
                                                        artist = item,
                                                        onClick = { onOpenArtist(item.id) }
                                                    )
                                                    is AlbumItem -> YouTubeAlbumCard(
                                                        album = item,
                                                        onClick = { onOpenAlbum(item.id) }
                                                    )
                                                    is PlaylistItem -> YouTubePlaylistCard(
                                                        playlist = item,
                                                        onClick = { onOpenPlaylist(item.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    is SearchUiState.Error -> {
                                        SearchEmptyState("Error: ${state.message}")
                                    }
                                    else -> SearchEmptyState(searchText)
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
                    song = songForPreview,
                    progress = previewProgress,
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
                color = Color.White.copy(alpha = 0.6f),
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
    val interaction = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scaleOnPress(interaction, PressScale.list)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
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
                    Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$songCount canciones", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
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
    val interaction = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scaleOnPress(interaction, PressScale.list)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$artist · $songCount canciones", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(song: Song, onClick: () -> Unit, onEditTags: () -> Unit = {}, onLongPress: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scaleOnPress(interaction, PressScale.list)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                Text(song.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} · ${song.album}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatTime(song.duration), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar etiquetas", color = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        onClick = { onEditTags(); showMenu = false }
                    )
                }
            }
        }
    }
}
