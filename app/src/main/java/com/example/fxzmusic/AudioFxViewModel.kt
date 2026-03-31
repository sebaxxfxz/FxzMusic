package com.example.fxzmusic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AudioFxViewModel : ViewModel() {

    private var prefs: SharedPreferences? = null
    private var virtualizer: Virtualizer? = null
    private var bassBoost: BassBoost? = null
    private var audioSessionId = 0

    var virtualizerEnabled by mutableStateOf(false)
        private set
    var virtualizerStrength by mutableStateOf(800)
        private set

    var bassBoostEnabled by mutableStateOf(false)
        private set
    var bassBoostStrength by mutableStateOf(500)
        private set

    var selectedPreset by mutableStateOf("Normal")
        private set

    val presets = listOf("Normal", "Concierto", "Estadio", "Club", "Sala")

    fun init(context: Context) {
        prefs = context.getSharedPreferences("audio_fx_prefs", Context.MODE_PRIVATE)
        virtualizerEnabled  = prefs?.getBoolean("virtualizer_on", false) ?: false
        virtualizerStrength = prefs?.getInt("virtualizer_strength", 800) ?: 800
        bassBoostEnabled    = prefs?.getBoolean("bass_on", false) ?: false
        bassBoostStrength   = prefs?.getInt("bass_strength", 500) ?: 500
        selectedPreset      = prefs?.getString("preset", "Normal") ?: "Normal"
    }

    fun attachToSession(context: Context, sessionId: Int) {
        if (sessionId == 0 || sessionId == audioSessionId) return
        audioSessionId = sessionId
        releaseEffects()
        try {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = virtualizerEnabled
                if (virtualizerEnabled) setStrength(virtualizerStrength.toShort())
            }
        } catch (_: Exception) {}
        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = bassBoostEnabled
                if (bassBoostEnabled) setStrength(bassBoostStrength.toShort())
            }
        } catch (_: Exception) {}
        applyPreset(selectedPreset)
    }

    fun toggleVirtualizer(context: Context) {
        virtualizerEnabled = !virtualizerEnabled
        try { virtualizer?.enabled = virtualizerEnabled } catch (_: Exception) {}
        if (virtualizerEnabled) try { virtualizer?.setStrength(virtualizerStrength.toShort()) } catch (_: Exception) {}
        prefs?.edit()?.putBoolean("virtualizer_on", virtualizerEnabled)?.apply()
    }

    fun updateVirtualizerStrength(strength: Int) {
        virtualizerStrength = strength
        try { if (virtualizerEnabled) virtualizer?.setStrength(strength.toShort()) } catch (_: Exception) {}
        prefs?.edit()?.putInt("virtualizer_strength", strength)?.apply()
    }

    fun toggleBassBoost(context: Context) {
        bassBoostEnabled = !bassBoostEnabled
        try { bassBoost?.enabled = bassBoostEnabled } catch (_: Exception) {}
        if (bassBoostEnabled) try { bassBoost?.setStrength(bassBoostStrength.toShort()) } catch (_: Exception) {}
        prefs?.edit()?.putBoolean("bass_on", bassBoostEnabled)?.apply()
    }

    fun updateBassBoostStrength(strength: Int) {
        bassBoostStrength = strength
        try { if (bassBoostEnabled) bassBoost?.setStrength(strength.toShort()) } catch (_: Exception) {}
        prefs?.edit()?.putInt("bass_strength", strength)?.apply()
    }

    fun selectPreset(context: Context, preset: String) {
        selectedPreset = preset
        applyPreset(preset)
        prefs?.edit()?.putString("preset", preset)?.apply()
    }

    private fun applyPreset(preset: String) {
        when (preset) {
            "Concierto" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(900) } catch (_: Exception) {}
                try { bassBoost?.enabled = true; bassBoost?.setStrength(600) } catch (_: Exception) {}
            }
            "Estadio" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(1000) } catch (_: Exception) {}
                try { bassBoost?.enabled = true; bassBoost?.setStrength(800) } catch (_: Exception) {}
            }
            "Club" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(700) } catch (_: Exception) {}
                try { bassBoost?.enabled = true; bassBoost?.setStrength(700) } catch (_: Exception) {}
            }
            "Sala" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(500) } catch (_: Exception) {}
                try { bassBoost?.enabled = false } catch (_: Exception) {}
            }
            else -> {
                try { virtualizer?.enabled = virtualizerEnabled; if (virtualizerEnabled) virtualizer?.setStrength(virtualizerStrength.toShort()) } catch (_: Exception) {}
                try { bassBoost?.enabled = bassBoostEnabled; if (bassBoostEnabled) bassBoost?.setStrength(bassBoostStrength.toShort()) } catch (_: Exception) {}
            }
        }
    }

    private fun releaseEffects() {
        try { virtualizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release()   } catch (_: Exception) {}
        virtualizer = null
        bassBoost   = null
    }

    override fun onCleared() {
        super.onCleared()
        releaseEffects()
    }
}
