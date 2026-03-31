package com.example.fxzmusic

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LegacyMigrationManager {

    private const val MIGRATION_PREFS = "migration_flags"
    private const val KEY_ROOM_MIGRATED = "room_migrated_v1"

    suspend fun migrateIfNeeded(context: Context) {
        val app = context.applicationContext
        val flags = app.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (flags.getBoolean(KEY_ROOM_MIGRATED, false)) return

        val db = FxzDatabase.getInstance(app)
        val gson = Gson()

        withContext(Dispatchers.IO) {
            migrateLibraryPrefs(app, db, gson)
            migrateStatsPrefs(app, db, gson)
            flags.edit().putBoolean(KEY_ROOM_MIGRATED, true).apply()
        }
    }

    private suspend fun migrateLibraryPrefs(context: Context, db: FxzDatabase, gson: Gson) {
        val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)
        val playlistJson = prefs.getString("playlists", null)
        val playlists = if (playlistJson.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<Playlist>>() {}.type
                gson.fromJson<List<Playlist>>(playlistJson, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        if (playlists.isNotEmpty()) {
            val entities = playlists.map {
                PlaylistEntity(
                    id = it.id,
                    name = it.name,
                    songCount = it.songCount,
                    coverColors = it.coverColor.encodeColorList(),
                    coverUrl = it.coverUrl,
                    isSmart = it.isSmart,
                    smartType = it.smartType?.name
                )
            }
            db.playlistDao().upsertPlaylists(entities)

            playlists.forEach { playlist ->
                db.playlistDao().clearPlaylistSongs(playlist.id)
                db.playlistDao().upsertPlaylistSongs(
                    playlist.songs.mapIndexed { index, song ->
                        PlaylistSongEntity(
                            playlistId = playlist.id,
                            songId = song.id,
                            position = index
                        )
                    }
                )
            }
        }

        val likedIds = prefs.getStringSet("liked_songs", emptySet()) ?: emptySet()
        if (likedIds.isNotEmpty()) {
            db.songMetaDao().upsertAll(
                likedIds.map { id ->
                    SongMetaEntity(songId = id, playCount = 0, lastPlayed = 0L, isLiked = true)
                }
            )
        }
    }

    private suspend fun migrateStatsPrefs(context: Context, db: FxzDatabase, gson: Gson) {
        val prefs = context.getSharedPreferences("stats_prefs", Context.MODE_PRIVATE)
        val statsJson = prefs.getString("song_stats", null)
        if (statsJson.isNullOrBlank()) return

        val stats = try {
            val type = object : TypeToken<List<SongStats>>() {}.type
            gson.fromJson<List<SongStats>>(statsJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        if (stats.isEmpty()) return

        db.songStatsDao().upsertAll(
            stats.map {
                SongStatEntity(
                    songId = it.songId,
                    title = it.title,
                    artist = it.artist,
                    coverUrl = it.coverUrl,
                    playCount = it.playCount,
                    totalListenedMs = it.totalListenedMs,
                    lastPlayedAt = it.lastPlayedAt
                )
            }
        )

        db.songMetaDao().upsertAll(
            stats.map {
                SongMetaEntity(
                    songId = it.songId,
                    playCount = it.playCount,
                    lastPlayed = it.lastPlayedAt,
                    isLiked = false
                )
            }
        )
    }
}


