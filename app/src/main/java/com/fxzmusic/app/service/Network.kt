package com.fxzmusic.app.service
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.util.*

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

interface ITunesApi {
    @GET("search?media=music&entity=song&limit=10")
    suspend fun searchSong(@Query("term") term: String): Response<ITunesResponse>
}

object NetworkModule {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val iTunesApi: ITunesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITunesApi::class.java)
    }

    suspend fun searchCoverUrl(title: String, artist: String, album: String = ""): String? =
        searchBestTrack(title, artist, album)?.artworkUrl100?.replace("100x100bb", "600x600bb")

    suspend fun searchBestTrack(title: String, artist: String, album: String = ""): ITunesTrack? {
        val cleanTitle  = title.trim()
        val cleanArtist = artist.trim()
        val cleanAlbum  = album.trim()
        val isUnknown   = cleanArtist.isBlank() || cleanArtist.contains("unknown", true)
        val queries = when {
            !isUnknown && cleanAlbum.isNotBlank() && !cleanAlbum.contains("unknown", true) ->
                listOf("$cleanArtist - $cleanTitle", "$cleanAlbum $cleanArtist", "$cleanTitle $cleanArtist", "$cleanArtist $cleanTitle", cleanTitle, cleanArtist)
            !isUnknown ->
                listOf("$cleanArtist - $cleanTitle", "$cleanTitle $cleanArtist", "$cleanArtist $cleanTitle", cleanTitle, cleanArtist)
            else ->
                listOf(cleanTitle)
        }
        for (query in queries) {
            var lastError: Exception? = null
            repeat(2) { attempt ->
                try {
                    val response = iTunesApi.searchSong(query)
                    if (!response.isSuccessful) return@repeat
                    val results = response.body()?.results ?: return@repeat
                    val match = findBestMatch(results, cleanTitle, cleanArtist)
                    if (match != null) return match
                } catch (e: Exception) {
                    lastError = e
                    if (attempt == 0) delay(500)
                }
            }
            if (lastError != null) android.util.Log.w("NetworkModule", "All retries failed for query: $query", lastError)
        }
        return null
    }

    fun findBestMatch(
        results: List<ITunesTrack>,
        title: String,
        artist: String
    ): ITunesTrack? {
        if (results.isEmpty()) return null

        val cleanTitle  = title.lowercase().trim()
        val cleanArtist = artist.lowercase().trim()
        val isUnknownArtist = cleanArtist.contains("unknown") || cleanArtist.isBlank()

        val titleMatch = results.firstOrNull { track ->
            val trackTitle = track.trackName?.lowercase()?.trim() ?: ""
            trackTitle.contains(cleanTitle) || cleanTitle.contains(trackTitle)
        }

        if (!isUnknownArtist) {
            val bothMatch = results.firstOrNull { track ->
                val trackTitle  = track.trackName?.lowercase()?.trim() ?: ""
                val trackArtist = track.artistName?.lowercase()?.trim() ?: ""
                val titleOk  = trackTitle.contains(cleanTitle) || cleanTitle.contains(trackTitle)
                val artistOk = trackArtist.contains(cleanArtist) || cleanArtist.contains(trackArtist) ||
                        similarityScore(trackArtist, cleanArtist) > 0.6f
                titleOk && artistOk
            }
            if (bothMatch != null) return bothMatch

            val artistMatch = results.firstOrNull { track ->
                val trackArtist = track.artistName?.lowercase()?.trim() ?: ""
                trackArtist.contains(cleanArtist) || cleanArtist.contains(trackArtist) ||
                        similarityScore(trackArtist, cleanArtist) > 0.6f
            }
            if (artistMatch != null) return artistMatch
        }

        return titleMatch ?: results.firstOrNull()
    }

    private fun similarityScore(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val longer  = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        if (longer.isEmpty()) return 1f
        val matchingChars = shorter.toSet().count { ch -> longer.contains(ch) }
        return matchingChars.toFloat() / longer.length.toFloat()
    }
}
