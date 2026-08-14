package com.fxzmusic.app.viewmodel
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.service.*
import com.fxzmusic.app.util.*

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.util.Log

class AudioFxViewModel : ViewModel() {

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

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
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences("audio_fx_prefs", Context.MODE_PRIVATE)
        virtualizerEnabled  = prefs?.getBoolean("virtualizer_on", false) ?: false
        virtualizerStrength = prefs?.getInt("virtualizer_strength", 800) ?: 800
        bassBoostEnabled    = prefs?.getBoolean("bass_on", false) ?: false
        bassBoostStrength   = prefs?.getInt("bass_strength", 500) ?: 500
        selectedPreset      = prefs?.getString("preset", "Normal") ?: "Normal"
    }

    fun toggleVirtualizer(context: Context) {
        virtualizerEnabled = !virtualizerEnabled
        prefs?.edit()?.putBoolean("virtualizer_on", virtualizerEnabled)?.apply()
        notifyService()
    }

    fun updateVirtualizerStrength(strength: Int) {
        virtualizerStrength = strength
        prefs?.edit()?.putInt("virtualizer_strength", strength)?.apply()
        notifyService()
    }

    fun toggleBassBoost(context: Context) {
        bassBoostEnabled = !bassBoostEnabled
        prefs?.edit()?.putBoolean("bass_on", bassBoostEnabled)?.apply()
        notifyService()
    }

    fun updateBassBoostStrength(strength: Int) {
        bassBoostStrength = strength
        prefs?.edit()?.putInt("bass_strength", strength)?.apply()
        notifyService()
    }

    fun selectPreset(context: Context, preset: String) {
        selectedPreset = preset
        prefs?.edit()?.putString("preset", preset)?.apply()
        notifyService()
    }

    private fun notifyService() {
        val ctx = appContext ?: return
        val intent = Intent(ACTION_FX_UPDATE).apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(intent)
    }
}
