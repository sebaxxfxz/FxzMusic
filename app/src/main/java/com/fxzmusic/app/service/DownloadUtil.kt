package com.fxzmusic.app.service

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.fxzmusic.app.data.DownloadEntity
import com.fxzmusic.app.data.FxzDatabase
import com.fxzmusic.app.data.SongFormatEntity
import com.fxzmusic.ytpipeline.log.AudioQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DownloadUtil private constructor(
    private val appContext: Context,
    private val database: FxzDatabase,
    private val playerCache: SimpleCache,
    private val downloadCache: SimpleCache,
    private val databaseProvider: DatabaseProvider
) {

    val downloads: MutableStateFlow<Map<String, Download>> = MutableStateFlow(emptyMap())

    private val connectivityManager: ConnectivityManager =
        appContext.getSystemService()!!
    private val youTubeRepo = YouTubeMusicRepository.get()
    private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val upstreamFactory: DataSource.Factory = ChunkingDataSourceFactory(
        OkHttpDataSource.Factory(okHttpClient).setUserAgent("com.fxzmusic.app")
    )

    private val dataSourceFactory: DataSource.Factory =
        ResolvingDataSource.Factory(
            CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        ) { dataSpec ->
            val rawKey = dataSpec.key ?: ""
            val rawUri = dataSpec.uri.toString()

            if (rawUri.startsWith("content://") || rawUri.startsWith("file://") ||
                rawUri.startsWith("/")) {
                return@Factory dataSpec
            }

            val videoId = extractVideoId(rawKey, rawUri) ?: return@Factory dataSpec

            val cached = songUrlCache[videoId]
            if (cached != null && cached.second > System.currentTimeMillis()) {
                return@Factory dataSpec.withUri(cached.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                try {
                    com.fxzmusic.ytpipeline.YTPlayerUtils.playerResponseForPlayback(
                        videoId = videoId,
                        audioQuality = getPreferredAudioQuality(),
                        connectivityManager = connectivityManager,
                        context = appContext,
                        isDownload = true,
                        showAudioFallbackToast = false
                    ).getOrNull()
                } catch (_: Exception) { null }
            }

            val url = playbackData?.streamUrl ?: runBlocking {
                try {
                    youTubeRepo.resolveStreamUrl(
                        videoId = videoId,
                        context = appContext,
                        connectivityManager = connectivityManager,
                    ).getOrNull()
                } catch (_: Exception) { null }
            }

            if (url != null) {
                songUrlCache[videoId] = url to (System.currentTimeMillis() + 5 * 60 * 60 * 1000L)
                if (playbackData != null) {
                    val format = playbackData.format
                    try {
                        runBlocking(Dispatchers.IO) {
                            database.songFormatDao().upsert(
                                SongFormatEntity(
                                    mediaId = videoId,
                                    itag = format.itag,
                                    mimeType = format.mimeType.split(";")[0],
                                    codecs = format.mimeType.substringAfter("codecs=\"", "").substringBefore("\""),
                                    bitrate = format.bitrate,
                                    sampleRate = format.audioSampleRate ?: 0,
                                    contentLength = format.contentLength ?: 0L,
                                    playbackUrl = url,
                                    streamExpiresAt = System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L,
                                    savedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("DownloadUtil", "Failed to persist format: ${e.message}")
                    }
                }
                return@Factory dataSpec.withUri(url.toUri())
            }

            throw androidx.media3.common.PlaybackException(
                "Cannot resolve $videoId",
                IllegalStateException("Failed to resolve stream for $videoId"),
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            )
        }

    private val executor = Executors.newFixedThreadPool(2)

    private val downloadManager: DownloadManager = DownloadManager(
        appContext,
        databaseProvider,
        downloadCache,
        dataSourceFactory,
        executor
    ).apply {
        addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                scope.launch { refreshDownloads() }
                if (download.state == Download.STATE_COMPLETED) {
                    
                    val videoId = download.request.id
                    scope.launch {
                        try {
                            val existing = database.downloadDao().getAll().find { it.videoId == videoId }
                            if (existing == null) {
                                
                                database.downloadDao().upsert(
                                    DownloadEntity(
                                        videoId = videoId,
                                        title = videoId,
                                        artist = "",
                                        fileSize = download.bytesDownloaded
                                    )
                                )
                            } else {
                                database.downloadDao().upsert(
                                    existing.copy(fileSize = download.bytesDownloaded)
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("DownloadUtil", "Failed to persist DownloadEntity: ${e.message}")
                        }
                    }
                }
            }
        })
    }

    init {
        scope.launch {
            refreshDownloads()
        }
    }

    private suspend fun refreshDownloads() {
        try {
            val cursor = downloadManager.downloadIndex.getDownloads()
            val collected = mutableMapOf<String, Download>()
            cursor.use {
                while (it.moveToNext()) {
                    val d = it.getDownload()
                    if (d != null) collected[d.request.id] = d
                }
            }
            downloads.value = collected
        } catch (e: Exception) {
            android.util.Log.w("DownloadUtil", "refreshDownloads failed: ${e.message}")
        }
    }

    fun getDownloadManager(): DownloadManager = downloadManager

    val downloadsDbFlow: Flow<List<DownloadEntity>> = database.downloadDao().getAllFlow()

    fun getPreferredAudioQuality(): AudioQuality {
        val prefs = appContext.getSharedPreferences("fxz_download_settings", Context.MODE_PRIVATE)
        val name = prefs.getString("download_quality", AudioQuality.OPUS.name) ?: AudioQuality.OPUS.name
        return try { AudioQuality.valueOf(name) } catch (_: Exception) { AudioQuality.OPUS }
    }

    fun setPreferredAudioQuality(quality: AudioQuality) {
        appContext.getSharedPreferences("fxz_download_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("download_quality", quality.name)
            .apply()
    }

    fun enqueueBatch(
        songs: List<com.fxzmusic.app.data.Song>,
        audioQuality: AudioQuality = getPreferredAudioQuality()
    ) {
        setPreferredAudioQuality(audioQuality)
        scope.launch {
            for (song in songs) {
                if (!song.isYouTube && song.filePath.isNotEmpty()) continue
                val videoId = song.youtubeVideoId?.takeIf { it.isNotEmpty() } ?: song.id
                if (videoId.isEmpty()) continue
                enqueue(
                    videoId = videoId,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    coverUrl = song.coverUrl,
                    duration = song.duration,
                    audioQuality = audioQuality
                )
            }
        }
    }

    fun enqueue(
        videoId: String,
        title: String = "",
        artist: String = "",
        album: String = "",
        coverUrl: String? = null,
        duration: Int = 0,
        audioQuality: AudioQuality = getPreferredAudioQuality()
    ) {
        val cleanVideoId = extractVideoId(videoId, videoId) ?: videoId
        val request = DownloadRequest.Builder(cleanVideoId, "yt:$cleanVideoId".toUri())
            .setCustomCacheKey(cleanVideoId)
            .setData("yt:$cleanVideoId".toByteArray())
            .build()
        
        scope.launch {
            try {
                database.downloadDao().upsert(
                    DownloadEntity(
                        videoId = cleanVideoId,
                        title = title.ifBlank { cleanVideoId },
                        artist = artist,
                        album = album,
                        coverUrl = coverUrl,
                        duration = duration
                    )
                )
                
                if (coverUrl != null) {
                    val request2 = ImageRequest.Builder(appContext)
                        .data(coverUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    appContext.imageLoader.enqueue(request2)
                }
            } catch (e: Exception) {
                android.util.Log.w("DownloadUtil", "Failed to pre-populate DownloadEntity: ${e.message}")
            }
        }
        try {
            DownloadService.sendAddDownload(appContext, FxzDownloadService::class.java, request, false)
            scope.launch { refreshDownloads() }
        } catch (e: Exception) {
            android.util.Log.e("DownloadUtil", "enqueue failed for $cleanVideoId", e)
        }
    }

    fun cancel(videoId: String, audioQuality: AudioQuality = AudioQuality.OPUS) {
        val cleanVideoId = extractVideoId(videoId, videoId) ?: videoId
        try {
            DownloadService.sendSetStopReason(appContext, FxzDownloadService::class.java, cleanVideoId, 1, false)
        } catch (_: Exception) {}
    }

    fun resume(videoId: String, audioQuality: AudioQuality = AudioQuality.OPUS) {
        val cleanVideoId = extractVideoId(videoId, videoId) ?: videoId
        try {
            DownloadService.sendSetStopReason(appContext, FxzDownloadService::class.java, cleanVideoId, 0, false)
        } catch (_: Exception) {}
    }

    fun delete(videoId: String, audioQuality: AudioQuality = AudioQuality.OPUS) {
        val cleanVideoId = extractVideoId(videoId, videoId) ?: videoId
        try {
            DownloadService.sendRemoveDownload(appContext, FxzDownloadService::class.java, cleanVideoId, false)
            scope.launch {
                database.songFormatDao().delete(cleanVideoId)
                database.downloadDao().deleteById(cleanVideoId)
                refreshDownloads()
            }
        } catch (e: Exception) {
            android.util.Log.w("DownloadUtil", "delete failed: ${e.message}")
        }
    }

    suspend fun persistFormatEntity(
        mediaId: String,
        itag: Int,
        mimeType: String,
        codecs: String?,
        bitrate: Int,
        sampleRate: Int,
        contentLength: Long,
        playbackUrl: String,
        streamExpiresAt: Long
    ) {
        database.songFormatDao().upsert(
            SongFormatEntity(
                mediaId = mediaId,
                itag = itag,
                mimeType = mimeType,
                codecs = codecs,
                bitrate = bitrate,
                sampleRate = sampleRate,
                contentLength = contentLength,
                playbackUrl = playbackUrl,
                streamExpiresAt = streamExpiresAt,
                savedAt = System.currentTimeMillis()
            )
        )
    }

    private fun extractVideoId(key: String?, uri: String?): String? {
        if (!key.isNullOrBlank()) {
            val candidate = key.removePrefix("yt:").substringBefore("_")
            if (candidate.length == 11 && candidate.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                return candidate
            }
        }
        if (!uri.isNullOrBlank()) {
            if (uri.startsWith("yt:")) {
                val candidate = uri.removePrefix("yt:").substringBefore("_")
                if (candidate.length == 11 && candidate.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                    return candidate
                }
            }
            try {
                val parsedUri = android.net.Uri.parse(uri)
                val videoIdParam = parsedUri.getQueryParameter("v")
                if (!videoIdParam.isNullOrBlank() && videoIdParam.length == 11) {
                    return videoIdParam
                }
            } catch (_: Exception) {}
            val candidate = uri.substringAfterLast("/").substringBefore("?").substringBefore("_")
            if (candidate.length == 11 && candidate.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                return candidate
            }
        }
        return null
    }

    private fun isYoutubeMediaId(mediaId: String): Boolean {
        return extractVideoId(mediaId, mediaId) != null
    }

    private fun AudioQuality.toCacheKey(videoId: String): String = "${videoId}_${name}"

    fun clearBrokenCache() {
        scope.launch {
            try {
                
                downloadCache.keys.forEach { downloadCache.removeResource(it) }
                
                database.downloadDao().deleteAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: DownloadUtil? = null

        fun get(): DownloadUtil {
            return INSTANCE ?: error(
                "DownloadUtil not initialised. Call init() from FxzApplication."
            )
        }

        fun init(
            context: Context,
            database: FxzDatabase,
            playerCache: SimpleCache,
            downloadCache: SimpleCache,
            databaseProvider: DatabaseProvider
        ): DownloadUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadUtil(
                    context.applicationContext,
                    database,
                    playerCache,
                    downloadCache,
                    databaseProvider
                ).also { INSTANCE = it }
            }
        }
    }
}
