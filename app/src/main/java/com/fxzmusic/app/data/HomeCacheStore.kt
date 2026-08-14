package com.fxzmusic.app.data

import android.content.Context
import com.fxzmusic.innertube.pages.HomePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class HomeCacheStore(context: Context) {

    private val cacheFile = File(context.cacheDir, "home_cache.json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): HomePage? = withContext(Dispatchers.IO) {
        runCatching {
            if (!cacheFile.exists()) return@runCatching null
            json.decodeFromString<HomePage>(cacheFile.readText())
        }.getOrNull()
    }

    suspend fun save(page: HomePage) = withContext(Dispatchers.IO) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json.encodeToString(page))
        }
    }

    fun clear() {
        runCatching { cacheFile.delete() }
    }
}
