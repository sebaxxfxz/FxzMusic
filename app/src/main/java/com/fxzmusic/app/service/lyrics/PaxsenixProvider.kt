package com.fxzmusic.app.service.lyrics

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.abs

interface PaxsenixSearchApi {
    @GET("apple-music/search")
    suspend fun search(@Query("q") query: String): Response<List<PaxsenixSearchResult>>
}

interface PaxsenixLyricsApi {
    @GET("apple-music/lyrics")
    suspend fun getLyrics(@Query("id") id: String): Response<PaxsenixLyricsResponse>
}

data class PaxsenixSearchResult(
    val id: String,
    val name: String? = null,
    val displayName: String? = null,
    val displayArtist: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Long? = null
) {
    val displayTitle: String get() = displayName ?: name ?: ""
    val displayArtistName: String get() = displayArtist ?: artistName ?: ""
}

data class PaxsenixLyricsResponse(
    val ttmlContent: String? = null,
    val elrcMultiPerson: String? = null,
    val elrc: String? = null,
    val plain: String? = null,
    val content: List<PaxsenixLyricsLine>? = null,
    val type: String? = null
)

data class PaxsenixLyricsLine(
    val timestamp: Long = 0L,
    val text: List<PaxsenixLyricsWord>? = null,
    val background: Boolean = false,
    val oppositeTurn: Boolean = false
)

data class PaxsenixLyricsWord(
    val text: String = "",
    val timestamp: Double = 0.0,
    val endtime: Double = 0.0
)

class PaxsenixProvider : LyricsProvider {
    override val name = "Paxsenix"

    private val searchApi: PaxsenixSearchApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://lyrics.paxsenix.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PaxsenixSearchApi::class.java)
    }

    private val lyricsApi: PaxsenixLyricsApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://lyrics.paxsenix.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PaxsenixLyricsApi::class.java)
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

    private fun scoreResults(
        results: List<PaxsenixSearchResult>,
        title: String,
        artist: String?,
        duration: Long?
    ): List<Pair<PaxsenixSearchResult, Double>> {
        val cleanedTitle = cleanTitle(title).lowercase()
        val cleanedArtist = artist?.let { cleanArtist(it).lowercase() } ?: ""
        val durationMs = duration ?: 0L
        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)

        return results.map { result ->
            var score = 0.0

            result.duration?.let { durMs ->
                val diff = abs(durMs - durationMs)
                when {
                    diff <= 2000 -> score += 100
                    diff <= 5000 -> score += 50
                    diff <= 10000 -> score += 10
                    else -> score -= 50
                }
            }

            val resultTitle = result.displayTitle.lowercase()
            when {
                resultTitle == cleanedTitle -> score += 80
                resultTitle.contains(cleanedTitle) || cleanedTitle.contains(resultTitle) -> score += 40
            }

            val resultIsMixed = result.displayTitle.contains("mixed", ignoreCase = true)
            val resultIsRemix = result.displayTitle.contains("remix", ignoreCase = true)
            if (resultIsMixed && !targetIsMixed) score -= 60
            if (resultIsRemix && !targetIsRemix) score -= 40

            if (cleanedArtist.isNotEmpty()) {
                val resultArtist = result.displayArtistName.lowercase()
                when {
                    resultArtist == cleanedArtist -> score += 50
                    resultArtist.contains(cleanedArtist) -> score += 25
                    else -> {
                        val artistWords = cleanedArtist.split(Regex("\\s+")).filter { it.length > 2 }
                        if (artistWords.any { resultArtist.contains(it) }) score += 25
                    }
                }
            }

            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
    }

    private suspend fun parseLyricsResponse(id: String): Pair<String, Boolean> {
        return try {
            val response = lyricsApi.getLyrics(id)
            if (!response.isSuccessful) return "" to false
            val body = response.body() ?: return "" to false

            body.ttmlContent?.let { ttml ->
                val parsed = TtmlParser.parse(ttml)
                if (parsed.isNotEmpty()) {
                    val lrc = parsed.joinToString("\n") { line ->
                        val min = line.timeMs / 60_000
                        val sec = (line.timeMs % 60_000) / 1_000
                        val ms = line.timeMs % 1_000
                        "[${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}.${ms.toString().padStart(3, '0')}]${line.text}"
                    }
                    return lrc to false
                }
            }

            body.elrcMultiPerson?.let { return it to true }
            body.elrc?.let { return it to false }
            body.plain?.let { return it to false }

            if (!body.content.isNullOrEmpty()) {
                val hasWordLevel = body.type == "Syllable"
                if (!hasWordLevel) {
                    val plain = body.content.mapNotNull { line ->
                        line.text?.joinToString(" ") { it.text }?.takeIf { it.isNotBlank() }
                    }.joinToString("\n")
                    return plain to false
                }
                val lrc = buildString {
                    body.content.forEach { line ->
                        val timeMs = line.timestamp
                        val minutes = timeMs / 1000 / 60
                        val seconds = (timeMs / 1000) % 60
                        val centiseconds = (timeMs % 1000) / 10

                        val agent = when {
                            line.background -> "{bg}"
                            line.oppositeTurn -> "{agent:v2}"
                            else -> "{agent:v1}"
                        }

                        val lineText = line.text?.joinToString(" ") { it.text } ?: ""
                        if (lineText.isNotBlank()) {
                            appendLine(java.lang.String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agent, lineText))
                        }
                    }
                }
                return lrc to (body.type == "Syllable")
            }

            "" to false
        } catch (e: Exception) {
            "" to false
        }
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        return try {
            val cleanedTitle = cleanTitle(title)
            val cleanedArtist = artist?.let { cleanArtist(it) } ?: ""

            val searchQueries = buildList {
                add("$cleanedTitle $cleanedArtist")
                add(cleanedTitle)
                if (!album.isNullOrBlank()) {
                    add("$cleanedTitle $cleanedArtist $album")
                }
            }

            var allResults: List<Pair<PaxsenixSearchResult, Double>> = emptyList()
            for (query in searchQueries) {
                if (allResults.isEmpty()) {
                    val searchResponse = searchApi.search(query)
                    if (searchResponse.isSuccessful) {
                        val results = searchResponse.body().orEmpty()
                        if (results.isNotEmpty()) {
                            allResults = scoreResults(results, title, artist, duration)
                        }
                    }
                }
            }

            if (allResults.isEmpty()) return LyricsResult.NotFound

            val candidates = allResults.take(5)
            var plainFallback: LyricsResult? = null

            for ((result, _) in candidates) {
                val (lrc, hasWordTimings) = parseLyricsResponse(result.id)
                if (lrc.isNotEmpty()) {
                    val lines = LrcParser.parseSyncedLyrics(lrc)
                    if (lines.isNotEmpty()) {
                        if (hasWordTimings) return LyricsResult.Synced(lines)
                        if (plainFallback == null) plainFallback = LyricsResult.Synced(lines)
                    } else {
                        if (plainFallback == null) plainFallback = LyricsResult.Plain(lrc)
                    }
                }
            }

            plainFallback ?: LyricsResult.NotFound
        } catch (e: Exception) {
            LyricsResult.Error("Paxsenix failed: ${e.message}")
        }
    }
}
