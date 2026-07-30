package com.fxzmusic.app.service.lyrics

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class SimpMusicApiResponse(
    val type: String = "",
    val data: List<SimpMusicLyricsData>?
)

data class SimpMusicLyricsData(
    val id: String? = null,
    @SerializedName("videoId") val videoId: String? = null,
    @SerializedName("songTitle") val title: String? = null,
    @SerializedName("artistName") val artist: String? = null,
    @SerializedName("albumName") val album: String? = null,
    @SerializedName("durationSeconds") val duration: Int? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("plainLyric") val plainLyrics: String? = null,
    @SerializedName("richSyncLyrics") val richSyncLyrics: String? = null,
    val vote: Int? = null
)

interface SimpMusicApi {
    @GET("v1/{videoId}")
    suspend fun getLyrics(@Path("videoId") videoId: String): Response<SimpMusicApiResponse>
}

class SimpMusicProvider : LyricsProvider {
    override val name = "SimpMusic"

    private val api: SimpMusicApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api-lyrics.simpmusic.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SimpMusicApi::class.java)
    }

    private val fallbackApi: SimpMusicApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://vivi-yt-music-server.onrender.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SimpMusicApi::class.java)
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        return try {
            val videoId = youtubeVideoId?.takeIf { it.isNotBlank() }
                ?: extractVideoId(title, artist)
                ?: return LyricsResult.NotFound

            val result = tryFetchFromApi(api, videoId)
            if (result != null) return result

            tryFetchFromApi(fallbackApi, videoId) ?: LyricsResult.NotFound
        } catch (e: Exception) {
            LyricsResult.Error("SimpMusic failed: ${e.message}")
        }
    }

    private suspend fun tryFetchFromApi(api: SimpMusicApi, videoId: String): LyricsResult? {
        return try {
            val response = api.getLyrics(videoId)
            if (!response.isSuccessful) return null

            val body = response.body()
            if (body == null || body.type != "success" || body.data.isNullOrEmpty()) return null

            val best = body.data.firstOrNull() ?: return null

            val durationSec = best.duration?.toLong()?.takeIf { it > 0 }

            val synced = best.syncedLyrics
            val plain = best.plainLyrics
            val richSync = best.richSyncLyrics

            val lrcToParse = richSync ?: synced

            if (!lrcToParse.isNullOrBlank()) {
                val lines = LrcParser.parseSyncedLyrics(lrcToParse)
                if (lines.isNotEmpty()) return LyricsResult.Synced(lines)
            }

            if (!plain.isNullOrBlank()) return LyricsResult.Plain(plain)

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractVideoId(title: String, artist: String?): String? {
        val combined = if (!artist.isNullOrBlank()) "$title $artist" else title
        val normalized = combined.lowercase().trim()
        if (normalized.isBlank()) return null
        return normalized.replace(Regex("[^a-z0-9 ]"), "").replace(" ", "+")
    }
}
