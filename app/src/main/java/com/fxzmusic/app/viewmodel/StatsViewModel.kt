package com.fxzmusic.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

    fun init(context: Context) {
        database = FxzDatabase.getInstance(context.applicationContext)
        viewModelScope.launch {
            LegacyMigrationManager.migrateIfNeeded(context.applicationContext)
            loadStatsFromRoom()
        }
        viewModelScope.launch {
            EventBus.events.collect { event ->
                if (event is UiEvent.SongStatsChanged) {
                    loadStatsFromRoom()
                }
            }
        }
    }

    fun setLikedStatus(songId: String, isLiked: Boolean) {
        likedStatusMap[songId] = isLiked
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

    suspend fun loadStatsFromRoom() {
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
