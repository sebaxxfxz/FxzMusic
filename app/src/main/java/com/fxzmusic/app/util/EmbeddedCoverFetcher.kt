package com.fxzmusic.app.util

import android.content.Context
import android.media.MediaMetadataRetriever
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.File

data class EmbeddedCoverModel(
    val filePath: String,
    val songId: String = ""
)

class EmbeddedCoverFetcher(
    private val data: EmbeddedCoverModel,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val file = File(data.filePath)
        if (!file.exists() || !file.isFile) return@withContext null

        val retriever = MediaMetadataRetriever()
        val picture = try {
            retriever.setDataSource(file.absolutePath)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        if (picture == null || picture.isEmpty()) return@withContext null

        val buffer = Buffer().write(picture)
        SourceResult(
            source = ImageSource(source = buffer, context = options.context),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context? = null) : Fetcher.Factory<EmbeddedCoverModel> {
        override fun create(data: EmbeddedCoverModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return EmbeddedCoverFetcher(data, options)
        }
    }
}
