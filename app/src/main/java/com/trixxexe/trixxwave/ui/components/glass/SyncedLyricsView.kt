package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    listState.animateScrollToItem((currentActiveIndex - 2).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .testTag("synced_lyrics_view")
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = 50.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lrcLines) { idx, line ->
                    val isActive = idx == currentActiveIndex
                    Text(
                        text = line.text,
                        color = if (isActive) Color(0xFFF27D26) else Color(0x99FFFFFF),
                        fontSize = if (isActive) 22.sp else 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .clickable { onSeekToTimestamp(line.timestampMs) }
                    )
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
