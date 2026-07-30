package com.fxzmusic.app.service.lyrics

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class UnisonResponse(
    val success: Boolean = false,
    val data: UnisonEntry?
)

data class UnisonEntry(
    val id: Long = 0,
    @SerializedName("videoId") val videoId: String? = null,
    val song: String = "",
    val artist: String = "",
    val lyrics: String = "",
    val format: String = "",
    @SerializedName("syncType") val syncType: String = "",
    val score: Double = 0.0,
    @SerializedName("effectiveScore") val effectiveScore: Double = 0.0,
    @SerializedName("voteCount") val voteCount: Int = 0,
    val confidence: String = "",
    val language: String? = null
)

interface UnisonApi {
    @GET("lyrics")
    suspend fun getLyrics(
        @Query("song") song: String,
        @Query("artist") artist: String,
        @Query("album") album: String?,
        @Query("duration") duration: Int?
    ): Response<UnisonResponse>
}

class UnisonProvider : LyricsProvider {
    override val name = "Unison"

    private val api: UnisonApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://unison.boidu.dev/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UnisonApi::class.java)
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        return try {
            val response = api.getLyrics(
                song = title,
                artist = artist ?: title,
                album = album,
                duration = duration?.toInt()?.takeIf { it > 0 }?.let { it / 1000 }
            )
            if (!response.isSuccessful) return LyricsResult.NotFound

            val body = response.body()
            if (body == null || !body.success || body.data == null) return LyricsResult.NotFound

            val entry = body.data
            val lyrics = entry.lyrics
            if (lyrics.isBlank()) return LyricsResult.NotFound

            val trimmed = lyrics.trim()
            val lines = when {
                trimmed.startsWith("<") && trimmed.contains("tt") -> {
                    val parsed = TtmlParser.parse(trimmed)
                    if (parsed.isNotEmpty()) parsed else LrcParser.parseSyncedLyrics(lyrics)
                }
                else -> LrcParser.parseSyncedLyrics(lyrics)
            }
            if (lines.isNotEmpty()) LyricsResult.Synced(lines)
            else LyricsResult.Plain(lyrics)
        } catch (e: Exception) {
            LyricsResult.Error("Unison failed: ${e.message}")
        }
    }
}
