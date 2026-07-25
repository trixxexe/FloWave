package com.trixxexe.trixxwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.service.PlaybackService
import com.trixxexe.trixxwave.ui.components.glass.AmbientGlassBackground
import com.trixxexe.trixxwave.ui.components.glass.DynamicIslandPlayer
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassNavigationBar
import com.trixxexe.trixxwave.ui.components.glass.MiniPlayer
import com.trixxexe.trixxwave.ui.components.glass.TopGlassmorphicClock
import com.trixxexe.trixxwave.ui.screens.*
import com.trixxexe.trixxwave.ui.theme.TrixxWaveTheme
import com.trixxexe.trixxwave.ui.viewmodel.MainViewModel
import com.trixxexe.trixxwave.ui.viewmodel.SettingsViewModel

import androidx.navigation.compose.currentBackStackEntryAsState
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor

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

        // Bind PlaybackService
        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

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
            val profiles by settingsViewModel.profiles.collectAsState()
            val isFirstRun by settingsViewModel.isFirstRun.collectAsState()

            var showNowPlayingModal by remember { mutableStateOf(false) }

            val visualizerBands = playbackService?.visualizerHelper?.fftBands?.collectAsState()?.value
                ?: FloatArray(20) { 0.1f }

            val visualizerWaveform = playbackService?.visualizerHelper?.waveformPoints?.collectAsState()?.value
                ?: FloatArray(32) { 0f }

            val gaplessState by mainViewModel.gaplessState.collectAsState()
            val recentSearches by mainViewModel.recentSearches.collectAsState()

            LaunchedEffect(currentSong, isPlaying, themeConfig.gaplessEnabled, isBound) {
                val song = currentSong
                if (song != null) {
                    if (isPlaying) {
                        playbackService?.playTrackWithGapless(
                            song = song,
                            queue = mainViewModel.playbackQueue.value,
                            isGaplessEnabled = themeConfig.gaplessEnabled
                        )
                    } else if (isBound) {
                        playbackService?.prepareTrackForResume(
                            song = song,
                            queue = mainViewModel.playbackQueue.value,
                            positionMs = currentPositionMs,
                            isGaplessEnabled = themeConfig.gaplessEnabled
                        )
                    }
                }
            }

            LaunchedEffect(isPlaying, isBound) {
                if (isPlaying && isBound) {
                    while (isPlaying) {
                        val pos = playbackService?.getCurrentPosition() ?: 0L
                        if (pos > 0) {
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
                                Column {
                                    // Floating Liquid Glass MiniPlayer
                                    if (currentSong != null && !showNowPlayingModal) {
                                        MiniPlayer(
                                            song = currentSong,
                                            isPlaying = isPlaying,
                                            onPlayPauseToggle = { mainViewModel.togglePlayPause() },
                                            onSkipNext = { mainViewModel.skipNext() },
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
                                    onSongClick = { song -> mainViewModel.playSong(song) },
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
                                    onPlaylistClick = { },
                                    onSongClick = { song -> mainViewModel.playSong(song) },
                                    onRescanLibrary = { settingsViewModel.rescanLibrary() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    themeConfig = themeConfig,
                                    aiConfig = aiConfig,
                                    testStatus = testStatus,
                                    onSaveAiConfig = { cfg -> settingsViewModel.saveAiConfig(cfg) },
                                    onTestAiConnection = { settingsViewModel.testAiConnection() },
                                    onSelectPreset = { preset -> settingsViewModel.updateThemePreset(preset) },
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
                            bands = visualizerBands,
                            waveform = visualizerWaveform,
                            themeConfig = themeConfig,
                            onPlayPauseToggle = { mainViewModel.togglePlayPause() },
                            onSkipNext = { mainViewModel.skipNext() },
                            onSkipPrevious = { mainViewModel.skipPrevious() },
                            onExpandNowPlaying = { showNowPlayingModal = true },
                            onToggleLike = { song -> mainViewModel.toggleLikeSong(song) }
                        )
                    }

                    // Fullscreen Now Playing Overlay
                    if (showNowPlayingModal) {
                        NowPlayingScreen(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            visualizerBands = visualizerBands,
                            visualizerWaveform = visualizerWaveform,
                            lyrics = lyrics,
                            aiInsight = aiInsight,
                            themeConfig = themeConfig,
                            onPlayPauseToggle = { mainViewModel.togglePlayPause() },
                            onSkipNext = { mainViewModel.skipNext() },
                            onSkipPrevious = { mainViewModel.skipPrevious() },
                            onSeek = { pos -> mainViewModel.seekToPosition(pos) },
                            onToggleLike = { song -> mainViewModel.toggleLikeSong(song) },
                            onDismiss = { showNowPlayingModal = false },
                            onReTag = { song -> mainViewModel.reTagSongWithAi(song) },
                            onSaveLyrics = { songId, plainLyrics, syncedLrc -> mainViewModel.saveCorrectedLyrics(songId, plainLyrics, syncedLrc) }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
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
