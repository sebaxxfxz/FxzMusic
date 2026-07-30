package com.fxzmusic.app.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    AMOLED,
    SOFT_DARK,
    LIGHT
}

@Immutable
@Stable
data class FxzTheme(
    val accent: Color = Color(0xFF7DFFA2),
    val background: Color = Color(0xFF000000),
    val surface: Color = Color(0xFF0A0A0A),
    val surfaceVariant: Color = Color(0xFF141414),
    val mode: ThemeMode = ThemeMode.AMOLED
) {
    companion object {
        val Default = FxzTheme()
    }
}

val ACCENT_COLORS = listOf(
    Color(0xFF7DFFA2) to "Verde",
    Color(0xFF2979FF) to "Azul",
    Color(0xFFFFFFFF) to "Blanco",
    Color(0xFF00E5FF) to "Cyan",
    Color(0xFFD500F9) to "Morado",
    Color(0xFFFF1744) to "Rojo",
    Color(0xFFFFEA00) to "Amarillo",
    Color(0xFFFF6D00) to "Naranja"
)

val ACCENT_NAMES = listOf("Verde", "Azul", "Blanco", "Cyan", "Morado", "Rojo", "Amarillo", "Naranja")

val THEME_NAMES = listOf("AMOLED", "Soft Dark", "Light")
val THEME_DESCRIPTIONS = listOf(
    "Fondo negro puro para pantallas OLED",
    "Tema oscuro con grises suaves",
    "Tema claro clásico"
)

val LocalFxzTheme = staticCompositionLocalOf {
    FxzTheme.Default
}

val FxzTheme.current: FxzTheme
    get() = this

fun buildTheme(accent: Color, mode: ThemeMode): FxzTheme = when (mode) {
    ThemeMode.AMOLED -> FxzTheme(
        accent = accent,
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceVariant = Color(0xFF141414),
        mode = mode
    )
    ThemeMode.SOFT_DARK -> FxzTheme(
        accent = accent,
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF2C2C2C),
        mode = mode
    )
    ThemeMode.LIGHT -> FxzTheme(
        accent = accent,
        background = Color(0xFFF6F6F6),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEEEEEE),
        mode = mode
    )
}
