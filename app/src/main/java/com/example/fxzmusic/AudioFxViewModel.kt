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
import android.util.Log

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
        } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to create Virtualizer for session $sessionId", e)
        }
        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = bassBoostEnabled
                if (bassBoostEnabled) setStrength(bassBoostStrength.toShort())
            }
        } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to create BassBoost for session $sessionId", e)
        }
        applyPreset(selectedPreset)
    }

    fun toggleVirtualizer(context: Context) {
        virtualizerEnabled = !virtualizerEnabled
        try { virtualizer?.enabled = virtualizerEnabled } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to set virtualizer enabled", e)
        }
        if (virtualizerEnabled) try { virtualizer?.setStrength(virtualizerStrength.toShort()) } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to set virtualizer strength", e)
        }
        prefs?.edit()?.putBoolean("virtualizer_on", virtualizerEnabled)?.apply()
    }

    fun updateVirtualizerStrength(strength: Int) {
        virtualizerStrength = strength
        try { if (virtualizerEnabled) virtualizer?.setStrength(strength.toShort()) } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to update virtualizer strength", e)
        }
        prefs?.edit()?.putInt("virtualizer_strength", strength)?.apply()
    }

    fun toggleBassBoost(context: Context) {
        bassBoostEnabled = !bassBoostEnabled
        try { bassBoost?.enabled = bassBoostEnabled } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to set bass boost enabled", e)
        }
        if (bassBoostEnabled) try { bassBoost?.setStrength(bassBoostStrength.toShort()) } catch (e: Exception) {
            Log.w("AudioFxViewModel", "Failed to set bass boost strength", e)
        }
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
                try { virtualizer?.enabled = true; virtualizer?.setStrength(900) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Concierto preset to virtualizer", e)
                }
                try { bassBoost?.enabled = true; bassBoost?.setStrength(600) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Concierto preset to bass boost", e)
                }
            }
            "Estadio" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(1000) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Estadio preset to virtualizer", e)
                }
                try { bassBoost?.enabled = true; bassBoost?.setStrength(800) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Estadio preset to bass boost", e)
                }
            }
            "Club" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(700) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Club preset to virtualizer", e)
                }
                try { bassBoost?.enabled = true; bassBoost?.setStrength(700) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Club preset to bass boost", e)
                }
            }
            "Sala" -> {
                try { virtualizer?.enabled = true; virtualizer?.setStrength(500) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply Sala preset to virtualizer", e)
                }
                try { bassBoost?.enabled = false } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to disable bass boost for Sala preset", e)
                }
            }
            else -> {
                try { virtualizer?.enabled = virtualizerEnabled; if (virtualizerEnabled) virtualizer?.setStrength(virtualizerStrength.toShort()) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply default preset to virtualizer", e)
                }
                try { bassBoost?.enabled = bassBoostEnabled; if (bassBoostEnabled) bassBoost?.setStrength(bassBoostStrength.toShort()) } catch (e: Exception) {
                    Log.w("AudioFxViewModel", "Failed to apply default preset to bass boost", e)
                }
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
