package com.fxzmusic.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "yt_liked_songs")
data class YtLikedSongEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val artistId: String?,
    val album: String?,
    val albumId: String?,
    val duration: Int?,
    val thumbnail: String?,
    val explicit: Boolean = false,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "yt_liked_albums")
data class YtAlbumEntity(
    @PrimaryKey val browseId: String,
    val playlistId: String?,
    val title: String,
    val artist: String?,
    val year: Int?,
    val thumbnail: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "yt_artists")
data class YtArtistEntity(
    @PrimaryKey val channelId: String,
    val name: String,
    val thumbnail: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "yt_playlists")
data class YtPlaylistEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val author: String?,
    val songCount: Int?,
    val thumbnail: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis(),
)

@Dao
interface YtLikedSongDao {
    @Query("SELECT * FROM yt_liked_songs ORDER BY synced_at DESC")
    suspend fun getAll(): List<YtLikedSongEntity>

    @Query("SELECT videoId FROM yt_liked_songs")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<YtLikedSongEntity>)

    @Query("DELETE FROM yt_liked_songs")
    suspend fun deleteAll()

    @Query("DELETE FROM yt_liked_songs WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)
}

@Dao
interface YtAlbumDao {
    @Query("SELECT * FROM yt_liked_albums ORDER BY synced_at DESC")
    suspend fun getAll(): List<YtAlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<YtAlbumEntity>)

    @Query("DELETE FROM yt_liked_albums")
    suspend fun deleteAll()
}

@Dao
interface YtArtistDao {
    @Query("SELECT * FROM yt_artists ORDER BY synced_at DESC")
    suspend fun getAll(): List<YtArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<YtArtistEntity>)

    @Query("DELETE FROM yt_artists")
    suspend fun deleteAll()
}

@Dao
interface YtPlaylistDao {
    @Query("SELECT * FROM yt_playlists ORDER BY synced_at DESC")
    suspend fun getAll(): List<YtPlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<YtPlaylistEntity>)

    @Query("DELETE FROM yt_playlists")
    suspend fun deleteAll()
}
