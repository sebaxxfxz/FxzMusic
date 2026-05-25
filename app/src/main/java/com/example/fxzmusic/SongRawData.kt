package com.example.fxzmusic

data class SongRawData(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val durationMs: Int,
    val dateAdded: Long,
    val coverUrl: String? = null,
    val embeddedCoverUri: String? = null
)
