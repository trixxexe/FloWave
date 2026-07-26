package com.trixxexe.trixxwave.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.db.Profile
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherScreen(
    profiles: List<Profile>,
    themeConfig: ThemeConfig,
    onSelectProfile: (Profile) -> Unit,
    onCreateProfile: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newAvatarUri by remember { mutableStateOf<String?>(null) }

    val accentColor = getThemeAccentColor(themeConfig)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            newAvatarUri = it.toString()
        }
    }

    Column(
        modifier = Modifier
            .testTag("profile_switcher_screen")
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Profiles & Listeners",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(profiles) { profile ->
                LiquidGlassCard(
                    themeConfig = themeConfig,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    onClick = { onSelectProfile(profile) },
                    testTag = "profile_item_${profile.id}"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2238))
                                .border(1.5.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile.avatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = profile.avatarUri,
                                    contentDescription = profile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (profile.isActive) {
                                Text(
                                    text = "Active Profile",
                                    color = accentColor,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (profile.isActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = accentColor
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_profile_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Profile")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Listener Profile", fontWeight = FontWeight.Bold)
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Profile", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2238))
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (newAvatarUri != null) {
                            AsyncImage(
                                model = newAvatarUri,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = accentColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap circle to upload, or pick a preset below:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val packageName = context.packageName

                    val presetAvatars = listOf(
                        "Cyber Girl" to "android.resource://$packageName/drawable/ic_avatar_anime_1",
                        "Wave Boy" to "android.resource://$packageName/drawable/ic_avatar_anime_2",
                        "Gold Star" to "android.resource://$packageName/drawable/ic_avatar_anime_3",
                        "Mint Cyber" to "android.resource://$packageName/drawable/ic_avatar_anime_4",
                        "Synth Vocal" to "android.resource://$packageName/drawable/ic_avatar_anime_5",
                        "Cat Pink" to "android.resource://$packageName/drawable/ic_avatar_anime_6",
                        "Hoodie Lofi" to "android.resource://$packageName/drawable/ic_avatar_anime_7",
                        "Acoustic Sun" to "android.resource://$packageName/drawable/ic_avatar_anime_8"
                    )

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetAvatars.size) { index ->
                            val (label, url) = presetAvatars[index]
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (newAvatarUri == url) 2.dp else 0.dp,
                                        color = if (newAvatarUri == url) accentColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newAvatarUri = url }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            onCreateProfile(newProfileName, newAvatarUri)
                            newProfileName = ""
                            newAvatarUri = null
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1D2E)
        )
    }
}
