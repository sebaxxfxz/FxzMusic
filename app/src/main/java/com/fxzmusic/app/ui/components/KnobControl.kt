package com.fxzmusic.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

@Composable
fun KnobControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1000f,
    label: String = "",
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    size: Dp = 100.dp,
    displayPercentage: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val minVal = valueRange.start
    val maxVal = valueRange.endInclusive
    val span = (maxVal - minVal).coerceAtLeast(1f)

    val normalizedFraction = ((value - minVal) / span).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = if (enabled) normalizedFraction else 0f,
        animationSpec = spring(stiffness = 800f),
        label = "knob_fraction"
    )

    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    var lastHapticStep by remember { mutableStateOf<Int?>(null) }

    fun processValueUpdate(newFraction: Float) {
        val clamped = newFraction.coerceIn(0f, 1f)
        val calculatedValue = minVal + clamped * span

        // Haptic feedback every 5%
        val step = (clamped * 20).toInt()
        if (lastHapticStep != step) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticStep = step
        }

        currentOnValueChange(calculatedValue)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(enabled, minVal, maxVal) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            accumulatedDelta = 0f
                        },
                        onDragEnd = {
                            accumulatedDelta = 0f
                            lastHapticStep = null
                        },
                        onDragCancel = {
                            accumulatedDelta = 0f
                            lastHapticStep = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Vertical drag sensitivity: dragging up increases, dragging down decreases
                            val sensitivity = 0.005f
                            val deltaFraction = -dragAmount.y * sensitivity
                            val currentFrac = ((value - minVal) / span).coerceIn(0f, 1f)
                            processValueUpdate(currentFrac + deltaFraction)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val activeStrokeWidth = 8.5.dp.toPx()
                val diameter = this.size.minDimension - strokeWidth * 2.2f
                val radius = diameter / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(diameter, diameter)

                // 1. Outer Inactive Arc Track
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Outer Active Illuminated Track with Glow
                if (enabled && animatedFraction > 0.001f) {
                    val sweep = animatedFraction * SWEEP_ANGLE

                    // Glow Track
                    drawArc(
                        color = accent.copy(alpha = 0.28f),
                        startAngle = START_ANGLE,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = activeStrokeWidth + 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Sharp Active Track
                    drawArc(
                        color = accent,
                        startAngle = START_ANGLE,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = activeStrokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 3. Inner 3D Beveled Dial Body
                val knobRadius = radius - strokeWidth * 1.2f
                if (knobRadius > 0f) {
                    // Drop Shadow beneath knob body
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = knobRadius + 1.5.dp.toPx(),
                        center = center + Offset(0f, 2.dp.toPx())
                    )

                    // Metallic Gradient Body
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2A2D34),
                                Color(0xFF1C1E23),
                                Color(0xFF141518)
                            ),
                            center = center - Offset(knobRadius * 0.2f, knobRadius * 0.2f),
                            radius = knobRadius * 1.3f
                        ),
                        radius = knobRadius,
                        center = center
                    )

                    // Beveled Rim Specular Highlight
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            start = center - Offset(knobRadius, knobRadius),
                            end = center + Offset(knobRadius, knobRadius)
                        ),
                        radius = knobRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Inner decorative circle
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = knobRadius * 0.72f,
                        center = center
                    )

                    // 4. Indicator Notch / LED Marker
                    val currentAngle = START_ANGLE + (animatedFraction * SWEEP_ANGLE)
                    val angleRad = (currentAngle * PI / 180.0).toFloat()
                    val indicatorDist = knobRadius * 0.78f
                    val indicatorCenter = center + Offset(
                        cos(angleRad) * indicatorDist,
                        sin(angleRad) * indicatorDist
                    )

                    // LED Glow
                    drawCircle(
                        color = if (enabled) accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                        radius = 4.5.dp.toPx(),
                        center = indicatorCenter
                    )

                    // LED Core
                    drawCircle(
                        color = if (enabled) accent else Color.White.copy(alpha = 0.4f),
                        radius = 2.5.dp.toPx(),
                        center = indicatorCenter
                    )
                }
            }

            // Digital percentage readout in center
            val percentageInt = if (enabled) {
                if (displayPercentage) (normalizedFraction * 100f).roundToInt() else value.roundToInt()
            } else {
                0
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (displayPercentage) "$percentageInt%" else "$percentageInt",
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
                    fontSize = (size.value * 0.17f).sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
