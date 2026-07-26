package com.trixxexe.trixxwave.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.db.Playlist
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard

@Composable
fun LibraryScreen(
    playlists: List<Playlist>,
    likedSongs: List<Song>,
    allSongs: List<Song>,
    themeConfig: ThemeConfig,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song) -> Unit,
    onRescanLibrary: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Playlists", "Liked Tracks", "Smart Folders")

    Column(
        modifier = Modifier
            .testTag("library_screen")
            .fillMaxSize()
            .padding(bottom = 120.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onRescanLibrary,
                modifier = Modifier.testTag("rescan_library_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = Color(0xFFF27D26)
                )
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFF27D26),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color(0xFFF27D26) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Playlists Tab
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlists) { playlist ->
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 16.dp,
                            onClick = { onPlaylistClick(playlist) },
                            testTag = "playlist_item_${playlist.id}"
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Playlist",
                                    tint = Color(0xFFF27D26),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = playlist.description ?: "Local Playlist",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Liked Songs Tab
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(likedSongs) { song ->
                        SongRowItem(
                            song = song,
                            themeConfig = themeConfig,
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }
            2 -> {
                // Smart Folders
                val smartFolders = listOf(
                    Triple(
                        Playlist(
                            id = -100L,
                            name = "Recently Added",
                            description = "30 most recently added tracks",
                            isAutoGenerated = true
                        ),
                        "Recently Added",
                        "${allSongs.take(30).size} tracks"
                    ),
                    Triple(
                        Playlist(
                            id = -101L,
                            name = "Most Played This Month",
                            description = "Top 30 frequent tracks",
                            isAutoGenerated = true
                        ),
                        "Most Played This Month",
                        "Top 30 frequent tracks"
                    ),
                    Triple(
                        Playlist(
                            id = -102L,
                            name = "Forgotten Favorites",
                            description = "Liked & saved tracks",
                            isAutoGenerated = true
                        ),
                        "Forgotten Favorites",
                        "${likedSongs.size} liked tracks"
                    )
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(smartFolders) { (playlist, folderName, subtitle) ->
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 16.dp,
                            onClick = { onPlaylistClick(playlist) },
                            testTag = "smart_folder_${playlist.id}"
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = Color(0xFF7B2CBF),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = folderName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = subtitle,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
