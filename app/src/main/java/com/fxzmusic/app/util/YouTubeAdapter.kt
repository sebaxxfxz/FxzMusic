package com.fxzmusic.app.util

import com.fxzmusic.app.data.Song
import com.fxzmusic.innertube.models.SongItem
import com.fxzmusic.innertube.models.YTItem

fun SongItem.toSong(): Song = Song(
    id = this.id,
    title = this.title,
    artist = this.artists.firstOrNull()?.name ?: "Artista desconocido",
    album = this.album?.name ?: "YouTube Music",
    duration = this.duration ?: 0,
    filePath = "",
    coverUrl = this.thumbnail,
    lastPlayed = 0L,
    playCount = 0,
    isLiked = false,
    dateAdded = System.currentTimeMillis(),
    isYouTube = true,
    youtubeVideoId = this.id,
    youtubeThumbnailUrl = this.thumbnail,
)

fun List<YTItem>.toSongs(): List<Song> = mapNotNull { item ->
    (item as? SongItem)?.toSong()
}

fun SongItem.toLocalSong(): Song = toSong()

fun List<SongItem>.toLocalSongs(): List<Song> = map(SongItem::toSong)
