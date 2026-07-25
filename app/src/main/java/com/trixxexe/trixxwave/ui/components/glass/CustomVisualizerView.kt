package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CustomVisualizerView(
    modifier: Modifier = Modifier,
    bands: FloatArray,
    waveform: FloatArray = FloatArray(32) { 0f },
    style: String = "Spectrum", // "Spectrum", "Waveform", "Liquid Wave", "Circular", "None"
    accentColor: Color = Color(0xFFF27D26),
    secondaryColor: Color = Color(0xFF8B5CF6),
    height: Dp = 60.dp
) {
    if (style == "None") return

    Canvas(
        modifier = modifier
            .testTag("audio_visualizer")
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val viewHeight = size.height
        val numBands = bands.size.coerceAtLeast(1)

        when (style) {
            "Waveform" -> {
                // Real PCM Waveform Rendering
                val wavePoints = if (waveform.any { it != 0f }) waveform else {
                    FloatArray(32) { idx ->
                        val b = bands[idx % numBands]
                        if (idx % 2 == 0) b else -b
                    }
                }

                val path = Path()
                val fillPath = Path()
                val step = width / (wavePoints.size - 1).coerceAtLeast(1)
                val centerY = viewHeight / 2f

                fillPath.moveTo(0f, viewHeight)
                for (i in wavePoints.indices) {
                    val x = i * step
                    val normVal = wavePoints[i].coerceIn(-1f, 1f)
                    val y = centerY + normVal * (centerY * 0.85f)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * step
                        val prevVal = wavePoints[i - 1].coerceIn(-1f, 1f)
                        val prevY = centerY + prevVal * (centerY * 0.85f)
                        val cx = (prevX + x) / 2f

                        path.cubicTo(cx, prevY, cx, y, x, y)
                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }
                fillPath.lineTo(width, viewHeight)
                fillPath.close()

                // Draw filled gradient area under wave
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.35f), secondaryColor.copy(alpha = 0.05f))
                    )
                )

                // Draw main wave stroke
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(accentColor, secondaryColor, accentColor)),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            "Liquid Wave" -> {
                // Multi-phase Fluid Sine Audio Wave
                val centerY = viewHeight / 2f
                val avgBand = if (bands.isNotEmpty()) bands.average().toFloat().coerceIn(0.1f, 1f) else 0.3f

                val primaryPath = Path()
                val secondaryPath = Path()
                val steps = 40

                primaryPath.moveTo(0f, centerY)
                secondaryPath.moveTo(0f, centerY)

                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * width
                    val waveSample = if (waveform.isNotEmpty()) waveform[i % waveform.size] else 0f
                    val phase = (i.toFloat() / steps) * (2 * Math.PI).toFloat()

                    val y1 = centerY + sin(phase) * (centerY * 0.7f * avgBand) + (waveSample * 12f)
                    val y2 = centerY + cos(phase * 1.5f) * (centerY * 0.5f * avgBand) - (waveSample * 8f)

                    if (i == 0) {
                        primaryPath.moveTo(x, y1)
                        secondaryPath.moveTo(x, y2)
                    } else {
                        primaryPath.lineTo(x, y1)
                        secondaryPath.lineTo(x, y2)
                    }
                }

                drawPath(
                    path = primaryPath,
                    brush = Brush.horizontalGradient(listOf(accentColor, secondaryColor)),
                    style = Stroke(width = 3.5.dp.toPx())
                )
                drawPath(
                    path = secondaryPath,
                    brush = Brush.horizontalGradient(listOf(secondaryColor, accentColor)),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            "Circular" -> {
                val center = Offset(width / 2f, viewHeight / 2f)
                val baseRadius = (viewHeight / 2.6f).coerceAtLeast(10f)

                // Draw pulsating central liquid ring
                val avgIntensity = if (bands.isNotEmpty()) bands.average().toFloat().coerceIn(0.1f, 1f) else 0.2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.5f), Color.Transparent),
                        center = center,
                        radius = baseRadius * (1f + avgIntensity * 0.4f)
                    ),
                    center = center,
                    radius = baseRadius * (1f + avgIntensity * 0.4f)
                )

                // Draw radiating frequency spikes
                val totalSpikes = bands.size.coerceAtLeast(12)
                val angleStep = (2 * Math.PI / totalSpikes).toFloat()

                for (i in 0 until totalSpikes) {
                    val valNorm = bands[i % bands.size].coerceIn(0.1f, 1f)
                    val spikeLen = valNorm * (viewHeight / 2.2f)
                    val angle = i * angleStep

                    val startX = center.x + cos(angle) * baseRadius
                    val startY = center.y + sin(angle) * baseRadius
                    val endX = center.x + cos(angle) * (baseRadius + spikeLen)
                    val endY = center.y + sin(angle) * (baseRadius + spikeLen)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, secondaryColor),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            else -> {
                // "Spectrum" Bar Graph (Default)
                val totalBarWidth = width / numBands
                val barWidth = (totalBarWidth * 0.65f).coerceAtLeast(2f)
                val spacing = totalBarWidth * 0.35f

                for (i in bands.indices) {
                    val valNorm = bands[i].coerceIn(0.08f, 1f)
                    val barHeight = valNorm * viewHeight * 0.9f
                    val x = i * totalBarWidth + spacing / 2
                    val y = viewHeight - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor, secondaryColor)
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )

                    // Draw subtle glowing cap dot on top of each spectrum bar
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = (barWidth / 2.5f).coerceAtMost(3.dp.toPx()),
                        center = Offset(x + barWidth / 2f, y + 2.dp.toPx())
                    )
                }
            }
        }
    }
}
