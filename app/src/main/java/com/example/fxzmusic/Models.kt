package com.example.fxzmusic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val duration: Int = 180,
    val filePath: String = "",
    val coverUrl: String? = null,
    val albumArt: List<Color> = listOf(Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70)),
    val lastPlayed: Long = 0L,
    val playCount: Int = 0,
    val isLiked: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val coverColor: List<Color>,
    val coverUrl: String? = null,
    val description: String = "",
    val songs: List<Song> = emptyList(),
    val isSmart: Boolean = false,
    val smartType: SmartPlaylistType? = null
)

data class UserProfile(
    val userName: String,
    val userEmail: String,
    val userAvatar: List<Color> = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
    val recentSongs: List<Song>,
    val generatedMixes: List<Playlist>,
    val userPlaylists: List<Playlist>,
    val likedSongsCount: Int,
    val likedSongs: List<Song> = emptyList()
)

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

data class ITunesResponse(val results: List<ITunesTrack>)

data class ITunesTrack(
    val trackName: String?,
    val artistName: String?,
    val collectionName: String?,
    val artworkUrl100: String?
)

enum class SmartPlaylistType {
    MOST_PLAYED, FORGOTTEN_GEMS, NEVER_PLAYED, RECENT_ADDED
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, remainingSeconds)
}
