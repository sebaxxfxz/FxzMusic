package com.fxzmusic.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class UpdateInfo(
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val changelog: String,
    val apkUrl: String?,
    val apkSizeMb: String,
    val releaseDate: String
)

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/sebaxxfxz/FxzMusic/releases/latest"
    private const val TAG = "UpdateChecker"

    suspend fun checkForUpdates(context: Context): Result<UpdateInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }

                val url = URL(GITHUB_API_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "FxzMusicApp")
                }

                if (connection.responseCode != 200) {
                    return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
                }

                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONObject(jsonStr)

                val tagName = release.optString("tag_name", "")
                val cleanLatest = tagName.removePrefix("v").removePrefix("b").trim()
                val cleanCurrent = currentVersion.removePrefix("v").removePrefix("b").trim()

                val isNewer = isVersionNewer(cleanLatest, cleanCurrent)

                val body = release.optString("body", "Sin notas de versión de la actualización.")
                val publishedAt = release.optString("published_at", "")
                val formattedDate = formatDate(publishedAt)

                val assets = release.optJSONArray("assets")
                var apkUrl: String? = null
                var apkSizeMb = ""

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                            val bytes = asset.optLong("size", 0L)
                            if (bytes > 0) {
                                apkSizeMb = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
                            }
                            break
                        }
                    }
                }

                val info = UpdateInfo(
                    latestVersion = cleanLatest,
                    isUpdateAvailable = isNewer,
                    changelog = body,
                    apkUrl = apkUrl,
                    apkSizeMb = apkSizeMb,
                    releaseDate = formattedDate
                )

                Log.d(TAG, "Checked update: latest=$cleanLatest, current=$cleanCurrent, available=$isNewer")
                Result.success(info)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking updates", e)
                Result.failure(e)
            }
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        if (latest == current) return false

        try {
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {
            return latest != current
        }
        return false
    }

    private fun formatDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date = inputFormat.parse(isoDate) ?: return isoDate
            val outputFormat = SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es", "ES"))
            outputFormat.format(date)
        } catch (_: Exception) {
            isoDate.take(10)
        }
    }
}
