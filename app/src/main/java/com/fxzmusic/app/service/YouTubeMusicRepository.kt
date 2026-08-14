package com.fxzmusic.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.BrowseEndpoint
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.models.WatchEndpoint
import com.fxzmusic.innertube.models.YTItem
import com.fxzmusic.innertube.models.comment.CommentThreadRenderer
import com.fxzmusic.innertube.models.response.GetTranscriptResponse
import com.fxzmusic.innertube.pages.AlbumPage
import com.fxzmusic.innertube.pages.ArtistPage
import com.fxzmusic.innertube.pages.BrowseResult
import com.fxzmusic.innertube.pages.ChartsPage
import com.fxzmusic.innertube.pages.ExplorePage
import com.fxzmusic.innertube.pages.HistoryPage
import com.fxzmusic.innertube.pages.HomePage
import com.fxzmusic.innertube.pages.MoodAndGenres
import com.fxzmusic.innertube.pages.PlaylistPage
import com.fxzmusic.innertube.pages.RelatedPage
import com.fxzmusic.innertube.pages.SearchResult
import com.fxzmusic.innertube.pages.SearchSummaryPage
import com.fxzmusic.ytpipeline.YTPlayerUtils
import com.fxzmusic.ytpipeline.log.AudioQuality
import kotlin.math.abs

class YouTubeMusicRepository {

    suspend fun home(continuation: String? = null, params: String? = null): Result<HomePage> =
        YouTube.home(continuation, params)

    suspend fun browse(browseId: String, params: String? = null): Result<BrowseResult> =
        YouTube.browse(browseId, params)

    suspend fun search(query: String, filter: YouTube.SearchFilter): Result<SearchResult> =
        YouTube.search(query, filter)

    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        YouTube.searchContinuation(continuation)

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
        YouTube.searchSummary(query)

    suspend fun searchSuggestions(query: String): Result<List<String>> =
        YouTube.searchSuggestions(query).map { it.queries }

    suspend fun album(browseId: String): Result<AlbumPage> =
        YouTube.album(browseId, withSongs = true)

    suspend fun albumSongs(playlistId: String): Result<List<SongItem>> =
        YouTube.albumSongs(playlistId)

    suspend fun artist(browseId: String): Result<ArtistPage> =
        YouTube.artist(browseId)

    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        YouTube.playlist(playlistId)

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        YouTube.moodAndGenres()

    suspend fun charts(): Result<ChartsPage> = YouTube.getChartsPage()

    suspend fun explore(): Result<ExplorePage> = YouTube.explore()

    suspend fun history(): Result<HistoryPage> = YouTube.musicHistory()

    data class TranscriptCue(
        val startMs: Long,
        val durationMs: Long,
        val text: String,
    )

    suspend fun transcript(videoId: String): Result<List<TranscriptCue>> =
        YouTube.transcriptCues(videoId).map { cues ->
            cues.map { TranscriptCue(it.startMs, it.durationMs, it.text) }
        }

    suspend fun comments(videoId: String): Result<Pair<List<CommentThreadRenderer>, String?>> =
        YouTube.comments(videoId)

    suspend fun commentContinuation(token: String): Result<Pair<List<CommentThreadRenderer>, String?>> =
        YouTube.commentContinuation(token)

    suspend fun resolveStreamUrl(
        videoId: String,
        context: Context,
        connectivityManager: ConnectivityManager,
        playlistId: String? = null,
        audioQuality: AudioQuality = AudioQuality.OPUS,
        showAudioFallbackToast: Boolean = true,
        knownArtist: String? = null,
        knownTitle: String? = null,
        knownDurationMs: Long? = null,
    ): Result<String> {
        Log.d("YTRepo", "resolveStreamUrl: videoId=$videoId, quality=$audioQuality")
        val playback = YTPlayerUtils.playerResponseForPlayback(
            videoId = videoId,
            playlistId = playlistId,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager,
            context = context,
            knownArtist = knownArtist,
            knownTitle = knownTitle,
            knownDurationMs = knownDurationMs,
            showAudioFallbackToast = showAudioFallbackToast,
        )
        playback.onSuccess { Log.d("YTRepo", "resolveStreamUrl: got streamUrl len=${it.streamUrl.length}, expiresIn=${it.streamExpiresInSeconds}") }
        playback.onFailure { Log.e("YTRepo", "resolveStreamUrl: FAILED: ${it.message}", it) }
        return playback.map { it.streamUrl }
    }

    suspend fun resolvePlaybackData(
        videoId: String,
        context: Context,
        connectivityManager: ConnectivityManager,
        playlistId: String? = null,
        audioQuality: AudioQuality = AudioQuality.OPUS,
        showAudioFallbackToast: Boolean = true,
        knownArtist: String? = null,
        knownTitle: String? = null,
        knownDurationMs: Long? = null,
    ): Result<YTPlayerUtils.PlaybackData> {
        Log.d("YTRepo", "resolvePlaybackData: videoId=$videoId, quality=$audioQuality")
        return YTPlayerUtils.playerResponseForPlayback(
            videoId = videoId,
            playlistId = playlistId,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager,
            context = context,
            knownArtist = knownArtist,
            knownTitle = knownTitle,
            knownDurationMs = knownDurationMs,
            showAudioFallbackToast = showAudioFallbackToast,
        )
    }

    suspend fun getRelated(endpoint: BrowseEndpoint): Result<RelatedPage> =
        YouTube.related(endpoint)

    suspend fun resolveVideoIdForSong(
        title: String,
        artist: String,
        durationMs: Long? = null
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = artist.trim()
        val isArtistValid = cleanedArtist.isNotBlank() && !cleanedArtist.equals("unknown", ignoreCase = true)

        val queries = mutableListOf<Pair<String, YouTube.SearchFilter>>()
        if (isArtistValid) {
            queries.add("$cleanedTitle $cleanedArtist" to YouTube.SearchFilter.FILTER_SONG)
            queries.add("$cleanedTitle $cleanedArtist" to YouTube.SearchFilter.FILTER_VIDEO)
        }
        queries.add(cleanedTitle to YouTube.SearchFilter.FILTER_SONG)
        if (isArtistValid && cleanedTitle != title.trim()) {
            queries.add("${title.trim()} $cleanedArtist" to YouTube.SearchFilter.FILTER_SONG)
        }

        val targetSec = durationMs?.takeIf { it > 0 }?.let { (it / 1000).toInt() }

        for ((query, filter) in queries) {
            val searchResult = search(query, filter).getOrNull() ?: continue
            val songCandidates = searchResult.items.filterIsInstance<SongItem>()
            if (songCandidates.isEmpty()) continue

            if (targetSec != null) {
                val exactMatch = songCandidates.firstOrNull { item ->
                    val dur = item.duration
                    dur != null && abs(dur - targetSec) <= 8
                }
                if (exactMatch != null) {
                    return@runCatching exactMatch.id
                }

                val closeMatch = songCandidates
                    .filter { item ->
                        val dur = item.duration
                        dur != null && abs(dur - targetSec) <= 25
                    }
                    .minByOrNull { item ->
                        val dur = item.duration ?: 0
                        abs(dur - targetSec)
                    }
                if (closeMatch != null) {
                    return@runCatching closeMatch.id
                }
            }

            return@runCatching songCandidates.first().id
        }

        throw NoSuchElementException("Could not resolve videoId for $title - $artist")
    }

    fun flattenSearch(result: SearchResult): List<YTItem> = result.items

    suspend fun getSimilarForSong(videoId: String): Result<RelatedPage> = runCatching {
        val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
        if (nextResult != null) {
            val relatedEndpoint = nextResult.relatedEndpoint
            if (relatedEndpoint != null) {
                val relatedRes = getRelated(relatedEndpoint).getOrNull()
                if (relatedRes != null && (relatedRes.songs.isNotEmpty() || relatedRes.artists.isNotEmpty() || relatedRes.albums.isNotEmpty())) {
                    return@runCatching relatedRes
                }
            }

            val automixSongs = nextResult.items.filter { it.id != videoId }
            if (automixSongs.isNotEmpty()) {
                return@runCatching RelatedPage(
                    songs = automixSongs,
                    albums = emptyList(),
                    artists = emptyList(),
                    playlists = emptyList()
                )
            }
        }

        val radioSongs = getSongRadio(videoId).getOrNull()?.filter { it.id != videoId }
        if (!radioSongs.isNullOrEmpty()) {
            return@runCatching RelatedPage(
                songs = radioSongs,
                albums = emptyList(),
                artists = emptyList(),
                playlists = emptyList()
            )
        }

        throw IllegalStateException("No similar content found for videoId $videoId")
    }

    suspend fun getSongRadio(videoId: String): Result<List<SongItem>> = runCatching {
        val radioNext = YouTube.next(WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")).getOrNull()
        if (radioNext != null && radioNext.items.isNotEmpty()) {
            return@runCatching radioNext.items
        }

        val standardNext = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
        if (standardNext != null && standardNext.items.isNotEmpty()) {
            return@runCatching standardNext.items
        }

        val queueItems = YouTube.queue(playlistId = "RDAMVM$videoId").getOrNull()
        if (!queueItems.isNullOrEmpty()) {
            return@runCatching queueItems
        }

        throw IllegalStateException("Could not load song radio for $videoId")
    }

    companion object {
        val TRACK_NUMBER_PREFIX = Regex("""^\d{1,3}[.\s\-_]+""")
        val CLEAN_REGEX = Regex(
            """\s*(\(|\[|\{).*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit|clip|music\s*video|prod\.?|feat\.?|ft\.?).*?(\)|\]|\})""",
            RegexOption.IGNORE_CASE
        )

        fun cleanTitle(title: String): String {
            var cleaned = title.replace(TRACK_NUMBER_PREFIX, "").trim()
            cleaned = cleaned.replace(CLEAN_REGEX, "").trim()
            return cleaned.ifBlank { title.trim() }
        }

        @Volatile private var instance: YouTubeMusicRepository? = null

        fun get(): YouTubeMusicRepository =
            instance ?: synchronized(this) {
                instance ?: YouTubeMusicRepository().also { instance = it }
            }
    }
}
