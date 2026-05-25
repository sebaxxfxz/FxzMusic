package com.example.fxzmusic

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.playbackDataStore by preferencesDataStore(name = "playlist_playback_state")

class PlaylistPlaybackDataStore(private val context: Context) {

    private fun songIdKey(playlistId: Long) = stringPreferencesKey("playlist_${playlistId}_song_id")
    private fun progressKey(playlistId: Long) = longPreferencesKey("playlist_${playlistId}_progress_ms")

    suspend fun saveState(playlistId: Long, songId: String, progressMs: Long) {
        context.playbackDataStore.edit { prefs ->
            prefs[songIdKey(playlistId)] = songId
            prefs[progressKey(playlistId)] = progressMs
        }
    }

    suspend fun loadState(playlistId: Long): Pair<String?, Long> {
        val prefs = context.playbackDataStore.data.first()
        val songId = prefs[songIdKey(playlistId)]
        val progress = prefs[progressKey(playlistId)] ?: 0L
        return songId to progress
    }
}


