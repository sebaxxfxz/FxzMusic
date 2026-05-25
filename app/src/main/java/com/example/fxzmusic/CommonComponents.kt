@file:Suppress("unused")

package com.example.fxzmusic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Glassmorphic Card (Improved) ────────────────────────────────────────────
/**
 * Premium glassmorphic container for Obsidian Cinema design.
 * Background: 8% white gradient, border: 9% white hairline, rounded: 32dp.
 * Perfect for cards, panels, and elevated surfaces.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(32.dp),
    backgroundColor: Color = Cinematic_GlassBackground,
    borderColor: Color = Cinematic_GlassBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        backgroundColor.copy(alpha = 0.08f),
                        backgroundColor.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

// ─── Elevated Glass Surface ─────────────────────────────────────────────────
/**
 * More pronounced glassmorphic surface for major UI elements.
 * Background: 10% white, perfect for containers and bottom sheets.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(32.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Cinematic_GlassNavBackground)
            .border(1.dp, Cinematic_GlassBorder, shape)
    ) {
        content()
    }
}

// ─── Bouncy Icon Button ─────────────────────────────────────────────────────
@Composable
fun BouncyIconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(if (isPressed) 0.86f else 1f)
            .clip(CircleShape)
            .background(Cinematic_GlassNavBackground)
            .border(1.dp, Cinematic_GlassBorder, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// ─── Filter Chips Row ───────────────────────────────────────────────────────
/**
 * Horizontal chips row. Active chip uses accent color fill + black text.
 * Inactive chips are transparent glass with muted border.
 */
@Composable
fun FilterChipsRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()

            val bgColor = if (isSelected) LocalFxzTheme.current.accent else Cinematic_GlassBackground
            val borderColor = if (isSelected) LocalFxzTheme.current.accent else Cinematic_GlassBorder
            val textColor = if (isSelected) Color.Black else Cinematic_OnSurface

            Box(
                modifier = Modifier
                    .scale(if (isPressed) 0.93f else 1f)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(9999.dp))
                    .clickable(interactionSource = interaction, indication = null) { onFilterSelected(filter) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    filter,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// ─── Interactive Search Bar ─────────────────────────────────────────────────
/**
 * Semi-transparent glass search bar with a 5% white border.
 * Placeholder uses Cinematic_PlatinumText (55% white).
 */
@Composable
fun InteractiveSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Buscar carpetas o pistas...", color = Cinematic_PlatinumText, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Cinematic_PlatinumText) },
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onClear?.invoke(); onValueChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Cinematic_PlatinumText)
                    }
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Cinematic_PlatinumText)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Cinematic_GlassBackground,
            focusedContainerColor   = Cinematic_GlassBackground,
            unfocusedBorderColor    = Cinematic_GlassBorder,
            focusedBorderColor      = LocalFxzTheme.current.accent,
            cursorColor             = LocalFxzTheme.current.accent
        ),
        textStyle = LocalTextStyle.current.copy(color = Cinematic_OnSurface)
    )
}

// ─── Section Header ─────────────────────────────────────────────────────────
/**
 * Section headers are uppercase with generous letter-spacing,
 * emulating professional audio equipment labeling.
 */
@Composable
fun SectionHeader(title: String, onShowAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title.uppercase(),
            color = Cinematic_PlatinumText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Ver Todo",
            color = LocalFxzTheme.current.accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable { onShowAllClick() }
        )
    }
}

// ─── Mood Chip ───────────────────────────────────────────────────────────────
@Composable
fun MoodChip(mood: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val color = when (mood) {
        "Chill"       -> Color(0xFF009688)
        "Intenso"     -> Color(0xFFE91E63)
        "Melancólico" -> Color(0xFF3F51B5)
        "Alegre"      -> Color(0xFFFFC107)
        else          -> Color(0xFF9C27B0)
    }
    Box(
        modifier = Modifier
            .scale(if (isPressed) 0.92f else 1f)
            .clip(RoundedCornerShape(9999.dp))
            .background(color.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(9999.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(mood, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Create Playlist Dialog ──────────────────────────────────────────────────
@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Playlist", color = Cinematic_OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la carpeta", color = Cinematic_PlatinumText) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = Cinematic_OnSurface,
                    unfocusedTextColor   = Cinematic_OnSurface,
                    focusedBorderColor   = LocalFxzTheme.current.accent,
                    unfocusedBorderColor = Cinematic_OutlineVariant,
                    focusedContainerColor   = Cinematic_SurfaceContainerHigh,
                    unfocusedContainerColor = Cinematic_SurfaceContainerHigh,
                    cursorColor          = LocalFxzTheme.current.accent
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = LocalFxzTheme.current.accent,
                    disabledContainerColor = Cinematic_SurfaceVariant
                )
            ) { Text("Crear", color = Cinematic_OnSecondary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Cinematic_PlatinumText) }
        },
        containerColor = Cinematic_SurfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}
