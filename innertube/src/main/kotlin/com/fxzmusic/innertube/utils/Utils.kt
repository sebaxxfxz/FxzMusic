package com.fxzmusic.innertube.utils

import com.fxzmusic.innertube.YouTube
import com.fxzmusic.innertube.pages.LibraryPage
import com.fxzmusic.innertube.pages.PlaylistPage
import java.security.MessageDigest

private const val MAX_PAGINATION_REQUESTS = 50
private const val MAX_CONSECUTIVE_EMPTY_RESPONSES = 2

@JvmName("completedLibrary")
suspend fun Result<PlaylistPage>.completed(): Result<PlaylistPage> = runCatching {
    val page = getOrThrow()
    val songs = page.songs.toMutableList()
    var continuation = page.songsContinuation
    val visited = mutableSetOf<String>()
    var emptyStreak = 0

    while (continuation != null && visited.size < MAX_PAGINATION_REQUESTS) {
        if (!visited.add(continuation)) break
        val next = YouTube.playlistContinuation(continuation).getOrNull() ?: break
        if (next.songs.isEmpty()) {
            if (++emptyStreak >= MAX_CONSECUTIVE_EMPTY_RESPONSES) break
        } else {
            emptyStreak = 0
            songs += next.songs
        }
        continuation = next.continuation
    }
    PlaylistPage(
        playlist = page.playlist,
        songs = songs,
        songsContinuation = null,
        continuation = page.continuation,
    )
}

@JvmName("completedPlaylist")
suspend fun Result<LibraryPage>.completed(): Result<LibraryPage> = runCatching {
    val page = getOrThrow()
    val items = page.items.toMutableList()
    var continuation = page.continuation
    val visited = mutableSetOf<String>()
    var emptyStreak = 0

    while (continuation != null && visited.size < MAX_PAGINATION_REQUESTS) {
        if (!visited.add(continuation)) break
        val next = YouTube.libraryContinuation(continuation).getOrNull() ?: break
        if (next.items.isEmpty()) {
            if (++emptyStreak >= MAX_CONSECUTIVE_EMPTY_RESPONSES) break
        } else {
            emptyStreak = 0
            items += next.items
        }
        continuation = next.continuation
    }
    LibraryPage(items = items, continuation = null)
}

fun ByteArray.toHex(): String =
    joinToString(separator = "") { "%02x".format(it) }

fun sha1(str: String): String =
    MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

fun parseCookieString(cookie: String): Map<String, String> =
    buildMap {
        for (part in cookie.split("; ")) {
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            if (eq == -1) continue
            put(part.substring(0, eq), part.substring(eq + 1))
        }
    }

fun String.parseTime(): Int? = runCatching {
    val parts = split(":").map(String::toInt)
    when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> null
    }
}.getOrNull()

fun isPrivateId(browseId: String): Boolean =
    browseId.contains("privately")
