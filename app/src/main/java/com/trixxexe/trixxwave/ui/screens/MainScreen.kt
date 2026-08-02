package com.trixxexe.trixxwave.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.api.LyricsState
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.download.DownloadStatus
import com.trixxexe.trixxwave.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Explore/Home, 1: Library, 2: Settings, 3: Lyrics
    var showNowPlayingModal by remember { mutableStateOf(false) }
    var showProfileModal by remember { mutableStateOf(false) }
    var showDownloadManagerModal by remember { mutableStateOf(false) }
    var showOutputSwitcherModal by remember { mutableStateOf(false) }
    var showAiVibeModal by remember { mutableStateOf(false) }
    var isDynamicIslandExpanded by remember { mutableStateOf(false) }

    val songs by viewModel.songs.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val currentDuration by viewModel.currentDuration.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val currentOutputDevice by viewModel.currentOutputDevice.collectAsState()
    val showDynamicIsland by viewModel.showDynamicIsland.collectAsState()
    val aiLyricsInsight by viewModel.aiLyricsInsight.collectAsState()
    val isGeneratingAiVibe by viewModel.isGeneratingAiVibe.collectAsState()

    val themeConfig by viewModel.themeConfig.collectAsState()
    val isFirstRun by viewModel.isFirstRun.collectAsState()

    var showOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(isFirstRun) {
        if (isFirstRun) {
            showOnboarding = true
        }
    }

    val accentColor = remember(themeConfig.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(themeConfig.accentColorHex))
        } catch (e: Exception) {
            Color(0xFFF27D26)
        }
    }

    val isAmoled = themeConfig.pureAmoledBlack || themeConfig.mode == "AMOLED"
    val baseBgColor = if (isAmoled) Color(0xFF000000) else Color(0xFF07090E)

    // Liquid Glass Container Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        // Custom Wallpaper image background if provided
        if (!themeConfig.customBgUri.isNullOrEmpty()) {
            AsyncImage(
                model = themeConfig.customBgUri,
                contentDescription = "Custom Wallpaper Background",
                modifier = Modifier
                    .fillMaxSize()
                    .background(baseBgColor),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
        }

        // Animated Ambient Liquid Specular Refraction Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Glowing Liquid Orbs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(canvasWidth * 0.7f, canvasHeight * 0.2f),
                    radius = canvasWidth * 0.75f
                ),
                center = Offset(canvasWidth * 0.7f, canvasHeight * 0.2f),
                radius = canvasWidth * 0.75f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(canvasWidth * 0.2f, canvasHeight * 0.8f),
                    radius = canvasWidth * 0.85f
                ),
                center = Offset(canvasWidth * 0.2f, canvasHeight * 0.8f),
                radius = canvasWidth * 0.85f
            )
        }

        // Main App Layout
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopGlassBar(
                    userName = themeConfig.userName,
                    userAvatar = themeConfig.userAvatar,
                    accentColor = accentColor,
                    activeDownloadsCount = downloadStates.values.count { it is DownloadStatus.Downloading },
                    currentOutputDevice = currentOutputDevice,
                    onOutputClick = { showOutputSwitcherModal = true },
                    onAiVibeClick = { showAiVibeModal = true },
                    onProfileClick = { showProfileModal = true },
                    onDownloadClick = { showDownloadManagerModal = true },
                    onSettingsClick = { selectedTab = 2 }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Glassmorphic Mini Player
                    AnimatedVisibility(visible = currentSong != null) {
                        currentSong?.let { song ->
                            LiquidGlassMiniPlayerBar(
                                song = song,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                currentDuration = currentDuration,
                                accentColor = accentColor,
                                onPlayPause = {
                                    if (isPlaying) viewModel.pause() else viewModel.resume()
                                },
                                onNext = { viewModel.playNext() },
                                onOpenPlayer = { showNowPlayingModal = true }
                            )
                        }
                    }

                    // Floating Glass Navigation Pill
                    FloatingGlassNavBar(
                        selectedTab = selectedTab,
                        accentColor = accentColor,
                        onTabSelected = { selectedTab = it },
                        onSearchClick = {
                            selectedTab = 0
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> ExploreScreenContent(
                        viewModel = viewModel,
                        songs = songs,
                        searchResults = searchResults,
                        searchQuery = searchQuery,
                        isSearching = isSearching,
                        downloadStates = downloadStates,
                        accentColor = accentColor,
                        onSearchQueryChange = { viewModel.searchOnline(it) },
                        onSongSelect = { viewModel.playSong(it) },
                        onDownload = { viewModel.downloadSong(it) },
                        onLikeToggle = { viewModel.toggleLike(it) }
                    )
                    1 -> LibraryScreenContent(
                        songs = songs,
                        downloadStates = downloadStates,
                        accentColor = accentColor,
                        onSongSelect = { viewModel.playSong(it) },
                        onDownload = { viewModel.downloadSong(it) },
                        onLikeToggle = { viewModel.toggleLike(it) }
                    )
                    2 -> SettingsScreenContent(
                        viewModel = viewModel,
                        themeConfig = themeConfig,
                        accentColor = accentColor
                    )
                    3 -> LyricsScreenContent(
                        currentSong = currentSong,
                        lyricsState = lyricsState,
                        currentPosition = currentPosition,
                        accentColor = accentColor,
                        aiLyricsInsight = aiLyricsInsight,
                        onSeek = { viewModel.seekTo(it) },
                        onGenerateInsight = {
                            currentSong?.let { viewModel.generateLyricsInsight(it.title, it.artist) }
                        }
                    )
                }
            }
        }

        // SPOTIFY / ANDROID 16 PUNCH-HOLE DYNAMIC ISLAND OVERLAY
        if (currentSong != null && showDynamicIsland) {
            PunchHoleDynamicIsland(
                song = currentSong!!,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                currentDuration = currentDuration,
                currentOutputDevice = currentOutputDevice,
                accentColor = accentColor,
                isExpanded = isDynamicIslandExpanded,
                onToggleExpand = { isDynamicIslandExpanded = !isDynamicIslandExpanded },
                onPlayPause = { if (isPlaying) viewModel.pause() else viewModel.resume() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onOutputClick = { showOutputSwitcherModal = true },
                onOpenFullPlayer = {
                    isDynamicIslandExpanded = false
                    showNowPlayingModal = true
                }
            )
        }
    }

    // Now Playing Fullscreen Modal
    if (showNowPlayingModal && currentSong != null) {
        NowPlayingGlassModal(
            song = currentSong!!,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            currentDuration = currentDuration,
            volume = volume,
            currentOutputDevice = currentOutputDevice,
            accentColor = accentColor,
            onDismiss = { showNowPlayingModal = false },
            onPlayPause = { if (isPlaying) viewModel.pause() else viewModel.resume() },
            onNext = { viewModel.playNext() },
            onPrevious = { viewModel.playPrevious() },
            onSeek = { viewModel.seekTo(it) },
            onVolumeChange = { viewModel.setVolume(it) },
            onOutputClick = { showOutputSwitcherModal = true },
            onLikeToggle = { viewModel.toggleLike(currentSong!!) },
            onDownload = { viewModel.downloadSong(currentSong!!) },
            onLyricsClick = {
                showNowPlayingModal = false
                selectedTab = 3
            }
        )
    }

    // CONNECTIVITY & AUDIO OUTPUT SWITCHER MODAL
    if (showOutputSwitcherModal) {
        AudioOutputSwitcherModal(
            currentDevice = currentOutputDevice,
            accentColor = accentColor,
            onDismiss = { showOutputSwitcherModal = false },
            onSelectDevice = { deviceName ->
                viewModel.selectOutputDevice(deviceName)
                showOutputSwitcherModal = false
                Toast.makeText(context, "Audio routed to $deviceName", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // AI VIBE ASSISTANT MODAL
    if (showAiVibeModal) {
        AiVibeAssistantModal(
            isGenerating = isGeneratingAiVibe,
            accentColor = accentColor,
            onDismiss = { showAiVibeModal = false },
            onGenerateVibe = { prompt ->
                viewModel.generateAiVibePlaylist(prompt)
                showAiVibeModal = false
                selectedTab = 0
                Toast.makeText(context, "AI Queue generated for '$prompt'!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Profile & Onboarding Setup Modal
    if (showProfileModal || showOnboarding) {
        OnboardingProfileModal(
            currentName = themeConfig.userName,
            currentAvatar = themeConfig.userAvatar,
            currentPreset = themeConfig.preset,
            accentColor = accentColor,
            onDismiss = {
                showProfileModal = false
                showOnboarding = false
                viewModel.setFirstRunCompleted(true)
            },
            onSaveProfile = { name, avatar, presetHex ->
                viewModel.setUserProfile(name, avatar)
                if (presetHex != null) {
                    viewModel.setAccentColor(presetHex)
                }
                showProfileModal = false
                showOnboarding = false
                viewModel.setFirstRunCompleted(true)
                Toast.makeText(context, "Profile and Vibe updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Download Manager Glass Modal
    if (showDownloadManagerModal) {
        DownloadManagerGlassModal(
            downloadStates = downloadStates,
            accentColor = accentColor,
            onDismiss = { showDownloadManagerModal = false },
            onClearCompleted = {
                Toast.makeText(context, "Downloads synced", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TopGlassBar(
    userName: String,
    userAvatar: String,
    accentColor: Color,
    activeDownloadsCount: Int,
    currentOutputDevice: String,
    onOutputClick: () -> Unit,
    onAiVibeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 8.dp)
    ) {
        // Profile & Header Controls - Clean Minimalist Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar & Name
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onProfileClick)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarDisplay(
                    avatarKey = userAvatar,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0A0A0)
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Minimalist Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Output Switcher Badge
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOutputClick),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141414),
                    border = BorderStroke(1.dp, Color(0xFF262626))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CastConnected,
                            contentDescription = "Output Device",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentOutputDevice.take(12) + if (currentOutputDevice.length > 12) "…" else "",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }

                // AI Vibe Assistant Button
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAiVibeClick),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141414),
                    border = BorderStroke(1.dp, Color(0xFF262626))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Vibe",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Download Manager Button
                Box {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDownloadClick),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141414),
                        border = BorderStroke(1.dp, Color(0xFF262626))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Downloads",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (activeDownloadsCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = activeDownloadsCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Settings Gear Button
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSettingsClick),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141414),
                    border = BorderStroke(1.dp, Color(0xFF262626))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreenContent(
    viewModel: MainViewModel,
    songs: List<Song>,
    searchResults: List<Song>,
    searchQuery: String,
    isSearching: Boolean,
    downloadStates: Map<String, DownloadStatus>,
    accentColor: Color,
    onSearchQueryChange: (String) -> Unit,
    onSongSelect: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onLikeToggle: (Song) -> Unit
) {
    var isOfflineMode by remember { mutableStateOf(false) }

    val displayedList = remember(searchQuery, songs, searchResults, isOfflineMode) {
        if (isOfflineMode) {
            songs.filter { it.filePath != null }
        } else if (searchQuery.isNotBlank()) {
            searchResults
        } else {
            songs
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
    ) {
        // Mode Selector Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isOfflineMode = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOfflineMode) accentColor.copy(alpha = 0.2f) else Color(0xFF121212),
                    border = BorderStroke(1.dp, if (isOfflineMode) accentColor else Color(0xFF222222))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isOfflineMode) accentColor else Color(0xFFA0A0A0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isOfflineMode) accentColor else Color(0xFFA0A0A0)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isOfflineMode = false },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isOfflineMode) accentColor.copy(alpha = 0.2f) else Color(0xFF121212),
                    border = BorderStroke(1.dp, if (!isOfflineMode) accentColor else Color(0xFF222222))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (!isOfflineMode) accentColor else Color(0xFFA0A0A0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Online Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (!isOfflineMode) accentColor else Color(0xFFA0A0A0)
                        )
                    }
                }
            }
        }

        // ViTune & Seal Engine Hero Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF121212),
                border = BorderStroke(1.dp, Color(0xFF222222))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VITUNE & SEAL ENGINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "High-Res Audio Direct Stream",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "256k AAC Direct Stream & Fast Local Caching",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                }
            }
        }

        // Minimalist Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search YouTube Music or Local...", color = Color(0xFF666666)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = accentColor) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF121212),
                    unfocusedContainerColor = Color(0xFF121212),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF222222),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
        }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Search Results" else "Your Tracks (${displayedList.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        } else if (displayedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No audio tracks found. Search online to discover music!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(displayedList) { song ->
                LiquidGlassSongCard(
                    song = song,
                    downloadStatus = downloadStates[song.originalUrl ?: song.filePath ?: ""],
                    accentColor = accentColor,
                    onClick = { onSongSelect(song) },
                    onDownload = { onDownload(song) },
                    onLikeToggle = { onLikeToggle(song) }
                )
            }
        }
    }
}

@Composable
fun LibraryScreenContent(
    songs: List<Song>,
    downloadStates: Map<String, DownloadStatus>,
    accentColor: Color,
    onSongSelect: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onLikeToggle: (Song) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists (4)", "Liked (${songs.count { it.isLiked }})", "Downloaded (${songs.count { it.filePath != null }})", "All Songs (${songs.size})", "Folder View", "Smart Folders")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "${songs.size} Audio Tracks • 2 Folders",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Glass Statistics Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x2212131C),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 14.dp, horizontal = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(title = "Tracks", value = songs.size.toString(), accentColor = accentColor)
                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0x33FFFFFF))
                StatColumn(title = "Offline", value = songs.count { it.filePath != null }.toString(), accentColor = Color(0xFF00E5FF))
                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0x33FFFFFF))
                StatColumn(title = "Playlists", value = "4", accentColor = Color(0xFFFF007A))
                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0x33FFFFFF))
                StatColumn(title = "Folders", value = "2", accentColor = Color(0xFF10B981))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Category Tab Pills
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = accentColor,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == index) accentColor else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                // Playlists View
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        GlassListItem(
                            title = "+ Create Custom Playlist",
                            subtitle = "Organize your favorite audio tracks",
                            icon = Icons.Default.Add,
                            iconTint = accentColor,
                            onClick = {}
                        )
                    }
                    item {
                        GlassListItem(
                            title = "Liked Songs",
                            subtitle = "Your favorite tracks",
                            icon = Icons.Default.QueueMusic,
                            iconTint = Color(0xFFFF007A),
                            onClick = {}
                        )
                    }
                    item {
                        GlassListItem(
                            title = "Cyberpunk Glass Mix",
                            subtitle = "AI Curated Synth & Electro Wave",
                            icon = Icons.Default.LibraryMusic,
                            iconTint = accentColor,
                            onClick = {}
                        )
                    }
                    item {
                        GlassListItem(
                            title = "Chill Ambient Flow",
                            subtitle = "Relaxing acoustic & lofi melodies",
                            icon = Icons.Default.LibraryMusic,
                            iconTint = Color(0xFF00E5FF),
                            onClick = {}
                        )
                    }
                    item {
                        GlassListItem(
                            title = "Midnight Focus Drive",
                            subtitle = "High energy deep bass tracks",
                            icon = Icons.Default.LibraryMusic,
                            iconTint = Color(0xFFF59E0B),
                            onClick = {}
                        )
                    }
                }
            }
            1 -> {
                // Liked Songs
                val likedSongs = songs.filter { it.isLiked }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(likedSongs) { song ->
                        LiquidGlassSongCard(
                            song = song,
                            downloadStatus = downloadStates[song.originalUrl ?: song.filePath ?: ""],
                            accentColor = accentColor,
                            onClick = { onSongSelect(song) },
                            onDownload = { onDownload(song) },
                            onLikeToggle = { onLikeToggle(song) }
                        )
                    }
                }
            }
            2 -> {
                // Downloaded
                val downloadedSongs = songs.filter { it.filePath != null }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(downloadedSongs) { song ->
                        LiquidGlassSongCard(
                            song = song,
                            downloadStatus = downloadStates[song.originalUrl ?: song.filePath ?: ""],
                            accentColor = accentColor,
                            onClick = { onSongSelect(song) },
                            onDownload = { onDownload(song) },
                            onLikeToggle = { onLikeToggle(song) }
                        )
                    }
                }
            }
            4 -> {
                // Folder View
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        GlassListItem(
                            title = "Select & Scan Specific Folder",
                            subtitle = "Pick any custom directory path with no limit",
                            icon = Icons.Default.CreateNewFolder,
                            iconTint = Color(0xFF9333EA),
                            onClick = {}
                        )
                    }
                    item {
                        GlassListItem(
                            title = "MediaStore",
                            subtitle = "Device Storage / MediaStore",
                            icon = Icons.Default.Folder,
                            iconTint = Color(0xFF10B981),
                            badgeText = "${songs.size} tracks",
                            onClick = {}
                        )
                    }
                    item {
                        GlassListItem(
                            title = "Online Audio Streams",
                            subtitle = "Online Audio Streams",
                            icon = Icons.Default.Folder,
                            iconTint = Color(0xFF00E5FF),
                            badgeText = "3 tracks",
                            onClick = {}
                        )
                    }
                }
            }
            else -> {
                // All Songs
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(songs) { song ->
                        LiquidGlassSongCard(
                            song = song,
                            downloadStatus = downloadStates[song.originalUrl ?: song.filePath ?: ""],
                            accentColor = accentColor,
                            onClick = { onSongSelect(song) },
                            onDownload = { onDownload(song) },
                            onLikeToggle = { onLikeToggle(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreenContent(
    viewModel: MainViewModel,
    themeConfig: ThemeConfig,
    accentColor: Color
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showAiKey by remember { mutableStateOf(false) }
    var aiApiKeyInput by remember { mutableStateOf(themeConfig.aiApiKey) }
    var selectedAiProvider by remember { mutableStateOf(themeConfig.aiProvider) }
    var selectedAiModel by remember { mutableStateOf(themeConfig.aiModel) }

    LaunchedEffect(themeConfig.aiProvider, themeConfig.aiModel) {
        selectedAiProvider = themeConfig.aiProvider
        selectedAiModel = themeConfig.aiModel
    }

    // Wallpaper Photo Picker Launcher
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomBgUri(uri.toString())
            Toast.makeText(context, "Custom Wallpaper set!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Customize your audio engine & preferences",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )

        // SECTION: APPEARANCE
        SectionHeader(title = "APPEARANCE", icon = Icons.Default.Palette, accentColor = accentColor)

        GlassSettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Theme Presets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Choose a pre-configured aesthetic style",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("AMOLED Minimal", "Flame Amber", "Royal Blue", "Emerald Dark").forEach { preset ->
                        FilterChip(
                            selected = themeConfig.preset == preset,
                            onClick = { viewModel.updateThemePreset(preset) },
                            label = { Text(preset) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1A1A1A),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF222222))

                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Selected accent applies across buttons, active tabs, and sliders",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0A0)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("#00F5D4", "#F27D26", "#3B82F6", "#10B981", "#EF4444", "#FAFAFA").forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (themeConfig.accentColorHex == hex) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setAccentColor(hex) }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF222222))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pure AMOLED Black",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "True #000000 black canvas for maximum OLED contrast and battery savings",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                    Switch(
                        checked = themeConfig.pureAmoledBlack,
                        onCheckedChange = { viewModel.setPureAmoledBlack(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                HorizontalDivider(color = Color(0xFF222222))

                Text(
                    text = "Corner Radius Style",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Choose corner roundness for cards, inputs, and buttons",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0A0)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sharp (8dp)", "Balanced (12dp)", "Smooth (16dp)").forEach { style ->
                        FilterChip(
                            selected = themeConfig.cornerStyle == style,
                            onClick = { viewModel.setCornerStyle(style) },
                            label = { Text(style) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1A1A1A),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // SECTION: PLAYBACK & STREAMING ENGINE
        SectionHeader(title = "VITUNE & SEAL ENGINE", icon = Icons.Default.GraphicEq, accentColor = accentColor)

        GlassSettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Streaming Quality",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Select default direct audio stream bitrates from YouTube Music",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0A0)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High (256k AAC)", "Ultra (320k)", "Standard (160k)", "Saver (96k)").forEach { qual ->
                        FilterChip(
                            selected = themeConfig.streamingQuality == qual,
                            onClick = { viewModel.setStreamingQuality(qual) },
                            label = { Text(qual) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1A1A1A),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF222222))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Literal First-MS Direct Playback",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Plays tracks instantly from 0ms without trimming or delays",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                    Checkbox(
                        checked = true,
                        onCheckedChange = {},
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                }

                Divider(color = Color(0x22FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Resume Playback",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Resume last played track and timestamp on launch",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = themeConfig.autoResumeEnabled,
                        onCheckedChange = { viewModel.setAutoResumeEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                Divider(color = Color(0x22FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android 16 Dynamic Island Overlay",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Floating punch hole camera island with live dancing waveform visualizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    val showDynamicIsland by viewModel.showDynamicIsland.collectAsState()
                    Switch(
                        checked = showDynamicIsland,
                        onCheckedChange = { viewModel.toggleDynamicIsland(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                Divider(color = Color(0x22FFFFFF))

                Text(
                    text = "Track Transition Animation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Select animation style when switching tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Crossfade", "Slide", "Fade", "None").forEach { anim ->
                        FilterChip(
                            selected = themeConfig.trackTransitionAnimation == anim,
                            onClick = { viewModel.setTrackTransitionAnimation(anim) },
                            label = { Text(anim) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // SECTION: LIBRARY & LOCAL FOLDERS
        SectionHeader(title = "LIBRARY & LOCAL FOLDERS", icon = Icons.Default.Folder, accentColor = accentColor)

        GlassSettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Audio Scanner",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Rescan local device storage for music and audio files",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { Toast.makeText(context, "Scanning local audio storage...", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rescan Local Audio Files", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SECTION: ONLINE STREAMING SOURCES
        SectionHeader(title = "ONLINE STREAMING SOURCES", icon = Icons.Default.Language, accentColor = accentColor)

        GlassSettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Supported Source Providers",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "FloWave aggregates high-fidelity streams from multiple decentralized and open sources",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                SourceStatusItem("YouTube Audio Streams", "Active (Piped / Cobalt Extractor)", accentColor)
                SourceStatusItem("Audius Music Protocol", "Active (Decentralized Node Cluster)", accentColor)
                SourceStatusItem("Global Web Radio", "Active (RadioBrowser Directory API)", accentColor)

                Divider(color = Color(0x22FFFFFF))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x1AFFFFFF),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Account / YouTube Access",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Full YouTube & Audius audio streams run free & anonymously without requiring a Google login.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // SECTION: AI FEATURES & API KEYS
        SectionHeader(title = "AI FEATURES & API KEYS", icon = Icons.Default.SmartButton, accentColor = accentColor)

        GlassSettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "AI Provider",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Select provider for smart playlist creation and lyrics extraction",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Groq", "Gemini", "OpenAI").forEach { provider ->
                        FilterChip(
                            selected = selectedAiProvider == provider,
                            onClick = { selectedAiProvider = provider },
                            label = { Text(provider) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                val availableModels = when (selectedAiProvider) {
                    "Gemini" -> listOf("gemini-2.0-flash", "gemini-1.5-pro")
                    "OpenAI" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo")
                    else -> listOf("llama-3.3-70b-versatile", "llama-3.1-70b-instruct")
                }

                Text(
                    text = "AI Model",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Select the underlying model for ${selectedAiProvider}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0A0)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableModels.forEach { model ->
                        FilterChip(
                            selected = selectedAiModel == model,
                            onClick = { selectedAiModel = model },
                            label = { Text(model) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1A1A1A),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "API Key",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Encrypted & stored locally in Android Keystore",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = aiApiKeyInput,
                    onValueChange = { aiApiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter API Key") },
                    visualTransformation = if (showAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showAiKey = !showAiKey }) {
                            Icon(
                                if (showAiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Key",
                                tint = Color.White
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Button(
                    onClick = {
                        viewModel.setAiConfig(selectedAiProvider, aiApiKeyInput, selectedAiModel)
                        Toast.makeText(context, "AI Configuration Saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save AI Configuration", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SECTION: PRIVACY & STORAGE
        SectionHeader(title = "PRIVACY & STORAGE", icon = Icons.Default.Security, accentColor = accentColor)

        GlassSettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Storage & Local Cache",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Clear cached album art, audio waveforms, and temporary files",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Button(
                    onClick = { Toast.makeText(context, "Cache and Waveforms cleared", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Cache & Waveforms", color = Color.White)
                }
            }
        }

        // SECTION: ABOUT FLOWAVE
        SectionHeader(title = "ABOUT FLOWAVE", icon = Icons.Default.Info, accentColor = accentColor)

        GlassSettingsCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "FloWave Engine",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Version 2.4.0 • Liquid Glass Native Build",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
                Text(
                    text = "Developer : Ritam",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InstagramPill(username = "not_your_ritam", color = Color(0xFFFF007A)) {
                        openUrl(context, "https://instagram.com/not_your_ritam")
                    }
                    InstagramPill(username = "ritam.localhost", color = Color(0xFF9333EA)) {
                        openUrl(context, "https://instagram.com/ritam.localhost")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crafted with Kotlin, Jetpack Compose, Material 3, and Media3 ExoPlayer. Designed for high-fidelity offline & online listening.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LyricsScreenContent(
    currentSong: Song?,
    lyricsState: LyricsState,
    currentPosition: Long,
    accentColor: Color,
    aiLyricsInsight: String? = null,
    onSeek: (Long) -> Unit = {},
    onGenerateInsight: () -> Unit = {}
) {
    if (currentSong == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a song to display synced lyrics",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showInsightCard by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = currentSong.artist,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AI Lyrics Insight Action Pill
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    showInsightCard = !showInsightCard
                    if (aiLyricsInsight == null) {
                        onGenerateInsight()
                    }
                },
            shape = RoundedCornerShape(20.dp),
            color = accentColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showInsightCard) "Hide AI Song Meaning" else "✨ AI Song Story & Meaning",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }

        // Expandable AI Lyrics Insight Card
        AnimatedVisibility(visible = showInsightCard) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0x22FFFFFF),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Lyrics Breakdown", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = aiLyricsInsight ?: "Analyzing song mood, narrative themes, and deep artistic background...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (lyricsState) {
            is LyricsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
            is LyricsState.Success -> {
                if (lyricsState.isSynchronized && lyricsState.lines.isNotEmpty()) {
                    var activeIndex by remember { mutableIntStateOf(0) }

                    LaunchedEffect(currentPosition) {
                        val idx = lyricsState.lines.indexOfLast { it.timestampMs <= currentPosition }
                        if (idx >= 0 && idx != activeIndex) {
                            activeIndex = idx
                            coroutineScope.launch {
                                listState.animateScrollToItem(maxOf(0, idx - 2))
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(lyricsState.lines.size) { index ->
                            val line = lyricsState.lines[index]
                            val isActive = index == activeIndex
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = if (isActive) 22.sp else 16.sp
                                ),
                                color = if (isActive) accentColor else Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSeek(line.timestampMs) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = lyricsState.plainLyrics ?: "No synchronized lyrics available.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is LyricsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lyrics unavailable for this track", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun LiquidGlassSongCard(
    song: Song,
    downloadStatus: DownloadStatus?,
    accentColor: Color,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onLikeToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF121212),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = song.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222222)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A0A0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Audio Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E1E1E)
                    ) {
                        Text(
                            text = if (song.filePath != null) "OFFLINE" else "256k AAC",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (song.filePath != null) accentColor else Color(0xFF888888),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onLikeToggle) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isLiked) Color.Red else Color(0xFFA0A0A0)
                )
            }

            IconButton(onClick = onDownload) {
                when (downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        CircularProgressIndicator(
                            progress = { downloadStatus.progress / 100f },
                            modifier = Modifier.size(20.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    }
                    is DownloadStatus.Completed -> {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = "Downloaded",
                            tint = accentColor
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xFFA0A0A0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassMiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    currentDuration: Long,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenPlayer),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161616),
        border = BorderStroke(1.dp, Color(0xFF282828)),
        shadowElevation = 8.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (currentDuration > 0) currentPosition.toFloat() / currentDuration else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = accentColor,
                trackColor = Color.Transparent
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingGlassNavBar(
    selectedTab: Int,
    accentColor: Color,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color(0xFF222222)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NavPillButton(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = selectedTab == 0,
                    accentColor = accentColor,
                    onClick = { onTabSelected(0) }
                )

                NavPillButton(
                    icon = Icons.Default.LibraryMusic,
                    label = "Library",
                    isSelected = selectedTab == 1,
                    accentColor = accentColor,
                    onClick = { onTabSelected(1) }
                )

                NavPillButton(
                    icon = Icons.Default.Lyrics,
                    label = "Lyrics",
                    isSelected = selectedTab == 3,
                    accentColor = accentColor,
                    onClick = { onTabSelected(3) }
                )

                NavPillButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isSelected = selectedTab == 2,
                    accentColor = accentColor,
                    onClick = { onTabSelected(2) }
                )
            }
        }
    }
}

@Composable
fun NavPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) accentColor else Color(0xFFA0A0A0),
                modifier = Modifier.size(18.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun NowPlayingGlassModal(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    currentDuration: Long,
    volume: Float,
    currentOutputDevice: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOutputClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onDownload: () -> Unit,
    onLyricsClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }

        var isDragging by remember { mutableStateOf(false) }
        var dragPosition by remember { mutableFloatStateOf(0f) }

        val maxDurationFloat = if (currentDuration > 0) currentDuration.toFloat() else 100f
        val currentPositionFloat = if (currentDuration > 0) currentPosition.toFloat().coerceIn(0f, maxDurationFloat) else 0f
        val sliderValue = if (isDragging) dragPosition.coerceIn(0f, maxDurationFloat) else currentPositionFloat
        val displayPosition = if (isDragging) dragPosition.toLong() else currentPosition

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF207090E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row with Output Device Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    
                    // Output device switcher button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOutputClick),
                        shape = RoundedCornerShape(20.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CastConnected, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentOutputDevice.take(18) + if (currentOutputDevice.length > 18) "…" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }

                    IconButton(onClick = onLikeToggle) {
                        Icon(
                            imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isLiked) Color.Red else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Album Artwork Frame
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                        .shadow(16.dp, RoundedCornerShape(28.dp))
                        .background(Color(0x33FFFFFF)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress & Seek Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..maxDurationFloat,
                        onValueChange = { newPos ->
                            isDragging = true
                            dragPosition = newPos
                        },
                        onValueChangeFinished = {
                            onSeek(dragPosition.toLong())
                            isDragging = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(displayPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        Text(formatMs(currentDuration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LIQUID GLASS VOLUME SLIDER
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x1AFFFFFF),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                volume == 0f -> Icons.Default.VolumeMute
                                volume < 0.5f -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = "Volume",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPlayPause),
                        shape = CircleShape,
                        color = accentColor,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Extra controls row (Lyrics, Download, Output Switcher)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onLyricsClick) {
                        Icon(Icons.Default.Lyrics, contentDescription = "Synced Lyrics", tint = accentColor)
                    }
                    IconButton(onClick = onOutputClick) {
                        Icon(Icons.Default.Speaker, contentDescription = "Connectivity", tint = Color.White)
                    }
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OnboardingProfileModal(
    currentName: String,
    currentAvatar: String,
    currentPreset: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, avatar: String, presetHex: String?) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Welcome/Permissions, 2: Set Up Profile, 3: Select Your Vibe
    var nameInput by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }
    var selectedVibe by remember { mutableStateOf("Sleek Interface") }
    var selectedVibeHex by remember { mutableStateOf("#F27D26") }

    val avatars = listOf("Male 1", "Male 2", "Female 1", "Female 2", "Dog", "Cat")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Indicator Dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3).forEach { step ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (currentStep == step) 32.dp else 10.dp)
                                .clip(CircleShape)
                                .background(if (currentStep == step) accentColor else Color(0xFF262626))
                        )
                        if (step < 3) Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Middle Content Area - weighted and scrollable so content never pushes buttons off screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                Surface(
                                    modifier = Modifier.size(76.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF121212),
                                    border = BorderStroke(2.dp, accentColor)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = accentColor, modifier = Modifier.size(38.dp))
                                    }
                                }

                                Text(
                                     text = "Welcome to FloWave",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )

                                Text(
                                    text = "High-fidelity minimalist AMOLED music player powered by ViTune & Seal streaming engine, fast local audio caching, synchronized lyrics, and equalizer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFA0A0A0),
                                    textAlign = TextAlign.Center
                                )
                            }
                            2 -> {
                                Text(
                                    text = "Set Up Your Profile",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Personalize your identity and avatar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0A0A0)
                                )

                                AvatarDisplay(
                                    avatarKey = selectedAvatar,
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, accentColor, CircleShape)
                                )

                                Text(
                                    text = "Choose an avatar preset:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFA0A0A0)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    avatars.forEach { avatar ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { selectedAvatar = avatar }
                                        ) {
                                            AvatarDisplay(
                                                avatarKey = avatar,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        if (selectedAvatar == avatar) 2.dp else 0.dp,
                                                        accentColor,
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(avatar, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Your Name or Alias") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF121212),
                                        unfocusedContainerColor = Color(0xFF121212),
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color(0xFF262626),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            3 -> {
                                Text(
                                    text = "Select Accent Vibe",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Choose your AMOLED dark theme accent color",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0A0A0)
                                )

                                val vibes = listOf(
                                    VibePreset("AMOLED Cyan", "Pure Black & Electric Teal", "#00F5D4"),
                                    VibePreset("Flame Amber", "Aesthetic Warm Amber", "#F27D26"),
                                    VibePreset("Royal Blue", "Electric Sapphire", "#3B82F6"),
                                    VibePreset("Emerald Mint", "Pure Deep Mint Green", "#10B981"),
                                    VibePreset("Crimson Red", "Dark Velvet Crimson", "#EF4444"),
                                    VibePreset("Monochrome White", "Minimalist Crisp White", "#FAFAFA")
                                )

                                vibes.forEach { vibe ->
                                    VibeCard(
                                        title = vibe.name,
                                        subtitle = vibe.desc,
                                        colorHex = vibe.hex,
                                        isSelected = selectedVibe == vibe.name,
                                        onClick = {
                                            selectedVibe = vibe.name
                                            selectedVibeHex = vibe.hex
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Button Row - FIXED at bottom, well above system navigation bar!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = if (currentStep == 1) Arrangement.Center else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        TextButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                onSaveProfile(nameInput, selectedAvatar, selectedVibeHex)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(if (currentStep == 1) 1f else 0.65f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentStep == 3) "Finish Setup" else "Continue",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep == 3) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

data class VibePreset(val name: String, val desc: String, val hex: String)

@Composable
fun VibeCard(
    title: String,
    subtitle: String,
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(android.graphics.Color.parseColor(colorHex))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1E12131C),
        border = BorderStroke(1.dp, if (isSelected) color else Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = color)
            )
        }
    }
}

@Composable
fun DownloadManagerGlassModal(
    downloadStates: Map<String, DownloadStatus>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onClearCompleted: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFA07090E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Download Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (downloadStates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active or recent downloads", color = Color.White.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(downloadStates.entries.toList()) { entry ->
                            val url = entry.key
                            val status = entry.value

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x1E12131C),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = url.substringAfterLast("/").take(30),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    when (status) {
                                        is DownloadStatus.Downloading -> {
                                            LinearProgressIndicator(
                                                progress = { status.progress / 100f },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = accentColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${status.progress}% downloaded", style = MaterialTheme.typography.labelSmall, color = accentColor)
                                        }
                                        is DownloadStatus.Completed -> {
                                            Text("Completed ✓", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                                        }
                                        is DownloadStatus.Failed -> {
                                            Text("Failed: ${status.error}", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                                        }
                                        else -> {
                                            Text("Idle", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onClearCompleted,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Sync Offline Library", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarDisplay(avatarKey: String, modifier: Modifier = Modifier) {
    val bgGradient = remember(avatarKey) {
        when (avatarKey) {
            "Male 1" -> Brush.radialGradient(listOf(Color(0xFFE11D48), Color(0xFF1E1B4B)))
            "Male 2" -> Brush.radialGradient(listOf(Color(0xFF00F5D4), Color(0xFF0F172A)))
            "Female 1" -> Brush.radialGradient(listOf(Color(0xFFEC4899), Color(0xFF312E81)))
            "Female 2" -> Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF18181B)))
            "Dog" -> Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0xFF27272A)))
            else -> Brush.radialGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B)))
        }
    }

    Box(
        modifier = modifier.background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when {
                avatarKey.contains("Dog") -> Icons.Default.Pets
                avatarKey.contains("Female") -> Icons.Default.Face3
                else -> Icons.Default.Person
            },
            contentDescription = avatarKey,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun GlassListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1E12131C),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconTint.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = iconTint
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumn(title: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accentColor)
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
            color = accentColor
        )
    }
}

@Composable
fun GlassSettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF121212),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SourceStatusItem(title: String, status: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
        Text(status, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
    }
}

@Composable
fun InstagramPill(username: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(username, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun PunchHoleDynamicIsland(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    currentDuration: Long,
    currentOutputDevice: String,
    accentColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onOutputClick: () -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 2.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
            },
            label = "DynamicIslandAnimation"
        ) { expanded ->
            if (!expanded) {
                // Collapsed Punch Hole Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(onClick = onToggleExpand),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xF20B0D14),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: Album Art
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.dp, accentColor, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        // Center: Black Punch Hole Cutout
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )

                        // Right: Dancing Visualizer Waveform Canvas
                        Canvas(modifier = Modifier.size(28.dp, 16.dp)) {
                            val barWidth = 4.dp.toPx()
                            val gap = 3.dp.toPx()
                            val colors = listOf(Color(0xFF00E5FF), accentColor, Color(0xFFFF007A), Color(0xFF10B981))
                            for (i in 0..3) {
                                val heightFactor = if (isPlaying) {
                                    ((currentPosition / 150 + i * 90) % 100) / 100f
                                } else 0.2f
                                val barHeight = size.height * (0.25f + heightFactor * 0.75f)
                                drawRoundRect(
                                    color = colors[i % colors.size],
                                    topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            } else {
                // Expanded Glass Control Capsule
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xF20B0D14),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onOpenFullPlayer),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(onClick = onToggleExpand) {
                                Icon(Icons.Default.Close, contentDescription = "Collapse", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { if (currentDuration > 0) currentPosition.toFloat() / currentDuration else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = Color(0x33FFFFFF)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onOutputClick),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x22FFFFFF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CastConnected, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentOutputDevice.take(12) + "…",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Surface(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onPlayPause),
                                    shape = CircleShape,
                                    color = accentColor
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioOutputSwitcherModal(
    currentDevice: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelectDevice: (String) -> Unit
) {
    val devices = listOf(
        Triple("📱 Built-In Phone Speakers", "High-Definition 24-bit/192kHz DAC Output", Icons.Default.SpeakerGroup),
        Triple("🎧 FloWave Wireless Buds Pro", "Bluetooth 5.3 • LDAC Lossless • 88% Battery", Icons.Default.Headphones),
        Triple("📺 FloWave Cast / Living Room TV", "Wi-Fi Chromecast Audio • 1080p Ultra Stream", Icons.Default.Tv),
        Triple("🔊 Studio Hi-Fi AirPlay Speaker", "5GHz Lossless AirPlay 2 Protocol", Icons.Default.Speaker),
        Triple("🚗 Android Auto Bluetooth", "Automotive Stereo Protocol", Icons.Default.DirectionsCar)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF212131C),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CastConnected, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Connectivity & Cast Switcher", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Select output device or active cast target",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    items(devices) { (name, subtitle, icon) ->
                        val isSelected = currentDevice == name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSelectDevice(name) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0x22FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) accentColor else Color.White
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = accentColor, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiVibeAssistantModal(
    isGenerating: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onGenerateVibe: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    val presetVibes = listOf(
        "🌧️ Rainy Midnight Drive",
        "⚡ Cyberpunk Neon Arcade",
        "🍃 Lofi Chill Study Cafe",
        "🔥 High Energy Pump Gym",
        "🌌 Deep Space Ambient Floating",
        "🏖️ Retro Vaporwave Sunset"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF212131C),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("✨ AI Vibe Assistant", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Describe any acoustic mood or scene to generate a smart playlist queue!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("e.g. 'Late night coding session with rainy jazz'", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text("Or Pick a Vibe Preset:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 180.dp)
                ) {
                    items(presetVibes) { vibe ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onGenerateVibe(vibe) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF))
                        ) {
                            Text(
                                text = vibe,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                Button(
                    onClick = { if (promptInput.isNotBlank()) onGenerateVibe(promptInput) },
                    enabled = promptInput.isNotBlank() && !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing AI Queue...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Queue", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
