package com.fxzmusic.app.ui.components

import com.fxzmusic.app.data.Song
import com.fxzmusic.app.data.formatTime
import com.fxzmusic.app.util.buildCoverRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun QueuePanel(
    queue: List<Song>,
    currentSong: Song,
    onPlaySong: (Song) -> Unit,
    onRemoveFromQueue: ((Song) -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onMoveInQueue: ((from: Int, to: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 56.dp.toPx() }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var draggedOffset by remember { mutableStateOf(0f) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }

    val upcomingSongs = remember(queue, currentSong) {
        val currentIndex = queue.indexOfFirst { it.id == currentSong.id }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            queue.subList(currentIndex + 1, queue.size)
        } else if (currentIndex == -1) {
            queue
        } else {
            emptyList()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "REPRODUCIENDO AHORA",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(currentSong.albumArt))
                    ) {
                        if (currentSong.coverUrl != null) {
                            AsyncImage(
                                model = buildCoverRequest(LocalContext.current, currentSong.coverUrl, maxSize = 1024),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AudioVisualizerBars(
                                isPlaying = true,
                                barCount = 3,
                                accent = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentSong.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            currentSong.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        formatTime(currentSong.duration),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "A continuación",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (upcomingSongs.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${upcomingSongs.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (queue.isNotEmpty() && onClearQueue != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClearQueue() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Limpiar cola",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Limpiar",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (upcomingSongs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "No hay más canciones en la cola",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            
            itemsIndexed(upcomingSongs, key = { _, song -> song.id }) { index, song ->
                val isDragged = index == draggedIndex
                val interaction = remember { MutableInteractionSource() }
                val isPressed by interaction.collectIsPressedAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isDragged) Modifier.shadow(16.dp, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .graphicsLayer {
                            if (isDragged) {
                                translationY = draggedOffset
                                scaleX = 1.04f
                                scaleY = 1.04f
                            }
                        }
                        .scale(if (isPressed && !isDragged) 0.97f else 1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDragged) accent.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.08f)
                        )
                        .clickable(interactionSource = interaction, indication = null) {
                            onPlaySong(song)
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    if (onMoveInQueue != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(index, upcomingSongs.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedIndex = index
                                            draggedOffset = 0f
                                            dragTargetIndex = index
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedOffset += dragAmount.y
                                            val currentTarget = (index + (draggedOffset / itemHeightPx).toInt())
                                                .coerceIn(0, upcomingSongs.lastIndex)
                                            if (currentTarget != dragTargetIndex) {
                                                dragTargetIndex = currentTarget
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggedIndex != -1 && dragTargetIndex != -1 && draggedIndex != dragTargetIndex) {
                                                val fullFromIndex = queue.indexOfFirst { it.id == upcomingSongs[draggedIndex].id }
                                                val fullToIndex = queue.indexOfFirst { it.id == upcomingSongs[dragTargetIndex].id }
                                                if (fullFromIndex != -1 && fullToIndex != -1) {
                                                    onMoveInQueue(fullFromIndex, fullToIndex)
                                                }
                                            }
                                            draggedIndex = -1
                                            draggedOffset = 0f
                                            dragTargetIndex = -1
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            draggedOffset = 0f
                                            dragTargetIndex = -1
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = "Reordenar",
                                tint = if (isDragged) accent else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        "${index + 1}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(song.albumArt))
                    ) {
                        if (song.coverUrl != null) {
                            AsyncImage(
                                model = buildCoverRequest(LocalContext.current, song.coverUrl, maxSize = 1024),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        formatTime(song.duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )

                    if (onRemoveFromQueue != null) {
                        IconButton(
                            onClick = { onRemoveFromQueue(song) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Eliminar de la cola",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
