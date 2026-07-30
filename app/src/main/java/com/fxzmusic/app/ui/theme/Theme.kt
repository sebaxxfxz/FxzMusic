package com.fxzmusic.app.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun FxzMusicTheme(
    theme: FxzTheme,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val shouldUseDark = theme.mode != ThemeMode.LIGHT

    val backgroundColor = theme.background
    val surfaceColor = theme.surface
    val surfaceVariantColor = theme.surfaceVariant

    val seedColor = remember(theme.accent) { theme.accent }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            
            val dynamicColors = if (context is Activity) {
                if (shouldUseDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                
                if (shouldUseDark) darkColorScheme() else lightColorScheme()
            }

            when {
                shouldUseDark -> darkColorScheme(
                    primary = if (seedColor != theme.accent) seedColor else dynamicColors.primary,
                    onPrimary = if (seedColor != theme.accent)
                        dynamicContrastColor(
                            background = seedColor,
                            targetColor = Color.White,
                            minContrast = 4.5f
                        ) else dynamicColors.onPrimary,
                    secondary = if (seedColor != theme.accent) seedColor else dynamicColors.secondary,
                    onSecondary = if (seedColor != theme.accent)
                        dynamicContrastColor(
                            background = seedColor,
                            targetColor = Color.White,
                            minContrast = 3.0f
                        ) else dynamicColors.onSecondary
                )
                else -> lightColorScheme(
                    primary = if (seedColor != theme.accent) seedColor else dynamicColors.primary,
                    onPrimary = if (seedColor != theme.accent)
                        dynamicContrastColor(
                            background = seedColor,
                            targetColor = Color.Black,
                            minContrast = 4.5f
                        ) else dynamicColors.onPrimary,
                    secondary = if (seedColor != theme.accent) seedColor else dynamicColors.secondary,
                    onSecondary = if (seedColor != theme.accent)
                        dynamicContrastColor(
                            background = seedColor,
                            targetColor = Color.Black,
                            minContrast = 3.0f
                        ) else dynamicColors.onSecondary
                )
            }
        }
else -> {
             
             when {
                shouldUseDark -> darkColorScheme(
                    primary = seedColor,
                    onPrimary = dynamicContrastColor(
                        background = seedColor,
                        targetColor = Color.White,
                        minContrast = 4.5f
                    ),
                    secondary = seedColor,
                    onSecondary = dynamicContrastColor(
                        background = seedColor,
                        targetColor = Color.White,
                        minContrast = 3.0f
                    ),
                    background = backgroundColor,
                    onBackground = dynamicContrastColor(
                        background = backgroundColor,
                        targetColor = Color.White,
                        minContrast = 3.0f
                    ),
                    surface = surfaceColor,
                    onSurface = dynamicContrastColor(
                        background = surfaceColor,
                        targetColor = Color.White,
                        minContrast = 3.0f
                    ),
                    surfaceVariant = surfaceVariantColor,
                    onSurfaceVariant = dynamicContrastColor(
                        background = surfaceVariantColor,
                        targetColor = Color.White,
                        minContrast = 3.0f
                    )
                )
                else -> lightColorScheme(
                    primary = seedColor,
                    onPrimary = dynamicContrastColor(
                        background = seedColor,
                        targetColor = Color.Black,
                        minContrast = 4.5f
                    ),
                    secondary = seedColor,
                    onSecondary = dynamicContrastColor(
                        background = seedColor,
                        targetColor = Color.Black,
                        minContrast = 3.0f
                    ),
                    background = backgroundColor,
                    onBackground = dynamicContrastColor(
                        background = backgroundColor,
                        targetColor = Color.Black,
                        minContrast = 3.0f
                    ),
                    surface = surfaceColor,
                    onSurface = dynamicContrastColor(
                        background = surfaceColor,
                        targetColor = Color.Black,
                        minContrast = 3.0f
                    ),
                    surfaceVariant = surfaceVariantColor,
                    onSurfaceVariant = dynamicContrastColor(
                        background = surfaceVariantColor,
                        targetColor = Color.Black,
                        minContrast = 3.0f
                    )
                )
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        
        val window = (view.context as? Activity)?.window
        window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            val controller = WindowInsetsControllerCompat(win, win.decorView)
            controller.isAppearanceLightStatusBars = !shouldUseDark
            controller.isAppearanceLightNavigationBars = !shouldUseDark
        }
        onDispose {}
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}

private fun dynamicContrastColor(background: Color, targetColor: Color, minContrast: Float): Color {
    val bgLuminance = relativeLuminance(background)
    val targetLuminance = relativeLuminance(targetColor)
    val contrast = (maxOf(bgLuminance, targetLuminance) + 0.05f) /
                   (minOf(bgLuminance, targetLuminance) + 0.05f)
    return if (contrast >= minContrast) {
        targetColor
    } else {
        val dark = Color(0xFF0A0A0A)
        val light = Color(0xFFFCFCFC)
        val darkContrast = (maxOf(bgLuminance, relativeLuminance(dark)) + 0.05f) /
                           (minOf(bgLuminance, relativeLuminance(dark)) + 0.05f)
        val lightContrast = (maxOf(bgLuminance, relativeLuminance(light)) + 0.05f) /
                            (minOf(bgLuminance, relativeLuminance(light)) + 0.05f)
        if (darkContrast >= lightContrast) dark else light
    }
}

private fun relativeLuminance(color: Color): Float {
    fun channelLinear(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val r = channelLinear(color.red)
    val g = channelLinear(color.green)
    val b = channelLinear(color.blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
