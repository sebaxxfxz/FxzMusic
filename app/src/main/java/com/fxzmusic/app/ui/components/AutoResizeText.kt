package com.fxzmusic.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    targetFontSize: TextUnit = 32.sp,
    minFontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    color: Color = Color.White,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 2,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    var fontSize by remember { mutableStateOf(targetFontSize) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { textLayoutResult ->
            val didOverflow = textLayoutResult.didOverflowHeight
            if (didOverflow && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
            }
        }
    )
}
