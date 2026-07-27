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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.db.RecentSearch
import com.trixxexe.trixxwave.data.db.Song
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard
import com.trixxexe.trixxwave.ui.components.glass.getThemeAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    allSongs: List<Song>,
    themeConfig: ThemeConfig,
    recentSearches: List<RecentSearch> = emptyList(),
    onSaveSearchQuery: (String) -> Unit = {},
    onDeleteRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onSongClick: (Song) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val accentColor = getThemeAccentColor(themeConfig)

    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) {
            allSongs
        } else {
            val q = searchQuery.trim().lowercase()
            allSongs.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        (it.moodTags?.lowercase()?.contains(q) == true)
            }
        }
    }

    Column(
        modifier = Modifier
            .testTag("search_screen")
            .fillMaxSize()
    ) {
        Text(
            text = "Search Library",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs, artists, albums, or mood tags...", color = Color(0xFF64748B)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = accentColor)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        onSaveSearchQuery(searchQuery)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0x33FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Searches Section (Shown when search bar is idle or when active)
        if (searchQuery.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent Searches",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (recentSearches.isNotEmpty()) {
                        TextButton(
                            onClick = { onClearRecentSearches() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear All", color = accentColor, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (recentSearches.isEmpty()) {
                    LiquidGlassCard(
                        themeConfig = themeConfig,
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No recent searches yet. Search for tracks, artists, or AI mood tags.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(recentSearches) { recent ->
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable {
                                    searchQuery = recent.query
                                    onSaveSearchQuery(recent.query)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = recent.query,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .clickable { onDeleteRecentSearch(recent.query) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Delete Search",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All Tracks (${allSongs.size})",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Results List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp, top = 8.dp)
        ) {
            items(filteredSongs) { song ->
                SongRowItem(
                    song = song,
                    themeConfig = themeConfig,
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onSaveSearchQuery(searchQuery)
                        }
                        onSongClick(song)
                    }
                )
            }
        }
    }
}
