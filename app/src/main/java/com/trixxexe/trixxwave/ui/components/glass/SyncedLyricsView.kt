package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

object LrcParser {
    fun parseLrc(lrcText: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        val regex = """\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""".toRegex()
        lrcText.lines().forEach { lineStr ->
            val match = regex.find(lineStr.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val frac = match.groupValues[3].toLongOrNull() ?: 0L
                val fracMs = if (match.groupValues[3].length == 2) frac * 10 else frac
                val ms = (min * 60 + sec) * 1000 + fracMs
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LrcLine(ms, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}

@Composable
fun SyncedLyricsView(
    modifier: Modifier = Modifier,
    plainLyrics: String?,
    syncedLrc: String?,
    currentPositionMs: Long,
    onSeekToTimestamp: (Long) -> Unit,
    onFixClick: (() -> Unit)? = null
) {
    val lrcLines = remember(syncedLrc) {
        if (!syncedLrc.isNullOrBlank()) LrcParser.parseLrc(syncedLrc) else emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (lrcLines.isNotEmpty()) {
            val currentActiveIndex = remember(currentPositionMs, lrcLines) {
                val idx = lrcLines.indexOfLast { it.timestampMs <= currentPositionMs }
                if (idx == -1) 0 else idx
            }

            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(currentActiveIndex) {
                coroutineScope.launch {
                    val targetIndex = (currentActiveIndex - 2).coerceAtLeast(0)
                    listState.animateScrollToItem(targetIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .testTag("synced_lyrics_view")
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = 60.dp, bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lrcLines) { idx, line ->
                    val isActive = idx == currentActiveIndex

                    val lineScale by animateFloatAsState(
                        targetValue = if (isActive) 1.06f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "lyrics_scale"
                    )

                    val textAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1.0f else 0.5f,
                        animationSpec = tween(durationMillis = 300),
                        label = "lyrics_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                            .scale(lineScale)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (isActive) {
                                    Modifier
                                        .drawBehind {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0x66F27D26),
                                                        Color(0x2200F5D4),
                                                        Color.Transparent
                                                    )
                                                ),
                                                radius = size.width * 0.6f
                                            )
                                        }
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0x33F27D26),
                                                    Color(0x441E1E2C),
                                                    Color(0x3300F5D4)
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            border = BorderStroke(
                                                1.dp,
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xAAF27D26),
                                                        Color(0xCC00F5D4),
                                                        Color(0xAAF27D26)
                                                    )
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSeekToTimestamp(line.timestampMs) }
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.text,
                            color = if (isActive) Color.White else Color.White.copy(alpha = textAlpha),
                            fontSize = if (isActive) 21.sp else 16.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else if (!plainLyrics.isNullOrBlank()) {
            LazyColumn(
                modifier = Modifier
                    .testTag("plain_lyrics_view")
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = 50.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = plainLyrics,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .testTag("no_lyrics_found")
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No lyrics found for this track.\nTap 'Fix' above to write lyrics manually.",
                    color = Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Top Bar with 'Fix' button
        if (onFixClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onFixClick,
                    modifier = Modifier.testTag("fix_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Fix Lyrics",
                        tint = Color(0xFFF27D26),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fix",
                        color = Color(0xFFF27D26),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
