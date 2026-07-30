package com.fxzmusic.app.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import com.fxzmusic.app.data.ACTION_SLEEP_TIMER_EXPIRE
import com.fxzmusic.app.data.EXTRA_FADE_DURATION_S
import com.fxzmusic.app.ui.components.PlayerBackgroundStyle
import com.fxzmusic.app.ui.theme.ACCENT_COLORS
import com.fxzmusic.app.ui.theme.FxzTheme
import com.fxzmusic.app.ui.theme.ThemeMode
import com.fxzmusic.app.ui.theme.buildTheme
import com.fxzmusic.app.util.EventBus
import com.fxzmusic.app.util.UiEvent
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.models.IpVersion

private const val FADE_COMPLETION_BUFFER_MS = 500L

val SLEEP_TIMER_OPTIONS = listOf(0, 5, 10, 15, 20, 30, 45, 60, 90)
val SLEEP_TIMER_LABELS  = listOf("Off", "5m", "10m", "15m", "20m", "30m", "45m", "1h", "1.5h")

class PlaybackSettingsViewModel : ViewModel() {

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    var playbackSpeed           by mutableFloatStateOf(1.0f)
    var sleepTimerMinutes       by mutableIntStateOf(0)
    var sleepTimerRemainingMs   by mutableLongStateOf(0L)
    var isSleepTimerActive      by mutableStateOf(false)
    var customSleepTimerMinutes by mutableIntStateOf(0)
    var accentColorIndex        by mutableIntStateOf(0)
    var shakeToSkip                     by mutableStateOf(false)
    var dynamicColorBySong              by mutableStateOf(false)
    var dynamicSongAccentColor          by mutableStateOf<Color?>(null)
    var pauseOnHeadphonesDisconnect     by mutableStateOf(true)
    var resumeOnHeadphonesConnect        by mutableStateOf(false)
    var sleepAfterTrack         by mutableStateOf(false)
    var fadeDurationSeconds     by mutableIntStateOf(20)
    var loudnessNormalization   by mutableStateOf(true)
    var wifiOnlyCovers          by mutableStateOf(true)
    var ipVersion               by mutableStateOf(IpVersion.IPV4)
    var playerBackgroundStyle   by mutableStateOf(PlayerBackgroundStyle.DEFAULT)
    var themeMode               by mutableStateOf(ThemeMode.AMOLED)
        private set

    var totalStorageSpaceGb     by mutableFloatStateOf(128f)
    var usedStorageSpaceGb      by mutableFloatStateOf(42.5f)

    var blacklistedFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    private var sleepTimerJob: Job? = null

    fun calculateStorageSpace(context: Context) {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalSpaceBytes = totalBlocks * blockSize
            val availableSpaceBytes = availableBlocks * blockSize
            val usedSpaceBytes = totalSpaceBytes - availableSpaceBytes

            totalStorageSpaceGb = totalSpaceBytes / (1024f * 1024f * 1024f)
            usedStorageSpaceGb = usedSpaceBytes / (1024f * 1024f * 1024f)
        } catch (e: Exception) {
            totalStorageSpaceGb = 128f
            usedStorageSpaceGb = 42.5f
        }
    }

    fun init(context: Context) {
        appContext              = context.applicationContext
        prefs                  = context.applicationContext.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        playbackSpeed          = prefs?.getFloat("playback_speed", 1.0f) ?: 1.0f
        accentColorIndex       = prefs?.getInt("accent_color", 0) ?: 0
        shakeToSkip                    = prefs?.getBoolean("shake_to_skip", false) ?: false
        dynamicColorBySong             = prefs?.getBoolean("dynamic_color_by_song", false) ?: false
        pauseOnHeadphonesDisconnect     = prefs?.getBoolean("pause_on_disconnect", true) ?: true
        resumeOnHeadphonesConnect        = prefs?.getBoolean("resume_on_connect", false) ?: false
        sleepAfterTrack        = prefs?.getBoolean("sleep_after_track", false) ?: false
        fadeDurationSeconds    = prefs?.getInt("fade_duration_s", 20) ?: 20
        customSleepTimerMinutes = prefs?.getInt("custom_sleep_minutes", 0) ?: 0
        loudnessNormalization  = prefs?.getBoolean("loudness_normalization", true) ?: true
        wifiOnlyCovers         = prefs?.getBoolean("wifi_only_covers", true) ?: true
        ipVersion              = try {
            IpVersion.entries.getOrElse(prefs?.getInt("ip_version", 1) ?: 1) { IpVersion.IPV4 }
        } catch (_: Exception) { IpVersion.IPV4 }
        YouTube.ipVersion      = ipVersion
        playerBackgroundStyle  = try {
            PlayerBackgroundStyle.entries.getOrElse(prefs?.getInt("player_bg_style", 0) ?: 0) { PlayerBackgroundStyle.DEFAULT }
        } catch (_: Exception) { PlayerBackgroundStyle.DEFAULT }
        themeMode              = ThemeMode.entries.getOrElse(prefs?.getInt("theme_mode", ThemeMode.entries.indexOf(ThemeMode.AMOLED)) ?: ThemeMode.entries.indexOf(ThemeMode.AMOLED)) { ThemeMode.AMOLED }
        EventBus.tryPublish(UiEvent.SpeedRestored(playbackSpeed))
        calculateStorageSpace(context)
        loadBlacklistedFolders()
    }

    private fun loadBlacklistedFolders() {
        blacklistedFolders = prefs?.getStringSet("blacklisted_folders", emptySet()) ?: emptySet()
    }

    fun addBlacklistedFolder(path: String) {
        blacklistedFolders = blacklistedFolders + path
        prefs?.edit()?.putStringSet("blacklisted_folders", blacklistedFolders)?.apply()
        EventBus.tryPublish(UiEvent.BlacklistChanged(blacklistedFolders))
    }

    fun removeBlacklistedFolder(path: String) {
        blacklistedFolders = blacklistedFolders - path
        prefs?.edit()?.putStringSet("blacklisted_folders", blacklistedFolders)?.apply()
        EventBus.tryPublish(UiEvent.BlacklistChanged(blacklistedFolders))
    }

    fun isBlacklisted(path: String): Boolean {
        return blacklistedFolders.any { path.startsWith(it) }
    }

    fun currentTheme(): FxzTheme {
        val rawAccent = if (dynamicColorBySong && dynamicSongAccentColor != null) {
            dynamicSongAccentColor!!
        } else {
            ACCENT_COLORS.getOrElse(accentColorIndex) { ACCENT_COLORS[0] }.first
        }
        val accent = if (rawAccent == Color.White || rawAccent == Color(0xFFFFFFFF)) {
            Color(0xFFF0F0F0)
        } else {
            rawAccent
        }
        return buildTheme(accent, themeMode)
    }

    fun updateSpeed(speed: Float) {
        if (playbackSpeed == speed) return
        playbackSpeed = speed
        prefs?.edit()?.putFloat("playback_speed", speed)?.apply()
    }

    fun toggleShakeToSkip() {
        shakeToSkip = !shakeToSkip
        prefs?.edit()?.putBoolean("shake_to_skip", shakeToSkip)?.apply()
    }

    fun toggleDynamicColorBySong() {
        dynamicColorBySong = !dynamicColorBySong
        prefs?.edit()?.putBoolean("dynamic_color_by_song", dynamicColorBySong)?.apply()
    }

    fun togglePauseOnHeadphonesDisconnect() {
        pauseOnHeadphonesDisconnect = !pauseOnHeadphonesDisconnect
        prefs?.edit()?.putBoolean("pause_on_disconnect", pauseOnHeadphonesDisconnect)?.apply()
    }

    fun toggleResumeOnHeadphonesConnect() {
        resumeOnHeadphonesConnect = !resumeOnHeadphonesConnect
        prefs?.edit()?.putBoolean("resume_on_connect", resumeOnHeadphonesConnect)?.apply()
    }

    fun toggleSleepAfterTrack() {
        sleepAfterTrack = !sleepAfterTrack
        prefs?.edit()?.putBoolean("sleep_after_track", sleepAfterTrack)?.apply()
    }

    fun startSleepTimer(minutes: Int, onExpire: () -> Unit) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            isSleepTimerActive    = false
            sleepTimerRemainingMs = 0L
            sleepTimerMinutes     = 0
            return
        }
        sleepTimerMinutes     = minutes
        sleepTimerRemainingMs = minutes * 60_000L
        isSleepTimerActive    = true

        sleepTimerJob = viewModelScope.launch {
            while (sleepTimerRemainingMs > 0) {
                delay(1000)
                sleepTimerRemainingMs -= 1000
            }
            isSleepTimerActive = false

            appContext?.sendBroadcast(
                Intent(ACTION_SLEEP_TIMER_EXPIRE)
                    .setPackage(appContext?.packageName)
                    .putExtra(EXTRA_FADE_DURATION_S, fadeDurationSeconds)
            )

            val totalFadeMs = fadeDurationSeconds * 1000L + FADE_COMPLETION_BUFFER_MS
            if (totalFadeMs > 0) delay(totalFadeMs)

            onExpire()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        isSleepTimerActive    = false
        sleepTimerRemainingMs = 0L
        sleepTimerMinutes     = 0
    }

    fun setCustomSleepTimer(minutes: Int, onExpire: () -> Unit) {
        val clamped = minutes.coerceIn(1, 360)
        customSleepTimerMinutes = clamped
        prefs?.edit()?.putInt("custom_sleep_minutes", clamped)?.apply()
        startSleepTimer(clamped, onExpire)
    }

    fun formatSleepRemaining(): String {
        val ms   = sleepTimerRemainingMs
        val mins = (ms / 60_000).toInt()
        val secs = ((ms % 60_000) / 1000).toInt()
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun setAccentColor(index: Int) {
        if (accentColorIndex == index) return
        accentColorIndex = index
        prefs?.edit()?.putInt("accent_color", index)?.apply()
    }

    fun updateFadeDuration(seconds: Int) {
        val clamped = seconds.coerceIn(0, 60)
        if (fadeDurationSeconds == clamped) return
        fadeDurationSeconds = clamped
        prefs?.edit()?.putInt("fade_duration_s", clamped)?.apply()
    }

    fun updateLoudnessNormalization(enabled: Boolean) {
        if (loudnessNormalization == enabled) return
        loudnessNormalization = enabled
        prefs?.edit()?.putBoolean("loudness_normalization", enabled)?.apply()
    }

    fun updateWifiOnlyCovers(enabled: Boolean) {
        if (wifiOnlyCovers == enabled) return
        wifiOnlyCovers = enabled
        prefs?.edit()?.putBoolean("wifi_only_covers", enabled)?.apply()
    }

    fun updatePlayerBackgroundStyle(style: PlayerBackgroundStyle) {
        if (playerBackgroundStyle == style) return
        playerBackgroundStyle = style
        prefs?.edit()?.putInt("player_bg_style", PlayerBackgroundStyle.entries.indexOf(style))?.apply()
    }

    fun updateIpVersion(version: IpVersion) {
        if (ipVersion == version) return
        ipVersion = version
        YouTube.ipVersion = version
        prefs?.edit()?.putInt("ip_version", IpVersion.entries.indexOf(version))?.apply()
    }

    fun updateThemeMode(mode: ThemeMode) {
        if (themeMode == mode) return
        themeMode = mode
        prefs?.edit()?.putInt("theme_mode", ThemeMode.entries.indexOf(mode))?.apply()
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
    }
}
