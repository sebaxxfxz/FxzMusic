package com.fxzmusic.app.service
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.data.SongFormatEntity
import com.fxzmusic.app.util.*

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.audio.SonicAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.ytpipeline.YTPlayerUtils
import com.fxzmusic.ytpipeline.log.AudioQuality
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    companion object {
        var currentAudioSessionId by mutableIntStateOf(0)
            private set
        var exoPlayerInstance: ExoPlayer? = null
            private set

        private const val CHUNK_LENGTH = 512 * 1024L
        private const val PLAYER_CACHE_MAX_BYTES = 500L * 1024 * 1024
        private const val CROSSFADE_MS = 6_000L
        private const val ERROR_CODE_NO_STREAM = 1000001
    }

    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    private var equalizer: Equalizer? = null
    private lateinit var connectivityManager: ConnectivityManager

    private var crossfadeJob: Job? = null
    private val gson = Gson()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var database: FxzDatabase
    private var fadeJob: Job? = null
    private var isFading = false

    private var playerCache: SimpleCache? = null
    private var downloadCache: SimpleCache? = null

    private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var consecutiveErrorCount = 0

    private val eqUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_EQ_UPDATE) applyEqFromPrefs()
        }
    }

    private val sleepTimerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_SLEEP_TIMER_EXPIRE) return
            val player   = mediaSession?.player ?: return
            val fadeSecs = intent.getIntExtra(EXTRA_FADE_DURATION_S, 0)
            fadeAndPause(player, fadeSecs)
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val player = mediaSession?.player ?: return@OnAudioFocusChangeListener
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = player.isPlaying
                cancelFadeIfActive(player)
                if (player.isPlaying) player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = player.isPlaying
                cancelFadeIfActive(player)
                if (player.isPlaying) player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.volume = 0.3f * currentNormalizedVolume()
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = currentNormalizedVolume()
                if (wasPlayingBeforeFocusLoss) player.play()
            }
        }
    }

    private fun currentNormalizedVolume(): Float {
        val normalized = prefs.getFloat("current_normalized_volume", 1.0f)
        return normalized.coerceIn(0.35f, 1.0f)
    }

    private fun fadeAndPause(player: Player, fadeSecs: Int) {
        fadeJob?.cancel()
        if (fadeSecs <= 0) { if (player.isPlaying) player.pause(); return }
        isFading = true
        val totalMs = fadeSecs * 1_000L
        val steps   = 40
        val stepMs  = (totalMs / steps).coerceAtLeast(25L)
        fadeJob = serviceScope.launch {
            try {
                for (i in steps downTo 0) {
                    val fraction = i.toFloat() / steps.toFloat()
                    player.volume = fraction * fraction * currentNormalizedVolume()
                    delay(stepMs)
                }
                player.pause()
                player.volume = currentNormalizedVolume()
            } catch (e: Exception) {
                player.volume = currentNormalizedVolume()
            } finally {
                isFading = false
            }
        }
    }

    private fun cancelFadeIfActive(player: Player) {
        if (isFading) { fadeJob?.cancel(); isFading = false; player.volume = currentNormalizedVolume() }
    }

    override fun onCreate() {
        super.onCreate()
        prefs        = getSharedPreferences("playback_state", MODE_PRIVATE)
        settingsPrefs = getSharedPreferences("playback_settings", MODE_PRIVATE)
        database = FxzDatabase.getInstance(applicationContext)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        initPlayerCache()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                120_000,
                5_000,
                10_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = createRenderersFactory()

        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .build()

        val savedSpeed = prefs.getFloat("playback_speed", 1.0f)
        if (savedSpeed != 1.0f) player.playbackParameters = PlaybackParameters(savedSpeed)

        exoPlayerInstance = player
        currentAudioSessionId = player.audioSessionId
        initEqualizer(player.audioSessionId)

        val eqFilter    = IntentFilter(ACTION_EQ_UPDATE)
        val sleepFilter = IntentFilter(ACTION_SLEEP_TIMER_EXPIRE)
        ContextCompat.registerReceiver(this, eqUpdateReceiver, eqFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(this, sleepTimerReceiver, sleepFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                currentAudioSessionId = audioSessionId
                initEqualizer(audioSessionId)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) cancelFadeIfActive(player)
                savePlaybackState(player)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                applyLoudnessNormalization(player)
                savePlaybackState(player)
                preloadUpcoming(player)
                startCrossfadeCheck(player)
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) { savePlaybackState(player) }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    consecutiveErrorCount = 0
                }
            }
            override fun onPlayerErrorChanged(error: PlaybackException?) {
                if (error == null) return
                val item = player.currentMediaItem ?: return
                val mediaId = item.mediaId ?: ""
                Log.e("PlaybackService", "Player error: ${error.errorCodeName} code=${error.errorCode}", error)
                
                if (mediaId.isNotEmpty()) {
                    songUrlCache.remove(mediaId)
                    try {
                        playerCache?.removeResource(mediaId)
                    } catch (e: Exception) {
                        Log.w("PlaybackService", "Cache clear failed (non-critical): ${e.message}")
                    }
                }
                
                val currentIndex = player.currentMediaItemIndex
                val currentPosition = player.currentPosition
                
                consecutiveErrorCount++
                val maxRetries = 5
                if (consecutiveErrorCount > maxRetries) {
                    consecutiveErrorCount = 0
                    Log.w("PlaybackService", "Too many errors for this track, skipping to next")
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                        player.prepare()
                        player.playWhenReady = true
                    } else {
                        player.playWhenReady = false
                    }
                    return
                }
                
                val delayMs = (500L * consecutiveErrorCount).coerceAtMost(3000L)
                Log.i("PlaybackService", "Retrying playback (attempt $consecutiveErrorCount, delay ${delayMs}ms)...")
                serviceScope.launch {
                    kotlinx.coroutines.delay(delayMs)
                    if (currentIndex != C.INDEX_UNSET && player.currentMediaItemIndex == currentIndex) {
                        val currentItem = player.getMediaItemAt(currentIndex)
                        if (currentItem != null) {
                            player.setMediaItem(currentItem, currentPosition)
                        } else {
                            player.seekTo(currentIndex, currentPosition)
                        }
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            }
        })

        applyLoudnessNormalization(player)

        val sessionIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0, sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibraryService.MediaLibrarySession.Builder(
            this, player, FxzMediaLibrarySessionCallback()
        )
            .setSessionActivity(sessionActivityIntent)
            .build()

        requestAudioFocus()
    }

    private fun createRenderersFactory(): DefaultRenderersFactory {
        return object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(
                        SilenceSkippingAudioProcessor(2_000_000, 0.01f, 2_000_000, 0, 256),
                        SonicAudioProcessor()
                    ))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
    }

    private fun initPlayerCache() {
        try {
            CacheProvider.init(this)
            playerCache = CacheProvider.getPlayerCache()
            downloadCache = CacheProvider.getDownloadCache()
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Failed to init player cache", e)
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val dc = downloadCache ?: run {
            CacheProvider.init(this)
            downloadCache = CacheProvider.getDownloadCache()
            downloadCache ?: return DefaultDataSource.Factory(this)
        }
        val pc = playerCache ?: run {
            CacheProvider.init(this)
            playerCache = CacheProvider.getPlayerCache()
            playerCache ?: return DefaultDataSource.Factory(this)
        }

        val networkDataSourceFactory = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(
                this,
                OkHttpDataSource.Factory(
                    OkHttpClient.Builder()
                        .proxy(YouTube.proxy)
                        .build()
                )
            )
        ) { dataSpec ->
            
            val scheme = dataSpec.uri.scheme
            val uriString = dataSpec.uri.toString()

            val isDirectStream = scheme != null && (scheme == "http" || scheme == "https") && 
                (uriString.contains("googlevideo.com") || uriString.contains("videoplayback"))

            if (isDirectStream) {
                return@Factory dataSpec
            }

            val isLocalUri = scheme != null && (scheme == "file" || scheme == "content")
            val isLocalPath = scheme == null && (uriString.startsWith("/") || uriString.contains('/'))
            if (isLocalUri || isLocalPath) {
                return@Factory dataSpec
            }

            val rawKey = dataSpec.key
            val videoId = extractVideoId(rawKey, uriString) ?: return@Factory dataSpec
            val mediaId = videoId

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = mediaId,
                    audioQuality = AudioQuality.OPUS,
                    connectivityManager = connectivityManager,
                    context = this@PlaybackService,
                    showAudioFallbackToast = false
                )
            }.getOrElse { throwable ->
                when (throwable) {
                    is ConnectException, is UnknownHostException -> {
                        throw PlaybackException("No internet connection", throwable, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
                    }
                    is SocketTimeoutException -> {
                        throw PlaybackException("Connection timed out", throwable, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
                    }
                    is PlaybackException -> throw throwable
                    else -> throw PlaybackException("Stream error: ${throwable.message}", throwable, PlaybackException.ERROR_CODE_REMOTE_ERROR)
                }
            }

            val format = playbackData.format

            try {
                runBlocking(Dispatchers.IO) {
                    database.songFormatDao().upsert(
                        SongFormatEntity(
                            mediaId = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.substringAfter("codecs=\"", "").substringBefore("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate ?: 0,
                            contentLength = format.contentLength ?: 0L,
                            playbackUrl = "",
                            streamExpiresAt = System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L,
                            savedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("PlaybackService", "Failed to persist format: ${e.message}")
            }

            songUrlCache[mediaId] = playbackData.streamUrl to (System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L)
            dataSpec.buildUpon().setUri(playbackData.streamUrl.toUri()).build()
        }

        return CacheDataSource.Factory()
            .setCache(dc)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(pc)
                    .setUpstreamDataSourceFactory(networkDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun createMediaSourceFactory(): DefaultMediaSourceFactory =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            DefaultExtractorsFactory()
        )

    private fun startCrossfadeCheck(player: Player) {
        crossfadeJob?.cancel()
        if (!settingsPrefs.getBoolean("crossfade_enabled", false)) return
        crossfadeJob = serviceScope.launch {
            while (coroutineContext.isActive && player.isPlaying) {
                val duration = player.duration
                val position = player.currentPosition
                val remainingMs = duration - position
                if (remainingMs in 1L..CROSSFADE_MS && player.mediaItemCount > player.currentMediaItemIndex + 1) {
                    android.util.Log.d("PlaybackService", "Crossfade: fading out, remaining=${remainingMs}ms")
                    val steps = 30
                    val stepMs = CROSSFADE_MS / steps
                    val startVol = currentNormalizedVolume()
                    for (i in 1..steps) {
                        val ratio = 1f - (i.toFloat() / steps)
                        player.volume = (startVol * ratio).coerceIn(0f, 1f)
                        delay(stepMs)
                    }
                    player.volume = 0f
                    player.seekToNext()
                    player.volume = currentNormalizedVolume()
                    if (settingsPrefs.getBoolean("volume_fade_in", true)) {
                        val fadeInSteps = 20
                        val fadeInStepMs = 1_000L / fadeInSteps
                        for (i in 1..fadeInSteps) {
                            player.volume = (currentNormalizedVolume() * i / fadeInSteps).coerceIn(0f, 1f)
                            delay(fadeInStepMs)
                        }
                        player.volume = currentNormalizedVolume()
                    }
                    break
                }
                delay(200)
            }
        }
    }

    private fun preloadUpcoming(player: Player) {
        val currentIndex = player.currentMediaItemIndex
        val itemCount = player.mediaItemCount
        if (currentIndex < 0 || itemCount == 0) return
        val upperBound = (currentIndex + 3).coerceAtMost(itemCount - 1)
        if (currentIndex >= upperBound) return

        val upcomingIds = mutableListOf<String>()
        for (i in (currentIndex + 1)..upperBound) {
            val item = player.getMediaItemAt(i)
            val id = item.mediaId ?: continue
            if (id.length == 11) upcomingIds.add(id)
        }

        preloadScope.launch {
            for (videoId in upcomingIds) {
                val cached = songUrlCache[videoId]
                if (cached != null && cached.second > System.currentTimeMillis()) continue
                try {
                    val playbackData = YTPlayerUtils.playerResponseForPlayback(
                        videoId = videoId,
                        audioQuality = AudioQuality.OPUS,
                        connectivityManager = connectivityManager,
                        context = this@PlaybackService,
                        showAudioFallbackToast = false
                    ).getOrNull() ?: continue
                    songUrlCache[videoId] = playbackData.streamUrl to (System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L)
                } catch (_: Exception) {}
            }
        }
    }

    private fun applyLoudnessNormalization(player: Player) {
        val enabled = settingsPrefs.getBoolean("loudness_normalization", true)
        if (!enabled) {
            prefs.edit { putFloat("current_normalized_volume", 1.0f) }
            player.volume = 1.0f
            return
        }

        val mediaId = player.currentMediaItem?.mediaId ?: return
        serviceScope.launch(Dispatchers.IO) {
            val existing = database.songLoudnessDao().get(mediaId)
            val gainDb = existing?.gainDb ?: estimateAndStoreGain(mediaId)
            val gainLinear = dbToLinear(gainDb)
            val normalized = (gainLinear * 0.92f).coerceIn(0.35f, 1.0f)
            prefs.edit { putFloat("current_normalized_volume", normalized) }
            withContext(Dispatchers.Main) {
                if (!isFading) player.volume = normalized
            }
        }
    }

    private suspend fun estimateAndStoreGain(songId: String): Float {
        val hash = songId.hashCode().toLong() and 0xFFFF
        val gainDb = (((hash % 400L).toFloat() / 100f) - 2.0f).coerceIn(-2.0f, 2.0f)
        database.songLoudnessDao().upsert(
            SongLoudnessEntity(
                songId = songId,
                gainDb = gainDb,
                analyzedAt = System.currentTimeMillis()
            )
        )
        return gainDb
    }

    private fun dbToLinear(gainDb: Float): Float = 10.0.pow((gainDb / 20.0).toDouble()).toFloat()

    private fun initEqualizer(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == currentAudioSessionId && equalizer != null) { applyEqFromPrefs(); return }
        try {
            equalizer?.release()
            equalizer = null
            equalizer = Equalizer(0, audioSessionId)
            applyEqFromPrefs()
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "initEqualizer failed", e)
        }
    }

    private fun applyEqFromPrefs() {
        val eq        = equalizer ?: return
        val eqPrefs   = getSharedPreferences("eq_prefs", MODE_PRIVATE)
        val isEnabled = eqPrefs.getBoolean(EXTRA_EQ_ENABLED, true)
        try { eq.enabled = isEnabled } catch (_: Exception) {}
        if (!isEnabled) return

        val profileId = eqPrefs.getString(EXTRA_EQ_PROFILE, "flat") ?: "flat"
        val bands: List<EqBand>? = when (profileId) {
            "__temp__" -> {
                val json = eqPrefs.getString("temp_bands", null)
                if (json != null) {
                    val type = object : TypeToken<List<EqBand>>() {}.type
                    try { gson.fromJson(json, type) } catch (_: Exception) { null }
                } else null
            }
            else -> {
                val preset = PRESET_PROFILES.find { it.id == profileId }
                if (preset != null) {
                    preset.bands
                } else {
                    val customJson = eqPrefs.getString("custom_profiles", null)
                    if (customJson != null) {
                        val type = object : TypeToken<List<EqProfile>>() {}.type
                        try {
                            val profiles: List<EqProfile> = gson.fromJson(customJson, type) ?: emptyList()
                            profiles.find { it.id == profileId }?.bands
                        } catch (_: Exception) { null }
                    } else null
                }
            }
        }
        val numberOfBands = eq.numberOfBands.toInt()
        bands?.forEach { band ->
            if (band.index < numberOfBands) {
                try { eq.setBandLevel(band.index.toShort(), (band.gainDb * 100).toInt().toShort()) } catch (_: Exception) {}
            }
        }
    }

    private fun savePlaybackState(player: Player) {
        val currentItem = player.currentMediaItem ?: return
        val meta     = currentItem.mediaMetadata
        val coverUrl = meta.artworkUri?.toString()
        prefs.edit {
            putInt("last_index",        player.currentMediaItemIndex)
            putLong("last_position",    player.currentPosition)
            putBoolean("was_playing",   player.isPlaying)
            putString("last_title",     meta.title?.toString() ?: "")
            putString("last_artist",    meta.artist?.toString() ?: "")
            putString("last_cover_url", coverUrl)
        }
    }

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
        }
    }

    private fun abandonAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession? = mediaSession as? MediaLibraryService.MediaLibrarySession

    override fun onDestroy() {
        mediaSession?.player?.pause()
        abandonAudioFocus()
        fadeJob?.cancel()
        preloadScope.cancel()
        serviceScope.cancel()
        currentAudioSessionId = 0
        try { unregisterReceiver(eqUpdateReceiver)   } catch (_: Exception) {}
        try { unregisterReceiver(sleepTimerReceiver) } catch (_: Exception) {}
        try { equalizer?.release()                   } catch (_: Exception) {}
        mediaSession?.run { player.release(); release(); mediaSession = null }
        exoPlayerInstance = null
        try { playerCache?.release() } catch (_: Exception) {}
        super.onDestroy()
    }
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
