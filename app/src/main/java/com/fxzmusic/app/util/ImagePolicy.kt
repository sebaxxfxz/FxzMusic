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

fun String?.toHighResThumbnailUrl(maxSize: Int = 640): String? {
    if (this == null || isBlank()) return this
    if (contains("googleusercontent.com") || contains("ggpht.com")) {
        return this
            .replace(Regex("""=w\d+-h\d+.*"""), "=w$maxSize-h$maxSize-p-k-no-nd")
            .replace(Regex("""=s\d+.*"""), "=s$maxSize")
            .replace(Regex("""=w\d+"""), "=w$maxSize")
    }
    return this
}

fun buildCoverRequest(context: Context, song: Song, maxSize: Int = 512): ImageRequest {
    val model: Any? = when {
        !song.coverUrl.isNullOrBlank() -> song.coverUrl.toHighResThumbnailUrl(maxSize)
        song.filePath.isNotBlank() -> EmbeddedCoverModel(filePath = song.filePath, songId = song.id)
        else -> null
    }

    val wifiOnly = context.applicationContext
        .getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        .getBoolean("wifi_only_covers", true)

    val networkPolicy = if (!wifiOnly || context.isWifiConnected()) {
        CachePolicy.ENABLED
    } else {
        CachePolicy.READ_ONLY
    }

    return ImageRequest.Builder(context)
        .data(model)
        .crossfade(true)
        .size(maxSize)
        .networkCachePolicy(networkPolicy)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}

fun buildCoverRequest(context: Context, coverUrl: String?, maxSize: Int = 512): ImageRequest {
    val wifiOnly = context.applicationContext
        .getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
        .getBoolean("wifi_only_covers", true)

    val networkPolicy = if (!wifiOnly || context.isWifiConnected()) {
        CachePolicy.ENABLED
    } else {
        CachePolicy.READ_ONLY
    }

    return ImageRequest.Builder(context)
        .data(coverUrl.toHighResThumbnailUrl(maxSize))
        .crossfade(true)
        .size(maxSize)
        .networkCachePolicy(networkPolicy)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}


