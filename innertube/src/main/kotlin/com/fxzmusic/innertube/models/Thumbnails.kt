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

fun Thumbnails?.getHighResUrl(): String? {
    return this?.thumbnails?.lastOrNull()?.url
        ?.replace(Regex("w\\d+-h\\d+"), "w1080-h1080")
        ?.replace(Regex("=s\\d+"), "=s1080")
        ?.replace("hqdefault", "maxresdefault")
}
