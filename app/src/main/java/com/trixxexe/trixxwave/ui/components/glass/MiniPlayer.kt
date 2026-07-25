package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    song: Song?,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    themeConfig: ThemeConfig = ThemeConfig()
) {
    if (song == null) return

    val shape = when (themeConfig.miniPlayerShape) {
        "Pill" -> CircleShape
        "GlassCard" -> RoundedCornerShape(16.dp)
        else -> RoundedCornerShape(24.dp)
    }

    Box(
        modifier = modifier
            .testTag("mini_player")
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .liquidGlass(
                themeConfig = themeConfig,
                cornerRadius = 28.dp
            )
            .clickable(onClick = onExpandNowPlaying)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFFF27D26)
                )
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.testTag("mini_player_skip_next")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Next",
                    tint = Color.White
                )
            }
        }
    }
}
