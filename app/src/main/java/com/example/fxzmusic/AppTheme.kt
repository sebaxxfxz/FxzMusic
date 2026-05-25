package com.example.fxzmusic

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    AMOLED,
    SOFT_DARK,
    GLASSMORPHISM
}

val THEME_NAMES = listOf("AMOLED", "Suave", "Cinematic")
val THEME_DESCRIPTIONS = listOf(
    "Negro puro · máx. batería",
    "Grises cálidos · más suave",
    "Glassmorphic · premium"
)

// ─── Obsidian Cinema tokens (from the new stitched design) ───────────────────
val Cinematic_Background           = Color(0xFF0A0A0A)
val Cinematic_Surface              = Color(0xFF131313)
val Cinematic_SurfaceContainerLow  = Color(0xFF1C1B1B)
val Cinematic_SurfaceContainer     = Color(0xFF201F1F)
val Cinematic_SurfaceContainerHigh = Color(0xFF2A2A2A)
val Cinematic_SurfaceVariant       = Color(0xFF353534)
val Cinematic_SurfaceBright        = Color(0xFF3A3939)
val Cinematic_OnSurface            = Color(0xFFE5E2E1)
val Cinematic_OnSurfaceVariant     = Color(0xFFC4C7C8)
val Cinematic_Primary              = Color(0xFFFFFFFF)
val Cinematic_OnPrimary            = Color(0xFF2F3131)
val Cinematic_Secondary            = Color(0xFF7DFFA2)  // Electric green — active states
val Cinematic_OnSecondary          = Color(0xFF003918)
val Cinematic_Outline              = Color(0xFF8E9192)
val Cinematic_OutlineVariant       = Color(0xFF444748)

// Glassmorphic panel tokens
val Cinematic_GlassBackground      = Color(0x0EFFFFFF)   // ~5.5% white — panel fill
val Cinematic_GlassBorder          = Color(0x17FFFFFF)   // ~9% white — hairline border
val Cinematic_GlassNavBackground   = Color(0x1AFFFFFF)   // ~10% white — nav bar fill
val Cinematic_PlatinumText         = Color(0xFFC4C7C8)   // supporting text
val Cinematic_MutedText            = Color(0xFF8E9192)   // muted metadata / inactive icons
// ─────────────────────────────────────────────────────────────────────────────

data class FxzTheme(
    val accent: Color         = Cinematic_Secondary,
    val background: Color     = Cinematic_Background,
    val surface: Color        = Cinematic_SurfaceContainer,
    val surfaceVariant: Color = Cinematic_SurfaceContainerHigh,
    val isAmoled: Boolean     = true,
    val mode: ThemeMode       = ThemeMode.GLASSMORPHISM
)

val LocalFxzTheme = staticCompositionLocalOf { FxzTheme() }

// Accent rotation palette (long-press in player to cycle)
val ACCENT_COLORS = listOf(
    Color(0xFF7DFFA2),   // Electric Green (default)
    Color(0xFF2979FF),   // Deep Blue
    Color(0xFFFFFFFF),   // Starlight White
    Color(0xFF00E5FF),   // Cyan
    Color(0xFFD500F9),   // Purple
    Color(0xFFFF1744),   // Crimson
    Color(0xFFFFEA00),   // Amber
    Color(0xFFFF6D00)    // Orange
)

val ACCENT_NAMES = listOf("Verde", "Azul", "Blanco", "Cyan", "Morado", "Rojo", "Amarillo", "Naranja")

fun buildTheme(accent: Color, mode: ThemeMode): FxzTheme = when (mode) {
    ThemeMode.AMOLED -> FxzTheme(
        accent         = accent,
        background     = Cinematic_Background,
        surface        = Cinematic_Surface,
        surfaceVariant = Cinematic_SurfaceContainerHigh,
        isAmoled       = true,
        mode           = mode
    )
    ThemeMode.SOFT_DARK -> FxzTheme(
        accent         = accent,
        background     = Color(0xFF121212),
        surface        = Color(0xFF1D1D1D),
        surfaceVariant = Color(0xFF282828),
        isAmoled       = false,
        mode           = mode
    )
    ThemeMode.GLASSMORPHISM -> FxzTheme(
        accent         = accent,
        background     = Cinematic_Background,
        surface        = Cinematic_SurfaceContainer,
        surfaceVariant = Cinematic_SurfaceContainerHigh,
        isAmoled       = true,
        mode           = mode
    )
}
