package com.trixxexe.trixxwave.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    themeConfig: ThemeConfig,
    onCompleteOnboarding: (name: String, avatarUri: String?, selectedTheme: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var profileName by remember { mutableStateOf("Main Listener") }
    var avatarUriString by remember { mutableStateOf<String?>(null) }
    var selectedPreset by remember { mutableStateOf(themeConfig.preset) }
    var aiAutoTaggingEnabled by remember { mutableStateOf(true) }

    // Image Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUriString = it.toString()
        }
    }

    val totalSteps = 4
    val accentColor = getThemeAccentColor(themeConfig.copy(preset = selectedPreset))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen")
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0 until totalSteps).forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (index == currentStep) 28.dp else 8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index == currentStep) accentColor
                                else Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (currentStep) {
                0 -> WelcomeTutorialStep(accentColor)
                1 -> ProfileSetupStep(
                    profileName = profileName,
                    onNameChange = { profileName = it },
                    avatarUri = avatarUriString,
                    onSelectAvatarUri = { uri -> avatarUriString = uri },
                    onPickFromGallery = { photoPickerLauncher.launch("image/*") },
                    accentColor = accentColor,
                    themeConfig = themeConfig
                )
                2 -> ThemeSelectionStep(
                    selectedPreset = selectedPreset,
                    onPresetSelected = { selectedPreset = it },
                    accentColor = accentColor,
                    themeConfig = themeConfig
                )
                3 -> MusicPreferencesStep(
                    aiAutoTaggingEnabled = aiAutoTaggingEnabled,
                    onToggleAiAutoTagging = { aiAutoTaggingEnabled = it },
                    accentColor = accentColor,
                    themeConfig = themeConfig
                )
            }
        }

        // Bottom Action Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Back", color = Color(0xFF94A3B8), fontSize = 15.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onCompleteOnboarding(
                            profileName.ifBlank { "Main Listener" },
                            avatarUriString,
                            selectedPreset
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(50.dp)
                    .widthIn(min = 140.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (currentStep == totalSteps - 1) "Start Listening" else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (currentStep == totalSteps - 1) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeTutorialStep(accentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .border(2.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Logo",
                tint = accentColor,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome to FloWave",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Next-generation liquid glass audio player with AI acoustic mood tagging, synchronized lyrics, and dynamic visualizer refraction.",
            color = Color(0xFF94A3B8),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ProfileSetupStep(
    profileName: String,
    onNameChange: (String) -> Unit,
    avatarUri: String?,
    onSelectAvatarUri: (String?) -> Unit,
    onPickFromGallery: () -> Unit,
    accentColor: Color,
    themeConfig: ThemeConfig
) {
    val defaultAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=300&q=80"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Set Up Your Profile",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Personalize your identity and profile picture",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Avatar Display & Pick Button
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2238))
                .border(2.dp, accentColor, CircleShape)
                .clickable { onPickFromGallery() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = accentColor,
                    modifier = Modifier.size(52.dp)
                )
            }

            // Camera Overlay badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Pick Photo",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPickFromGallery,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF), contentColor = Color.White),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Choose from Gallery / Photos", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preset Avatars Section (2 Male, 2 Female, 4 Animals)
        Text("Or pick an animated Ghibli preset avatar:", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        val presetAvatars = listOf(
            "Male 1" to "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=400&q=80",
            "Male 2" to "https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=400&q=80",
            "Female 1" to "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=400&q=80",
            "Female 2" to "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80",
            "Dog" to "https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=400&q=80",
            "Cat" to "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=400&q=80",
            "Elephant" to "https://images.unsplash.com/photo-1557050543-4d5f4e07ef46?auto=format&fit=crop&w=400&q=80",
            "Monkey" to "https://images.unsplash.com/photo-1540573133985-780688d172e2?auto=format&fit=crop&w=400&q=80"
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(presetAvatars.size) { index ->
                val (label, url) = presetAvatars[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectAvatarUri(url) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (avatarUri == url) 2.5.dp else 1.dp,
                                color = if (avatarUri == url) accentColor else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = if (avatarUri == url) accentColor else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = if (avatarUri == url) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = profileName,
            onValueChange = onNameChange,
            label = { Text("Your Name or Alias") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                unfocusedBorderColor = Color(0x33FFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input"),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun ThemeSelectionStep(
    selectedPreset: String,
    onPresetSelected: (String) -> Unit,
    accentColor: Color,
    themeConfig: ThemeConfig
) {
    val presets = listOf(
        Triple("Sleek Interface", "#F27D26", "Warm Amber Orange"),
        Triple("Liquid Obsidian", "#00F5D4", "Neon Cyan & Obsidian"),
        Triple("Cyber Pink", "#FF007A", "Neon Pink & Electric"),
        Triple("Emerald Wave", "#10B981", "Deep Forest Mint"),
        Triple("Sunset Gold", "#F59E0B", "Amber Gold Glow"),
        Triple("Aether White", "#6366F1", "Light Slate Canvas")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Select Your Vibe",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose your glass material and theme palette",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEach { (name, hex, desc) ->
                val cardAccent = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { accentColor }
                val isSelected = selectedPreset == name

                LiquidGlassCard(
                    themeConfig = themeConfig.copy(preset = name, accentColorHex = hex),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPresetSelected(name) },
                    cornerRadius = 16.dp,
                    testTag = "theme_option_$name"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(cardAccent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onPresetSelected(name) },
                            colors = RadioButtonDefaults.colors(selectedColor = cardAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicPreferencesStep(
    aiAutoTaggingEnabled: Boolean,
    onToggleAiAutoTagging: (Boolean) -> Unit,
    accentColor: Color,
    themeConfig: ThemeConfig
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Smart Audio Preferences",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Configure AI track tagging and audio engine features",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        LiquidGlassCard(
            themeConfig = themeConfig,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = accentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Acoustic Tagging", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Automatically categorizes tracks by mood and genre tags", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = aiAutoTaggingEnabled,
                        onCheckedChange = onToggleAiAutoTagging,
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Engine", tint = accentColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("10-Band Graphic Equalizer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Custom frequency tuning & bass boost powered by Android AudioFX", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
