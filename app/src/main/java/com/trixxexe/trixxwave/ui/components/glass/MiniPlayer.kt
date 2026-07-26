package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long = 0L,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onToggleLike: (Song) -> Unit = {},
    onExpandNowPlaying: () -> Unit,
    themeConfig: ThemeConfig = ThemeConfig()
) {
    if (song == null) return

    val accentColor = getThemeAccentColor(themeConfig)
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeviceDialog by remember { mutableStateOf(false) }
    var connectedDevices by remember { mutableStateOf(getConnectedOutputDevices(context)) }
    var selectedDeviceId by remember { mutableIntStateOf(-1) }

    val activeDevice = remember(connectedDevices, selectedDeviceId) {
        connectedDevices.find { it.id == selectedDeviceId }
            ?: connectedDevices.find { it.isExternal }
            ?: connectedDevices.find { !it.isExternal }
    }

    val totalDurationMs = song.durationMs.coerceAtLeast(1L)
    val rawProgress = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val activeProgress = if (isDragging) dragProgress else rawProgress
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    // Floating Glass Card Container
    Box(
        modifier = modifier
            .testTag("mini_player")
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .liquidGlass(
                themeConfig = themeConfig,
                cornerRadius = 28.dp
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP ROW: Title & Artist (Center) + Animated Equalizer (Right) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandNowPlaying),
                contentAlignment = Alignment.Center
            ) {
                // Song Title & Artist Stack
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 36.dp)
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Top Right: Equalizer Animated Wave
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                ) {
                    AnimatedEqualizerWaveIcon(
                        isPlaying = isPlaying,
                        accentColor = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- MIDDLE ROW: Elapsed Time - Scrubber Bar - Remaining Time ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Elapsed Time Text (e.g., "1:04")
                Text(
                    text = formatDuration(currentPositionMs),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(38.dp)
                )

                // Center Liquid Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val newFrac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                onSeek((newFrac * totalDurationMs).toLong())
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragProgress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    onSeek((dragProgress * totalDurationMs).toLong())
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    dragProgress = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Track Background Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                    )

                    // Active Progress Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(activeProgress)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        accentColor,
                                        Color(0xFFA855F7)
                                    )
                                )
                            )
                    )
                }

                // Right Remaining Time Text (e.g., "-0:38")
                Text(
                    text = formatRemainingTime(totalDurationMs, currentPositionMs),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BOTTOM ROW: Star - Prev - Play/Pause - Next - Audio Output ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Far Left: Favorite Star Button
                IconButton(
                    onClick = { onToggleLike(song) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_like_button")
                ) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isLiked) Color(0xFFFFD700) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Middle Left: Skip Previous Button (<<)
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_player_skip_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center: Solid Play/Pause Pill Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFE2E8F0))
                            )
                        )
                        .clickable(onClick = onPlayPauseToggle)
                        .testTag("mini_player_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Middle Right: Skip Next Button (>>)
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_player_skip_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Far Right: Audio Output Selector (AirPods / Speaker / Cast Icon)
                IconButton(
                    onClick = { showDeviceDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_audio_device")
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audio Device Output",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Audio Device Selection Glass Dialog
    if (showDeviceDialog) {
        AudioOutputDeviceDialog(
            themeConfig = themeConfig,
            devices = connectedDevices,
            activeDeviceId = activeDevice?.id ?: -1,
            onSelectDevice = { deviceItem ->
                selectedDeviceId = deviceItem.id
                switchAudioDevice(context, deviceItem)
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false }
        )
    }
}

@Composable
fun AnimatedEqualizerWaveIcon(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Canvas(
        modifier = modifier
            .size(width = 18.dp, height = 16.dp)
    ) {
        val barWidth = 2.5.dp.toPx()
        val spacing = 2.dp.toPx()
        val maxHeight = size.height

        val heights = if (isPlaying) listOf(h1, h2, h3, h4) else listOf(0.3f, 0.5f, 0.3f, 0.4f)

        heights.forEachIndexed { i, frac ->
            val barH = maxHeight * frac
            val x = i * (barWidth + spacing)
            val y = maxHeight - barH

            drawRoundRect(
                color = if (isPlaying) Color.White else Color(0xFF64748B),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

private fun formatRemainingTime(durationMs: Long, currentMs: Long): String {
    val remMs = (durationMs - currentMs).coerceAtLeast(0)
    return "-${formatDuration(remMs)}"
}

