package com.example.fxzmusic

data class SongStats(
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val albumArt: List<androidx.compose.ui.graphics.Color>,
    val playCount: Int,
    val totalListenedMs: Long,
    val lastPlayedAt: Long
)

data class ArtistStats(
    val artistName: String,
    val songCount: Int,
    val totalPlayCount: Int,
    val totalListenedMs: Long
)

data class ListeningStats(
    val topSongs: List<SongStats>,
    val topArtists: List<ArtistStats>,
    val totalSongs: Int,
    val totalListenedMs: Long,
    val todayListenedMs: Long,
    val currentStreak: Int
)
