package com.fxzmusic.app.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.Color as AndroidColor

object ColorExtractor {
    private val cache = android.util.LruCache<String, List<Color>>(100)

    suspend fun extractDominantColors(context: Context, urlOrPath: String?): List<Color> {
        if (urlOrPath.isNullOrEmpty()) return defaultPalette()
        cache.get(urlOrPath)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(urlOrPath)
                    .allowHardware(false)
                    .size(256, 256)
                    .build()
                val result = loader.execute(request)
                var bitmap: android.graphics.Bitmap? = null
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        bitmap = drawable.bitmap
                    }
                }

                if (bitmap == null && !urlOrPath.startsWith("http") && File(urlOrPath).exists()) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(urlOrPath)
                        val art = retriever.embeddedPicture
                        retriever.release()
                        if (art != null) {
                            bitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size)
                        }
                    } catch (_: Exception) {}
                }

                if (bitmap != null) {
                    val colors = extractPaletteFromBitmap(bitmap)
                    cache.put(urlOrPath, colors)
                    colors
                } else {
                    defaultPalette()
                }
            } catch (e: Exception) {
                defaultPalette()
            }
        }
    }

    fun extractPaletteFromBitmap(bitmap: android.graphics.Bitmap): List<Color> {
        val palette = Palette.from(bitmap).maximumColorCount(24).generate()

        val swatches = mutableListOf<Palette.Swatch>()
        palette.vibrantSwatch?.let { swatches.add(it) }
        palette.darkVibrantSwatch?.let { swatches.add(it) }
        palette.lightVibrantSwatch?.let { swatches.add(it) }
        palette.mutedSwatch?.let { swatches.add(it) }
        palette.darkMutedSwatch?.let { swatches.add(it) }
        palette.lightMutedSwatch?.let { swatches.add(it) }
        palette.dominantSwatch?.let { swatches.add(it) }

        val sortedByPop = palette.swatches.sortedByDescending { it.population }
        for (swatch in sortedByPop) {
            if (swatches.none { it.rgb == swatch.rgb }) {
                swatches.add(swatch)
            }
        }

        if (swatches.isEmpty()) return defaultPalette()

        val distinctColors = mutableListOf<Color>()
        for (swatch in swatches) {
            val c = Color(swatch.rgb)
            if (isDistinct(c, distinctColors)) {
                distinctColors.add(c)
            }
            if (distinctColors.size >= 5) break
        }

        if (distinctColors.isNotEmpty()) {
            val baseColor = distinctColors.first()
            var step = 1
            while (distinctColors.size < 5) {
                val shifted = shiftHue(baseColor, step * 35f)
                distinctColors.add(shifted)
                step++
            }
        }

        return distinctColors.take(5)
    }

    private fun isDistinct(color: Color, existing: List<Color>): Boolean {
        val hsv1 = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv1)

        for (other in existing) {
            val hsv2 = FloatArray(3)
            AndroidColor.colorToHSV(other.toArgb(), hsv2)

            val hueDiff = Math.abs(hsv1[0] - hsv2[0]).let { if (it > 180) 360 - it else it }
            val satDiff = Math.abs(hsv1[1] - hsv2[1])
            val valDiff = Math.abs(hsv1[2] - hsv2[2])

            if (hueDiff < 20f && satDiff < 0.25f && valDiff < 0.25f) {
                return false
            }
        }
        return true
    }

    private fun shiftHue(color: Color, degrees: Float): Color {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        hsv[0] = (hsv[0] + degrees) % 360f
        if (hsv[0] < 0) hsv[0] += 360f
        hsv[1] = hsv[1].coerceAtLeast(0.35f)
        hsv[2] = hsv[2].coerceIn(0.3f, 0.85f)
        return Color(AndroidColor.HSVToColor(hsv))
    }

    private fun defaultPalette(): List<Color> = listOf(
        Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70), Color(0xFF6366F1), Color(0xFF22D3EE)
    )
}
