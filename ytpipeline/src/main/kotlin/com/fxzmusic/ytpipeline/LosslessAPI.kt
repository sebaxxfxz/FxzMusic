package com.fxzmusic.ytpipeline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class LosslessTrack(
    val song: String,
    val artist: String,
    val url: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val isFlac: Boolean = false,
)

object LosslessAPI {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(queryTitle: String, queryArtist: String): LosslessTrack? =
        withContext(Dispatchers.IO) {
            try {
                val query = buildQuery(queryTitle, queryArtist)
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.deezer.com/search?q=$encoded&limit=5"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FxzMusic/1.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    val body = response.body?.string() ?: return@withContext null
                    val searchResult = json.decodeFromString<DeezerSearchResult>(body)
                    val data = searchResult.data ?: return@withContext null
                    if (data.isEmpty()) return@withContext null

                    val best = pickBest(data, queryTitle, queryArtist) ?: return@withContext null

                    LosslessTrack(
                        song = best.title ?: queryTitle,
                        artist = best.artist?.name ?: queryArtist,
                        url = best.preview,
                        album = best.album?.title,
                        durationMs = best.duration?.toLong()?.times(1000),
                        isFlac = false
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun buildQuery(title: String, artist: String): String {
        val cleanTitle = title
            .replace(Regex("\\(feat\\.?[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[feat\\.?[^]]*]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(remix\\b.*\\)", RegexOption.IGNORE_CASE), "")
            .trim()
        val cleanArtist = artist.replace(" - Topic", "").trim()
        return "$cleanArtist $cleanTitle".trim()
    }

    private fun pickBest(
        tracks: List<DeezerTrack>,
        title: String,
        artist: String
    ): DeezerTrack? {
        val normalisedTitle = title.lowercase().trim()
        val normalisedArtist = artist.lowercase().replace(" - topic", "").trim()

        return tracks
            .filter { t ->
                val tTitle = (t.title ?: "").lowercase()
                val tArtist = (t.artist?.name ?: "").lowercase()
                tTitle.contains(normalisedTitle) || normalisedTitle.contains(tTitle)
            }
            .sortedByDescending { t ->
                var score = 0
                val tArtist = (t.artist?.name ?: "").lowercase()
                if (tArtist.contains(normalisedArtist) || normalisedArtist.contains(tArtist)) score += 10
                val tTitle = (t.title ?: "").lowercase()
                if (tTitle == normalisedTitle) score += 5
                else if (tTitle.contains(normalisedTitle) || normalisedTitle.contains(tTitle)) score += 3
                score
            }
            .firstOrNull() ?: tracks.firstOrNull()
    }
}

@Serializable
data class DeezerSearchResult(
    val data: List<DeezerTrack>? = null,
    val total: Int = 0,
)

@Serializable
data class DeezerTrack(
    val id: Long = 0,
    val title: String? = null,
    val title_short: String? = null,
    val preview: String = "",
    val duration: Int? = null,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null,
    val rank: Long? = null,
)

@Serializable
data class DeezerArtist(
    val id: Long = 0,
    val name: String? = null,
)

@Serializable
data class DeezerAlbum(
    val id: Long = 0,
    val title: String? = null,
)
