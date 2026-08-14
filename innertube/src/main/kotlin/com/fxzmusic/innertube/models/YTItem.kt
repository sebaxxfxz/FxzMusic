package com.fxzmusic.innertube.models

import androidx.compose.runtime.Immutable
import com.fxzmusic.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed class YTItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnail: String?
    abstract val explicit: Boolean
    abstract val shareLink: String
}

@Immutable
@Serializable
data class Artist(
    val name: String,
    val id: String?,
)

@Immutable
@Serializable
data class Album(
    val name: String,
    val id: String,
)

@Immutable
@Serializable
data class SongItem(
    override val id: String,
    override val title: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val duration: Int? = null,
    val musicVideoType: String? = null,
    val chartPosition: Int? = null,
    val chartChange: String? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val endpoint: WatchEndpoint? = null,
    val setVideoId: String? = null,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
    val historyRemoveToken: String? = null,
) : YTItem() {

    val isVideoSong: Boolean
        get() = musicVideoType != null && musicVideoType != MUSIC_VIDEO_TYPE_ATV

    override val shareLink: String
        get() = "https://share.fxzmusic.app/watch?v=$id"
}

@Immutable
@Serializable
data class AlbumItem(
    val browseId: String,
    val playlistId: String,
    override val id: String = browseId,
    override val title: String,
    val artists: List<Artist>?,
    val year: Int? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val description: String? = null,
) : YTItem() {
    override val shareLink: String
        get() = "https://share.fxzmusic.app/playlist?list=$playlistId"
}

@Immutable
@Serializable
data class PlaylistItem(
    override val id: String,
    override val title: String,
    val author: Artist?,
    val songCountText: String?,
    override val thumbnail: String?,
    val playEndpoint: WatchEndpoint?,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
    val isEditable: Boolean = false,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://share.fxzmusic.app/playlist?list=$id"
}

@Immutable
@Serializable
data class ArtistItem(
    override val id: String,
    override val title: String,
    override val thumbnail: String?,
    val channelId: String? = null,
    val playEndpoint: WatchEndpoint? = null,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://share.fxzmusic.app/channel/$id"
}

fun <T : YTItem> List<T>.filterExplicit(enabled: Boolean = true) =
    when (enabled) {
        true -> filter { !it.explicit }
        false -> this
    }

fun <T : YTItem> List<T>.filterVideoSongs(disableVideos: Boolean = false) =
    takeIf { !disableVideos } ?: filterNot { it is SongItem && it.isVideoSong }

fun <T : YTItem> List<T>.filterYoutubeShorts(enabled: Boolean = false) =
    takeIf { !enabled } ?: filterNot { it is PlaylistItem && it.id.startsWith("SS") }
