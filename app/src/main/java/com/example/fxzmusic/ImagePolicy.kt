package com.example.fxzmusic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import coil.request.CachePolicy
import coil.request.ImageRequest

fun Context.isWifiConnected(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun buildCoverRequest(context: Context, song: Song): ImageRequest {
    // First check for embedded cover in the audio file metadata
    val embeddedCoverPath = MetadataUtils.getEmbeddedCoverFilePath(context, song)
    val coverData = if (embeddedCoverPath != null) {
        embeddedCoverPath
    } else {
        song.coverUrl
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
        .data(coverData)
        .crossfade(true)
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
        .data(coverUrl)
        .crossfade(true)
        .networkCachePolicy(networkPolicy)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}

