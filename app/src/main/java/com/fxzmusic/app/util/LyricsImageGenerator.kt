package com.fxzmusic.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object LyricsImageGenerator {

    data class LyricsImageConfig(
        val backgroundColor: Int = 0xFF1A1A1A.toInt(),
        val textColor: Int = 0xFFFFFFFF.toInt(),
        val secondaryTextColor: Int = 0xFFB3B3B3.toInt(),
        val useGradient: Boolean = true
    )

    fun createLyricsImage(
        context: Context,
        songTitle: String,
        artistName: String,
        lyricsText: String,
        coverBitmap: Bitmap? = null,
        config: LyricsImageConfig = LyricsImageConfig()
    ): Bitmap {
        val size = 1080
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = size / 340f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (config.useGradient && coverBitmap != null) {
            val scaledCover = Bitmap.createScaledBitmap(coverBitmap, size / 4, size / 4, true)
            val blurredPaint = Paint().apply {
                shader = android.graphics.BitmapShader(scaledCover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.drawBitmap(scaledCover, 0f, 0f, blurredPaint)
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), Paint().apply {
                color = 0xCC000000.toInt()
            })
        } else {
            bgPaint.color = config.backgroundColor
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x18FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
        }
        val borderRect = android.graphics.RectF(
            8f * scale, 8f * scale,
            size - 8f * scale, size - 8f * scale
        )
        canvas.drawRoundRect(borderRect, 20f * scale, 20f * scale, borderPaint)

        val padding = 28f * scale
        val coverSize = 64f * scale

        if (coverBitmap != null) {
            val scaledCover = Bitmap.createScaledBitmap(coverBitmap, coverSize.toInt(), coverSize.toInt(), true)
            val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val coverRect = android.graphics.RectF(padding, padding, padding + coverSize, padding + coverSize)
            canvas.drawRoundRect(coverRect, 3f * scale, 3f * scale, coverPaint)
            canvas.save()
            val coverPath = android.graphics.Path().apply {
                addRoundRect(coverRect, 3f * scale, 3f * scale, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(coverPath)
            canvas.drawBitmap(scaledCover, padding, padding, coverPaint)
            canvas.restore()

            val borderCoverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x29FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            }
            canvas.drawRoundRect(coverRect, 3f * scale, 3f * scale, borderCoverPaint)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.textColor
            textSize = 18f * scale
            isFakeBoldText = true
        }
        val titleX = padding + coverSize + 12f * scale
        val titleMaxWidth = size - titleX - padding

        val titleLines = breakText(songTitle, titlePaint, titleMaxWidth)
        var textY = padding + 20f * scale
        for (line in titleLines.take(2)) {
            canvas.drawText(line, titleX, textY, titlePaint)
            textY += titlePaint.textSize * 1.2f
        }

        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.secondaryTextColor
            textSize = 14f * scale
        }
        val artistLines = breakText(artistName, artistPaint, titleMaxWidth)
        for (line in artistLines.take(1)) {
            canvas.drawText(line, titleX, textY, artistPaint)
            textY += artistPaint.textSize * 1.4f
        }

        val lyricsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.textColor
            isFakeBoldText = true
        }
        val availableHeight = size - padding * 2 - coverSize - 40f * scale
        val lyricsTop = padding + coverSize + 40f * scale
        val lyricsBottom = size - padding - 40f * scale

        var fontSize = 40f * scale
        lyricsPaint.textSize = fontSize
        val lyricsLines = breakTextMultiLine(lyricsText, lyricsPaint, size - padding * 2)
        val neededHeight = lyricsLines.size * fontSize * 1.3f

        if (neededHeight > availableHeight) {
            fontSize *= availableHeight / neededHeight
            fontSize = fontSize.coerceAtLeast(10f * scale)
            lyricsPaint.textSize = fontSize
        }

        val finalLyricsLines = breakTextMultiLine(lyricsText, lyricsPaint, size - padding * 2)
        val totalLyricsHeight = finalLyricsLines.size * fontSize * 1.3f
        var lyricsY = lyricsTop + (availableHeight - totalLyricsHeight) / 2f

        for (line in finalLyricsLines) {
            canvas.drawText(line, size / 2f, lyricsY + fontSize, lyricsPaint)
            lyricsY += fontSize * 1.3f
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
            textSize = 10f * scale
        }
        canvas.drawText("FxzMusic", padding, size - 16f * scale, footerPaint)

        return bitmap
    }

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/fxzmusic/")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            uri
        } else {
            val dir = File(context.cacheDir, "images")
            dir.mkdirs()
            val file = File(dir, "$filename.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir letra"))
    }

    private fun breakText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private fun breakTextMultiLine(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isBlank()) {
                result.add("")
            } else {
                result.addAll(breakText(paragraph, paint, maxWidth))
            }
        }
        return result
    }
}
