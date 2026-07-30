package com.fxzmusic.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

enum class PlayerBackgroundStyle(val label: String, val icon: ImageVector, val description: String) {
    DEFAULT("Normal", Icons.Filled.WbSunny, "Fondo dinámico con gradiente de colores"),
    BLUR("Desenfoque", Icons.Filled.BlurOn, "Portada desenfocada como fondo"),
    GRADIENT("Gradiente", Icons.Filled.Gradient, "Gradiente animado con colores de la portada")
}
