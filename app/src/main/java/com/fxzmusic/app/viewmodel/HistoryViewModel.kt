package com.fxzmusic.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.data.PlaybackHistoryEntity
import com.fxzmusic.app.data.Song
import com.fxzmusic.app.service.YouTubeMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HistoryItem(
    val id: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnail: String,
    val playedAt: Long,
    val listenedMs: Long,
    val source: String = "local",
)

data class HistoryGroup(
    val label: String,
    val items: List<HistoryItem>,
)

sealed class HistorySource {
    data object Local : HistorySource()
    data object Remote : HistorySource()
    data object Both : HistorySource()
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FxzDatabase.getInstance(application)
    private val repo = YouTubeMusicRepository.get()

    private val _groups = MutableStateFlow<List<HistoryGroup>>(emptyList())
    val groups: StateFlow<List<HistoryGroup>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _source = MutableStateFlow<HistorySource>(HistorySource.Local)
    val source: StateFlow<HistorySource> = _source.asStateFlow()

    private var localItems: List<HistoryItem> = emptyList()
    private var remoteItems: List<HistoryItem> = emptyList()

    init {
        loadLocalHistory()
    }

    fun setSource(source: HistorySource) {
        _source.value = source
        rebuildGroups()
    }

    fun loadLocalHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val entities = database.playbackHistoryDao().getRecent(500)
            localItems = entities.map { entity ->
                HistoryItem(
                    id = entity.id,
                    songId = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    thumbnail = entity.thumbnail,
                    playedAt = entity.playedAt,
                    listenedMs = entity.listenedMs,
                    source = "local",
                )
            }
            rebuildGroups()
            _isLoading.value = false
        }
    }

    fun loadRemoteHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            repo.history()
                .onSuccess { page ->
                    remoteItems = page.sections?.flatMap { section ->
                        section.songs.map { songItem ->
                            HistoryItem(
                                songId = songItem.id,
                                title = songItem.title,
                                artist = songItem.artists.firstOrNull()?.name ?: "",
                                thumbnail = songItem.thumbnail,
                                playedAt = 0L,
                                listenedMs = 0L,
                                source = "youtube",
                            )
                        }
                    } ?: emptyList()
                    rebuildGroups()
                }
                .onFailure {
                    remoteItems = emptyList()
                    rebuildGroups()
                }
            _isLoading.value = false
        }
    }

    fun deleteItem(item: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.source == "local" || item.id > 0) {
                database.playbackHistoryDao().deleteById(item.id)
            } else {
                database.playbackHistoryDao().deleteBySongId(item.songId)
            }
            localItems = localItems.filter { it.id != item.id && it.songId != item.songId }
            rebuildGroups()
        }
    }

    fun clearHistory(source: HistorySource) {
        viewModelScope.launch(Dispatchers.IO) {
            when (source) {
                is HistorySource.Local -> {
                    database.playbackHistoryDao().clearAll()
                    localItems = emptyList()
                }
                is HistorySource.Remote -> remoteItems = emptyList()
                is HistorySource.Both -> {
                    database.playbackHistoryDao().clearAll()
                    localItems = emptyList()
                    remoteItems = emptyList()
                }
            }
            rebuildGroups()
        }
    }

    private fun rebuildGroups() {
        val items = when (_source.value) {
            is HistorySource.Local -> localItems
            is HistorySource.Remote -> remoteItems
            is HistorySource.Both -> {
                val merged = (localItems + remoteItems).distinctBy { it.songId }
                merged.sortedByDescending { it.playedAt }
            }
        }
        _groups.value = groupByDate(items)
    }

    private fun groupByDate(items: List<HistoryItem>): List<HistoryGroup> {
        if (items.isEmpty()) return emptyList()

        val now = Calendar.getInstance()
        val todayStart = now.clone() as Calendar
        todayStart.set(Calendar.HOUR_OF_DAY, 0); todayStart.set(Calendar.MINUTE, 0)
        todayStart.set(Calendar.SECOND, 0); todayStart.set(Calendar.MILLISECOND, 0)
        val todayMs = todayStart.timeInMillis

        val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayMs = yesterdayStart.timeInMillis

        val weekStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        val weekMs = weekStart.timeInMillis

        val monthStart = (todayStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val monthMs = monthStart.timeInMillis

        val grouped = mutableListOf<HistoryGroup>()
        val todayItems = items.filter { it.playedAt >= todayMs }
        val yesterdayItems = items.filter { it.playedAt in yesterdayMs until todayMs }
        val weekItems = items.filter { it.playedAt in weekMs until yesterdayMs }
        val monthItems = items.filter { it.playedAt in monthMs until weekMs }
        val olderItems = items.filter { it.playedAt < monthMs }

        if (todayItems.isNotEmpty()) grouped.add(HistoryGroup("Hoy", todayItems))
        if (yesterdayItems.isNotEmpty()) grouped.add(HistoryGroup("Ayer", yesterdayItems))
        if (weekItems.isNotEmpty()) grouped.add(HistoryGroup("Esta semana", weekItems))
        if (monthItems.isNotEmpty()) grouped.add(HistoryGroup("Mes pasado", monthItems))
        if (olderItems.isNotEmpty()) grouped.add(HistoryGroup("Anterior", olderItems))

        return grouped
    }
}
