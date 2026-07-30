package com.fxzmusic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.ui.theme.LocalFxzTheme

data class MoodCategory(
    val id: String,
    val title: String
)

val DEFAULT_MOODS = listOf(
    MoodCategory("all", "Para ti"),
    MoodCategory("chill", "Relax"),
    MoodCategory("workout", "Entrenamiento"),
    MoodCategory("party", "Fiesta"),
    MoodCategory("focus", "Enfoque"),
    MoodCategory("romance", "Romance"),
    MoodCategory("drive", "En Ruta"),
    MoodCategory("sad", "Desamor")
)

@Composable
fun YouTubeMoodPills(
    selectedMoodId: String = "all",
    onMoodSelected: (MoodCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalFxzTheme.current.accent

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(DEFAULT_MOODS, key = { _, item -> item.id }) { _, mood ->
            val isSelected = mood.id == selectedMoodId
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.06f),
                animationSpec = tween(250),
                label = "mood_bg"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                animationSpec = tween(250),
                label = "mood_text"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.12f),
                animationSpec = tween(250),
                label = "mood_border"
            )

            Box(
                modifier = Modifier
                    .scaleOnPress(interaction, PressScale.chip)
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onMoodSelected(mood) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mood.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}
