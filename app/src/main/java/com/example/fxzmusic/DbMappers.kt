package com.example.fxzmusic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun List<Color>.encodeColorList(): String =
    joinToString(",") { it.toArgb().toUInt().toString() }

fun decodeColorList(encoded: String): List<Color> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(',').mapNotNull { raw ->
        raw.toUIntOrNull()?.toInt()?.let { Color(it) }
    }
}

