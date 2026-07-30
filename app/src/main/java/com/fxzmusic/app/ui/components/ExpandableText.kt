package com.fxzmusic.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class LinkSegment(
    val text: String,
    val url: String? = null,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    runs: List<LinkSegment>? = null,
    collapsedMaxLines: Int = 3,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    linkColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }

    val annotatedText: AnnotatedString = remember(text, runs) {
        if (runs.isNullOrEmpty()) {
            buildAnnotatedString { append(text) }
        } else {
            buildAnnotatedString {
                runs.forEach { segment ->
                    if (segment.onClick != null) {
                        val link = LinkAnnotation.Clickable(tag = segment.url ?: segment.text, linkInteractionListener = {
                            segment.onClick()
                        })
                        withLink(link) {
                            append(segment.text)
                        }
                    } else if (segment.url != null) {
                        val link = LinkAnnotation.Url(url = segment.url)
                        withLink(link) {
                            append(segment.text)
                        }
                    } else {
                        append(segment.text)
                    }
                }
            }
        }
    }

    var hasOverflow by remember(text) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)),
    ) {
        SelectionContainer {
            Text(
                text = annotatedText,
                style = style.copy(color = if (runs.isNullOrEmpty()) color else linkColor),
                maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
                overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                onTextLayout = { result: TextLayoutResult ->
                    if (!expanded) {
                        hasOverflow = result.hasVisualOverflow
                    }
                },
            )
        }

        if (hasOverflow || expanded) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (expanded) "Ver menos" else "Ver más",
                style = MaterialTheme.typography.labelMedium,
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
            )
        }
    }
}
