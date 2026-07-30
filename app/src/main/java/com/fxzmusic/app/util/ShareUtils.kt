package com.fxzmusic.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fxzmusic.app.data.Song
import java.io.File

object ShareUtils {

    fun shareSong(context: Context, song: Song) {
        val file = File(song.filePath)
        val intent = if (song.filePath.isNotBlank() && file.exists() && file.canRead()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
            }
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Compartir canción"))
        } catch (e: android.content.ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}
