package com.trixxexe.trixxwave.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.trixxexe.trixxwave.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.preferences.AiConfig
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.CustomVisualizerView
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor
import com.trixxexe.trixxwave.ui.viewmodel.GaplessAnalysisState

data class AiProviderOption(
    val id: String,
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val popularModels: List<String>
)

val WELL_KNOWN_AI_PROVIDERS = listOf(
    AiProviderOption(
        id = "Groq",
        displayName = "Groq (Ultra-Fast LPUs)",
        defaultEndpoint = "https://api.groq.com/openai/v1/",
        defaultModel = "llama-3.3-70b-versatile",
        popularModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "deepseek-r1-distill-llama-70b", "mixtral-8x7b-32768", "gemma2-9b-it")
    ),
    AiProviderOption(
        id = "Google Gemini",
        displayName = "Google Gemini API",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/openai/",
        defaultModel = "gemini-2.5-flash",
        popularModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro")
    ),
    AiProviderOption(
        id = "OpenAI",
        displayName = "OpenAI (GPT-4o)",
        defaultEndpoint = "https://api.openai.com/v1/",
        defaultModel = "gpt-4o-mini",
        popularModels = listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "gpt-4-turbo", "gpt-3.5-turbo")
    ),
    AiProviderOption(
        id = "OpenRouter",
        displayName = "OpenRouter (Unified Gateway)",
        defaultEndpoint = "https://openrouter.ai/api/v1/",
        defaultModel = "google/gemini-2.5-flash",
        popularModels = listOf("google/gemini-2.5-flash", "meta-llama/llama-3.3-70b-instruct", "anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat", "qwen/qwen-2.5-72b-instruct")
    ),
    AiProviderOption(
        id = "NVIDIA NIM",
        displayName = "NVIDIA NIM Cloud",
        defaultEndpoint = "https://integrate.api.nvidia.com/v1/",
        defaultModel = "meta/llama-3.3-70b-instruct",
        popularModels = listOf("meta/llama-3.3-70b-instruct", "nvidia/llama-3.1-nemotron-70b-instruct", "deepseek-ai/deepseek-r1", "mistralai/mistral-large-2-instruct")
    ),
    AiProviderOption(
        id = "Anthropic Claude",
        displayName = "Anthropic Claude (via OpenRouter)",
        defaultEndpoint = "https://openrouter.ai/api/v1/",
        defaultModel = "anthropic/claude-3.5-sonnet",
        popularModels = listOf("anthropic/claude-3.5-sonnet", "anthropic/claude-3.5-haiku", "anthropic/claude-3-opus")
    ),
    AiProviderOption(
        id = "Together AI",
        displayName = "Together AI Engine",
        defaultEndpoint = "https://api.together.xyz/v1/",
        defaultModel = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        popularModels = listOf("meta-llama/Llama-3.3-70B-Instruct-Turbo", "mistralai/Mixtral-8x7B-Instruct-v0.1", "Qwen/Qwen2.5-72B-Instruct-Turbo")
    ),
    AiProviderOption(
        id = "DeepSeek",
        displayName = "DeepSeek AI Direct",
        defaultEndpoint = "https://api.deepseek.com/v1/",
        defaultModel = "deepseek-chat",
        popularModels = listOf("deepseek-chat", "deepseek-reasoner")
    ),
    AiProviderOption(
        id = "Mistral AI",
        displayName = "Mistral AI Platform",
        defaultEndpoint = "https://api.mistral.ai/v1/",
        defaultModel = "mistral-small-latest",
        popularModels = listOf("mistral-small-latest", "mistral-large-latest", "pixtral-12b-2409", "open-mistral-nemo")
    ),
    AiProviderOption(
        id = "Fireworks AI",
        displayName = "Fireworks AI Platform",
        defaultEndpoint = "https://api.fireworks.ai/inference/v1/",
        defaultModel = "accounts/fireworks/models/llama-v3p3-70b-instruct",
        popularModels = listOf("accounts/fireworks/models/llama-v3p3-70b-instruct", "accounts/fireworks/models/deepseek-r1", "accounts/fireworks/models/qwen2p5-coder-32b-instruct")
    ),
    AiProviderOption(
        id = "Cohere",
        displayName = "Cohere Command Models",
        defaultEndpoint = "https://api.cohere.com/v2/",
        defaultModel = "command-r-plus",
        popularModels = listOf("command-r-plus", "command-r")
    ),
    AiProviderOption(
        id = "Custom Endpoint",
        displayName = "Custom Endpoint (Ollama / LocalLM)",
        defaultEndpoint = "http://localhost:11434/v1/",
        defaultModel = "llama3.2",
        popularModels = listOf("llama3.2", "qwen2.5", "mistral", "deepseek-r1")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeConfig: ThemeConfig,
    aiConfig: AiConfig,
    testStatus: String?,
    fetchedModels: List<String> = emptyList(),
    isFetchingModels: Boolean = false,
    fetchModelsStatus: String? = null,
    onFetchModels: ((provider: String, apiKey: String, customEndpoint: String) -> Unit)? = null,
    onSaveAiConfig: (AiConfig) -> Unit,
    onTestAiConnection: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSetPrimaryColor: ((String) -> Unit)? = null,
    onSetAccentColor: ((String) -> Unit)? = null,
    onSetCustomBgUri: ((String?) -> Unit)? = null,
    onToggleContrastSafeMode: (Boolean) -> Unit,
    onSetVisualizerStyle: ((String) -> Unit)? = null,
    onToggleDynamicIsland: ((Boolean) -> Unit)? = null,
    onToggleStaticBlurMode: ((Boolean) -> Unit)? = null,
    onToggleAutoResume: ((Boolean) -> Unit)? = null,
    onSetTrackTransitionAnimation: ((String) -> Unit)? = null,
    onSetWidgetStyle: ((String) -> Unit)? = null,
    onSetWidgetOpacity: ((Float) -> Unit)? = null,
    onSetWidgetShowSkip: ((Boolean) -> Unit)? = null,
    onSetWidgetShowAlbumArt: ((Boolean) -> Unit)? = null,
    onSetWidgetShowWaveform: ((Boolean) -> Unit)? = null,
    onSetWidgetShowFavorite: ((Boolean) -> Unit)? = null,
    gaplessState: GaplessAnalysisState = GaplessAnalysisState(),
    onToggleGapless: ((Boolean) -> Unit)? = null,
    onSetSilenceThresholdDb: ((Float) -> Unit)? = null,
    onToggleAutoScanGapless: ((Boolean) -> Unit)? = null,
    onRunGaplessScan: (() -> Unit)? = null,
    onResetGaplessTrims: (() -> Unit)? = null,
    onRescanLibrary: () -> Unit,
    onReRunOnboarding: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember(aiConfig) { mutableStateOf(aiConfig.apiKey) }
    var provider by remember(aiConfig) { mutableStateOf(aiConfig.provider) }
    var modelName by remember(aiConfig) { mutableStateOf(aiConfig.modelName) }
    var customEndpoint by remember(aiConfig) { mutableStateOf(aiConfig.customEndpoint) }

    var providerMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showAdvancedEndpoint by remember { mutableStateOf(false) }

    val activeProviderInfo = remember(provider) {
        WELL_KNOWN_AI_PROVIDERS.find { it.id.equals(provider, ignoreCase = true) || it.displayName.equals(provider, ignoreCase = true) }
            ?: WELL_KNOWN_AI_PROVIDERS.first()
    }

    val currentAvailableModels = remember(activeProviderInfo, fetchedModels) {
        if (fetchedModels.isNotEmpty()) {
            fetchedModels
        } else {
            activeProviderInfo.popularModels
        }
    }

    var customPrimaryHexInput by remember(themeConfig.primaryColorHex) { mutableStateOf(themeConfig.primaryColorHex) }
    var customAccentHexInput by remember(themeConfig.accentColorHex) { mutableStateOf(themeConfig.accentColorHex) }

    val presets = listOf("Sleek Interface", "AMOLED Dark", "AMOLED Light", "Liquid Obsidian", "Cyber Pink", "Emerald Wave", "Sunset Gold", "Aether White")

    val rainbowSwatches = listOf(
        "#EF4444", "#F59E0B", "#10B981", "#00F5D4", "#2563EB", "#8B5CF6", "#FF007A", "#F43F5E", "#000000", "#FFFFFF", "#1E293B", "#1E1B4B"
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSetCustomBgUri?.invoke(it.toString()) }
    }

    val accentColor = getThemeAccentColor(themeConfig)

    LazyColumn(
        modifier = Modifier
            .testTag("settings_screen")
            .fillMaxSize()
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FloWave Engine Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Theme Customization Section
        item {
            Text("THEME ENGINE & GLASS MATERIAL", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Theme", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Presets & AMOLED Dark/Light Themes", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            FilterChip(
                                selected = themeConfig.preset == preset,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Contrast Safe Mode", color = Color.White, fontSize = 14.sp)
                            Text("Enhances accessibility text contrast", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = themeConfig.contrastSafeMode,
                            onCheckedChange = onToggleContrastSafeMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Accessibility & Battery Optimization Section
        item {
            Text("ACCESSIBILITY & BATTERY OPTIMIZATION", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("accessibility_card"),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = "Battery Saver", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Render Engine Mode", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Switch between dynamic liquid refraction and low-power static blur", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (themeConfig.staticBlurMode) "Static Blur Mode" else "Liquid Glass Mode",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (themeConfig.staticBlurMode)
                                    "Battery-optimized mode: Uses a static blur canvas to conserve GPU & battery power."
                                else
                                    "High-intensity mode: Displays animated fluid gradient glass refraction effects.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = themeConfig.staticBlurMode,
                            onCheckedChange = { onToggleStaticBlurMode?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor),
                            modifier = Modifier.testTag("static_blur_toggle_switch")
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Auto-Resume & Playback State Configuration Section
        item {
            Text("AUTOMATED PLAYBACK & AUTO-RESUME CONFIGURATION", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("auto_resume_card"),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = "Auto Resume", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Auto-Resume Playback on Startup", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Automatically detects and restores the last played track & seek position", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (themeConfig.autoResumeEnabled) "Auto-Resume Enabled" else "Auto-Resume Disabled",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (themeConfig.autoResumeEnabled)
                                    "When app opens, MediaSession & playback queue automatically re-initialize to the exact track and timestamp where you left off."
                                else
                                    "App starts with an empty player state without re-loading the previous track.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = themeConfig.autoResumeEnabled,
                            onCheckedChange = { onToggleAutoResume?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor),
                            modifier = Modifier.testTag("auto_resume_toggle_switch")
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Home Screen Widget Customizer Section
        item {
            Text("HOME-SCREEN WIDGET CUSTOMIZER & LIQUID GLASS PRESETS", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            val widgetStyles = listOf("Compact Bar", "Standard Card", "Full Glass Suite", "Audio Waveform & Stats", "Quick Playlist Launcher")

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("widget_customizer_card"),
                cornerRadius = 20.dp
            ) {
                Column {
                    Text("Selected Widget Preset / Type:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(widgetStyles) { style ->
                            FilterChip(
                                selected = themeConfig.widgetStyle == style,
                                onClick = { onSetWidgetStyle?.invoke(style) },
                                label = { Text(style) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Interactive Widget Mockup Preview
                    Text("Live Widget Glass Preview:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val baseColorInt = try { android.graphics.Color.parseColor(themeConfig.primaryColorHex) } catch (e: Exception) { android.graphics.Color.parseColor("#050505") }
                    val previewBgColor = Color(baseColorInt).copy(alpha = themeConfig.widgetOpacity.coerceIn(0.1f, 1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(previewBgColor)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        when (themeConfig.widgetStyle) {
                            "Compact Bar" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (themeConfig.widgetShowAlbumArt) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accentColor.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎵", fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Neon Horizon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Liquid Glass Compact", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("▶", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "Full Glass Suite" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (themeConfig.widgetShowAlbumArt) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentColor.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🎶", fontSize = 24.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Aether Pulse", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Glass Suite Master", color = Color.LightGray, fontSize = 12.sp)
                                            Text("FLAC • 24-bit/96kHz", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (themeConfig.widgetShowFavorite) {
                                            Text("♥", color = accentColor, fontSize = 18.sp)
                                        }
                                    }
                                    if (themeConfig.widgetShowWaveform) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(0.6f)
                                                    .background(accentColor)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (themeConfig.widgetShowSkip) {
                                            Text("⏮", color = Color.White, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(20.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("▶", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (themeConfig.widgetShowSkip) {
                                            Spacer(modifier = Modifier.width(20.dp))
                                            Text("⏭", color = Color.White, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                            "Audio Waveform & Stats" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Cyber Waveform", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Realtime Audio Visualizer", color = Color.LightGray, fontSize = 11.sp)
                                        }
                                        Text("48 kHz • 320 kbps", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val heights = listOf(12, 22, 16, 28, 10, 20, 24, 14, 26, 18)
                                        heights.forEachIndexed { idx, h ->
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(h.dp)
                                                    .clip(CircleShape)
                                                    .background(if (idx % 2 == 0) accentColor else Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                            "Quick Playlist Launcher" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Quick Launch Bar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Tap shortcut to start flow", color = Color.LightGray, fontSize = 11.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("▶", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) { Text("♥ Liked", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) { Text("⚡ Recent", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.15f)).padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) { Text("✨ Smart", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                            else -> { // Standard Card
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (themeConfig.widgetShowAlbumArt) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(accentColor.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) { Text("🎧", fontSize = 20.sp) }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Liquid Track", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Standard Glass Card", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (themeConfig.widgetShowSkip) {
                                            Text("⏮", color = Color.White, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(accentColor),
                                            contentAlignment = Alignment.Center
                                        ) { Text("▶", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        if (themeConfig.widgetShowSkip) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("⏭", color = Color.White, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opacity Slider
                    Text("Widget Glass Opacity: ${(themeConfig.widgetOpacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = themeConfig.widgetOpacity,
                        onValueChange = { onSetWidgetOpacity?.invoke(it) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Skip Next/Prev Buttons", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = themeConfig.widgetShowSkip,
                            onCheckedChange = { onSetWidgetShowSkip?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Album Art Thumbnail", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = themeConfig.widgetShowAlbumArt,
                            onCheckedChange = { onSetWidgetShowAlbumArt?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Waveform / Progress Bar", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = themeConfig.widgetShowWaveform,
                            onCheckedChange = { onSetWidgetShowWaveform?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Favorite Heart Button", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = themeConfig.widgetShowFavorite,
                            onCheckedChange = { onSetWidgetShowFavorite?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Audio Session Visualizer & Dynamic Island Section
        item {
            Text("AUDIO ENGINE VISUALIZER & DYNAMIC ISLAND NOTCH PLAYER", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            val vizModes = listOf("Spectrum", "Waveform", "Liquid Wave", "Circular", "None")

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("visualizer_settings_card"),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Visualizer", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Realtime Canvas Audio Visualizer Engine", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Selected Visualizer Style:", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vizModes) { mode ->
                            FilterChip(
                                selected = themeConfig.visualizerStyle == mode,
                                onClick = { onSetVisualizerStyle?.invoke(mode) },
                                label = { Text(mode) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Interactive Canvas Preview
                    Text("Live Visualizer Engine Preview:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomVisualizerView(
                            bands = floatArrayOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.3f, 0.95f, 0.5f, 0.7f, 0.4f, 0.85f, 0.6f),
                            waveform = floatArrayOf(0f, 0.4f, 0.8f, 0.3f, -0.5f, -0.9f, -0.2f, 0.6f, 0.9f, 0.1f, -0.7f, -0.4f, 0.2f, 0.5f),
                            style = themeConfig.visualizerStyle,
                            accentColor = accentColor,
                            height = 44.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Island Punch-Hole Floating Notch Player Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mobile Dynamic Island Notch Player", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Floats over punch-hole display notch with animated wave & media controls", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = themeConfig.dynamicIslandEnabled,
                            onCheckedChange = { onToggleDynamicIsland?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Track Transition Animations Section
        item {
            Text("PLAYBACK & TRACK TRANSITION ANIMATIONS", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            val transitions = listOf("Crossfade", "Fade to Black", "Slide In", "Zoom & Pop", "Instant Jump")
            var previewState by remember { mutableStateOf(false) }
            var isTransitionMenuExpanded by remember { mutableStateOf(false) }

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("track_transition_card"),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = "Transition", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Track Transition Animation", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Replaces standard jump when switching tracks", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Select Animation Style (Menu):", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Transition Selector Dropdown Menu
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isTransitionMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("track_transition_menu_button"),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transition: ${themeConfig.trackTransitionAnimation}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Transition Menu",
                                    tint = accentColor
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isTransitionMenuExpanded,
                            onDismissRequest = { isTransitionMenuExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF1E1E28))
                                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .testTag("track_transition_dropdown_menu")
                        ) {
                            transitions.forEach { trans ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = trans,
                                            color = if (themeConfig.trackTransitionAnimation == trans) accentColor else Color.White,
                                            fontWeight = if (themeConfig.trackTransitionAnimation == trans) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSetTrackTransitionAnimation?.invoke(trans)
                                        isTransitionMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(transitions) { trans ->
                            FilterChip(
                                selected = themeConfig.trackTransitionAnimation == trans,
                                onClick = { onSetTrackTransitionAnimation?.invoke(trans) },
                                label = { Text(trans) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Transition Animation Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Animation Preview:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { previewState = !previewState },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Trigger Transition", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = previewState,
                            transitionSpec = {
                                when (themeConfig.trackTransitionAnimation) {
                                    "Fade to Black" -> {
                                        androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(600, delayMillis = 300))
                                            .togetherWith(androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)))
                                    }
                                    "Slide In" -> {
                                        (androidx.compose.animation.slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = androidx.compose.animation.core.tween(600)) + androidx.compose.animation.fadeIn())
                                            .togetherWith(androidx.compose.animation.slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = androidx.compose.animation.core.tween(600)) + androidx.compose.animation.fadeOut())
                                    }
                                    "Zoom & Pop" -> {
                                        (androidx.compose.animation.scaleIn(initialScale = 0.4f, animationSpec = androidx.compose.animation.core.tween(600)) + androidx.compose.animation.fadeIn())
                                            .togetherWith(androidx.compose.animation.scaleOut(targetScale = 1.6f, animationSpec = androidx.compose.animation.core.tween(600)) + androidx.compose.animation.fadeOut())
                                    }
                                    "Instant Jump" -> {
                                        androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(0)).togetherWith(androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(0)))
                                    }
                                    else -> { // Crossfade
                                        (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(600)) +
                                         androidx.compose.animation.scaleIn(initialScale = 0.88f, animationSpec = androidx.compose.animation.core.tween(600)))
                                            .togetherWith(
                                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(600)) +
                                                androidx.compose.animation.scaleOut(targetScale = 1.12f, animationSpec = androidx.compose.animation.core.tween(600))
                                            )
                                    }
                                }
                            },
                            label = "settings_transition_preview"
                        ) { state ->
                            Surface(
                                color = if (state) accentColor.copy(alpha = 0.3f) else Color(0xFF3B82F6).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (state) accentColor else Color(0xFF3B82F6))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (state) "Track A: Neon Waves" else "Track B: Ocean Sunset",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Automated Gapless Playback Analysis Tool Section
        item {
            Text("AUTOMATED GAPLESS PLAYBACK ANALYSIS ENGINE", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("gapless_analysis_card"),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "Gapless", tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Automated Gapless Analysis Tool", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Scans library for silent padding & trims playback boundaries", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = themeConfig.gaplessEnabled,
                            onCheckedChange = { onToggleGapless?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor),
                            modifier = Modifier.testTag("gapless_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Scan Progress & Statistics Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Silent Padding Trim Status", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    color = if (gaplessState.isAnalyzing) accentColor.copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (gaplessState.isAnalyzing) "Scanning Audio..." else "Engine Active",
                                        color = if (gaplessState.isAnalyzing) accentColor else Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = gaplessState.statusMessage,
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )

                            if (gaplessState.isAnalyzing && gaplessState.totalCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = gaplessState.analyzedCount.toFloat() / gaplessState.totalCount.toFloat(),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = accentColor,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }

                            if (gaplessState.totalSilenceTrimmedMs > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Trimmed ${(gaplessState.totalSilenceTrimmedMs / 1000f).let { String.format("%.2f", it) }}s of silent padding gaps",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scan Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onRunGaplessScan?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("run_gapless_scan_button"),
                            enabled = !gaplessState.isAnalyzing
                        ) {
                            Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Silence", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onResetGaplessTrims?.invoke() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("reset_gapless_trims_button")
                        ) {
                            Text("Reset Trims", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Silence Cutoff Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Silence Floor Sensitivity", color = Color.White, fontSize = 13.sp)
                            Text("${themeConfig.gaplessSilenceThresholdDb.toInt()} dB", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = themeConfig.gaplessSilenceThresholdDb,
                            onValueChange = { onSetSilenceThresholdDb?.invoke(it) },
                            valueRange = -60f..-30f,
                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Auto Scan New Tracks Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Analyze On Library Import", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Automatically calculate trim padding when new songs are added", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = themeConfig.autoScanGaplessOnImport,
                            onCheckedChange = { onToggleAutoScanGapless?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }
            }
        }
        item {
            Text("ASCENTIC CUSTOM COLOR & RAINBOW PALETTE", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ColorLens, contentDescription = "Colors", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Full Spectrum Accent & Canvas Colors", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Accent Color Palette Swatches:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(rainbowSwatches) { hex ->
                            val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (themeConfig.accentColorHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                        color = if (themeConfig.accentColorHex.equals(hex, ignoreCase = true)) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onSetAccentColor?.invoke(hex) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Primary Canvas Color Swatches:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(rainbowSwatches) { hex ->
                            val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (themeConfig.primaryColorHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                        color = if (themeConfig.primaryColorHex.equals(hex, ignoreCase = true)) accentColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onSetPrimaryColor?.invoke(hex) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customAccentHexInput,
                            onValueChange = { customAccentHexInput = it },
                            label = { Text("Accent Hex (e.g. #00F5D4)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onSetAccentColor?.invoke(customAccentHexInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                        ) {
                            Text("Apply")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPrimaryHexInput,
                            onValueChange = { customPrimaryHexInput = it },
                            label = { Text("Primary Canvas Hex (e.g. #000000)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onSetPrimaryColor?.invoke(customPrimaryHexInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Custom Background Photo Section
        item {
            Text("CUSTOM APP BACKGROUND WALLPAPER", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Wallpaper", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery Photo Wallpaper", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload any custom photo from your device gallery to use as an ambient liquid glass background.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!themeConfig.customBgUri.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = themeConfig.customBgUri,
                                contentDescription = "Custom Background Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active Wallpaper", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Custom image overlay active", color = Color(0xFF10B981), fontSize = 11.sp)
                            }
                            IconButton(onClick = { onSetCustomBgUri?.invoke(null) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Wallpaper", tint = Color(0xFFFF007A))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().testTag("select_custom_bg_button")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Pick Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (themeConfig.customBgUri.isNullOrBlank()) "Choose Gallery Image" else "Change Gallery Image", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // AI Provider Integration
        item {
            Text("AI LYRICS & PLAYLIST ENGINE", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = "AI", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LLM API & Provider Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "Select an AI Inference provider or custom endpoint to power track insights, mood tags, and smart playlist generation.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // 1. Provider Dropdown Menu
                    Text("AI Provider", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = providerMenuExpanded,
                        onExpandedChange = { providerMenuExpanded = !providerMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = provider,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Provider (10+ Top AI Services)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("ai_provider_input")
                        )

                        ExposedDropdownMenu(
                            expanded = providerMenuExpanded,
                            onDismissRequest = { providerMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E1E2C))
                        ) {
                            WELL_KNOWN_AI_PROVIDERS.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(opt.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(opt.defaultEndpoint, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        provider = opt.id
                                        customEndpoint = opt.defaultEndpoint
                                        modelName = opt.defaultModel
                                        providerMenuExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Model Selection Dropdown & Fetch Button
                    Text("Selected Model", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = modelMenuExpanded,
                            onExpandedChange = { modelMenuExpanded = !modelMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = modelName,
                                onValueChange = { modelName = it },
                                label = { Text("Model Name") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = accentColor,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("ai_model_input")
                            )

                            ExposedDropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E1E2C))
                            ) {
                                currentAvailableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model, color = Color.White) },
                                        onClick = {
                                            modelName = model
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Fetch Models Button
                        IconButton(
                            onClick = {
                                onFetchModels?.invoke(provider, apiKey, customEndpoint)
                            },
                            enabled = !isFetchingModels,
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                                .size(54.dp)
                                .testTag("fetch_models_button")
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = accentColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Fetch Available Models from API",
                                    tint = accentColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. API Key Field
                    Text("API Key", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key (EncryptedSharedPreferences)") },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key Visibility",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_key_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Advanced Custom Endpoint Toggle
                    TextButton(
                        onClick = { showAdvancedEndpoint = !showAdvancedEndpoint },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (showAdvancedEndpoint) "Hide Endpoint Settings" else "Show Custom Endpoint URL",
                            color = accentColor.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }

                    if (showAdvancedEndpoint) {
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { customEndpoint = it },
                            label = { Text("Custom Base Endpoint URL") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("ai_custom_endpoint_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Action Buttons (Save Key & Test API)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onSaveAiConfig(
                                    AiConfig(
                                        provider = provider,
                                        modelName = modelName,
                                        apiKey = apiKey,
                                        customEndpoint = customEndpoint
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).testTag("save_ai_key_button")
                        ) {
                            Text("Save Key", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onTestAiConnection,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = BorderStroke(1.dp, accentColor),
                            modifier = Modifier.weight(1f).testTag("test_ai_button")
                        ) {
                            Text("Test API", fontWeight = FontWeight.Bold)
                        }
                    }

                    // 5. Model Fetch Status Display
                    if (!fetchModelsStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isError = fetchModelsStatus.contains("Error") || fetchModelsStatus.contains("HTTP") || fetchModelsStatus.contains("Failed") || fetchModelsStatus.contains("Network")
                        Surface(
                            color = if (isError) Color(0x22FF0055) else Color(0x2200F5D4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isError) Color(0xFFFF0055) else Color(0x8800F5D4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                    contentDescription = "Status",
                                    tint = if (isError) Color(0xFFFF4477) else Color(0xFF00F5D4),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fetchModelsStatus,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 6. Test API Status Display
                    if (!testStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isSuccess = testStatus.contains("Success") || testStatus.contains("Connected")
                        Surface(
                            color = if (isSuccess) Color(0x2200F5D4) else Color(0x22FF0055),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSuccess) Color(0x8800F5D4) else Color(0xFFFF0055)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = "Test Result",
                                    tint = if (isSuccess) Color(0xFF00F5D4) else Color(0xFFFF4477),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = testStatus,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Local Library Management
        item {
            Text("LIBRARY SOURCES & SCANNER", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Folder", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MediaStore Audio Auto-Scanner", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scans device storage for MP3, FLAC, M4A, WAV audio files and auto-extracts waveform peaks & album covers.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRescanLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("rescan_storage_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rescan Local Storage Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // First-Time Setup Wizard Section
        item {
            Text("ONBOARDING & SETUP WIZARD", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    Text("Re-run Setup Wizard", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Re-configure your profile name, picture, visual theme, and audio preferences through the initial walkthrough.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onReRunOnboarding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        modifier = Modifier.fillMaxWidth().testTag("rerun_onboarding_button")
                    ) {
                        Text("Launch Onboarding Setup")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // About & Credits Section
        item {
            Text("ABOUT & CREDITS", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(
                themeConfig = themeConfig,
                modifier = Modifier.fillMaxWidth().testTag("about_app_card"),
                cornerRadius = 20.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FloWave Audio Engine",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Version 2.4.0 (Liquid Glass Edition)",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Developer", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("Ritam", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Framework", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("Jetpack Compose & Media3", color = Color.White, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Render Engine", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("Dynamic Refraction Glass Canvas", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_instagram),
                            contentDescription = "Instagram Logo",
                            tint = Color(0xFFE1306C),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INSTAGRAM PROFILES",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First handle: not_your_ritam
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/not_your_ritam"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_instagram),
                                    contentDescription = "Instagram Logo",
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "not_your_ritam",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Follow",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Second handle: ritam.localhost
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/ritam.localhost"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_instagram),
                                    contentDescription = "Instagram Logo",
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ritam.localhost",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Follow",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "© 2026 Ritam. All rights reserved.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Designed & crafted for high-performance audio playback.",
                        color = Color(0xFF475569),
                        fontSize = 10.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }
    }
}
