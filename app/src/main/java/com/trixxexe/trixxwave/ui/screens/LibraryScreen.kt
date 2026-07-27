package com.trixxexe.trixxwave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.db.Playlist
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import java.io.File

enum class LibrarySortOption(val label: String) {
    A_TO_Z("Title: A → Z"),
    Z_TO_A("Title: Z → A"),
    ARTIST_A_TO_Z("Artist: A → Z"),
    DATE_NEWEST("Download Time: Newest"),
    DATE_OLDEST("Download Time: Oldest"),
    DURATION_DESC("Duration: Longest First")
}

fun getFolderForSong(song: Song): String {
    val path = song.filePath
    if (path.startsWith("content://")) {
        return if (song.source == "DOWNLOADED") "Downloaded Media" else "Device Storage / MediaStore"
    }
    if (path.startsWith("http://") || path.startsWith("https://")) {
        return "Online Audio Streams"
    }
    val file = File(path)
    return file.parent ?: "Root Storage"
}

@Composable
fun LibraryScreen(
    playlists: List<Playlist>,
    likedSongs: List<Song>,
    allSongs: List<Song>,
    themeConfig: ThemeConfig,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song) -> Unit,
    onRescanLibrary: () -> Unit,
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> },
    onScanCustomFolder: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    // Sort & Folder state
    var selectedSortOption by remember { mutableStateOf(LibrarySortOption.DATE_NEWEST) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var showFolderSelectDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var customFolderPathInput by remember { mutableStateOf("") }

    val downloadedSongs = remember(allSongs) {
        allSongs.filter { it.source == "DOWNLOADED" || it.filePath.contains("/downloads/", ignoreCase = true) || it.filePath.contains("/download/", ignoreCase = true) }
    }

    // Extract all unique folders from allSongs
    val detectedFolders = remember(allSongs) {
        allSongs.map { getFolderForSong(it) }.distinct().sorted()
    }

    // Process song filtering and sorting
    fun processSongList(sourceList: List<Song>): List<Song> {
        var filtered = sourceList
        if (!selectedFolderPath.isNullOrBlank()) {
            val folder = selectedFolderPath!!
            filtered = filtered.filter { song ->
                val songFolder = getFolderForSong(song)
                songFolder.equals(folder, ignoreCase = true) || song.filePath.startsWith(folder, ignoreCase = true)
            }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            filtered = filtered.filter {
                it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) || it.album.contains(q, ignoreCase = true)
            }
        }
        return when (selectedSortOption) {
            LibrarySortOption.A_TO_Z -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.Z_TO_A -> filtered.sortedByDescending { it.title.lowercase() }
            LibrarySortOption.ARTIST_A_TO_Z -> filtered.sortedBy { it.artist.lowercase() }
            LibrarySortOption.DATE_NEWEST -> filtered.sortedByDescending { it.dateAdded }
            LibrarySortOption.DATE_OLDEST -> filtered.sortedBy { it.dateAdded }
            LibrarySortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
        }
    }

    val processedDownloadedSongs = remember(downloadedSongs, selectedFolderPath, selectedSortOption, searchQuery) {
        processSongList(downloadedSongs)
    }

    val processedAllSongs = remember(allSongs, selectedFolderPath, selectedSortOption, searchQuery) {
        processSongList(allSongs)
    }

    val processedLikedSongs = remember(likedSongs, selectedFolderPath, selectedSortOption, searchQuery) {
        processSongList(likedSongs)
    }

    val tabs = listOf(
        "Playlists (${playlists.size})",
        "Liked (${likedSongs.size})",
        "Downloaded (${downloadedSongs.size})",
        "All Songs (${allSongs.size})",
        "Folder View",
        "Smart Folders"
    )

    Column(
        modifier = Modifier
            .testTag("library_screen")
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                    text = "${allSongs.size} Audio Tracks • ${detectedFolders.size} Folders",
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

        // Summary Stats Card
        LiquidGlassCard(
            themeConfig = themeConfig,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
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
                    Text(text = "${downloadedSongs.size}", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Offline", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.15f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${playlists.size}", color = Color(0xFFF27D26), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Playlists", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.15f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${detectedFolders.size}", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Folders", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

        // Persistent Sort & Folder Filter Control Bar (Shown on Offline, Downloaded, Liked, and All Songs tabs)
        if (selectedTab == 1 || selectedTab == 2 || selectedTab == 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder Filter Chip
                Surface(
                    onClick = { showFolderSelectDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedFolderPath != null) Color(0xFFF27D26).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFolderPath != null) Color(0xFFF27D26) else Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = if (selectedFolderPath != null) Color(0xFFF27D26) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFolderPath != null) {
                                val name = File(selectedFolderPath!!).name.ifBlank { selectedFolderPath!! }
                                "Folder: $name"
                            } else "All Folders",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        if (selectedFolderPath != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Folder Filter",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedFolderPath = null }
                            )
                        }
                    }
                }

                // Sort Mode Selector
                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedSortOption.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1E2A))
                    ) {
                        LibrarySortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (option == selectedSortOption) Color(0xFF00F0FF) else Color.White,
                                        fontWeight = if (option == selectedSortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedSortOption = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Playlists Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    item {
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
                if (processedLikedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedFolderPath != null) "No liked tracks in selected folder." else "No liked tracks yet. Tap the heart icon on any song to save it here!",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        items(processedLikedSongs) { song ->
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
                // Downloaded Songs Tab
                if (processedDownloadedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedFolderPath != null) "No offline tracks in selected folder." else "No downloaded tracks yet. Download any online track to play offline anytime!",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        items(processedDownloadedSongs) { song ->
                            SongRowItem(
                                song = song,
                                themeConfig = themeConfig,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            }
            3 -> {
                // All Songs Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter local songs by name/artist...", color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF27D26)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.LightGray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF27D26),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    if (processedAllSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedFolderPath != null) "No audio files match folder + search criteria." else if (searchQuery.isBlank()) "No audio files found on device." else "No matching tracks.",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 140.dp)
                        ) {
                            items(processedAllSongs) { song ->
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
            4 -> {
                // Folder View (Dedicated folder grouping)
                val folderGroups = remember(allSongs) {
                    allSongs.groupBy { getFolderForSong(it) }.toList().sortedByDescending { it.second.size }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    item {
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 16.dp,
                            onClick = { showFolderSelectDialog = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFA855F7).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = "Scan Folder",
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Select & Scan Specific Folder",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pick any custom directory path with no limit",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    items(folderGroups) { (folderPath, songsInFolder) ->
                        val isSelected = selectedFolderPath == folderPath
                        LiquidGlassCard(
                            themeConfig = themeConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            cornerRadius = 16.dp,
                            onClick = {
                                selectedFolderPath = if (isSelected) null else folderPath
                                selectedTab = 3 // Switch to All Songs filtered by this folder
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Folder",
                                        tint = if (isSelected) Color(0xFFF27D26) else Color(0xFFA855F7),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = File(folderPath).name.ifBlank { folderPath },
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = folderPath,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${songsInFolder.size} tracks",
                                        color = Color(0xFF00F0FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            5 -> {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
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

    // Folder Selection & Custom Path Scanner Dialog
    if (showFolderSelectDialog) {
        AlertDialog(
            onDismissRequest = { showFolderSelectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFF27D26))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select / Scan Specific Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Show songs from a specific folder only, or scan any directory on your device without limits:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // All Folders Reset Button
                    Surface(
                        onClick = {
                            selectedFolderPath = null
                            showFolderSelectDialog = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedFolderPath == null) Color(0xFFF27D26).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFolderPath == null) Color(0xFFF27D26) else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFFF27D26))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("All Folders (${allSongs.size} tracks)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Detected Music Directories:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // List detected folders
                    LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                        items(detectedFolders) { folder ->
                            val count = allSongs.count { getFolderForSong(it) == folder }
                            val isSelected = selectedFolderPath == folder
                            Surface(
                                onClick = {
                                    selectedFolderPath = folder
                                    showFolderSelectDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFF27D26).copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = File(folder).name.ifBlank { folder },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(text = "$count tracks", color = Color(0xFF00F0FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Enter Any Custom Folder Path:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customFolderPathInput,
                        onValueChange = { customFolderPathInput = it },
                        placeholder = { Text("/storage/emulated/0/Music", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF27D26),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
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
                        if (customFolderPathInput.isNotBlank()) {
                            onScanCustomFolder(customFolderPathInput.trim())
                            selectedFolderPath = customFolderPathInput.trim()
                            showFolderSelectDialog = false
                        } else {
                            showFolderSelectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF27D26))
                ) {
                    Text("Apply & Scan Path", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderSelectDialog = false }) {
                    Text("Close", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF181824)
        )
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
