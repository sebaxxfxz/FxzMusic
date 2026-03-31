package com.example.fxzmusic

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface LrclibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name")  trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name")  albumName: String? = null,
        @Query("duration")    duration: Int? = null
    ): Response<LyricsResponse>

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name")  trackName: String,
        @Query("artist_name") artistName: String
    ): Response<List<LyricsResponse>>

    @GET("api/search")
    suspend fun searchLyricsByTitle(
        @Query("track_name") trackName: String
    ): Response<List<LyricsResponse>>
}

object LyricsModule {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: LrclibApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrclibApi::class.java)
    }
}
