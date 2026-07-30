package com.fxzmusic.app.service

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.fxzmusic.app.data.Song
import com.fxzmusic.innertube.models.SongItem
import java.io.File

fun Song.toMediaItem(): MediaItem {
    val uri = when {
        isYouTube && !youtubeVideoId.isNullOrEmpty() -> "yt://$youtubeVideoId"
        filePath.isNotEmpty() -> filePath
        else -> null
    }
    val artworkUri: Uri? = when {
        !coverUrl.isNullOrEmpty() -> coverUrl.toUri()
        filePath.isNotEmpty() -> Uri.fromFile(File(filePath))
        else -> null
    }
    val mediaKey = if (isYouTube) youtubeVideoId ?: id else null

    val meta = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setIsBrowsable(false)
        .setIsPlayable(true)
    if (artworkUri != null) meta.setArtworkUri(artworkUri)

    val builder = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(meta.build())

    if (mediaKey != null) {
        builder.setCustomCacheKey(mediaKey)
    }
    return builder.build()
}

fun SongItem.toMediaItem(): MediaItem {
    val mediaId = id
    val artworkUri = thumbnail.toUri()
    val meta = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString { it.name })
        .setAlbumTitle(album?.name)
        .setIsBrowsable(false)
        .setIsPlayable(true)
    if (artworkUri != null) meta.setArtworkUri(artworkUri)
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri("yt://$mediaId")
        .setCustomCacheKey(mediaId)
        .setMediaMetadata(meta.build())
        .build()
}

fun SongItem.toSong(): Song = Song(
    id = id,
    title = title,
    artist = artists.joinToString { it.name },
    album = album?.name ?: "",
    duration = duration ?: 0,
    filePath = "",
    coverUrl = thumbnail,
    isYouTube = true,
    youtubeVideoId = id,
    youtubeThumbnailUrl = thumbnail
)
