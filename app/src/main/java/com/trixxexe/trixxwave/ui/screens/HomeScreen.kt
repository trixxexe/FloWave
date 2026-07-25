package com.trixxexe.trixxwave.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.R
import com.trixxexe.trixxwave.data.db.Playlist
import com.trixxexe.trixxwave.data.db.Profile
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    themeConfig: ThemeConfig,
    activeProfile: Profile?,
    recentlyPlayed: List<Song>,
    likedSongs: List<Song>,
    allSongs: List<Song>,
    playlists: List<Playlist>,
    onSongClick: (Song) -> Unit,
    onGenerateSmartMix: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    var smartMixPrompt by remember { mutableStateOf("") }
    val accentColor = com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor(themeConfig)

    val dynamicGreetings = remember {
        listOf(
            "Welcome back,",
            "Good to see you,",
            "Step into the wave,",
            "Ready for high fidelity,",
            "Curated for your vibe,",
            "Elevate your audio,",
            "Your daily soundtrack,",
            "Resonate & unwind,",
            "Midnight audio drive,"
        )
    }
    val greeting = remember { dynamicGreetings.random() }

    LazyColumn(
        modifier = Modifier
            .testTag("home_screen")
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Bar Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onNavigateToProfiles)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2238))
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!activeProfile?.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = activeProfile?.avatarUri,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = greeting,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = activeProfile?.name ?: "Main Listener",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                Row {
                    IconButton(onClick = onNavigateToEqualizer) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Equalizer",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Hero Glass Banner
        item {
            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIQUID GLASS AUDIO",
                            color = Color(0xFFF27D26),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FloWave Engine",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time specular refraction & 10-band EQ",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // AI Smart Mix Prompt Bar
        item {
            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFFF27D26),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Smart Mix Generator",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smartMixPrompt,
                        onValueChange = { smartMixPrompt = it },
                        placeholder = {
                            Text("e.g. 'Chill midnight synth drive' or 'Upbeat workout'", color = Color(0xFF64748B), fontSize = 13.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF27D26),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input"),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (smartMixPrompt.isNotBlank()) {
                                        onGenerateSmartMix(smartMixPrompt)
                                        smartMixPrompt = ""
                                    }
                                },
                                modifier = Modifier.testTag("ai_generate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Generate",
                                    tint = Color(0xFFF27D26)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Recently Played Section
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Played",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyPlayed) { song ->
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier
                                .width(130.dp)
                                .clickable { onSongClick(song) },
                            cornerRadius = 16.dp,
                            testTag = "recent_song_card_${song.id}"
                        ) {
                            Column {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // All Songs Quick Grid
        item {
            Text(
                text = "Your Tracks (${allSongs.size})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        if (allSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks found.\nPoint FloWave to audio folders in Settings -> Library Sources.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(allSongs) { song ->
                SongRowItem(
                    song = song,
                    themeConfig = themeConfig,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    themeConfig: ThemeConfig,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        themeConfig = themeConfig,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        cornerRadius = 16.dp,
        onClick = onClick,
        testTag = "song_row_${song.id}"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!song.moodTags.isNullOrBlank()) {
                    Text(
                        text = song.moodTags.split(",").take(3).joinToString(" • ") { "#${it.trim().removePrefix("#")}" },
                        color = Color(0xFFF27D26),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (song.isLiked) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked",
                    tint = Color(0xFFFF007A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
