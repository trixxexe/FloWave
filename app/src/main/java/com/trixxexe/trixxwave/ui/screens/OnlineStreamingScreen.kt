package com.trixxexe.trixxwave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.db.Playlist
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineStreamingScreen(
    themeConfig: ThemeConfig,
    activeTab: String,
    youtubeResults: List<Song>,
    audiusTracks: List<Song>,
    radioStations: List<Song>,
    playlists: List<Playlist> = emptyList(),
    downloadStatusMap: Map<Long, Float> = emptyMap(),
    isExtractingStream: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onTabSelected: (String) -> Unit,
    onExtractYoutubeUrl: (String) -> Unit,
    onSearchYoutube: (String) -> Unit,
    onSearchAudius: (String) -> Unit,
    onSearchRadio: (String) -> Unit,
    onPlayTrack: (Song) -> Unit,
    onToggleLike: (Song) -> Unit = {},
    onAddToPlaylist: (Long, Song) -> Unit = { _, _ -> },
    onDownloadSong: (Song) -> Unit = {},
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> }
) {
    val accentColor = getThemeAccentColor(themeConfig)
    var urlOrSearchInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (selectedSongForPlaylist != null) {
        AlertDialog(
            onDismissRequest = { selectedSongForPlaylist = null },
            containerColor = Color(0xFF18181A),
            title = {
                Text(
                    text = "Add to Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select a playlist to add '${selectedSongForPlaylist?.title}':",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No custom playlists found. Create one below!",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists) { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedSongForPlaylist?.let { song ->
                                                onAddToPlaylist(pl.id, song)
                                            }
                                            selectedSongForPlaylist = null
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = pl.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = true
                }) {
                    Text("+ New Playlist", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSongForPlaylist = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = Color(0xFF18181A),
            title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist Name", color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName, "Custom Playlist")
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("online_streaming_screen")
    ) {
        // Liquid Glass Sub-Tab Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                "YOUTUBE" to ("YouTube Audio" to Icons.Default.Subscriptions),
                "AUDIUS" to ("Audius Discover" to Icons.Default.MusicNote),
                "RADIO" to ("Live Radio" to Icons.Default.Radio)
            )

            tabs.forEach { (tabKey, pair) ->
                val (label, icon) = pair
                val isSelected = activeTab == tabKey

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onTabSelected(tabKey) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Search / Keyword Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlOrSearchInput,
                onValueChange = { urlOrSearchInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("online_search_input"),
                placeholder = {
                    Text(
                        text = when (activeTab) {
                            "YOUTUBE" -> "Search Song Name, Artist, or Paste Link..."
                            "AUDIUS" -> "Search Audius Artists, Tracks..."
                            else -> "Search Radio Stations by Name or Genre..."
                        },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (urlOrSearchInput.startsWith("http")) Icons.Default.Link else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accentColor
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    when (activeTab) {
                        "YOUTUBE" -> {
                            val trimmed = urlOrSearchInput.trim()
                            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("youtube.com/") || trimmed.contains("youtu.be/")) {
                                onExtractYoutubeUrl(trimmed)
                            } else {
                                onSearchYoutube(trimmed)
                            }
                        }
                        "AUDIUS" -> onSearchAudius(urlOrSearchInput)
                        "RADIO" -> onSearchRadio(urlOrSearchInput)
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    when (activeTab) {
                        "YOUTUBE" -> {
                            val trimmed = urlOrSearchInput.trim()
                            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("youtube.com/") || trimmed.contains("youtu.be/")) {
                                onExtractYoutubeUrl(trimmed)
                            } else {
                                onSearchYoutube(trimmed)
                            }
                        }
                        "AUDIUS" -> onSearchAudius(urlOrSearchInput)
                        "RADIO" -> onSearchRadio(urlOrSearchInput)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (activeTab == "YOUTUBE" && urlOrSearchInput.startsWith("http")) "Extract" else "Search",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Black
                )
            }
        }

        // Extraction / Loading / Error Indicators
        if (isExtractingStream) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Extracting high-fidelity YouTube stream...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        }

        // Tab Content List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp, top = 8.dp)
        ) {
            when (activeTab) {
                "YOUTUBE" -> {
                    if (youtubeResults.isEmpty() && !isLoading && !isExtractingStream) {
                        item {
                            EmptyStatePlaceholder(
                                title = "YouTube Audio Streaming",
                                description = "Search any song name or artist keyword above (e.g., 'Anuv Jain', 'Arz Kiya Hai') to play or download instantly.",
                                icon = Icons.Default.Subscriptions,
                                accentColor = accentColor
                            )
                        }
                    } else {
                        items(youtubeResults) { song ->
                            OnlineTrackRow(
                                song = song,
                                themeConfig = themeConfig,
                                downloadProgress = downloadStatusMap[song.id],
                                onPlayClick = { onPlayTrack(song) },
                                onToggleLike = { onToggleLike(song) },
                                onAddToPlaylistClick = { selectedSongForPlaylist = song },
                                onDownloadClick = { onDownloadSong(song) }
                            )
                        }
                    }
                }

                "AUDIUS" -> {
                    if (audiusTracks.isEmpty() && !isLoading) {
                        item {
                            EmptyStatePlaceholder(
                                title = "Discover Open Audius Catalog",
                                description = "Explore trending independent electronic, hip-hop, ambient tracks or search the open catalog.",
                                icon = Icons.Default.MusicNote,
                                accentColor = accentColor
                            )
                        }
                    } else {
                        items(audiusTracks) { song ->
                            OnlineTrackRow(
                                song = song,
                                themeConfig = themeConfig,
                                downloadProgress = downloadStatusMap[song.id],
                                onPlayClick = { onPlayTrack(song) },
                                onToggleLike = { onToggleLike(song) },
                                onAddToPlaylistClick = { selectedSongForPlaylist = song },
                                onDownloadClick = { onDownloadSong(song) }
                            )
                        }
                    }
                }

                "RADIO" -> {
                    if (radioStations.isEmpty() && !isLoading) {
                        item {
                            EmptyStatePlaceholder(
                                title = "Global Live Radio Stations",
                                description = "Stream thousands of worldwide live radio stations (Icecast, .m3u8, AAC).",
                                icon = Icons.Default.Radio,
                                accentColor = accentColor
                            )
                        }
                    } else {
                        items(radioStations) { station ->
                            OnlineTrackRow(
                                song = station,
                                themeConfig = themeConfig,
                                isRadio = true,
                                downloadProgress = downloadStatusMap[station.id],
                                onPlayClick = { onPlayTrack(station) },
                                onToggleLike = { onToggleLike(station) },
                                onAddToPlaylistClick = { selectedSongForPlaylist = station },
                                onDownloadClick = { onDownloadSong(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineTrackRow(
    song: Song,
    themeConfig: ThemeConfig,
    isRadio: Boolean = false,
    downloadProgress: Float? = null,
    onPlayClick: () -> Unit,
    onToggleLike: () -> Unit = {},
    onAddToPlaylistClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {}
) {
    val accentColor = getThemeAccentColor(themeConfig)

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onPlayClick),
        themeConfig = themeConfig,
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork / Favicon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isRadio) Icons.Default.Radio else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!song.genre.isNullOrBlank() || !song.album.isNullOrBlank()) {
                    Text(
                        text = song.genre ?: song.album,
                        color = accentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Action Buttons: Like, Add to Playlist, Download, Play
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like Button
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Song",
                        tint = if (song.isLiked) Color.Red else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Add to Playlist Button
                IconButton(
                    onClick = onAddToPlaylistClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Download Button / Progress
                if (!isRadio) {
                    if (downloadProgress != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (downloadProgress >= 1.0f) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Downloaded",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    color = accentColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (song.source == "DOWNLOADED") Icons.Default.Check else Icons.Default.Download,
                                contentDescription = "Download Song",
                                tint = if (song.source == "DOWNLOADED") accentColor else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Play Button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.25f))
                        .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Stream",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 18.sp
        )
    }
}
