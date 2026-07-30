package com.fxzmusic.app.service.equalizer

import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.util.Log

object EqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var sessionId: Int = -1

    private var enabled = false
    private var bands = mutableListOf<Short>()
    private var bassStrength: Short = 0
    private var virtualizerStrength: Short = 0

    @Synchronized
    fun initialize(audioSessionId: Int) {
        release()
        sessionId = audioSessionId
        bands.clear()
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = this@EqualizerManager.enabled
                val numBands = numberOfBands
                for (i in 0 until numBands) {
                    bands.add(getBandLevel(i.toShort()))
                }
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = this@EqualizerManager.enabled
                setStrength(bassStrength)
            }
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = this@EqualizerManager.enabled
                setStrength(virtualizerStrength)
            }
        } catch (e: Exception) {
            Log.w("EqualizerManager", "Failed to init equalizer", e)
        }
    }

    @Synchronized
    fun release() {
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
    }

    @Synchronized
    fun isEnabled() = enabled

    @Synchronized
    fun getBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 0

    @Synchronized
    fun getBandFrequencies(): List<Pair<Float, Float>> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfBands).map { i ->
            val freqRange = eq.getBandFreqRange(i.toShort())
            Pair(freqRange[0].toFloat(), freqRange[1].toFloat())
        }
    }

    @Synchronized
    fun getMinBandLevel(): Short = equalizer?.bandLevelRange?.get(0)?.toShort() ?: -1500
    @Synchronized
    fun getMaxBandLevel(): Short = equalizer?.bandLevelRange?.get(1)?.toShort() ?: 1500

    @Synchronized
    fun setBandLevel(band: Int, level: Short) {
        if (band in bands.indices) bands[band] = level
        try {
            equalizer?.setBandLevel(band.toShort(), level)
        } catch (e: Exception) {
            Log.w("EqualizerManager", "Failed to set band level", e)
        }
    }

    @Synchronized
    fun getBandLevel(band: Int): Short = if (band in bands.indices) bands[band] else 0

    @Synchronized
    fun setBassStrength(strength: Short) {
        bassStrength = strength
        bassBoost?.setStrength(strength)
    }

    @Synchronized
    fun getBassStrength(): Short = bassStrength

    @Synchronized
    fun setVirtualizerStrength(strength: Short) {
        virtualizerStrength = strength
        virtualizer?.setStrength(strength)
    }

    @Synchronized
    fun getVirtualizerStrength(): Short = virtualizerStrength

    @Synchronized
    fun getPresetNames(): List<String> {
        val count = equalizer?.numberOfPresets?.toInt() ?: 0
        return (0 until count).mapNotNull { equalizer?.getPresetName(it.toShort()) }
    }

    @Synchronized
    fun applyPreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            val numBands = equalizer?.numberOfBands?.toInt() ?: return
            bands.clear()
            for (i in 0 until numBands) {
                bands.add(equalizer?.getBandLevel(i.toShort()) ?: 0)
            }
        } catch (e: Exception) {
            Log.w("EqualizerManager", "Failed to apply preset", e)
        }
    }

    @Synchronized
    fun reset() {
        val numBands = equalizer?.numberOfBands?.toInt() ?: return
        for (i in 0 until numBands) {
            setBandLevel(i, 0)
        }
        setBassStrength(0)
        setVirtualizerStrength(0)
    }
}
