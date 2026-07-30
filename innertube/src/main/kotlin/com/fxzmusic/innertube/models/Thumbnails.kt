package com.fxzmusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail>,
)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int?,
    val height: Int?,
)

fun String.toHighResThumbnailUrl(): String {
    if (isBlank()) return this
    return this
}

fun Thumbnails?.getHighResUrl(): String? {
    return this?.thumbnails?.lastOrNull()?.url
}
