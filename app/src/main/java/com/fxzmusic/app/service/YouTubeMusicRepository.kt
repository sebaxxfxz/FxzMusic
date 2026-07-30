package com.fxzmusic.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.SongItem
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
import com.fxzmusic.innertube.pages.SearchResult
import com.fxzmusic.innertube.pages.SearchSummaryPage
import com.fxzmusic.ytpipeline.YTPlayerUtils
import com.fxzmusic.ytpipeline.log.AudioQuality

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

    fun flattenSearch(result: SearchResult): List<YTItem> = result.items

    companion object {
        
        @Volatile private var instance: YouTubeMusicRepository? = null

        fun get(): YouTubeMusicRepository =
            instance ?: synchronized(this) {
                instance ?: YouTubeMusicRepository().also { instance = it }
            }
    }
}
