package com.fxzmusic.app.service.lyrics

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class BetterLyricsResponse(
    val ttml: String?,
    val lyrics: String?,
    val instrumental: Boolean?,
    val score: Double? = null
)

interface BetterLyricsApi {
    @GET("getLyrics")
    suspend fun getLyrics(
        @Query("s") title: String,
        @Query("a") artist: String?,
        @Query("d") duration: Int?,
        @Query("al") album: String?,
        @Query("videoId") videoId: String? = null
    ): Response<BetterLyricsResponse>

    @GET("kugou/getLyrics")
    suspend fun getKugouLyrics(
        @Query("s") title: String,
        @Query("a") artist: String?,
        @Query("d") duration: Int?,
        @Query("al") album: String?
    ): Response<BetterLyricsResponse>
}

class BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    private val api: BetterLyricsApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://lyrics-api.boidu.dev/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BetterLyricsApi::class.java)
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        return try {
            val durSec = duration?.div(1000)?.takeIf { it > 0 }?.toInt()
            val response = api.getLyrics(
                title = title,
                artist = artist,
                duration = durSec,
                album = album,
                videoId = youtubeVideoId?.takeIf { it.isNotBlank() }
            )

            if (response.isSuccessful) {
                val body = response.body() ?: return LyricsResult.NotFound
                val parsed = parseResponse(body)
                if (parsed != null) return parsed
            }

            val kugouResponse = api.getKugouLyrics(
                title = title,
                artist = artist,
                duration = durSec,
                album = album
            )
            if (kugouResponse.isSuccessful) {
                val kugouBody = kugouResponse.body()
                val parsed = kugouBody?.let { parseResponse(it) }
                if (parsed != null) return parsed
            }

            LyricsResult.NotFound
        } catch (e: Exception) {
            LyricsResult.Error("BetterLyrics failed: ${e.message}")
        }
    }

    private fun parseResponse(body: BetterLyricsResponse): LyricsResult? {
        if (body.instrumental == true) return LyricsResult.Instrumental

        val ttml = body.ttml
        if (!ttml.isNullOrBlank()) {
            val lines = TtmlParser.parse(ttml)
            if (lines.isNotEmpty()) return LyricsResult.Synced(lines)
        }

        val lyrics = body.lyrics
        if (!lyrics.isNullOrBlank()) {
            val lines = LrcParser.parseSyncedLyrics(lyrics)
            if (lines.isNotEmpty()) return LyricsResult.Synced(lines)
            return LyricsResult.Plain(lyrics)
        }

        return null
    }
}
