package com.trixxexe.trixxwave

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trixxexe.trixxwave.data.db.Playlist
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.service.DynamicIslandOverlayService
import com.trixxexe.trixxwave.service.PlaybackService
import com.trixxexe.trixxwave.ui.components.glass.AmbientGlassBackground
import com.trixxexe.trixxwave.ui.components.glass.DynamicIslandPlayer
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassNavigationBar
import com.trixxexe.trixxwave.ui.components.glass.MiniPlayer
import com.trixxexe.trixxwave.ui.components.glass.OverlayPermissionDialog
import com.trixxexe.trixxwave.ui.components.glass.PlaylistDetailDialog
import com.trixxexe.trixxwave.ui.components.glass.TopGlassmorphicClock
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor
import com.trixxexe.trixxwave.ui.screens.*
import com.trixxexe.trixxwave.ui.theme.TrixxWaveTheme
import com.trixxexe.trixxwave.ui.viewmodel.MainViewModel
import com.trixxexe.trixxwave.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var playbackService: PlaybackService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            playbackService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely bind PlaybackService
        try {
            val serviceIntent = Intent(this, PlaybackService::class.java)
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setContent {
            val themeConfig by mainViewModel.themeConfig.collectAsState()
            val activeProfile by mainViewModel.activeProfile.collectAsState()
            val allSongs by mainViewModel.allSongs.collectAsState()
            val likedSongs by mainViewModel.likedSongs.collectAsState()
            val recentlyPlayed by mainViewModel.recentlyPlayed.collectAsState()
            val playlists by mainViewModel.playlists.collectAsState()

            val currentSong by mainViewModel.currentSong.collectAsState()
            val isPlaying by mainViewModel.isPlaying.collectAsState()
            val currentPositionMs by mainViewModel.currentPositionMs.collectAsState()
            val lyrics by mainViewModel.currentLyrics.collectAsState()
            val aiInsight by mainViewModel.aiTrackInsight.collectAsState()

            val aiConfig by settingsViewModel.aiConfig.collectAsState()
            val testStatus by settingsViewModel.testStatus.collectAsState()
            val fetchedModels by settingsViewModel.fetchedModels.collectAsState()
            val isFetchingModels by settingsViewModel.isFetchingModels.collectAsState()
            val fetchModelsStatus by settingsViewModel.fetchModelsStatus.collectAsState()
            val profiles by settingsViewModel.profiles.collectAsState()
            val isFirstRun by settingsViewModel.isFirstRun.collectAsState()

            var showNowPlayingModal by remember { mutableStateOf(false) }
            var showOverlayPermissionDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, themeConfig.dynamicIslandEnabled) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    try {
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_START,
                            androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                                DynamicIslandOverlayService.stopService(context)
                            }
                            androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                                if (themeConfig.dynamicIslandEnabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                                        DynamicIslandOverlayService.startService(context)
                                    } else {
                                        showOverlayPermissionDialog = true
                                    }
                                }
                            }
                            else -> {}
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val gaplessState by mainViewModel.gaplessState.collectAsState()
            val recentSearches by mainViewModel.recentSearches.collectAsState()

            val isOnlineMode by mainViewModel.isOnlineMode.collectAsState()
            val activeOnlineTab by mainViewModel.activeOnlineTab.collectAsState()
            val youtubeResults by mainViewModel.youtubeSearchResults.collectAsState()
            val audiusTracks by mainViewModel.audiusTrending.collectAsState()
            val radioStations by mainViewModel.radioStations.collectAsState()
            val isExtractingStream by mainViewModel.isExtractingStream.collectAsState()
            val isOnlineSearchLoading by mainViewModel.isOnlineSearchLoading.collectAsState()
            val onlineStreamError by mainViewModel.onlineStreamError.collectAsState()
            val downloadStatusMap by mainViewModel.downloadStatusMap.collectAsState()

            val permissionsToRequest = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val allGranted = permissionsMap.values.all { it }
                if (allGranted) {
                    settingsViewModel.rescanLibrary()
                }
            }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissionsToRequest)
            }

            var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
            val playlistSongsFlow = remember(selectedPlaylistForDetail, allSongs, likedSongs) {
                selectedPlaylistForDetail?.let { pl ->
                    when (pl.id) {
                        -100L -> kotlinx.coroutines.flow.flowOf(allSongs.sortedByDescending { it.dateAdded }.take(30))
                        -101L -> kotlinx.coroutines.flow.flowOf(allSongs.sortedByDescending { it.playCount }.take(30))
                        -102L -> kotlinx.coroutines.flow.flowOf(if (likedSongs.isNotEmpty()) likedSongs else allSongs.filter { it.isLiked }.ifEmpty { allSongs.take(20) })
                        else -> mainViewModel.getSongsForPlaylist(pl.id)
                    }
                }
            }
            val playlistSongs by (playlistSongsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<Song>()) })

            LaunchedEffect(currentSong?.id, isPlaying, themeConfig.gaplessEnabled, isBound) {
                val song = currentSong
                if (song != null && isBound) {
                    if (isPlaying) {
                        playbackService?.playTrackWithGapless(
                            song = song,
                            queue = mainViewModel.playbackQueue.value,
                            isGaplessEnabled = themeConfig.gaplessEnabled
                        )
                    } else {
                        playbackService?.pause()
                    }
                }
            }

            LaunchedEffect(isPlaying, isBound) {
                if (isPlaying && isBound) {
                    while (isPlaying) {
                        val pos = playbackService?.getCurrentPosition() ?: 0L
                        if (pos >= 0) {
                            mainViewModel.updatePositionFromService(pos)
                        }
                        kotlinx.coroutines.delay(500L)
                    }
                }
            }

            val accentColor = getThemeAccentColor(themeConfig)

            TrixxWaveTheme(themeConfig = themeConfig) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Animated Liquid Glass Ambient Background
                    AmbientGlassBackground(
                        modifier = Modifier.fillMaxSize(),
                        themeConfig = themeConfig
                    )

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    var lastBackPressTime by remember { mutableLongStateOf(0L) }

                    BackHandler(enabled = true) {
                        when {
                            showNowPlayingModal -> {
                                showNowPlayingModal = false
                            }
                            selectedPlaylistForDetail != null -> {
                                selectedPlaylistForDetail = null
                            }
                            currentRoute != "home" && currentRoute != "onboarding" && currentRoute != null -> {
                                navController.popBackStack()
                            }
                            currentRoute == "home" -> {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastBackPressTime < 2000L) {
                                    (context as? Activity)?.finish()
                                } else {
                                    lastBackPressTime = currentTime
                                    Toast.makeText(context, "Press back again to exit FloWave", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            if (currentRoute != "onboarding") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(top = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TopGlassmorphicClock(themeConfig = themeConfig)
                                }
                            }
                        },
                        bottomBar = {
                            if (currentRoute != "onboarding") {
                                Column(modifier = Modifier.navigationBarsPadding()) {
                                    // Floating Liquid Glass MiniPlayer
                                    if (currentSong != null && !showNowPlayingModal) {
                                        MiniPlayer(
                                            song = currentSong,
                                            isPlaying = isPlaying,
                                            currentPositionMs = currentPositionMs,
                                            onPlayPauseToggle = { mainViewModel.togglePlayPause() },
                                            onSkipNext = { mainViewModel.skipNext() },
                                            onSkipPrevious = { mainViewModel.skipPrevious() },
                                            onSeek = { pos ->
                                                mainViewModel.seekToPosition(pos)
                                                playbackService?.seekTo(pos)
                                            },
                                            onToggleLike = { song -> mainViewModel.toggleLikeSong(song) },
                                            onExpandNowPlaying = { showNowPlayingModal = true },
                                            themeConfig = themeConfig
                                        )
                                    }

                                    // Liquid Glass Floating Dock Navigation Bar
                                    LiquidGlassNavigationBar(
                                        currentRoute = currentRoute,
                                        themeConfig = themeConfig,
                                        onNavigate = { targetRoute ->
                                            if (currentRoute != targetRoute) {
                                                navController.navigate(targetRoute) {
                                                    popUpTo("home") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (isFirstRun) "onboarding" else "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    themeConfig = themeConfig,
                                    onCompleteOnboarding = { name, avatarUri, selectedTheme ->
                                        settingsViewModel.completeOnboarding(name, avatarUri, selectedTheme)
                                        navController.navigate("home") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                HomeScreen(
                                    themeConfig = themeConfig,
                                    activeProfile = activeProfile,
                                    recentlyPlayed = recentlyPlayed,
                                    likedSongs = likedSongs,
                                    allSongs = allSongs,
                                    playlists = playlists,
                                    downloadStatusMap = downloadStatusMap,
                                    isOnlineMode = isOnlineMode,
                                    activeOnlineTab = activeOnlineTab,
                                    youtubeResults = youtubeResults,
                                    audiusTracks = audiusTracks,
                                    radioStations = radioStations,
                                    isExtractingStream = isExtractingStream,
                                    isOnlineSearchLoading = isOnlineSearchLoading,
                                    onlineStreamError = onlineStreamError,
                                    onToggleOnlineMode = { online -> mainViewModel.toggleOnlineMode(online) },
                                    onSelectOnlineTab = { tab -> mainViewModel.setOnlineTab(tab) },
                                    onExtractYoutubeUrl = { url -> mainViewModel.extractAndPlayYoutubeUrl(url) },
                                    onSearchYoutube = { q -> mainViewModel.searchYoutube(q) },
                                    onSearchAudius = { q -> mainViewModel.searchAudius(q) },
                                    onSearchRadio = { q -> mainViewModel.fetchRadioStations(q) },
                                    onPlayOnlineTrack = { song -> mainViewModel.playOnlineTrack(song) },
                                    onToggleLike = { song -> mainViewModel.toggleLikeSong(song) },
                                    onAddToPlaylist = { plId, song -> mainViewModel.addSongToPlaylist(plId, song) },
                                    onDownloadSong = { song -> mainViewModel.downloadOnlineSong(song) },
                                    onCreatePlaylist = { name, desc -> mainViewModel.createCustomPlaylist(name, desc) },
                                    onSongClick = { song -> mainViewModel.playSong(song) },
                                    onPlaylistClick = { pl -> selectedPlaylistForDetail = pl },
                                    onGenerateSmartMix = { prompt -> mainViewModel.generateSmartMix(prompt) },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToProfiles = { navController.navigate("profiles") },
                                    onNavigateToEqualizer = { navController.navigate("equalizer") }
                                )
                            }
                            composable("search") {
                                SearchScreen(
                                    allSongs = allSongs,
                                    themeConfig = themeConfig,
                                    recentSearches = recentSearches,
                                    onSaveSearchQuery = { query -> mainViewModel.saveSearchQuery(query) },
                                    onDeleteRecentSearch = { query -> mainViewModel.deleteRecentSearch(query) },
                                    onClearRecentSearches = { mainViewModel.clearRecentSearches() },
                                    onSongClick = { song -> mainViewModel.playSong(song) }
                                )
                            }
                            composable("library") {
                                LibraryScreen(
                                    playlists = playlists,
                                    likedSongs = likedSongs,
                                    allSongs = allSongs,
                                    themeConfig = themeConfig,
                                    onPlaylistClick = { pl -> selectedPlaylistForDetail = pl },
                                    onSongClick = { song -> mainViewModel.playSong(song) },
                                    onRescanLibrary = { settingsViewModel.rescanLibrary() },
                                    onCreatePlaylist = { name, desc -> mainViewModel.createCustomPlaylist(name, desc) }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    themeConfig = themeConfig,
                                    aiConfig = aiConfig,
                                    testStatus = testStatus,
                                    fetchedModels = fetchedModels,
                                    isFetchingModels = isFetchingModels,
                                    fetchModelsStatus = fetchModelsStatus,
                                    onFetchModels = { provider, key, endpoint -> settingsViewModel.fetchAvailableModels(provider, key, endpoint) },
                                    onSaveAiConfig = { cfg -> settingsViewModel.saveAiConfig(cfg) },
                                    onTestAiConnection = { settingsViewModel.testAiConnection() },
                                    onSelectPreset = { preset -> settingsViewModel.updateThemePreset(preset) },
                                    onSetThemeMode = { mode -> settingsViewModel.setThemeMode(mode) },
                                    onSetBlurIntensity = { intensity -> settingsViewModel.setBlurIntensity(intensity) },
                                    onSetPrimaryColor = { hex -> settingsViewModel.setPrimaryColor(hex) },
                                    onSetAccentColor = { hex -> settingsViewModel.setAccentColor(hex) },
                                    onSetCustomBgUri = { uri -> settingsViewModel.setCustomBgUri(uri) },
                                    onToggleContrastSafeMode = { enabled -> settingsViewModel.setContrastSafeMode(enabled) },
                                    onSetWidgetStyle = { style -> settingsViewModel.setWidgetStyle(style) },
                                    onSetWidgetOpacity = { opacity -> settingsViewModel.setWidgetOpacity(opacity) },
                                    onSetWidgetShowSkip = { show -> settingsViewModel.setWidgetShowSkip(show) },
                                    onSetWidgetShowAlbumArt = { show -> settingsViewModel.setWidgetShowAlbumArt(show) },
                                    onSetWidgetShowWaveform = { show -> settingsViewModel.setWidgetShowWaveform(show) },
                                    onSetWidgetShowFavorite = { show -> settingsViewModel.setWidgetShowFavorite(show) },
                                    onSetVisualizerStyle = { style -> settingsViewModel.setVisualizerStyle(style) },
                                    onToggleDynamicIsland = { enabled -> settingsViewModel.setDynamicIslandEnabled(enabled) },
                                    onToggleStaticBlurMode = { enabled -> settingsViewModel.setStaticBlurMode(enabled) },
                                    onToggleAutoResume = { enabled -> settingsViewModel.setAutoResumeEnabled(enabled) },
                                    onSetTrackTransitionAnimation = { anim -> settingsViewModel.setTrackTransitionAnimation(anim) },
                                    gaplessState = gaplessState,
                                    onToggleGapless = { enabled -> settingsViewModel.setGaplessEnabled(enabled) },
                                    onSetSilenceThresholdDb = { threshold -> settingsViewModel.setGaplessSilenceThresholdDb(threshold) },
                                    onToggleAutoScanGapless = { auto -> settingsViewModel.setAutoScanGaplessOnImport(auto) },
                                    onRunGaplessScan = { mainViewModel.runGaplessAnalysisScan(themeConfig.gaplessSilenceThresholdDb) },
                                    onResetGaplessTrims = { mainViewModel.resetGaplessTrims() },
                                    onRescanLibrary = { settingsViewModel.rescanLibrary() },
                                    onReRunOnboarding = { navController.navigate("onboarding") },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("profiles") {
                                ProfileSwitcherScreen(
                                    profiles = profiles,
                                    themeConfig = themeConfig,
                                    onSelectProfile = { p -> settingsViewModel.switchProfile(p.id) },
                                    onCreateProfile = { name, avatar -> settingsViewModel.createProfile(name, avatar) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("equalizer") {
                                PlaybackServiceEqualizerScreen(
                                    playbackService = playbackService,
                                    themeConfig = themeConfig,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // Floating Dynamic Island Punch-Hole Notch Player
                    if (currentSong != null && currentRoute != "onboarding" && !showNowPlayingModal) {
                        DynamicIslandPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            visualizerHelper = playbackService?.visualizerHelper,
                            themeConfig = themeConfig,
                            onPlayPauseToggle = { mainViewModel.togglePlayPause() },
                            onSkipNext = { mainViewModel.skipNext() },
                            onSkipPrevious = { mainViewModel.skipPrevious() },
                            onExpandNowPlaying = { showNowPlayingModal = true },
                            onToggleLike = { song -> mainViewModel.toggleLikeSong(song) }
                        )
                    }

                    // Overlay Permission Dialog for System Dynamic Island
                    if (showOverlayPermissionDialog) {
                        OverlayPermissionDialog(
                            themeConfig = themeConfig,
                            onDismiss = { showOverlayPermissionDialog = false },
                            onPermissionGranted = {
                                DynamicIslandOverlayService.startService(context)
                            }
                        )
                    }

                    // Fullscreen Now Playing Overlay
                    if (showNowPlayingModal) {
                        NowPlayingScreen(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            visualizerHelper = playbackService?.visualizerHelper,
                            lyrics = lyrics,
                            aiInsight = aiInsight,
                            themeConfig = themeConfig,
                            onPlayPauseToggle = { mainViewModel.togglePlayPause() },
                            onSkipNext = { mainViewModel.skipNext() },
                            onSkipPrevious = { mainViewModel.skipPrevious() },
                            onSeek = { pos ->
                                mainViewModel.seekToPosition(pos)
                                playbackService?.seekTo(pos)
                            },
                            onToggleLike = { song -> mainViewModel.toggleLikeSong(song) },
                            onDismiss = { showNowPlayingModal = false },
                            onReTag = { song -> mainViewModel.reTagSongWithAi(song) },
                            onSaveLyrics = { songId, plainLyrics, syncedLrc -> mainViewModel.saveCorrectedLyrics(songId, plainLyrics, syncedLrc) }
                        )
                    }

                    // Playlist Detail Glass Modal Dialog
                    if (selectedPlaylistForDetail != null) {
                        val pl = selectedPlaylistForDetail!!
                        PlaylistDetailDialog(
                            playlist = pl,
                            songs = playlistSongs,
                            themeConfig = themeConfig,
                            onPlayAll = { songs -> mainViewModel.playPlaylist(songs, 0) },
                            onPlayShuffle = { songs -> mainViewModel.playPlaylist(songs.shuffled(), 0) },
                            onSongClick = { song, queue ->
                                val idx = queue.indexOf(song).coerceAtLeast(0)
                                mainViewModel.playPlaylist(queue, idx)
                            },
                            onRemoveSong = { songId -> mainViewModel.removeSongFromPlaylist(pl.id, songId) },
                            onDeletePlaylist = { mainViewModel.deletePlaylist(pl) },
                            onDismiss = { selectedPlaylistForDetail = null }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            if (isBound) {
                unbindService(connection)
                isBound = false
            }
            DynamicIslandOverlayService.stopService(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}

@Composable
fun PlaybackServiceEqualizerScreen(
    playbackService: PlaybackService?,
    themeConfig: ThemeConfig,
    onBack: () -> Unit
) {
    if (playbackService != null) {
        EqualizerScreen(
            equalizerManager = playbackService.equalizerManager,
            themeConfig = themeConfig,
            onBack = onBack
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Equalizer binding to playback service...", color = Color.White)
        }
    }
}
