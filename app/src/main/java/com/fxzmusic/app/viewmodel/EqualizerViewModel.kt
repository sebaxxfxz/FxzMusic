package com.fxzmusic.app.viewmodel
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.util.*

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EqualizerViewModel : ViewModel() {

    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    private var appContext: Context? = null

    private var hardwareEq: Equalizer? = null
    private var currentAudioSessionId = 0

    var isEnabled by mutableStateOf(true)
        private set

    var currentProfile by mutableStateOf(PRESET_PROFILES.first())
        private set

    var customProfiles by mutableStateOf<List<EqProfile>>(emptyList())
        private set

    val allProfiles get() = PRESET_PROFILES + customProfiles

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences("eq_prefs", Context.MODE_PRIVATE)
        isEnabled = prefs?.getBoolean("eq_enabled", true) ?: true
        loadSavedProfiles()
        loadSavedProfile()
    }

    fun attachToAudioSession(sessionId: Int) {
        if (sessionId == 0 || sessionId == currentAudioSessionId) return
        currentAudioSessionId = sessionId
        try {
            hardwareEq?.release()
            hardwareEq = Equalizer(0, sessionId).apply {
                enabled = isEnabled
            }
            applyCurrentProfileToHardware()
            hardwareEq?.let { Log.i("EqualizerViewModel", "EQ attached to session $sessionId: ${it.numberOfBands} bands") }
        } catch (e: Exception) {
            Log.w("EqualizerViewModel", "Failed to attach to audio session $sessionId — EQ disabled", e)
            hardwareEq = null
        }
    }

    fun toggleEnabled() {
        isEnabled = !isEnabled
        prefs?.let { it.edit { putBoolean(EXTRA_EQ_ENABLED, isEnabled) } }
        try { hardwareEq?.enabled = isEnabled } catch (e: Exception) { Log.w("EqualizerViewModel", "Failed to toggle EQ", e) }
        notifyService()
    }

    fun selectProfile(profile: EqProfile) {
        currentProfile = profile
        prefs?.let { it.edit { putString(EXTRA_EQ_PROFILE, profile.id) } }
        applyCurrentProfileToHardware()
        notifyService()
    }

    fun updateBand(bandIndex: Int, gainDb: Float) {
        val bands = currentProfile.bands
        if (bandIndex < 0 || bandIndex >= bands.size) return
        val newBands = bands.toMutableList()
        newBands[bandIndex] = newBands[bandIndex].copy(gainDb = gainDb)
        val updatedProfile = currentProfile.copy(bands = newBands)

        if (updatedProfile.isCustom && customProfiles.any { it.id == updatedProfile.id }) {
            customProfiles = customProfiles.map {
                if (it.id == updatedProfile.id) updatedProfile else it
            }
            currentProfile = updatedProfile
            saveProfilesToPrefs()
            prefs?.let { it.edit { putString(EXTRA_EQ_PROFILE, updatedProfile.id) } }
        } else {
            currentProfile = updatedProfile.copy(isCustom = true)
            saveCurrentBandsAsTemp()
        }
        applyCurrentProfileToHardware()
        notifyService()
    }

    fun saveCustomProfile(name: String) {
        if (name.isBlank()) return
        val newProfile = currentProfile.copy(
            id       = "custom_${System.currentTimeMillis()}",
            name     = name,
            isCustom = true
        )
        customProfiles = customProfiles + newProfile
        currentProfile = newProfile
        saveProfilesToPrefs()
        prefs?.let { it.edit { putString(EXTRA_EQ_PROFILE, newProfile.id) } }
        notifyService()
    }

    fun deleteCustomProfile(profile: EqProfile) {
        if (!profile.isCustom) return
        customProfiles = customProfiles.filter { it.id != profile.id }
        if (currentProfile.id == profile.id) selectProfile(PRESET_PROFILES.first())
        saveProfilesToPrefs()
    }

    private fun applyCurrentProfileToHardware() {
        val eq = hardwareEq ?: return
        if (!isEnabled) return
        val numberOfBands = eq.numberOfBands.toInt()
        currentProfile.bands.forEach { band ->
            if (band.index < numberOfBands) {
                try { eq.setBandLevel(band.index.toShort(), (band.gainDb * 100).toInt().toShort()) } catch (e: Exception) { Log.w("EqualizerViewModel", "Failed to set band ${band.index}", e) }
            }
        }
    }

    private fun saveCurrentBandsAsTemp() {
        val json = gson.toJson(currentProfile.bands)
        prefs?.let {
            it.edit {
                putString("temp_bands", json)
                putString(EXTRA_EQ_PROFILE, "__temp__")
            }
        }
    }

    private fun notifyService() {
        val ctx = appContext ?: return
        val intent = Intent(ACTION_EQ_UPDATE).apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(intent)
    }

    private fun saveProfilesToPrefs() {
        prefs?.let { it.edit { putString("custom_profiles", gson.toJson(customProfiles)) } }
    }

    private fun loadSavedProfiles() {
        val json = prefs?.getString("custom_profiles", null) ?: return
        val type = object : TypeToken<List<EqProfile>>() {}.type
        try {
            customProfiles = gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w("EqualizerViewModel", "Failed to load saved profiles", e)
        }
    }

    private fun loadSavedProfile() {
        val savedId = prefs?.getString(EXTRA_EQ_PROFILE, "flat") ?: "flat"
        currentProfile = allProfiles.find { it.id == savedId } ?: PRESET_PROFILES.first()
    }

    override fun onCleared() {
        super.onCleared()
        try { hardwareEq?.release() } catch (e: Exception) { Log.w("EqualizerViewModel", "Failed to release EQ", e) }
    }
}
