package com.fxzmusic.app.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "library_songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Int = -1,
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val inLibraryAt: Long? = null,
    val liked: Boolean = false,
    val filePath: String = "",
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)

@Entity(tableName = "library_artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String? = null
)

@Entity(tableName = "library_albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val year: Int? = null,
    val coverUrl: String? = null
)

@Entity(
    tableName = "song_artist_map",
    primaryKeys = ["songId", "artistId"],
    indices = [Index("artistId")]
)
data class SongArtistMap(
    val songId: String,
    val artistId: String,
    val position: Int
)

@Entity(
    tableName = "song_album_map",
    primaryKeys = ["songId", "albumId"],
    indices = [Index("albumId")]
)
data class SongAlbumMap(
    val songId: String,
    val albumId: String
)

data class UnifiedSong(
    @Embedded val song: SongEntity,
    
    @Relation(
        entity = ArtistEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy = Junction(
            value = SongArtistMap::class,
            parentColumn = "songId",
            entityColumn = "artistId"
        )
    )
    val artists: List<ArtistEntity>,
    
    @Relation(
        entity = AlbumEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy = Junction(
            value = SongAlbumMap::class,
            parentColumn = "songId",
            entityColumn = "albumId"
        )
    )
    val album: AlbumEntity?
) {
    fun toSongDataClass(): Song {
        return Song(
            id = song.id,
            title = song.title,
            artist = if (artists.isNotEmpty()) artists.joinToString(", ") { it.name } else "Unknown Artist",
            album = song.albumName ?: album?.title ?: "Unknown Album",
            duration = song.duration,
            filePath = song.filePath,
            coverUrl = song.thumbnailUrl ?: album?.coverUrl,
            lastPlayed = song.lastPlayedAt,
            playCount = song.playCount,
            isLiked = song.liked,
            isYouTube = !song.isLocal,
            youtubeVideoId = if (!song.isLocal) song.id else null,
            youtubeThumbnailUrl = if (!song.isLocal) song.thumbnailUrl else null
        )
    }
}
