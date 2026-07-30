package com.fxzmusic.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_lyrics")
data class CachedLyrics(
    @PrimaryKey val cacheKey: String,
    val linesJson: String,
    val provider: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notFound: Boolean = false
)
