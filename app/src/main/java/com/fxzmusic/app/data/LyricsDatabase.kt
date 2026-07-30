package com.fxzmusic.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedLyrics::class], version = 2, exportSchema = false)
abstract class LyricsDatabase : RoomDatabase() {
    abstract fun cachedLyricsDao(): CachedLyricsDao

    companion object {
        @Volatile
        private var INSTANCE: LyricsDatabase? = null

        fun getInstance(context: Context): LyricsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LyricsDatabase::class.java,
                    "fxz_lyrics_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
