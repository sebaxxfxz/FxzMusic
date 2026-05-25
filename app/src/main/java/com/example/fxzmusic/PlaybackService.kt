package com.example.fxzmusic

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
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow


@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    companion object {
        var currentAudioSessionId by mutableIntStateOf(0)
            private set
    }

    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    private var equalizer: Equalizer? = null
    private val gson = Gson()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var database: FxzDatabase
    private var fadeJob: Job? = null
    private var isFading = false

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

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()

        val savedSpeed = prefs.getFloat("playback_speed", 1.0f)
        if (savedSpeed != 1.0f) player.playbackParameters = PlaybackParameters(savedSpeed)

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
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) { savePlaybackState(player) }
        })

        applyLoudnessNormalization(player)

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
                    MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
            })
            .build()

        requestAudioFocus()
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
        bands?.forEach { band ->
            try { eq.setBandLevel(band.index.toShort(), (band.gainDb * 100).toInt().toShort()) } catch (_: Exception) {}
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.player?.pause()
        abandonAudioFocus()
        fadeJob?.cancel()
        serviceScope.cancel()
        currentAudioSessionId = 0
        try { unregisterReceiver(eqUpdateReceiver)   } catch (_: Exception) {}
        try { unregisterReceiver(sleepTimerReceiver) } catch (_: Exception) {}
        try { equalizer?.release()                   } catch (_: Exception) {}
        mediaSession?.run { player.release(); release(); mediaSession = null }
        super.onDestroy()
    }
}
