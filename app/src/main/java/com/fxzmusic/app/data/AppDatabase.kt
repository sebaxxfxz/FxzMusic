package com.fxzmusic.app.data
import com.fxzmusic.app.*

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String = "",
    val songCount: Int,
    val coverColors: String,
    val coverUrl: String?,
    val isSmart: Boolean,
    val smartType: String?
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val position: Int
)

@Entity(tableName = "song_meta")
data class SongMetaEntity(
    @PrimaryKey val songId: String,
    val playCount: Int,
    val lastPlayed: Long,
    val isLiked: Boolean
)

@Entity(tableName = "song_stats")
data class SongStatEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val playCount: Int,
    val totalListenedMs: Long,
    val lastPlayedAt: Long
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String = "",
    val artist: String = "",
    val thumbnail: String = "",
    val duration: Long = 0L,
    val playedAt: Long,
    val listenedMs: Long = 0L,
)

@Entity(tableName = "song_loudness")
data class SongLoudnessEntity(
    @PrimaryKey val songId: String,
    val gainDb: Float,
    val analyzedAt: Long
)

@Entity(tableName = "song_format")
data class SongFormatEntity(
    @PrimaryKey val mediaId: String,
    val itag: Int,
    val mimeType: String,
    val codecs: String?,
    val bitrate: Int,
    val sampleRate: Int,
    val contentLength: Long,
    val playbackUrl: String,
    val streamExpiresAt: Long,
    val savedAt: Long
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isSmart ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongsForPlaylist(playlistId: Long): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylists(playlists: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistSongs(items: List<PlaylistSongEntity>)

    @androidx.room.Update
    suspend fun updatePlaylistSongs(items: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Transaction
    suspend fun replacePlaylistSongs(playlistId: Long, items: List<PlaylistSongEntity>) {
        clearPlaylistSongs(playlistId)
        if (items.isNotEmpty()) upsertPlaylistSongs(items)
    }

    @Query("DELETE FROM playlists WHERE isSmart = 0 AND id NOT IN (:ids)")
    suspend fun deleteNonSmartNotIn(ids: List<Long>)

    @Query("DELETE FROM playlists WHERE isSmart = 0")
    suspend fun deleteAllNonSmart()

    @Query("SELECT ps.* FROM playlist_songs ps WHERE ps.playlistId = :playlistId ORDER BY ps.position ASC")
    suspend fun getPlaylistSongsOrdered(playlistId: Long): List<PlaylistSongEntity>

    @Query("SELECT ps.* FROM playlist_songs ps JOIN playback_history ph ON ps.songId = ph.songId WHERE ps.playlistId = :playlistId ORDER BY ph.playedAt DESC LIMIT 1")
    suspend fun getLastPlayedSongInPlaylist(playlistId: Long): PlaylistSongEntity?

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongsOrderedByPosition(playlistId: Long): List<PlaylistSongEntity>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY (SELECT title FROM song_stats WHERE songId = playlist_songs.songId COLLATE NOCASE) ASC")
    suspend fun getSongsOrderedByTitle(playlistId: Long): List<PlaylistSongEntity>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY (SELECT artist FROM song_stats WHERE songId = playlist_songs.songId COLLATE NOCASE) ASC")
    suspend fun getSongsOrderedByArtist(playlistId: Long): List<PlaylistSongEntity>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY (SELECT lastPlayedAt FROM song_stats WHERE songId = playlist_songs.songId) DESC")
    suspend fun getSongsOrderedByDateAdded(playlistId: Long): List<PlaylistSongEntity>
}

@Dao
interface SongMetaDao {
    @Query("SELECT * FROM song_meta")
    suspend fun getAll(): List<SongMetaEntity>

    @Query("SELECT songId FROM song_meta WHERE isLiked = 1")
    suspend fun getLikedSongIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SongMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SongMetaEntity>)
}

@Dao
interface SongStatsDao {
    @Query("SELECT * FROM song_stats")
    suspend fun getAll(): List<SongStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SongStatEntity>)
}

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 200): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history WHERE songId = :songId ORDER BY playedAt DESC")
    suspend fun getBySong(songId: String): List<PlaybackHistoryEntity>

    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM playback_history WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()
}

@Dao
interface SongLoudnessDao {
    @Query("SELECT * FROM song_loudness WHERE songId = :songId LIMIT 1")
    suspend fun get(songId: String): SongLoudnessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SongLoudnessEntity)
}

@Dao
interface SongFormatDao {
    @Query("SELECT * FROM song_format WHERE mediaId = :mediaId LIMIT 1")
    suspend fun get(mediaId: String): SongFormatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SongFormatEntity)

    @Query("DELETE FROM song_format WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: String)
}

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        SongMetaEntity::class,
        SongStatEntity::class,
        PlaybackHistoryEntity::class,
        SongLoudnessEntity::class,
        SongFormatEntity::class,
        YtLikedSongEntity::class,
        YtAlbumEntity::class,
        YtArtistEntity::class,
        YtPlaylistEntity::class,
        DownloadEntity::class,
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FxzDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songMetaDao(): SongMetaDao
    abstract fun songStatsDao(): SongStatsDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun songLoudnessDao(): SongLoudnessDao
    abstract fun songFormatDao(): SongFormatDao
    abstract fun ytLikedSongDao(): YtLikedSongDao
    abstract fun ytAlbumDao(): YtAlbumDao
    abstract fun ytArtistDao(): YtArtistDao
    abstract fun ytPlaylistDao(): YtPlaylistDao
    abstract fun downloadDao(): DownloadDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile private var INSTANCE: FxzDatabase? = null

        fun getInstance(context: Context): FxzDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FxzDatabase::class.java,
                    "fxz_music.db"
                ).fallbackToDestructiveMigration()
                    .addCallback(object : androidx.room.RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS migration_log (version INTEGER NOT NULL, migrated_at INTEGER NOT NULL)")
                            db.execSQL("INSERT INTO migration_log (version, migrated_at) VALUES (4, ${System.currentTimeMillis()})")
                        }
                    })
                    .build().also { INSTANCE = it }
            }
        }
    }
}

