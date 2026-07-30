package com.fxzmusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeLocale(
    val gl: String, 
    val hl: String, 
)
