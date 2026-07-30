package com.fxzmusic.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fxzmusic.app.util.EventBus
import com.fxzmusic.app.util.UiEvent
import com.fxzmusic.app.util.LegacyMigrationManager
import com.fxzmusic.app.data.*

class StatsViewModel : ViewModel() {

    private var database: FxzDatabase? = null

    var stats by mutableStateOf<ListeningStats?>(null)
        private set

    private var songStatsMap = mutableMapOf<String, SongStats>()
    private var likedStatusMap = mutableMapOf<String, Boolean>()
    private var sessionStartMs = 0L
    private var currentTrackId: String? = null

    fun init(context: Context) {
        database = FxzDatabase.getInstance(context.applicationContext)
        viewModelScope.launch {
            LegacyMigrationManager.migrateIfNeeded(context.applicationContext)
            loadStatsFromRoom()
        }
    }

    private var checkpointJob: Job? = null

    fun onSongStarted(song: Song) {
        onSongEnded()
        currentTrackId = song.id
        sessionStartMs = System.currentTimeMillis()

        if (!songStatsMap.containsKey(song.id)) {
            songStatsMap[song.id] = SongStats(
                songId          = song.id,
                title           = song.title,
                artist          = song.artist,
                coverUrl        = song.coverUrl,
                albumArt        = song.albumArt,
                playCount       = 0,
                totalListenedMs = 0L,
                lastPlayedAt    = System.currentTimeMillis()
            )
        }

        val current = songStatsMap[song.id] ?: return
        val updated = current.copy(
            playCount    = current.playCount + 1,
            lastPlayedAt = System.currentTimeMillis()
        )
        songStatsMap[song.id] = updated
        EventBus.tryPublish(UiEvent.SongStatsChanged(song.id, updated.playCount, updated.lastPlayedAt))

        checkpointJob?.cancel()
        checkpointJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                flushSessionProgress()
            }
        }

        saveAndRefresh()
    }

    private fun flushSessionProgress() {
        val trackId = currentTrackId ?: return
        val elapsed = System.currentTimeMillis() - sessionStartMs
        if (elapsed < 5_000) return
        val current = songStatsMap[trackId] ?: return
        songStatsMap[trackId] = current.copy(totalListenedMs = current.totalListenedMs + elapsed)
        sessionStartMs = System.currentTimeMillis()
        viewModelScope.launch {
            persistStatsToRoom()
        }
    }

    fun onSongEnded() {
        checkpointJob?.cancel()
        checkpointJob = null
        val trackId = currentTrackId ?: return
        val elapsed = System.currentTimeMillis() - sessionStartMs
        if (elapsed < 5_000) { currentTrackId = null; sessionStartMs = 0L; return }

        val current = songStatsMap[trackId] ?: return
        songStatsMap[trackId] = current.copy(
            totalListenedMs = current.totalListenedMs + elapsed
        )
        currentTrackId = null
        sessionStartMs = 0L

        viewModelScope.launch(Dispatchers.IO) {
            database?.playbackHistoryDao()?.insert(
                PlaybackHistoryEntity(
                    songId = trackId,
                    title = current.title,
                    artist = current.artist,
                    thumbnail = current.coverUrl ?: "",
                    duration = current.totalListenedMs,
                    playedAt = System.currentTimeMillis(),
                    listenedMs = elapsed,
                )
            )
        }
        saveAndRefresh()
    }

    private fun saveAndRefresh() {
        viewModelScope.launch {
            persistStatsToRoom()
            refreshStats()
        }
    }

    fun setLikedStatus(songId: String, isLiked: Boolean) {
        likedStatusMap[songId] = isLiked
    }

    private suspend fun persistStatsToRoom() {
        val rows = songStatsMap.values.map {
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
        val metaRows = songStatsMap.values.map {
            SongMetaEntity(
                songId = it.songId,
                playCount = it.playCount,
                lastPlayed = it.lastPlayedAt,
                isLiked = likedStatusMap[it.songId] ?: false
            )
        }
        withContext(Dispatchers.IO) {
            database?.songStatsDao()?.upsertAll(rows)
            database?.songMetaDao()?.upsertAll(metaRows)
        }
    }

    private fun refreshStats() {
        val allStats = songStatsMap.values.toList()

        val topSongs = allStats
            .filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(10)

        val topArtists = allStats
            .groupBy { it.artist }
            .map { (artist, songs) ->
                ArtistStats(
                    artistName      = artist,
                    songCount       = songs.size,
                    totalPlayCount  = songs.sumOf { it.playCount },
                    totalListenedMs = songs.sumOf { it.totalListenedMs }
                )
            }
            .sortedByDescending { it.totalPlayCount }
            .take(5)

        val todayStart = getTodayStartMs()
        val todayMs = allStats
            .filter { it.lastPlayedAt >= todayStart }
            .sumOf { it.totalListenedMs }

        stats = ListeningStats(
            topSongs        = topSongs,
            topArtists      = topArtists,
            totalSongs      = allStats.count { it.playCount > 0 },
            totalListenedMs = allStats.sumOf { it.totalListenedMs },
            todayListenedMs = todayMs,
            currentStreak   = calculateStreak()
        )
    }

    private suspend fun loadStatsFromRoom() {
        val rows = withContext(Dispatchers.IO) { database?.songStatsDao()?.getAll().orEmpty() }
        songStatsMap = rows.associate { row ->
            row.songId to SongStats(
                songId = row.songId,
                title = row.title,
                artist = row.artist,
                coverUrl = row.coverUrl,
                albumArt = generateAlbumArt(row.songId),
                playCount = row.playCount,
                totalListenedMs = row.totalListenedMs,
                lastPlayedAt = row.lastPlayedAt
            )
        }.toMutableMap()
        refreshStats()
    }

    private fun generateAlbumArt(id: String): List<androidx.compose.ui.graphics.Color> {
        val hue1 = (id.hashCode().and(0xFF).toFloat() / 255f) * 360f
        val hue2 = (hue1 + 40f) % 360f
        val hue3 = (hue1 + 80f) % 360f
        fun hslToColor(h: Float): androidx.compose.ui.graphics.Color {
            val s = 0.6f
            val l = 0.45f
            val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
            val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
            val m = l - c / 2f
            val (r, g, b) = when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return androidx.compose.ui.graphics.Color(r + m, g + m, b + m)
        }
        return listOf(hslToColor(hue1), hslToColor(hue2), hslToColor(hue3))
    }

    private fun getTodayStartMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun calculateStreak(): Int {
        val allStats = songStatsMap.values.toList()
        if (allStats.isEmpty()) return 0

        var streak = 0
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        for (i in 0..30) {
            val dayStart = cal.timeInMillis
            val dayEnd   = dayStart + 86_400_000L
            val playedThisDay = allStats.any { it.lastPlayedAt in dayStart until dayEnd }
            if (playedThisDay) streak++ else break
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    override fun onCleared() {
        super.onCleared()
        checkpointJob?.cancel()
        val trackId = currentTrackId ?: return
        val elapsed = System.currentTimeMillis() - sessionStartMs
        if (elapsed >= 5_000) {
            val current = songStatsMap[trackId] ?: return
            songStatsMap[trackId] = current.copy(totalListenedMs = current.totalListenedMs + elapsed)
            kotlinx.coroutines.runBlocking { persistStatsToRoom() }
        }
    }

    fun formatDuration(ms: Long): String {
        val hours   = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        return when {
            hours > 0   -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else        -> "< 1m"
        }
    }
}
