package com.fxzmusic.app.service.lyrics

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Base64
import java.util.concurrent.TimeUnit

interface KugouApi {
    @GET("search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("pagesize") pageSize: Int = 10
    ): Response<KugouSearchResponse>

    @GET("lyric")
    suspend fun getLyric(
        @Query("keyword") keyword: String,
        @Query("hash") hash: String
    ): Response<KugouLyricResponse>
}

data class KugouSearchResponse(
    val data: KugouSearchData?
)

data class KugouSearchData(
    val lists: List<KugouSearchItem>?
)

data class KugouSearchItem(
    val hash: String?,
    val songname: String?,
    val singername: String?,
    val duration: Int?
)

data class KugouLyricResponse(
    val lrcContent: String?
)

class KugouProvider : LyricsProvider {
    override val name = "KuGou"

    private val api: KugouApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://mobileservice.kugou.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KugouApi::class.java)
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        return try {
            val keyword = buildString {
                append(title)
                if (!artist.isNullOrBlank()) append(" $artist")
            }

            val searchResponse = api.search(keyword)
            if (!searchResponse.isSuccessful) return LyricsResult.NotFound

            val items = searchResponse.body()?.data?.lists
            if (items.isNullOrEmpty()) return LyricsResult.NotFound

            val bestItem = if (duration != null && duration > 0) {
                val durationSec = duration / 1000
                items.firstOrNull { item ->
                    item.duration != null && kotlin.math.abs(item.duration - durationSec) <= 3
                } ?: items.firstOrNull()
            } else {
                items.firstOrNull()
            } ?: return LyricsResult.NotFound
            val hash = bestItem.hash ?: return LyricsResult.NotFound

            val lyricResponse = api.getLyric(keyword, hash)
            if (!lyricResponse.isSuccessful) return LyricsResult.NotFound

            val lrcContent = lyricResponse.body()?.lrcContent
            if (lrcContent.isNullOrBlank()) return LyricsResult.NotFound

            val decodedLrc = try {
                String(Base64.getDecoder().decode(lrcContent))
            } catch (e: Exception) {
                lrcContent
            }

            val lines = LrcParser.parseSyncedLyrics(decodedLrc)
            if (lines.isNotEmpty()) LyricsResult.Synced(lines)
            else LyricsResult.Plain(decodedLrc)
        } catch (e: Exception) {
            LyricsResult.Error("KuGou failed: ${e.message}")
        }
    }
}
