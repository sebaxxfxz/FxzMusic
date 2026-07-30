
package com.fxzmusic.ytpipeline

import androidx.datastore.preferences.core.edit
import com.fxzmusic.innertube.YouTube
import com.fxzmusic.ytpipeline.log.VisitorDataKey
import com.fxzmusic.ytpipeline.cipher.CipherDeobfuscator
import com.fxzmusic.ytpipeline.log.PlaybackLogManager
import com.fxzmusic.ytpipeline.log.PlaybackLogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

object BotDetectionMitigator {
    private const val TAG = "BotDetectionMitigator"

    private val failureCount = AtomicInteger(0)
    private val rotationMutex = Mutex()

    private val GEO_ERROR_SIGNATURES = listOf(
        "not available in your country",
        "not available in your region",
        "not available in this country",
        "not available in this region",
        "geo-restricted",
        "GEO_RESTRICTED",
        "NOT_AVAILABLE_IN_THIS_COUNTRY",
        "only available in certain countries",
        "country restriction",
        "region restriction",
    )

    private val BOT_ERROR_SIGNATURES = listOf(
        "Sign in to confirm",
        "confirm you're not a bot",
        "automated queries",
        "Error 2000",
        "403",
        "This content isn't available on this device",
    )

    fun notifyPlaybackFailure(isLoggedIn: Boolean, errorMessage: String? = null): Boolean {
        if (isLoggedIn) return false
        if (isGeoError(errorMessage)) return false

        failureCount.incrementAndGet()
        return true
    }

    fun notifyPlaybackSuccess() {
        failureCount.set(0)
    }

    suspend fun rotateGuestSession() {
        rotationMutex.withLock {
            Timber.tag(TAG).i("Rotating guest session to bypass bot detection...")
            PlaybackLogManager.log(
                PlaybackLogLevel.BOT, 
                "Rotating guest session", 
                "Bypassing bot detection by refreshing visitorData (locale preserved)"
            )
            
            withContext(Dispatchers.IO) {
                
                val currentLocale = YouTube.locale

                YouTube.visitorData = null
                
                YouTube.refreshVisitorData().onSuccess { newData ->
                    Timber.tag(TAG).i("New visitorData obtained successfully for region ${currentLocale.gl}.")
                    
                    CipherDeobfuscator.appContext?.ytPipelineDataStore?.edit { settings ->
                        settings[VisitorDataKey] = newData
                    }
                }.onFailure { e ->
                    Timber.tag(TAG).e(e, "Failed to refresh visitorData during rotation")
                    
                    YouTube.locale = currentLocale
                }
            }
            
            failureCount.set(0)
        }
    }

    fun isGeoError(message: String?): Boolean {
        if (message == null) return false
        val lower = message.lowercase()
        return GEO_ERROR_SIGNATURES.any { lower.contains(it.lowercase()) }
    }

    fun isBotDetectionError(message: String?): Boolean {
        if (message == null) return false
        val lower = message.lowercase()
        return BOT_ERROR_SIGNATURES.any { lower.contains(it.lowercase()) }
    }

    fun reset() {
        failureCount.set(0)
    }
}
