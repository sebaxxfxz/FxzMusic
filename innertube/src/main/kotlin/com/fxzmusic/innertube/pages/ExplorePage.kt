package com.fxzmusic.innertube.pages

import androidx.compose.runtime.Immutable
import com.fxzmusic.innertube.models.AlbumItem
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)
