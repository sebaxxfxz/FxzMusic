package com.fxzmusic.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String? = null,
    val duration: Int = 0,              
    val bitrate: Int = 0,               
    val fileSize: Long = 0L,            
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long = System.currentTimeMillis(),
)

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY downloaded_at DESC")
    fun getAllFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY downloaded_at DESC")
    suspend fun getAll(): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM downloads WHERE videoId = :videoId")
    suspend fun existsCount(videoId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM downloads")
    suspend fun totalBytes(): Long
}
