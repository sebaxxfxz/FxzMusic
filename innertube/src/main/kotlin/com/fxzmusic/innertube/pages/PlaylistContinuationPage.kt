package com.fxzmusic.innertube.pages

import com.fxzmusic.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
