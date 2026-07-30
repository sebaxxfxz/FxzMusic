package com.fxzmusic.app.ui.components

import com.fxzmusic.app.util.buildCoverRequest
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.util.toSong
import com.fxzmusic.innertube.models.SongItem
import kotlin.math.abs

object YouTubeHomeSections {

    fun isQuickPicks(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("quick picks") ||
            t.contains("recientes") ||
            t.contains("for you") ||
            t.contains("para ti") ||
            t.contains("tu mix") ||
            t.contains("discover mix")
    }

    fun isListenAgain(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("listen again") ||
            t.contains("keep listening") ||
            t.contains("volver a escuchar") ||
            t.contains("sigue escuchando") ||
            t.contains("replay mix")
    }

    fun isRecommendedPlaylists(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("recommended playlists") ||
            t.contains("playlists for you") ||
            t.contains("playlist recomendadas") ||
            t.contains("creadas para ti") ||
            t.contains("made for you")
    }

    fun isForgottenFavorites(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("forgotten") ||
            t.contains("favorites") ||
            t.contains("favoritos") ||
            t.contains("forgotten favorites") ||
            t.contains("forgotten gems")
    }

    fun isNewReleases(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("new releases") ||
            t.contains("new album") ||
            t.contains("nuevos lanzamientos") ||
            t.contains("estrenos")
    }

    fun isCommunityPlaylists(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("community playlist") ||
            t.contains("playlists de la comunidad") ||
            t.contains("playlists comunitarias") ||
            t.contains("user playlists") ||
            t.contains("created by listeners")
    }

    fun isRecommendedArtists(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("recommended artist") ||
            t.contains("artistas recomendados") ||
            t.contains("similar artist") ||
            t.contains("artista similar") ||
            t.contains("you might like")
    }
}

@Composable
fun YouTubeQuickPicksCarousel(
    songs: List<SongItem>,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) return

    val state = rememberLazyListState()

    val activeIndex by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                val itemCenter = info.offset + info.size / 2
                abs(itemCenter - center)
            }?.index ?: 0
        }
    }

    LazyRow(
        state = state,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 60.dp),
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id },
        ) { index, song ->
            YouTubeQuickPickCard(
                song = song,
                isActive = index == activeIndex,
                onClick = {
                    val all = songs.map { it.toSong() }
                    onPlaySong(song.toSong(), all)
                },
            )
        }
    }
}

@Composable
private fun YouTubeQuickPickCard(
    song: SongItem,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.85f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "qp_scale",
    )
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .width(240.dp)
            .height(280.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (isActive) 1f else 0.55f }
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        AsyncImage(
            model = buildCoverRequest(LocalContext.current, song.thumbnail),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artists.joinToString(", ") { it.name },
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (isPressed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            )
        }
    }
}

@Composable
fun YouTubeMoodGenresGrid(
    items: List<com.fxzmusic.innertube.pages.MoodAndGenres.Item>,
    onMoodClick: (com.fxzmusic.innertube.models.BrowseEndpoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = (items.size + 1) / 2
    val rowsToShow = rows.coerceAtMost(3)
    if (rowsToShow == 0 || items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var i = 0
        while (i < items.size && i < rowsToShow * 2) {
            val left = items[i]
            val right = items.getOrNull(i + 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoodChip(item = left, onClick = { onMoodClick(left.endpoint) })
                if (right != null) {
                    MoodChip(item = right, onClick = { onMoodClick(right.endpoint) })
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            i += 2
        }
    }
}

@Composable
private fun RowScope.MoodChip(
    item: com.fxzmusic.innertube.pages.MoodAndGenres.Item,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val color = remember(item.stripeColor, primary) {
        try {
            Color(item.stripeColor)
        } catch (_: Throwable) {
            primary
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "mood_chip_scale",
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            item.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, end = 8.dp),
        )
    }
}
