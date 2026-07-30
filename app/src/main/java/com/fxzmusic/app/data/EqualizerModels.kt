package com.fxzmusic.app.data
import com.fxzmusic.app.*

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
    )
)
