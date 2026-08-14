package com.fxzmusic.app.service

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.fxzmusic.app.data.EXTRA_EQ_ENABLED
import com.fxzmusic.app.data.EXTRA_EQ_PROFILE
import com.fxzmusic.app.data.EqBand
import com.fxzmusic.app.data.EqProfile
import com.fxzmusic.app.data.PRESET_PROFILES
import com.fxzmusic.app.data.applyProfileToHardwareEqualizer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AudioEffectManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioEffectManager"
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    var currentAudioSessionId: Int = 0
        private set

    private val gson = Gson()

    fun attachSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == currentAudioSessionId && equalizer != null) {
            applyAllFromPrefs()
            return
        }

        release()
        currentAudioSessionId = audioSessionId

        // Equalizer
        runCatching {
            equalizer = Equalizer(0, audioSessionId)
        }.onFailure { e ->
            Log.w(TAG, "Failed to instantiate Equalizer on session $audioSessionId", e)
            equalizer = null
        }

        // BassBoost
        runCatching {
            bassBoost = BassBoost(0, audioSessionId)
        }.onFailure { e ->
            Log.w(TAG, "Failed to instantiate BassBoost on session $audioSessionId", e)
            bassBoost = null
        }

        // Virtualizer
        runCatching {
            virtualizer = Virtualizer(0, audioSessionId)
        }.onFailure { e ->
            Log.w(TAG, "Failed to instantiate Virtualizer on session $audioSessionId", e)
            virtualizer = null
        }

        // LoudnessEnhancer
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to instantiate LoudnessEnhancer on session $audioSessionId", e)
            loudnessEnhancer = null
        }

        // PresetReverb
        runCatching {
            presetReverb = PresetReverb(0, audioSessionId)
        }.onFailure { e ->
            Log.w(TAG, "Failed to instantiate PresetReverb on session $audioSessionId", e)
            presetReverb = null
        }

        applyAllFromPrefs()
    }

    fun applyAllFromPrefs() {
        val eqPrefs = context.getSharedPreferences("eq_prefs", Context.MODE_PRIVATE)
        val fxPrefs = context.getSharedPreferences("audio_fx_prefs", Context.MODE_PRIVATE)
        applyEqFromPrefs(eqPrefs)
        applyFxFromPrefs(fxPrefs)
    }

    fun applyEqFromPrefs(eqPrefs: SharedPreferences) {
        val eq = equalizer ?: return
        val isEnabled = eqPrefs.getBoolean(EXTRA_EQ_ENABLED, true)
        runCatching {
            eq.enabled = isEnabled
        }.onFailure { e ->
            Log.w(TAG, "Failed to set Equalizer enabled state", e)
        }

        if (!isEnabled) return

        val profileId = eqPrefs.getString(EXTRA_EQ_PROFILE, "flat") ?: "flat"
        val bands: List<EqBand>? = when (profileId) {
            "__temp__" -> {
                val json = eqPrefs.getString("temp_bands", null)
                if (json != null) {
                    val type = object : TypeToken<List<EqBand>>() {}.type
                    try {
                        gson.fromJson(json, type)
                    } catch (_: Exception) {
                        null
                    }
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
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }
            }
        }

        if (bands != null) {
            applyProfileToHardwareEqualizer(eq, bands)
        }
    }

    fun applyFxFromPrefs(fxPrefs: SharedPreferences) {
        val virtualizerOn = fxPrefs.getBoolean("virtualizer_on", false)
        val virtualizerStrength = fxPrefs.getInt("virtualizer_strength", 800)
        val bassOn = fxPrefs.getBoolean("bass_on", false)
        val bassStrength = fxPrefs.getInt("bass_strength", 500)
        val preset = fxPrefs.getString("preset", "Normal") ?: "Normal"

        // Virtualizer
        runCatching {
            virtualizer?.let { virt ->
                val shouldEnable = virtualizerOn || preset != "Normal"
                virt.enabled = shouldEnable
                if (shouldEnable) {
                    val strength = when (preset) {
                        "Concierto" -> 900
                        "Estadio" -> 1000
                        "Club" -> 700
                        "Sala" -> 500
                        else -> virtualizerStrength
                    }
                    virt.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to apply Virtualizer settings", e)
        }

        // BassBoost
        runCatching {
            bassBoost?.let { bb ->
                val shouldEnable = if (preset == "Sala") false else (bassOn || preset != "Normal")
                bb.enabled = shouldEnable
                if (shouldEnable) {
                    val strength = when (preset) {
                        "Concierto" -> 600
                        "Estadio" -> 800
                        "Club" -> 700
                        else -> bassStrength
                    }
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to apply BassBoost settings", e)
        }

        // PresetReverb
        runCatching {
            presetReverb?.let { pr ->
                val reverbPreset = when (preset) {
                    "Concierto" -> PresetReverb.PRESET_LARGEHALL
                    "Estadio" -> PresetReverb.PRESET_PLATE
                    "Club" -> PresetReverb.PRESET_MEDIUMHALL
                    "Sala" -> PresetReverb.PRESET_SMALLROOM
                    else -> PresetReverb.PRESET_NONE
                }
                pr.enabled = reverbPreset != PresetReverb.PRESET_NONE
                if (reverbPreset != PresetReverb.PRESET_NONE) {
                    pr.preset = reverbPreset
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to apply PresetReverb settings", e)
        }
    }

    fun setLoudnessEnhancerGain(gainMb: Int, enabled: Boolean) {
        runCatching {
            loudnessEnhancer?.let { le ->
                le.enabled = enabled
                if (enabled) {
                    le.setTargetGain(gainMb.coerceIn(0, 600))
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to set LoudnessEnhancer target gain", e)
        }
    }

    fun release() {
        runCatching { equalizer?.release() }.onFailure { Log.w(TAG, "Error releasing Equalizer", it) }
        runCatching { bassBoost?.release() }.onFailure { Log.w(TAG, "Error releasing BassBoost", it) }
        runCatching { virtualizer?.release() }.onFailure { Log.w(TAG, "Error releasing Virtualizer", it) }
        runCatching { loudnessEnhancer?.release() }.onFailure { Log.w(TAG, "Error releasing LoudnessEnhancer", it) }
        runCatching { presetReverb?.release() }.onFailure { Log.w(TAG, "Error releasing PresetReverb", it) }

        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
        currentAudioSessionId = 0
    }
}
