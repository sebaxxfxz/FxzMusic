package com.example.fxzmusic

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

private const val FADE_COMPLETION_BUFFER_MS = 500L

val PLAYBACK_SPEEDS       = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
val PLAYBACK_SPEED_LABELS = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x")

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
    var shakeToSkip             by mutableStateOf(false)
    var fadeDurationSeconds     by mutableIntStateOf(20)
    var loudnessNormalization   by mutableStateOf(true)
    var wifiOnlyCovers          by mutableStateOf(true)
    var themeMode               by mutableStateOf(ThemeMode.GLASSMORPHISM)
        private set

    var totalStorageSpaceGb     by mutableFloatStateOf(128f)
    var usedStorageSpaceGb      by mutableFloatStateOf(42.5f)

    var blacklistedFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    var onSpeedRestored: ((Float) -> Unit)? = null
    var onBlacklistChanged: ((Set<String>) -> Unit)? = null

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
        shakeToSkip            = prefs?.getBoolean("shake_to_skip", false) ?: false
        fadeDurationSeconds    = prefs?.getInt("fade_duration_s", 20) ?: 20
        customSleepTimerMinutes = prefs?.getInt("custom_sleep_minutes", 0) ?: 0
        loudnessNormalization  = prefs?.getBoolean("loudness_normalization", true) ?: true
        wifiOnlyCovers         = prefs?.getBoolean("wifi_only_covers", true) ?: true
        themeMode              = ThemeMode.entries.getOrElse(prefs?.getInt("theme_mode", ThemeMode.entries.indexOf(ThemeMode.GLASSMORPHISM)) ?: ThemeMode.entries.indexOf(ThemeMode.GLASSMORPHISM)) { ThemeMode.GLASSMORPHISM }
        onSpeedRestored?.invoke(playbackSpeed)
        calculateStorageSpace(context)
        loadBlacklistedFolders()
    }

    private fun loadBlacklistedFolders() {
        blacklistedFolders = prefs?.getStringSet("blacklisted_folders", emptySet()) ?: emptySet()
    }

    fun addBlacklistedFolder(path: String) {
        blacklistedFolders = blacklistedFolders + path
        prefs?.edit()?.putStringSet("blacklisted_folders", blacklistedFolders)?.apply()
        onBlacklistChanged?.invoke(blacklistedFolders)
    }

    fun removeBlacklistedFolder(path: String) {
        blacklistedFolders = blacklistedFolders - path
        prefs?.edit()?.putStringSet("blacklisted_folders", blacklistedFolders)?.apply()
        onBlacklistChanged?.invoke(blacklistedFolders)
    }

    fun isBlacklisted(path: String): Boolean {
        return blacklistedFolders.any { path.startsWith(it) }
    }

    fun currentTheme(): FxzTheme {
        val rawAccent = ACCENT_COLORS.getOrElse(accentColorIndex) { ACCENT_COLORS[0] }
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
