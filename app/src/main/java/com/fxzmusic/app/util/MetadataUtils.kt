package com.fxzmusic.app.util
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import java.io.File

object MetadataUtils {

    fun getEmbeddedCoverFilePath(context: Context, song: Song): String? {
        return getEmbeddedCoverFilePath(context, song.filePath, song.id)
    }

    fun getEmbeddedCoverFilePath(context: Context, filePath: String, songId: String? = null): String? {
        val sourceFile = filePath.trim().takeIf { it.isNotEmpty() }?.let(::File) ?: return null
        if (!sourceFile.exists() || !sourceFile.isFile) return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(sourceFile.absolutePath)
            val embeddedPicture = retriever.embeddedPicture ?: return null
            if (embeddedPicture.isEmpty()) return null

            val cacheDir = File(context.cacheDir, "embedded_covers").apply { mkdirs() }
            val safeKey = buildString {
                append(songId?.takeIf { it.isNotBlank() } ?: sourceFile.nameWithoutExtension.ifBlank { "track" })
                append('_')
                append(sourceFile.length())
                append('_')
                append(sourceFile.lastModified())
            }.replace(Regex("[^A-Za-z0-9._-]"), "_")

            val outFile = File(cacheDir, "$safeKey.cover")
            if (!outFile.exists() || outFile.length() != embeddedPicture.size.toLong()) {
                outFile.writeBytes(embeddedPicture)
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun getEmbeddedLyrics(song: Song): String? {
        val filePath = song.filePath.trim()
        if (filePath.isEmpty()) return null

        val sourceFile = File(filePath)
        if (!sourceFile.exists() || !sourceFile.isFile) return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(sourceFile.absolutePath)
            val lyric = extractLyricsMetadata(retriever)
            lyric?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun extractAudioMetadata(filePath: String): AudioMetadata {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return AudioMetadata(filePath = filePath)

        return try {
            extractWithMediaExtractor(filePath)
        } catch (_: Exception) {
            extractWithRetriever(filePath)
        }
    }

    private fun extractWithMediaExtractor(filePath: String): AudioMetadata {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(filePath)
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio") == true) {
                    format = trackFormat
                    break
                }
            }
            format?.let {
                AudioMetadata(
                    bitrate = it.getIntegerSafe(MediaFormat.KEY_BIT_RATE),
                    sampleRate = it.getIntegerSafe(MediaFormat.KEY_SAMPLE_RATE),
                    channels = it.getIntegerSafe(MediaFormat.KEY_CHANNEL_COUNT),
                    durationMs = it.getLongSafe(MediaFormat.KEY_DURATION)?.div(1000),
                    mimeType = it.getString(MediaFormat.KEY_MIME),
                    codecString = it.getString(MediaFormat.KEY_CODECS_STRING),
                    fileSize = File(filePath).length(),
                    filePath = filePath
                )
            } ?: extractWithRetriever(filePath)
        } finally {
            extractor.release()
        }
    }

    private fun extractWithRetriever(filePath: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            AudioMetadata(
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull(),
                sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull(),
                channels = null,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                fileSize = File(filePath).length(),
                filePath = filePath
            )
        } catch (_: Exception) {
            AudioMetadata(filePath = filePath)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun MediaFormat.getIntegerSafe(key: String): Int? =
        try { getInteger(key) } catch (_: Exception) { null }

    private fun MediaFormat.getLongSafe(key: String): Long? =
        try { getLong(key) } catch (_: Exception) { null }

    private fun extractLyricsMetadata(retriever: MediaMetadataRetriever): String? {
        val fieldNames = listOf("METADATA_KEY_LYRIC", "METADATA_KEY_LYRICS")
        for (fieldName in fieldNames) {
            val key = runCatching {
                MediaMetadataRetriever::class.java.getField(fieldName).getInt(null)
            }.getOrNull() ?: continue

            val value = runCatching { retriever.extractMetadata(key) }
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (value != null) return value
        }
        return null
    }
}

