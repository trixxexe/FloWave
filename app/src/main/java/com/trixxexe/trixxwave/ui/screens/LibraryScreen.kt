package com.trixxexe.trixxwave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onRescanLibrary: () -> Unit,
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    val tabs = listOf("Playlists (${playlists.size})", "Liked (${likedSongs.size})", "All Songs (${allSongs.size})", "Smart Folders")

    Column(
        modifier = Modifier
            .testTag("library_screen")
            .fillMaxSize()
            .padding(bottom = 120.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Library",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allSongs.size} Total Audio Tracks Available",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
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

        // Library Summary Stats Card (Functionalizes previous empty space)
        LiquidGlassCard(
            themeConfig = themeConfig,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${allSongs.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Tracks", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.15f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${playlists.size}", color = Color(0xFFF27D26), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Playlists", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.15f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${likedSongs.size}", color = Color(0xFFFF007A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Favorites", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFF27D26),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color(0xFFF27D26) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // Playlists Tab
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        // Create Playlist Button Card
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 16.dp,
                            onClick = { showCreatePlaylistDialog = true },
                            testTag = "create_playlist_card"
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF27D26).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Create",
                                        tint = Color(0xFFF27D26),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "+ Create Custom Playlist",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Organize your favorite audio tracks",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (playlists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No custom playlists created yet. Tap above to create one!",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
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
            }
            1 -> {
                // Liked Songs Tab
                if (likedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No liked tracks yet. Tap the heart icon on any song to save it here!",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                } else {
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
            }
            2 -> {
                // All Songs Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter local songs...", color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF27D26)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF27D26),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    val filtered = remember(searchQuery, allSongs) {
                        if (searchQuery.isBlank()) allSongs
                        else allSongs.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.artist.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No audio files found on device. Tap rescan above." else "No matching tracks.",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filtered) { song ->
                                SongRowItem(
                                    song = song,
                                    themeConfig = themeConfig,
                                    onClick = { onSongClick(song) }
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
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

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create New Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF27D26),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF27D26),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName, newPlaylistDesc)
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF27D26))
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF181820)
        )
    }
}
