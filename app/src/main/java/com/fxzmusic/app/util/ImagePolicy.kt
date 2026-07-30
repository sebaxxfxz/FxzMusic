package com.fxzmusic.app.util
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.media.MediaMetadataRetriever
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import java.io.File

fun Context.isWifiConnected(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun String?.toHighResThumbnailUrl(): String? {
    if (this == null || isBlank()) return this
    if (contains("googleusercontent.com") || contains("ggpht.com")) {
        return this
            .replace(Regex("""=w\d+-h\d+.*"""), "=w1200-h1200-p-k-no-nd")
            .replace(Regex("""=s\d+.*"""), "=s1200")
            .replace(Regex("""=w\d+"""), "=w1200")
    }
    return this
}

fun buildCoverRequest(context: Context, song: Song): ImageRequest {
    val embeddedCoverPath = getEmbeddedCoverFilePath(context, song)
    val rawCover = if (embeddedCoverPath != null) {
        embeddedCoverPath
    } else {
        song.coverUrl
    }
    val coverData = rawCover.toHighResThumbnailUrl()

    val wifiOnly = context.applicationContext
        .getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        .getBoolean("wifi_only_covers", true)

    val networkPolicy = if (!wifiOnly || context.isWifiConnected()) {
        CachePolicy.ENABLED
    } else {
        CachePolicy.READ_ONLY
    }

    return ImageRequest.Builder(context)
        .data(coverData)
        .crossfade(true)
        .size(Size.ORIGINAL)
        .networkCachePolicy(networkPolicy)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}

fun buildCoverRequest(context: Context, coverUrl: String?): ImageRequest {
    val wifiOnly = context.applicationContext
        .getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        .getBoolean("wifi_only_covers", true)

    val networkPolicy = if (!wifiOnly || context.isWifiConnected()) {
        CachePolicy.ENABLED
    } else {
        CachePolicy.READ_ONLY
    }

    return ImageRequest.Builder(context)
        .data(coverUrl.toHighResThumbnailUrl())
        .crossfade(true)
        .size(Size.ORIGINAL)
        .networkCachePolicy(networkPolicy)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}

private fun getEmbeddedCoverFilePath(context: Context, song: Song): String? {
    val sourceFile = song.filePath.trim().takeIf { it.isNotEmpty() }?.let(::File) ?: return null
    if (!sourceFile.exists() || !sourceFile.isFile) return null

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(sourceFile.absolutePath)
        val embeddedPicture = retriever.embeddedPicture ?: return null
        if (embeddedPicture.isEmpty()) return null

        val cacheDir = File(context.cacheDir, "embedded_covers").apply { mkdirs() }
        val safeKey = buildString {
            append(song.id.ifBlank { sourceFile.nameWithoutExtension.ifBlank { "track" } })
            append('_')
            append(sourceFile.length())
            append('_')
            append(sourceFile.lastModified())
        }.replace(Regex("[^A-Za-z0-9._-]"), "_")

        val outFile = File(cacheDir, "$safeKey.cover")
        if (!outFile.exists() || outFile.length() != embeddedPicture.size.toLong()) {
            outFile.writeBytes(embeddedPicture)
        }
        outFile.absolutePath
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

