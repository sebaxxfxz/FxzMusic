package com.fxzmusic.app.ui.screens

import com.fxzmusic.app.data.Song
import com.fxzmusic.app.ui.components.GlassCard
import com.fxzmusic.app.viewmodel.PlaybackSettingsViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FolderBlacklistScreen(
    settingsViewModel: PlaybackSettingsViewModel,
    allSongs: List<Song>,
    onBack: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var searchQuery by remember { mutableStateOf("") }

    data class FolderInfo(
        val path: String,
        val name: String,
        val songCount: Int
    )

    val folders = remember(allSongs, settingsViewModel.blacklistedFolders) {
        val fromLibrary = allSongs.groupBy { song ->
            song.filePath.substringBeforeLast("/")
        }.map { (folderPath, songs) ->
            val parts = folderPath.split("/")
            FolderInfo(
                path = folderPath,
                name = if (parts.size >= 2) parts.last() else "Raíz",
                songCount = songs.size
            )
        }

        val knownPaths = fromLibrary.map { it.path }.toSet()
        val blacklistedExtra = settingsViewModel.blacklistedFolders
            .filter { it !in knownPaths }
            .map { path ->
                val parts = path.split("/")
                FolderInfo(path = path, name = parts.last(), songCount = 0)
            }

        (fromLibrary + blacklistedExtra).sortedBy { it.name.lowercase() }
    }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true) }
    }

    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Carpetas Ocultas",
                    icon = Icons.Filled.FolderOff,
                    accent = accent,
                    onBack = onBack
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FolderSpecial, null, tint = accent, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Excluir de la Biblioteca", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Oculta carpetas no deseadas (audio de WhatsApp, grabaciones, notificaciones) para mantener limpia tu música.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar carpeta...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = accent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                        focusedBorderColor = accent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )
            }

            if (filteredFolders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No se encontraron carpetas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            items(filteredFolders.size, key = { filteredFolders[it].path }) { index ->
                val folder = filteredFolders[index]
                val isBlacklisted = settingsViewModel.blacklistedFolders.contains(folder.path)

                GlassCard(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isBlacklisted) {
                                    settingsViewModel.removeBlacklistedFolder(folder.path)
                                } else {
                                    settingsViewModel.addBlacklistedFolder(folder.path)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isBlacklisted) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    else accent.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isBlacklisted) Icons.Filled.VisibilityOff else Icons.Filled.Folder,
                                contentDescription = null,
                                tint = if (isBlacklisted) MaterialTheme.colorScheme.error else accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    folder.name,
                                    color = if (isBlacklisted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                if (isBlacklisted) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Oculta", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${folder.songCount} temas · ${folder.path}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        Switch(
                            checked = !isBlacklisted,
                            onCheckedChange = {
                                if (isBlacklisted) {
                                    settingsViewModel.removeBlacklistedFolder(folder.path)
                                } else {
                                    settingsViewModel.addBlacklistedFolder(folder.path)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = accent,
                                uncheckedThumbColor = MaterialTheme.colorScheme.error,
                                uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            }
        }
    }
}
