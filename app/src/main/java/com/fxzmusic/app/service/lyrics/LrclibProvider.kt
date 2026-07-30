package com.fxzmusic.app.service.lyrics

import com.fxzmusic.app.data.LyricsResponse
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.abs

interface LrclibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): Response<LyricsResponse>

    @GET("api/search")
    suspend fun search(
        @Query("track_name") trackName: String? = null,
        @Query("artist_name") artistName: String? = null,
        @Query("q") query: String? = null,
        @Query("album_name") albumName: String? = null
    ): Response<List<LyricsResponse>>
}

class LrclibProvider : LyricsProvider {
    override val name = "LRCLIB"

    private val api: LrclibApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrclibApi::class.java)
    }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a.contains(b) || b.contains(a)) return 0.8
        val maxLen = maxOf(a.length, b.length)
        val distance = levenshteinDistance(a, b)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[m][n]
    }

    private suspend fun searchVariations(
        trackName: String? = null,
        artistName: String? = null,
        albumName: String? = null,
        query: String? = null
    ): List<LyricsResponse> = runCatching {
        api.search(trackName = trackName, artistName = artistName, query = query, albumName = albumName)
    }.getOrNull()?.takeIf { it.isSuccessful }?.body().orEmpty()

    private suspend fun queryLyrics(artist: String, title: String, album: String?): List<LyricsResponse> {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        var results = searchVariations(trackName = cleanedTitle, artistName = cleanedArtist, albumName = album)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        results = searchVariations(trackName = cleanedTitle)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        results = searchVariations(query = "$cleanedArtist $cleanedTitle")
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        results = searchVariations(query = cleanedTitle)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        if (cleanedTitle != title.trim()) {
            results = searchVariations(trackName = title.trim(), artistName = artist.trim())
                .filter { it.syncedLyrics != null || it.plainLyrics != null }
        }

        return results
    }

    private fun bestMatch(tracks: List<LyricsResponse>, title: String, artist: String?, duration: Long?): LyricsResponse? {
        if (tracks.isEmpty()) return null
        val cleanedTitle = cleanTitle(title).lowercase()
        val cleanedArtist = artist?.let { cleanArtist(it).lowercase() } ?: ""

        val scored = tracks.map { track ->
            var score = 0.0

            duration?.let { dur ->
                if (track.duration > 0) {
                    val diff = abs(track.duration - dur / 1000.0)
                    when {
                        diff <= 2.0 -> score += 100
                        diff <= 5.0 -> score += 50
                        diff <= 10.0 -> score += 10
                        else -> score -= 50
                    }
                }
            }

            if (track.syncedLyrics != null) score += 30

            val trackTitle = track.trackName.lowercase()
            when {
                trackTitle == cleanedTitle -> score += 80
                trackTitle.contains(cleanedTitle) || cleanedTitle.contains(trackTitle) -> score += 40
            }

            if (cleanedArtist.isNotEmpty()) {
                val trackArtist = track.artistName.lowercase()
                when {
                    trackArtist == cleanedArtist -> score += 50
                    trackArtist.contains(cleanedArtist) || cleanedArtist.contains(trackArtist) -> score += 25
                }
            }

            track to score
        }.sortedByDescending { it.second }

        return scored.firstOrNull { it.second > 0 }?.first
            ?: scored.firstOrNull()?.first
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        return try {
            val tracks = queryLyrics(artist ?: "", title, album)
            if (tracks.isEmpty()) return LyricsResult.NotFound

            val best = bestMatch(tracks, title, artist, duration) ?: return LyricsResult.NotFound
            if (best.instrumental) return LyricsResult.Instrumental

            best.syncedLyrics?.let { synced ->
                val lines = LrcParser.parseSyncedLyrics(synced)
                if (lines.isNotEmpty()) return LyricsResult.Synced(lines)
            }

            best.plainLyrics?.let { return LyricsResult.Plain(it) }
            LyricsResult.NotFound
        } catch (e: Exception) {
            LyricsResult.Error("LRCLIB failed: ${e.message}")
        }
    }
}
