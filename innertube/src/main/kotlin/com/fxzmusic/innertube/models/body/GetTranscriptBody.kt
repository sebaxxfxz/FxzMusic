package com.fxzmusic.innertube.models.body

import com.fxzmusic.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
