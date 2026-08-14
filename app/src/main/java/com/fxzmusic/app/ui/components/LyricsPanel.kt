package com.fxzmusic.app.ui.components

import com.fxzmusic.app.R
import com.fxzmusic.app.util.LyricsImageGenerator
import com.fxzmusic.app.data.LyricsState
import com.fxzmusic.app.data.LyricsStyle
import com.fxzmusic.app.viewmodel.LyricsViewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

private fun lerpFloat(a: Float, b: Float, fraction: Float): Float =
    a + (b - a) * fraction.coerceIn(0f, 1f)

private fun lerpFontWeight(a: FontWeight, b: FontWeight, fraction: Float): FontWeight {
    val f = fraction.coerceIn(0f, 1f)
    val w = lerpFloat(a.weight.toFloat(), b.weight.toFloat(), f).toInt().coerceIn(1, 1000)
    return FontWeight(w)
}

private fun alphaForDistance(distance: Int, style: LyricsStyle = LyricsStyle.DEFAULT): Float = when (style) {
    LyricsStyle.FADE -> when (distance) {
        0 -> 1.0f
        1 -> 0.35f
        2 -> 0.18f
        else -> 0.08f
    }
    else -> when (distance) {
        0 -> 1.0f
        1 -> 0.65f
        2 -> 0.45f
        else -> 0.35f
    }
}

private fun scaleForDistance(distance: Int): Float = when (distance) {
    0 -> 1.05f
    1 -> 1.0f
    else -> 0.95f
}

private fun Modifier.distanceEffects(distance: Int, style: LyricsStyle = LyricsStyle.DEFAULT): Modifier {
    val alpha = alphaForDistance(distance, style)
    val scale = scaleForDistance(distance)
    return this.graphicsLayer {
        this.alpha = alpha
        this.scaleX = scale
        this.scaleY = scale
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

@Composable
fun LyricsShimmerSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lyrics_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    val lineFractions = remember { listOf(0.60f, 0.85f, 0.70f, 0.90f, 0.50f, 0.80f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        lineFractions.forEach { frac ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(22.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.lyrics_loading),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsPanel(lyricsViewModel: LyricsViewModel, onSeek: (Int) -> Unit = {}, modifier: Modifier = Modifier) {
    val listState    = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var isUserScrolling by remember { mutableStateOf(false) }
    val haptic       = LocalHapticFeedback.current
    val currentIndex = lyricsViewModel.currentLineIndex
    val selectedStyle = lyricsViewModel.selectedStyle
    val context      = LocalContext.current
    var selectedIndices by remember { mutableStateOf(emptySet<Int>()) }
    var selectionMode by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            isUserScrolling = true
        } else if (isUserScrolling) {
            delay(3500L)
            isUserScrolling = false
        }
    }

    LaunchedEffect(currentIndex, isUserScrolling) {
        if (!isUserScrolling && currentIndex >= 0) {
            listState.animateScrollToCenter(currentIndex)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val state = lyricsViewModel.lyricsState) {
            is LyricsState.Loading -> LyricsShimmerSkeleton()
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
                            Modifier.distanceEffects(distance, selectedStyle)
                        }

                        val styleBackgroundModifier = when {
                            isActive && selectedStyle == LyricsStyle.GLOW -> Modifier.background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            isActive && selectedStyle == LyricsStyle.KARAOKE && line.words.isEmpty() -> Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            else -> Modifier
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
                                onClick = {
                                    runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                    isUserScrolling = false
                                    onSeek((line.timeMs / 1000).toInt())
                                },
                                onLongClick = {
                                    selectionMode = true
                                    selectedIndices = setOf(index)
                                }
                            )
                        }

                        val animatedFontSize = lerpFloat(20f, 28f, activeProgress).sp
                        val animatedFontWeight = lerpFontWeight(FontWeight.Normal, FontWeight.Bold, activeProgress)

                        val activeColor = when (selectedStyle) {
                            LyricsStyle.KARAOKE -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        val targetColor = when {
                            isActive -> activeColor
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

                        val textShadow = if (isActive && selectedStyle == LyricsStyle.GLOW) {
                            Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                blurRadius = 24f,
                                offset = Offset.Zero
                            )
                        } else null

                        val currentPos = lyricsViewModel.currentPlaybackPositionMs
                        if (isActive && line.words.isNotEmpty()) {
                            val annotatedString = buildAnnotatedString {
                                line.words.forEachIndexed { wordIdx, word ->
                                    val isWordActive = currentPos >= word.startMs && currentPos <= word.endMs
                                    val isWordPast = currentPos > word.endMs
                                    val wordColor = when {
                                        isWordActive -> MaterialTheme.colorScheme.primary
                                        isWordPast -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    }
                                    val wordWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.SemiBold
                                    withStyle(SpanStyle(color = wordColor, fontWeight = wordWeight)) {
                                        append(word.text)
                                    }
                                    if (wordIdx < line.words.lastIndex && !word.text.endsWith(" ") && !line.words[wordIdx + 1].text.startsWith(" ")) {
                                        append(" ")
                                    }
                                }
                            }
                            Text(
                                text = annotatedString,
                                style = TextStyle(shadow = textShadow),
                                fontSize = animatedFontSize,
                                textAlign = TextAlign.Center,
                                lineHeight = animatedFontSize * 1.25f,
                                modifier = distanceModifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .then(styleBackgroundModifier)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .then(clickModifier)
                            )
                        } else {
                            Text(
                                text = line.text.ifEmpty { "\u266A" },
                                style = TextStyle(shadow = textShadow),
                                color = animatedColor,
                                fontSize = animatedFontSize,
                                fontWeight = if (isActive && selectedStyle == LyricsStyle.KARAOKE) FontWeight.ExtraBold else animatedFontWeight,
                                textAlign = TextAlign.Center,
                                lineHeight = animatedFontSize * 1.25f,
                                modifier = distanceModifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .then(styleBackgroundModifier)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .then(clickModifier)
                            )
                        }
                    }
                }
            }
            is LyricsState.Plain -> LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                item { Text(state.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 30.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
            }
            is LyricsState.Instrumental -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFB8", fontSize = 40.sp); Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.lyrics_instrumental), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
            is LyricsState.NotFound -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDFA4", fontSize = 40.sp); Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.lyrics_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val provList = state.providers.joinToString(", ")
                Text(stringResource(R.string.lyrics_providers_failed, provList), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
        LyricsStyle.DEFAULT -> stringResource(R.string.lyrics_style_normal)
        LyricsStyle.FADE    -> stringResource(R.string.lyrics_style_fade)
        LyricsStyle.GLOW    -> stringResource(R.string.lyrics_style_glow)
        LyricsStyle.KARAOKE -> stringResource(R.string.lyrics_style_karaoke)
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
                    contentDescription = stringResource(R.string.lyrics_exit_selection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.lyrics_selected_count, selectedCount),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onShareSelection) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.lyrics_share_selection),
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
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.lyrics_reduce_offset))
            }
            Text(
                text = offsetLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(onClick = { lyricsViewModel.adjustOffset(250) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.lyrics_increase_offset))
            }
            IconButton(onClick = { lyricsViewModel.resetOffset() }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.lyrics_reset_offset))
            }
            IconButton(onClick = { showOffsetControls = false }) {
                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.lyrics_hide_offset))
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
                        contentDescription = stringResource(R.string.lyrics_change_style, styleLabel),
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
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.lyrics_alt_sources_desc, allResults.size),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showResultsDropdown,
                        onDismissRequest = { showResultsDropdown = false }
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_alt_sources),
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
                        contentDescription = stringResource(R.string.lyrics_adjust_offset),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
