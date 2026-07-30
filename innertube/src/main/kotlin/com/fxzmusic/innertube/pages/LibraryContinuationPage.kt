package com.fxzmusic.innertube.pages

import com.fxzmusic.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
