package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

data class NavItemData(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun LiquidGlassNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    themeConfig: ThemeConfig,
    modifier: Modifier = Modifier
) {
    val accentColor = getThemeAccentColor(themeConfig)

    val navItems = listOf(
        NavItemData("home", "Home", Icons.Default.Home),
        NavItemData("library", "Library", Icons.Default.LibraryMusic),
        NavItemData("settings", "Settings", Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            .testTag("liquid_glass_navigation_dock"),
        contentAlignment = Alignment.Center
    ) {
        // Floating Outer Liquid Glass Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    themeConfig = themeConfig,
                    cornerRadius = 32.dp
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Home, Library, Settings
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val itemBgColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 250),
                        label = "itemBgColor"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor else Color(0xFF94A3B8),
                        animationSpec = tween(durationMillis = 250),
                        label = "iconColor"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(itemBgColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(accentColor.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f))
                                        ),
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Action Pill: Search Circle Button
            val isSearchSelected = currentRoute == "search"
            val searchBgColor by animateColorAsState(
                targetValue = if (isSearchSelected) accentColor else Color.White.copy(alpha = 0.12f),
                animationSpec = tween(durationMillis = 250),
                label = "searchBgColor"
            )
            val searchIconColor by animateColorAsState(
                targetValue = if (isSearchSelected) Color.Black else accentColor,
                animationSpec = tween(durationMillis = 250),
                label = "searchIconColor"
            )

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(searchBgColor)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.4f), accentColor.copy(alpha = 0.3f))
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigate("search") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = searchIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
