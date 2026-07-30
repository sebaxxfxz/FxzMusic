package com.fxzmusic.app.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object SearchRoute
@Serializable object LibraryRoute
@Serializable object ProfileRoute
@Serializable object SettingsRoute

@Serializable data class AlbumRoute(val albumName: String)
@Serializable data class ArtistRoute(val artistName: String)
@Serializable data class FolderRoute(val folderPath: String)
@Serializable data class PlaylistRoute(val playlistId: Long)
@Serializable object FavoritesRoute

@Serializable object YouTubeHomeRoute
@Serializable object YouTubeSearchRoute
@Serializable data class YouTubeAlbumRoute(val browseId: String)
@Serializable data class YouTubeArtistRoute(val browseId: String)
@Serializable data class YouTubePlaylistRoute(val playlistId: String)
@Serializable data class YouTubeCategoryRoute(val category: String)
@Serializable object SyncRoute

@Serializable object EqualizerRoute
@Serializable object AudioFxRoute
@Serializable object ThemeRoute
@Serializable object SleepTimerRoute
@Serializable object FolderBlacklistRoute
@Serializable object HistoryRoute
@Serializable object AiPlaylistRoute
@Serializable object DownloadsRoute
