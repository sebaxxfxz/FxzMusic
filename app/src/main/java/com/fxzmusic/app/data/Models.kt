package com.fxzmusic.app.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
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
    val dateAdded: Long = System.currentTimeMillis(),
    
    val isYouTube: Boolean = false,
    val youtubeVideoId: String? = null,
    val youtubeThumbnailUrl: String? = null,
) {
    
    @Transient
    var youtubeStreamUrl: String? = null
        internal set
}

@Stable
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

@Stable
data class AlbumGroup(
    val name: String,
    val songs: List<Song>,
    val coverUrl: String? = null,
    val coverColor: List<Color> = listOf(Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70))
)

@Stable
data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
    val coverUrl: String? = null,
    val coverColor: List<Color> = listOf(Color(0xFF667eea), Color(0xFF764ba2))
)

@Stable
data class FolderGroup(
    val name: String,
    val songs: List<Song>
)

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, remainingSeconds)
}

@Immutable
data class AudioMetadata(
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val durationMs: Long? = null,
    val mimeType: String? = null,
    val codecString: String? = null,
    val fileSize: Long? = null,
    val filePath: String? = null
) {
    val bitrateKbps: String get() = bitrate?.let { "${it / 1000} kbps" } ?: "N/A"
    val sampleRateFormatted: String get() = sampleRate?.let { "${it / 1000.0} kHz" } ?: "N/A"
    val channelsText: String get() = when (channels) {
        1 -> "Mono"
        2 -> "Stereo"
        else -> channels?.let { "${it}ch" } ?: "N/A"
    }
    val durationFormatted: String get() = durationMs?.let {
        val totalSec = it / 1000
        String.format(java.util.Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
    } ?: "N/A"
    val formatDisplayName: String get() = when {
        mimeType?.contains("mpeg") == true -> "MP3"
        mimeType?.contains("flac") == true -> "FLAC"
        mimeType?.contains("opus") == true -> "Opus"
        mimeType?.contains("vorbis") == true -> "OGG Vorbis"
        mimeType?.contains("aac") == true || mimeType?.contains("mp4a") == true -> "AAC"
        mimeType?.contains("wav") == true -> "WAV"
        else -> mimeType?.substringAfterLast('/')?.uppercase() ?: "N/A"
    }
    val fileSizeFormatted: String get() = fileSize?.let { size ->
        when {
            size < 1024 -> "$size B"
            size < 1_048_576 -> String.format(java.util.Locale.US, "%.1f KB", size / 1024.0)
            size < 1_073_741_824 -> String.format(java.util.Locale.US, "%.1f MB", size / 1_048_576.0)
            else -> String.format(java.util.Locale.US, "%.2f GB", size / 1_073_741_824.0)
        }
    } ?: "N/A"
}
