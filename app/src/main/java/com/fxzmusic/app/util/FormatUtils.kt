package com.fxzmusic.app.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

fun formatDuration(ms: Long): String {
    val clamped = ms.coerceAtLeast(0)
    val hours = clamped / 3_600_000
    val minutes = (clamped % 3_600_000) / 60_000
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

fun formatMsToMsS(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun generateAlbumArt(seed: Long): List<Color> {
    val hue1 = (seed.hashCode().and(0xFF).toFloat() / 255f) * 360f
    val hue2 = (hue1 + 40f) % 360f
    val hue3 = (hue1 + 80f) % 360f

    fun hslToColor(h: Float): Color {
        val s = 0.6f
        val l = 0.45f
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r + m, g + m, b + m)
    }

    return listOf(hslToColor(hue1), hslToColor(hue2), hslToColor(hue3))
}

fun dynamicContrastColor(
    background: Color,
    targetColor: Color,
    minContrast: Float = 4.5f
): Color {
    val bgLuminance = relativeLuminance(background)
    val targetLuminance = relativeLuminance(targetColor)
    val contrast = (maxOf(bgLuminance, targetLuminance) + 0.05f) /
            (minOf(bgLuminance, targetLuminance) + 0.05f)
    return if (contrast >= minContrast) {
        targetColor
    } else {
        val dark = Color(0xFF0A0A0A)
        val light = Color(0xFFFCFCFC)
        val darkContrast = (maxOf(bgLuminance, relativeLuminance(dark)) + 0.05f) /
                (minOf(bgLuminance, relativeLuminance(dark)) + 0.05f)
        val lightContrast = (maxOf(bgLuminance, relativeLuminance(light)) + 0.05f) /
                (minOf(bgLuminance, relativeLuminance(light)) + 0.05f)
        if (darkContrast >= lightContrast) dark else light
    }
}

private fun relativeLuminance(color: Color): Float {
    fun channelLinear(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val r = channelLinear(color.red)
    val g = channelLinear(color.green)
    val b = channelLinear(color.blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
