package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorDialog(
    initialPlainLyrics: String?,
    initialSyncedLrc: String?,
    themeConfig: ThemeConfig,
    onDismiss: () -> Unit,
    onSave: (plainLyrics: String?, syncedLrc: String?) -> Unit
) {
    var activeEditorTab by remember { mutableStateOf(if (!initialSyncedLrc.isNullOrBlank()) 0 else 1) } // 0: Synced LRC, 1: Plain Text
    var syncedLrcText by remember { mutableStateOf(initialSyncedLrc ?: "") }
    var plainLyricsText by remember { mutableStateOf(initialPlainLyrics ?: "") }

    val accentColor = getThemeAccentColor(themeConfig)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121218),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("lyrics_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Fix Lyrics",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Manual Lyrics Editor",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Saves directly to Room database",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Row for Synced LRC vs Plain Text
                TabRow(
                    selectedTabIndex = activeEditorTab,
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Tab(
                        selected = activeEditorTab == 0,
                        onClick = { activeEditorTab = 0 },
                        text = { Text("Synced LRC", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeEditorTab == 1,
                        onClick = { activeEditorTab = 1 },
                        text = { Text("Plain Text", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeEditorTab == 0) {
                    Text(
                        text = "Format: [mm:ss.xx] Lyrics line text",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = syncedLrcText,
                        onValueChange = { syncedLrcText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .testTag("synced_lrc_input_field"),
                        placeholder = { Text("[00:12.50] Hello world\n[00:15.80] Second line...", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                } else {
                    Text(
                        text = "Enter raw lyrics text (one verse per line)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = plainLyricsText,
                        onValueChange = { plainLyricsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .testTag("plain_lyrics_input_field"),
                        placeholder = { Text("Verse 1\nLine one of lyrics...", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_fix_lyrics_button")
                    ) {
                        Text("Cancel", color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            onSave(plainLyricsText, syncedLrcText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_fix_lyrics_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Room DB", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
