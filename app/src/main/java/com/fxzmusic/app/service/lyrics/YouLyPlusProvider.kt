package com.fxzmusic.app.service.lyrics

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class YouLyPlusResponse(
    val id: Int? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    val lyrics: List<YouLyPlusLyricsItem>? = null,
    val type: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
)

data class YouLyPlusLyricsItem(
    val text: String? = null,
    val time: Long? = null,
    val duration: Long? = null,
    val syllabus: List<YouLyPlusSyllable>? = null,
)

data class YouLyPlusSyllable(
    val text: String? = null,
    val time: Long? = null,
    val duration: Long? = null,
    val isBackground: Boolean? = null,
)

interface YouLyPlusApi {
    @GET("v2/lyrics/get")
    suspend fun getLyrics(
        @Query("title") title: String,
        @Query("artist") artist: String,
        @Query("duration") duration: Int,
        @Query("album") album: String? = null,
        @Query("id") id: String? = null,
        @Query("isrc") isrc: String? = null,
    ): YouLyPlusResponse
}

class YouLyPlusProvider : LyricsProvider {
    override val name = "YouLyPlus"

    private val BASE_SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )

    private val lastWorkingServer = AtomicReference<String?>(null)

    private val servers: List<String> get() {
        val lws = lastWorkingServer.get() ?: return BASE_SERVERS
        return listOf(lws) + BASE_SERVERS.filter { it != lws }
    }

    private val apis: Map<String, YouLyPlusApi> by lazy {
        servers.associateWith { baseUrl ->
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YouLyPlusApi::class.java)
        }
    }

    override suspend fun fetch(
        title: String,
        artist: String?,
        album: String?,
        duration: Long?,
        youtubeVideoId: String?
    ): LyricsResult {
        val durSec = duration?.toInt()?.takeIf { it > 0 } ?: 0

        return try {
            val scope = CoroutineScope(Dispatchers.IO)
            val jobs = servers.map { server ->
                server to scope.async { fetchFromServer(apis[server]!!, title, artist ?: title, durSec, album) }
            }

            try {
                var remaining = jobs.toMutableList()
                while (remaining.isNotEmpty()) {
                    val (winServer, winResponse) = select {
                        remaining.forEach { (srv, deferred) ->
                            deferred.onAwait { response -> srv to response }
                        }
                    }
                    remaining.removeAll { it.first == winServer }

                    val lrc = winResponse?.let { resp ->
                        resp.syncedLyrics?.takeIf { it.isNotBlank() }
                            ?: resp.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
                            ?: resp.plainLyrics?.takeIf { it.isNotBlank() }
                    }
                    if (!lrc.isNullOrBlank()) {
                        lastWorkingServer.set(winServer)
                        return@fetch when {
                            lrc.startsWith("[") -> LrcParser.parseSyncedLyrics(lrc).let {
                                if (it.isNotEmpty()) LyricsResult.Synced(it) else LyricsResult.Plain(lrc)
                            }
                            else -> LyricsResult.Plain(lrc)
                        }
                    }
                }
                throw IllegalStateException("No lyrics found from any YouLyPlus server")
            } finally {
                jobs.forEach { it.second.cancel() }
            }
        } catch (e: Exception) {
            LyricsResult.Error("YouLyPlus failed: ${e.message}")
        }
    }

    private suspend fun fetchFromServer(
        api: YouLyPlusApi,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): YouLyPlusResponse? {
        return try {
            api.getLyrics(title, artist, duration, album)
        } catch (e: Exception) {
            null
        }
    }

    private fun List<YouLyPlusLyricsItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val lineTime = item.time ?: 0L
            val isBg = item.syllabus?.any { it.isBackground == true } == true
            val lineTimestamp = formatTime(lineTime)
            val bgMarker = if (isBg) "{bg}" else ""

            val syllabus = item.syllabus
            if (!syllabus.isNullOrEmpty()) {
                val sb = StringBuilder(lineTimestamp)
                sb.append(bgMarker)
                syllabus.forEach { syl ->
                    val sylTime = syl.time ?: 0L
                    sb.append(formatTime(sylTime, isSyllable = true))
                    sb.append(syl.text ?: "")
                    if ((syl.text ?: "").endsWith(" ").not()) {
                        sb.append(" ")
                    }
                }
                sb.toString().trim()
            } else {
                lineTimestamp + bgMarker + (item.text ?: "")
            }
        }
    }

    private fun formatTime(timeMs: Long, isSyllable: Boolean = false): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return String.format("%s%02d:%02d.%03d%s", prefix, minutes, seconds, millis, suffix)
    }
}
