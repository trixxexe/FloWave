package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WaveformSeekBar(
    modifier: Modifier = Modifier,
    waveformPointsStr: String?,
    progress: Float, // 0.0f to 1.0f
    onSeek: (Float) -> Unit,
    activeColor: Color = Color(0xFFF27D26),
    inactiveColor: Color = Color(0x33FFFFFF),
    height: Dp = 48.dp
) {
    val points = remember(waveformPointsStr) {
        waveformPointsStr?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: (1..60).map { (it * 7) % 80 + 20 }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    val activeProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .testTag("waveform_seek_bar")
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { size ->
                barWidthPx = size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val clickedProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                    onSeek(clickedProgress)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek(dragProgress)
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val width = size.width
        val viewHeight = size.height
        val count = points.size
        val barWidth = (width / count) * 0.6f
        val gap = (width / count) * 0.4f

        for (i in 0 until count) {
            val normProgress = i.toFloat() / count
            val isPlayed = normProgress <= activeProgress
            val rawAmp = points[i].coerceIn(10, 100)
            val barHeight = (rawAmp / 100f) * viewHeight * 0.85f
            val x = i * (barWidth + gap) + gap / 2
            val y = (viewHeight - barHeight) / 2

            drawRoundRect(
                color = if (isPlayed) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}


