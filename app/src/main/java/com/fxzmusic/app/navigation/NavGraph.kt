package com.fxzmusic.app.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import java.net.URLDecoder

fun NavController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

val subScreenEnterTransition = slideInHorizontally(
    initialOffsetX = { it / 5 },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(tween(260))

val subScreenExitTransition = slideOutHorizontally(
    targetOffsetX = { -it / 5 },
    animationSpec = tween(220, easing = FastOutSlowInEasing)
) + fadeOut(tween(200))

val subScreenPopEnterTransition = slideInHorizontally(
    initialOffsetX = { -it / 5 },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(tween(260))

val subScreenPopExitTransition = slideOutHorizontally(
    targetOffsetX = { it / 5 },
    animationSpec = tween(220, easing = FastOutSlowInEasing)
) + fadeOut(tween(200))

@Composable
fun LibraryNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    libraryContent: @Composable (navController: NavHostController) -> Unit,
    albumContent: @Composable (albumName: String, navController: NavHostController) -> Unit,
    artistContent: @Composable (artistName: String, navController: NavHostController) -> Unit,
    folderContent: @Composable (folderPath: String, navController: NavHostController) -> Unit,
    playlistContent: @Composable (playlistId: Long, navController: NavHostController) -> Unit,
    favoritesContent: @Composable (navController: NavHostController) -> Unit,
    downloadsContent: @Composable (navController: NavHostController) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        modifier = modifier,
        enterTransition = { subScreenEnterTransition },
        exitTransition = { subScreenExitTransition },
        popEnterTransition = { subScreenPopEnterTransition },
        popExitTransition = { subScreenPopExitTransition }
    ) {
        composable<LibraryRoute> {
            libraryContent(navController)
        }
        composable<AlbumRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumRoute>()
            albumContent(URLDecoder.decode(route.albumName, "UTF-8"), navController)
        }
        composable<ArtistRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ArtistRoute>()
            artistContent(route.artistName, navController)
        }
        composable<FolderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FolderRoute>()
            folderContent(route.folderPath, navController)
        }
        composable<PlaylistRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PlaylistRoute>()
            playlistContent(route.playlistId, navController)
        }
        composable<FavoritesRoute> {
            favoritesContent(navController)
        }
        composable<DownloadsRoute> {
            downloadsContent(navController)
        }
    }
}

@Composable
fun YouTubeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    homeContent: @Composable (navController: NavHostController) -> Unit,
    searchContent: @Composable (navController: NavHostController) -> Unit,
    albumContent: @Composable (browseId: String, navController: NavHostController) -> Unit,
    artistContent: @Composable (browseId: String, navController: NavHostController) -> Unit,
    playlistContent: @Composable (playlistId: String, navController: NavHostController) -> Unit,
    categoryContent: @Composable (category: String, navController: NavHostController) -> Unit,
    syncContent: @Composable (navController: NavHostController) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = YouTubeHomeRoute,
        modifier = modifier,
        enterTransition = { subScreenEnterTransition },
        exitTransition = { subScreenExitTransition },
        popEnterTransition = { subScreenPopEnterTransition },
        popExitTransition = { subScreenPopExitTransition }
    ) {
        composable<YouTubeHomeRoute> {
            homeContent(navController)
        }
        composable<YouTubeSearchRoute> {
            searchContent(navController)
        }
        composable<YouTubeAlbumRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<YouTubeAlbumRoute>()
            albumContent(route.browseId, navController)
        }
        composable<YouTubeArtistRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<YouTubeArtistRoute>()
            artistContent(route.browseId, navController)
        }
        composable<YouTubePlaylistRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<YouTubePlaylistRoute>()
            playlistContent(route.playlistId, navController)
        }
        composable<YouTubeCategoryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<YouTubeCategoryRoute>()
            categoryContent(route.category, navController)
        }
        composable<SyncRoute> {
            syncContent(navController)
        }
    }
}
