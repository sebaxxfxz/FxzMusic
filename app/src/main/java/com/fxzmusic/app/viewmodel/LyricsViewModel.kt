package com.fxzmusic.app.viewmodel
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.service.lyrics.*
import com.fxzmusic.app.util.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import android.app.Application

val Context.lyricsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lyrics_prefs")

class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val videoIdDependentProviders = setOf("YouTube Music", "YouTube Subtitle", "SimpMusic")
    private val lyricsManager = LyricsManager(
        providers = listOf(
            YouTubeLyricsProvider(),
            LrclibProvider(),
            UnisonProvider(),
            KugouProvider(),
            BetterLyricsProvider(),
            SimpMusicProvider(),
            PaxsenixProvider(),
            YouLyPlusProvider(),
            YouTubeSubtitleProvider()
        )
    )

    val allProviders: List<String> get() = lyricsManager.providerNames

    var currentProvider by mutableStateOf<String?>(null)
        private set

    private var db: LyricsDatabase? = null
    private val gson = Gson()

    var lyricsState by mutableStateOf<LyricsState>(LyricsState.Idle)
        private set

    var allResults by mutableStateOf<List<Pair<String, LyricsState>>>(emptyList())
        private set

    var showResultsMenu by mutableStateOf(false)

    var currentLineIndex by mutableIntStateOf(-1)
        private set

    var lyricsOffsetMs by mutableLongStateOf(0L)
        private set

    var selectedStyle by mutableStateOf(LyricsStyle.DEFAULT)
        private set

    init {
        viewModelScope.launch {
            selectedStyle = loadStyle()
            cleanupExpiredCache()
        }
    }

    private suspend fun cleanupExpiredCache() {
        try {
            val db = ensureDb() ?: return
            val cutoff = System.currentTimeMillis() - NEGATIVE_CACHE_TTL_MS
            db.cachedLyricsDao().deleteExpiredNotFound(cutoff)
        } catch (_: Exception) {}
    }

    fun cycleStyle() {
        val next = LyricsStyle.entries[(selectedStyle.ordinal + 1) % LyricsStyle.entries.size]
        selectedStyle = next
        saveStyle(next)
    }

    private suspend fun loadStyle(): LyricsStyle {
        return try {
            val context = getApplication<Application>()
            val dataStore = context.lyricsDataStore
            val prefKey = stringPreferencesKey("lyrics_style")
            val prefs = dataStore.data.first()
            prefs[prefKey]?.let { runCatching { LyricsStyle.valueOf(it) }.getOrNull() } ?: LyricsStyle.DEFAULT
        } catch (e: Exception) {
            LyricsStyle.DEFAULT
        }
    }

    private fun saveStyle(style: LyricsStyle) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val dataStore = context.lyricsDataStore
                val prefKey = stringPreferencesKey("lyrics_style")
                dataStore.edit { prefs ->
                    prefs[prefKey] = style.name
                }
            } catch (e: Exception) {
                android.util.Log.w("LyricsViewModel", "Failed to save style", e)
            }
        }
    }

    private var syncJob: Job? = null
    private val cache = mutableMapOf<String, LyricsState>()
    private var lastSongId: String? = null
    private val songIdToKey = mutableMapOf<String, String>()
    private var currentSongKey: String? = null
    private var currentVideoId: String? = null

    private fun cacheKey(song: Song) = buildString {
        append(song.title.lowercase().trim())
        append('|')
        append(song.artist.lowercase().trim())
        append('|')
        append(song.duration)
    }

    fun loadLyrics(song: Song) {
        if (song.id == lastSongId && lyricsState !is LyricsState.Idle) return
        lastSongId = song.id
        currentVideoId = song.youtubeVideoId?.takeIf { it.isNotBlank() }

        val key = cacheKey(song)
        currentSongKey = key
        val cached = cache[key]
        if (cached != null) { lyricsState = cached; return }

        if (isMeaninglessTitle(song.title)) {
            lyricsState = LyricsState.NotFound(allProviders)
            cache[key] = LyricsState.NotFound(allProviders)
            songIdToKey[song.id] = key
            return
        }

        lyricsState = LyricsState.Loading
        viewModelScope.launch {
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

            val dbResult = withContext(Dispatchers.IO) { loadFromDb(key) }
            if (dbResult != null) {
                cache[key] = dbResult
                songIdToKey[song.id] = key
                lyricsState = dbResult
                return@launch
            }

            val result = withContext(Dispatchers.IO) { fetchLyrics(song) }
            cache[key] = result
            songIdToKey[song.id] = key
            lyricsState = result

            withContext(Dispatchers.IO) { saveToDb(key, result) }
        }
    }

    private fun ensureDb(): LyricsDatabase? {
        if (db == null) {
            try {
                val context = getApplication<Application>()
                db = LyricsDatabase.getInstance(context)
            } catch (e: Exception) {
                android.util.Log.w("LyricsViewModel", "Failed to init Room DB", e)
            }
        }
        return db
    }

    private suspend fun loadFromDb(key: String): LyricsState? {
        return try {
            val database = ensureDb() ?: return null
            val cached = database.cachedLyricsDao().getByKey(key) ?: return null

            if (cached.notFound) {
                val age = System.currentTimeMillis() - cached.timestamp
                if (age < NEGATIVE_CACHE_TTL_MS) return LyricsState.NotFound(allProviders)
                return null
            }

            val type = object : TypeToken<List<LyricsLine>>() {}.type
            val lines: List<LyricsLine> = try {
                gson.fromJson(cached.linesJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            if (lines.isNotEmpty()) LyricsState.Synced(lines)
            else if (cached.linesJson.isNotBlank()) LyricsState.Plain(cached.linesJson)
            else null
        } catch (e: Exception) {
            android.util.Log.w("LyricsViewModel", "Failed to load from DB", e)
            null
        }
    }

    private suspend fun saveToDb(key: String, state: LyricsState) {
        try {
            val database = ensureDb() ?: return
            val (json, notFound) = when (state) {
                is LyricsState.Synced -> gson.toJson(state.lines) to false
                is LyricsState.Plain -> state.text to false
                is LyricsState.NotFound -> "" to true
                else -> return
            }
            database.cachedLyricsDao().insert(
                CachedLyrics(cacheKey = key, linesJson = json, provider = "multi", notFound = notFound)
            )
        } catch (e: Exception) {
            android.util.Log.w("LyricsViewModel", "Failed to save to DB", e)
        }
    }

    private suspend fun fetchLyrics(song: Song): LyricsState {
        val (cleanedTitle, parsedArtist) = parseLocalSongInfo(song)
        val videoId = song.youtubeVideoId?.takeIf { it.isNotBlank() }
            ?: song.id.takeIf { it.length == 11 && it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } }

        val activeProviders = if (videoId == null) {
            lyricsManager.providers.filter { it.name !in videoIdDependentProviders }
        } else {
            lyricsManager.providers
        }

        return try {
            val fetchResult = lyricsManager.fetch(
                title = cleanedTitle,
                artist = parsedArtist,
                album = song.album.takeIf { it != "Unknown Album" },
                duration = song.duration.toLong().takeIf { it > 0 },
                youtubeVideoId = videoId,
                providers = activeProviders
            )

            currentProvider = fetchResult.provider

            val state = when (fetchResult.result) {
                is LyricsResult.Synced -> LyricsState.Synced(fetchResult.result.lines, fetchResult.provider)
                is LyricsResult.Plain -> LyricsState.Plain(fetchResult.result.text, fetchResult.provider)
                is LyricsResult.Instrumental -> LyricsState.Instrumental
                is LyricsResult.NotFound -> {
                    val provs = fetchResult.provider.split(", ").filter { it.isNotBlank() }
                    LyricsState.NotFound(if (provs.isNotEmpty()) provs else allProviders)
                }
                is LyricsResult.Error -> LyricsState.NotFound(allProviders)
            }

            if (state is LyricsState.Synced || state is LyricsState.Plain) {
                withContext(Dispatchers.IO) { trySaveLocalLrc(song.filePath, state) }
            }

            viewModelScope.launch {
                val multiResults = withContext(Dispatchers.IO) {
                    lyricsManager.fetchAll(
                        title = cleanedTitle,
                        artist = parsedArtist,
                        album = song.album.takeIf { it != "Unknown Album" },
                        duration = song.duration.toLong().takeIf { it > 0 },
                        youtubeVideoId = videoId,
                        providers = activeProviders
                    )
                }
                allResults = multiResults.mapNotNull { (name, result) ->
                    if (name == currentProvider) return@mapNotNull null
                    val st = when (result) {
                        is LyricsResult.Synced -> LyricsState.Synced(result.lines, name)
                        is LyricsResult.Plain -> LyricsState.Plain(result.text, name)
                        is LyricsResult.Instrumental -> LyricsState.Instrumental
                        else -> null
                    }
                    st?.let { name to it }
                }
            }

            state
        } catch (e: Exception) {
            android.util.Log.w("LyricsViewModel", "Failed to fetch lyrics for ${song.title}", e)
            LyricsState.NotFound(allProviders)
        }
    }

    fun selectResult(name: String) {
        val result = allResults.firstOrNull { it.first == name } ?: return
        lyricsState = result.second
        currentProvider = name
        showResultsMenu = false
    }

    private fun parseLocalSongInfo(song: Song): Pair<String, String?> {
        var title = song.title.trim()
        var artist = song.artist.takeUnless {
            it.isBlank() || it.equals("unknown", ignoreCase = true) ||
            it.equals("unknown artist", ignoreCase = true) || it.equals("desconocido", ignoreCase = true)
        }

        if (artist == null && (title.contains(" - ") || title.contains(" – "))) {
            val parts = title.split(Regex("""\s*[-–]\s*"""), limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                artist = parts[0].trim()
                title = parts[1].trim()
            }
        }

        title = cleanTitle(title)
        artist = artist?.let { cleanTitle(it) }

        return Pair(title, artist)
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
            .replace(Regex("""\.(mp3|flac|m4a|wav|ogg|aac)$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[\s*(?:320kbps|flac|lossless|official video|audio|hd|4k|lyrics|lyric video|music video)[^\]]*]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(\s*(?:320kbps|flac|lossless|official video|audio|hd|4k|lyrics|lyric video|music video)[^)]*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(feat\.?[^)]*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[feat\.?[^]]*]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*ft\.?\s+[^(-]+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(remix|remaster(?:ed)?|acoustic|live|radio edit|extended|version|edit|mix|rework)[^-]*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\([^)]*(?:remix|remaster|acoustic|live|radio edit|extended|version|edit|mix|rework)[^)]*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[[^\]]*(?:NCS|No Copyright|Copyright Free|Release|Official)[^\]]*]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[|¦].*$"""), "")
            .trim()

    fun invalidate(songId: String) {
        val key = songIdToKey.remove(songId)
        if (key != null) cache.remove(key)
        if (lastSongId == songId) { lastSongId = null; lyricsState = LyricsState.Idle }
    }

    fun startSync(getCurrentPositionMs: () -> Long) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (true) {
                val state = lyricsState
                if (state is LyricsState.Synced) {
                    val posMs = getCurrentPositionMs() + SYNC_OFFSET_MS + lyricsOffsetMs
                    val index = state.lines.indexOfLast { it.timeMs <= posMs }
                    if (index != currentLineIndex) currentLineIndex = index
                } else if (state is LyricsState.NotFound || state is LyricsState.Plain || state is LyricsState.Instrumental) {
                    break
                }
                delay(50)
            }
        }
    }

    fun adjustOffset(deltaMs: Long) {
        lyricsOffsetMs = (lyricsOffsetMs + deltaMs).coerceIn(-5000, 5000)
        currentSongKey?.let { saveOffset(it, lyricsOffsetMs) }
    }

    fun resetOffset() {
        lyricsOffsetMs = 0L
        currentSongKey?.let { saveOffset(it, 0L) }
    }

    private fun saveOffset(key: String, offsetMs: Long) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val dataStore = context.lyricsDataStore
                val prefKey = longPreferencesKey("offset_$key")
                dataStore.edit { prefs ->
                    prefs[prefKey] = offsetMs
                }
            } catch (e: Exception) {
                android.util.Log.w("LyricsViewModel", "Failed to save offset", e)
            }
        }
    }

    private suspend fun loadOffset(key: String): Long {
        return try {
            val context = getApplication<Application>()
            val dataStore = context.lyricsDataStore
            val prefKey = longPreferencesKey("offset_$key")
            val prefs = dataStore.data.first()
            prefs[prefKey] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun loadLyricsWithOffset(song: Song) {
        viewModelScope.launch {
            val key = cacheKey(song)
            lyricsOffsetMs = loadOffset(key)
            loadLyrics(song)
        }
    }

    fun refetchFromProvider(providerName: String) {
        val songKey = currentSongKey ?: return
        val parts = songKey.split("|")
        if (parts.size < 2) return
        val title = parts[0]
        val artistName = parts[1]

        viewModelScope.launch {
            lyricsState = LyricsState.Loading
            val result = withContext(Dispatchers.IO) {
                lyricsManager.fetchFromProvider(
                    providerName = providerName,
                    title = title,
                    artist = artistName,
                    album = null,
                    duration = null,
                    youtubeVideoId = currentVideoId
                )
            }
            currentProvider = result.provider
            when (result.result) {
                is LyricsResult.Synced -> lyricsState = LyricsState.Synced(result.result.lines, result.provider)
                is LyricsResult.Plain -> lyricsState = LyricsState.Plain(result.result.text, result.provider)
                is LyricsResult.Instrumental -> lyricsState = LyricsState.Instrumental
                else -> lyricsState = LyricsState.NotFound(allProviders)
            }
        }
    }

    companion object {
        const val SYNC_OFFSET_MS = 300L
        private const val NEGATIVE_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }

    fun stopSync() { syncJob?.cancel(); currentLineIndex = -1 }

    fun reset() { lyricsState = LyricsState.Idle; currentLineIndex = -1; lastSongId = null; lyricsOffsetMs = 0L; currentSongKey = null; currentVideoId = null; stopSync() }

    private fun tryLoadLocalLrc(filePath: String): LyricsState? {
        if (filePath.isEmpty()) return null
        return try {
            val lrcFile = File(filePath.substringBeforeLast(".") + ".lrc")
            if (!lrcFile.exists()) return null
            val raw = lrcFile.readText()
            val lines = LrcParser.parseSyncedLyrics(raw)
            if (lines.isNotEmpty()) LyricsState.Synced(lines) else LyricsState.Plain(raw)
        } catch (e: Exception) { android.util.Log.w("LyricsViewModel", "Failed to load local LRC file", e); null }
    }

    private fun trySaveLocalLrc(filePath: String, state: LyricsState) {
        if (filePath.isEmpty()) return
        try {
            val lrcFile = File(filePath.substringBeforeLast(".") + ".lrc")
            val content = when (state) {
                is LyricsState.Synced -> LrcParser.toLrc(state.lines)
                is LyricsState.Plain -> state.text
                else -> return
            }
            lrcFile.writeText(content)
        } catch (e: Exception) {
            android.util.Log.w("LyricsViewModel", "Failed to save local LRC file", e)
        }
    }
}
