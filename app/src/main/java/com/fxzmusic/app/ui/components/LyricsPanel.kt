package com.fxzmusic.app.ui.components

import com.fxzmusic.app.util.LyricsImageGenerator
import com.fxzmusic.app.data.LyricsState
import com.fxzmusic.app.data.LyricsStyle
import com.fxzmusic.app.viewmodel.LyricsViewModel
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private fun lerpFloat(a: Float, b: Float, fraction: Float): Float =
    a + (b - a) * fraction.coerceIn(0f, 1f)

private fun lerpFontWeight(a: FontWeight, b: FontWeight, fraction: Float): FontWeight {
    val f = fraction.coerceIn(0f, 1f)
    val w = lerpFloat(a.weight.toFloat(), b.weight.toFloat(), f).toInt().coerceIn(1, 1000)
    return FontWeight(w)
}

private fun alphaForDistance(distance: Int): Float = when (distance) {
    0 -> 1.0f
    1 -> 0.65f
    2 -> 0.45f
    else -> 0.35f
}

private fun scaleForDistance(distance: Int): Float = when (distance) {
    0 -> 1.05f
    1 -> 1.0f
    else -> 0.95f
}

private fun blurDpForDistance(distance: Int): Float = when (distance) {
    0 -> 0f
    1 -> 2f
    2 -> 4f
    else -> 6f
}

private fun Modifier.distanceEffects(distance: Int, apiLevel: Int): Modifier {
    val alpha = alphaForDistance(distance)
    val scale = scaleForDistance(distance)
    val blurDp = blurDpForDistance(distance)
    return if (apiLevel >= 31 && blurDp > 0f) {
        this
            .blur(blurDp.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            }
    } else {
        this.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
        }
    }
}

private fun Modifier.fadingEdge(vertical: Dp): Modifier =
    graphicsLayer(alpha = 0.99f)
        .drawWithContent {
            drawContent()
            val h = vertical.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = h
                ),
                blendMode = BlendMode.DstIn
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - h,
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }

private suspend fun LazyListState.animateScrollToCenter(index: Int) {
    if (index < 0) return
    var targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (targetItem == null) {
        scrollToItem(index)
        targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    if (targetItem != null) {
        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        if (viewportHeight > 0) {
            val itemCenter = targetItem.offset + targetItem.size / 2
            val viewportCenter = viewportHeight / 2
            val delta = itemCenter - viewportCenter
            if (abs(delta) > 2) {
                animateScrollBy(delta.toFloat())
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsPanel(lyricsViewModel: LyricsViewModel, onSeek: (Int) -> Unit = {}, modifier: Modifier = Modifier) {
    val listState    = rememberLazyListState()
    val currentIndex = lyricsViewModel.currentLineIndex
    val context      = LocalContext.current
    val apiLevel     = remember { android.os.Build.VERSION.SDK_INT }
    var selectedIndices by remember { mutableStateOf(emptySet<Int>()) }
    var selectionMode by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToCenter(currentIndex)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val state = lyricsViewModel.lyricsState) {
            is LyricsState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFB5", fontSize = 32.sp); Spacer(modifier = Modifier.height(12.dp))
                Text("Buscando letra...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            is LyricsState.Synced -> Column(modifier = Modifier.fillMaxSize()) {
                LyricsPanelHeader(
                    lyricsViewModel = lyricsViewModel,
                    selectionMode = selectionMode,
                    selectedCount = selectedIndices.size,
                    allResults = lyricsViewModel.allResults,
                    onSelectResult = { lyricsViewModel.selectResult(it) },
                    onExitSelection = { selectionMode = false; selectedIndices = emptySet() },
                    onShareSelection = {
                        val linesText = selectedIndices.map { state.lines.getOrNull(it)?.text }.filterNotNull().joinToString("\n")
                        shareLyricsAsImage(context, "", "", linesText)
                    }
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 80.dp)
                ) {
                    itemsIndexed(state.lines, key = { index, _ -> index }) { index, line ->
                        val isActive  = index == currentIndex
                        val isPast    = index < currentIndex
                        val distance  = abs(index - currentIndex)
                        val isSelected = selectedIndices.contains(index)

                        val activeProgress by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "active_progress"
                        )

                        val lineScale by animateFloatAsState(
                            targetValue = scaleForDistance(distance),
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label = "line_scale"
                        )

                        val distanceModifier = if (isActive) {
                            Modifier.graphicsLayer {
                                this.alpha = 1f
                                this.scaleX = lineScale
                                this.scaleY = lineScale
                            }
                        } else {
                            Modifier.distanceEffects(distance, apiLevel)
                        }

                        val clickModifier = if (selectionMode) {
                            Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (selectedIndices.contains(index)) {
                                            selectedIndices = selectedIndices - index
                                            if (selectedIndices.isEmpty()) selectionMode = false
                                        } else if (selectedIndices.size < 5) {
                                            selectedIndices = selectedIndices + index
                                        }
                                    },
                                    onLongClick = {
                                        if (selectedIndices.contains(index)) {
                                            selectedIndices = selectedIndices - index
                                            if (selectedIndices.isEmpty()) selectionMode = false
                                        } else if (selectedIndices.size < 5) {
                                            selectedIndices = selectedIndices + index
                                        }
                                    }
                                )
                        } else {
                            Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSeek((line.timeMs / 1000).toInt()) },
                                onLongClick = {
                                    selectionMode = true
                                    selectedIndices = setOf(index)
                                }
                            )
                        }

                        val animatedFontSize = lerpFloat(20f, 28f, activeProgress).sp
                        val animatedFontWeight = lerpFontWeight(FontWeight.Normal, FontWeight.Bold, activeProgress)

                        val targetColor = when {
                            isActive -> MaterialTheme.colorScheme.onSurface
                            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val animatedColor by animateColorAsState(
                            targetValue = targetColor,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "line_color"
                        )

                        Text(
                            text = line.text.ifEmpty { "\u266A" },
                            color = animatedColor,
                            fontSize = animatedFontSize,
                            fontWeight = animatedFontWeight,
                            textAlign = TextAlign.Center,
                            lineHeight = animatedFontSize * 1.25f,
                            modifier = distanceModifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .then(clickModifier)
                        )
                    }
                }
            }
            is LyricsState.Plain -> LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                item { Text(state.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 30.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
            }
            is LyricsState.Instrumental -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFB8", fontSize = 40.sp); Spacer(modifier = Modifier.height(12.dp))
                Text("Pista instrumental", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
            is LyricsState.NotFound -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFA4", fontSize = 40.sp); Spacer(modifier = Modifier.height(12.dp))
                Text("Letra no encontrada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val provList = state.providers.joinToString(", ")
                Text("${provList} fallaron", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            else -> {}
        }
    }
}

private fun shareLyricsAsImage(context: android.content.Context, title: String, artist: String, text: String) {
    val bitmap = LyricsImageGenerator.createLyricsImage(
        context = context,
        songTitle = title,
        artistName = artist,
        lyricsText = text,
        coverBitmap = null
    )
    val uri = LyricsImageGenerator.saveBitmapAsFile(context, bitmap, "lyrics_share")
    if (uri != null) LyricsImageGenerator.shareImage(context, uri)
}

@Composable
private fun LyricsPanelHeader(
    lyricsViewModel: LyricsViewModel,
    selectionMode: Boolean = false,
    selectedCount: Int = 0,
    allResults: List<Pair<String, LyricsState>> = emptyList(),
    onSelectResult: (String) -> Unit = {},
    onExitSelection: () -> Unit = {},
    onShareSelection: () -> Unit = {}
) {
    var showOffsetControls by remember { mutableStateOf(false) }
    var showResultsDropdown by remember { mutableStateOf(false) }
    val offset = lyricsViewModel.lyricsOffsetMs
    val offsetLabel = when {
        offset > 0  -> "+${offset}ms"
        offset < 0  -> "${offset}ms"
        else        -> "0ms"
    }
    val styleLabel = when (lyricsViewModel.selectedStyle) {
        LyricsStyle.DEFAULT -> "Normal"
        LyricsStyle.FADE    -> "Fade"
        LyricsStyle.GLOW    -> "Glow"
        LyricsStyle.KARAOKE -> "Karaoke"
    }

    if (selectionMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExitSelection) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Salir de selección",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$selectedCount seleccionadas",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onShareSelection) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Compartir selección",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (showOffsetControls) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { lyricsViewModel.adjustOffset(-250) }) {
                Icon(Icons.Filled.Remove, contentDescription = "Reducir 250ms")
            }
            Text(
                text = offsetLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(onClick = { lyricsViewModel.adjustOffset(250) }) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar 250ms")
            }
            IconButton(onClick = { lyricsViewModel.resetOffset() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Resetear offset")
            }
            IconButton(onClick = { showOffsetControls = false }) {
                Icon(Icons.Filled.Tune, contentDescription = "Ocultar controles de offset")
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { lyricsViewModel.cycleStyle() }) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = "Cambiar estilo de letras ($styleLabel)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                lyricsViewModel.currentProvider?.let { prov ->
                    Text(
                        text = prov,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Box {
                if (allResults.isNotEmpty()) {
                    IconButton(onClick = { showResultsDropdown = true }) {
                        Icon(
                            Icons.Filled.List,
                            contentDescription = "Ver resultados alternativos (${allResults.size})",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showResultsDropdown,
                        onDismissRequest = { showResultsDropdown = false }
                    ) {
                        Text(
                            text = "Fuentes alternativas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        allResults.forEach { (name, _) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    showResultsDropdown = false
                                    onSelectResult(name)
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = { showOffsetControls = true }) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Ajustar offset de letras",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
