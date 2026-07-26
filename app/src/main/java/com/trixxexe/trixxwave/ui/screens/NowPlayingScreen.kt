package com.trixxexe.trixxwave.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.db.LyricsCache
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.CustomVisualizerView
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassVolumeControlRow
import com.trixxexe.trixxwave.ui.components.glass.LyricsEditorDialog
import com.trixxexe.trixxwave.ui.components.glass.SyncedLyricsView
import com.trixxexe.trixxwave.ui.components.glass.WaveformSeekBar
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    visualizerBands: FloatArray,
    visualizerWaveform: FloatArray = FloatArray(32),
    lyrics: LyricsCache?,
    aiInsight: String?,
    themeConfig: ThemeConfig,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: (Song) -> Unit,
    onDismiss: () -> Unit,
    onReTag: ((Song) -> Unit)? = null,
    onSaveLyrics: ((Long, String?, String?) -> Unit)? = null
) {
    if (song == null) return

    var activeTab by remember { mutableStateOf(0) } // 0: Art & Controls, 1: Lyrics, 2: AI Insights
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var showLyricsEditorDialog by remember { mutableStateOf(false) }

    val accentColor = getThemeAccentColor(themeConfig)

    Box(
        modifier = Modifier
            .testTag("now_playing_screen")
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050505),
                        Color(0xFF121212),
                        Color(0xFF050505)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("now_playing_close_button")
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text("Track", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text("Lyrics", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        label = { Text("AI Story", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                IconButton(onClick = { onToggleLike(song) }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = if (song.isLiked) Color(0xFFFF007A) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                0 -> {
                    // Album Art & Visualizer Tab with Configurable Track Transition Animation
                    val crossfadeDurationMs = (themeConfig.crossfadeDurationSec.coerceIn(1, 10) * 1000)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = {
                                when (themeConfig.trackTransitionAnimation) {
                                    "Fade to Black" -> {
                                        fadeIn(animationSpec = tween(crossfadeDurationMs, delayMillis = crossfadeDurationMs / 2))
                                            .togetherWith(fadeOut(animationSpec = tween(crossfadeDurationMs / 2)))
                                    }
                                    "Slide In" -> {
                                        (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(crossfadeDurationMs)) + fadeIn())
                                            .togetherWith(slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(crossfadeDurationMs)) + fadeOut())
                                    }
                                    "Zoom & Pop" -> {
                                        (scaleIn(initialScale = 0.4f, animationSpec = tween(crossfadeDurationMs)) + fadeIn())
                                            .togetherWith(scaleOut(targetScale = 1.6f, animationSpec = tween(crossfadeDurationMs)) + fadeOut())
                                    }
                                    "Instant Jump" -> {
                                        fadeIn(animationSpec = tween(0)).togetherWith(fadeOut(animationSpec = tween(0)))
                                    }
                                    else -> { // "Crossfade"
                                        (fadeIn(animationSpec = tween(crossfadeDurationMs, easing = LinearOutSlowInEasing)) +
                                         scaleIn(initialScale = 0.88f, animationSpec = tween(crossfadeDurationMs)))
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(crossfadeDurationMs, easing = FastOutLinearInEasing)) +
                                                scaleOut(targetScale = 1.12f, animationSpec = tween(crossfadeDurationMs))
                                            )
                                    }
                                }
                            },
                            label = "artwork_transition"
                        ) { currentSong ->
                            Box(contentAlignment = Alignment.Center) {
                                // Blurred artwork background reflection glow
                                AsyncImage(
                                    model = currentSong.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(260.dp)
                                        .blur(32.dp)
                                )

                                LiquidGlassCard(
                                    themeConfig = themeConfig,
                                    modifier = Modifier.size(280.dp),
                                    cornerRadius = 28.dp
                                ) {
                                    AsyncImage(
                                        model = currentSong.albumArtUri,
                                        contentDescription = currentSong.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Audio Visualizer
                    CustomVisualizerView(
                        bands = visualizerBands,
                        waveform = visualizerWaveform,
                        style = themeConfig.visualizerStyle,
                        accentColor = accentColor,
                        height = 50.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Song Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • ${song.album}",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Mood & Genre Tag Badges
                        val tags = remember(song.moodTags) {
                            song.moodTags?.split(",")?.map { it.trim().removePrefix("#") }?.filter { it.isNotEmpty() } ?: emptyList()
                        }
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tags.forEach { tag ->
                                    Surface(
                                        color = Color(0x22F27D26),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44F27D26)),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            color = Color(0xFFF27D26),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Gapless Trim Badge
                        if (song.trimStartMs > 0 || song.trimEndMs > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = "Gapless Trim",
                                        tint = accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gapless Trimmed: -${song.trimStartMs}ms / -${song.trimEndMs}ms",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Waveform Seek Bar with Timestamps
                    val progress = if (song.durationMs > 0) currentPositionMs.toFloat() / song.durationMs else 0f
                    Column(modifier = Modifier.fillMaxWidth()) {
                        WaveformSeekBar(
                            waveformPointsStr = song.waveformPoints,
                            progress = progress,
                            onSeek = { p -> onSeek((p * song.durationMs).toLong()) },
                            height = 42.dp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDurationMs(currentPositionMs),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatDurationMs(song.durationMs),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isShuffle = !isShuffle }) {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (isShuffle) Color(0xFFF27D26) else Color(0xFF64748B))
                        }
                        IconButton(onClick = onSkipPrevious) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(onClick = onPlayPauseToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = { isRepeat = !isRepeat }) {
                            Icon(imageVector = Icons.Default.Repeat, contentDescription = "Repeat", tint = if (isRepeat) Color(0xFFF27D26) else Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Glassmorphic Volume Slider & Bluetooth Device Shower
                    LiquidGlassVolumeControlRow(
                        themeConfig = themeConfig,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
                1 -> {
                    // Synced Lyrics Tab
                    Box(modifier = Modifier.weight(1f)) {
                        SyncedLyricsView(
                            plainLyrics = lyrics?.plainLyrics,
                            syncedLrc = lyrics?.syncedLrc,
                            currentPositionMs = currentPositionMs,
                            onSeekToTimestamp = onSeek,
                            onFixClick = { showLyricsEditorDialog = true }
                        )
                    }
                }
                2 -> {
                    // AI Story & Insights Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 24.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Track Insights & Mood",
                                        color = Color(0xFFF27D26),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (onReTag != null) {
                                        TextButton(onClick = { onReTag(song) }) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Re-Tag", tint = Color(0xFFF27D26), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Re-Tag AI", color = Color(0xFFF27D26), fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = aiInsight ?: "Generating background story...",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Current Tags:", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = song.moodTags ?: "Auto-tagging in background...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showLyricsEditorDialog) {
            LyricsEditorDialog(
                initialPlainLyrics = lyrics?.plainLyrics,
                initialSyncedLrc = lyrics?.syncedLrc,
                themeConfig = themeConfig,
                onDismiss = { showLyricsEditorDialog = false },
                onSave = { plainText, syncedLrc ->
                    onSaveLyrics?.invoke(song.id, plainText, syncedLrc)
                }
            )
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

