package com.fxzmusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.data.EqBand
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FrequencyResponseCurve(
    bands: List<EqBand>,
    accent: Color,
    onBandChange: (index: Int, gainDb: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var activeBandIndex by remember { mutableIntStateOf(-1) }
    var lastHapticGain by remember { mutableStateOf<Float?>(null) }

    // Reusable Path instances to ensure ZERO allocations in draw phase (120 FPS target)
    val curvePath = remember { Path() }
    val fillPath = remember { Path() }

    val gridLineColor = Color.White.copy(alpha = 0.08f)
    val centerLineColor = Color.White.copy(alpha = 0.22f)
    val labelColor = android.graphics.Color.argb(120, 255, 255, 255)
    val activeLabelColor = android.graphics.Color.argb(
        255,
        (accent.red * 255).toInt(),
        (accent.green * 255).toInt(),
        (accent.blue * 255).toInt()
    )

    fun updateBandGainFromTouch(touchX: Float, touchY: Float) {
        if (bands.isEmpty() || canvasSize.width <= 0f || canvasSize.height <= 0f) return
        val width = canvasSize.width
        val height = canvasSize.height

        val paddingHorizontal = 24.dp.value
        val paddingVertical = 20.dp.value
        val usableWidth = (width - paddingHorizontal * 2).coerceAtLeast(1f)
        val usableHeight = (height - paddingVertical * 2).coerceAtLeast(1f)
        val midY = paddingVertical + usableHeight / 2f

        val stepX = usableWidth / (bands.size - 1).coerceAtLeast(1)
        val relativeX = (touchX - paddingHorizontal).coerceIn(0f, usableWidth)
        val closestIndex = (relativeX / stepX).roundToInt().coerceIn(0, bands.size - 1)

        val clampedY = touchY.coerceIn(paddingVertical, paddingVertical + usableHeight)
        val normalizedY = (midY - clampedY) / (usableHeight / 2f)
        val rawGain = normalizedY * 12f
        // Round to 0.5 dB steps for precision and stability
        val gainDb = ((rawGain * 2f).roundToInt() / 2f).coerceIn(-12f, 12f)

        activeBandIndex = closestIndex

        // Trigger haptic feedback when crossing 0dB or on significant step change
        val lastGain = lastHapticGain
        if (lastGain == null || (lastGain != gainDb && (gainDb == 0f || abs(gainDb - lastGain) >= 1.5f))) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticGain = gainDb
        }

        onBandChange(closestIndex, gainDb)
    }

    GlassCard(
        modifier = modifier.height(185.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        canvasSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
                    }
                    .pointerInput(bands) {
                        detectTapGestures(
                            onPress = { offset ->
                                updateBandGainFromTouch(offset.x, offset.y)
                            }
                        )
                    }
                    .pointerInput(bands) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                updateBandGainFromTouch(offset.x, offset.y)
                            },
                            onDragEnd = {
                                activeBandIndex = -1
                                lastHapticGain = null
                            },
                            onDragCancel = {
                                activeBandIndex = -1
                                lastHapticGain = null
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                updateBandGainFromTouch(change.position.x, change.position.y)
                            }
                        )
                    }
            ) {
                if (bands.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height

                val paddingX = 24.dp.toPx()
                val paddingY = 20.dp.toPx()
                val usableWidth = (width - paddingX * 2).coerceAtLeast(1f)
                val usableHeight = (height - paddingY * 2).coerceAtLeast(1f)
                val midY = paddingY + usableHeight / 2f

                // Draw Horizontal dB Grid Lines (-12dB, -6dB, 0dB, +6dB, +12dB)
                val dbLevels = listOf(12f, 6f, 0f, -6f, -12f)
                val paint = android.graphics.Paint().apply {
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                for (db in dbLevels) {
                    val y = midY - (db / 12f) * (usableHeight / 2f)
                    val isCenter = db == 0f
                    drawLine(
                        color = if (isCenter) centerLineColor else gridLineColor,
                        start = Offset(paddingX, y),
                        end = Offset(width - paddingX, y),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )

                    // dB label on the right
                    paint.color = if (isCenter) activeLabelColor else labelColor
                    val labelText = if (db > 0) "+${db.toInt()}" else "${db.toInt()}"
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        width - 4.dp.toPx(),
                        y + 3.dp.toPx(),
                        paint
                    )
                }

                // Compute control point coordinates for each frequency band
                val bandCount = bands.size
                val stepX = usableWidth / (bandCount - 1).coerceAtLeast(1)

                val points = Array(bandCount) { i ->
                    val band = bands[i]
                    val x = paddingX + i * stepX
                    val normalized = (band.gainDb.coerceIn(-12f, 12f) / 12f)
                    val y = midY - (normalized * (usableHeight / 2f))
                    Offset(x, y)
                }

                // Draw vertical grid lines and frequency labels
                val freqPaint = android.graphics.Paint().apply {
                    textSize = 8.5.sp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                for (i in 0 until bandCount) {
                    val pt = points[i]
                    val isCurrentActive = i == activeBandIndex
                    drawLine(
                        color = if (isCurrentActive) accent.copy(alpha = 0.35f) else gridLineColor,
                        start = Offset(pt.x, paddingY),
                        end = Offset(pt.x, height - paddingY),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw frequency label at bottom
                    freqPaint.color = if (isCurrentActive) activeLabelColor else labelColor
                    drawContext.canvas.nativeCanvas.drawText(
                        bands[i].label,
                        pt.x,
                        height - 4.dp.toPx(),
                        freqPaint
                    )
                }

                // Catmull-Rom to Cubic Bézier curve construction (Zero allocations with rewind)
                curvePath.rewind()
                fillPath.rewind()

                curvePath.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, height - paddingY)
                fillPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until bandCount - 1) {
                    val p0 = if (i > 0) points[i - 1] else points[i]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = if (i + 2 < bandCount) points[i + 2] else p2

                    // Catmull-Rom cubic Bézier control points conversion
                    val cp1X = p1.x + (p2.x - p0.x) / 6f
                    val cp1Y = p1.y + (p2.y - p0.y) / 6f
                    val cp2X = p2.x - (p3.x - p1.x) / 6f
                    val cp2Y = p2.y - (p3.y - p1.y) / 6f

                    curvePath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
                    fillPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
                }

                fillPath.lineTo(points[bandCount - 1].x, height - paddingY)
                fillPath.close()

                // Draw Gradient Fill Area Under Curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.40f),
                            accent.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = paddingY,
                        endY = height - paddingY
                    )
                )

                // Draw Main Glow Line
                drawPath(
                    path = curvePath,
                    color = accent.copy(alpha = 0.35f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Main Sharp Line
                drawPath(
                    path = curvePath,
                    color = accent,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Interactive Control Nodes
                for (i in 0 until bandCount) {
                    val pt = points[i]
                    val band = bands[i]
                    val isSelected = i == activeBandIndex
                    val isNonZero = band.gainDb != 0f

                    // Outer Halo
                    drawCircle(
                        color = when {
                            isSelected -> accent.copy(alpha = 0.5f)
                            isNonZero -> accent.copy(alpha = 0.25f)
                            else -> Color.White.copy(alpha = 0.1f)
                        },
                        radius = if (isSelected) 14.dp.toPx() else 10.dp.toPx(),
                        center = pt
                    )

                    // Inner Dot
                    drawCircle(
                        color = if (isSelected || isNonZero) accent else Color.White,
                        radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                        center = pt
                    )

                    // White highlight dot in center for active node
                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
