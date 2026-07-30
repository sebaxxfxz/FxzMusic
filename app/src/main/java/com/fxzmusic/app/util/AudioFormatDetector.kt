package com.fxzmusic.app.util

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File

data class AudioFormatInfo(
    val container: String = "Unknown",
    val codec: String = "Unknown",
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val isLossless: Boolean = false,
    val isLossy: Boolean = false,
    val bitDepth: Int = 0
)

object AudioFormatDetector {

    fun detect(filePath: String): AudioFormatInfo {
        if (filePath.isEmpty()) return AudioFormatInfo()
        val file = File(filePath)
        if (!file.exists()) return AudioFormatInfo()

        val ext = file.extension.lowercase()
        val headerInfo = readHeader(file)

        return when {
            ext == "flac" -> AudioFormatInfo(
                container = "FLAC",
                codec = "FLAC (Lossless)",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossless = true,
                bitDepth = headerInfo.bitDepth
            )
            ext == "opus" || ext == "ogg" -> AudioFormatInfo(
                container = "OGG",
                codec = "Opus",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossy = true
            )
            ext == "alac" || (ext == "m4a" && headerInfo.codec.contains("alac", ignoreCase = true)) -> AudioFormatInfo(
                container = "M4A",
                codec = "ALAC (Lossless)",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossless = true,
                bitDepth = headerInfo.bitDepth
            )
            ext == "aac" || ext == "m4a" || ext == "mp4" -> AudioFormatInfo(
                container = "MP4/M4A",
                codec = "AAC",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossy = true
            )
            ext == "mp3" -> AudioFormatInfo(
                container = "MP3",
                codec = "MPEG Audio",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossy = true
            )
            ext == "wav" -> AudioFormatInfo(
                container = "WAV",
                codec = "PCM (Lossless)",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossless = true,
                bitDepth = headerInfo.bitDepth
            )
            ext == "wma" -> AudioFormatInfo(
                container = "WMA",
                codec = "Windows Media Audio",
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels,
                isLossy = true
            )
            else -> AudioFormatInfo(
                container = ext.uppercase().ifEmpty { "Unknown" },
                codec = headerInfo.codec.ifEmpty { "Unknown" },
                bitrate = headerInfo.bitrate,
                sampleRate = headerInfo.sampleRate,
                channels = headerInfo.channels
            )
        }
    }

    private data class HeaderInfo(
        val codec: String = "",
        val bitrate: Int = 0,
        val sampleRate: Int = 0,
        val channels: Int = 0,
        val bitDepth: Int = 0
    )

    private fun readHeader(file: File): HeaderInfo {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            val format = extractor.getTrackFormat(0)
            HeaderInfo(
                codec = format.getString(MediaFormat.KEY_MIME) ?: "",
                bitrate = format.getIntegerSafe(MediaFormat.KEY_BIT_RATE),
                sampleRate = format.getIntegerSafe(MediaFormat.KEY_SAMPLE_RATE),
                channels = format.getIntegerSafe(MediaFormat.KEY_CHANNEL_COUNT),
                bitDepth = format.getIntegerSafe("bits-per-sample")
            )
        } catch (e: Exception) {
            HeaderInfo()
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.getIntegerSafe(key: String): Int {
        return try {
            if (containsKey(key)) getInteger(key) else 0
        } catch (e: Exception) {
            0
        }
    }

    fun formatBitrate(bitrate: Int): String {
        if (bitrate <= 0) return "N/A"
        return when {
            bitrate >= 1_000_000 -> String.format("%.1f Mbps", bitrate / 1_000_000.0)
            bitrate >= 1_000 -> String.format("%d kbps", bitrate / 1_000)
            else -> "$bitrate bps"
        }
    }

    fun formatSampleRate(sampleRate: Int): String {
        if (sampleRate <= 0) return "N/A"
        return when (sampleRate) {
            44100 -> "44.1 kHz"
            48000 -> "48 kHz"
            96000 -> "96 kHz"
            192000 -> "192 kHz"
            else -> "${sampleRate / 1000.0} kHz"
        }
    }

    fun getQualityLabel(info: AudioFormatInfo): String {
        return when {
            info.isLossless && info.bitDepth >= 24 -> "Hi-Res Lossless"
            info.isLossless && info.bitDepth >= 16 -> "Lossless CD Quality"
            info.isLossless -> "Lossless"
            info.bitrate >= 320_000 -> "High Quality"
            info.bitrate >= 256_000 -> "Good Quality"
            info.bitrate >= 128_000 -> "Standard"
            info.bitrate > 0 -> "Low Quality"
            else -> ""
        }
    }
}
