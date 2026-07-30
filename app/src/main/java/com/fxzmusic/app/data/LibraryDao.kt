package com.fxzmusic.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Transaction
    @Query("SELECT * FROM library_songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllUnifiedSongsFlow(): Flow<List<UnifiedSong>>

    @Transaction
    @Query("SELECT * FROM library_songs")
    suspend fun getAllUnifiedSongs(): List<UnifiedSong>

    @Transaction
    @Query("SELECT * FROM library_songs WHERE id = :songId LIMIT 1")
    suspend fun getUnifiedSong(songId: String): UnifiedSong?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtist(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongArtistMap(map: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongAlbumMap(map: SongAlbumMap)

    @Query("DELETE FROM library_songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)

    @Query("DELETE FROM library_songs WHERE isLocal = 1")
    suspend fun deleteAllLocalSongs()
}
