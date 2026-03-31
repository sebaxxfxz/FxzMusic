package com.example.fxzmusic

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    AMOLED,
    SOFT_DARK,
    GLASSMORPHISM
}

val THEME_NAMES = listOf("AMOLED", "Suave", "Glass")
val THEME_DESCRIPTIONS = listOf(
    "Negro puro · máx. batería",
    "Grises cálidos · más suave",
    "Cards con efecto cristal"
)

data class FxzTheme(
    val accent: Color         = Color(0xFF00E676),
    val background: Color     = Color.Black,
    val surface: Color        = Color(0xFF111111),
    val surfaceVariant: Color = Color(0xFF1E1E1E),
    val isAmoled: Boolean     = true,
    val mode: ThemeMode       = ThemeMode.AMOLED
)

val LocalFxzTheme = staticCompositionLocalOf { FxzTheme() }

val ACCENT_COLORS = listOf(
    Color(0xFF00E676),
    Color(0xFF2979FF),
    Color(0xFFFF6D00),
    Color(0xFFD500F9),
    Color(0xFFFF1744),
    Color(0xFFFFEA00),
    Color(0xFF00E5FF),
    Color(0xFFFFFFFF)
)

val ACCENT_NAMES = listOf("Verde", "Azul", "Naranja", "Morado", "Rojo", "Amarillo", "Cyan", "Blanco")

fun buildTheme(accent: Color, mode: ThemeMode): FxzTheme = when (mode) {
    ThemeMode.AMOLED -> FxzTheme(
        accent         = accent,
        background     = Color.Black,
        surface        = Color(0xFF111111),
        surfaceVariant = Color(0xFF1E1E1E),
        isAmoled       = true,
        mode           = mode
    )
    ThemeMode.SOFT_DARK -> FxzTheme(
        accent         = accent,
        background     = Color(0xFF1A1814),
        surface        = Color(0xFF23201C),
        surfaceVariant = Color(0xFF2E2A25),
        isAmoled       = false,
        mode           = mode
    )
    ThemeMode.GLASSMORPHISM -> FxzTheme(
        accent         = accent,
        background     = Color(0xFF0D0D0D),
        surface        = Color(0x801A1A1A),
        surfaceVariant = Color(0x661E1E1E),
        isAmoled       = false,
        mode           = mode
    )
}
