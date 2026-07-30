package com.fxzmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fxzmusic.app.viewmodel.HistoryGroup
import com.fxzmusic.app.viewmodel.HistoryItem
import com.fxzmusic.app.viewmodel.HistorySource
import com.fxzmusic.app.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onPlaySong: (HistoryItem) -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val source by viewModel.source.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (selectionMode) {
            selectionMode = false
            selectedIds = emptySet()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} seleccionados") },
                    navigationIcon = {
                        IconButton(onClick = { selectionMode = false; selectedIds = emptySet() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            
                            val allIds = groups.flatMap { group -> group.items.map { it.id } }.toSet()
                            selectedIds = allIds
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        IconButton(onClick = {
                            selectedIds.forEach { id ->
                                groups.flatMap { it.items }.find { it.id == id }?.let { viewModel.deleteItem(it) }
                            }
                            selectedIds = emptySet()
                            selectionMode = false
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            } else {
                TopAppBar(
                    title = { Text("Historial", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpiar historial")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = source is HistorySource.Local,
                    onClick = { viewModel.setSource(HistorySource.Local) },
                    label = { Text("Local") },
                    leadingIcon = if (source is HistorySource.Local) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
                FilterChip(
                    selected = source is HistorySource.Remote,
                    onClick = {
                        viewModel.setSource(HistorySource.Remote)
                        viewModel.loadRemoteHistory()
                    },
                    label = { Text("YouTube") },
                    leadingIcon = if (source is HistorySource.Remote) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
                FilterChip(
                    selected = source is HistorySource.Both,
                    onClick = {
                        viewModel.setSource(HistorySource.Both)
                        viewModel.loadRemoteHistory()
                    },
                    label = { Text("Todo") },
                    leadingIcon = if (source is HistorySource.Both) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                groups.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Sin historial",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        groups.forEach { group ->
                            stickyHeader {
                                Text(
                                    text = group.label,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            items(group.items, key = { "${it.id}_${it.songId}" }) { item ->
                                HistoryItemRow(
                                    item = item,
                                    isSelected = selectedIds.contains(item.id),
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedIds = if (selectedIds.contains(item.id)) {
                                                selectedIds - item.id
                                            } else {
                                                selectedIds + item.id
                                            }
                                        } else {
                                            onPlaySong(item)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedIds = setOf(item.id)
                                        }
                                    },
                                    onDelete = { viewModel.deleteItem(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpiar historial") },
            text = { Text("¿Eliminar todo el historial? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory(HistorySource.Both)
                    showClearDialog = false
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItemRow(
    item: HistoryItem,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
            )
        }

        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (item.source == "youtube") {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "YouTube",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(14.dp),
            )
        }

        if (!selectionMode) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
