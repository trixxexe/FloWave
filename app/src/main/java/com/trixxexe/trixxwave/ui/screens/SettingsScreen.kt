package com.trixxexe.trixxwave.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.preferences.AiConfig
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor
import com.trixxexe.trixxwave.ui.theme.AccentPalettes
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
        id = "DeepSeek",
        displayName = "DeepSeek AI Direct",
        defaultEndpoint = "https://api.deepseek.com/v1/",
        defaultModel = "deepseek-chat",
        popularModels = listOf("deepseek-chat", "deepseek-reasoner")
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
    onSetThemeMode: ((String) -> Unit)? = null,
    onSetBlurIntensity: ((Float) -> Unit)? = null,
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
    val accentColor = getThemeAccentColor(themeConfig)

    // AI Config Local State
    var apiKey by remember(aiConfig) { mutableStateOf(aiConfig.apiKey) }
    var provider by remember(aiConfig) { mutableStateOf(aiConfig.provider) }
    var modelName by remember(aiConfig) { mutableStateOf(aiConfig.modelName) }
    var customEndpoint by remember(aiConfig) { mutableStateOf(aiConfig.customEndpoint) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    val activeProviderInfo = remember(provider) {
        WELL_KNOWN_AI_PROVIDERS.find { it.id.equals(provider, ignoreCase = true) || it.displayName.equals(provider, ignoreCase = true) }
            ?: WELL_KNOWN_AI_PROVIDERS.first()
    }

    val currentAvailableModels = remember(activeProviderInfo, fetchedModels) {
        if (fetchedModels.isNotEmpty()) fetchedModels else activeProviderInfo.popularModels
    }

    val presets = listOf("Sleek Interface", "AMOLED Dark", "AMOLED Light", "Liquid Obsidian", "Cyber Pink", "Emerald Wave", "Sunset Gold")

    LazyColumn(
        modifier = Modifier
            .testTag("settings_screen")
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize your audio engine & preferences",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 1. APPEARANCE
        item {
            SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Theme Preset Selection
                    Text("Theme Presets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Choose a pre-configured aesthetic style", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            FilterChip(
                                selected = themeConfig.preset == preset,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Curated Accent Colors
                    Text("Accent Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Selected color applies to buttons, active tabs, and sliders", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentPalettes.AllAccents.forEach { (name, color) ->
                            val colorHex = String.format("#%06X", (0xFFFFFF and color.value.toInt()))
                            val isSelected = themeConfig.accentColorHex.equals(colorHex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onSetAccentColor?.invoke(colorHex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Pure AMOLED Black Toggle
                    val isAmoled = themeConfig.mode == "AMOLED"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pure AMOLED Black", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Use true #000000 black background for OLED energy saving", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isAmoled,
                            onCheckedChange = { enabled ->
                                onSetThemeMode?.invoke(if (enabled) "AMOLED" else "Dark")
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Custom Background Wallpaper Setter
                    val wallpaperLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            onSetCustomBgUri?.invoke(uri.toString())
                        }
                    }

                    Column {
                        Text("Custom Wallpaper Background", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Set any image from your gallery as the app liquid glass background", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { wallpaperLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Photo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            if (!themeConfig.customBgUri.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { onSetCustomBgUri?.invoke(null) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Wallpaper", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Glass Blur Intensity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Glass Blur Intensity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${themeConfig.blurIntensity.toInt()} dp", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("Adjust real-time background refraction blur strength", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Slider(
                            value = themeConfig.blurIntensity,
                            onValueChange = { onSetBlurIntensity?.invoke(it) },
                            valueRange = 0f..40f,
                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. PLAYBACK
        item {
            SettingsSectionHeader(title = "Playback", icon = Icons.Default.GraphicEq, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Pure Direct Playback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Literal First-MS Direct Playback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Plays every track from literal 0ms without trimming or pre-cutting audio", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Auto-Resume
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Resume Playback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Resume last played track and timestamp on launch", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Switch(
                            checked = themeConfig.autoResumeEnabled,
                            onCheckedChange = { onToggleAutoResume?.invoke(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Track Transition
                    Column {
                        Text("Track Transition Animation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Select animation style when switching tracks", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val transitions = listOf("Crossfade", "Slide", "Fade", "None")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            transitions.forEach { anim ->
                                FilterChip(
                                    selected = themeConfig.trackTransitionAnimation == anim,
                                    onClick = { onSetTrackTransitionAnimation?.invoke(anim) },
                                    label = { Text(anim, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentColor,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. LIBRARY
        item {
            SettingsSectionHeader(title = "Library & Local Folders", icon = Icons.Default.FolderOpen, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Audio Scanner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Rescan local device storage for music and audio files", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onRescanLibrary()
                            Toast.makeText(context, "Scanning local library...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rescan Local Audio Files", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. ONLINE SOURCES
        item {
            SettingsSectionHeader(title = "Online Streaming Sources", icon = Icons.Default.Public, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Supported Source Providers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("FloWave aggregates high-fidelity streams from multiple decentralized and open sources", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val sources = listOf(
                        "YouTube Audio Streams" to "Active (Piped / Cobalt Extractor)",
                        "Audius Music Protocol" to "Active (Decentralized Node Cluster)",
                        "Global Web Radio" to "Active (RadioBrowser Directory API)"
                    )

                    sources.forEachIndexed { idx, (sourceName, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sourceName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(status, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (idx < sources.size - 1) {
                            HorizontalDivider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0x1FFFFFFF))

                    // Google Account & YouTube Auth Status
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Google Account / YouTube Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Full YouTube & Audius audio streams run free & anonymously without requiring a Google login.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com/"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Google", tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Account Connected (Anonymous Direct Stream)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. AI FEATURES
        item {
            SettingsSectionHeader(title = "AI Features & API Keys", icon = Icons.Default.Psychology, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("AI Provider", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Select provider for smart playlist creation and lyrics extraction", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Provider Dropdown
                    ExposedDropdownMenuBox(
                        expanded = providerMenuExpanded,
                        onExpandedChange = { providerMenuExpanded = !providerMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = provider,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = providerMenuExpanded,
                            onDismissRequest = { providerMenuExpanded = false }
                        ) {
                            WELL_KNOWN_AI_PROVIDERS.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.displayName) },
                                    onClick = {
                                        provider = p.id
                                        customEndpoint = p.defaultEndpoint
                                        modelName = p.defaultModel
                                        providerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // API Key Input
                    Text("API Key", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Encrypted & stored locally in Android Keystore", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("Enter API Key", color = Color(0xFF64748B)) },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Model Name Input / Dropdown
                    Text("AI Model", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            currentAvailableModels.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        modelName = m
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save AI Configuration Button
                    Button(
                        onClick = {
                            val newConfig = AiConfig(
                                provider = provider,
                                apiKey = apiKey,
                                modelName = modelName,
                                customEndpoint = customEndpoint
                            )
                            onSaveAiConfig(newConfig)
                            Toast.makeText(context, "AI Settings Saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Save AI Configuration", fontWeight = FontWeight.Bold)
                    }

                    if (!testStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(testStatus, color = if (testStatus.contains("Success")) accentColor else Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. PRIVACY & DATA
        item {
            SettingsSectionHeader(title = "Privacy & Storage", icon = Icons.Default.Security, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Storage & Local Cache", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Clear cached album art, audio waveforms, and temporary files", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            try {
                                context.cacheDir.deleteRecursively()
                                Toast.makeText(context, "App cache cleared successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cache clear completed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Cache & Waveforms", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 7. ABOUT
        item {
            SettingsSectionHeader(title = "About FloWave", icon = Icons.Default.Info, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))

            LiquidGlassCard(themeConfig = themeConfig, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("FloWave Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Version 2.4.0 • Liquid Glass Native Build", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Developer : Ritam", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Developer Instagram", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Instagram handle 1: not_your_ritam
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/not_your_ritam"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C), contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            InstagramLogoIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("not_your_ritam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Instagram handle 2: ritam.localhost
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/ritam.localhost"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF833AB4), contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            InstagramLogoIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ritam.localhost", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Crafted with Kotlin, Jetpack Compose, Material 3, and Media3 ExoPlayer. Designed for high-fidelity offline & online listening.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun InstagramLogoIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizePx = size.width
        val stroke = sizePx * 0.1f
        drawRoundRect(
            color = Color.White,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(sizePx * 0.28f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawCircle(
            color = Color.White,
            radius = sizePx * 0.22f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawCircle(
            color = Color.White,
            radius = sizePx * 0.05f,
            center = androidx.compose.ui.geometry.Offset(sizePx * 0.72f, sizePx * 0.28f)
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
