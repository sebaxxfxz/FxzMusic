package com.fxzmusic.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedLyricsDao {
    @Query("SELECT * FROM cached_lyrics WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): CachedLyrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cached: CachedLyrics)

    @Query("DELETE FROM cached_lyrics WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM cached_lyrics WHERE notFound = 1 AND timestamp < :cutoff")
    suspend fun deleteExpiredNotFound(cutoff: Long)
}
