package com.fxzmusic.app.data
import com.fxzmusic.app.*

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun List<Color>.encodeColorList(): String =
    joinToString(",") { it.toArgb().toUInt().toString() }

fun decodeColorList(encoded: String): List<Color> {
    if (encoded.isBlank()) return listOf(Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70))
    val decoded = encoded.split(',').mapNotNull { raw ->
        raw.toUIntOrNull()?.toInt()?.let { Color(it) }
    }
    return if (decoded.size < 2) {
        listOf(Color(0xFF4158D0), Color(0xFFC850C0), Color(0xFFFFCC70))
    } else {
        decoded
    }
}
