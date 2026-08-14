package com.fxzmusic.app.data
import com.fxzmusic.app.*
import android.media.audiofx.Equalizer

data class EqBand(
    val index: Int,
    val frequencyHz: Int,
    val label: String,
    val gainDb: Float = 0f
)

data class EqProfile(
    val id: String,
    val name: String,
    val bands: List<EqBand>,
    val isCustom: Boolean = false
)

val DEFAULT_BANDS = listOf(
    EqBand(0,    32,  "32Hz"),
    EqBand(1,    64,  "64Hz"),
    EqBand(2,   125, "125Hz"),
    EqBand(3,   250, "250Hz"),
    EqBand(4,   500, "500Hz"),
    EqBand(5,  1000,   "1K"),
    EqBand(6,  2000,   "2K"),
    EqBand(7,  4000,   "4K"),
    EqBand(8,  8000,   "8K"),
    EqBand(9, 16000,  "16K")
)

val PRESET_PROFILES = listOf(
    EqProfile(
        id = "flat", name = "Plano",
        bands = DEFAULT_BANDS.map { it.copy(gainDb = 0f) }
    ),
    EqProfile(
        id = "bass_boost", name = "Bass Boost",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> 6f; 1 -> 5f; 2 -> 4f; 3 -> 2f; else -> 0f })
        }
    ),
    EqProfile(
        id = "electronic", name = "Electrónica",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> 5f; 1 -> 4f; 2 -> 1f; 4 -> -1f; 6 -> 3f; 7 -> 4f; 8 -> 3f; else -> 0f })
        }
    ),
    EqProfile(
        id = "acoustic", name = "Acústico",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 2 -> 3f; 3 -> 3f; 4 -> 2f; 5 -> 3f; 6 -> 3f; else -> 0f })
        }
    ),
    EqProfile(
        id = "vocal", name = "Voces",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> -2f; 1 -> -1f; 4 -> 3f; 5 -> 4f; 6 -> 3f; 7 -> 1f; else -> 0f })
        }
    ),
    EqProfile(
        id = "rock", name = "Rock",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> 4f; 1 -> 3f; 2 -> 1f; 5 -> -1f; 7 -> 2f; 8 -> 3f; 9 -> 3f; else -> 0f })
        }
    ),
    EqProfile(
        id = "hiphop", name = "Hip Hop",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> 5f; 1 -> 4f; 2 -> 1f; 3 -> 3f; 5 -> -1f; 8 -> 2f; else -> 0f })
        }
    ),
    EqProfile(
        id = "podcast", name = "Podcast",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> -3f; 1 -> -2f; 3 -> 2f; 4 -> 4f; 5 -> 4f; 6 -> 2f; 9 -> -1f; else -> 0f })
        }
    ),
    EqProfile(
        id = "pop", name = "Pop",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> -1f; 1 -> 1f; 2 -> 3f; 3 -> 4f; 4 -> 4f; 5 -> 2f; 6 -> 0f; 7 -> -1f; 8 -> 1f; 9 -> 2f; else -> 0f })
        }
    ),
    EqProfile(
        id = "jazz", name = "Jazz",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> 3f; 1 -> 2f; 2 -> 1f; 3 -> 2f; 4 -> -1f; 5 -> -1f; 6 -> 0f; 7 -> 1f; 8 -> 2f; 9 -> 3f; else -> 0f })
        }
    ),
    EqProfile(
        id = "classical", name = "Clásica",
        bands = DEFAULT_BANDS.mapIndexed { i, b ->
            b.copy(gainDb = when (i) { 0 -> 4f; 1 -> 3f; 2 -> 2f; 3 -> 1f; 4 -> -1f; 5 -> -1f; 6 -> 0f; 7 -> 2f; 8 -> 3f; 9 -> 3f; else -> 0f })
        }
    )
)

fun applyProfileToHardwareEqualizer(eq: Equalizer, bands: List<EqBand>) {
    runCatching {
        val numberOfBands = eq.numberOfBands.toInt()
        if (numberOfBands <= 0 || bands.isEmpty()) return@runCatching

        val bandLevelRange = runCatching { eq.bandLevelRange }.getOrNull()
        val minLevel = bandLevelRange?.getOrNull(0)?.toInt() ?: -1500
        val maxLevel = bandLevelRange?.getOrNull(1)?.toInt() ?: 1500

        for (i in 0 until numberOfBands) {
            val bandIndex = i.toShort()
            val centerFreqMhz = runCatching { eq.getCenterFreq(bandIndex) }.getOrNull() ?: continue
            val centerFreqHz = (centerFreqMhz / 1000.0).coerceAtLeast(1.0)
            val logCenterFreq = kotlin.math.ln(centerFreqHz)

            val closestBand = bands.minByOrNull { band ->
                val freqHz = band.frequencyHz.toDouble().coerceAtLeast(1.0)
                kotlin.math.abs(kotlin.math.ln(freqHz) - logCenterFreq)
            } ?: continue

            val gainMillibels = (closestBand.gainDb * 100).toInt()
            val clampedLevel = gainMillibels.coerceIn(minLevel, maxLevel).toShort()
            eq.setBandLevel(bandIndex, clampedLevel)
        }
    }.onFailure { e ->
        android.util.Log.w("EqualizerModels", "Failed to apply profile to equalizer", e)
    }
}
