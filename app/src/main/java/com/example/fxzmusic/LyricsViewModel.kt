package com.example.fxzmusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.fxzmusic.MetadataUtils

class LyricsViewModel : ViewModel() {

    var lyricsState by mutableStateOf<LyricsState>(LyricsState.Idle)
        private set

    var currentLineIndex by mutableIntStateOf(-1)
        private set

    private var syncJob: Job? = null
    private val cache = mutableMapOf<String, LyricsState>()
    private var lastSongId: String? = null
    private val songIdToKey = mutableMapOf<String, String>()

    private fun cacheKey(song: Song) = "${song.title.lowercase().trim()}|${song.artist.lowercase().trim()}"

    fun loadLyrics(song: Song) {
        if (song.id == lastSongId && lyricsState !is LyricsState.Idle) return
        lastSongId = song.id

        val key    = cacheKey(song)
        val cached = cache[key]
        if (cached != null) { lyricsState = cached; return }

        if (isMeaninglessTitle(song.title)) {
            lyricsState = LyricsState.NotFound
            cache[key] = LyricsState.NotFound
            songIdToKey[song.id] = key
            return
        }

        lyricsState = LyricsState.Loading
        viewModelScope.launch {
            // First check for embedded lyrics in the audio file metadata
            val embeddedLyrics = withContext(Dispatchers.IO) { MetadataUtils.getEmbeddedLyrics(song) }
            if (embeddedLyrics != null) {
                val state = LyricsState.Plain(embeddedLyrics)
                cache[key] = state
                songIdToKey[song.id] = key
                lyricsState = state
                return@launch
            }

            val localLrc = withContext(Dispatchers.IO) { tryLoadLocalLrc(song.filePath) }
            if (localLrc != null) { cache[key] = localLrc; songIdToKey[song.id] = key; lyricsState = localLrc; return@launch }
            val result = withContext(Dispatchers.IO) { fetchLyrics(song) }
            cache[key]         = result
            songIdToKey[song.id] = key
            lyricsState        = result
        }
    }

    private suspend fun fetchLyrics(song: Song): LyricsState {
        val cleanedTitle = cleanTitle(song.title)
        val artistName   = song.artist.takeUnless { it.isBlank() || it.contains("unknown", ignoreCase = true) }
        return try {
            val response = LyricsModule.api.getLyrics(
                trackName  = cleanedTitle,
                artistName = artistName ?: cleanedTitle,
                albumName  = song.album.takeIf { it != "Unknown Album" },
                duration   = song.duration.takeIf { it > 0 }
            )
            when {
                !response.isSuccessful               -> trySearch(song, cleanedTitle)
                response.body()?.instrumental == true -> LyricsState.Instrumental
                response.body()?.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(response.body()!!.syncedLyrics!!)
                    if (lines.isNotEmpty()) LyricsState.Synced(lines)
                    else response.body()?.plainLyrics?.let { LyricsState.Plain(it) } ?: trySearch(song, cleanedTitle)
                }
                response.body()?.plainLyrics != null  -> LyricsState.Plain(response.body()!!.plainLyrics!!)
                else                                  -> trySearch(song, cleanedTitle)
            }
        } catch (_: Exception) {
            trySearch(song, cleanedTitle)
        }
    }

    private fun isMeaninglessTitle(title: String): Boolean {
        val t = title.trim()
        return t.isBlank() || t.equals("unknown", ignoreCase = true) ||
                t.startsWith("AUD-", ignoreCase = true) ||
                t.startsWith("VID-", ignoreCase = true) ||
                t.startsWith("PTT-", ignoreCase = true) || t.length < 2
    }

    private fun cleanTitle(title: String): String =
        title
            .replace(Regex("""\s*\(feat\.?[^)]*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[feat\.?[^]]*]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*ft\.?\s+[^(-]+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(remix|remaster(?:ed)?|acoustic|live|radio edit|extended|version)[^-]*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\([^)]*(?:remix|remaster)[^)]*\)""", RegexOption.IGNORE_CASE), "")
            .trim()

    fun invalidate(songId: String) {
        val key = songIdToKey.remove(songId)
        if (key != null) cache.remove(key)
        if (lastSongId == songId) { lastSongId = null; lyricsState = LyricsState.Idle }
    }

    private suspend fun trySearch(song: Song, cleanedTitle: String): LyricsState {
        val artistName = song.artist.takeUnless { it.isBlank() || it.contains("unknown", ignoreCase = true) }
        val queries = buildList {
            if (artistName != null) add(Pair(cleanedTitle, artistName))
            if (cleanedTitle != song.title && artistName != null) add(Pair(song.title, artistName))
            add(Pair(cleanedTitle, null))
            if (artistName != null) add(Pair(artistName, null))
        }
        for ((trackName, artist) in queries) {
            try {
                val response = if (artist != null) {
                    LyricsModule.api.searchLyrics(trackName, artist)
                } else {
                    LyricsModule.api.searchLyricsByTitle(trackName)
                }
                val results = response.body() ?: continue
                val best = findBestLyricsMatch(results, cleanedTitle, song.artist) ?: continue
                val state: LyricsState? = when {
                    best.instrumental         -> LyricsState.Instrumental
                    best.syncedLyrics != null -> {
                        val lines = parseSyncedLyrics(best.syncedLyrics!!)
                        if (lines.isNotEmpty()) LyricsState.Synced(lines) else null
                    }
                    best.plainLyrics != null  -> LyricsState.Plain(best.plainLyrics!!)
                    else                      -> null
                }
                if (state != null) return state
            } catch (_: Exception) { continue }
        }
        return LyricsState.NotFound
    }

    private fun findBestLyricsMatch(
        results: List<LyricsResponse>,
        title: String,
        artist: String
    ): LyricsResponse? {
        if (results.isEmpty()) return null
        val cleanT = title.lowercase().trim()
        val cleanA = artist.lowercase().trim()
        val exact = results.firstOrNull { r ->
            r.trackName.lowercase().trim() == cleanT &&
                    (cleanA.isBlank() || r.artistName.lowercase().contains(cleanA))
        }
        if (exact != null) return exact
        return results.firstOrNull { r ->
            val t = r.trackName.lowercase()
            val a = r.artistName.lowercase()
            (t.contains(cleanT) || cleanT.contains(t)) &&
                    (cleanA.isBlank() || a.contains(cleanA) || cleanA.contains(a))
        } ?: results.firstOrNull()
    }

    fun startSync(getCurrentPositionMs: () -> Long) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (true) {
                val state = lyricsState
                if (state is LyricsState.Synced) {
                    val posMs = getCurrentPositionMs() + SYNC_OFFSET_MS
                    val index = state.lines.indexOfLast { it.timeMs <= posMs }
                    if (index != currentLineIndex) currentLineIndex = index
                } else if (state is LyricsState.NotFound || state is LyricsState.Plain || state is LyricsState.Instrumental) {
                    break
                }
                delay(50)
            }
        }
    }

    companion object { const val SYNC_OFFSET_MS = 300L }

    fun stopSync() { syncJob?.cancel(); currentLineIndex = -1 }

    fun reset() { lyricsState = LyricsState.Idle; currentLineIndex = -1; lastSongId = null; stopSync() }

    private fun tryLoadLocalLrc(filePath: String): LyricsState? {
        if (filePath.isEmpty()) return null
        return try {
            val lrcFile = File(filePath.substringBeforeLast(".") + ".lrc")
            if (!lrcFile.exists()) return null
            val raw   = lrcFile.readText()
            val lines = parseSyncedLyrics(raw)
            if (lines.isNotEmpty()) LyricsState.Synced(lines) else LyricsState.Plain(raw)
        } catch (_: Exception) { null }
    }
}
